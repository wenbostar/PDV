package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.*;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Import mzIdentML file
 *
 * Created by Ken on 9/28/2017.
 */
public class MzIDFileImport {

    /**
     * mzIdentML files saving objective
     */
    private MzIdentMLType mzIdentMLType;
    /**
     * Modification mass map from unimod
     */
    private HashMap<String,HashMap<Double, String >> modificationMass;
    /**
     * mzID file name
     */
    private String mzIDName;
    /**
     * Peptide ref and details map
     */
    private HashMap<String, Object[]> peptideMap = new HashMap<>();
    /**
     * Spectrum files ref amd details map
     */
    private HashMap<String, String> spectrumFileMap = new HashMap<>();
    /**
     * SpectrumFactory load all spectrum files import from utilities
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Spectrum file type
     */
    private String spectrumFileType;
    /**
     * Original information hash
     */
    private HashMap<String, Object> originalInfor = new HashMap<>();
    /**
     * Details information list
     */
    private ArrayList<String> detailsList = new ArrayList<>();
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * Extral parameters
     */
    private ArrayList<String> scoreName;
    /**
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param mzIDFile MzIdentML file
     * @param mzIdentMLType Object saving result
     * @param modificationMass Modification and mass
     * @param spectrumFactory Object saving spectrum
     * @param spectrumFileType Spectrum file type
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public MzIDFileImport(PDVMainClass pdvMainClass, File mzIDFile, MzIdentMLType mzIdentMLType, HashMap<String,HashMap<Double, String >> modificationMass,
                          SpectrumFactory spectrumFactory, String spectrumFileType, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {

        this.pdvMainClass = pdvMainClass;
        this.mzIDName = mzIDFile.getName();
        this.mzIdentMLType = mzIdentMLType;
        this.modificationMass = modificationMass;
        this.progressDialog = progressDialog;
        this.spectrumFactory = spectrumFactory;
        this.spectrumFileType = spectrumFileType;

        String dbName = mzIDFile.getParentFile().getAbsolutePath()+"/"+ mzIDName+".db";
        if (new File(dbName).exists()){
            dbName = mzIDFile.getParentFile().getAbsolutePath()+"/"+ mzIDName+"1.db";
        }
        sqLiteConnection = new SQLiteConnection(dbName);

        scoreName = getAllOtherPara();

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseMzID();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse mzIdentML file, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                    currentThread().interrupt();
                }
            }
        }.start();
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    private ArrayList<String> getAllOtherPara(){
        ArrayList<String> scoreName = new ArrayList<>();

        try {
            for (SpectrumIdentificationListType spectrumIdentificationListType : mzIdentMLType.getDataCollection().getAnalysisData().getSpectrumIdentificationList()) {

                for (SpectrumIdentificationResultType spectrumIdentificationResultType : spectrumIdentificationListType.getSpectrumIdentificationResult()) {
                    for (SpectrumIdentificationItemType spectrumIdentificationItemType : spectrumIdentificationResultType.getSpectrumIdentificationItem()) {

                        for (AbstractParamType abstractParamType : spectrumIdentificationItemType.getParamGroup()) {

                            if (abstractParamType instanceof CVParamType) {
                                String name = abstractParamType.getName().replaceAll("[^a-zA-Z]", "");
                                if (name.length() >= 30) {
                                    name = name.substring(0, 29);
                                }
                                if (!scoreName.contains(name)) {
                                    scoreName.add(name);
                                }
                            } else if (abstractParamType instanceof UserParamType) {
                                String name = "UP" + abstractParamType.getName().replaceAll("[^a-zA-Z]", "");
                                if (name.length() >= 30) {
                                    name = name.substring(0, 29);
                                }
                                if (!scoreName.contains(name)) {
                                    scoreName.add(name);
                                }
                            }

                        }
                        break;
                    }
                }
            }
            sqLiteConnection.setScoreNum(scoreName.size());
        } catch (Exception e){
            JOptionPane.showMessageDialog(
                    null, "Failed to get score name from mzIdentML. Please check your file.",
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return scoreName;
    }

    /**
     * Parsing file
     * @throws SQLException
     * @throws IOException
     */
    private void parseMzID() throws SQLException, IOException {

        Connection connection = sqLiteConnection.getConnection();

        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?");

        for (String name : scoreName){
            addQuery.append(", ").append(name).append(" OBJECT(50)");
            addValuesQuery.append(",?");
        }
        addValuesQuery.append(")");

        String addTableQuery = "CREATE TABLE SpectrumMatch" + " (PSMIndex Char, MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object" + addQuery +", PRIMARY KEY(PSMIndex))";
        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            System.err.println("An error occurred while creating table SpectrumMatch");
            JOptionPane.showMessageDialog(
                    null, "Failed to connect database.",
                    "Error Connecting to DB", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
            e.printStackTrace();
        }finally {
            statement.close();
        }

        try {

            List<PeptideType> peptideTypeList = mzIdentMLType.getSequenceCollection().getPeptide();

            if (peptideTypeList.size() == 0){
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(
                        null, "No peptide found in result file.",
                        "Error Parsing", JOptionPane.ERROR_MESSAGE);
                progressDialog.setRunFinished();
            }

            Object[] peptideArray;

            for (PeptideType peptideType : peptideTypeList) {

                peptideArray = new Object[2];
                peptideArray[0] = peptideType.getPeptideSequence();

                if (peptideType.getModification() != null) {
                    peptideArray[1] = peptideType.getModification();
                }

                peptideMap.put(peptideType.getId(), peptideArray);
            }

            ArrayList<String> spectrumLocationList = new ArrayList<>();
            List<SpectraDataType> spectraDataTypeList = mzIdentMLType.getDataCollection().getInputs().getSpectraData();
            for (SpectraDataType spectraDataType : spectraDataTypeList) {
                spectrumLocationList.add(spectraDataType.getLocation());
                if (spectraDataType.getName() != null) {
                    spectrumFileMap.put(spectraDataType.getId(), spectraDataType.getName());
                } else {
                    String spectrumFileName;
                    if (spectraDataType.getLocation().contains("/")){
                        spectrumFileName = spectraDataType.getLocation().split("/")[spectraDataType.getLocation().split("/").length - 1];
                    } else {
                        spectrumFileName = spectraDataType.getLocation().split("\\\\")[spectraDataType.getLocation().split("\\\\").length - 1];
                    }
                    spectrumFileMap.put(spectraDataType.getId(), spectrumFileName);
                }
            }

            getOrignInfor(spectrumLocationList);
        }catch (Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null, "Failed to parse mzIdentML, please check your file.",
                    "Error Parsing", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
        }

        ArrayList<String> fileInFactory = spectrumFactory.getMgfFileNames();

        Integer matchFileNum = 0;

        for(String id: spectrumFileMap.keySet()){

            if(spectrumFileType.equals("mgf")){

                if (fileInFactory.contains(spectrumFileMap.get(id))){
                    matchFileNum++;
                }

             } else if (spectrumFileType.equals("mzml") || spectrumFileType.equals("mzxml")){
                matchFileNum++;
            }
        }

        if (matchFileNum == 0){
            System.out.println("No matching file");
            JOptionPane.showMessageDialog(
                    null, "The spectrum file cannot match it in mzIdentML",
                    "Error Matching", JOptionPane.ERROR_MESSAGE);
            Thread.currentThread().interrupt();
            progressDialog.setRunFinished();
        }

        int count = 0;
        int countRound = 0;

        String spectrumIndex;//Spectrum index in spectrum file;
        String spectrumNumber;//Spectrum id in mzID;
        String spectrumFileRef;
        String currentSpectrumFile;
        String peptideRef;
        int rank;
        double massError = 0;
        double calculatedMZ;
        double experimentMZ = 0.0;
        String peptideSequence;
        String sequenceSaved = "";
        String spectrumTitle = "";
        String spectrumID;//Spectrum index in spectrum file;
        String spectrumId;//Spectrum id in mzID;
        List<ModificationType> modifications;
        ArrayList<String> spectrumIndexList = new ArrayList<>();
        List<AbstractParamType> abstractParamTypeList = new ArrayList<>();
        PreparedStatement preparedStatement = null;
        String modificationName = null;
        HashMap<Double, String> massModification;
        ArrayList<ModificationMatch> utilitiesModifications;
        Charge peptideCharge;
        PeptideAssumption peptideAssumption;
        SpectrumMatch currentMatch;
        List<SpectrumIdentificationItemType> spectrumIdentificationItems;

        Pattern pattern = Pattern.compile("-?[0-9]+.?[0-9]+");

        for (SpectrumIdentificationListType spectrumIdentificationListType : mzIdentMLType.getDataCollection().getAnalysisData().getSpectrumIdentificationList()) {

            List<SpectrumIdentificationResultType> spectrumIdentificationResults = spectrumIdentificationListType.getSpectrumIdentificationResult();

            for (SpectrumIdentificationResultType spectrumIdentificationResultType : spectrumIdentificationResults) {

                int rankNum = 0;//The number of matches;

                if(count == 0){
                    String addDataQuery = "INSERT INTO SpectrumMatch "+addValuesQuery;

                    preparedStatement = connection.prepareStatement(addDataQuery);
                }

                spectrumID = spectrumIdentificationResultType.getSpectrumID();

                if(spectrumID.contains(" ")){
                    String [] eachBig = spectrumID.split(" ");
                    spectrumIndex = eachBig[eachBig.length-1].split("=")[1];
                }else {
                    spectrumIndex = spectrumID.split("=")[1];
                }

                //get id of mzID
                spectrumId = spectrumIdentificationResultType.getId();

                if (spectrumId != null) {
                    spectrumNumber = spectrumId;
                } else {
                    JOptionPane.showMessageDialog(
                            null, "Spectrum ID format error in mzIdentML.",
                            "Error Format", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    break;
                }

                spectrumIndexList.add(spectrumNumber);

                spectrumFileRef = spectrumIdentificationResultType.getSpectraDataRef();
                currentSpectrumFile = spectrumFileMap.get(spectrumFileRef);

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(currentSpectrumFile, spectrumIndex));

                if (spectrumFileType.equals("mgf")) {
                    try {
                        spectrumTitle = spectrumFactory.getSpectrumTitle(currentSpectrumFile, Integer.parseInt(spectrumIndex) + 1);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null, "The spectrum title cannot match it in mzIdentML",
                                "Error Matching", JOptionPane.ERROR_MESSAGE);
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                    }
                } else {
                    spectrumTitle = spectrumIndex;
                }

                currentMatch.setSpectrumNumber(Integer.valueOf(spectrumIndex));

                spectrumIdentificationItems = spectrumIdentificationResultType.getSpectrumIdentificationItem();

                for (SpectrumIdentificationItemType spectrumIdentificationItemType : spectrumIdentificationItems) {

                    rankNum++;

                    rank = spectrumIdentificationItemType.getRank();

                    calculatedMZ = spectrumIdentificationItemType.getCalculatedMassToCharge();
                    experimentMZ = spectrumIdentificationItemType.getExperimentalMassToCharge();

                    massError = experimentMZ - calculatedMZ;

                    peptideCharge = new Charge(Charge.PLUS, spectrumIdentificationItemType.getChargeState()); // + 1

                    peptideRef = spectrumIdentificationItemType.getPeptideRef();

                    peptideSequence = (String) peptideMap.get(peptideRef)[0];

                    // get the modifications
                    utilitiesModifications = new ArrayList<>();

                    if (peptideMap.get(peptideRef)[1] != null) {
                        modifications = (List<ModificationType>) peptideMap.get(peptideRef)[1];

                        for (ModificationType modificationType : modifications) {
                            int location = modificationType.getLocation();
                            double monoMassDelta = modificationType.getMonoisotopicMassDelta();

                            if (location == 0) {
                                massModification = modificationMass.get("N-terminus"); //Todo Need add any term in it?
                                location = 1;

                            } else if (location == peptideSequence.length() + 1) {
                                massModification = modificationMass.get("C-terminus");
                                location = peptideSequence.length();

                            } else {
                                massModification = modificationMass.get(peptideSequence.charAt(location - 1) + "");
                            }

                            for (Double mass : massModification.keySet()) {
                                if (Math.abs(mass - monoMassDelta) < 0.005) {//Mass error may cause problem
                                    modificationName = massModification.get(mass);
                                }
                            }

                            if (!allModifications.contains(modificationName)) {
                                allModifications.add(modificationName);
                            }

                            utilitiesModifications.add(new ModificationMatch(modificationName, true, location));

                        }
                    }

                    // create the peptide
                    com.compomics.util.experiment.biology.Peptide peptide = new com.compomics.util.experiment.biology.Peptide(peptideSequence, utilitiesModifications);

                    peptideAssumption = new PeptideAssumption(peptide, rank, 0, peptideCharge, massError, mzIDName);

                    // Check ambiguous amino acid
                    if (AminoAcidSequence.hasCombination(peptideAssumption.getPeptide().getSequence())) {
                        ArrayList<ModificationMatch> previousModificationMatches = peptide.getModificationMatches(),
                                newModificationMatches = null;
                        if (previousModificationMatches != null) {
                            newModificationMatches = new ArrayList<>(previousModificationMatches.size());
                        }
                        for (StringBuilder expandedSequence : AminoAcidSequence.getCombinations(peptide.getSequence())) {
                            com.compomics.util.experiment.biology.Peptide newPeptide = new com.compomics.util.experiment.biology.Peptide(expandedSequence.toString(), newModificationMatches);
                            if (previousModificationMatches != null) {
                                for (ModificationMatch modificationMatch : previousModificationMatches) {
                                    newPeptide.addModificationMatch(new ModificationMatch(modificationMatch.getTheoreticPtm(), modificationMatch.isVariable(), modificationMatch.getModificationSite()));
                                }
                            }
                            PeptideAssumption newAssumption = new PeptideAssumption(newPeptide, peptideAssumption.getRank(), peptideAssumption.getAdvocate(), peptideAssumption.getIdentificationCharge(), peptideAssumption.getScore(), peptideAssumption.getIdentificationFile());
                            currentMatch.addHit(0, newAssumption, false);

                            if (rankNum == 1) {
                                sequenceSaved = newAssumption.getPeptide().getSequence();
                                currentMatch.setBestPeptideAssumption(newAssumption);
                                abstractParamTypeList = spectrumIdentificationItemType.getParamGroup();//It is parameter of one SpectrumIdentificationItem seem as all score;
                            }

                        }

                    } else {
                        currentMatch.addHit(0, peptideAssumption, false);

                        if (rankNum == 1) {
                            sequenceSaved = peptideAssumption.getPeptide().getSequence();
                            currentMatch.setBestPeptideAssumption(peptideAssumption);
                            abstractParamTypeList = spectrumIdentificationItemType.getParamGroup();//It is parameter of one SpectrumIdentificationItem seem as all score;
                        }
                    }
                }

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    try {
                        oos.writeObject(currentMatch);
                    } finally {
                        oos.close();
                    }
                } finally {
                    bos.close();
                }

                preparedStatement.setString(1, spectrumNumber);
                preparedStatement.setDouble(2, experimentMZ);
                preparedStatement.setString(3, spectrumTitle);
                preparedStatement.setString(4, sequenceSaved);
                preparedStatement.setDouble(5, Math.abs(massError));
                preparedStatement.setBytes(6, bos.toByteArray());

                for (AbstractParamType abstractParamType : abstractParamTypeList) {

                    if (abstractParamType instanceof CVParamType) {
                        String name = abstractParamType.getName().replaceAll("[^a-zA-Z]", "");
                        if (name.length() >= 30) {
                            name = name.substring(0, 29);
                        }

                        Integer index = scoreName.indexOf(name) + 7;

                        String value = abstractParamType.getValue();
                        if (pattern.matcher(value).matches()) {
                            preparedStatement.setDouble(index, Double.parseDouble(value));
                        } else {
                            preparedStatement.setString(index, value);
                        }

                    } else if (abstractParamType instanceof UserParamType) {
                        String name = "UP" + abstractParamType.getName().replaceAll("[^a-zA-Z]", "");
                        if (name.length() >= 30) {
                            name = name.substring(0, 29);
                        }

                        Integer index = scoreName.indexOf(name) + 7;

                        String value = abstractParamType.getValue();

                        if (pattern.matcher(value).matches()) {
                            preparedStatement.setDouble(index, Double.parseDouble(value));
                        } else {
                            preparedStatement.setString(index, value);
                        }
                    }
                }

                preparedStatement.addBatch();

                connection.setAutoCommit(false);

                count++;

                if (count == 1000){
                    int[] counts = preparedStatement.executeBatch();
                    connection.setAutoCommit(true);
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                    if(countRound == 0){

                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound ++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                        countRound ++;
                    }

                    count = 0;

                    spectrumIndexList = new ArrayList<>();
                }
            }

            if (count != 0){
                int[] counts = preparedStatement.executeBatch();
                connection.setAutoCommit(true);
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                if(countRound == 0){

                    pdvMainClass.displayResult();
                    pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                    progressDialog.setRunFinished();

                } else {
                    pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                }
            }

            pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
            pdvMainClass.loadingJButton.setText("Import done");
            pdvMainClass.searchButton.setToolTipText("Find items");
            pdvMainClass.searchItemTextField.setToolTipText("Find items");
        }
    }

    /**
     * Return original information
     * @param spectrumLocationList Spectrum location
     */
    private void getOrignInfor(ArrayList<String> spectrumLocationList){
        originalInfor.put("Identification file", mzIDName);
        detailsList.add("Identification file/t/"+mzIDName);
        originalInfor.put("Spectrum File(s)", spectrumLocationList);
        for (int index = 0; index < spectrumLocationList.size(); index ++){
            detailsList.add("Spectrum file: "+index +"/t/" + spectrumLocationList.get(index));
        }

        for(AnalysisSoftwareType analysisSoftwareType: mzIdentMLType.getAnalysisSoftwareList().getAnalysisSoftware()){
            detailsList.add("AnalysisSoftware/t/"+analysisSoftwareType.getName()+"("+analysisSoftwareType.getVersion()+")");
            originalInfor.put("Soft Name", analysisSoftwareType.getName()+" version: "+analysisSoftwareType.getVersion());
        }

        for(SearchDatabaseType searchDatabaseType: mzIdentMLType.getDataCollection().getInputs().getSearchDatabase()){
            detailsList.add(searchDatabaseType.getId()+"/t/"+searchDatabaseType.getLocation());
            originalInfor.put("DB Name", searchDatabaseType.getLocation());
            List<CVParamType> cvParamTypeList = searchDatabaseType.getCvParam();
            ArrayList<String> parameterList = new ArrayList<>() ;
            for(CVParamType cvParamType: cvParamTypeList){
                detailsList.add(cvParamType.getAccession()+"/t/"+cvParamType.getName());
                parameterList.add(cvParamType.getName());
            }
            originalInfor.put("DB Parameters", parameterList);
        }

        HashMap<String, String> fixedModiMap = new HashMap<>();
        HashMap<String, String > variableModiMap = new HashMap<>();
        for(SpectrumIdentificationProtocolType spectrumIdentificationProtocolType : mzIdentMLType.getAnalysisProtocolCollection().getSpectrumIdentificationProtocol() ) {
            for (SearchModificationType searchModificationType: spectrumIdentificationProtocolType.getModificationParams().getSearchModification()){
                if (searchModificationType.isFixedMod()){
                    if(searchModificationType.getResidues().size() == 0 || searchModificationType.getResidues().get(0).equals(".") && searchModificationType.getSpecificityRules().size()!=0){
                        fixedModiMap.put(searchModificationType.getCvParam().get(0).getName()+" on "+searchModificationType.getSpecificityRules().get(0).getCvParam().get(0).getName(), " massDelta: "+searchModificationType.getMassDelta());
                    } else {
                        fixedModiMap.put(searchModificationType.getCvParam().get(0).getName()+" of "+searchModificationType.getResidues().get(0), " massDelta: "+searchModificationType.getMassDelta());
                    }
                } else {
                    if(searchModificationType.getResidues().size() == 0 || searchModificationType.getResidues().get(0).equals(".") && searchModificationType.getSpecificityRules().size()!=0){
                        variableModiMap.put(searchModificationType.getCvParam().get(0).getName()+" on "+searchModificationType.getSpecificityRules().get(0).getCvParam().get(0).getName(),  " massDelta: "+searchModificationType.getMassDelta());
                    } else {
                        variableModiMap.put(searchModificationType.getCvParam().get(0).getName()+" of "+searchModificationType.getResidues().get(0), " massDelta: "+searchModificationType.getMassDelta());
                    }
                }
            }
            int count = 0;
            for (String name : fixedModiMap.keySet()){

                detailsList.add("Fixed Modification: "+count +"/t/"+name+"("+fixedModiMap.get(name)+")");
                count ++;
            }
            count = 0;
            for (String name : variableModiMap.keySet()){

                detailsList.add("Variable Modification: "+count +"/t/"+name+"("+variableModiMap.get(name)+"(");
                count ++;
            }
            originalInfor.put("Fixed Modification", fixedModiMap);
            originalInfor.put("Variable Modification", variableModiMap);

            for (EnzymeType enzymeType: spectrumIdentificationProtocolType.getEnzymes().getEnzyme()){
                detailsList.add("Enzyme/t/"+enzymeType.getEnzymeName().getParamGroup().get(0).getName()+"("+enzymeType.getMissedCleavages()+")");
                originalInfor.put("Enzyme", enzymeType.getEnzymeName().getParamGroup().get(0).getName()+" missedCleavages: "+enzymeType.getMissedCleavages());
            }

            ArrayList<String> parentToleranceList = new ArrayList<>();
            for (CVParamType cvParamType : spectrumIdentificationProtocolType.getParentTolerance().getCvParam()){
                parentToleranceList.add(cvParamType.getName()+": "+cvParamType.getValue()+" "+cvParamType.getUnitName());
                detailsList.add(cvParamType.getName()+"/t/"+cvParamType.getValue()+"("+cvParamType.getUnitName()+")");
            }
            originalInfor.put("Parent Tolerance", parentToleranceList);

            HashMap<String, String> additionalPara = new HashMap<>();

            if (spectrumIdentificationProtocolType.getAdditionalSearchParams() != null){
                for(AbstractParamType abstractParamType: spectrumIdentificationProtocolType.getAdditionalSearchParams().getParamGroup()){
                    if(abstractParamType instanceof UserParamType){
                        additionalPara.put(abstractParamType.getName(), abstractParamType.getValue());
                        detailsList.add(abstractParamType.getName()+"/t/"+abstractParamType.getValue());
                    }
                }
                originalInfor.put("Additional Para", additionalPara);
            }
        }
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Return original information
     * @return Hash map
     */
    public HashMap<String, Object> getOriginalInfor(){
        return originalInfor;
    }

    /**
     * Get result detail list
     * @return ArrayList
     */
    public ArrayList<String> getDetailsList(){
        return detailsList;
    }

    /**
     * Return all modification
     * @return Array list
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }

    /**
     * Return extral parameters
     * @return ArrayList
     */
    public ArrayList<String> getScoreName(){
        return scoreName;
    }
}

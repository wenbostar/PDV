package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import uk.ac.ebi.jmzidml.model.mzidml.PeptideEvidence;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.*;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Import proBed file
 * Created by Ken on 2/23/2018.
 */
public class ProBedFileImport {

    /**
     * ProBed file path
     */
    private String proBedFilePath;
    /**
     * Object to save identification results
     */
    private MzIdentMLType mzIdentMLType;
    /**
     * Protein accession to spectrum details map
     */
    private HashMap<String, ArrayList<String[]>> proteinAndSpectrumMap = new HashMap<>();
    /**
     * Protein accession to peptide
     */
    private HashMap<String, Peptide> proteinAndPeptide = new HashMap<>();
    /**
     * Object to save spectrum
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Object to save modification
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Additional attributes list
     */
    private ArrayList<String> scoreName = new ArrayList<>();
    /**
     * SQLite database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * All modifications list
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum file type
     */
    private String  spectrumFileType;
    /**
     * Spectrum ID to spectrum number
     */
    private HashMap<String, Integer> spectrumIdAndNumber = new HashMap<>();

    /**
     * Main constructor
     * @param pdvMainClass Parent class
     * @param spectrumFactory Spectrum factory
     * @param proBedFile ProBed file
     * @param mzIdentMLType mzIdentML file object
     * @param progressDialog Progress
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ProBedFileImport(PDVMainClass pdvMainClass, SpectrumFactory spectrumFactory, File proBedFile, MzIdentMLType mzIdentMLType,
                            ProgressDialogX progressDialog, String spectrumFileType, HashMap<String, Integer> spectrumIdAndNumber)
            throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.spectrumFactory = spectrumFactory;
        this.proBedFilePath = proBedFile.getAbsolutePath();
        this.mzIdentMLType = mzIdentMLType;
        this.progressDialog = progressDialog;
        this.spectrumFileType = spectrumFileType;
        this.spectrumIdAndNumber = spectrumIdAndNumber;

        String dbName = proBedFile.getParentFile().getAbsolutePath()+"/"+ proBedFile.getName()+".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        scoreName.add("name");
        scoreName.add("score");
        scoreName.add("chrom");
        scoreName.add("chromStart");
        scoreName.add("chromEnd");
        scoreName.add("strand");
        scoreName.add("thickStart");
        scoreName.add("thickEnd");
        scoreName.add("reserved");
        scoreName.add("blockCount");
        scoreName.add("blockSites");
        scoreName.add("chromStarts");
        scoreName.add("proteinAccesion");
        scoreName.add("uniqueness");
        scoreName.add("genomeReferenceVersion");
        scoreName.add("fdr");
        scoreName.add("psmRank");
        scoreName.add("datasetID");
        scoreName.add("uri");

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(19);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseMzID();
                    parsingFile();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing proBed file
     * @throws IOException
     * @throws SQLException
     */
    private void parsingFile() throws IOException, SQLException {

        Connection connection = sqLiteConnection.getConnection();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(proBedFilePath));
        ArrayList<String> psmIndexList = new ArrayList<>();
        HashMap<String, String> modificationMap = getModificationMap();

        String line;
        String[] eachItem;
        ArrayList<String[]> spectrumList;
        SpectrumMatch currentMatch;
        ArrayList<ModificationMatch> utilitiesModifications;
        Peptide peptide;
        PeptideAssumption peptideAssumption;
        String spectrumIndex;
        String spectrumTitle = "";
        String currentSpectrumFile;
        String spectrumID;

        int countAll = 0;
        int count = 0;
        int countRound = 0;
        String score = "";
        String proteinAccesion = "";
        String peptideSequence = "";
        //Double psmScore = 0.0;
        String modifications = "";
        Integer charge = 0;
        Double expMassToCharge = 0.0;
        Double calMassToCharge = 0.0;
        String rank = "";
        String name = "";
        String chrom = "";
        String chromStart = "";
        String chromEnd = "";
        String strand = "";
        String thickStart = "";
        String thickEnd = "";
        String reserved = "";
        String blockCount = "";
        String blockSites = "";
        String chromStarts = "";
        String uniqueness = "";
        String genomeReferenceVersion = "";
        String fdr = "";
        String datasetID = "";
        String uri = "";

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?");

        for (String eachName : scoreName){
            addQuery.append(", ").append(eachName).append(" OBJECT(50)");
            addValuesQuery.append(",?");
        }
        addValuesQuery.append(")");

        String addTableQuery = "CREATE TABLE SpectrumMatch" + " (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object" + addQuery +", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(
                    null, "Failed to connect database.",
                    "Error Connecting to DB", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch "+addValuesQuery;
        PreparedStatement preparedStatement = null;

        while ((line = bufferedReader.readLine()) != null){

            eachItem = line.split("\t");

            if (!eachItem[0].contains("#")) {

                for (Integer index = 0; index < eachItem.length; index++) {

                    if (index == 4) {
                        score = eachItem[4];
                    } else if (index == 12) {
                        proteinAccesion = eachItem[12];
                    } else if (index == 13) {
                        peptideSequence = eachItem[13];
                    } else if (index == 16) {
                        //psmScore = Double.valueOf(eachItem[16]);
                    } else if (index == 18) {
                        modifications = eachItem[18].replace("\"", "");
                    } else if (index == 19) {
                        charge = Integer.valueOf(eachItem[19]);
                    } else if (index == 20) {
                        expMassToCharge = Double.valueOf(eachItem[20]);
                    } else if (index == 21) {
                        calMassToCharge = Double.valueOf(eachItem[21]);
                    } else if (index == 22) {
                        rank = eachItem[22];
                    } else if (index == 0) {
                        chrom = eachItem[0];
                    } else if (index == 1) {
                        chromStart = eachItem[1];
                    } else if (index == 2) {
                        chromEnd = eachItem[2];
                    } else if (index == 3) {
                        name = eachItem[3];
                    } else if (index == 5) {
                        strand = eachItem[5];
                    } else if (index == 6) {
                        thickStart = eachItem[6];
                    } else if (index == 7) {
                        thickEnd = eachItem[7];
                    } else if (index == 8) {
                        reserved = eachItem[8];
                    } else if (index == 9) {
                        blockCount = eachItem[9];
                    } else if (index == 10) {
                        blockSites = eachItem[10];
                    } else if (index == 11) {
                        chromStarts = eachItem[11];
                    } else if (index == 14) {
                        uniqueness = eachItem[14];
                    } else if (index == 15) {
                        genomeReferenceVersion = eachItem[15];
                    } else if (index == 17) {
                        fdr = eachItem[17];
                    } else if (index == 23) {
                        datasetID = eachItem[23];
                    } else if (index == 24) {
                        uri = eachItem[24];
                    }
                }

                peptide = getPeptide(proteinAccesion, peptideSequence);

                peptideAssumption = new PeptideAssumption(peptide, Integer.valueOf(rank), 0, new Charge(1, charge), (calMassToCharge - expMassToCharge));

                spectrumList = getSpectrumList(proteinAccesion, peptideSequence);

                for (String[] fileAndSpectrumIndexAndSpectrumID : spectrumList) {

                    psmIndexList.add(String.valueOf(countAll));

                    if (count == 0) {
                        preparedStatement = connection.prepareStatement(addDataIntoTable);
                    }

                    currentSpectrumFile = fileAndSpectrumIndexAndSpectrumID[0];
                    spectrumIndex = fileAndSpectrumIndexAndSpectrumID[1];
                    spectrumID = fileAndSpectrumIndexAndSpectrumID[2];

                    currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(currentSpectrumFile, spectrumIndex));

                    if (spectrumFileType.equals("mgf")) {
                        currentMatch.setSpectrumNumber(Integer.valueOf(spectrumIndex));
                        try {
                            spectrumTitle = spectrumFactory.getSpectrumTitle(currentSpectrumFile, Integer.parseInt(spectrumIndex) + 1);
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(
                                    null, "The spectrum title cannot match it in proBed",
                                    "Error Matching", JOptionPane.ERROR_MESSAGE);
                            progressDialog.setRunFinished();
                            e.printStackTrace();
                        }
                    } else if (spectrumFileType.equals("mzml")) {
                        currentMatch.setSpectrumNumber(spectrumIdAndNumber.get(spectrumID));
                        spectrumTitle = spectrumIndex;
                    } else {
                        currentMatch.setSpectrumNumber(Integer.valueOf(spectrumIndex));
                        spectrumTitle = spectrumIndex;
                    }

                    currentMatch.setBestPeptideAssumption(peptideAssumption);
                    currentMatch.addHit(0, peptideAssumption, false);

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

                    preparedStatement.setInt(1, countAll);
                    preparedStatement.setDouble(2, expMassToCharge);
                    preparedStatement.setString(3, spectrumTitle);
                    preparedStatement.setString(4, peptideSequence);
                    preparedStatement.setDouble(5, (calMassToCharge - expMassToCharge));
                    preparedStatement.setBytes(6, bos.toByteArray());
                    preparedStatement.setString(7, name);
                    preparedStatement.setString(8, score);
                    preparedStatement.setString(9, chrom);
                    preparedStatement.setInt(10, Integer.parseInt(chromStart));
                    preparedStatement.setInt(11, Integer.parseInt(chromEnd));
                    preparedStatement.setString(12, strand);
                    preparedStatement.setInt(13, Integer.parseInt(thickStart));
                    preparedStatement.setInt(14, Integer.parseInt(thickEnd));
                    preparedStatement.setString(15, reserved);
                    preparedStatement.setInt(16, Integer.parseInt(blockCount));
                    preparedStatement.setString(17, blockSites);
                    preparedStatement.setString(18, chromStarts);
                    preparedStatement.setString(19, proteinAccesion);
                    preparedStatement.setString(20, uniqueness);
                    preparedStatement.setString(21, genomeReferenceVersion);
                    preparedStatement.setString(22, fdr);
                    preparedStatement.setInt(23, Integer.parseInt(rank));
                    preparedStatement.setString(24, datasetID);
                    preparedStatement.setString(25, uri);

                    preparedStatement.addBatch();

                    count++;
                    countAll++;

                    if (count == 1000) {
                        int[] counts = preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        pdvMainClass.allSpectrumIndex.add(psmIndexList);

                        count = 0;

                        if (countRound == 0) {
                            pdvMainClass.displayResult();
                            pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                            progressDialog.setRunFinished();

                            countRound++;

                        } else {
                            pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                            countRound++;
                        }

                        pdvMainClass.buttonCheck();

                        psmIndexList = new ArrayList<>();
                    }
                }
            }
        }

        if(count != 0){

            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.allSpectrumIndex.add(psmIndexList);

            if(countRound == 0){

                pdvMainClass.displayResult();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();

            } else {
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }

            pdvMainClass.buttonCheck();

        }
        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    /**
     * Parsing mzIdentML file
     */
    private void parseMzID(){

        String spectrumID;//Spectrum index in spectrum file;
        String spectrumIndex;//Spectrum index in spectrum file;
        String spectrumFileRef;
        String currentSpectrumFile = "";
        String peptideSequence;
        String modificationName;
        Peptide peptide;
        ArrayList<ModificationMatch> utilitiesModifications;
        ArrayList<String> residues;
        List<ModificationType> modifications;
        String[] fileAndSpectrumIndexAndSpectrumID;
        HashMap<String, String> spectrumFileMap = new HashMap<>();
        HashMap<String, String> dbRefToAccession = new HashMap<>();
        HashMap<String, String> peptideRefAndDBAccession = new HashMap<>();
        HashMap<String, String> peptideRefAndSequence = new HashMap<>();
        HashMap<String, Peptide> peptideRefAndPeptide = new HashMap<>();
        HashMap<String, String> peptideIdAndSequence = new HashMap<>();
        HashMap<String, Peptide> peptideIdAndPeptide = new HashMap<>();

        List<DBSequenceType> dbSequenceTypes = mzIdentMLType.getSequenceCollection().getDBSequence();
        String softName = getSoftName(mzIdentMLType);

        for (DBSequenceType dbSequenceType : dbSequenceTypes){
            dbRefToAccession.put(dbSequenceType.getId(), dbSequenceType.getAccession());
        }

        List<PeptideType> peptideTypes = mzIdentMLType.getSequenceCollection().getPeptide();

        for (PeptideType peptideType : peptideTypes){

            peptideSequence = peptideType.getPeptideSequence();
            utilitiesModifications = new ArrayList<>();

            if (peptideType.getModification() != null) {
                modifications = peptideType.getModification();

                for (ModificationType modificationType : modifications) {
                    int location = modificationType.getLocation();
                    double monoMassDelta = modificationType.getMonoisotopicMassDelta();
                    String nameFromMzID;
                    List<CVParamType> cvParamTypes  = modificationType.getCvParam();

                    if (cvParamTypes != null){

                        CVParamType firstType = cvParamTypes.get(0);
                        if (firstType.getName() != null){
                            nameFromMzID = firstType.getName();
                        } else {
                            nameFromMzID = String.valueOf(monoMassDelta);
                        }

                    } else {
                        nameFromMzID = String.valueOf(monoMassDelta);
                    }


                    if (location == 0) {

                        modificationName = nameFromMzID + " of N-term";

                        if (!ptmFactory.containsPTM(modificationName)){
                            PTM ptm = new PTM(PTM.MODNP, modificationName, monoMassDelta, null);
                            ptm.setShortName(nameFromMzID);
                            ptmFactory.addUserPTM(ptm);
                        }

                        location = 1;

                    } else if (location == peptideSequence.length() + 1) {
                        modificationName = nameFromMzID + " of C-term";

                        if (!ptmFactory.containsPTM(modificationName)){
                            PTM ptm = new PTM(PTM.MODCP, modificationName, monoMassDelta, null);
                            ptm.setShortName(nameFromMzID);
                            ptmFactory.addUserPTM(ptm);
                        }

                        location = peptideSequence.length();

                    } else {
                        residues = new ArrayList<>();
                        String aa = String.valueOf(peptideSequence.charAt(location - 1));
                        residues.add(aa);

                        modificationName = nameFromMzID + " of " + aa;

                        if (!ptmFactory.containsPTM(modificationName)){
                            PTM ptm = new PTM(PTM.MODAA, modificationName, monoMassDelta, residues);
                            ptm.setShortName(nameFromMzID);
                            ptmFactory.addUserPTM(ptm);
                        }
                    }

                    if (!allModifications.contains(modificationName)) {
                        allModifications.add(modificationName);
                    }

                    utilitiesModifications.add(new ModificationMatch(modificationName, true, location));
                }
            }

            peptide = new Peptide(peptideSequence, utilitiesModifications);

            peptideIdAndPeptide.put(peptideType.getId(), peptide);
            peptideIdAndSequence.put(peptideType.getId(), peptideSequence);
        }

        List<PeptideEvidenceType> PeptideEvidenceTypeList = mzIdentMLType.getSequenceCollection().getPeptideEvidence();

        for (PeptideEvidenceType peptideEvidenceType : PeptideEvidenceTypeList) {

            String peptideEvidenceTypeId = peptideEvidenceType.getId();

            peptideRefAndDBAccession.put(peptideEvidenceTypeId, dbRefToAccession.get(peptideEvidenceType.getDBSequenceRef()));
            peptideRefAndSequence.put(peptideEvidenceTypeId, peptideIdAndSequence.get(peptideEvidenceType.getPeptideRef()));
            peptideRefAndPeptide.put(peptideEvidenceTypeId, peptideIdAndPeptide.get(peptideEvidenceType.getPeptideRef()));
        }

        for (String peptideEvidenceID : peptideRefAndDBAccession.keySet()){
            String dbAndSequence = peptideRefAndDBAccession.get(peptideEvidenceID) + "_pdvSplit_" + peptideRefAndSequence.get(peptideEvidenceID);
            proteinAndSpectrumMap.put(dbAndSequence, new ArrayList<>());
            proteinAndPeptide.put(dbAndSequence, peptideRefAndPeptide.get(peptideEvidenceID));
        }

        List<SpectraDataType> spectraDataTypeList = mzIdentMLType.getDataCollection().getInputs().getSpectraData();
        for (SpectraDataType spectraDataType : spectraDataTypeList) {
            if (spectraDataType.getName() != null) {
                spectrumFileMap.put(spectraDataType.getId(), spectraDataType.getName());
            } else {
                String spectrumFileName = spectraDataType.getLocation().split("/")[spectraDataType.getLocation().split("/").length - 1];
                spectrumFileMap.put(spectraDataType.getId(), spectrumFileName);
            }
        }

        for (SpectrumIdentificationListType spectrumIdentificationListType : mzIdentMLType.getDataCollection().getAnalysisData().getSpectrumIdentificationList()) {

            List<SpectrumIdentificationResultType> spectrumIdentificationResults = spectrumIdentificationListType.getSpectrumIdentificationResult();

            for (SpectrumIdentificationResultType spectrumIdentificationResultType : spectrumIdentificationResults) {

                fileAndSpectrumIndexAndSpectrumID = new String[3];

                spectrumID = spectrumIdentificationResultType.getSpectrumID();

                if(spectrumID.contains(" ")){
                    String [] eachBig = spectrumID.split(" ");
                    spectrumIndex = eachBig[eachBig.length-1].split("=")[1];

                    if (softName.toLowerCase().contains("mascot")){ // Soft: MASCOT spectrumID="mzMLid=controllerType=0 controllerNumber=1 scan=0"
                        spectrumID = spectrumID.split("mzMLid=")[1];
                    }
                }else {
                    if (spectrumID.contains("=")) {
                        spectrumIndex = spectrumID.split("=")[1];

                        if (softName.toLowerCase().contains("metamorpheus")){ // soft: MetaMorpheus Scan=000
                            spectrumID = "controllerType=0 controllerNumber=1 scan="+spectrumIndex;
                        }

                    } else { // soft:Crux
                        spectrumIndex = spectrumID.split("-")[0];
                        if (spectrumFileType.equals("mgf")){
                            spectrumIndex = String.valueOf(Integer.valueOf(spectrumID.split("-")[0]) - 1);
                        }
                        spectrumID = "controllerType=0 controllerNumber=1 scan="+spectrumIndex;
                    }
                }

                spectrumFileRef = spectrumIdentificationResultType.getSpectraDataRef();
                if (spectrumFileRef == null){
                    for (String key : spectrumFileMap.keySet()){
                        currentSpectrumFile = spectrumFileMap.get(key);
                    }
                } else {
                    currentSpectrumFile = spectrumFileMap.get(spectrumFileRef);
                }

                fileAndSpectrumIndexAndSpectrumID[0] = currentSpectrumFile;
                fileAndSpectrumIndexAndSpectrumID[1] = spectrumIndex;
                fileAndSpectrumIndexAndSpectrumID[2] = spectrumID;

                for (SpectrumIdentificationItemType spectrumIdentificationItemType : spectrumIdentificationResultType.getSpectrumIdentificationItem()){

                    for (PeptideEvidenceRefType peptideEvidenceRefType : spectrumIdentificationItemType.getPeptideEvidenceRef()){

                        String mainID = peptideRefAndDBAccession.get(peptideEvidenceRefType.getPeptideEvidenceRef()) + "_pdvSplit_" + peptideRefAndSequence.get(peptideEvidenceRefType.getPeptideEvidenceRef());

                        proteinAndSpectrumMap.get(mainID).add(fileAndSpectrumIndexAndSpectrumID);
                    }
                }
            }
        }
    }

    /**
     * Get corresponding spectrum identification item ID according to the protein accession and peptide sequence.
     * @param proteinRef Protein accession
     * @param peptideSequence Peptide sequence
     * @return ArrayList
     */
    private ArrayList<String[]> getSpectrumList(String proteinRef, String peptideSequence){

        ArrayList<String[]> spectrumList = new ArrayList<>();

        String dbAccession;
        String sequence;

        for (String protein : proteinAndSpectrumMap.keySet()){

            if (protein.contains(proteinRef) && protein.contains(peptideSequence)) {
                dbAccession = protein.split("_pdvSplit_")[0];
                sequence = protein.split("_pdvSplit_")[1];
                if (dbAccession.contains(proteinRef) && sequence.equals(peptideSequence)) {

                    spectrumList = proteinAndSpectrumMap.get(protein);

                    break;
                }
            }
        }

        return spectrumList;
    }

    /**
     * Get corresponding peptide according to the protein accession and peptide sequence.
     * @param proteinRef Protein accession
     * @param peptideSequence Peptide sequence
     * @return Peptide
     */
    private Peptide getPeptide(String proteinRef, String peptideSequence){

        Peptide peptide = new Peptide();

        String dbAccession;
        String sequence;

        for (String protein : proteinAndPeptide.keySet()){

            if (protein.contains(proteinRef) && protein.contains(peptideSequence)) {
                dbAccession = protein.split("_pdvSplit_")[0];
                sequence = protein.split("_pdvSplit_")[1];
                if (dbAccession.contains(proteinRef) && sequence.equals(peptideSequence)) {

                    peptide = proteinAndPeptide.get(protein);

                    break;
                }
            }
        }

        return peptide;
    }

    /**
     * Get all modification CV values
     * @return HashMap
     */
    private HashMap<String, String> getModificationMap(){

        HashMap<String, String> modificationMap = new HashMap<>();

        for (String modName : ptmFactory.getPTMs()){

            PTM ptm = ptmFactory.getPTM(modName);

            if (ptm.getCvTerm() != null){
                modificationMap.put(ptm.getCvTerm().getAccession(), ptm.getCvTerm().getName());
            }

        }

        return modificationMap;
    }

    /**
     * Return original information
     */
    private String getSoftName(MzIdentMLType mzIdentMLType){

        String softName = "";

        if (mzIdentMLType.getAnalysisSoftwareList() != null) {
            for (AnalysisSoftwareType analysisSoftwareType : mzIdentMLType.getAnalysisSoftwareList().getAnalysisSoftware()) {

                if (analysisSoftwareType.getName() == null && analysisSoftwareType.getSoftwareName() != null){
                    if (analysisSoftwareType.getSoftwareName().getCvParam() != null){
                        softName = analysisSoftwareType.getSoftwareName().getCvParam().getName();
                    }
                } else {
                    softName = analysisSoftwareType.getName();
                }

            }
        }

        return softName;
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getScoreName(){
        return scoreName;
    }

    /**
     * Return all modifications
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }
}

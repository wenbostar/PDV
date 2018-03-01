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
     * Main constructor
     * @param pdvMainClass Parent class
     * @param spectrumFactory Spectrum factory
     * @param proBedFile ProBed file
     * @param mzIdentMLType mzIdentML file object
     * @param progressDialog Progress
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ProBedFileImport(PDVMainClass pdvMainClass, SpectrumFactory spectrumFactory, File proBedFile, MzIdentMLType mzIdentMLType, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.spectrumFactory = spectrumFactory;
        this.proBedFilePath = proBedFile.getAbsolutePath();
        this.mzIdentMLType = mzIdentMLType;
        this.progressDialog = progressDialog;

        String dbName = proBedFile.getParentFile().getAbsolutePath()+"/"+ proBedFile.getName()+".db";

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

        int countAll = 0;
        int count = 0;
        int countRound = 0;
        String score = "";
        String proteinAccesion = "";
        String peptideSequence = "";
        Double psmScore = 0.0;
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

            for (Integer index = 0; index < eachItem.length; index ++){

                if (index == 4){
                    score = eachItem[4];
                } else if (index == 12){
                    proteinAccesion = eachItem[12];
                } else if (index == 13){
                    peptideSequence = eachItem[13];
                } else if (index == 16){
                    psmScore = Double.valueOf(eachItem[16]);
                } else if (index == 18){
                    modifications = eachItem[18].replace("\"", "");
                } else if (index == 19){
                    charge = Integer.valueOf(eachItem[19]);
                } else if (index == 20){
                    expMassToCharge = Double.valueOf(eachItem[20]);
                } else if (index == 21){
                    calMassToCharge = Double.valueOf(eachItem[21]);
                } else if (index == 22){
                    rank = eachItem[22];
                } else if (index == 0){
                    chrom = eachItem[0];
                } else if (index == 1){
                    chromStart = eachItem[1];
                } else if (index == 2){
                    chromEnd = eachItem[2];
                } else if (index == 3){
                    name = eachItem[3];
                } else if (index == 5){
                    strand = eachItem[5];
                } else if (index == 6){
                    thickStart = eachItem[6];
                } else if (index == 7){
                    thickEnd = eachItem[7];
                } else if (index == 8){
                    reserved = eachItem[8];
                } else if (index == 9){
                    blockCount = eachItem[9];
                } else if (index == 10){
                    blockSites = eachItem[10];
                } else if (index == 11){
                    chromStarts = eachItem[11];
                } else if (index == 14){
                    uniqueness = eachItem[14];
                } else if (index == 15){
                    genomeReferenceVersion = eachItem[15];
                } else if (index == 17){
                    fdr = eachItem[17];
                } else if (index == 23){
                    datasetID = eachItem[23];
                } else if (index ==24){
                    uri = eachItem[24];
                }
            }

            utilitiesModifications = new ArrayList<>();

            if (!modifications.equals(".")) {

                for (String eachMod : modifications.split(", ")) {

                    Integer position = Integer.valueOf(eachMod.split("-")[0]);
                    String modName = eachMod.split("-")[1];

                    String ptmName = modificationMap.get(modName);

                    if (!allModifications.contains(ptmName)){
                        allModifications.add(ptmName);
                    }

                    utilitiesModifications.add(new ModificationMatch(ptmName, true, position));
                }
            }

            peptide = new Peptide(peptideSequence, utilitiesModifications);

            peptideAssumption = new PeptideAssumption(peptide, Integer.valueOf(rank), 0, new Charge(1, charge), (calMassToCharge - expMassToCharge));

            spectrumList = getSpectrumList(proteinAccesion, peptideSequence);

            for (String[] spectrumFileAndID : spectrumList){

                psmIndexList.add(String.valueOf(countAll));

                if (count == 0){
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileAndID[0], spectrumFileAndID[1]));

                String spectrumTitle;
                try {
                    spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileAndID[0], Integer.parseInt(spectrumFileAndID[1]) + 1);
                }catch (Exception e){
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(
                            null, "The spectrum title cannot match it in proBAM",
                            "Error Matching", JOptionPane.ERROR_MESSAGE);
                    break;
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

                count ++;
                countAll ++;

                if (count == 1000){
                    int[] counts = preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(psmIndexList);

                    count = 0;

                    if(countRound == 0){
                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound ++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                        countRound ++;
                    }

                    pdvMainClass.buttonCheck();

                    psmIndexList = new ArrayList<>();
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
    }

    /**
     * Parsing mzIdentML file
     */
    private void parseMzID(){

        String peptideRef;
        ArrayList<String[]> spectrumIDList;
        String[] fileAndID;
        HashMap<String, String[]> spectrumRefAndIDMap = new HashMap<>();
        HashMap<String, String> spectrumFileMap = new HashMap<>();

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

                fileAndID = new String[2];
                fileAndID[0] = spectrumFileMap.get(spectrumIdentificationResultType.getSpectraDataRef());

                String spectrumIndex;
                if(spectrumIdentificationResultType.getSpectrumID().contains(" ")){
                    String [] eachBig = spectrumIdentificationResultType.getSpectrumID().split(" ");
                    spectrumIndex = eachBig[eachBig.length-1].split("=")[1];
                }else {
                    spectrumIndex = spectrumIdentificationResultType.getSpectrumID().split("=")[1];
                }
                fileAndID[1] = spectrumIndex;
                for (SpectrumIdentificationItemType spectrumIdentificationItemType : spectrumIdentificationResultType.getSpectrumIdentificationItem()){
                    spectrumRefAndIDMap.put(spectrumIdentificationItemType.getId(), fileAndID);
                }
            }
        }
        
        for (ProteinAmbiguityGroupType proteinAmbiguityGroupType : mzIdentMLType.getDataCollection().getAnalysisData().getProteinDetectionList().getProteinAmbiguityGroup()){
            for (ProteinDetectionHypothesisType proteinDetectionHypothesisType : proteinAmbiguityGroupType.getProteinDetectionHypothesis()){
                for (PeptideHypothesisType peptideHypothesisType : proteinDetectionHypothesisType.getPeptideHypothesis()){

                    peptideRef = peptideHypothesisType.getPeptideEvidenceRef();
                    spectrumIDList = new ArrayList<>();

                    for (SpectrumIdentificationItemRefType spectrumIdRef : peptideHypothesisType.getSpectrumIdentificationItemRef()){

                        spectrumIDList.add(spectrumRefAndIDMap.get(spectrumIdRef.getSpectrumIdentificationItemRef()));
                    }

                    proteinAndSpectrumMap.put(peptideRef, spectrumIDList);
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

        for (String protein : proteinAndSpectrumMap.keySet()){
            if (protein.contains(proteinRef) && protein.contains(peptideSequence)){

                spectrumList = proteinAndSpectrumMap.get(protein);

                break;
            }
        }

        return spectrumList;
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
                modificationMap.put(ptm.getCvTerm().getAccession(), ptm.getName());
            }

        }

        return modificationMap;
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

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
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;


/**
 * For inputting MSFragger tsm result
 */
public class FragePipeImport {

    /**
     * Spectrum files and file type hash
     */
    private HashMap<File, String> fileToType;
    /**
     * tsm file
     */
    private File resultFile;
    /**
     * Current file scan collection saving spectrum
     */
    private ScanCollectionDefault currentScan;
    /**
     * Current spectrum file name
     */
    private String currentFileName;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Available threads numbers
     */
    private Integer threads = Runtime.getRuntime().availableProcessors();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * All modifications name in result file
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Result file column index
     */
    private int spectrumIndex = -1, peptideSequenceIndex = -1, chargeIndex = -1, caculatedMZIndex = -1, observedMZIndex = -1, assignenModIndex = -1;
    /**
     * Index to name
     */
    private HashMap<Integer, String> indexToName = new HashMap<>();
    /**
     * All included file name in result file
     */
    private ArrayList<String> includedFileNameList = new ArrayList<>();
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;

    /**
     * Main constructor
     * @param fileToType File name to file type hash
     * @param resultFile Result file
     * @param progressDialog Progress dialog
     * @param pdvMainClass Parent class
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public FragePipeImport(HashMap<File, String> fileToType, File resultFile, ProgressDialogX progressDialog, PDVMainClass pdvMainClass) throws SQLException, ClassNotFoundException, IOException {

        this.fileToType = fileToType;
        this.resultFile = resultFile;
        this.progressDialog = progressDialog;
        this.pdvMainClass = pdvMainClass;

        String dbName = resultFile.getAbsolutePath() +".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        getParameters();

        ArrayList<String> fileNameInSelection = new ArrayList<>();
        for (File file : fileToType.keySet()){
            fileNameInSelection.add(file.getName().substring(0, file.getName().lastIndexOf(".")));
        }

        Boolean hasNoMatchFile = false;
        StringBuilder outPutError = new StringBuilder();
        for (String fileName : includedFileNameList){
            if (! fileNameInSelection.contains(fileName)){
                hasNoMatchFile = true;
                outPutError.append(fileName).append("\n");
            }
        }

        if (!hasNoMatchFile) {

            sqLiteConnection = new SQLiteConnection(dbName);
            sqLiteConnection.setScoreNum(indexToName.size());

            new Thread("DisplayThread") {
                @Override
                public void run() {
                    try {
                        parseResult();
                        pdvMainClass.searchButton.setEnabled(true);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                null, "Failed to parse MSfrage result, please check your file.",
                                "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                    }
                }
            }.start();
        } else {
            JOptionPane.showMessageDialog(
                    pdvMainClass, "Follow files can not find in your input path:\n" + outPutError + "Please close PDV and check it. ",
                    "No match spectrum file", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
        }
    }

    /**
     * Parsing result file and putting into DB
     * @throws IOException
     * @throws SQLException
     */
    private void parseResult() throws IOException, SQLException {

        Pattern pattern = Pattern.compile("-?[0-9]+.?[0-9]+");

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        PreparedStatement preparedStatement = null;

        String line;
        String[] lineSplit;

        String spectrumName;
        String fileNameInResult;
        String peptideSequence;
        Integer chargeValue;
        Integer scanNumInResult;
        Double caculatedMZ;
        Double observedMZ;
        Double massError;
        String assignedMod;

        Integer lineCount = 0;
        Integer count = 0;
        Integer countRound = 0;
        ArrayList<String> spectrumList = new ArrayList<>();

        SpectrumMatch spectrumMatch = null;
        Peptide peptide;
        PeptideAssumption peptideAssumption;
        MSnSpectrum currentSpectrum;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?,?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();

        int countFirst = 0;
        for (Integer index : indexToName.keySet()){
            countFirst ++;
            addQuery.append(", ").append(indexToName.get(index)).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(indexToName.get(index), 7+countFirst);
        }
        addValuesQuery.append(")");

        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, Spectrum Object" + addQuery + ", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "An error occurred while creating table SpectrumMatch in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch " + addValuesQuery;

        for (File eachSpectrumFile : fileToType.keySet()){

            currentFileName = eachSpectrumFile.getName();
            String shortFileName = currentFileName.substring(0, currentFileName.lastIndexOf("."));

            if (includedFileNameList.contains(shortFileName)) {
                currentScan = getCurrentScan(eachSpectrumFile, fileToType.get(eachSpectrumFile));
            }

            BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile));
            bufferedReader.readLine();

            while ((line = bufferedReader.readLine()) != null){

                lineSplit = line.split("\t");

                spectrumName = lineSplit[spectrumIndex];

                fileNameInResult = spectrumName.split("\\.")[0];
                scanNumInResult = Integer.valueOf(spectrumName.split("\\.")[1]);

                if (shortFileName.equalsIgnoreCase(fileNameInResult)) {

                    chargeValue = Integer.valueOf(lineSplit[chargeIndex]);
                    peptideSequence = lineSplit[peptideSequenceIndex];
                    caculatedMZ = Double.valueOf(lineSplit[caculatedMZIndex]);
                    observedMZ = Double.valueOf(lineSplit[observedMZIndex]);
                    massError = caculatedMZ - observedMZ;
                    assignedMod = lineSplit[assignenModIndex];

                    if (count == 0){
                        preparedStatement = connection.prepareStatement(addDataIntoTable);
                    }

                    String modAA;
                    Integer position;
                    Double modMass;
                    String singleModificationName;
                    ArrayList<String> residues;
                    ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

                    if (assignedMod != null && !assignedMod.equals("")) {

                        for (String eachMod : assignedMod.split(",")) {

                            residues = new ArrayList<>();

                            modMass = Double.valueOf(eachMod.substring(eachMod.lastIndexOf("(") + 1, eachMod.lastIndexOf(")")));

                            if (eachMod.contains("n")) {
                                modAA = "N-term";
                                position = 1;

                            } else if (eachMod.contains("c")) {
                                modAA = "C-term";
                                position = peptideSequence.length();

                            } else {
                                modAA = eachMod.substring(eachMod.lastIndexOf("(") - 1, eachMod.lastIndexOf("("));

                                position = Integer.valueOf(eachMod.substring(0, eachMod.lastIndexOf("(") - 1).trim());
                            }

                            singleModificationName = modMass + " of " + modAA;

                            if (!ptmFactory.containsPTM(singleModificationName)) {
                                if (modAA.equalsIgnoreCase("n-term")) {
                                    residues.add(modAA);
                                    PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modMass, residues);
                                    ptm.setShortName(String.valueOf(modMass));
                                    ptmFactory.addUserPTM(ptm);
                                } else if (modAA.equalsIgnoreCase("c-term")) {
                                    residues.add(modAA);
                                    PTM ptm = new PTM(PTM.MODCP, singleModificationName, modMass, residues);
                                    ptm.setShortName(String.valueOf(modMass));
                                    ptmFactory.addUserPTM(ptm);
                                } else {
                                    residues.add(modAA);
                                    PTM ptm = new PTM(PTM.MODAA, singleModificationName, modMass, residues);
                                    ptm.setShortName(String.valueOf(modMass));
                                    ptmFactory.addUserPTM(ptm);
                                }
                            }

                            if (!allModifications.contains(singleModificationName)) {
                                allModifications.add(singleModificationName);
                            }

                            utilitiesModifications.add(new ModificationMatch(singleModificationName, true, position));
                        }
                    }

                    peptide = new Peptide(peptideSequence, utilitiesModifications);

                    peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, chargeValue), massError, "*");

                    spectrumMatch = new SpectrumMatch(spectrumName);

                    spectrumMatch.addHit(0, peptideAssumption, false);

                    spectrumMatch.setBestPeptideAssumption(peptideAssumption);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        try {
                            oos.writeObject(spectrumMatch);
                        } finally {
                            oos.close();
                        }
                    } finally {
                        bos.close();
                    }

                    currentSpectrum = getSpectrum(scanNumInResult, chargeValue);

                    ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(bos1);
                        try {
                            oos.writeObject(currentSpectrum);
                        } finally {
                            oos.close();
                        }
                    } finally {
                        bos1.close();
                    }

                    spectrumList.add(String.valueOf(lineCount));

                    preparedStatement.setInt(1, lineCount);
                    preparedStatement.setDouble(2, caculatedMZ);
                    preparedStatement.setString(3, spectrumName);
                    preparedStatement.setString(4, peptideSequence);
                    preparedStatement.setDouble(5, massError);
                    preparedStatement.setBytes(6, bos.toByteArray());
                    preparedStatement.setBytes(7, bos1.toByteArray());

                    for (Integer index : indexToName.keySet()){
                        String name = indexToName.get(index);
                        String value;
                        if (index == lineSplit.length){
                            value = "";
                        } else {
                            value = lineSplit[index];
                        }
                        if (pattern.matcher(value).matches()) {
                            preparedStatement.setDouble(nameToDBIndex.get(name), Double.parseDouble(value));
                        } else {
                            preparedStatement.setString(nameToDBIndex.get(name), value);
                        }
                    }

                    preparedStatement.addBatch();

                    count ++;

                    if(count == 1000){
                        int[] counts = preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        pdvMainClass.allSpectrumIndex.add(spectrumList);

                        count = 0;

                        if(countRound == 0){
                            pdvMainClass.displayFrage();
                            pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                            progressDialog.setRunFinished();

                            countRound ++;
                        } else {
                            pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                            countRound ++;
                        }

                        spectrumList = new ArrayList<>();
                    }
                    lineCount ++;
                }

            }bufferedReader.close();

            currentScan.reset();
        }
        if(count != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if(countRound == 0){
                pdvMainClass.displayFrage();
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

    /**
     * Get current spectrum file scan collection
     * @param spectrumFile Spectrum file
     * @param spectrumFileType Spectrum file type
     * @return Scan collection
     */
    private ScanCollectionDefault getCurrentScan(File spectrumFile, String spectrumFileType){

        if (spectrumFileType.equals("mzml")){
            MZMLFile mzmlFile = new MZMLFile(spectrumFile.getAbsolutePath());

            ScanCollectionDefault scans = new ScanCollectionDefault();

            scans.setDefaultStorageStrategy(StorageStrategy.SOFT);

            scans.isAutoloadSpectra(true);

            scans.setDataSource(mzmlFile);

            mzmlFile.setNumThreadsForParsing(threads);

            try {
                scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA);
            } catch (FileParsingException e) {
                JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                progressDialog.setRunFinished();
                e.printStackTrace();
            }

            return scans;

        } else {

            MzXMLScanImport mzXMLScanImport = new MzXMLScanImport(spectrumFile.getAbsolutePath());

            return mzXMLScanImport.getScans();
        }
    }

    /**
     * Get spectrum object
     * @param scanNum Scan number
     * @param chargeValue Charge value
     * @return Msn Spectrum
     */
    private MSnSpectrum getSpectrum(Integer scanNum, Integer chargeValue){

        IScan iScan = currentScan.getScanByNum(scanNum);
        ISpectrum spectrum = iScan.getSpectrum();

        Charge charge = new Charge(1, chargeValue);
        ArrayList<Charge> charges = new ArrayList<>();
        charges.add(charge);

        Double precursorInt;

        if (iScan.getPrecursor().getIntensity() != null){
            precursorInt = iScan.getPrecursor().getIntensity();
        } else {
            precursorInt = 0.0;
        }

        Precursor precursor = new Precursor(currentScan.getScanByNum(iScan.getPrecursor().getParentScanNum()).getRt(), iScan.getPrecursor().getMzTarget(),
                precursorInt, charges);

        double[] mzs = spectrum.getMZs();
        double[] ins = spectrum.getIntensities();
        HashMap<Double, Peak> peakMap = new HashMap<>();
        for(int i = 0; i<mzs.length; i++){
            Peak peak = new Peak(mzs[i], ins[i]);
            peakMap.put(mzs[i], peak);
        }

        return new MSnSpectrum(2, precursor, String.valueOf(scanNum), peakMap, currentFileName);
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Get all parameters
     * @throws IOException
     */
    private void getParameters() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile));

        String[] headers = bufferedReader.readLine().trim().split("\t");

        String line;
        String[] lineSplit;

        String spectrumName;
        String fileNameInResult;

        for (int i = 0; i < headers.length; i++){

            String header = headers[i];

            if (header.equalsIgnoreCase("Spectrum")) {
                spectrumIndex = i;
            } else if (header.equalsIgnoreCase("Peptide")) {
                peptideSequenceIndex = i;
            } else if (header.equalsIgnoreCase("Charge")) {
                chargeIndex = i;
            } else if (header.equalsIgnoreCase("Calculated M/Z")) {
                caculatedMZIndex = i;
            } else if (header.equalsIgnoreCase("Observed M/Z")) {
                observedMZIndex = i;
            } else if (header.equalsIgnoreCase("Assigned Modifications")) {
                assignenModIndex = i;
            } else {
                indexToName.put(i, header.trim().replace(" ", ""));
            }
        }

        while ((line = bufferedReader.readLine()) != null) {

            lineSplit = line.split("\t");

            spectrumName = lineSplit[spectrumIndex];

            fileNameInResult = spectrumName.split("\\.")[0];

            if (!includedFileNameList.contains(fileNameInResult)){
                includedFileNameList.add(fileNameInResult);
            }
        }

        bufferedReader.close();

    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getScoreName(){

        ArrayList<String> scoreName = new ArrayList<>();

        for (Integer index : indexToName.keySet()){
            scoreName.add(indexToName.get(index));
        }
        return scoreName;
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }

}

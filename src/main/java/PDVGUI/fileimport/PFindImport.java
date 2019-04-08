package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * pFind results importer
 */
public class PFindImport {

    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * psm results file
     */
    private File resultsFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Parameter file
     */
    private File parFile;
    /**
     * Modification file
     */
    private File modFile;
    /**
     * Index to name
     */
    private HashMap<Integer, String> indexToName = new HashMap<>();
    /**
     * Spectrum title index
     */
    private int titleIndex = -1;
    /**
     * Scan number index
     */
    private int scanNoIndex = -1;
    /**
     * Charge index
     */
    private int chargeIndex = -1;
    /**
     * Peptide sequence index
     */
    private int sequenceIndex = -1;
    /**
     * Modification index
     */
    private int modIndex = -1;
    /**
     * Exp mh + index
     */
    private int expMHIndex = -1;
    /**
     * Mass error index
     */
    private int massErrorIndex = -1;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Name to DB index
     */
    private HashMap<String, Integer> nameToDBIndex = new HashMap<>();

    /**
     * Main constructor
     * @param pdvMainClass Parent class
     * @param confFile Configure file
     * @param spectrumFile Spectrum file
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public PFindImport(PDVMainClass pdvMainClass, File confFile, File spectrumFile, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException, IOException {

        this.pdvMainClass = pdvMainClass;
        this.progressDialog = progressDialog;
        this.spectrumFile = spectrumFile;
        this.resultsFile = new File(confFile.getParentFile().getAbsolutePath() + "/result/pFind.spectra");
        this.modFile = new File(confFile.getParentFile().getAbsolutePath() + "/result/1.mod");
        this.parFile = new File(confFile.getParentFile().getAbsolutePath() + "/param/pFind.cfg");

        String dbName = confFile.getParentFile().getAbsolutePath()+"/"+ confFile.getName()+".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        parseModFile();
        getParameters();

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(1 + indexToName.size());

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {

                    parseResultFile();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (IOException | SQLException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * Parsing pFind .spectral file
     * @throws IOException
     * @throws SQLException
     */
    private void parseResultFile() throws IOException, SQLException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?,?");

        int countFirst = 0;
        for (Integer index : indexToName.keySet()){
            countFirst ++;
            addQuery.append(", ").append(indexToName.get(index)).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(indexToName.get(index), 7+countFirst);
        }

        addValuesQuery.append(")");

        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, Modification varchar(200)" + addQuery +", PRIMARY KEY(PSMIndex))";

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

        String addDataIntoTable = "INSERT INTO SpectrumMatch "+addValuesQuery;
        PreparedStatement preparedStatement = null;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(resultsFile));

        int lineCount = 0;
        int count = 0;
        int countRound = 0;
        ArrayList<String> spectrumList = new ArrayList<>();
        String spectrumTitle;
        String sequence;
        String modification;
        String scanNo;
        int charge;
        double expMH;
        double massError;
        ArrayList<ModificationMatch> utilitiesModifications;
        Peptide peptide;
        SpectrumMatch currentMatch;
        PeptideAssumption peptideAssumption;
        ByteArrayOutputStream bos;
        String eachLine;
        String[] splitLine;

        while ((eachLine = bufferedReader.readLine()) != null){
            splitLine = eachLine.split("\t");
            if (lineCount != 0){

                if (count == 0){
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                utilitiesModifications = new ArrayList<>();
                spectrumTitle = splitLine[titleIndex];
                scanNo = splitLine[scanNoIndex];
                sequence = splitLine[sequenceIndex];
                charge = Integer.parseInt(splitLine[chargeIndex]);
                modification = splitLine[modIndex];
                expMH = Double.parseDouble(splitLine[expMHIndex]);
                massError = Double.parseDouble(splitLine[massErrorIndex]);
                if (!modification.equals("")){
                    for (String eachMod : modification.split(";")){
                        int pos = Integer.parseInt(eachMod.split(",")[0]);
                        String modName = eachMod.split(",")[1];
                        if (pos == 0){
                            pos = 1; //For N-term mod
                        } else if (pos > sequence.length()){
                            pos = sequence.length(); // For C-term mod
                        }

                        String ptmName;
                        String aa;
                        if (modName.contains("][")){
                            ptmName = modName.split("]\\[")[0] + "]";
                            aa = modName.split("]\\[")[1].replace("]", "");
                        } else {
                            ptmName = modName.split("\\[")[0];
                            aa = modName.split("\\[")[1].split("]")[0];
                        }

                        String singleModificationName = ptmName.replace(">","&gt;") + " of " + aa;

                        utilitiesModifications.add(new ModificationMatch(singleModificationName, true, pos));

                        if (!allModifications.contains(singleModificationName)){
                            allModifications.add(singleModificationName);
                        }
                    }
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFile.getName(), spectrumTitle));
                peptide = new Peptide(sequence, utilitiesModifications);
                peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge), 0, "*");
                currentMatch.addHit(0, peptideAssumption, false);
                currentMatch.setBestPeptideAssumption(peptideAssumption);

                spectrumList.add(String.valueOf(lineCount));

                bos = new ByteArrayOutputStream();
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

                preparedStatement.setInt(1, lineCount);
                preparedStatement.setDouble(2, expMH);
                preparedStatement.setString(3, spectrumTitle);
                preparedStatement.setString(4, sequence);
                preparedStatement.setDouble(5, massError);
                preparedStatement.setBytes(6, bos.toByteArray());
                preparedStatement.setString(7, modification);

                for (Integer index : indexToName.keySet()){
                    String name = indexToName.get(index);
                    String value = splitLine[index];
                    preparedStatement.setString(nameToDBIndex.get(name), value);
                }

                preparedStatement.addBatch();
                count ++;

                if(count == 1000){

                    int[] counts = preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(spectrumList);

                    count = 0;

                    pdvMainClass.updatePTMSetting();

                    if(countRound == 0){
                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound ++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(pdvMainClass.selectedPageNum + "/" + pdvMainClass.allSpectrumIndex.size());
                        countRound ++;
                    }

                    spectrumList = new ArrayList<>();
                }

            }
            lineCount ++;
        }bufferedReader.close();

        if(count != 0){

            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.updatePTMSetting();
            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if(countRound == 0){

                pdvMainClass.displayResult();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();

            } else {
                pdvMainClass.pageNumJTextField.setText(pdvMainClass.selectedPageNum + "/" + pdvMainClass.allSpectrumIndex.size());
            }
        }

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    /**
     * Get modification information
     * @throws IOException
     */
    private void parseModFile() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(modFile));

        ptmFactory.clearFactory();

        String eachLine;
        String modName;
        String singleModificationName;
        double modMass;
        String[] splitLine;
        ArrayList<String> residues;
        while ((eachLine = bufferedReader.readLine()) != null){
            splitLine = eachLine.split("\t");
            if (splitLine.length > 2){
                modName = splitLine[0].split("#")[0];
                modMass = Double.parseDouble(splitLine[2]);

                if (!ptmFactory.containsPTM(modName)){
                    residues = new ArrayList<>();
                    String ptmName;
                    String aa;
                    if (modName.contains("][")){
                        ptmName = modName.split("]\\[")[0] + "]";
                        aa = modName.split("]\\[")[1].replace("]", "");
                    } else {
                        ptmName = modName.split("\\[")[0];
                        aa = modName.split("\\[")[1].split("]")[0];
                    }

                    singleModificationName = ptmName + " of " + aa;

                    residues.add(aa);

                    if(ptmName.contains(">")){
                        ptmName = ptmName.replace(">","&gt;");
                    }

                    if (aa.contains("ProteinN-term")){
                        if (aa.equals("ProteinN-term")){
                            PTM ptm = new PTM(PTM.MODN, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            PTM ptm = new PTM(PTM.MODNAA, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        }
                    } else if (aa.contains("AnyN-term")){
                        if (aa.equals("AnyN-term")){
                            PTM ptm = new PTM(PTM.MODNP, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        }
                    } else if (aa.contains("ProteinC-term")){
                        if (aa.equals("ProteinC-term")){
                            PTM ptm = new PTM(PTM.MODC, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            PTM ptm = new PTM(PTM.MODCAA, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        }
                    } else if (aa.contains("AnyC-term")){
                        if (aa.equals("AnyC-term")){
                            PTM ptm = new PTM(PTM.MODCP, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            PTM ptm = new PTM(PTM.MODCPAA, singleModificationName, modMass, residues);
                            ptm.setShortName(ptmName);
                            ptmFactory.addUserPTM(ptm);
                        }
                    } else {
                        PTM ptm = new PTM(PTM.MODAA, singleModificationName, modMass, residues);
                        if (aa.equals("T") || aa.equals("S")){
                            if (modMass < 80.01 && modMass > 79.9){
                                ptm.addNeutralLoss(NeutralLoss.H3PO4);
                            }
                        }

                        ptm.setShortName(ptmName);
                        ptmFactory.addUserPTM(ptm);
                    }

                }

            }
        }
    }

    /**
     * Get other information
     * @throws IOException
     */
    private void getParameters() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(resultsFile));

        int count = 0;
        String eachLine;
        String[] splitLine;

        while ((eachLine = bufferedReader.readLine()) != null){
            splitLine = eachLine.split("\t");
            if (count == 0){
                for (int index = 0; index < splitLine.length; index ++){
                    switch (splitLine[index]){
                        case "File_Name":
                            titleIndex = index;
                            break;
                        case "Scan_No":
                            scanNoIndex = index;
                            break;
                        case "Charge":
                            chargeIndex = index;
                            break;
                        case "Modification":
                            modIndex = index;
                            break;
                        case "Sequence":
                            sequenceIndex = index;
                            break;
                        case "Exp.MH+":
                            expMHIndex = index;
                            break;
                        case "Mass_Shift(Exp.-Calc.)":
                            massErrorIndex = index;
                            break;
                        default:
                            indexToName.put(index, splitLine[index].replace(".", "_").replace("-", "_").
                                    replace("+", "").replace("/", "_"));
                            break;

                    }
                }
            } else{
                break;
            }
            count ++;
        }

        //BufferedReader bufferedReader1 = new BufferedReader(new FileReader(parFile));
        // Add later
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

        ArrayList<String> scoreName = new ArrayList<>();

        scoreName.add("Modification");

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

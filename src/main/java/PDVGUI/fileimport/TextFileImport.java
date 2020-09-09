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
 * Import new soft .txt results
 * Created by Ken on 10/23/2017.
 */
public class TextFileImport {

    /**
     * Text Id file
     */
    private File textIdFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Spectrum title to rank
     */
    private HashMap<String, Integer> spectrumTitleToRank = new HashMap<>();
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Spectrum title column index
     */
    private Integer spectrumTitleIndex = 0;
    /**
     * Sequence column index
     */
    private Integer sequenceIndex = 0;
    /**
     * Modification column index
     */
    private Integer modificationIndex = 0;
    /**
     * Charge column index
     */
    private Integer chargeIndex = 0;
    /**
     * MZ column index
     */
    private Integer mzIndex = -1;
    /**
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Check file format
     */
    private Boolean isNewSoft = false;
    /**
     * Index to name
     */
    private HashMap<Integer, String> indexToName = new HashMap<>();
    /**
     * Name to DB index
     */
    private HashMap<String, Integer> nameToDBIndex = new HashMap<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param textIdFile Text identification file
     * @param spectrumFile Spectrum file
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public TextFileImport(PDVMainClass pdvMainClass, File textIdFile, File spectrumFile, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException, IOException {

        this.pdvMainClass = pdvMainClass;
        this.textIdFile = textIdFile;
        this.spectrumFile = spectrumFile;
        this.progressDialog = progressDialog;

        String dbName = textIdFile.getParentFile().getAbsolutePath()+"/"+ textIdFile.getName()+".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);

        getParameters();

        sqLiteConnection.setScoreNum(1 + indexToName.size());

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    if (!isNewSoft){
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                                "No support file format, please check your file."),
                                "File Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        parseTextFile();
                    }

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (IOException | SQLException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing text file
     * @throws IOException
     * @throws SQLException
     */
    private void parseTextFile() throws IOException, SQLException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?,?");

        int countFirst = 0;
        for (Integer index : indexToName.keySet()){
            if (!indexToName.get(index).equalsIgnoreCase("mz")){
                countFirst ++;
                addQuery.append(", ").append(indexToName.get(index)).append(" OBJECT(50)");
                addValuesQuery.append(",?");
                nameToDBIndex.put(indexToName.get(index), 7+countFirst);
            }
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

        BufferedReader bufferedReader = new BufferedReader(new FileReader(textIdFile));

        String line;
        String[] values;
        String spectrumTitle;
        String modificationNames;
        String singleModificationName;
        String sequence;
        Integer modificationSite;
        Double modificationMass;
        Double pepMz;
        Integer peptideCharge;
        String rankString;
        Peptide peptide;

        ArrayList<String> spectrumList = new ArrayList<>();
        ArrayList<ModificationMatch> utilitiesModifications;
        SpectrumMatch currentMatch;
        PeptideAssumption peptideAssumption;

        ByteArrayOutputStream bos;

        int lineCount = 0;
        int count = 0;
        int countRound = 0;

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (lineCount == 0) {

            } else if(isNewSoft){

                if (count == 0){
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                utilitiesModifications = new ArrayList<>();

                spectrumTitle = values[spectrumTitleIndex];
                sequence = values[sequenceIndex];
                modificationNames = values[modificationIndex];
                peptideCharge = Integer.valueOf(values[chargeIndex]);

                ArrayList<String> residues;

                if(!modificationNames.equals("-")){
                    for (String singleModification: modificationNames.split(";")){
                        singleModificationName = singleModification.split("@")[0];
                        modificationSite = Integer.valueOf(singleModification.split("@")[1].split("\\[")[0]);
                        modificationMass = Double.valueOf(singleModification.split("@")[1].split("\\[")[1].replace("]", ""));

                        if (!ptmFactory.containsPTM(singleModificationName)){
                            String modificationName = singleModificationName.split(" of ")[0];
                            residues = new ArrayList<>();
                            residues.add(singleModificationName.split(" of ")[1]);

                            if (singleModificationName.toLowerCase().contains("n-term")){
                                PTM ptm = new PTM(PTM.MODNP, singleModificationName, modificationMass, residues);
                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            } else if (singleModificationName.toLowerCase().contains("c-term")){
                                PTM ptm = new PTM(PTM.MODCP, singleModificationName, modificationMass, residues);
                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            } else {
                                PTM ptm = new PTM(PTM.MODAA, singleModificationName, modificationMass, residues);

                                if (singleModificationName.split(" of ")[1].equals("T") || singleModificationName.split(" of ")[1].equals("S")){
                                    if (modificationMass < 80.01 && modificationMass > 79.9){
                                        ptm.addNeutralLoss(NeutralLoss.H3PO4);
                                    }
                                }

                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            }
                        }

                        if (singleModificationName.toLowerCase().contains("n-term")){
                            modificationSite = 1;
                        } else if (singleModificationName.toLowerCase().contains("c-term")){
                            modificationSite = sequence.length();
                        }

                        if (!allModifications.contains(singleModificationName)){
                            allModifications.add(singleModificationName);
                        }

                        utilitiesModifications.add(new ModificationMatch(singleModificationName, true, modificationSite));
                    }
                }

                if(spectrumTitleToRank.containsKey(spectrumTitle)){
                    int rank = spectrumTitleToRank.get(spectrumTitle) + 1;
                    spectrumTitleToRank.put(spectrumTitle, rank);
                    rankString = String.valueOf(rank);

                } else{

                    spectrumTitleToRank.put(spectrumTitle, 1);
                    rankString = "1";
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFile.getName(), spectrumTitle+"_rank_"+rankString));

                peptide = new Peptide(sequence, utilitiesModifications);

                peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, peptideCharge), 0, "*");

                if (mzIndex != -1){
                    pepMz = Double.valueOf(values[mzIndex]);
                } else {
                    pepMz = peptideAssumption.getTheoreticMz();
                }

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
                preparedStatement.setDouble(2, pepMz);
                preparedStatement.setString(3, spectrumTitle);
                preparedStatement.setString(4, sequence);
                preparedStatement.setDouble(5, -1);
                preparedStatement.setBytes(6, bos.toByteArray());
                preparedStatement.setString(7, modificationNames);

                for (Integer index : indexToName.keySet()){
                    String name = indexToName.get(index);
                    String value = values[index];
                    if (!name.equalsIgnoreCase("mz")) {
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

                    pdvMainClass.updatePTMSetting();

                    if(countRound == 0){
                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound ++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                        countRound ++;
                    }

                    spectrumList = new ArrayList<>();
                }
            }
            lineCount ++;
        }bufferedReader.close();

        if(count != 0 && isNewSoft){

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
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }
        }

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    /**
     * Get all parameters
     * @throws IOException
     */
    private void getParameters() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(textIdFile));

        int lineCount = 0;
        String line;
        String[] values;

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (lineCount == 0) {
                for (int index = 0; index < values.length; index++) {
                    switch (values[index]) {
                        case "spectrum_title":
                            isNewSoft = true;
                            spectrumTitleIndex = index;
                            break;
                        case "peptide":
                            sequenceIndex = index;
                            break;
                        case "modification":
                            modificationIndex = index;
                            break;
                        case "charge":
                            chargeIndex = index;
                            break;
                        case "mz":
                            mzIndex = index;
                            break;
                            //Do nothing
                        default:
                            indexToName.put(index, values[index].replaceAll("[\\\\/:*?\"<>|]", ""));
                            break;
                    }
                }
            } else {
                break;
            }
            lineCount ++;
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

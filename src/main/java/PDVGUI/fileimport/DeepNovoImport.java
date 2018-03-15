package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Import deep novo results
 * Created by Ken on 11/23/2017.
 */
public class DeepNovoImport {

    /**
     * Result file
     */
    private File resultFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Scan to title map
     */
    private HashMap<String, String> scanToTitle = new HashMap<>();
    /**
     * Index to modification name
     */
    private HashMap<String, String> indexToModName = new HashMap<>();
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param resultFile Result file
     * @param spectrumFile Spectrum file
     * @param spectrumFactory Spectrum factory
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public DeepNovoImport(PDVMainClass pdvMainClass, File resultFile, File spectrumFile, SpectrumFactory spectrumFactory, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {

        this.pdvMainClass = pdvMainClass;
        this.resultFile = resultFile;
        this.spectrumFile = spectrumFile;
        this.spectrumFactory = spectrumFactory;
        this.progressDialog = progressDialog;

        String dbName = resultFile.getParentFile().getAbsolutePath()+"/"+ resultFile.getName()+".db";

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(1);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseSpectrum();

                    importResults();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * Import results from file
     * @throws SQLException
     */
    private void importResults() throws SQLException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        String addTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, PositionScore Char, PRIMARY KEY(PSMIndex))";
        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?)";

        PreparedStatement preparedStatement = null;

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile));

            String line;
            int psmIndex = 0;
            int count = 0;
            int countRound = 0;
            int scanIndex = 0;
            int seqIndex = 0;
            int scoreIndex = 0;
            int positionScoreIndex = 0;

            String spectrumScan;
            String sequence;
            StringBuilder peptideSeq;
            String spectrumTitle;
            Double score;
            Double massError;
            Integer charge;

            Peptide peptide;
            Precursor precursor;
            PeptideAssumption peptideAssumption;
            SpectrumMatch newSpectrumMatch;

            ArrayList<String> spectrumList = new ArrayList<>();
            ArrayList<ModificationMatch> modificationMatches;

            while ((line = bufferedReader.readLine()) != null){

                String[] eachItemArray = line.split("\t");

                if (psmIndex == 0){

                    for (int index = 0; index < eachItemArray.length; index ++){
                        switch (eachItemArray[index]){
                            case "scan":
                                scanIndex = index;
                                break;
                            case "predicted_sequence":
                                seqIndex = index;
                                break;
                            case "predicted_score":
                                scoreIndex = index;
                                break;
                            case "predicted_position_score":
                                positionScoreIndex = index;
                                break;
                        }
                    }
                } else {

                    if (count == 0){
                        preparedStatement = connection.prepareStatement(addDataIntoTable);
                    }

                    peptideSeq = new StringBuilder();

                    spectrumList.add(String.valueOf(psmIndex));

                    spectrumScan = eachItemArray[scanIndex];
                    sequence = eachItemArray[seqIndex];
                    score = Double.valueOf(eachItemArray[scoreIndex]);

                    spectrumTitle = scanToTitle.get(spectrumScan);
                    precursor = spectrumFactory.getPrecursor(spectrumFile.getName(), spectrumTitle);
                    charge = precursor.getPossibleCharges().get(0).value;

                    modificationMatches = new ArrayList<>();
                    newSpectrumMatch = new SpectrumMatch();
                    newSpectrumMatch.setSpectrumNumber(psmIndex);
                    newSpectrumMatch.setKey(Spectrum.getSpectrumKey(spectrumFile.getName(), spectrumTitle));

                    String[] sequenceSplit = sequence.split(",");
                    for (int i = 0; i<sequenceSplit.length; i++){
                        if (sequenceSplit[i].contains("mod")){
                            peptideSeq.append(sequenceSplit[i].charAt(0));

                            if (!allModifications.contains(indexToModName.get(sequenceSplit[i]))){
                                allModifications.add(indexToModName.get(sequenceSplit[i]));
                            }

                            modificationMatches.add(new ModificationMatch(indexToModName.get(sequenceSplit[i]), true, i+1));
                        } else {
                            peptideSeq.append(sequenceSplit[i]);
                        }
                    }

                    peptide = new Peptide(peptideSeq.toString(), modificationMatches);

                    massError = peptide.getMass()/charge;

                    peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, charge), massError, null);

                    peptideAssumption.setScore(score);

                    newSpectrumMatch.setBestPeptideAssumption(peptideAssumption);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        try {
                            oos.writeObject(newSpectrumMatch);
                        } finally {
                            oos.close();
                        }
                    } finally {
                        bos.close();
                    }

                    preparedStatement.setInt(1, psmIndex);
                    preparedStatement.setDouble(2, 0.0);
                    preparedStatement.setString(3, spectrumTitle);
                    preparedStatement.setString(4, peptideSeq.toString());
                    preparedStatement.setDouble(5, 0.0);
                    preparedStatement.setBytes(6, bos.toByteArray());
                    preparedStatement.setString(7, eachItemArray[positionScoreIndex]);

                    preparedStatement.addBatch();

                    count ++;

                    if(count >= 1000){

                        int[] counts = preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        pdvMainClass.allSpectrumIndex.add(spectrumList);

                        count = 0;

                        if(countRound == 0){

                            pdvMainClass.displayDenovo();
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
                psmIndex ++;
            }

            if(count != 0){

                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumList);

                if(countRound == 0){

                    pdvMainClass.displayDenovo();
                    pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                    progressDialog.setRunFinished();

                } else {
                    pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                }

            } else {
                if (countRound == 0){
                    progressDialog.setRunFinished();
                    throw new IOException("No PSM found");
                }
            }
            pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
            pdvMainClass.loadingJButton.setText("Import done");
            pdvMainClass.searchButton.setToolTipText("Find items");
            pdvMainClass.searchItemTextField.setToolTipText("Find items");

        } catch (Exception e) {
           progressDialog.setRunFinished();
           e.printStackTrace();
        }
    }

    /**
     * Get spectrum
     * @throws IOException
     * @throws MzMLUnmarshallerException
     */
    private void parseSpectrum() throws IOException, MzMLUnmarshallerException {

        ArrayList<String> titles = spectrumFactory.getSpectrumTitles(spectrumFile.getName());
        for (String title : titles){
            scanToTitle.put(spectrumFactory.getSpectrum(spectrumFile.getName(), title).getScanNumber(), title);
        }

        indexToModName.put("Nmod", "Deamidation of N");
        indexToModName.put("Cmod", "Deamidation of Q");
        indexToModName.put("Qmod", "Carbamidomethyl of C");
        indexToModName.put("Mmod", "Oxidation of M");
    }

    /**
     * Return database connection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }
}

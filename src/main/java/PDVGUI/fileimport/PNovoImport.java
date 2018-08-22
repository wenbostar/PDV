package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.JOptionEditorPane;
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
 * Import new version pNovo result
 */
public class PNovoImport {

    /**
     * Parameter file
     */
    private File paraFile;
    /**
     * Result file
     */
    private File resultFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Name to modification
     */
    private HashMap<String, String> nameToModification = new HashMap<>();
    /**
     * Term modification
     */
    private ArrayList<String> termModification = new ArrayList<>();
    /**
     * Name to amino acid
     */
    private HashMap<String, String> nameToAA = new HashMap<>();
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();

    /**
     * Main constructor
     * @param pdvMainClass Parent class
     * @param paraFile Parameter file
     * @param resultFile Result file
     * @param spectrumFile Spectrum file
     * @param spectrumFactory Spectrum factory
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public PNovoImport(PDVMainClass pdvMainClass, File paraFile, File resultFile, File spectrumFile,
                       SpectrumFactory spectrumFactory, ProgressDialogX progressDialog)
            throws SQLException, ClassNotFoundException {

        this.pdvMainClass = pdvMainClass;
        this.paraFile = paraFile;
        this.resultFile = resultFile;
        this.spectrumFile = spectrumFile;
        this.spectrumFactory = spectrumFactory;
        this.progressDialog =progressDialog;

        String dbName = resultFile.getAbsolutePath()+".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(2);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parsePara();

                    parseResultFile();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse pNovo result, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                    currentThread().interrupt();
                }
            }
        }.start();
    }

    /**
     * Parse parameter file
     * @throws IOException
     */
    private void parsePara() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(paraFile));

        String line;
        String[] lineElements;
        String modAA;
        String modName;
        String modIndex;
        String modification;
        Double modMass;

        while ((line = bufferedReader.readLine()) != null){

            if (line.contains("#")){

                if (line.contains("FixMod")){

                    lineElements = line.split("\t");

                    modification = lineElements[1].split("\\[")[0];

                    if (lineElements[1].split("\\[")[1].contains("C-term")){
                        modAA = "C-term";
                    } else if (lineElements[1].split("\\[")[1].contains("N-term")){
                        modAA = "N-term";
                    } else {
                        modAA = lineElements[2];
                    }

                    line = bufferedReader.readLine();
                    modMass = Double.valueOf(line.split("=")[1].split(" ")[0]);
                    modIndex = line.split("=")[0];

                    modName = modification + " of " + modAA;

                    if (!ptmFactory.containsPTM(modName)){
                        PTM ptm = new PTM(PTM.MODNP, modName, modMass, null);
                        ptm.setShortName(modification);
                        ptmFactory.addUserPTM(ptm);
                    }

                    if (modAA.contains("term")){
                        termModification.add(modIndex);
                    }

                    if (!allModifications.contains(modName)) {
                        allModifications.add(modName);
                    }

                    nameToModification.put(modIndex, modName);
                    nameToAA.put(modIndex, modAA);

                } else if (line.contains("VarMod")){

                    lineElements = line.split("\t");

                    modification = lineElements[1].split("\\[")[0];

                    if (lineElements[1].split("\\[")[1].contains("C-term")){
                        modAA = "C-term";
                    } else if (lineElements[1].split("\\[")[1].contains("N-term")){
                        modAA = "N-term";
                    } else {
                        modAA = lineElements[2];
                    }

                    line = bufferedReader.readLine();
                    modMass = Double.valueOf(line.split("=")[1].split(" ")[0]);
                    modIndex = line.split("=")[0];

                    modName = modification + " of " + modAA;

                    if (!ptmFactory.containsPTM(modName)){
                        PTM ptm = new PTM(PTM.MODNP, modName, modMass, null);
                        ptm.setShortName(modification);
                        ptmFactory.addUserPTM(ptm);
                    }

                    if (modAA.contains("term")){
                        termModification.add(modIndex);
                    }

                    if (!allModifications.contains(modName)) {
                        allModifications.add(modName);
                    }

                    nameToModification.put(modIndex, modName);
                    nameToAA.put(modIndex, modAA);
                }

            } else {

            }
        } bufferedReader.close();
    }

    /**
     * Parse result file
     * @throws IOException
     * @throws SQLException
     * @throws MzMLUnmarshallerException
     */
    private void parseResultFile() throws IOException, SQLException, MzMLUnmarshallerException {

        Connection connection = sqLiteConnection.getConnection();

        Statement statement = connection.createStatement();
        connection.setAutoCommit(false);
        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, Score DOUBLE, AminoAcidsScore Char, PRIMARY KEY(PSMIndex))";

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

        String addDataIntoTable = "INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = null;

        BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile));

        String line;
        String[] lineElements;
        Integer lineCount = 0;
        Integer count = 0;
        Integer countRound = 0;

        String spectrumTitle;
        String modSequence;
        String sequence;
        String aaScores;
        String score;
        String massError;
        Integer modPosition;
        String modificationName;

        SpectrumMatch spectrumMatch;
        Peptide peptide;
        PeptideAssumption peptideAssumption;
        ByteArrayOutputStream bos;

        ArrayList<String> spectrumList = new ArrayList<>();
        ArrayList<String> containTerms;
        ArrayList<ModificationMatch> utilitiesModifications;

        while ((line = bufferedReader.readLine()) != null){

            if (count == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            lineElements = line.split("\t");

            spectrumTitle = lineElements[0];
            modSequence = lineElements[1];
            score = lineElements[4];
            aaScores = lineElements[5];
            massError = lineElements[8];

            containTerms = new ArrayList<>();
            utilitiesModifications = new ArrayList<>();

            for (String termIndex : termModification){
                if (modSequence.contains(termIndex)){
                    containTerms.add(termIndex);

                    modSequence = modSequence.replace(termIndex, "");
                    modSequence = modSequence.replace(String.valueOf(modSequence.charAt(0)), String.valueOf(modSequence.charAt(0)).toUpperCase());
                }
            }

            for (String termIndex : containTerms){
                modificationName = nameToModification.get(termIndex);

                if (modificationName.contains("N-term")){
                    modPosition = 1;
                } else {
                    modPosition = modSequence.length();
                }

                utilitiesModifications.add(new ModificationMatch(modificationName, true, modPosition));
            }

            for (Integer position = 0; position < modSequence.length(); position ++){

                String aa = String.valueOf(modSequence.charAt(position));

                if (nameToModification.containsKey(aa)){
                    modPosition = position + 1;

                    modificationName = nameToModification.get(aa);

                    utilitiesModifications.add(new ModificationMatch(modificationName, true, modPosition));
                }
            }

            for (String modIndex : nameToModification.keySet()){

                modSequence = modSequence.replace(modIndex, nameToAA.get(modIndex));
            }

            sequence = modSequence.toUpperCase();

            spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFile.getName(), spectrumTitle));

            MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile.getName(), spectrumTitle);

            peptide = new Peptide(sequence, utilitiesModifications);

            peptideAssumption = new PeptideAssumption(peptide, 1, 0, spectrum.getPrecursor().getPossibleCharges().get(0), Double.valueOf(score), "*");

            spectrumMatch.addHit(0, peptideAssumption, false);
            spectrumMatch.setBestPeptideAssumption(peptideAssumption);

            spectrumList.add(String.valueOf(lineCount));

            bos = new ByteArrayOutputStream();
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

            preparedStatement.setInt(1, lineCount);
            preparedStatement.setDouble(2, spectrum.getPrecursor().getMz());
            preparedStatement.setString(4, sequence);
            preparedStatement.setDouble(5, Double.parseDouble(massError));
            preparedStatement.setBytes(6, bos.toByteArray());
            preparedStatement.setDouble(7, Double.parseDouble(score));
            preparedStatement.setString(8, aaScores);

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

            lineCount ++;

        }bufferedReader.close();

        if(count != 0){

            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.updatePTMSetting();
            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if(countRound == 0){

                pdvMainClass.displayDenovo();
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
     * Return SQLiteConnection
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

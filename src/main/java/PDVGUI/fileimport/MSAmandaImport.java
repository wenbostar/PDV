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

public class MSAmandaImport {

    /**
     * ID file
     */
    File csvFile;
    /**
     * Spectrum file type
     */
    private String spectrumType;
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
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum ID to spectrum number
     */
    private HashMap<String, Integer> spectrumIdAndNumber;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Main constructor
     * @param pdvMainClass Parent class
     * @param csvFile CSV file
     * @param spectrumType Spectrum file type
     * @param progressDialog Progress dialog
     * @param spectrumIdAndNumber Spectrum and number map
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public MSAmandaImport(PDVMainClass pdvMainClass, File csvFile, String spectrumType, ProgressDialogX progressDialog, HashMap<String, Integer> spectrumIdAndNumber) throws SQLException, ClassNotFoundException {

        this.pdvMainClass = pdvMainClass;
        this.csvFile = csvFile;
        this.spectrumType = spectrumType;
        this.progressDialog = progressDialog;
        this.spectrumIdAndNumber = spectrumIdAndNumber;

        String dbName = csvFile.getAbsolutePath() + ".db";

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
                    parseFile();
                    pdvMainClass.searchButton.setEnabled(true);
                } catch (IOException | SQLException e) {
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse MSAmanda result, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing result file
     * @throws IOException
     * @throws SQLException
     */
    private void parseFile() throws IOException, SQLException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, AmandaScore DOUBLE, WeightedProbability DOUBLE, PRIMARY KEY(PSMIndex))";

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

        String headerString;

        String version = bufferedReader.readLine();

        // skip the version number
        if (version.toLowerCase().startsWith("#version: ")) {
            headerString = bufferedReader.readLine();
        } else {
            headerString = version;
        }

        String[] headers = headerString.split("\t");

        int scanNumberIndex = -1, titleIndex = -1, sequenceIndex = -1, modificationsIndex = -1, proteinAccessionsIndex = -1,
                amandaScoreIndex = -1, rankIndex = -1, mzIndex = -1, chargeIndex = -1, rtIndex = -1, filenameIndex = -1,
                amandaWeightedProbabilityIndex = -1;

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];

            if (header.equalsIgnoreCase("Scan Number")) {
                scanNumberIndex = i;
            } else if (header.equalsIgnoreCase("Title")) {
                titleIndex = i;
            } else if (header.equalsIgnoreCase("Sequence")) {
                sequenceIndex = i;
            } else if (header.equalsIgnoreCase("Modifications")) {
                modificationsIndex = i;
            } else if (header.equalsIgnoreCase("Protein Accessions")) {
                proteinAccessionsIndex = i;
            } else if (header.equalsIgnoreCase("Amanda Score")) {
                amandaScoreIndex = i;
            } else if (header.equalsIgnoreCase("Weighted Probability")) {
                amandaWeightedProbabilityIndex = i;
            } else if (header.equalsIgnoreCase("Rank")) {
                rankIndex = i;
            } else if (header.equalsIgnoreCase("m/z")) {
                mzIndex = i;
            } else if (header.equalsIgnoreCase("Charge")) {
                chargeIndex = i;
            } else if (header.equalsIgnoreCase("RT")) {
                rtIndex = i;
            } else if (header.equalsIgnoreCase("Filename")) {
                filenameIndex = i;
            }
        }

        Integer lineCount = 0;
        Integer count = 0;
        Integer countRound = 0;

        String line;
        SpectrumMatch spectrumMatch = null;
        String orderString;
        Peptide peptide;
        PeptideAssumption peptideAssumption;
        ArrayList<String> spectrumList = new ArrayList<>();
        HashMap<String, Integer> spectrumTitleToRank = new HashMap<>();

        ByteArrayOutputStream bos;

        while ((line = bufferedReader.readLine()) != null){

            if (count == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            String[] lineArray = line.split("\t");

            String scanNumber = lineArray[scanNumberIndex];
            String spectrumTitle = lineArray[titleIndex];
            String peptideSequence = lineArray[sequenceIndex].toUpperCase();
            String modifications = lineArray[modificationsIndex].trim();
            String proteinAccessions = lineArray[proteinAccessionsIndex];
            Double score = Double.valueOf(lineArray[amandaScoreIndex]);
            Double probability = Double.valueOf(lineArray[amandaWeightedProbabilityIndex]);
            Integer rank = Integer.valueOf(lineArray[rankIndex]);
            Double mz = Double.valueOf(lineArray[mzIndex]);
            Integer charge = Integer.valueOf(lineArray[chargeIndex]);
            Double rt = Double.valueOf(lineArray[rtIndex]);
            String filename = lineArray[filenameIndex];

            ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

            Integer modSite;

            if (modifications != null && !modifications.equals("")) {

                String[] modificationArray = modifications.split(";");
                for (String modification : modificationArray) {
                    String site = modification.split("\\(")[0];
                    String details = modification.split("\\(")[1];
                    String modAA;

                    if (site.equalsIgnoreCase("n-term")) {
                        modAA = "N-term";
                        modSite = 1;
                    } else if (site.equalsIgnoreCase("c-term")) {
                        modAA = "C-term";
                        modSite = peptideSequence.length();
                    } else {
                        modAA = site.substring(0, 1);
                        modSite = Integer.valueOf(site.substring(1, site.length()));
                    }

                    String[] detailsArray = details.split("\\|");
                    String modName = detailsArray[0];
                    String singleModificationName = modName + " of " + modAA;
                    Double modMass = Double.valueOf(detailsArray[1]);
                    ArrayList<String> residues = new ArrayList<>();

                    if (!ptmFactory.containsPTM(singleModificationName)) {
                        if (site.equalsIgnoreCase("n-term")) {
                            residues.add(site);
                            PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modMass, residues);
                            ptm.setShortName(modName);
                            ptmFactory.addUserPTM(ptm);
                        } else if (site.equalsIgnoreCase("c-term")) {
                            residues.add(site);
                            PTM ptm = new PTM(PTM.MODCP, singleModificationName, modMass, residues);
                            ptm.setShortName(modName);
                            ptmFactory.addUserPTM(ptm);
                        } else {
                            residues.add(site);
                            PTM ptm = new PTM(PTM.MODAA, singleModificationName, modMass, residues);
                            ptm.setShortName(modName);
                            ptmFactory.addUserPTM(ptm);
                        }
                    }

                    if (!allModifications.contains(singleModificationName)) {
                        allModifications.add(singleModificationName);
                    }

                    utilitiesModifications.add(new ModificationMatch(singleModificationName, true, modSite));
                }
            }

            if (spectrumType.equals("mgf")){
                if(spectrumTitleToRank.containsKey(spectrumTitle)){
                    int order = spectrumTitleToRank.get(spectrumTitle) + 1;
                    spectrumTitleToRank.put(spectrumTitle, order);
                    orderString = String.valueOf(order);

                } else{
                    spectrumTitleToRank.put(spectrumTitle, 1);
                    orderString = "1";
                }

                spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(filename, spectrumTitle+"_rank_"+orderString));
                preparedStatement.setString(3, spectrumTitle);
            } else if (spectrumType.equals("mzml")){

                if(spectrumTitleToRank.containsKey(scanNumber)){
                    int order = spectrumTitleToRank.get(scanNumber) + 1;
                    spectrumTitleToRank.put(scanNumber, order);
                    orderString = String.valueOf(order);

                } else{
                    spectrumTitleToRank.put(scanNumber, 1);
                    orderString = "1";
                }

                String scanId = "controllerType=0 controllerNumber=1 scan=" + scanNumber;
                spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(filename, String.valueOf(spectrumIdAndNumber.get(scanId) - 1) + "_rank_"+orderString));
                preparedStatement.setString(3, scanNumber);

            } else {
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                        "The spectrum file format is not support."),
                        "Format Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("An error occurred while reading spectrum file.");
            }

            spectrumMatch.setSpectrumNumber(Integer.valueOf(scanNumber));

            peptide = new Peptide(peptideSequence, utilitiesModifications);

            peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge), -1, "*");
            peptideAssumption.setRawScore(score);

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
            preparedStatement.setDouble(2, mz);
            preparedStatement.setString(4, peptideSequence);
            preparedStatement.setDouble(5, -1);
            preparedStatement.setBytes(6, bos.toByteArray());
            preparedStatement.setDouble(7, score);
            preparedStatement.setDouble(8, probability);

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

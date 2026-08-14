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
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Import Sage (https://github.com/lazear/sage) results from results.sage.tsv
 * Created by Bo Wen on 08/13/2026.
 */
public class SageImport {

    /**
     * Proton mass used to turn Sage's neutral expmass into an observed m/z
     */
    private static final double PROTON_MASS = 1.00727646677;
    /**
     * Pattern extracting the scan number from an mzML nativeID, e.g. "controllerType=0 controllerNumber=1 scan=36182"
     */
    private static final Pattern SCAN_PATTERN = Pattern.compile("scan=(\\d+)");
    /**
     * Sage result file (results.sage.tsv)
     */
    private File resultFile;
    /**
     * Spectrum file type, mgf or mzml
     */
    private String spectrumFileType;
    /**
     * Spectrum file name to nativeID to scan number, only used for mzML
     */
    private HashMap<String, HashMap<String, Integer>> spectrumIdAndNumber;
    /**
     * Exclude the decoy hits (label = -1)
     */
    private Boolean targetsOnly;
    /**
     * Exclude the hits with spectrum_q > 0.01
     */
    private Boolean filterQValue;
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
     * All modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Column index to the database column name, every Sage column is kept
     */
    private LinkedHashMap<Integer, String> indexToName = new LinkedHashMap<>();
    /**
     * Normalized spectrum file name to the name of the loaded spectrum file
     */
    private HashMap<String, String> nameToSpectrumFile = new HashMap<>();
    /**
     * Indexes of the columns the import needs
     */
    private int peptideIndex = -1, fileNameIndex = -1, scanNrIndex = -1, chargeIndex = -1, expMassIndex = -1,
            calcMassIndex = -1, labelIndex = -1, spectrumQIndex = -1, hyperScoreIndex = -1;

    /**
     * Main constructor
     * @param pdvMainClass Parent class
     * @param resultFile Sage result file
     * @param spectrumsFileFactory Spectrum file factory
     * @param spectrumFileType Spectrum file type
     * @param spectrumIdAndNumber Spectrum file name to nativeID to scan number
     * @param targetsOnly Exclude decoy hits
     * @param filterQValue Exclude hits with spectrum_q > 0.01
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public SageImport(PDVMainClass pdvMainClass, File resultFile, Object spectrumsFileFactory, String spectrumFileType,
                      HashMap<String, HashMap<String, Integer>> spectrumIdAndNumber, Boolean targetsOnly,
                      Boolean filterQValue, ProgressDialogX progressDialog)
            throws SQLException, ClassNotFoundException, IOException {

        this.pdvMainClass = pdvMainClass;
        this.resultFile = resultFile;
        this.spectrumFileType = spectrumFileType;
        this.spectrumIdAndNumber = spectrumIdAndNumber;
        this.targetsOnly = targetsOnly;
        this.filterQValue = filterQValue;
        this.progressDialog = progressDialog;

        if (!spectrumFileType.equals("mgf") && !spectrumFileType.equals("mzml")) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "Sage only supports mgf and mzML spectrum files."),
                    "Format Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException("Unsupported spectrum file type for Sage: " + spectrumFileType);
        }

        getSpectrumFileNames(spectrumsFileFactory);

        getParameters();

        String dbName = resultFile.getAbsolutePath() + ".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);
        sqLiteConnection.setScoreNum(indexToName.size());
    }

    /**
     * Parsing the result file, called after the parent class has picked up the column names
     */
    public void importResults() {

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseFile();
                    if (!pdvMainClass.allSpectrumIndex.isEmpty()) {
                        pdvMainClass.searchButton.setEnabled(true);
                    }
                } catch (Exception e) {
                    // anything escaping here would leave the modal progress dialog up for good
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse Sage result, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Collect the names of the spectrum files that have been loaded, indexed by their normalized name so
     * that Sage's file name (which keeps the .gz suffix) can be matched against the decompressed file
     * @param spectrumsFileFactory Spectrum file factory
     */
    private void getSpectrumFileNames(Object spectrumsFileFactory) {

        ArrayList<String> fileNames = new ArrayList<>();

        if (spectrumFileType.equals("mgf")) {
            fileNames.addAll(((SpectrumFactory) spectrumsFileFactory).getMgfFileNames());
        } else {
            fileNames.addAll(((HashMap<String, ScanCollectionDefault>) spectrumsFileFactory).keySet());
        }

        for (String fileName : fileNames) {
            String normalized = normalizeFileName(fileName);
            nameToSpectrumFile.put(normalized, fileName);
            nameToSpectrumFile.put(removeExtension(normalized), fileName);
        }
    }

    /**
     * Lower case the file name and drop the .gz suffix
     * @param fileName File name
     * @return Normalized file name
     */
    static String normalizeFileName(String fileName) {
        String normalized = fileName.toLowerCase().replace('\\', '/');
        // Sage normally writes a bare file name, but a path-qualified one must still match
        if (normalized.contains("/")) {
            normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        if (normalized.endsWith(".gz")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return normalized;
    }

    /**
     * Remove the last extension of a file name
     * @param fileName File name
     * @return File name without its extension
     */
    static String removeExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            return fileName.substring(0, dotIndex);
        }
        return fileName;
    }

    /**
     * Find the loaded spectrum file matching the file name used in the result file
     * @param nameInResult File name in the result file
     * @return Name of the loaded spectrum file or null
     */
    private String getSpectrumFileName(String nameInResult) {

        String normalized = normalizeFileName(nameInResult);

        String fileName = nameToSpectrumFile.get(normalized);
        if (fileName == null) {
            fileName = nameToSpectrumFile.get(removeExtension(normalized));
        }
        return fileName;
    }

    /**
     * Parsing the header and checking that every spectrum file used by the result file is available
     * @throws IOException
     */
    private void getParameters() throws IOException {

        HashSet<String> missingFiles = new HashSet<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile))) {

            parseHeader(bufferedReader.readLine());

            String line;

            while ((line = bufferedReader.readLine()) != null) {

                String[] lineSplit = line.split("\t");

                if (lineSplit.length <= fileNameIndex) {
                    continue;
                }

                String nameInResult = unquote(lineSplit[fileNameIndex]);

                if (getSpectrumFileName(nameInResult) == null) {
                    missingFiles.add(nameInResult);
                }
            }
        }

        if (!missingFiles.isEmpty()) {
            progressDialog.setRunFinished();
            StringBuilder message = new StringBuilder("The following spectrum file(s) used by the Sage result " +
                    "file were not selected:<br>");
            for (String missingFile : missingFiles) {
                message.append(missingFile).append("<br>");
            }
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(message.toString()),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException("Missing spectrum file(s): " + missingFiles);
        }
    }

    /**
     * Locate the columns the import needs and turn every Sage column into a database column name
     * @param headerLine Header line of the result file
     * @throws IOException
     */
    private void parseHeader(String headerLine) throws IOException {

        if (headerLine == null) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "The Sage result file is empty."), "File Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException("Empty Sage result file");
        }

        String[] headers = headerLine.split("\t");

        for (int i = 0; i < headers.length; i++) {
            String header = unquote(headers[i]);

            if (header.equalsIgnoreCase("peptide")) {
                peptideIndex = i;
            } else if (header.equalsIgnoreCase("filename")) {
                fileNameIndex = i;
            } else if (header.equalsIgnoreCase("scannr")) {
                scanNrIndex = i;
            } else if (header.equalsIgnoreCase("charge")) {
                chargeIndex = i;
            } else if (header.equalsIgnoreCase("expmass")) {
                expMassIndex = i;
            } else if (header.equalsIgnoreCase("calcmass")) {
                calcMassIndex = i;
            } else if (header.equalsIgnoreCase("label")) {
                labelIndex = i;
            } else if (header.equalsIgnoreCase("spectrum_q")) {
                spectrumQIndex = i;
            } else if (header.equalsIgnoreCase("hyperscore")) {
                hyperScoreIndex = i;
            }

            String columnName = header.replaceAll("[^a-zA-Z0-9]", "");

            if (columnName.length() == 0 || indexToName.containsValue(columnName)) {
                progressDialog.setRunFinished();
                JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                        "The column '" + header + "' cannot be used as a database column name, it is empty " +
                                "or clashes with another column."), "File Error", JOptionPane.ERROR_MESSAGE);
                throw new IOException("Unusable Sage column name: '" + header + "'");
            }

            indexToName.put(i, columnName);
        }

        if (peptideIndex == -1 || fileNameIndex == -1 || scanNrIndex == -1 || chargeIndex == -1
                || expMassIndex == -1 || calcMassIndex == -1) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "This does not look like a Sage result file: the peptide, filename, scannr, charge, " +
                            "expmass and calcmass columns are required."), "File Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException("Missing mandatory Sage columns");
        }
    }

    /**
     * Parsing the result file and putting the matches into the database
     * @throws IOException
     * @throws SQLException
     */
    private void parseFile() throws IOException, SQLException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?");

        HashMap<String, Integer> nameToDBIndex = new HashMap<>();

        int countColumn = 0;
        for (Integer index : indexToName.keySet()) {
            countColumn++;
            addQuery.append(", ").append(indexToName.get(index)).append(" OBJECT(50)");
            addValuesQuery.append(",?");
            nameToDBIndex.put(indexToName.get(index), 6 + countColumn);
        }
        addValuesQuery.append(")");

        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char," +
                " MassError DOUBLE, Match Object" + addQuery + ", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute("DROP TABLE IF EXISTS SpectrumMatch");
            statement.execute(matchTableQuery);
        } catch (SQLException e) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "An error occurred while creating table SpectrumMatch in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        } finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch " + addValuesQuery;
        PreparedStatement preparedStatement = null;

        Integer psmIndex = 0;
        Integer count = 0;
        Integer countRound = 0;

        String line;
        ArrayList<String> spectrumList = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile))) {
            bufferedReader.readLine();

            while ((line = bufferedReader.readLine()) != null) {

                String[] lineSplit = line.split("\t");

                if (lineSplit.length <= peptideIndex) {
                    continue;
                }

                if (targetsOnly && labelIndex != -1 && getDouble(lineSplit, labelIndex, 1.0) < 0) {
                    continue;
                }

                if (filterQValue && spectrumQIndex != -1 && getDouble(lineSplit, spectrumQIndex, 0.0) > 0.01) {
                    continue;
                }

                if (count == 0) {
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                String spectrumFileName = getSpectrumFileName(unquote(lineSplit[fileNameIndex]));
                String spectrumTitle = unquote(lineSplit[scanNrIndex]);
                Integer charge = Math.max(1, (int) getDouble(lineSplit, chargeIndex, 1.0));
                Double expMass = getDouble(lineSplit, expMassIndex, 0.0);
                Double calcMass = getDouble(lineSplit, calcMassIndex, 0.0);
                Double massError = expMass - calcMass;
                Double observedMZ = expMass / charge + PROTON_MASS;

                PeptideData peptideData = parsePeptide(unquote(lineSplit[peptideIndex]));

                ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();
                for (SageModification modification : peptideData.modifications) {
                    addModification(modification, modificationMatches);
                }

                Peptide peptide = new Peptide(peptideData.sequence, modificationMatches);

                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge),
                        massError, "Sage");

                if (hyperScoreIndex != -1) {
                    peptideAssumption.setRawScore(getDouble(lineSplit, hyperScoreIndex, 0.0));
                }

                SpectrumMatch spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));

                Integer scanNumber = getScanNumber(spectrumFileName, spectrumTitle);
                if (scanNumber != null) {
                    spectrumMatch.setSpectrumNumber(scanNumber);
                }

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

                spectrumList.add(String.valueOf(psmIndex));

                preparedStatement.setInt(1, psmIndex);
                preparedStatement.setDouble(2, observedMZ);
                preparedStatement.setString(3, spectrumTitle);
                preparedStatement.setString(4, peptideData.sequence);
                preparedStatement.setDouble(5, massError);
                preparedStatement.setBytes(6, bos.toByteArray());

                for (Integer index : indexToName.keySet()) {
                    String value = index < lineSplit.length ? unquote(lineSplit[index]) : "";
                    int dbIndex = nameToDBIndex.get(indexToName.get(index));
                    try {
                        preparedStatement.setDouble(dbIndex, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        preparedStatement.setString(dbIndex, value);
                    }
                }

                preparedStatement.addBatch();

                count++;
                psmIndex++;

                if (count == 1000) {

                    preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(spectrumList);

                    count = 0;

                    pdvMainClass.updatePTMSetting();

                    if (countRound == 0) {
                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();
                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                    }
                    countRound++;

                    spectrumList = new ArrayList<>();
                }
            }
        }

        if (count != 0) {

            preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.updatePTMSetting();
            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if (countRound == 0) {

                pdvMainClass.displayResult();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();

            } else {
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }
        }

        if (psmIndex == 0) {
            // nothing was paged in, leaving the window on "Import done" would let the user page or
            // search into an empty allSpectrumIndex
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "No PSM left after filtering, please check the import settings."),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            pdvMainClass.loadingJButton.setText("No PSM imported");
            return;
        }

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    /**
     * Get the scan number of one spectrum. For mzML the nativeID reported by Sage is looked up in the index
     * of the spectrum file, for mgf the scan number is only informative
     * @param spectrumFileName Spectrum file name
     * @param spectrumTitle Spectrum title or nativeID
     * @return Scan number or null
     */
    private Integer getScanNumber(String spectrumFileName, String spectrumTitle) {

        if (spectrumFileType.equals("mzml") && spectrumIdAndNumber != null) {
            HashMap<String, Integer> idToNumber = spectrumIdAndNumber.get(spectrumFileName);
            if (idToNumber != null && idToNumber.get(spectrumTitle) != null) {
                return idToNumber.get(spectrumTitle);
            }
        }

        Matcher matcher = SCAN_PATTERN.matcher(spectrumTitle);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    /**
     * Get one numeric value of a result line
     * @param lineSplit Result line
     * @param index Column index
     * @param defaultValue Value returned when the column is missing or not numeric
     * @return Value
     */
    private double getDouble(String[] lineSplit, int index, double defaultValue) {
        if (index < 0 || index >= lineSplit.length) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(unquote(lineSplit[index]));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Remove the CSV quoting Sage uses for the fields containing separators, e.g. the mgf titles
     * @param value Raw field
     * @return Field value
     */
    static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 1 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    /**
     * Parsing one Sage peptide, e.g. "[+42.0106]-NDTPC[+57.0215]GTTIGPILASR-[+12.0]"
     * @param value Sage peptide
     * @return Sequence and modifications
     */
    static PeptideData parsePeptide(String value) {

        StringBuilder sequence = new StringBuilder();
        ArrayList<SageModification> modifications = new ArrayList<>();
        Double nTermMass = null;
        Double cTermMass = null;

        int index = 0;

        if (value.startsWith("[")) {
            int closeIndex = value.indexOf(']');
            if (closeIndex > 0) {
                nTermMass = getModificationMass(value.substring(1, closeIndex));
                index = closeIndex + 1;
                if (index < value.length() && value.charAt(index) == '-') {
                    index++;
                }
            }
        }

        while (index < value.length()) {
            char current = value.charAt(index);

            if (Character.isLetter(current)) {
                sequence.append(Character.toUpperCase(current));
                index++;

            } else if (current == '[') {
                int closeIndex = value.indexOf(']', index);
                if (closeIndex < 0) {
                    break;
                }
                Double mass = getModificationMass(value.substring(index + 1, closeIndex));
                if (mass != null && sequence.length() > 0) {
                    modifications.add(new SageModification(mass,
                            String.valueOf(sequence.charAt(sequence.length() - 1)), sequence.length()));
                }
                index = closeIndex + 1;

            } else if (current == '-') {
                int openIndex = value.indexOf('[', index);
                int closeIndex = openIndex < 0 ? -1 : value.indexOf(']', openIndex);
                if (closeIndex > openIndex) {
                    cTermMass = getModificationMass(value.substring(openIndex + 1, closeIndex));
                    index = closeIndex + 1;
                } else {
                    index++;
                }

            } else {
                index++;
            }
        }

        if (nTermMass != null) {
            modifications.add(new SageModification(nTermMass, "N-term", 1));
        }
        if (cTermMass != null && sequence.length() > 0) {
            modifications.add(new SageModification(cTermMass, "C-term", sequence.length()));
        }

        return new PeptideData(sequence.toString(), modifications);
    }

    /**
     * Get the mass of one modification token, e.g. "+57.0215"
     * @param modificationToken Modification token
     * @return Mass or null
     */
    private static Double getModificationMass(String modificationToken) {
        String cleanToken = modificationToken.trim();
        if (cleanToken.startsWith("+")) {
            cleanToken = cleanToken.substring(1);
        }
        try {
            return Double.valueOf(cleanToken);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Create one modification match, registering the PTM when it is seen for the first time
     * @param modification Modification found in the Sage peptide
     * @param modificationMatches Modification matches of the current peptide
     */
    private void addModification(SageModification modification, ArrayList<ModificationMatch> modificationMatches) {

        String residue = modification.residue;
        Double mass = modification.mass;
        String singleModificationName = mass + " of " + residue;

        if (!ptmFactory.containsPTM(singleModificationName)) {
            ArrayList<String> residues = new ArrayList<>();
            residues.add(residue);

            PTM ptm;
            if (residue.equals("N-term")) {
                ptm = new PTM(PTM.MODNP, singleModificationName, mass, residues);
            } else if (residue.equals("C-term")) {
                ptm = new PTM(PTM.MODCP, singleModificationName, mass, residues);
            } else {
                ptm = new PTM(PTM.MODAA, singleModificationName, mass, residues);
                if ((residue.equals("T") || residue.equals("S")) && mass < 80.01 && mass > 79.9) {
                    ptm.addNeutralLoss(NeutralLoss.H3PO4);
                }
            }
            ptm.setShortName(String.valueOf(mass));
            ptmFactory.addUserPTM(ptm);
        }

        if (!allModifications.contains(singleModificationName)) {
            allModifications.add(singleModificationName);
        }

        modificationMatches.add(new ModificationMatch(singleModificationName, true, modification.position));
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection() {
        return sqLiteConnection;
    }

    /**
     * Return all the Sage column names
     * @return ArrayList
     */
    public ArrayList<String> getScoreName() {
        return new ArrayList<>(indexToName.values());
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications() {
        return allModifications;
    }

    /**
     * One parsed Sage peptide
     */
    static class PeptideData {
        final String sequence;
        final ArrayList<SageModification> modifications;

        PeptideData(String sequence, ArrayList<SageModification> modifications) {
            this.sequence = sequence;
            this.modifications = modifications;
        }
    }

    /**
     * One modification found in a Sage peptide
     */
    static class SageModification {
        final Double mass;
        final String residue;
        final int position;

        SageModification(Double mass, String residue, int position) {
            this.mass = mass;
            this.residue = residue;
            this.position = position;
        }
    }
}

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
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
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
import java.util.List;

/**
 * Import InstaNovo prediction CSV files.
 */
public class InstaNovoImport {

    private final PDVMainClass pdvMainClass;
    private final File resultFile;
    private final ArrayList<File> spectrumFiles;
    private final String spectrumFileType;
    private final ProgressDialogX progressDialog;
    private final SQLiteConnection sqLiteConnection;
    private final ArrayList<String> allModifications = new ArrayList<>();
    private final PTMFactory ptmFactory = PTMFactory.getInstance();
    private SpectrumFactory spectrumFactory;

    public InstaNovoImport(PDVMainClass pdvMainClass, File resultFile, ArrayList<File> spectrumFiles,
                           Object spectrumsFileFactory, String spectrumFileType,
                           ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.resultFile = resultFile;
        this.spectrumFiles = spectrumFiles;
        this.spectrumFileType = spectrumFileType;
        this.progressDialog = progressDialog;

        if ("mgf".equals(spectrumFileType)) {
            this.spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
        }

        String dbName = resultFile.getParentFile().getAbsolutePath() + "/" + resultFile.getName() + ".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);
        sqLiteConnection.setScoreNum(1);
    }

    public void importResults() throws IOException, SQLException {
        parseFile();
    }

    private void parseFile() throws IOException, SQLException {
        Connection connection = sqLiteConnection.getConnection();
        connection.setAutoCommit(false);

        Statement statement = connection.createStatement();
        String addTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, Score DOUBLE, PRIMARY KEY(PSMIndex))";
        try {
            statement.execute(addTableQuery);
        } catch (SQLException e) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "An error occurred while creating table SpectrumMatch in database."),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            throw e;
        } finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?)";
        PreparedStatement preparedStatement = null;

        HashMap<String, String> experimentToFileName = getExperimentToFileName();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile));
        String headerLine = bufferedReader.readLine();
        if (headerLine == null) {
            progressDialog.setRunFinished();
            throw new IOException("Empty InstaNovo result file");
        }

        ArrayList<String> headers = parseCsvLine(removeBom(headerLine));
        HashMap<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerMap.put(headers.get(i).trim().toLowerCase(), i);
        }

        int count = 0;
        int countRound = 0;
        int psmIndex = 0;
        ArrayList<String> spectrumList = new ArrayList<>();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            ArrayList<String> values = parseCsvLine(line);
            PeptideData peptideData = getPeptideData(values, headerMap);
            if (peptideData.sequence.length() == 0) {
                continue;
            }

            String spectrumFileName = getSpectrumFileName(values, headerMap, experimentToFileName);
            String scanNumber = getScanNumber(values, headerMap);
            String spectrumTitle = getSpectrumTitle(spectrumFileName, scanNumber, values, headerMap);
            Integer chargeValue = getCharge(values, headerMap, spectrumFileName, spectrumTitle);
            Double score = getScore(values, headerMap);

            if (count == 0) {
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            SpectrumMatch spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));
            if ("mzml".equals(spectrumFileType)) {
                spectrumMatch.setSpectrumNumber(parseInteger(scanNumber, psmIndex));
            } else {
                spectrumMatch.setSpectrumNumber(parseInteger(scanNumber, psmIndex));
            }

            Peptide peptide = new Peptide(peptideData.sequence, peptideData.modificationMatches);
            PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, chargeValue), 0, resultFile.getName());
            peptideAssumption.setScore(score);
            spectrumMatch.addHit(0, peptideAssumption, false);
            spectrumMatch.setBestPeptideAssumption(peptideAssumption);

            Double precursorMz = getPrecursorMz(values, headerMap, spectrumFileName, spectrumTitle, peptideAssumption);

            spectrumList.add(String.valueOf(psmIndex));

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

            preparedStatement.setInt(1, psmIndex);
            preparedStatement.setDouble(2, precursorMz);
            preparedStatement.setString(3, spectrumTitle);
            preparedStatement.setString(4, peptideData.sequence);
            preparedStatement.setDouble(5, 0.0);
            preparedStatement.setBytes(6, bos.toByteArray());
            preparedStatement.setDouble(7, score);
            preparedStatement.addBatch();

            count++;
            psmIndex++;

            if (count >= 1000) {
                preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumList);
                count = 0;

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
        bufferedReader.close();

        if (count != 0) {
            preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if (countRound == 0) {
                pdvMainClass.displayResult();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();
            } else {
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }
        } else if (countRound == 0) {
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "No InstaNovo predictions found in file, please check your file."),
                    "File Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException("No InstaNovo predictions found");
        }

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    private HashMap<String, String> getExperimentToFileName() {
        HashMap<String, String> experimentToFileName = new HashMap<>();
        for (File spectrumFile : spectrumFiles) {
            String fileName = spectrumFile.getName();
            experimentToFileName.put(fileName.toLowerCase(), fileName);
            int dotIndex = fileName.lastIndexOf(".");
            if (dotIndex > 0) {
                experimentToFileName.put(fileName.substring(0, dotIndex).toLowerCase(), fileName);
            }
        }
        return experimentToFileName;
    }

    private String getSpectrumFileName(ArrayList<String> values, HashMap<String, Integer> headerMap,
                                       HashMap<String, String> experimentToFileName) {
        String experimentName = getValue(values, headerMap, "experiment_name");
        if (experimentName.length() == 0) {
            String spectrumId = getValue(values, headerMap, "spectrum_id");
            if (spectrumId.contains(":")) {
                experimentName = spectrumId.substring(0, spectrumId.indexOf(":"));
            }
        }

        String fileName = experimentToFileName.get(experimentName.toLowerCase());
        if (fileName != null) {
            return fileName;
        }
        return spectrumFiles.get(0).getName();
    }

    private String getScanNumber(ArrayList<String> values, HashMap<String, Integer> headerMap) {
        String scanNumber = getValue(values, headerMap, "scan_number");
        if (scanNumber.length() == 0) {
            String spectrumId = getValue(values, headerMap, "spectrum_id");
            if (spectrumId.contains(":")) {
                scanNumber = spectrumId.substring(spectrumId.lastIndexOf(":") + 1);
            }
        }
        if (scanNumber.contains(".")) {
            scanNumber = scanNumber.substring(0, scanNumber.indexOf("."));
        }
        return scanNumber;
    }

    private String getSpectrumTitle(String spectrumFileName, String scanNumber, ArrayList<String> values,
                                    HashMap<String, Integer> headerMap) {
        if ("mgf".equals(spectrumFileType) && scanNumber.length() != 0) {
            int scanIndex = parseInteger(scanNumber, -1);
            if (scanIndex >= 0) {
                try {
                    return spectrumFactory.getSpectrumTitle(spectrumFileName, scanIndex + 1);
                } catch (Exception ignored) {
                    try {
                        return spectrumFactory.getSpectrumTitle(spectrumFileName, scanIndex);
                    } catch (Exception ignoredAgain) {
                    }
                }
            }
        }

        String spectrumId = getValue(values, headerMap, "spectrum_id");
        if (spectrumId.length() != 0 && !spectrumId.contains(":")) {
            return spectrumId;
        }
        if (scanNumber.length() != 0) {
            return scanNumber;
        }
        return String.valueOf(getRowFallbackIndex(values));
    }

    private Integer getCharge(ArrayList<String> values, HashMap<String, Integer> headerMap,
                              String spectrumFileName, String spectrumTitle) {
        String charge = getValue(values, headerMap, "precursor_charge");
        if (charge.length() != 0) {
            return parseInteger(charge, 1);
        }

        if ("mgf".equals(spectrumFileType)) {
            try {
                Precursor precursor = spectrumFactory.getPrecursor(spectrumFileName, spectrumTitle);
                if (precursor != null && precursor.getPossibleCharges() != null && precursor.getPossibleCharges().size() != 0) {
                    return precursor.getPossibleCharges().get(0).value;
                }
            } catch (Exception ignored) {
            }
        }

        return 1;
    }

    private Double getPrecursorMz(ArrayList<String> values, HashMap<String, Integer> headerMap, String spectrumFileName,
                                  String spectrumTitle, PeptideAssumption peptideAssumption) {
        String precursorMz = getValue(values, headerMap, "precursor_mz");
        if (precursorMz.length() != 0) {
            return parseDouble(precursorMz, peptideAssumption.getTheoreticMz());
        }

        if ("mgf".equals(spectrumFileType)) {
            try {
                Precursor precursor = spectrumFactory.getPrecursor(spectrumFileName, spectrumTitle);
                if (precursor != null) {
                    return precursor.getMz();
                }
            } catch (Exception ignored) {
            }
        }

        return peptideAssumption.getTheoreticMz();
    }

    private Double getScore(ArrayList<String> values, HashMap<String, Integer> headerMap) {
        String[] scoreColumns = new String[]{
                "log_probs", "log_probabilities", "prediction_log_probability",
                "diffusion_log_probabilities", "transformer_log_probabilities",
                "delta_mass_ppm"
        };

        for (String scoreColumn : scoreColumns) {
            String value = getValue(values, headerMap, scoreColumn);
            if (value.length() != 0) {
                return parseDouble(value, 0.0);
            }
        }
        return 0.0;
    }

    private PeptideData getPeptideData(ArrayList<String> values, HashMap<String, Integer> headerMap) {
        String[] sequenceColumns = new String[]{
                "diffusion_predictions_tokenised", "predictions_tokenised",
                "preds_tokenised", "transformer_predictions_tokenised",
                "diffusion_predictions", "predictions", "preds", "transformer_predictions"
        };

        for (String sequenceColumn : sequenceColumns) {
            String value = getValue(values, headerMap, sequenceColumn);
            if (value.length() != 0 && !"nan".equalsIgnoreCase(value)) {
                if (sequenceColumn.endsWith("tokenised")) {
                    return parseTokenisedPeptide(value);
                }
                return parsePeptideString(value);
            }
        }
        return new PeptideData("", new ArrayList<>());
    }

    private PeptideData parseTokenisedPeptide(String value) {
        ArrayList<String> tokens = new ArrayList<>();
        String cleanValue = value.trim();
        if (cleanValue.startsWith("[") && cleanValue.endsWith("]")) {
            cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
        }

        if (cleanValue.contains(",")) {
            for (String token : cleanValue.split(",")) {
                tokens.add(cleanToken(token));
            }
        } else {
            return parsePeptideString(cleanValue);
        }

        return parseTokens(tokens);
    }

    private PeptideData parseTokens(List<String> tokens) {
        StringBuilder sequence = new StringBuilder();
        ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();
        ArrayList<Double> pendingNTerminalMods = new ArrayList<>();

        for (String token : tokens) {
            if (token.length() == 0 || "nan".equalsIgnoreCase(token)) {
                continue;
            }

            if (Character.isLetter(token.charAt(0))) {
                char aa = Character.toUpperCase(token.charAt(0));
                sequence.append(aa);
                int position = sequence.length();
                if (token.length() > 1) {
                    addModification(token.substring(1), String.valueOf(aa), position, modificationMatches);
                }
            } else {
                Double mass = getModificationMass(token);
                if (mass != null) {
                    pendingNTerminalMods.add(mass);
                }
            }
        }

        for (Double mass : pendingNTerminalMods) {
            addModification(mass, "N-term", 1, modificationMatches);
        }

        return new PeptideData(sequence.toString(), modificationMatches);
    }

    private PeptideData parsePeptideString(String value) {
        String cleanValue = value.trim();
        if (cleanValue.startsWith("_") && cleanValue.endsWith("_") && cleanValue.length() > 1) {
            cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
        }
        if (cleanValue.startsWith(".") && cleanValue.endsWith(".") && cleanValue.length() > 1) {
            cleanValue = cleanValue.substring(1, cleanValue.length() - 1);
        }

        ArrayList<String> tokens = new ArrayList<>();
        ArrayList<Double> pendingNTerminalMods = new ArrayList<>();
        StringBuilder sequence = new StringBuilder();
        ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();

        int index = 0;
        if (cleanValue.contains("-") && cleanValue.length() > 0 && !Character.isLetter(cleanValue.charAt(0))) {
            String terminalToken = cleanValue.substring(0, cleanValue.indexOf("-"));
            Double mass = getModificationMass(terminalToken);
            if (mass != null) {
                pendingNTerminalMods.add(mass);
            }
            index = cleanValue.indexOf("-") + 1;
        }

        while (index < cleanValue.length()) {
            char current = cleanValue.charAt(index);
            if (Character.isLetter(current)) {
                char aa = Character.toUpperCase(current);
                sequence.append(aa);
                int position = sequence.length();
                index++;

                if (index < cleanValue.length() && (cleanValue.charAt(index) == '[' || cleanValue.charAt(index) == '(')) {
                    char closeChar = cleanValue.charAt(index) == '[' ? ']' : ')';
                    int closeIndex = cleanValue.indexOf(closeChar, index);
                    if (closeIndex > index) {
                        String modification = cleanValue.substring(index, closeIndex + 1);
                        addModification(modification, String.valueOf(aa), position, modificationMatches);
                        index = closeIndex + 1;
                    }
                }
            } else if (current == ',' || Character.isWhitespace(current)) {
                index++;
            } else {
                tokens.add(String.valueOf(current));
                index++;
            }
        }

        for (Double mass : pendingNTerminalMods) {
            addModification(mass, "N-term", 1, modificationMatches);
        }

        return new PeptideData(sequence.toString(), modificationMatches);
    }

    private void addModification(String modificationToken, String residue, int position,
                                 ArrayList<ModificationMatch> modificationMatches) {
        Double mass = getModificationMass(modificationToken);
        if (mass != null) {
            addModification(mass, residue, position, modificationMatches);
        }
    }

    private void addModification(Double mass, String residue, int position,
                                 ArrayList<ModificationMatch> modificationMatches) {
        String singleModificationName = mass + " of " + residue;
        addPtm(singleModificationName, mass);
        if (!allModifications.contains(singleModificationName)) {
            allModifications.add(singleModificationName);
        }
        modificationMatches.add(new ModificationMatch(singleModificationName, true, position));
    }

    private Double getModificationMass(String modificationToken) {
        String cleanToken = modificationToken.trim();
        if (cleanToken.startsWith("[") && cleanToken.endsWith("]")) {
            cleanToken = cleanToken.substring(1, cleanToken.length() - 1);
        }
        if (cleanToken.startsWith("(") && cleanToken.endsWith(")")) {
            cleanToken = cleanToken.substring(1, cleanToken.length() - 1);
        }

        String upperToken = cleanToken.toUpperCase();
        if (upperToken.startsWith("UNIMOD:")) {
            return getUnimodMass(upperToken);
        }
        if ("OX".equalsIgnoreCase(cleanToken)) {
            return 15.994915;
        }
        if ("P".equalsIgnoreCase(cleanToken)) {
            return 79.966331;
        }

        try {
            return Double.valueOf(cleanToken);
        } catch (NumberFormatException ignored) {
            if (cleanToken.startsWith("+") || cleanToken.startsWith("-")) {
                try {
                    return Double.valueOf(cleanToken.substring(0, cleanToken.length()));
                } catch (NumberFormatException ignoredAgain) {
                }
            }
        }
        return null;
    }

    private Double getUnimodMass(String unimod) {
        if ("UNIMOD:1".equals(unimod)) {
            return 42.010565;
        } else if ("UNIMOD:4".equals(unimod)) {
            return 57.021464;
        } else if ("UNIMOD:5".equals(unimod)) {
            return 43.005814;
        } else if ("UNIMOD:7".equals(unimod)) {
            return 0.984016;
        } else if ("UNIMOD:21".equals(unimod)) {
            return 79.966331;
        } else if ("UNIMOD:35".equals(unimod)) {
            return 15.994915;
        } else if ("UNIMOD:385".equals(unimod)) {
            return -17.026549;
        }
        return null;
    }

    private void addPtm(String singleModificationName, double modificationMass) {
        if (!ptmFactory.containsPTM(singleModificationName)) {
            String modificationName = singleModificationName.split(" of ")[0];
            String residue = singleModificationName.split(" of ")[1];
            ArrayList<String> residues = new ArrayList<>();
            residues.add(residue);

            PTM ptm;
            if (singleModificationName.toLowerCase().contains("n-term")) {
                ptm = new PTM(PTM.MODNP, singleModificationName, modificationMass, residues);
            } else if (singleModificationName.toLowerCase().contains("c-term")) {
                ptm = new PTM(PTM.MODCP, singleModificationName, modificationMass, residues);
            } else {
                ptm = new PTM(PTM.MODAA, singleModificationName, modificationMass, residues);
                if ((residue.equals("T") || residue.equals("S")) && modificationMass < 80.01 && modificationMass > 79.9) {
                    ptm.addNeutralLoss(NeutralLoss.H3PO4);
                }
            }
            ptm.setShortName(modificationName);
            ptmFactory.addUserPTM(ptm);
        }
    }

    private ArrayList<String> parseCsvLine(String line) {
        ArrayList<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }

    private String getValue(ArrayList<String> values, HashMap<String, Integer> headerMap, String columnName) {
        Integer index = headerMap.get(columnName.toLowerCase());
        if (index == null || index >= values.size()) {
            return "";
        }
        return values.get(index).trim();
    }

    private String cleanToken(String token) {
        String cleanToken = token.trim();
        if ((cleanToken.startsWith("'") && cleanToken.endsWith("'")) || (cleanToken.startsWith("\"") && cleanToken.endsWith("\""))) {
            cleanToken = cleanToken.substring(1, cleanToken.length() - 1);
        }
        return cleanToken.trim();
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        try {
            if (value.contains(".")) {
                return (int) Double.parseDouble(value);
            }
            return Integer.valueOf(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private Double parseDouble(String value, Double defaultValue) {
        try {
            if ("nan".equalsIgnoreCase(value) || "inf".equalsIgnoreCase(value) || "-inf".equalsIgnoreCase(value)) {
                return defaultValue;
            }
            return Double.valueOf(value);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String removeBom(String value) {
        if (value != null && value.startsWith("\uFEFF")) {
            return value.substring(1);
        }
        return value;
    }

    private int getRowFallbackIndex(ArrayList<String> values) {
        return values.hashCode();
    }

    public SQLiteConnection getSqLiteConnection() {
        return sqLiteConnection;
    }

    public ArrayList<String> getAllModifications() {
        return allModifications;
    }

    public ArrayList<String> getScoreName() {
        ArrayList<String> scoreName = new ArrayList<>();
        scoreName.add("Score");
        return scoreName;
    }

    private static class PeptideData {
        private final String sequence;
        private final ArrayList<ModificationMatch> modificationMatches;

        private PeptideData(String sequence, ArrayList<ModificationMatch> modificationMatches) {
            this.sequence = sequence;
            this.modificationMatches = modificationMatches;
        }
    }
}

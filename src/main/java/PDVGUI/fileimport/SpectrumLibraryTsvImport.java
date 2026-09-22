package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.SpectrumLibDisplay;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.pride.CvTerm;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Import a Carafe spectral library in tsv format.
 *
 * <p>Carafe writes one row per fragment ion, with the rows of a precursor kept together, so the
 * file is read as a stream and a library entry is flushed whenever the modified peptide or the
 * precursor charge changes. Columns are resolved by header name because the layout varies: the
 * optional {@code IonMobility} column shifts everything after it, and libraries written by
 * DIA-NN itself spell some of the columns differently.</p>
 *
 * <p>The {@code ModifiedPeptide} column carries the modifications inline, in whichever of the
 * three notations Carafe was asked for ({@code -lf_type}): UniMod accessions for the DIA-NN
 * format ({@code _(UniMod:737)SGGGGGR_}, with {@code M[UniMod:35]} for the few modifications
 * Carafe hard-codes), UniMod titles for the default format ({@code M[Oxidation]}) and
 * site-qualified names for the EncyclopeDIA format ({@code C[Carbamidomethyl (C)]}). All three
 * are handled by {@link #parseModifiedPeptide}, which only splits the label out of the sequence,
 * and {@link #resolvePtmName}, which maps a label onto a PTM of the factory.</p>
 *
 * <p>{@code Tr_recalibrated} is not put on the precursor: the value is an iRT and the library
 * display has no retention time column, so it goes into the comment instead, where the unit
 * cannot be mistaken for the seconds a {@link Precursor} expects.</p>
 */
public class SpectrumLibraryTsvImport {

    /**
     * Number of library entries per page of the display table
     */
    private static final int ENTRIES_PER_PAGE = 1000;
    /**
     * Header of the column holding the peptide with its modifications
     */
    static final String[] MODIFIED_PEPTIDE_COLUMNS = {"ModifiedPeptide", "ModifiedPeptideSequence", "ModifiedSequence"};
    /**
     * Header of the column holding the precursor m/z
     */
    static final String[] PRECURSOR_MZ_COLUMNS = {"PrecursorMz"};
    /**
     * Header of the column holding the precursor charge
     */
    static final String[] PRECURSOR_CHARGE_COLUMNS = {"PrecursorCharge"};
    /**
     * Header of the column holding the fragment m/z
     */
    static final String[] FRAGMENT_MZ_COLUMNS = {"FragmentMz", "ProductMz"};
    /**
     * Header of the column holding the fragment intensity
     */
    static final String[] INTENSITY_COLUMNS = {"RelativeIntensity", "LibraryIntensity"};
    /**
     * Header of the column holding the retention time
     */
    static final String[] RETENTION_TIME_COLUMNS = {"Tr_recalibrated", "NormalizedRetentionTime", "RetentionTime", "iRT"};
    /**
     * Header of the column holding the protein
     */
    static final String[] PROTEIN_COLUMNS = {"ProteinID", "ProteinName", "UniprotID"};
    /**
     * Header of the column holding the decoy flag
     */
    static final String[] DECOY_COLUMNS = {"Decoy"};
    /**
     * EncyclopeDIA modification name -&gt; UniMod title, for the names Carafe writes that are not
     * the UniMod title itself. Carafe writes "Phosphorylation (ST)" and "Phosphorylation (Y)"
     * where UniMod, and therefore the PTM factory, uses the title "Phospho".
     */
    private static final HashMap<String, String> MODIFICATION_ALIASES = new HashMap<>();
    static {
        MODIFICATION_ALIASES.put("Phosphorylation", "Phospho");
    }
    /**
     * Sites a peptide N-terminal PTM of the factory can be named after, in lookup order
     */
    private static final String[] NTERM_SITES = {"N-term", "peptide N-term", "protein N-term"};

    /**
     * parent class
     */
    private SpectrumLibDisplay spectrumLibDisplay;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * tsv file
     */
    private File tsvFile;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Database name
     */
    private String dbName;
    /**
     * PTM factory, holding the modifications imported from unimod.xml
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Modification label of the library -&gt; PTM name of the factory, see resolvePtmName
     */
    private HashMap<String, String> labelToPtmName = new HashMap<>();
    /**
     * Modification labels that no PTM of the factory could be found for, reported once each
     */
    private HashSet<String> unknownLabels = new HashSet<>();

    /**
     * Constructor for the modification resolution on its own, without a file or a database
     */
    SpectrumLibraryTsvImport() {
    }

    /**
     * Constructor
     * @param tsvFile Spectral library file
     * @param spectrumLibDisplay Parent class
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public SpectrumLibraryTsvImport(File tsvFile, SpectrumLibDisplay spectrumLibDisplay, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.tsvFile = tsvFile;
        this.spectrumLibDisplay = spectrumLibDisplay;
        this.progressDialog = progressDialog;

        dbName = tsvFile.getAbsolutePath() + ".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);
    }

    /**
     * Start reading the library on its own thread.
     *
     * <p>Kept out of the constructor because the reading thread calls back into the display as soon
     * as it has a first page, and the display only holds the database connection once the
     * constructor has returned to it.</p>
     */
    public void start() {

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseFile();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, e.getMessage() == null ? "Failed to parse spectral library file, please check your file." : e.getMessage(),
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parse the library file
     * @throws IOException
     * @throws SQLException
     */
    private void parseFile() throws IOException, SQLException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);

        Statement statement = connection.createStatement();
        String matchTableQuery = "CREATE TABLE SpectrumMatch (LibID INT(8), MZ DOUBLE, Charge INT(2), NumPeaks INT(4), Match Object, Spectrum Object, Comment Object, PRIMARY KEY(LibID))";

        try {
            statement.execute(matchTableQuery);
        } catch (SQLException e) {
            System.out.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        } finally {
            statement.close();
        }

        // the reader is closed and the half done transaction rolled back whichever way the reading
        // ends, so that a row the parser chokes on cannot keep the database file locked and make
        // the next import of the same file fail on a table that is already there
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tsvFile))) {
            readLibrary(connection, bufferedReader);
        } catch (IOException | SQLException | RuntimeException e) {
            connection.rollback();
            throw e;
        }
    }

    /**
     * Read the library rows and write one entry per precursor into the database
     * @param connection Database connection
     * @param bufferedReader Reader on the library file
     * @throws IOException
     * @throws SQLException
     */
    private void readLibrary(Connection connection, BufferedReader bufferedReader) throws IOException, SQLException {

        String headerLine = bufferedReader.readLine();
        if (headerLine == null) {
            throw new IOException("The spectral library file is empty.");
        }

        HashMap<String, Integer> columnIndexes = getColumnIndexes(headerLine);

        // the modified peptide is required rather than falling back to StrippedPeptide: a header
        // that is not recognised would otherwise load the whole library with unmodified sequences,
        // which looks perfectly normal in the table while every peptide mass is wrong
        int peptideIndex = getRequiredColumnIndex(columnIndexes, MODIFIED_PEPTIDE_COLUMNS);
        int precursorMzIndex = getRequiredColumnIndex(columnIndexes, PRECURSOR_MZ_COLUMNS);
        int precursorChargeIndex = getRequiredColumnIndex(columnIndexes, PRECURSOR_CHARGE_COLUMNS);
        int fragmentMzIndex = getRequiredColumnIndex(columnIndexes, FRAGMENT_MZ_COLUMNS);
        int intensityIndex = getRequiredColumnIndex(columnIndexes, INTENSITY_COLUMNS);
        int retentionTimeIndex = getColumnIndex(columnIndexes, RETENTION_TIME_COLUMNS);
        int proteinIndex = getColumnIndex(columnIndexes, PROTEIN_COLUMNS);
        int decoyIndex = getColumnIndex(columnIndexes, DECOY_COLUMNS);

        int columnsNeeded = 1 + Math.max(Math.max(peptideIndex, precursorMzIndex),
                Math.max(precursorChargeIndex, Math.max(fragmentMzIndex, intensityIndex)));

        PreparedStatement preparedStatement = null;

        // fragments of the precursor being read, keyed by m/z so that the two ions sharing an m/z
        // become one peak, the more intense one, as they do in the spectrum itself
        HashMap<Double, Peak> peakMap = new HashMap<>();
        String currentPeptide = null;
        String currentCharge = null;
        String currentDecoy = null;
        double currentPrecursorMz = 0.0;
        String currentComment = "";

        ArrayList<String> libIDList = new ArrayList<>();

        int libID = 0;
        int countRound = 0;
        int countImport = 0;

        String readLine;

        while ((readLine = bufferedReader.readLine()) != null) {

            if (readLine.trim().isEmpty()) {
                continue;
            }

            // the limit keeps the trailing empty fields, so a row whose last column is blank is
            // not mistaken below for a truncated one
            String[] values = readLine.split("\t", -1);

            // a run that was interrupted leaves a half written last row, which is left out rather
            // than losing the rest of the library to it
            if (values.length < columnsNeeded) {
                continue;
            }

            String peptide = values[peptideIndex];
            String charge = values[precursorChargeIndex];
            // a library that holds both can carry a target and a decoy of the same peptide and
            // charge, which are two entries rather than one spectrum of merged fragments
            String decoy = decoyIndex == -1 || decoyIndex >= values.length ? "" : values[decoyIndex];
            boolean samePrecursor = peptide.equals(currentPeptide) && charge.equals(currentCharge)
                    && decoy.equals(currentDecoy);

            if (!samePrecursor) {

                if (currentPeptide != null) {

                    if (countRound == 0) {
                        preparedStatement = connection.prepareStatement("INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?)");
                    }

                    libID++;
                    countRound++;
                    libIDList.add(String.valueOf(libID));

                    addEntry(preparedStatement, libID, currentPeptide, Integer.parseInt(currentCharge), currentPrecursorMz, peakMap, currentComment);

                    if (countRound == ENTRIES_PER_PAGE) {
                        preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        displayPage(libIDList, countImport);

                        countRound = 0;
                        countImport++;
                        libIDList = new ArrayList<>();
                    }

                    peakMap = new HashMap<>();
                }

                // the rest of the precursor level columns is the same on every row of the precursor
                currentPeptide = peptide;
                currentCharge = charge;
                currentDecoy = decoy;
                currentPrecursorMz = Double.parseDouble(values[precursorMzIndex]);
                currentComment = getComment(values, proteinIndex, retentionTimeIndex, decoyIndex);
            }

            addPeak(peakMap, Double.parseDouble(values[fragmentMzIndex]), Double.parseDouble(values[intensityIndex]));
        }

        if (currentPeptide != null) {

            if (countRound == 0) {
                preparedStatement = connection.prepareStatement("INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?)");
            }

            libID++;
            countRound++;
            libIDList.add(String.valueOf(libID));

            addEntry(preparedStatement, libID, currentPeptide, Integer.parseInt(currentCharge), currentPrecursorMz, peakMap, currentComment);
        }

        if (countRound != 0) {
            preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            displayPage(libIDList, countImport);
        } else {
            throw new IOException("The spectral library file has no fragment ion.");
        }
    }

    /**
     * Add one library entry to the batch
     * @param preparedStatement Insert statement
     * @param libID Library ID
     * @param modifiedPeptide Peptide with its modifications
     * @param charge Precursor charge
     * @param precursorMz Precursor m/z
     * @param peakMap Fragment peaks
     * @param comment Comment
     * @throws IOException
     * @throws SQLException
     */
    private void addEntry(PreparedStatement preparedStatement, int libID, String modifiedPeptide, int charge, double precursorMz,
                          HashMap<Double, Peak> peakMap, String comment) throws IOException, SQLException {

        ArrayList<Charge> chargeList = new ArrayList<>();
        chargeList.add(new Charge(Charge.PLUS, charge));

        MSnSpectrum spectrum = new MSnSpectrum(2, new Precursor(-1.0, precursorMz, chargeList),
                String.valueOf(libID), peakMap, "");

        SpectrumMatch spectrumMatch = getSpectrumMatch(modifiedPeptide, charge, libID);

        preparedStatement.setInt(1, libID);
        preparedStatement.setDouble(2, precursorMz);
        preparedStatement.setInt(3, charge);
        preparedStatement.setInt(4, peakMap.size());
        preparedStatement.setBytes(5, serialize(spectrumMatch));
        preparedStatement.setBytes(6, serialize(spectrum));
        preparedStatement.setString(7, comment);

        preparedStatement.addBatch();
    }

    /**
     * Show a page of library entries, opening the display on the first one
     * @param libIDList Library IDs of the page
     * @param countImport Number of pages shown so far
     */
    private void displayPage(ArrayList<String> libIDList, int countImport) {

        spectrumLibDisplay.allLibIDList.add(libIDList);

        if (countImport == 0) {
            spectrumLibDisplay.displayResults();
            spectrumLibDisplay.pageNumJTextField.setText(1 + "/" + 1);
            progressDialog.setRunFinished();
        } else {
            spectrumLibDisplay.pageNumJTextField.setText(spectrumLibDisplay.selectedPageNum + "/" + spectrumLibDisplay.allLibIDList.size());
            spectrumLibDisplay.buttonCheck();
        }
    }

    /**
     * Add one fragment peak, keeping the more intense one when an m/z is already taken
     * @param peakMap Fragment peaks
     * @param mz Fragment m/z
     * @param intensity Fragment intensity
     */
    private void addPeak(HashMap<Double, Peak> peakMap, Double mz, double intensity) {

        Peak existingPeak = peakMap.get(mz);

        if (existingPeak == null || existingPeak.intensity < intensity) {
            peakMap.put(mz, new Peak(mz, intensity));
        }
    }

    /**
     * Return the comment shown in the library table
     * @param values Values of the current row
     * @param proteinIndex Index of the protein column, -1 when absent
     * @param retentionTimeIndex Index of the retention time column, -1 when absent
     * @param decoyIndex Index of the decoy column, -1 when absent
     * @return String
     */
    private String getComment(String[] values, int proteinIndex, int retentionTimeIndex, int decoyIndex) {

        StringBuilder comment = new StringBuilder();

        if (proteinIndex != -1 && proteinIndex < values.length) {
            comment.append("Protein=").append(values[proteinIndex]);
        }
        if (retentionTimeIndex != -1 && retentionTimeIndex < values.length) {
            comment.append(comment.length() == 0 ? "" : " ").append("RT=").append(values[retentionTimeIndex]);
        }
        if (decoyIndex != -1 && decoyIndex < values.length) {
            comment.append(comment.length() == 0 ? "" : " ").append("Decoy=").append(values[decoyIndex]);
        }

        return comment.toString();
    }

    /**
     * Return the spectrum match of one library entry
     * @param modifiedPeptide Peptide with its modifications
     * @param charge Precursor charge
     * @param libID Library ID
     * @return SpectrumMatch
     */
    private SpectrumMatch getSpectrumMatch(String modifiedPeptide, int charge, int libID) {

        PeptideData peptideData = parseModifiedPeptide(modifiedPeptide);

        ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();

        for (LibraryModification modification : peptideData.modifications) {

            String ptmName = resolvePtmName(modification, peptideData.sequence);

            if (ptmName == null) {
                continue;
            }

            modificationMatches.add(new ModificationMatch(ptmName, true, Math.max(modification.site, 1)));
        }

        Peptide peptide = new Peptide(peptideData.sequence, modificationMatches);

        PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(Charge.PLUS, charge), 0);

        SpectrumMatch spectrumMatch = new SpectrumMatch(String.valueOf(libID));

        spectrumMatch.addHit(0, peptideAssumption, false);
        spectrumMatch.setBestPeptideAssumption(peptideAssumption);

        return spectrumMatch;
    }

    /**
     * Return the name of the factory PTM the modification label stands for, null when the label
     * cannot be resolved.
     *
     * <p>The factory is filled from unimod.xml by {@code ImportPTMsFromUnimod}, which names every
     * PTM "&lt;UniMod title&gt; of &lt;site&gt;" and keeps the UniMod accession and title in its cv
     * term, so both notations Carafe writes are looked up in the same index: the accession
     * ({@code UniMod:737}) for the DIA-NN format and the title ({@code TMT6plex},
     * {@code Carbamidomethyl (C)} once the site is stripped) for the other two.</p>
     *
     * @param modification Modification found in the peptide
     * @param sequence Peptide sequence
     * @return String
     */
    String resolvePtmName(LibraryModification modification, String sequence) {

        if (labelToPtmName.isEmpty()) {
            indexPtmFactory();
        }

        String label = modification.label;

        // "Carbamidomethyl (C)" -> "Carbamidomethyl", the site is already known from the position
        int siteStart = label.indexOf(" (");
        if (siteStart > 0 && label.endsWith(")")) {
            label = label.substring(0, siteStart);
        }
        if (MODIFICATION_ALIASES.containsKey(label)) {
            label = MODIFICATION_ALIASES.get(label);
        }

        String ptmName = null;
        if (modification.site == 0) {
            // the site a terminal PTM is named after depends on where the factory was filled from:
            // unimod.xml gives "<title> of N-term", the modifications built into the factory are
            // named "<title> of peptide N-term"
            for (String site : NTERM_SITES) {
                ptmName = labelToPtmName.get(getLabelKey(label, site));
                if (ptmName != null) {
                    break;
                }
            }
        } else {
            ptmName = labelToPtmName.get(getLabelKey(label, String.valueOf(sequence.charAt(modification.site - 1))));
        }

        if (ptmName == null && unknownLabels.add(modification.label)) {
            System.err.println("Modification " + modification.label + " of the spectral library matches no known modification, it is left out of the peptide.");
        }

        return ptmName;
    }

    /**
     * Index the PTMs of the factory by UniMod accession and by UniMod title, both per site
     */
    private void indexPtmFactory() {

        for (String ptmName : ptmFactory.getPTMs()) {

            int siteStart = ptmName.lastIndexOf(" of ");
            if (siteStart == -1) {
                continue;
            }
            String site = ptmName.substring(siteStart + 4);

            CvTerm cvTerm = ptmFactory.getPTM(ptmName).getCvTerm();
            if (cvTerm == null) {
                continue;
            }

            if (cvTerm.getAccession() != null) {
                labelToPtmName.putIfAbsent(getLabelKey(cvTerm.getAccession(), site), ptmName);
            }
            if (cvTerm.getName() != null) {
                labelToPtmName.putIfAbsent(getLabelKey(cvTerm.getName(), site), ptmName);
            }
        }
    }

    /**
     * Return the key a modification label is indexed under. UniMod accessions are upper cased
     * because the library writes "UniMod:737" where unimod.xml gives "UNIMOD:737".
     * @param label Modification label or UniMod accession
     * @param site Modification site
     * @return String
     */
    private static String getLabelKey(String label, String site) {
        return label.toUpperCase() + "|" + site;
    }

    /**
     * Split a modified peptide of the library into its sequence and its modifications.
     *
     * <p>The sequence is wrapped in underscores and each modification follows the residue it sits
     * on, in round brackets ({@code _AC(UniMod:4)DEK_}) or in square brackets
     * ({@code _ACC[Carbamidomethyl]DEK_}) depending on the modification and on the library format.
     * A modification in front of the first residue ({@code _(UniMod:737)SGGGGGR_}) is on the
     * peptide terminus and gets site 0; the others get the one based position of their residue.</p>
     *
     * @param modifiedPeptide Peptide with its modifications
     * @return PeptideData
     */
    static PeptideData parseModifiedPeptide(String modifiedPeptide) {

        StringBuilder sequence = new StringBuilder();
        ArrayList<LibraryModification> modifications = new ArrayList<>();

        String value = modifiedPeptide.trim();
        if (value.startsWith("_")) {
            value = value.substring(1);
        }
        if (value.endsWith("_")) {
            value = value.substring(0, value.length() - 1);
        }

        int index = 0;

        while (index < value.length()) {

            char current = value.charAt(index);

            if (current == '(' || current == '[') {

                int closeIndex = value.indexOf(current == '(' ? ')' : ']', index);
                if (closeIndex == -1) {
                    break;
                }

                modifications.add(new LibraryModification(value.substring(index + 1, closeIndex), sequence.length()));
                index = closeIndex + 1;

            } else {

                if (Character.isLetter(current)) {
                    sequence.append(Character.toUpperCase(current));
                }
                index++;
            }
        }

        return new PeptideData(sequence.toString(), modifications);
    }

    /**
     * Return the index of every column of the header
     * @param headerLine Header line of the library file
     * @return HashMap
     */
    static HashMap<String, Integer> getColumnIndexes(String headerLine) {

        HashMap<String, Integer> columnIndexes = new HashMap<>();

        // a byte order mark would otherwise stick to the first header and hide that column
        if (headerLine.startsWith("﻿")) {
            headerLine = headerLine.substring(1);
        }

        String[] headers = headerLine.split("\t");
        for (int index = 0; index < headers.length; index++) {
            columnIndexes.put(headers[index].trim().toLowerCase(), index);
        }

        return columnIndexes;
    }

    /**
     * Return the index of the first column of the header the names are spelled as, -1 when none is
     * @param columnIndexes Index of every column of the header
     * @param names Accepted spellings of the column
     * @return int
     */
    static int getColumnIndex(HashMap<String, Integer> columnIndexes, String[] names) {

        for (String name : names) {
            Integer index = columnIndexes.get(name.toLowerCase());
            if (index != null) {
                return index;
            }
        }

        return -1;
    }

    /**
     * Return the index of a column the library cannot be read without
     * @param columnIndexes Index of every column of the header
     * @param names Accepted spellings of the column
     * @return int
     * @throws IOException
     */
    static int getRequiredColumnIndex(HashMap<String, Integer> columnIndexes, String[] names) throws IOException {

        int index = getColumnIndex(columnIndexes, names);

        if (index == -1) {
            throw new IOException("The spectral library file has no " + names[0] + " column.");
        }

        return index;
    }

    /**
     * Serialize one object into the bytes stored in the database
     * @param object Object to serialize
     * @return byte array
     * @throws IOException
     */
    private static byte[] serialize(Object object) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            try {
                objectOutputStream.writeObject(object);
            } finally {
                objectOutputStream.close();
            }
        } finally {
            byteArrayOutputStream.close();
        }

        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Return database name
     * @return String
     */
    public String getDbName() {
        return dbName;
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection() {
        return sqLiteConnection;
    }

    /**
     * One peptide of the library, split into its sequence and its modifications
     */
    static class PeptideData {
        final String sequence;
        final ArrayList<LibraryModification> modifications;

        PeptideData(String sequence, ArrayList<LibraryModification> modifications) {
            this.sequence = sequence;
            this.modifications = modifications;
        }
    }

    /**
     * One modification found in a peptide of the library
     */
    static class LibraryModification {
        /**
         * Label the library writes between the brackets, a UniMod accession or a modification name
         */
        final String label;
        /**
         * One based position of the modified residue, 0 for the peptide N-terminus
         */
        final int site;

        LibraryModification(String label, int site) {
            this.label = label;
            this.site = site;
        }
    }
}

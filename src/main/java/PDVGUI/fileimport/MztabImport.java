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
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MztabImport {

    /**
     * Text Id file
     */
    private File textIdFile;
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
    private String spectrumFileType;
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
     * ProForma peptidoform column index (opt_global_cv_MS:1003169_proforma_peptidoform_sequence),
     * -1 when absent. Casanovo 5.2 puts the bracketed modified peptide here while the plain
     * {@code sequence} column is unmodified, so when present this column is the source of mods.
     */
    private Integer proformaIndex = -1;
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
     * Details information list
     */
    private ArrayList<String> detailsList = new ArrayList<>();
    /**
     * Original information hash
     */
    private HashMap<String, Object> originalInfor = new HashMap<>();
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
     * ProForma interim modification name -> compomics-utilities PTM name stem. ProForma (and
     * Casanovo's bracket labels) use Unimod interim names like "Carbamidomethyl"/"Deamidated"
     * that differ from the utilities PTM names ("Carbamidomethylation"/"Deamidation"); the full
     * residue-specific name is rebuilt as {@code <stem> of <residue>} in {@link #resolvePtmName}.
     */
    private static final HashMap<String, String> PROFORMA_PTM_ALIASES = new HashMap<>();
    static {
        PROFORMA_PTM_ALIASES.put("Carbamidomethyl", "Carbamidomethylation");
        PROFORMA_PTM_ALIASES.put("Deamidated", "Deamidation");
        PROFORMA_PTM_ALIASES.put("Oxidation", "Oxidation");
    }
    /**
     * Casanovo N-terminal ProForma label ("{@code <name> of N-term}", as built by
     * {@link #get_modification}) -> compomics-utilities PTM name. Only the labels that have a
     * factory PTM are mapped here; {@code Ammonia-loss} and Casanovo's numeric {@code [+25.980265]-}
     * have no factory entry and are registered on the fly from their delta mass instead (see
     * {@link #registerableNtermDelta}). Names/masses are taken from Casanovo's config.yaml residues
     * dict: Acetyl +42.010565 (UNIMOD:1), Carbamyl +43.005814 (UNIMOD:5).
     */
    private static final HashMap<String, String> PROFORMA_NTERM_ALIASES = new HashMap<>();
    static {
        PROFORMA_NTERM_ALIASES.put("Acetyl of N-term", "Acetylation of peptide N-term");
        PROFORMA_NTERM_ALIASES.put("Carbamyl of N-term", "Carbamilation of protein N-term");
    }

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param textIdFile Text identification file
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public MztabImport(PDVMainClass pdvMainClass, File textIdFile, String spectrumFileType, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException, IOException {

        this.pdvMainClass = pdvMainClass;
        this.textIdFile = textIdFile;
        this.spectrumFileType = spectrumFileType;
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

                    parseMztabFile();

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
    private void parseMztabFile() throws IOException, SQLException {
        System.out.println("Processing mztab.");

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

        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match OBJECT(50), Modification varchar(200)" + addQuery +", PRIMARY KEY(PSMIndex))";

        try {
            // Drop any leftover table first: when re-loading the same file the <file>.db could
            // not always be deleted (e.g. still open / locked on Windows), which would otherwise
            // make CREATE TABLE fail with "table SpectrumMatch already exists".
            statement.execute("DROP TABLE IF EXISTS SpectrumMatch");
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "An error occurred while creating table SpectrumMatch in database.<br>" + e.getMessage()),
                    "DB Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch: " + e.getMessage());
            System.err.println("CREATE TABLE statement was: " + matchTableQuery);
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
        String currentSpectrumFileName;
        String modificationNames;
        String singleModificationName;
        String sequence;
        String pureSequence;
        Double modificationMass;
        Double pepMz;
        Integer peptideCharge;
        Peptide peptide;

        ArrayList<String> spectrumList = new ArrayList<>();
        ArrayList<ModificationMatch> utilitiesModifications;
        SpectrumMatch currentMatch;
        PeptideAssumption peptideAssumption;

        ByteArrayOutputStream bos;

        int lineCount = 0;
        int count = 0;
        int countRound = 0;

        HashMap<String, String> spectrumFileMap = new HashMap<>();

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (line.startsWith("MTD") && values[1].startsWith("ms_run")){
                spectrumFileMap.put(values[1].split("-")[0], values[2].split("/")[values[2].split("/").length-1]);
            }

            if(!line.startsWith("MTD") && !line.startsWith("PSH")){

                if (count == 0){
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                utilitiesModifications = new ArrayList<>();

                if (Objects.equals(spectrumFileType, "mgf")){
                    spectrumTitle = values[spectrumTitleIndex].split(":index=")[1];
                    currentSpectrumFileName = spectrumFileMap.get(values[spectrumTitleIndex].split(":index=")[0]);
                } else {
                    // mzML
                    // ms_run[1]:scan=1882
                    // ms_run[1]:controllerType=0 controllerNumber=1 scan=7
                    spectrumTitle = values[spectrumTitleIndex].split(":.*scan=")[1];
                    currentSpectrumFileName = spectrumFileMap.get(values[spectrumTitleIndex].split(":.*scan=")[0]);
                }

                sequence = values[sequenceIndex];
                // Prefer the ProForma peptidoform column when present (Casanovo 5.2): it carries the
                // bracketed modifications (e.g. C[Carbamidomethyl]GC...) that the plain sequence
                // column lacks. Falls back to the sequence column when the column is absent, empty,
                // or "null". An unmodified proforma is just the bare sequence, so the bracket branch
                // below is skipped and behaviour is unchanged for those rows.
                if (proformaIndex != -1 && proformaIndex < values.length) {
                    String proforma = values[proformaIndex];
                    if (proforma != null && !proforma.isEmpty() && !proforma.equalsIgnoreCase("null")) {
                        sequence = proforma;
                    }
                }
                peptideCharge = (int) Double.parseDouble(values[chargeIndex]);
                if(sequence.contains("[")) {
                    // [Acetyl]-MAAAAAAAK
                    // PM[Oxidation]AAAAAK
                    // C[Carbamidomethyl]AAAA[Carbamidomethyl]
                    HashMap<String, String> mod_pep_map = get_modification(sequence);
                    pureSequence = mod_pep_map.get("sequence");
                    String mod = mod_pep_map.get("modification");
                    if(mod.contains("@")){
                        String[] mods = mod.split(";");
                        for(String m: mods){
                            String m_name = m.split("@")[0];
                            int eachPos = Integer.parseInt(m.split("@")[1]);
                            // N-term mods are emitted at position 0; compomics anchors them on the
                            // first residue, so move 0 -> 1 (residue mods are already 1-based).
                            if (eachPos == 0) {
                                eachPos = 1;
                            }
                            // ProForma interim names (e.g. "Carbamidomethyl of C", "Deamidated of N",
                            // "Acetyl of N-term") differ from compomics-utilities PTM names
                            // ("Carbamidomethylation of C", "Deamidation of N", "Acetylation of
                            // peptide N-term"); resolve to the factory's name before matching.
                            String resolved = resolvePtmName(m_name);
                            if(resolved != null){
                                // good
                                m_name = resolved;
                                utilitiesModifications.add(new ModificationMatch(m_name, true, eachPos));
                            }else{
                                // No factory PTM (Ammonia-loss, or Casanovo's numeric "[+25.980265]-"):
                                // register it on the fly from its exact delta mass, then use it.
                                Double delta = registerableNtermDelta(m_name);
                                if (delta == null) {
                                    for(String s_mod: ptmFactory.getPTMs()){
                                        System.out.println("Supported modification: "+s_mod);
                                    }
                                    // modification is not present
                                    throw new IllegalArgumentException("unknown modification:"+sequence+" -> "+m_name);
                                }
                                addModification(m_name, delta);
                                utilitiesModifications.add(new ModificationMatch(m_name, true, eachPos));
                            }
                            if (!allModifications.contains(m_name)) {
                                allModifications.add(m_name);
                            }
                        }
                    }
                    modificationNames = mod;

                }else{
                    modificationNames = values[modificationIndex];
                    // [Acetyl]-MAAAAAAAK
                    // PM[Oxidation]AAAAAK
                    // C[Carbamidomethyl]AAAA[Carbamidomethyl]
                    pureSequence = sequence.replaceAll("[^A-Za-z]+", "");

                    HashMap<Integer, Double> pos2delta = new HashMap<Integer, Double>();

                    if (sequence.contains("+") || sequence.contains("-")) {

                        // +42.011-17.33AAAAAA+18.88-190.88PPSSSSSSSSSDWFSLDDPVR+9.019
                        // int nmod = countOccurrencesOf(pep,"+") + countOccurrencesOf(pep,"-");
                        Pattern pattern = Pattern.compile("[+-]\\d+");
                        ArrayList<String> matches = new ArrayList<String>();
                        Matcher m = Pattern.compile("[+-][.0-9]+").matcher(sequence);
                        while (m.find()) {
                            matches.add(m.group(0));
                        }
                        int nmod = matches.size();
                        for (int i = 0; i < nmod; i++) {
                            int pos = sequence.indexOf(matches.get(i));
                            sequence = sequence.replaceFirst(Pattern.quote(matches.get(i)), "");
                            double delta_mass = Double.valueOf(matches.get(i));
                            if (pos2delta.containsKey(pos)) {
                                pos2delta.put(pos, pos2delta.get(pos) + delta_mass);
                            } else {
                                pos2delta.put(pos, delta_mass);
                            }
                        }
                    }

                    if (pos2delta.size() != 0) {
                        String modAA;
                        for (int eachPos : pos2delta.keySet()) {
                            if (eachPos == 0) {
                                modAA = "N-term";
                            } else {
                                modAA = pureSequence.split("")[eachPos - 1];
                            }
                            singleModificationName = pos2delta.get(eachPos) + " of " + modAA;
                            addModification(singleModificationName, pos2delta.get(eachPos));

                            if (!allModifications.contains(singleModificationName)) {
                                allModifications.add(singleModificationName);
                            }

                            if (eachPos == 0) {
                                eachPos = 1;
                            }
                            utilitiesModifications.add(new ModificationMatch(singleModificationName, true, eachPos));
                        }
                    }
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(currentSpectrumFileName, spectrumTitle));
                currentMatch.setSpectrumNumber(Integer.valueOf(spectrumTitle));

                peptide = new Peptide(pureSequence, utilitiesModifications);

                peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, peptideCharge), 0, "*");

                if (mzIndex != -1){
                    if (Objects.equals(values[mzIndex], "nan")){
                        pepMz = 0.0;
                    } else {
                        pepMz = Double.valueOf(values[mzIndex]);
                    }
                } else {
                    pepMz = peptideAssumption.getTheoreticMz();
                }

                currentMatch.addHit(0, peptideAssumption, false);
                currentMatch.setBestPeptideAssumption(peptideAssumption);

                spectrumList.add(String.valueOf(lineCount));

                // Map the verbatim mzTab spectra_ref to this PSM's row key so an external caller
                // (e.g. CasanovoGUI) can locate the row by the exact spectra_ref it read from the
                // same file. Stored verbatim (no parsing/normalization) to stay robust across mzTab
                // dialects, and guarded so a short/odd row can never break the import.
                if (spectrumTitleIndex < values.length) {
                    pdvMainClass.spectraRefToSpectrumIndex.putIfAbsent(values[spectrumTitleIndex], String.valueOf(lineCount));
                }

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
                preparedStatement.setString(4, pureSequence);
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
                lineCount ++;}
        }bufferedReader.close();

        if(count != 0){

            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();
            System.out.println("Import DB");

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
        System.out.println("Done all Import.");

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");

        // Whole mzTab is now imported: allSpectrumIndex and spectraRefToSpectrumIndex are
        // fully populated, so the optional control server may now serve /select requests.
        pdvMainClass.setLoadComplete(true);
    }

    /**
     * Get modification and peptide sequence
     * @param peptideWithMods peptide sequence with modification
     * @return A hashmap with modification and sequence information.
     */
    public HashMap<String,String> get_modification(String peptideWithMods) {
        if (peptideWithMods == null) {
            throw new IllegalArgumentException("peptideWithMods is null");
        }

        String s = peptideWithMods.trim();
        int n = s.length();
        int i = 0;

        List<String> mods = new ArrayList<>();
        StringBuilder clean = new StringBuilder();

        // 1) Optional N-term mod: [Something]-...
        if (i < n && s.charAt(i) == '[') {
            int close = s.indexOf(']', i + 1);
            if (close > i) {
                String modName = s.substring(i + 1, close).trim();
                int after = close + 1;
                if (after < n && s.charAt(after) == '-') {
                    if (!modName.isEmpty()) {
                        mods.add(modName + " of N-term@0");  // N-term stays at 0
                    }
                    i = after + 1;
                }
            }
        }

        // 2) Parse residues and residue-attached mods: X[Mod]...
        while (i < n) {
            char c = s.charAt(i);

            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }

            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException(
                        "Unexpected character '" + c + "' at index " + i + " in: " + peptideWithMods
                );
            }

            clean.append(c);
            int pos1 = clean.length();  // 1-based position (first residue = 1)
            i++;

            while (i < n && s.charAt(i) == '[') {
                int close = s.indexOf(']', i + 1);
                if (close < 0) {
                    throw new IllegalArgumentException("Unclosed '[' starting at index " + i + " in: " + peptideWithMods);
                }
                String modName = s.substring(i + 1, close).trim();
                if (!modName.isEmpty()) {
                    mods.add(modName + " of " + c + "@" + pos1);  // Now 1-based
                }
                i = close + 1;
            }
        }

        if (mods.isEmpty()) {
            mods.add("-");
        }

        HashMap<String,String> res = new HashMap<>();
        res.put("modification", StringUtils.join(mods,';'));
        res.put("sequence", clean.toString());
        return res;
    }

    /**
     * Resolve a "{@code <name> of <residue>}" modification (as built by {@link #get_modification})
     * to an existing compomics-utilities PTM name. Tries the name verbatim first (covers mods whose
     * ProForma label already matches, e.g. "Oxidation of M"), then retries with the ProForma->
     * utilities alias from {@link #PROFORMA_PTM_ALIASES} (e.g. "Carbamidomethyl of C" ->
     * "Carbamidomethylation of C"). Returns the resolved PTM name, or {@code null} if none matches.
     * @param nameOfResidue modification name in "{@code <name> of <residue>}" form
     * @return the matching PTM factory name, or null
     */
    private String resolvePtmName(String nameOfResidue) {
        if (ptmFactory.containsPTM(nameOfResidue)) {
            return nameOfResidue;
        }
        // N-terminal mods: the residue label is "N-term" but the factory uses "peptide/protein
        // N-term", so map the full name directly rather than via the residue-stem alias.
        String nterm = PROFORMA_NTERM_ALIASES.get(nameOfResidue);
        if (nterm != null && ptmFactory.containsPTM(nterm)) {
            return nterm;
        }
        int idx = nameOfResidue.lastIndexOf(" of ");
        if (idx < 0) {
            return null;
        }
        String alias = PROFORMA_PTM_ALIASES.get(nameOfResidue.substring(0, idx));
        if (alias != null) {
            String candidate = alias + " of " + nameOfResidue.substring(idx + 4);
            if (ptmFactory.containsPTM(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Delta mass for an N-terminal ProForma mod that has no compomics PTM and must be registered on
     * the fly: {@code Ammonia-loss} (-17.026549, UNIMOD:385) and Casanovo's numeric combined label
     * {@code [+25.980265]-} (written by {@link #get_modification} as "{@code +25.980265 of N-term}").
     * @param nameOfResidue modification name in "{@code <name> of <residue>}" form
     * @return the delta mass, or {@code null} if the name is neither Ammonia-loss nor numeric
     */
    private Double registerableNtermDelta(String nameOfResidue) {
        int idx = nameOfResidue.lastIndexOf(" of ");
        String stem = idx < 0 ? nameOfResidue : nameOfResidue.substring(0, idx);
        if (stem.equalsIgnoreCase("Ammonia-loss")) {
            return -17.026549;
        }
        try {
            return Double.valueOf(stem);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void addModification(String singleModificationName, double modificationMass){
        if (!ptmFactory.containsPTM(singleModificationName)){
            String modificationName = singleModificationName.split(" of ")[0];
            ArrayList<String> residues = new ArrayList<>();
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
    }

    /**
     * Get all parameters
     * @throws IOException
     */
    private void getParameters() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(textIdFile));

        String line;
        String[] values;
        HashMap<String, String> parameterMap = new HashMap<>();

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (!line.startsWith("MTD")) {
                for (int index = 0; index < values.length; index++) {
                    switch (values[index]) {
                        case "spectra_ref":
                            spectrumTitleIndex = index;
                            indexToName.put(index, "spectra_ref");
                            break;
                        case "sequence":
                            sequenceIndex = index;
                            indexToName.put(index, "sequence_in_mztab");
                            break;
                        case "charge":
                            chargeIndex = index;
                            break;
                        case "modifications":
                            modificationIndex = index;
                            indexToName.put(index, "modifications");
                            break;
                        case "calc_mass_to_charge":
                            mzIndex = index;
                            indexToName.put(index,  values[index]);
                            break;
                        //Do nothing
                        default:
//                            indexToName.put(index, "Test" + index);
                            // Casanovo ProForma peptidoform column: the bracketed modified peptide
                            // (e.g. C[Carbamidomethyl]GC...) used to recover modifications. The header
                            // name differs by version -- "opt_global_cv_MS:1003169_proforma_
                            // peptidoform_sequence" (5.2) vs "opt_ms_run[1]_proforma" (5.1.x) -- so
                            // match on the shared "proforma" substring.
                            if (values[index].toLowerCase().contains("proforma")) {
                                proformaIndex = index;
                            }
                            indexToName.put(index, "`" + values[index] + "`");
                            break;
                    }}
                break;}
            else {
                detailsList.add(line.split("\t")[1] + "/t/" + line.split("\t")[2]);
                originalInfor.put(line.split("\t")[1], line.split("\t")[2]);
            }
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

    /**
     * Get result detail list
     * @return ArrayList
     */
    public ArrayList<String> getDetailsList(){
        return detailsList;
    }
    /**
     * Return original information
     * @return Hash map
     */
    public HashMap<String, Object> getOriginalInfor(){
        return originalInfor;
    }
}

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
                            if(ptmFactory.containsPTM(m_name)){
                                // good
                                utilitiesModifications.add(new ModificationMatch(m_name, true, eachPos));
                            }else if(m_name.equalsIgnoreCase("Ammonia-loss of N-term")) {
                                // Add this modification
                                addModification(m_name, -17.026549);
                            }else{
                                for(String s_mod: ptmFactory.getPTMs()){
                                    System.out.println("Supported modification: "+s_mod);
                                }
                                // modification is not present
                                throw new IllegalArgumentException("unknown modification:"+sequence+" -> "+m_name);
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

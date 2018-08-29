package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Import PepXML file
 *
 * Created by Ken on 10/17/2017.
 */
public class PepXMLFileImport {

    /**
     * Original information hash
     */
    private HashMap<String, Object> originalInfor = new HashMap<>();
    /**
     * Details list
     */
    private ArrayList<String> detailsList = new ArrayList<>();
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * The score map saved in DB
     */
    private HashMap<String, String> scoreMap = new HashMap<>();
    /**
     * All kinds fo score names list
     */
    private ArrayList<String> scoreName = new ArrayList<>();
    /**
     * Modification mass to mass difference map
     */
    private HashMap<Double, Double> modMassToMassDif = new HashMap<>();
    private HashMap<Double, String> modMassToName = new HashMap<>();
    /**
     * Map stores term modification
     */
    private HashMap<Double, String> termModMassToTerm = new HashMap<>();
    /**
     * Fixed modification list
     */
    private ArrayList<String> fixedModificationAAs = new ArrayList<>();
    /**
     * Modification mass map from unimod
     */
    private HashMap<String,HashMap<Double, String >> modificationMass;
    /**
     * Spectrum matches map saving in DB
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum file name
     */
    private String spectrumFileName;
    /**
     * PepXMLFile imported
     */
    private File pepXMLFile;
    /**
     * The identification search engine
     */
    private String searchEngine = "";
    /**
     * The search Engine Version
     */
    private String searchEngineVersion = "";
    /**
     * Boolean indicate mgf file
     */
    private Boolean existNoMatchFile = false;
    /**
     * Spectrum xmlPullParserFactory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * PTM factory
     */
    PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Object saving spectrum
     */
    private ScanCollectionDefault scans;
    /**
     * Spectrum type
     */
    private String spectrumFileType;
    /**
     * For PTNProphet
     */
    DecimalFormat massDF = new DecimalFormat("#.0000");
    /**
     * For crux modification mass
     */
    DecimalFormat cruxDF = new DecimalFormat("#.00");
    /**
     * Spectrum ID to spectrum number
     */
    private HashMap<String, Integer> spectrumIdAndNumber;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param spectrumFileName Spectrum file name
     * @param pepXMLFile PepXML file
     * @param modificationMass modification mass
     * @param spectrumFactory Spectrum xmlPullParserFactory
     * @param scans Object saving spectrum
     * @param spectrumFileType Spectrum file type
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws XmlPullParserException
     */
    public PepXMLFileImport(PDVMainClass pdvMainClass, String spectrumFileName, File pepXMLFile, HashMap<String,HashMap<Double, String >> modificationMass,
                            SpectrumFactory spectrumFactory, ScanCollectionDefault scans, String spectrumFileType, ProgressDialogX progressDialog, HashMap<String, Integer> spectrumIdAndNumber)
            throws SQLException, ClassNotFoundException, IOException, XmlPullParserException {

        this.pdvMainClass = pdvMainClass;
        this.pepXMLFile = pepXMLFile;
        this.spectrumFileName = spectrumFileName;
        this.modificationMass = modificationMass;
        this.spectrumFactory = spectrumFactory;
        this.scans = scans;
        this.spectrumFileType = spectrumFileType;
        this.progressDialog = progressDialog;
        this.spectrumIdAndNumber = spectrumIdAndNumber;

        String dbName = pepXMLFile.getParentFile().getAbsolutePath()+"/"+ pepXMLFile.getName()+".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);

        getScoreName();

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parsePepXML();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse pepXML file, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing pepXML file
     * @throws XmlPullParserException
     * @throws IOException
     * @throws SQLException
     */
    private void parsePepXML() throws XmlPullParserException, SQLException {

        sqLiteConnection.setScoreNum(scoreName.size());

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);

        Statement statement = connection.createStatement();

        PreparedStatement preparedStatement = null;

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?");

        for (String name : scoreName){
            addQuery.append(", ").append(name).append(" OBJECT(50)");
            addValuesQuery.append(",?");
        }
        addValuesQuery.append(")");

        String addTableQuery = "CREATE TABLE SpectrumMatch" + " (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object" + addQuery +", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            System.err.println("An error occurred while creating table SpectrumMatch");
            JOptionPane.showMessageDialog(
                    null, "Failed to connect database.",
                    "Error Connecting to DB", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
            e.printStackTrace();
        }finally {
            statement.close();
        }

        ArrayList<String> spectrumIndexList = new ArrayList<>();

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        xmlPullParserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();

        originalInfor.put("Identification file", pepXMLFile.getAbsolutePath());
        detailsList.add("Identification file/t/"+pepXMLFile.getName());
        originalInfor.put("Spectrum File", spectrumFileName);
        detailsList.add("Spectrum file/t/"+spectrumFileName);

        int count = 0;
        int countRound = 0;
        Pattern pattern = Pattern.compile("-?[0-9]+.?[0-9]+");

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(pepXMLFile))) {
            xmlPullParser.setInput(bufferedReader);

            int tagNum;
            boolean hasMatch = false;

            int first = 0;

            SpectrumMatch currentMatch = null;
            Integer currentCharge = null;
            String scanNum = null;
            String spectrumTitle = "";
            Integer index = null;

            modMassToMassDif = new HashMap<>();
            termModMassToTerm = new HashMap<>();

            while ((tagNum = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                if (count == 0) {
                    String addDataQuery = "INSERT INTO SpectrumMatch " + addValuesQuery;

                    preparedStatement = connection.prepareStatement(addDataQuery);
                }

                String tagName = xmlPullParser.getName();
                //get spectrum file detail
                if (tagNum == XmlPullParser.START_TAG && tagName.equals("msms_run_summary")) {
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);
                        if (name.equals("base_name")) {

                            if (!spectrumFileName.contains(xmlPullParser.getAttributeValue(i))) {
                                existNoMatchFile = true;
                            }
                        }
                    }
                }

                /*
                if (existNoMatchFile) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "The spectrum file cannot match with the MS/MS file in pepXML.",
                            "Error Matching", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    System.err.println("No matching file");
                    break;
                }*/

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("search_summary")) {

                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);
                        if (name.equals("search_engine")) {
                            searchEngine = xmlPullParser.getAttributeValue(i);
                        } else if (name.equals("search_engine_version")) {
                            searchEngineVersion = xmlPullParser.getAttributeValue(i);
                        }
                    }

                    originalInfor.put("Soft Name", searchEngine + " version: " + searchEngineVersion);
                    detailsList.add("Software/t/" + searchEngine + "(" + searchEngineVersion + ")");

                    int xmlType;

                    HashMap<String, String> fixedModiMap = new HashMap<>();
                    HashMap<String, String> variableModiMap = new HashMap<>();

                    while ((xmlType = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                        if (xmlType == XmlPullParser.END_TAG && xmlPullParser.getName() != null) {
                            if (xmlPullParser.getName().equals("search_summary")) {
                                break;
                            }
                        }

                        if (xmlType == XmlPullParser.START_TAG) {

                            String tagName1 = xmlPullParser.getName();

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("aminoacid_modification")) {

                                Character aminoAcid = null;
                                Boolean variable = null;
                                Double massDiff = null;
                                Double mass = null;
                                String terminus = null;
                                String description = null;
                                String terminusName = null;

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);
                                    if (name.equals("aminoacid")) {
                                        aminoAcid = xmlPullParser.getAttributeValue(i).charAt(0);
                                    } else if (name.equals("massdiff")) {
                                        massDiff = new Double(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("mass")) {
                                        mass = new Double(xmlPullParser.getAttributeValue(i));

                                        if (scoreName.contains("ptmprophet_result")){
                                            mass = Double.valueOf(massDF.format(mass));
                                        }

                                    } else if (name.equals("variable")) {
                                        String variableString = xmlPullParser.getAttributeValue(i);
                                        if (variableString.equalsIgnoreCase("Y")) {
                                            variable = true;
                                        } else if (variableString.equalsIgnoreCase("N")) {
                                            variable = false;
                                        }
                                    } else if (name.equals("terminus")) {
                                        terminusName = name;
                                        String terminusString = xmlPullParser.getAttributeValue(i);
                                        if (terminusString.equalsIgnoreCase("N") || terminusString.equalsIgnoreCase("C")) {
                                            terminus = terminusString;
                                        }
                                    } else if (name.equals("description")) {
                                        description = xmlPullParser.getAttributeValue(i);
                                    }
                                }

                                if (variable != null && massDiff != null && mass != null && aminoAcid != null) {
                                    String ptmName;
                                    ArrayList<String> residues = new ArrayList<>();
                                    if (description == null){
                                        description = "[" + massDiff + "]";
                                    }

                                    ptmName = description + " of " + aminoAcid;

                                    if (!ptmFactory.containsPTM(ptmName)){

                                        residues.add(String.valueOf(aminoAcid));

                                        PTM ptm = new PTM(PTM.MODAA, ptmName, massDiff, residues);
                                        ptm.setShortName(description);

                                        ptmFactory.addUserPTM(ptm);
                                    }

                                    if (!variable) {
                                        if (terminusName == null) {
                                            fixedModiMap.put(description + " of " + aminoAcid, " massDelta: " + massDiff);
                                            fixedModificationAAs.add(String.valueOf(aminoAcid).toLowerCase());
                                        } else {
                                            termModMassToTerm.put(mass, terminus);
                                            fixedModificationAAs.add(String.valueOf(aminoAcid).toLowerCase());
                                            fixedModiMap.put(description + " on " + terminusName + " of " + aminoAcid + " " + terminus.toUpperCase() + "-term", " massDelta: " + massDiff);
                                        }

                                    } else {
                                        if (terminusName == null) {
                                            variableModiMap.put(description + " of " + aminoAcid, " massDelta: " + massDiff);
                                        } else {
                                            termModMassToTerm.put(mass, terminus);
                                            variableModiMap.put(description + " on " + terminusName + " of " + aminoAcid + " " + terminus.toUpperCase() + "-term", " massDelta: " + massDiff);
                                        }
                                    }

                                    if (searchEngine.toLowerCase().contains("crux")){
                                        modMassToName.put((double) Math.round(mass * 100) / 100, ptmName);
                                        modMassToName.put((double) Math.round((mass + 0.001) * 100) / 100 , ptmName);
                                        modMassToName.put((double) Math.round((mass - 0.001) * 100) / 100 , ptmName);
                                    }

                                    modMassToName.put(mass, ptmName);
                                } else {
                                    throw new IllegalArgumentException("An error occurred while parsing aminoacid_modification element. Missing values.");
                                }

                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("terminal_modification")) {

                                Boolean variable = null;
                                Boolean proteinTerm = false;
                                Double mass = null;
                                String terminus = null;
                                String description = null;
                                Double massDiff = null;

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);
                                    if (name.equals("terminus")) {
                                        String terminusString = xmlPullParser.getAttributeValue(i);
                                        if (terminusString.equalsIgnoreCase("N") || terminusString.equalsIgnoreCase("C")) {
                                            terminus = terminusString;
                                        }
                                    } else if (name.equals("protein_terminus")) {
                                        String proteinTermString = xmlPullParser.getAttributeValue(i);
                                        if (proteinTermString.equalsIgnoreCase("Y")){
                                            proteinTerm = true;
                                        } else if (proteinTermString.equalsIgnoreCase("N")){
                                            proteinTerm = false;
                                        }
                                    } else if (name.equals("mass")) {
                                        mass = new Double(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("massdiff")) {
                                        massDiff = new Double(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("variable")) {
                                        String variableString = xmlPullParser.getAttributeValue(i);
                                        if (variableString.equalsIgnoreCase("Y")) {
                                            variable = true;
                                        } else if (variableString.equalsIgnoreCase("N")) {
                                            variable = false;
                                        }
                                    } else if (name.equals("description")) {
                                        description = xmlPullParser.getAttributeValue(i);
                                    }
                                }

                                if (variable != null && mass != null && terminus != null) {

                                    String ptmName;
                                    if (description == null){
                                        description = "[" + massDiff + "]";
                                    }

                                    if (terminus.equalsIgnoreCase("N")){
                                        if (proteinTerm){
                                            ptmName = description + " of Protein N-term";
                                            if (!ptmFactory.containsPTM(ptmName)) {
                                                System.out.println("Ptm is "+ptmName);
                                                PTM ptm = new PTM(PTM.MODN, ptmName, massDiff, null);
                                                ptm.setShortName(description);
                                                ptmFactory.addUserPTM(ptm);
                                            }
                                        } else {
                                            ptmName = description + " of N-term";
                                            if (!ptmFactory.containsPTM(ptmName)) {
                                                PTM ptm = new PTM(PTM.MODNP, ptmName, massDiff, null);
                                                ptm.setShortName(description);
                                                ptmFactory.addUserPTM(ptm);
                                            }
                                        }
                                    } else {
                                        if (proteinTerm){
                                            ptmName = description + " of Protein C-term";
                                            if (!ptmFactory.containsPTM(ptmName)) {
                                                PTM ptm = new PTM(PTM.MODC, ptmName, massDiff, null);
                                                ptm.setShortName(description);
                                                ptmFactory.addUserPTM(ptm);
                                            }
                                        } else {
                                            ptmName = description + " of C-term";
                                            if (!ptmFactory.containsPTM(ptmName)) {
                                                PTM ptm = new PTM(PTM.MODCP, ptmName, massDiff, null);
                                                ptm.setShortName(description);
                                                ptmFactory.addUserPTM(ptm);
                                            }
                                        }
                                    }

                                    if (!variable) {
                                        if (terminus.equalsIgnoreCase("N")) {
                                            fixedModificationAAs.add("N-term");
                                            fixedModiMap.put(description + " on N-term", " massDelta: " + massDiff);
                                            mass = 0.0;
                                        } else {
                                            fixedModificationAAs.add("C-term");
                                            fixedModiMap.put(description + " on C-term", " massDelta: " + massDiff);
                                            mass = 1.0;
                                        }
                                    } else {
                                        if (terminus.equalsIgnoreCase("N")) {
                                            variableModiMap.put(description + " on N-term", " massDelta: " + massDiff);
                                            mass = 0.0;
                                        } else {
                                            variableModiMap.put(description + " on C-term", " massDelta: " + massDiff);
                                            mass = 1.0;
                                        }
                                    }

                                    modMassToName.put(mass, ptmName);
                                } else {
                                    throw new IllegalArgumentException("An error occurred while parsing terminal_modification element. Missing values.");
                                }
                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("search_database")) {
                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("local_path")) {
                                        originalInfor.put("Database", xmlPullParser.getAttributeValue(i));
                                        detailsList.add("Database/t/" + xmlPullParser.getAttributeValue(i));
                                    }
                                }
                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("enzymatic_search_constraint")) {
                                String enzyme = null;
                                String cleavages = null;
                                String minNum = null;
                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("enzyme")) {
                                        enzyme = xmlPullParser.getAttributeValue(i);
                                    }
                                    if (name.equals("max_num_internal_cleavages")) {
                                        cleavages = xmlPullParser.getAttributeValue(i);
                                    }
                                    if (name.equals("min_number_termini")){
                                        minNum = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                if (enzyme != null) {
                                    originalInfor.put("Enzyme", enzyme);
                                    originalInfor.put("Max number internal cleavages", cleavages);
                                    originalInfor.put("Min number termini", minNum);
                                    detailsList.add("Enzyme/t/" + enzyme);
                                    detailsList.add("Max number internal cleavages/t/" + cleavages);
                                    detailsList.add("Min number termini/t/" + minNum);
                                }
                            }
                        }
                    }

                    int count1 = 0;
                    for (String name : fixedModiMap.keySet()) {

                        detailsList.add("Fixed modification: " + count1 + "/t/" + name + "(" + fixedModiMap.get(name) + ")");
                        count1++;
                    }
                    count1 = 0;
                    for (String name : variableModiMap.keySet()) {

                        detailsList.add("Variable modification: " + count1 + "/t/" + name + "(" + variableModiMap.get(name) + ")");
                        count1++;
                    }

                    originalInfor.put("Fixed modification", fixedModiMap);
                    originalInfor.put("Variable modification", variableModiMap);
                }

                //Spectrum details
                if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("SpectraData")) {
                    String location = null;
                    String id = null;

                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {

                        String attributeName = xmlPullParser.getAttributeName(i);

                        if (attributeName.equalsIgnoreCase("location")) {
                            location = xmlPullParser.getAttributeValue(i);
                        } else if (attributeName.equalsIgnoreCase("id")) {
                            id = xmlPullParser.getAttributeValue(i);
                        }
                    }

                    if (location != null && id != null) {

                        String fileName = location;

                        if (location.lastIndexOf("/") != -1) {
                            fileName = location.substring(location.lastIndexOf("/") + 1);
                        } else if (location.lastIndexOf("\\") != -1) {
                            fileName = location.substring(location.lastIndexOf("\\") + 1);
                        }

                    }

                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("spectrum_query")) {

                    index = null;
                    hasMatch = false;
                    String spectrumId = null;
                    String spectrumNativeID = null;

                    first = 0;

                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);
                        if (name.equals("spectrum")) {
                            spectrumId = xmlPullParser.getAttributeValue(i);
                        } else if (name.equals("index")) {
                            String value = xmlPullParser.getAttributeValue(i);
                            try {
                                index = new Integer(value.trim());
                            } catch (Exception e) {
                                throw new IllegalArgumentException("An error occurred while parsing index " + value + ". Integer expected.");
                            }
                        } else if (name.equals("spectrumNativeID")) {
                            spectrumNativeID = xmlPullParser.getAttributeValue(i).trim();
                        } else if (name.equals("start_scan")) {
                            scanNum = xmlPullParser.getAttributeValue(i);
                        }
                    }

                    if (index == null) {
                        throw new IllegalArgumentException("No index found for spectrum " + spectrumId + ".");
                    }

                    if (spectrumNativeID != null) {
                        spectrumTitle = spectrumNativeID;
                    } else if (spectrumId != null){
                        spectrumTitle = spectrumId;
                    } else {
                        spectrumTitle = scanNum + "";
                    }

                    Integer spectrumIndex = Integer.valueOf(scanNum);

                    SpectrumMatch spectrumMatch;

                    if (spectrumFileType.equals("mgf")){
                        if (searchEngine.toLowerCase().contains("crux")){
                            spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(scanNum));
                        } else if (searchEngine.toLowerCase().contains("myrimatch")){
                            spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(scanNum) + 1);
                        }
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));
                    } else if (spectrumFileType.equals("mzml")){
                        if (spectrumNativeID == null){
                            if (searchEngine.toLowerCase().contains("mascot")){ // scan number is wrong
                                spectrumNativeID = spectrumTitle;
                            } else {
                                spectrumNativeID = "controllerType=0 controllerNumber=1 scan=" + scanNum;
                            }
                        }
                        spectrumIndex = spectrumIdAndNumber.get(spectrumNativeID);
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex - 1)));
                        spectrumTitle = String.valueOf(spectrumIndex - 1);
                    } else {
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex - 1)));

                    }

                    spectrumMatch.setSpectrumNumber(spectrumIndex);

                    currentMatch = spectrumMatch;

                    SpectrumMatch previousMatch = spectrumMatchesMap.get(currentMatch.getKey());
                    if (previousMatch != null) {
                        currentMatch = previousMatch;
                    }
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("assumed_charge")) {
                            String value = xmlPullParser.getAttributeValue(i);
                            try {
                                currentCharge = new Integer(value.trim());
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Charge " + value + " could not be parsed. Integer expected.");
                            }
                        }
                    }
                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("search_hit")) {
                    first++;
                    if (currentMatch == null) {
                        throw new IllegalArgumentException("No spectrum match when parsing search hit.");
                    }
                    if (currentCharge == null) {
                        throw new IllegalArgumentException("No charge found when parsing search hit of spectrum " + currentMatch.getKey() + ".");
                    }
                    PeptideAssumption peptideAssumption = getPeptideAssumption(xmlPullParser, currentCharge);
                    Peptide peptide = peptideAssumption.getPeptide();
                    String peptideSequence = peptide.getSequence();
                    hasMatch = true;
                    if (first == 1) {
                        currentMatch.setBestPeptideAssumption(peptideAssumption);
                    }

                    if (AminoAcidSequence.hasCombination(peptideSequence)) {
                        ArrayList<ModificationMatch> previousModificationMatchList = peptide.getModificationMatches(),
                                newModificationMatchList = null;
                        if (previousModificationMatchList != null) {
                            newModificationMatchList = new ArrayList<>(previousModificationMatchList.size());
                        }
                        for (StringBuilder expandedSequence : AminoAcidSequence.getCombinations(peptide.getSequence())) {
                            Peptide newPeptide = new Peptide(expandedSequence.toString(), newModificationMatchList);
                            if (previousModificationMatchList != null) {
                                for (ModificationMatch modificationMatch : previousModificationMatchList) {
                                    newPeptide.addModificationMatch(new ModificationMatch(modificationMatch.getTheoreticPtm(),
                                            modificationMatch.isVariable(), modificationMatch.getModificationSite()));
                                }
                            }
                            PeptideAssumption newAssumption = new PeptideAssumption(newPeptide, peptideAssumption.getRank(),
                                    peptideAssumption.getAdvocate(), peptideAssumption.getIdentificationCharge(),
                                    peptideAssumption.getScore(), peptideAssumption.getIdentificationFile());
                            currentMatch.addHit(1, newAssumption, false);

                        }
                    } else {
                        currentMatch.addHit(1, peptideAssumption, false);
                    }
                }

                if (tagNum == XmlPullParser.END_TAG && tagName.equals("spectrum_query")) {

                    if (hasMatch) {

                        spectrumIndexList.add(String.valueOf(index));

                        String key = currentMatch.getKey();
                        if (!spectrumMatchesMap.containsKey(key)) {
                            spectrumMatchesMap.put(key, currentMatch);
                        }

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
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

                        PeptideAssumption peptideAssumption = currentMatch.getBestPeptideAssumption();

                        preparedStatement.setInt(1, index);
                        preparedStatement.setDouble(2, peptideAssumption.getTheoreticMass() / currentCharge);
                        preparedStatement.setString(3, spectrumTitle);
                        preparedStatement.setString(4, peptideAssumption.getPeptide().getSequence());
                        preparedStatement.setDouble(5, Math.abs(peptideAssumption.getScore()));
                        preparedStatement.setBytes(6, bos.toByteArray());

                        for (String name : scoreMap.keySet()) {
                            if (scoreName.contains(name)) {
                                Integer indexInScore = scoreName.indexOf(name) + 7;

                                String value = scoreMap.get(name);

                                if (pattern.matcher(value).matches()) {
                                    preparedStatement.setDouble(indexInScore, Double.parseDouble(value));
                                } else {
                                    preparedStatement.setString(indexInScore, value);
                                }
                            }
                        }

                        preparedStatement.addBatch();

                        count++;

                        if (count == 1000) {
                            int[] counts = preparedStatement.executeBatch();
                            connection.commit();
                            preparedStatement.close();

                            pdvMainClass.updatePTMSetting();

                            pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                            if (countRound == 0) {

                                pdvMainClass.displayResult();
                                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                                progressDialog.setRunFinished();

                                countRound++;

                            } else {
                                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                                pdvMainClass.buttonCheck();
                                countRound++;
                            }

                            count = 0;

                            spectrumIndexList = new ArrayList<>();
                        }
                        hasMatch = false;
                        currentMatch = null;
                        currentCharge = null;
                    }
                }
            }

            if (count != 0) {
                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                if (countRound == 0) {

                    pdvMainClass.updatePTMSetting();

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

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null, "Failed to parse PepXML file, please check your file.",
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
            progressDialog.setRunFinished();
            e.printStackTrace();
        }
    }

    /**
     * Return peptide assumption of one scan
     * @param xmlPullParser XML xmlPullParser
     * @param charge Charge of the scan
     * @return Peptide assumption
     * @throws XmlPullParserException
     * @throws IOException
     */
    private PeptideAssumption getPeptideAssumption(XmlPullParser xmlPullParser, Integer charge) throws XmlPullParserException, IOException {

        Integer rank = null;
        String sequence = null;
        ArrayList<ModificationMatch> variableModifications = new ArrayList<>();
        ArrayList<ModificationMatch> fixedModifications = new ArrayList<>();
        HashMap<Integer, String> modificationMap = new HashMap<>();
        HashMap<Integer, Double> locationAndScore = new HashMap<>();
        Double score = null;
        scoreMap = new HashMap<>();

        for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
            String name = xmlPullParser.getAttributeName(i);
            if (name.equals("hit_rank")) {
                String value = xmlPullParser.getAttributeValue(i);
                try {
                    rank = new Integer(value.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("An error occurred while parsing rank " + value + ". Integer expected.");
                }
            } else if (name.equals("peptide")) {
                sequence = xmlPullParser.getAttributeValue(i).trim();
            }else if(name.equals("massdiff")){
                score = Double.valueOf(xmlPullParser.getAttributeValue(i).trim());
            }
        }

        int tagNum;
        while ((tagNum = xmlPullParser.next()) != XmlPullParser.START_TAG) {
        }

        while (xmlPullParser.getName().equals("alternative_protein")) {

            while ((tagNum = xmlPullParser.next()) != XmlPullParser.START_TAG) {
            }
        }
        String modificationName = null;
        StringBuilder ptmProphet = null;

        String tagName = xmlPullParser.getName();
        if (tagName.equals("modification_info")) {

            for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                String attributeName = xmlPullParser.getAttributeName(i);

                if (attributeName.equals("mod_nterm_mass") || attributeName.equals("mod_cterm_mass")) {

                    int site;
                    if (attributeName.equals("mod_nterm_mass")) {
                        site = 1;
                        modificationName = modMassToName.get(0.0);

                    } else {
                        site = sequence.length();
                        modificationName = modMassToName.get(1.0);
                    }

                    if (!allModifications.contains(modificationName)){
                        allModifications.add(modificationName);
                    }

                    if (fixedModificationAAs.contains("N-term") || fixedModificationAAs.contains("C-term") ){
                        fixedModifications.add(new ModificationMatch(modificationName, true, site));
                    } else {
                        variableModifications.add(new ModificationMatch(modificationName, true, site));
                    }
                }
            }

            while ((tagNum = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {
                tagName = xmlPullParser.getName();
                int location;
                if (tagName != null) {
                    if (tagName.equals("mod_aminoacid_mass")) {
                        Integer site = null;
                        for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                            String attributeName = xmlPullParser.getAttributeName(i);
                            if (attributeName.equals("position")) {
                                String value = xmlPullParser.getAttributeValue(i);
                                try {
                                    site = new Integer(value);
                                } catch (Exception e) {
                                    throw new IllegalArgumentException("An error occurred while parsing modification position " + value + ". Integer expected.");
                                }
                            }
                        }
                        if (site != null) {
                            Double modifiedAaMass = null;
                            for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                String attributeName = xmlPullParser.getAttributeName(i);
                                if (attributeName.equals("mass")) {
                                    String value = xmlPullParser.getAttributeValue(i);
                                    try {
                                        modifiedAaMass = new Double(value);
                                    } catch (Exception e) {
                                        throw new IllegalArgumentException("An error occurred while parsing modification mass " + value + ". Number expected.");
                                    }
                                }
                            }

                            if (modifiedAaMass != null) {

                                char aa = sequence.charAt(site - 1);

                                location = site;

                                if(termModMassToTerm.containsKey(modifiedAaMass)){
                                    if(termModMassToTerm.get(modifiedAaMass).equals("n")){
                                        modificationName = modMassToName.get(modifiedAaMass);
                                        location = 1;
                                    } else {
                                        modificationName = modMassToName.get(modifiedAaMass);
                                        location = sequence.length();
                                    }
                                } else {
                                    modificationName = modMassToName.get(modifiedAaMass);
                                }

                                if (!allModifications.contains(modificationName)){
                                    allModifications.add(modificationName);
                                }

                                if (fixedModificationAAs.contains(String.valueOf(aa).toLowerCase())){
                                    fixedModifications.add(new ModificationMatch(modificationName, true, location));
                                } else {

                                    modificationMap.put(location, modificationName);
                                    variableModifications.add(new ModificationMatch(modificationName, true, location));
                                }
                            }
                        }
                    } else if (tagNum == XmlPullParser.END_TAG && xmlPullParser.getName().equals("modification_info")) {
                        while ((tagNum = xmlPullParser.next()) != XmlPullParser.START_TAG) {
                        }
                        break;
                    }
                }
            }
        }

        while (tagNum != XmlPullParser.END_DOCUMENT) {
            tagName = xmlPullParser.getName();

            if (tagName != null) {
                if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("search_score")) {
                    String name = null;
                    String value = null;
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("name")) {
                            name = xmlPullParser.getAttributeValue(i).replaceAll("[^a-zA-Z]", ""); // Remove illegal string
                            if(name.length() >= 30){
                                name = name.substring(0, 29);
                            }
                        } else if (attributeName.equals("value")) {
                            value = xmlPullParser.getAttributeValue(i);
                        }
                        if(value != null) {
                            scoreMap.put(name, value);
                        }
                    }
                } else if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("peptideprophet_result")){
                    String value = null;
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("probability")) {
                            value = xmlPullParser.getAttributeValue(i);
                        }
                        if(value != null) {
                            scoreMap.put("peptideprophet_result", value);
                        }
                    }
                } else if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("analysis_result")){
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("analysis")) {
                            if (xmlPullParser.getAttributeValue(i).toLowerCase().equals("ptmprophet")){
                                ptmProphet = new StringBuilder();
                            }
                        }
                    }
                } else if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("ptmprophet_result")){
                    String value = null;
                    String ptm = null;
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("prior")) {
                            value = xmlPullParser.getAttributeValue(i);
                        } else if (attributeName.equals("ptm")) {
                            ptm = xmlPullParser.getAttributeValue(i).split("_")[1];
                        }
                    }
                    ptmProphet.append(" ").append(ptm).append(" prior:").append(value);
                } else if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("mod_aminoacid_probability")){
                    String position = null;
                    String value = null;
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("position")) {
                            position = xmlPullParser.getAttributeValue(i);
                        } else if (attributeName.equals("probability")){
                            value = xmlPullParser.getAttributeValue(i);
                        }
                    }
                    if (value != null && position != null){
                        locationAndScore.put(Integer.valueOf(position), Double.valueOf(value));
                    }
                    ptmProphet.append(" ").append(position).append(":").append(value);
                } else if (tagNum == XmlPullParser.END_TAG && tagName.equals("search_hit")) {

                    if (ptmProphet != null){
                        scoreMap.put("ptmprophet_result", String.valueOf(ptmProphet));
                    } else {
                        scoreMap.put("ptmprophet_result", "*");
                    }

                    break;
                }
            }
            tagNum = xmlPullParser.next();
        }

        if (locationAndScore.size() != 0){
            variableModifications = getModificationMacthes(sequence, modificationMap, locationAndScore);
        }

        variableModifications.addAll(fixedModifications);

        Peptide peptide = new Peptide(sequence, variableModifications);
        //Advocate advocate = Advocate.getAdvocate(searchEngine);
        if(score == null){
            score = 0.0;
        }

        return new PeptideAssumption(peptide, rank, 1, new Charge(Charge.PLUS, charge), score, spectrumFileName);
    }

    /**
     * Update modification according to the PTMProphet
     */
    private ArrayList<ModificationMatch> getModificationMacthes(String sequence, HashMap<Integer, String> modificationMap, HashMap<Integer, Double> locationAndScore){
        ArrayList<ModificationMatch> variableModifications = new ArrayList<>();

        HashMap<String, ArrayList<Integer>> aaToSites = new HashMap<>();
        HashMap<String, HashMap<Integer, Double>> aaToSitesToScore = new HashMap<>();
        HashMap<String, String> aaToModification = new HashMap<>();

        for (Integer position : modificationMap.keySet()){
            String aa = String.valueOf(sequence.charAt(position - 1));
            if (aaToSites.containsKey(aa)){
                aaToSites.get(aa).add(position);
            } else {
                ArrayList<Integer> sitesList = new ArrayList<>();
                sitesList.add(position);
                aaToSites.put(aa, sitesList);
            }
            aaToModification.put(aa, modificationMap.get(position));
        }

        for (Integer position : locationAndScore.keySet()){

            String aa = String.valueOf(sequence.charAt(position - 1));
            if (aaToSitesToScore.containsKey(aa)){
                aaToSitesToScore.get(aa).put(position, locationAndScore.get(position));
            } else {
                HashMap<Integer, Double> sitesToScore = new HashMap<>();
                sitesToScore.put(position, locationAndScore.get(position));
                aaToSitesToScore.put(aa, sitesToScore);
            }
        }

        for (String aa : aaToSites.keySet()){
            ArrayList<Integer> sitesList = aaToSites.get(aa);
            Integer size = sitesList.size();
            HashMap<Integer, Double> sitesToScore = aaToSitesToScore.get(aa);

            if (sitesToScore == null){
                for (Integer site : sitesList){
                    ModificationMatch modificationMatch = new ModificationMatch(aaToModification.get(aa), true, site);
                    variableModifications.add(modificationMatch);
                }
            } else{

                ArrayList<Integer> newSitesList = new ArrayList<>();

                List<Map.Entry<Integer, Double>> list = new ArrayList<>(sitesToScore.entrySet());
                Collections.sort(list, (o1, o2) -> {
                    return o2.getValue().compareTo(o1.getValue());
                });

                if (size == 1){

                    if (sitesToScore.get(sitesList.get(0)) < list.get(0).getValue()){
                        newSitesList.add(list.get(0).getKey());
                    } else {
                        newSitesList.add(sitesList.get(0));
                    }

                } else if (size > 1){

                    for (Integer index = 0; index < size; index ++){
                        Integer site = sitesList.get(index);

                        if (sitesToScore.get(site) == 1){
                            sitesToScore.remove(site);
                            sitesToScore.put(site, 0.0);
                            newSitesList.add(site);
                        }
                    }

                    sitesList.removeIf(newSitesList::contains);

                    list = new ArrayList<>(sitesToScore.entrySet());
                    Collections.sort(list, (o1, o2) -> {
                        return o2.getValue().compareTo(o1.getValue());
                    });

                    for (Integer index = 0; index < sitesList.size(); index ++){
                        Integer site = sitesList.get(index);

                        if (sitesToScore.get(site) < list.get(0).getValue()){
                            newSitesList.add(list.get(0).getKey());
                            break;
                        } else {
                            newSitesList.add(site);
                        }
                    }
                }

                HashSet hashSet = new  HashSet(newSitesList);
                newSitesList.clear();
                newSitesList.addAll(hashSet);

                for (Integer site : newSitesList){
                    ModificationMatch modificationMatch = new ModificationMatch(aaToModification .get(aa), true, site);
                    variableModifications.add(modificationMatch);
                }

            }
        }

        return variableModifications;

    }

    /**
     * Return additional parameters
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void getScoreName() throws IOException, XmlPullParserException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(pepXMLFile));

        checkPeptideProphet(new BufferedReader(new FileReader(pepXMLFile)));

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        xmlPullParserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();

        xmlPullParser.setInput(bufferedReader);

        String tagName;
        int tagNum;
        while ((tagNum = xmlPullParser.next()) != XmlPullParser.START_TAG) {
        }

        while (tagNum != XmlPullParser.END_DOCUMENT) {
            tagName = xmlPullParser.getName();

            if (tagName != null) {
                if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("search_score")) {
                    String name = null;
                    String value = null;
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String attributeName = xmlPullParser.getAttributeName(i);
                        if (attributeName.equals("name")) {
                            name = xmlPullParser.getAttributeValue(i).replaceAll("[^a-zA-Z]", "");
                            if(name.length() >= 30){
                                name = name.substring(0, 29);
                            }
                        } else if (attributeName.equals("value")) {
                            value = xmlPullParser.getAttributeValue(i);
                        }
                        if(!scoreName.contains(name)){
                            scoreName.add(name);
                        }
                    }
                } else if (tagNum == XmlPullParser.END_TAG && tagName.equals("search_hit")) {
                    break;
                }
            }
            tagNum = xmlPullParser.next();
        }
    }

    private void checkPeptideProphet(BufferedReader bufferedReader) throws IOException, XmlPullParserException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        xmlPullParserFactory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();

        xmlPullParser.setInput(bufferedReader);

        String tagName;
        int tagNum;

        while ((tagNum = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

            tagName = xmlPullParser.getName();

            if (tagNum == XmlPullParser.START_TAG && tagName.equals("analysis_timestamp")) {
                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                    String analysis = xmlPullParser.getAttributeValue(i);
                    if (analysis.equals("ptmprophet")) {

                        if(!scoreName.contains("ptmprophet_result")){
                            scoreName.add("ptmprophet_result");
                        }
                    } else if (analysis.equals("peptideprophet")) {
                        if(!scoreName.contains("peptideprophet_result")){
                            scoreName.add("peptideprophet_result");
                        }
                    }
                }

                int xmlType;

                while ((xmlType = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                    if (xmlType == XmlPullParser.END_TAG && xmlPullParser.getName() != null) {
                        if (xmlPullParser.getName().equals("analysis_timestamp")) {
                            break;
                        }
                    }

                    if (xmlType == XmlPullParser.START_TAG) {

                        String tagName1 = xmlPullParser.getName();

                        if (xmlType == XmlPullParser.START_TAG && tagName1.equals("analysis_timestamp")) {
                            for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                String analysis = xmlPullParser.getAttributeValue(i);
                                if (analysis.equals("ptmprophet")) {

                                    if(!scoreName.contains("ptmprophet_result")){
                                        scoreName.add("ptmprophet_result");
                                    }
                                } else if (analysis.equals("peptideprophet")) {
                                    if(!scoreName.contains("peptideprophet_result")){
                                        scoreName.add("peptideprophet_result");
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tagNum == XmlPullParser.END_TAG && tagName.equals("analysis_timestamp")){
                break;
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
    public ArrayList<String> getSoreName(){
        return scoreName;
    }

    /**
     * Return original information
     * @return Hash map
     */
    public HashMap<String, Object> getOriginalInfor(){
        return originalInfor;
    }

    /**
     * Return file details list
     * @return ArrayList
     */
    public ArrayList<String> getDetailsList() {
        return detailsList;
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }
}

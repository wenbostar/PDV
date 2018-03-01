package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.Atom;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
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
import java.util.ArrayList;
import java.util.HashMap;

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
    private HashMap<String, Double> scoreMap = new HashMap<>();
    /**
     * All kinds fo score names list
     */
    private ArrayList<String> scoreName = new ArrayList<>();
    /**
     * Modification mass to mass difference map
     */
    private HashMap<Double, Double> modMassToMassDif = new HashMap<>();
    /**
     * Map stores term modification
     */
    private HashMap<Double, String> termModMassToTerm = new HashMap<>();
    /**
     * n-terminus fixed modification mass lsit
     */
    private ArrayList<Double> nTermFixedModificationMassesList = new ArrayList<>();
    /**
     * c-terminus fixed modification mass lsit
     */
    private ArrayList<Double> cTermFixedModificationMassesList = new ArrayList<>();
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
    private String searchEngine = null;
    /**
     * The search Engine Version
     */
    private String searchEngineVersion = null;
    /**
     * Boolean indicate mgf file
     */
    private Boolean existNoMatchFile = false;
    /**
     * Spectrum xmlPullParserFactory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Object saving spectrum
     */
    private ScanCollectionDefault scans;
    /**
     * Spectrum type
     */
    private String spectrumFileType;

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
                            SpectrumFactory spectrumFactory, ScanCollectionDefault scans, String spectrumFileType, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException, IOException, XmlPullParserException {

        this.pdvMainClass = pdvMainClass;
        this.pepXMLFile = pepXMLFile;
        this.spectrumFileName = spectrumFileName;
        this.modificationMass = modificationMass;
        this.spectrumFactory = spectrumFactory;
        this.scans = scans;
        this.spectrumFileType = spectrumFileType;
        this.progressDialog = progressDialog;

        String dbName = pepXMLFile.getParentFile().getAbsolutePath()+"/"+ pepXMLFile.getName()+".db";
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
    private void parsePepXML() throws XmlPullParserException, IOException, SQLException {

        sqLiteConnection.setScoreNum(scoreName.size());

        Connection connection = sqLiteConnection.getConnection();

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

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(pepXMLFile))) {
            xmlPullParser.setInput(bufferedReader);

            int tagNum;
            boolean hasMatch = false;

            int first = 0;

            SpectrumMatch currentMatch = null;
            Integer currentCharge = null;
            Double rt = 0.0;
            String scanNum = null;
            String spectrumTitle = "";

            modMassToMassDif = new HashMap<>();
            termModMassToTerm = new HashMap<>();
            nTermFixedModificationMassesList = new ArrayList<>();
            cTermFixedModificationMassesList = new ArrayList<>();

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

                if (existNoMatchFile) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "The spectrum file cannot match it in mzIdentML.",
                            "Error Matching", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    System.err.println("No matching file");
                    break;
                }

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

                                if (variable != null && massDiff != null && mass != null && aminoAcid != null && description != null) {
                                    if (!variable) {
                                        if (terminusName == null) {
                                            fixedModiMap.put(description + " of " + aminoAcid, " massDelta: " + massDiff);
                                        } else {
                                            termModMassToTerm.put(mass, terminus);
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

                                    modMassToMassDif.put(mass, massDiff);
                                } else {
                                    throw new IllegalArgumentException("An error occurred while parsing aminoacid_modification element. Missing values.");
                                }

                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("terminal_modification")) {

                                Boolean variable = null;
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

                                if (variable != null && mass != null && terminus != null && description != null) {
                                    if (!variable) {
                                        if (terminus.equalsIgnoreCase("N")) {
                                            fixedModiMap.put(description + " on N-term", " massDelta: " + massDiff);
                                            nTermFixedModificationMassesList.add(mass);
                                        } else {
                                            fixedModiMap.put(description + " on C-term", " massDelta: " + massDiff);
                                            cTermFixedModificationMassesList.add(mass);
                                        }
                                    } else {
                                        if (terminus.equalsIgnoreCase("N")) {
                                            variableModiMap.put(description + " on N-term", " massDelta: " + massDiff);
                                            nTermFixedModificationMassesList.add(mass);
                                        } else {
                                            variableModiMap.put(description + " on C-term", " massDelta: " + massDiff);
                                            cTermFixedModificationMassesList.add(mass);
                                        }
                                    }
                                } else {
                                    throw new IllegalArgumentException("An error occurred while parsing terminal_modification element. Missing values.");
                                }
                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("search_database")) {
                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("local_path")) {
                                        originalInfor.put("DB Name", xmlPullParser.getAttributeValue(i));
                                        detailsList.add("search_database/t/" + xmlPullParser.getAttributeValue(i));
                                    }
                                }
                            } else if (xmlType == XmlPullParser.START_TAG && tagName1.equals("enzymatic_search_constraint")) {
                                String enzyme = null;
                                String cleavages = null;
                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("enzyme")) {
                                        enzyme = xmlPullParser.getAttributeValue(i);

                                    }
                                    if (name.equals("max_num_internal_cleavages")) {
                                        cleavages = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                if (enzyme != null) {
                                    originalInfor.put("Enzyme", enzyme + "max_num_internal_cleavages" + cleavages);
                                    detailsList.add("Enzyme/t/" + enzyme + "(" + cleavages + ")");
                                }
                            }
                        }
                    }

                    int count1 = 0;
                    for (String name : fixedModiMap.keySet()) {

                        detailsList.add("Fixed Modification: " + count1 + "/t/" + name + "(" + fixedModiMap.get(name) + ")");
                        count1++;
                    }
                    count1 = 0;
                    for (String name : variableModiMap.keySet()) {

                        detailsList.add("Variable Modification: " + count1 + "/t/" + name + "(" + variableModiMap.get(name) + "(");
                        count1++;
                    }

                    originalInfor.put("Fixed Modification", fixedModiMap);
                    originalInfor.put("Variable Modification", variableModiMap);
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

                    Integer index = null;
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
                            spectrumNativeID = xmlPullParser.getAttributeValue(i);
                        } else if (name.equals("start_scan")) {
                            scanNum = xmlPullParser.getAttributeValue(i);
                        }
                    }

                    if (index == null) {
                        throw new IllegalArgumentException("No index found for spectrum " + spectrumId + ".");
                    }

                    if (spectrumNativeID != null) {
                        spectrumTitle = spectrumNativeID;
                    } else {
                        spectrumTitle = index + "";
                    }

                    Integer spectrumIndex = Integer.valueOf(scanNum);

                    if (spectrumFileType.equals("mgf")) {
                        rt = ((MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle)).getPrecursor().getRtInMinutes();
                    } else {
                        rt = scans.getScanByNum(spectrumIndex).getRt();
                    }

                    SpectrumMatch spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));
                    spectrumMatch.setSpectrumNumber(spectrumIndex);

                    spectrumIndexList.add(String.valueOf(spectrumIndex));

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
                    boolean found = false;
                    if (first == 1) {
                        currentMatch.setBestPeptideAssumption(peptideAssumption);

                    }

                    if (currentMatch.getAllAssumptions() != null) {
                        for (SpectrumIdentificationAssumption tempAssumption : currentMatch.getAllAssumptions()) {
                            PeptideAssumption tempPeptideAssumption = (PeptideAssumption) tempAssumption;
                            Peptide tempPeptide = tempPeptideAssumption.getPeptide();
                            if (peptide.getSequence().equals(tempPeptide.getSequence())) {
                                boolean sameModifications = peptide.getNModifications() == tempPeptide.getNModifications();
                                if (sameModifications && peptide.isModified()) {
                                    for (ModificationMatch originalMatch : peptide.getModificationMatches()) {
                                        boolean ptmFound = false;
                                        for (ModificationMatch otherMatch : tempPeptide.getModificationMatches()) {
                                            if (originalMatch.getTheoreticPtm().equals(otherMatch.getTheoreticPtm()) && originalMatch.getModificationSite() == otherMatch.getModificationSite()) {
                                                ptmFound = true;
                                                break;
                                            }
                                        }
                                        if (!ptmFound) {
                                            sameModifications = false;
                                            break;
                                        }
                                    }
                                }
                                if (sameModifications) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (!found) {

                        Advocate advocate = Advocate.getAdvocate(searchEngine);
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
                                currentMatch.addHit(advocate.getIndex(), newAssumption, false);

                            }
                        } else {
                            currentMatch.addHit(advocate.getIndex(), peptideAssumption, false);
                        }
                    }

                }
                if (tagNum == XmlPullParser.END_TAG && tagName.equals("spectrum_query")) {

                    if (hasMatch) {
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

                        preparedStatement.setInt(1, currentMatch.getSpectrumNumber());
                        preparedStatement.setDouble(2, peptideAssumption.getTheoreticMass() / currentCharge);
                        preparedStatement.setString(3, spectrumTitle);
                        preparedStatement.setString(4, peptideAssumption.getPeptide().getSequence());
                        preparedStatement.setDouble(5, Math.abs(peptideAssumption.getScore()));
                        preparedStatement.setBytes(6, bos.toByteArray());

                        for (String name : scoreMap.keySet()) {
                            if (scoreName.contains(name)) {
                                Integer index = scoreName.indexOf(name) + 7;
                                preparedStatement.setDouble(index, scoreMap.get(name));
                            }

                        }

                        preparedStatement.addBatch();

                        connection.setAutoCommit(false);

                        count++;

                        if (count == 1000) {
                            int[] counts = preparedStatement.executeBatch();
                            connection.setAutoCommit(true);
                            preparedStatement.close();

                            pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                            if (countRound == 0) {

                                pdvMainClass.displayResult();
                                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                                progressDialog.setRunFinished();

                                countRound++;

                            } else {
                                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
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
                connection.setAutoCommit(true);
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumIndexList);

                if (countRound == 0) {

                    pdvMainClass.displayResult();
                    pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                    progressDialog.setRunFinished();

                } else {
                    pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                }
            }

            pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
            pdvMainClass.loadingJButton.setText("Import done");

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
        ArrayList<ModificationMatch> modificationMatches = new ArrayList<ModificationMatch>();
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

        HashMap<Double, String > massModification;

        String tagName = xmlPullParser.getName();
        if (tagName.equals("modification_info")) {

            for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                String attributeName = xmlPullParser.getAttributeName(i);

                if (attributeName.equals("mod_nterm_mass") || attributeName.equals("mod_cterm_mass")) {

                    String value = xmlPullParser.getAttributeValue(i).trim();
                    Double terminalMass;
                    try {
                        terminalMass = new Double(value);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("An error occurred while parsing modification terminal mass " + value + ". Number expected.");
                    }

                    boolean variableModification;
                    if (attributeName.equals("mod_nterm_mass")) {
                        variableModification = !nTermFixedModificationMassesList.contains(terminalMass);
                    } else {
                        variableModification = !cTermFixedModificationMassesList.contains(terminalMass);
                    }

                    int site;
                    if (attributeName.equals("mod_nterm_mass")) {
                        site = 1;
                        terminalMass -= Atom.H.getMonoisotopicMass();
                    } else {
                        site = sequence.length();
                        terminalMass -= (Atom.O.getMonoisotopicMass() + Atom.H.getMonoisotopicMass());

                        if (searchEngine != null && searchEngine.equalsIgnoreCase("Comet")
                                && searchEngineVersion != null
                                && !searchEngineVersion.equalsIgnoreCase("2015.02 rev. 4")
                                && !searchEngineVersion.equalsIgnoreCase("2015.02 rev. 5")) {
                            terminalMass -= Atom.H.getMonoisotopicMass();
                        }
                    }

                    char aa = sequence.charAt(site - 1);
                    terminalMass = Util.roundDouble(terminalMass, 2);
                    String tempModificationName = terminalMass + "@" + aa;
                    ModificationMatch modificationMatch = new ModificationMatch(tempModificationName, variableModification, site);
                    modificationMatches.add(modificationMatch);
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
                                        massModification = modificationMass.get("N-terminus");
                                        location = 1;
                                    } else {
                                        massModification = modificationMass.get("C-terminus");
                                        location = sequence.length();
                                    }
                                } else {
                                    massModification = modificationMass.get(aa+"");
                                }

                                String modificationName = null;

                                for(Double mass: massModification.keySet()){
                                    if (Math.abs(mass-modMassToMassDif.get(modifiedAaMass))<0.05){//Mass error may cause problem
                                        modificationName = massModification.get(mass);
                                    }
                                }

                                if (!allModifications.contains(modificationName)){
                                    allModifications.add(modificationName);
                                }

                                modificationMatches.add(new ModificationMatch(modificationName, true, location));
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
                            name = xmlPullParser.getAttributeValue(i).replaceAll("[^a-zA-Z]", "");
                            if(name.length() >= 30){
                                name = name.substring(0, 29);
                            }
                        } else if (attributeName.equals("value")) {
                            value = xmlPullParser.getAttributeValue(i);
                        }
                        if(value != null) {
                            scoreMap.put(name, Double.valueOf(value));
                        }
                    }
                } else if (tagNum == XmlPullParser.END_TAG && tagName.equals("search_hit")) {
                    break;
                }
            }
            tagNum = xmlPullParser.next();
        }

        Peptide peptide = new Peptide(sequence, modificationMatches);
        Advocate advocate = Advocate.getAdvocate(searchEngine);
        if(score == null){
            score = 0.0;
        }

        return new PeptideAssumption(peptide, rank, advocate.getIndex(), new Charge(Charge.PLUS, charge), score, spectrumFileName);
    }

    /**
     * Return additional parameters
     * @throws IOException
     * @throws XmlPullParserException
     */
    private void getScoreName() throws IOException, XmlPullParserException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(pepXMLFile));

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

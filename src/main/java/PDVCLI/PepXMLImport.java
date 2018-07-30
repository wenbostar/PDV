package PDVCLI;

import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * PepXMLFile parsing
 * Created by Ken on 2/8/2018.
 */
public class PepXMLImport {

    /**
     * Spectrum file name
     */
    private String spectrumFileName;
    /**
     * PepXMLFile imported
     */
    private File pepXMLFile;
    /**
     * Spectrum matches map saving in DB
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();
    /**
     * Modification mass to mass difference map
     */
    private HashMap<Double, Double> modifMassToMassDif = new HashMap<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * PTM setting
     */
    public PtmSettings ptmSettings = new PtmSettings();
    /**
     * Map
     */
    HashMap<Double, String> modMassToName = new HashMap<>();
    /**
     * Map stores term modification
     */
    private HashMap<Double, String> termModMassToTerm = new HashMap<>();
    /**
     * Fixed modification list
     */
    private ArrayList<String> fixedModificationAAs = new ArrayList<>();
    /**
     * Spectrum ID to spectrum number
     */
    private HashMap<String, Integer> spectrumIdAndNumber;
    /**
     * Spectrum type
     */
    private Integer spectrumFileType;

    /**
     * Constructor of pepXMLImporter
     * @param spectrumFileName spectrumFile name
     * @param pepXMLFile pepXML file
     */
    public PepXMLImport(String spectrumFileName, File pepXMLFile, HashMap<String, Integer> spectrumIdAndNumber, Integer spectrumFileType) {
        this.spectrumFileName = spectrumFileName;
        this.pepXMLFile = pepXMLFile;
        this.spectrumIdAndNumber = spectrumIdAndNumber;
        this.spectrumFileType = spectrumFileType;

        try {
            importFile();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Import results from pepXML file
     * @throws XmlPullParserException
     * @throws IOException
     */
    private void importFile() throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        factory.setNamespaceAware(true);
        XmlPullParser xmlPullParser = factory.newPullParser();

        BufferedReader bufferedReader = new BufferedReader(new FileReader(pepXMLFile));

        try {
            xmlPullParser.setInput(bufferedReader);

            int tagNum;
            boolean hasMatch = false;

            int first = 0;

            SpectrumMatch currentMatch = null;
            Integer currentCharge = null;
            String scanNum = null;

            modifMassToMassDif = new HashMap<>();

            while ((tagNum = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                String tagName = xmlPullParser.getName();

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("search_summary")) {

                    int xmlType;

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
                                            fixedModificationAAs.add(String.valueOf(aminoAcid).toLowerCase());
                                        } else {
                                            termModMassToTerm.put(mass, terminus);
                                            fixedModificationAAs.add(String.valueOf(aminoAcid).toLowerCase());
                                        }

                                    } else {
                                        if (terminusName == null) {
                                        } else {
                                            termModMassToTerm.put(mass, terminus);
                                        }
                                    }
                                    modMassToName.put(mass, ptmName);

                                    modifMassToMassDif.put(mass, massDiff);
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

                                    modMassToName.put(mass, ptmName);
                                    modifMassToMassDif.put(mass, massDiff);

                                } else {
                                    throw new IllegalArgumentException("An error occurred while parsing terminal_modification element. Missing values.");
                                }
                            }
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

                    String spectrumTitle;

                    if (spectrumNativeID != null) {
                        spectrumTitle = spectrumNativeID;
                    } else if (spectrumId != null){
                        spectrumTitle = spectrumId;
                    } else {
                        spectrumTitle = scanNum + "";
                    }

                    Integer spectrumIndex = Integer.valueOf(scanNum);

                    SpectrumMatch spectrumMatch;

                    if (spectrumFileType == 1){
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));
                    } else if (spectrumFileType == 2){
                        spectrumIndex = spectrumIdAndNumber.get(spectrumNativeID);
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex - 1)));
                    } else {
                        spectrumMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex - 1)));
                    }

                    spectrumMatch.setSpectrumNumber(spectrumIndex);

                    currentMatch = spectrumMatch;

                    SpectrumMatch previousMatch = spectrumMatchesMap.get(scanNum);
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
                                        1, peptideAssumption.getIdentificationCharge(),
                                        peptideAssumption.getScore(), peptideAssumption.getIdentificationFile());
                                currentMatch.addHit(1, newAssumption, false);

                            }
                        } else {
                            currentMatch.addHit(1, peptideAssumption, false);
                        }
                    }

                }
                if (tagNum == XmlPullParser.END_TAG && tagName.equals("spectrum_query")) {

                    if (hasMatch) {
                        String key = String.valueOf(currentMatch.getSpectrumNumber());
                        if (!spectrumMatchesMap.containsKey(key)) {
                            spectrumMatchesMap.put(key, currentMatch);
                        }

                        hasMatch = false;
                        currentMatch = null;
                        currentCharge = null;
                    }
                }

            }
        } finally {
            bufferedReader.close();
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

                    int site;
                    if (attributeName.equals("mod_nterm_mass")) {
                        site = 1;
                        modificationName = modMassToName.get(terminalMass);

                    } else {
                        site = sequence.length();
                        modificationName = modMassToName.get(terminalMass);
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
                if (tagNum == XmlPullParser.START_TAG && xmlPullParser.getName().equals("mod_aminoacid_probability")){
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
                } else if (tagNum == XmlPullParser.END_TAG && tagName.equals("search_hit")) {

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
     * Get all spectrum match
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchesMap() {
        return spectrumMatchesMap;
    }
}

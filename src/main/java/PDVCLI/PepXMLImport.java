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
import java.util.ArrayList;
import java.util.HashMap;

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
     * Map stores term modification
     */
    private HashMap<Double, String> termModifMassToTerm = new HashMap<>();
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
     * Constructor of pepXMLImporter
     * @param spectrumFileName spectrumFile name
     * @param pepXMLFile pepXML file
     */
    public PepXMLImport(String spectrumFileName, File pepXMLFile) {
        this.spectrumFileName = spectrumFileName;
        this.pepXMLFile = pepXMLFile;

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
            termModifMassToTerm = new HashMap<>();

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
                                        } else {
                                            termModifMassToTerm.put(mass, terminus);
                                        }

                                    } else {
                                        if (terminusName == null) {
                                        } else {
                                            termModifMassToTerm.put(mass, terminus);
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
                    } else {
                        spectrumTitle = scanNum + "";
                    }

                    String spectrumKey = Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle);
                    SpectrumMatch spectrumMatch = new SpectrumMatch(spectrumKey);
                    spectrumMatch.setSpectrumNumber(Integer.valueOf(scanNum));

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
     * Get peptide assumption of one hit
     * @param xmlPullParser XML xmlPullParser
     * @param charge Charge of the hit
     * @return the peptide assumption
     * @throws XmlPullParserException
     * @throws IOException
     */
    private PeptideAssumption getPeptideAssumption(XmlPullParser xmlPullParser, Integer charge) throws XmlPullParserException, IOException {

        Integer rank = null;
        String sequence = null;
        ArrayList<ModificationMatch> modificationMatches = new ArrayList<ModificationMatch>();
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

                    ModificationMatch modificationMatch = new ModificationMatch(modificationName, true, site);
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

                                location = site;

                                if(termModifMassToTerm.containsKey(modifiedAaMass)){
                                    if(termModifMassToTerm.get(modifiedAaMass).equals("n")){
                                        modificationName = modMassToName.get(modifiedAaMass);
                                        location = 1;
                                    } else {
                                        modificationName = modMassToName.get(modifiedAaMass);
                                        location = sequence.length();
                                    }
                                } else {
                                    modificationName = modMassToName.get(modifiedAaMass);
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

        Peptide peptide = new Peptide(sequence, modificationMatches);
        if (score == null){
            score = 0.0;
        }
        if (rank == null){
            rank = 1;
        }

        return new PeptideAssumption(peptide, rank, 1, new Charge(Charge.PLUS, charge), score, spectrumFileName);
    }

    /**
     * Get all spectrum match
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchesMap() {
        return spectrumMatchesMap;
    }
}

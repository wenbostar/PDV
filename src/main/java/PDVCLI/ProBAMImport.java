package PDVCLI;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import htsjdk.samtools.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * ProBAM File parsing
 * Created by Ken on 2/13/2018.
 */
public class ProBAMImport {

    /**
     * ProBAM file
     */
    private File proBamFile;
    /**
     * Spectrum matches map
     */
    private HashMap<String, SpectrumMatch> spectrumMatchHashMap;
    /**
     * Spectrum file name
     */
    private String spectrumFileName;
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;

    /**
     * Constructor
     * @param proBamFile proBAM file
     * @param spectrumFileName Spectrum file name
     * @param spectrumsFileFactory Spectrum factory
     */
    public ProBAMImport( File proBamFile, String spectrumFileName, Object spectrumsFileFactory) {
        this.proBamFile = proBamFile;
        this.spectrumFileName =spectrumFileName;
        if (spectrumFileName.toLowerCase().endsWith(".mgf")){
            this.spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
        } else {
            this.spectrumFactory = null;
        }

        importFile();
    }

    /**
     * Import results from proBAM file.
     */
    private void importFile(){

        spectrumMatchHashMap = new HashMap<>();

        final SamReaderFactory srf = SamReaderFactory.makeDefault().validationStringency(ValidationStringency.LENIENT);

        SamReader samFileReader = srf.open(SamInputResource.of(proBamFile));

        String version = samFileReader.getFileHeader().getVersion();

        SAMRecordIterator iter = samFileReader.iterator();
        ArrayList<ModificationMatch> modificationMatches;
        HashMap<String, Double> locationToMass = new HashMap<>();
        HashMap<String, HashMap<Double, String>> modificationMassMap = getModificationMass();
        HashMap<String, String> locationToModificationName = new HashMap<>();

        PTMFactory ptmFactory = PTMFactory.getInstance();
        HashMap<String, String> accessionToModification = new HashMap<>();
        for(String modificationName: ptmFactory.getPTMs()){
            PTM ptm = ptmFactory.getPTM(modificationName);
            if(ptm.getCvTerm()!= null){
                accessionToModification.put(ptm.getCvTerm().getAccession(), ptm.getCvTerm().getName());
            }
        }

        String fileName = "";
        int dot = spectrumFileName.lastIndexOf('.');
        if ((dot >-1) && (dot < (spectrumFileName.length()))) {
            fileName = spectrumFileName.substring(0, dot);
        }

        if (version.equals("1.0")) {

            while (iter.hasNext()) {

                String spectrumIndex = null;
                Peptide peptide = null;
                Peptide referencePeptide = null;
                String peptideSequence;
                int peptideCharge = 0;
                String referenceSequence;
                double score = 0.0;
                double fdr = 0.0;
                String spectrumTitle;

                SAMRecord rec = iter.next();
                List<SAMRecord.SAMTagAndValue> list = rec.getAttributes();

                String spectrumName = rec.getReadName();

                if (spectrumName.contains("index")) {

                    if (spectrumName.contains(":")){
                        spectrumIndex = spectrumName.split(":")[1].split("=")[1];
                    } else {
                        spectrumIndex = spectrumName.split("=")[1];
                    }

                } else if (spectrumName.contains(fileName)) {

                    int dot1 = spectrumName.lastIndexOf('.');
                    if ((dot1 > -1) && (dot1 < (spectrumName.length()))) {
                        spectrumIndex = spectrumName.substring(dot1 + 1);
                    }
                } else {
                    System.err.println("File format error");
                    break;
                }

                if (spectrumFactory != null){
                    spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(spectrumIndex) + 1);
                } else {
                    spectrumTitle = spectrumIndex;
                }

                SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));

                for (SAMRecord.SAMTagAndValue samTagAndValue : list) {
                    String modificationName = null;

                    if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        modificationMatches = new ArrayList<>();
                        peptideSequence = (String) samTagAndValue.value;
                        for (String location : locationToMass.keySet()) {
                            if (location.equals("0") || location.equals("-2147483648")) {
                                String aA = peptideSequence.split("")[0];
                                Double modificationMass = locationToMass.get(location);

                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }

                                if (modificationName == null) {
                                    for (Double mass : modificationMassMap.get("N-terminus").keySet()) {
                                        if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                            modificationName = modificationMassMap.get("N-terminus").get(mass);
                                        }
                                    }
                                }

                                modificationMatches.add(new ModificationMatch(modificationName, true, 1));
                                modificationName = null;
                            } else if (location.equals(String.valueOf(peptideSequence.length() + 1))) {
                                String aA = peptideSequence.split("")[peptideSequence.length() - 1];
                                Double modificationMass = locationToMass.get(location);

                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }

                                if (modificationName == null) {
                                    for (Double mass : modificationMassMap.get("C-terminus").keySet()) {
                                        if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                            modificationName = modificationMassMap.get("C-terminus").get(mass);
                                        }
                                    }
                                }
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                                modificationName = null;
                            } else {
                                String aA = peptideSequence.split("")[Integer.parseInt(location)];
                                Double modificationMass = locationToMass.get(location);
                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location) + 1));
                                modificationName = null;
                            }
                        }
                        peptide = new Peptide(peptideSequence, modificationMatches);
                        locationToMass = new HashMap<>();

                    } else if (samTagAndValue.tag.equals("XC") && !samTagAndValue.value.equals("*")) {
                        peptideCharge = (int) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XR") && !samTagAndValue.value.equals("*")) {
                        referenceSequence = (String) samTagAndValue.value;
                        referencePeptide = new Peptide(referenceSequence, null);
                    } else if (samTagAndValue.tag.equals("XS") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        score = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XB") && !samTagAndValue.value.equals("*")) {

                    } else if (samTagAndValue.tag.equals("XQ") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        fdr = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XM") && !samTagAndValue.value.equals("*") && !samTagAndValue.value.equals("-")) {
                        String modificationAccessionList = (String) samTagAndValue.value;
                        String[] allModifications = modificationAccessionList.split(",");
                        for (String eachModification : allModifications) {
                            String[] locationAndName = eachModification.split("@");
                            locationToMass.put(locationAndName[1], Double.valueOf(locationAndName[0]));
                        }
                    }
                }

                if (peptide != null) {

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, peptideCharge), score, null);

                    peptideAssumption.setRawScore(fdr);

                    currentMatch.addHit(1, peptideAssumption, false);

                    currentMatch.setBestPeptideAssumption(peptideAssumption);
                }

                if (referencePeptide != null) {
                    PeptideAssumption peptideAssumption = new PeptideAssumption(referencePeptide, 2, 1, new Charge(1, peptideCharge), 0, null);

                    currentMatch.addHit(1, peptideAssumption, false);
                }

                if (!spectrumMatchHashMap.containsKey(spectrumTitle)) {

                    spectrumMatchHashMap.put(spectrumTitle, currentMatch);

                }
            }
        } else {
            while (iter.hasNext()) {

                String spectrumIndex = null;
                Peptide peptide = null;
                Peptide referencePeptide = null;
                String peptideSequence;
                String spectrumTitle;
                int peptideCharge = 0;
                String referenceSequence;
                double score = 0.0;
                double fdr = 0.0;

                SAMRecord rec = iter.next();
                List<SAMRecord.SAMTagAndValue> list = rec.getAttributes();

                String spectrumName = rec.getReadName();

                if (spectrumName.contains("index")) {

                    if (spectrumName.contains(":")){
                        spectrumIndex = spectrumName.split(":")[1].split("=")[1];
                    } else {
                        spectrumIndex = spectrumName.split("=")[1];
                    }

                } else if (spectrumName.contains(fileName)) {

                    int dot1 = spectrumName.lastIndexOf('.');
                    if ((dot1 > -1) && (dot1 < (spectrumName.length()))) {
                        spectrumIndex = spectrumName.substring(dot1 + 1);
                    }

                } else {
                    break;
                }

                if (spectrumFactory != null){
                    spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(spectrumIndex) + 1);
                } else {
                    spectrumTitle = spectrumIndex;
                }

                SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, spectrumTitle));

                for (SAMRecord.SAMTagAndValue samTagAndValue : list) {

                    if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        modificationMatches = new ArrayList<>();
                        peptideSequence = ((String) samTagAndValue.value).replaceAll("[^a-z^A-Z]", "");
                        for(String location: locationToModificationName.keySet()){
                            if(location.equals("0")){
                                String modificationName = locationToModificationName.get(location) + " of N-term";
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                            } else if(location.equals(String.valueOf(peptideSequence.length()+1))){
                                String modificationName = locationToModificationName.get(location) + " of C-term";
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)-1));
                            } else {
                                String aA = peptideSequence.split("")[Integer.parseInt(location)-1];
                                String modificationName = locationToModificationName.get(location) + " of " + aA;
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                            }
                        }
                        peptide = new Peptide(peptideSequence, modificationMatches);
                        locationToModificationName = new HashMap<>();
                    } else if (samTagAndValue.tag.equals("XC") && !samTagAndValue.value.equals("*")) {
                        peptideCharge = (int) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XR") && !samTagAndValue.value.equals("*")){
                        referenceSequence = (String) samTagAndValue.value;
                        referencePeptide = new Peptide(referenceSequence, null);
                    } else if (samTagAndValue.tag.equals("XS") && !samTagAndValue.value.equals("*")){
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        score = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XB") && !samTagAndValue.value.equals("*")){

                    }else if (samTagAndValue.tag.equals("XM") && !samTagAndValue.value.equals("*")){
                        locationToModificationName = new HashMap<>();
                        String modificationAccessionList = (String) samTagAndValue.value;
                        String[] allModifications = modificationAccessionList.split(";");
                        for (String eachModification: allModifications){
                            String[] locationAndName = eachModification.split("-");
                            String modificationName = accessionToModification.get(locationAndName[1]);
                            locationToModificationName.put(locationAndName[0], modificationName);
                        }
                    }
                }

                if (peptide != null) {

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, peptideCharge), score, null);

                    peptideAssumption.setRawScore(fdr);

                    currentMatch.addHit(1, peptideAssumption, false);

                    currentMatch.setBestPeptideAssumption(peptideAssumption);
                }

                if (referencePeptide != null) {
                    PeptideAssumption peptideAssumption = new PeptideAssumption(referencePeptide, 2, 1, new Charge(1, peptideCharge), 0, null);

                    currentMatch.addHit(1, peptideAssumption, false);
                }

                if (!spectrumMatchHashMap.containsKey(spectrumTitle)) {

                    spectrumMatchHashMap.put(spectrumTitle, currentMatch);

                }
            }
        }
    }

    /**
     * Get Modification mass map
     * @return HashMap
     */
    private HashMap<String,HashMap<Double, String >> getModificationMass(){

        PTMFactory ptmFactory = PTMFactory.getInstance();

        HashMap<String,HashMap<Double, String > > modificationMass = new HashMap<>();
        ArrayList<String> orderedModifications = ptmFactory.getPTMs();
        for (String  modificationName : orderedModifications){
            String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
            String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
            if(modificationMass.containsKey(aminoAcidName)){
                modificationMass.get(aminoAcidName).put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
            }else {
                HashMap<Double, String> singleModi = new HashMap<>();
                singleModi.put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
                modificationMass.put(aminoAcidName, singleModi);
            }
        }
        return modificationMass;
    }

    /**
     * Get all spectrum matches
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchHashMap(){

        return spectrumMatchHashMap;
    }

}

package PDVCLI;

import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzidentml.MzIdentMLParser;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Ken on 2/6/2018.
 */
public class MzIDImport {

    /**
     * MzIdentML file
     */
    private File idFile;
    /**
     * Modification mass map from unimod
     */
    private HashMap<String,HashMap<Double, String >> modificationMass;
    /**
     * Spectrum files ref amd details map
     */
    private HashMap<String, String> spectrumFileMap = new HashMap<>();
    /**
     * Spectrum matches map saving in DB
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;

    /**
     * Constructor
     * @param idFile MzIdentML file
     * @param spectrumFileName Spectrum file name
     * @param spectrumsFileFactory Spectrum file factory
     */
    public MzIDImport(File idFile, String spectrumFileName, Object spectrumsFileFactory){

        this.modificationMass = getModificationMass();
        this.idFile = idFile;
        if (spectrumFileName.toLowerCase().endsWith("mgf")){
            this.spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
        } else {
            this.spectrumFactory = null;
        }

        try {
            parseMzID();
        } catch (FileParsingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read mzIdentML file
     * @throws FileParsingException
     */
    private void parseMzID() throws FileParsingException {

        MzIdentMLType mzIdentMLType = MzIdentMLParser.parse(Paths.get(idFile.getAbsolutePath()));

        HashMap<String, Object[]> peptideMap = new HashMap<>();

        try {

            List<PeptideType> peptideTypeList = mzIdentMLType.getSequenceCollection().getPeptide();

            if (peptideTypeList.size() == 0){
                Thread.interrupted();
            }

            Object[] peptideArray;

            for (PeptideType peptideType : peptideTypeList) {

                peptideArray = new Object[2];
                peptideArray[0] = peptideType.getPeptideSequence();

                if (peptideType.getModification() != null) {
                    peptideArray[1] = peptideType.getModification();
                }

                peptideMap.put(peptideType.getId(), peptideArray);
            }

            List<SpectraDataType> spectraDataTypeList = mzIdentMLType.getDataCollection().getInputs().getSpectraData();
            for (SpectraDataType spectraDataType : spectraDataTypeList) {
                if (spectraDataType.getName() != null) {
                    spectrumFileMap.put(spectraDataType.getId(), spectraDataType.getName());
                } else {
                    String spectrumFileName = spectraDataType.getLocation().split("/")[spectraDataType.getLocation().split("/").length - 1];
                    spectrumFileMap.put(spectraDataType.getId(), spectrumFileName);
                }
            }

        }catch (Exception e){
            Thread.interrupted();
        }

        String spectrumIndex;
        String spectrumTitle;
        String spectrumFileRef;
        String currentSpectrumFile;
        String peptideRef;
        int rank;
        double massError;
        double calculatedMZ;
        double experimentMZ;
        String peptideSequence;
        String spectrumID;
        List<ModificationType> modifications;
        String modificationName = null;
        HashMap<Double, String> massModification;
        ArrayList<ModificationMatch> utilitiesModifications;
        Charge peptideCharge;
        PeptideAssumption peptideAssumption;
        SpectrumMatch currentMatch;
        List<SpectrumIdentificationItemType> spectrumIdentificationItems;

        for (SpectrumIdentificationListType spectrumIdentificationListType : mzIdentMLType.getDataCollection().getAnalysisData().getSpectrumIdentificationList()) {

            List<SpectrumIdentificationResultType> spectrumIdentificationResults = spectrumIdentificationListType.getSpectrumIdentificationResult();

            for (SpectrumIdentificationResultType spectrumIdentificationResultType : spectrumIdentificationResults) {

                int rankNum = 0;

                spectrumID = spectrumIdentificationResultType.getSpectrumID();

                if(spectrumID.contains(" ")){
                    String [] eachBig = spectrumID.split(" ");
                    spectrumIndex = eachBig[eachBig.length-1].split("=")[1];
                }else {
                    spectrumIndex = spectrumID.split("=")[1];
                }

                spectrumFileRef = spectrumIdentificationResultType.getSpectraDataRef();
                currentSpectrumFile = spectrumFileMap.get(spectrumFileRef);

                if (spectrumFactory != null){
                    spectrumTitle = spectrumFactory.getSpectrumTitle(currentSpectrumFile, Integer.parseInt(spectrumIndex) + 1);
                } else {
                    spectrumTitle = spectrumIndex;
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(currentSpectrumFile, spectrumTitle));

                spectrumIdentificationItems = spectrumIdentificationResultType.getSpectrumIdentificationItem();

                for (SpectrumIdentificationItemType spectrumIdentificationItemType : spectrumIdentificationItems) {

                    rankNum++;

                    rank = spectrumIdentificationItemType.getRank();

                    calculatedMZ = spectrumIdentificationItemType.getCalculatedMassToCharge();
                    experimentMZ = spectrumIdentificationItemType.getExperimentalMassToCharge();

                    massError = experimentMZ - calculatedMZ;

                    peptideCharge = new Charge(Charge.PLUS, spectrumIdentificationItemType.getChargeState()); // + 1

                    peptideRef = spectrumIdentificationItemType.getPeptideRef();

                    peptideSequence = (String) peptideMap.get(peptideRef)[0];

                    utilitiesModifications = new ArrayList<>();

                    if (peptideMap.get(peptideRef)[1] != null) {
                        modifications = (List<ModificationType>) peptideMap.get(peptideRef)[1];

                        for (ModificationType modificationType : modifications) {
                            int location = modificationType.getLocation();
                            double monoMassDelta = modificationType.getMonoisotopicMassDelta();

                            if (location == 0) {
                                massModification = modificationMass.get("N-terminus"); //Todo Need add any term in it?
                                location = 1;

                            } else if (location == peptideSequence.length() + 1) {
                                massModification = modificationMass.get("C-terminus");
                                location = peptideSequence.length();

                            } else {
                                massModification = modificationMass.get(peptideSequence.charAt(location - 1) + "");
                            }

                            for (Double mass : massModification.keySet()) {
                                if (Math.abs(mass - monoMassDelta) < 0.05) {//Mass error may cause problem
                                    modificationName = massModification.get(mass);
                                }
                            }

                            utilitiesModifications.add(new ModificationMatch(modificationName, true, location));

                        }
                    }

                    com.compomics.util.experiment.biology.Peptide peptide = new com.compomics.util.experiment.biology.Peptide(peptideSequence, utilitiesModifications);

                    peptideAssumption = new PeptideAssumption(peptide, rank, 0, peptideCharge, massError, idFile.getName());

                    if (AminoAcidSequence.hasCombination(peptideAssumption.getPeptide().getSequence())) {
                        ArrayList<ModificationMatch> previousModificationMatches = peptide.getModificationMatches(),
                                newModificationMatches = null;
                        if (previousModificationMatches != null) {
                            newModificationMatches = new ArrayList<>(previousModificationMatches.size());
                        }
                        for (StringBuilder expandedSequence : AminoAcidSequence.getCombinations(peptide.getSequence())) {
                            com.compomics.util.experiment.biology.Peptide newPeptide = new com.compomics.util.experiment.biology.Peptide(expandedSequence.toString(), newModificationMatches);
                            if (previousModificationMatches != null) {
                                for (ModificationMatch modificationMatch : previousModificationMatches) {
                                    newPeptide.addModificationMatch(new ModificationMatch(modificationMatch.getTheoreticPtm(), modificationMatch.isVariable(), modificationMatch.getModificationSite()));
                                }
                            }
                            PeptideAssumption newAssumption = new PeptideAssumption(newPeptide, peptideAssumption.getRank(), peptideAssumption.getAdvocate(), peptideAssumption.getIdentificationCharge(), peptideAssumption.getScore(), peptideAssumption.getIdentificationFile());
                            currentMatch.addHit(0, newAssumption, false);

                            if (rankNum == 1) {
                                currentMatch.setBestPeptideAssumption(newAssumption);
                            }

                        }

                    } else {
                        if (rankNum == 1) {
                            currentMatch.setBestPeptideAssumption(peptideAssumption);
                        }
                        currentMatch.addHit(0, peptideAssumption, false);
                    }
                }

                spectrumMatchesMap.put(spectrumTitle + "_rank_" + currentSpectrumFile, currentMatch);
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
     * Get all spectrum match
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchesMap() {
        return spectrumMatchesMap;
    }
}

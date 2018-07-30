package PDVCLI;

import com.compomics.util.experiment.biology.AminoAcidSequence;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
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
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * PTM setting
     */
    public PtmSettings ptmSettings = new PtmSettings();
    /**
     * Spectrum ID to spectrum number
     */
    private HashMap<String, Integer> spectrumIdAndNumber;
    /**
     * Spectrum type
     */
    private Integer spectrumFileType;

    /**
     * Constructor
     * @param idFile MzIdentML file
     * @param spectrumFileName Spectrum file name
     * @param spectrumsFileFactory Spectrum file factory
     */
    public MzIDImport(File idFile, String spectrumFileName, Object spectrumsFileFactory, HashMap<String, Integer> spectrumIdAndNumber, Integer spectrumFileType){

        this.idFile = idFile;
        this.spectrumIdAndNumber =spectrumIdAndNumber;
        this.spectrumFileType = spectrumFileType;

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

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(currentSpectrumFile, spectrumIndex));

                if (spectrumFileType == 1){
                    spectrumTitle = spectrumFactory.getSpectrumTitle(currentSpectrumFile, Integer.parseInt(spectrumIndex) + 1);
                } else if (spectrumFileType == 2) {
                    currentMatch.setSpectrumNumber(spectrumIdAndNumber.get(spectrumID));
                    spectrumTitle = spectrumIndex;
                } else {
                    currentMatch.setSpectrumNumber(Integer.valueOf(spectrumIndex));
                    spectrumTitle = spectrumIndex;
                }

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
                    ArrayList<String> residues;

                    if (peptideMap.get(peptideRef)[1] != null) {
                        modifications = (List<ModificationType>) peptideMap.get(peptideRef)[1];

                        for (ModificationType modificationType : modifications) {
                            int location = modificationType.getLocation();
                            double monoMassDelta = modificationType.getMonoisotopicMassDelta();
                            String nameFromMzID;
                            List<CVParamType> cvParamTypes  = modificationType.getCvParam();

                            if (cvParamTypes != null){

                                CVParamType firstType = cvParamTypes.get(0);
                                if (firstType.getName() != null){
                                    nameFromMzID = firstType.getName();
                                } else {
                                    nameFromMzID = String.valueOf(monoMassDelta);
                                }

                            } else {
                                nameFromMzID = String.valueOf(monoMassDelta);
                            }


                            if (location == 0) {

                                modificationName = nameFromMzID + " of N-term";

                                if (!ptmFactory.containsPTM(modificationName)){
                                    PTM ptm = new PTM(PTM.MODNP, modificationName, monoMassDelta, null);
                                    ptm.setShortName(nameFromMzID);
                                    ptmFactory.addUserPTM(ptm);
                                }

                                location = 1;

                            } else if (location == peptideSequence.length() + 1) {

                                modificationName = nameFromMzID + " of C-term";

                                if (!ptmFactory.containsPTM(modificationName)){
                                    PTM ptm = new PTM(PTM.MODCP, modificationName, monoMassDelta, null);
                                    ptm.setShortName(nameFromMzID);
                                    ptmFactory.addUserPTM(ptm);
                                }

                                location = peptideSequence.length();

                            } else {
                                residues = new ArrayList<>();
                                String aa = String.valueOf(peptideSequence.charAt(location - 1));
                                residues.add(aa);

                                modificationName = nameFromMzID + " of " + aa;

                                if (!ptmFactory.containsPTM(modificationName)){
                                    PTM ptm = new PTM(PTM.MODAA, modificationName, monoMassDelta, residues);
                                    ptm.setShortName(nameFromMzID);
                                    ptmFactory.addUserPTM(ptm);
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

        ArrayList<String> modification =  ptmFactory.getPTMs();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }
    }

    /**
     * Get all spectrum match
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchesMap() {
        return spectrumMatchesMap;
    }
}

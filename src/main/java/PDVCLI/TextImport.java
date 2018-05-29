package PDVCLI;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Import new soft .txt results
 * Created by Ken on 10/23/2017.
 */
public class TextImport {

    /**
     * Text Id file
     */
    private File textIdFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Spectrum title to rank
     */
    private HashMap<String, Integer> spectrumTitleToRank = new HashMap<>();
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
     * Spectrum matches map saving in DB
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * PTM setting
     */
    public PtmSettings ptmSettings = new PtmSettings();

    /**
     * Constructor
     * @param textIdFile Text identification file
     * @param spectrumFile Spectrum file
     * @throws IOException
     */
    public TextImport(File textIdFile, File spectrumFile) throws IOException {

        this.textIdFile = textIdFile;
        this.spectrumFile = spectrumFile;

        getParameters();

        parseTextFile();
    }

    /**
     * Parsing text file
     * @throws IOException
     */
    private void parseTextFile() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(textIdFile));

        String line;
        String[] values;
        String spectrumTitle;
        String modificationNames;
        String singleModificationName;
        String sequence;
        Integer modificationSite;
        Integer peptideCharge;
        String rankString;
        Peptide peptide;
        Double modificationMass;

        ArrayList<ModificationMatch> utilitiesModifications;
        HashMap<Double, String> massModification;
        SpectrumMatch currentMatch;
        PeptideAssumption peptideAssumption;

        int lineCount = 0;

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (lineCount == 0) {

            } else {

                utilitiesModifications = new ArrayList<>();

                spectrumTitle = values[spectrumTitleIndex];
                sequence = values[sequenceIndex];
                modificationNames = values[modificationIndex];
                peptideCharge = Integer.valueOf(values[chargeIndex]);

                ArrayList<String> residues = new ArrayList<>();
                if(!modificationNames.equals("-")){
                    for (String singleModification: modificationNames.split(";")){
                        singleModificationName = singleModification.split("@")[0];
                        modificationSite = Integer.valueOf(singleModification.split("@")[1].split("\\[")[0]);
                        modificationMass = Double.valueOf(singleModification.split("@")[1].split("\\[")[1].replace("]", ""));

                        if (!ptmFactory.containsPTM(singleModificationName)){
                            String modificationName = singleModificationName.split(" of ")[0];
                            residues.add(singleModificationName.split(" of ")[1]);

                            if (singleModificationName.toLowerCase().contains("n-term")){
                                PTM ptm = new PTM(PTM.MODNPAA, singleModificationName, modificationMass, residues);
                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            } else if (singleModificationName.toLowerCase().contains("c-term")){
                                PTM ptm = new PTM(PTM.MODCP, singleModificationName, modificationMass, residues);
                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            } else {
                                PTM ptm = new PTM(PTM.MODAA, singleModificationName, modificationMass, residues);
                                ptm.setShortName(modificationName);
                                ptmFactory.addUserPTM(ptm);
                            }
                        }

                        if (singleModificationName.toLowerCase().contains("n-term")){
                            modificationSite = 1;
                        } else if (singleModificationName.toLowerCase().contains("c-term")){
                            modificationSite = sequence.length();
                        }

                        utilitiesModifications.add(new ModificationMatch(singleModificationName, true, modificationSite));
                    }
                }

                if(spectrumTitleToRank.containsKey(spectrumTitle)){
                    int rank = spectrumTitleToRank.get(spectrumTitle) + 1;
                    spectrumTitleToRank.put(spectrumTitle, rank);
                    rankString = String.valueOf(rank);

                } else{

                    spectrumTitleToRank.put(spectrumTitle, 1);
                    rankString = "1";
                }

                currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFile.getName(), spectrumTitle));

                peptide = new Peptide(sequence, utilitiesModifications);

                peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, peptideCharge), 0, "*");

                currentMatch.addHit(0, peptideAssumption, false);
                currentMatch.setBestPeptideAssumption(peptideAssumption);

                spectrumMatchesMap.put(spectrumTitle+"_rank_"+rankString, currentMatch);
            }
            lineCount ++;
        }bufferedReader.close();

        ArrayList<String> modification =  ptmFactory.getPTMs();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }
    }

    /**
     * Get all parameters
     * @throws IOException
     */
    private void getParameters() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(textIdFile));

        int lineCount = 0;
        String line;
        String[] values;

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (lineCount == 0) {
                for (int index = 0; index < values.length; index++) {
                    switch (values[index]) {
                        case "spectrum_title":
                            spectrumTitleIndex = index;
                            break;
                        case "peptide":
                            sequenceIndex = index;
                            break;
                        case "modification":
                            modificationIndex = index;
                            break;
                        case "charge":
                            chargeIndex = index;
                            break;
                        default:
                            break;
                    }
                }
            } else {
                break;
            }
            lineCount ++;
        }
    }

    /**
     * Get all spectrum matches
     * @return HashMap
     */
    public HashMap<String, SpectrumMatch> getSpectrumMatchHashMap(){

        return spectrumMatchesMap;
    }

}
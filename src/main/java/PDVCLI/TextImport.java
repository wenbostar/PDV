package PDVCLI;

import com.compomics.util.experiment.biology.Peptide;
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
     * ALl modification
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum matches map saving in DB
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();

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

        ArrayList<ModificationMatch> utilitiesModifications;
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

                if(!modificationNames.equals("-")){
                    for (String singleModification: modificationNames.split(";")){
                        singleModificationName = singleModification.split("@")[0];
                        modificationSite = Integer.valueOf(singleModification.split("@")[1].split("\\[")[0]);

                        if (!allModifications.contains(singleModificationName)){
                            allModifications.add(singleModificationName);
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
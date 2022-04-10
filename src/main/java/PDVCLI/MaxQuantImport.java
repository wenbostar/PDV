package PDVCLI;

import PDVGUI.utils.ImportUserMod;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import utils.MaxQuantGenMGF;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * MaxQuant results parsing
 * Created by Ken on 2/8/2018.
 */
public class MaxQuantImport {

    /**
     * MaxQuant results folder pathe
     */
    private File maxQuantDirectory;
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * All apl files list
     */
    private ArrayList<File> allAPLFiles = new ArrayList<>();
    /**
     * All modification files
     */
    private ArrayList<File> modificationFiles = new ArrayList<>();
    /**
     * All varied modifications
     */
    private ArrayList<String> variedModifications = new ArrayList<>();
    /**
     * Modification map
     */
    private HashMap<String, ArrayList<String>> fileNameToPositionList = new HashMap<>();
    /**
     * Sequence map
     */
    private HashMap<String, ArrayList<String>> fileNameToSeqList = new HashMap<>();
    /**
     * Modification map
     */
    private HashMap<ArrayList<String>, HashMap<String, ArrayList<String>>> msIDSToModificationFileToEachSite = new HashMap<>();
    /**
     * MsMsScan file
     */
    private File scansDetailFile;
    /**
     * modificationSpecificPeptides
     */
    private File modificationSpecificPeptidesFile;
    /**
     * MsMs file
     */
    private File msResultFile;
    /**
     * Parameters File
     */
    private File parametersFile;
    /**
     * Parameters XMl file
     */
    private File parametersXMLFile;
    /**
     * Has mgf files or not
     */
    private Boolean hasMGF = false;
    /**
     * File to title to RT map
     */
    private HashMap<String, HashMap<String, String>> fileToTitleToRTMap = new HashMap<>();
    /**
     * File to index map
     */
    private HashMap<String, Integer> fileToCountMap = new HashMap<>();
    /**
     * raw file column index
     */
    private int rawFileIndex = 0;
    /**
     * scan number column index
     */
    private int scanNumIndex = 0;
    /**
     * sequence column index
     */
    private int sequenceIndex = 0;
    /**
     * modification column index
     */
    private int modificationIndex = 0;
    /**
     * modification sequence column index
     */
    private int modificationSequenceIndex = 0;
    /**
     * mass error column index
     */
    private int massErrorIndex = 0;
    /**
     * Experiment Mass index
     */
    private int expMassIndex = 0;
    /**
     * score column index
     */
    private int scoreIndex = 0;
    /**
     * charge column index
     */
    private int chargeIndex = 0;
    /**
     * Spectrum title to rank
     */
    private HashMap<String, Integer> spectrumTitleToRank = new HashMap<>();
    /**
     * All spectrum matches
     */
    private HashMap<String, SpectrumMatch> allSpectrumMatches = new HashMap<>();
    /**
     * Fixed modification map
     */
    private HashMap<String, String> fixedModificationMap = new HashMap<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * SILAC label map
     */
    private HashMap<String, HashMap<String, Integer>> silacLabelMap = new HashMap<>();
    /**
     * SILAC modification map
     */
    private HashMap<String,  String> silacModMap = new HashMap<>();

    /**
     * Constructor
     * @param maxQuantDirectory MaxQuant results folder path
     * @throws IOException
     */
    public MaxQuantImport(File maxQuantDirectory) throws IOException {

        this.maxQuantDirectory = maxQuantDirectory;

        getAllFiles();
        try {
            parseParameters();
        } catch (IOException e) {
            e.printStackTrace();
        }

        new ImportUserMod();
        getAllSpectrumRT(hasMGF);
        getAllModificationSites();
        getModificationPeptides();
        getOriginalInfor();

        if (!hasMGF){
            generateNewMGFFile();
        } else {
            addAllSpectrumFiles();
        }
    }

    /**
     * Get all necessary files
     */
    private void getAllFiles(){

        Boolean isCombine = false;

        if(maxQuantDirectory != null && maxQuantDirectory.isDirectory() && maxQuantDirectory.listFiles() != null){

            for(File eachFileInMax : maxQuantDirectory.listFiles()){
                if(eachFileInMax.isDirectory() && eachFileInMax.getName().equals("andromeda")){

                    isCombine = true;

                    for(File eachFileInAnd : eachFileInMax.listFiles()){
                        if(eachFileInAnd.getName().contains(".apl")){
                            allAPLFiles.add(eachFileInAnd);
                        }
                    }

                } else if(eachFileInMax.isDirectory() && eachFileInMax.getName().equals("txt")){
                    isCombine = true;

                    for(File eachFileInTxt : eachFileInMax.listFiles()){
                        if(eachFileInTxt.getName().equals("msms.txt")){
                            msResultFile = eachFileInTxt;
                        } else if(eachFileInTxt.getName().equals("msmsScans.txt")){
                            scansDetailFile = eachFileInTxt;
                        } else if(eachFileInTxt.getName().equals("parameters.txt")){
                            parametersFile = eachFileInTxt;
                        } else if (eachFileInTxt.getName().contains("Sites")){
                            modificationFiles.add(eachFileInTxt);

                            variedModifications.add(eachFileInTxt.getName().split("Sites")[0].replace("-_", "->"));
                        } else if (eachFileInTxt.getName().equals("modificationSpecificPeptides.txt")){
                            modificationSpecificPeptidesFile = eachFileInTxt;
                        }
                    }
                } else if (eachFileInMax.isDirectory() && eachFileInMax.getName().equals("generatesMGF")){
                    hasMGF = true;
                }
                if (eachFileInMax.getName().equals("mqpar.xml")){
                    this.parametersXMLFile = eachFileInMax;
                }
            }
        } else {
            System.err.println("Please check your file.");
        }

        if (!isCombine){
            System.err.println("Please check your file.");
        }
    }

    /**
     * Get retention time and file scanNum
     * @param existMGF Exist mgf files
     * @throws IOException
     */
    private void getAllSpectrumRT(Boolean existMGF) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(scansDetailFile));

        String line;
        String[] values;
        String spectrumTitle;
        String rawFileName = "a";

        HashMap<String, String> titleToRTMap;

        int lineCount = 0;
        int rawFileIndex = 0;
        int scanNumIndex = 0;
        int rtIndex = 0;

        if(existMGF){
            while ((line = bufferedReader.readLine()) != null) {
                values = line.split("\t");

                if(lineCount == 0){
                    for(int index = 0;index<values.length;index++){
                        switch (values[index]) {
                            case "Raw file":
                                rawFileIndex = index;
                                break;
                        }
                    }
                }else {
                    if(!rawFileName.equals(values[rawFileIndex])){
                        rawFileName = values[rawFileIndex];
                        fileToCountMap.put(rawFileName, lineCount-1);
                    }

                }
                lineCount ++;
            }bufferedReader.close();
        }else {
            while ((line = bufferedReader.readLine()) != null) {
                values = line.split("\t");

                if(lineCount == 0){
                    for(int index = 0;index<values.length;index++){
                        switch (values[index]) {
                            case "Raw file":
                                rawFileIndex = index;
                                break;
                            case "Scan number":
                                scanNumIndex = index;
                                break;
                            case "Retention time":
                                rtIndex = index;
                                break;
                        }
                    }
                }else {

                    if(!rawFileName.equals(values[rawFileIndex])){
                        rawFileName = values[rawFileIndex];
                        fileToCountMap.put(rawFileName, lineCount-1);
                    }

                    spectrumTitle = "RawFile: "+values[rawFileIndex]+" Index: "+values[scanNumIndex];
                    if(fileToTitleToRTMap.containsKey(values[rawFileIndex])){
                        fileToTitleToRTMap.get(values[rawFileIndex]).put(spectrumTitle, values[rtIndex]);
                    }else {
                        titleToRTMap = new HashMap<>();
                        titleToRTMap.put(spectrumTitle, values[rtIndex]);
                        fileToTitleToRTMap.put(values[rawFileIndex], titleToRTMap);
                    }
                }
                lineCount ++;
            }bufferedReader.close();
        }
    }

    /**
     * Get all modification
     */
    private void getAllModificationSites(){

        BufferedReader bufferedReader;
        ArrayList<String> eachFile;
        ArrayList<String> eachFileSeq;

        for (File eachModificationFile : modificationFiles){
            eachFile = new ArrayList<>();
            eachFileSeq = new ArrayList<>();

            try {
                bufferedReader = new BufferedReader(new FileReader(eachModificationFile));

                String line;
                String[] lineElements;
                Integer count = 0;
                Integer positionIndex = 0;
                Integer modSeqIndex = 0;
                String modSeq;
                StringBuilder stringBuilder;

                while ((line = bufferedReader.readLine()) != null){

                    lineElements = line.split("\t");
                    if (count == 0){
                        for (Integer index = 0; index < lineElements.length; index ++){
                            if (lineElements[index].equals("Position in peptide")){
                                positionIndex = index;
                            } else if (lineElements[index].contains("Score diffs")){
                                modSeqIndex = index;
                            }
                        }
                    } else {
                        stringBuilder = new StringBuilder();

                        eachFile.add(lineElements[positionIndex]);
                        modSeq = lineElements[modSeqIndex];
                        String[] seqArray = modSeq.split("\\(");
                        for (Integer index = 0; index < seqArray.length; index ++){
                            if (index == 0){
                                stringBuilder.append(seqArray[0]);
                            } else {
                                if (seqArray[index].split("\\)").length > 1){
                                    stringBuilder.append(seqArray[index].split("\\)")[1]);
                                }
                            }
                        }
                        eachFileSeq.add(String.valueOf(stringBuilder));
                    }

                    count ++;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            fileNameToPositionList.put(eachModificationFile.getName().split("Sites")[0].replace("_", ">"), eachFile);
            fileNameToSeqList.put(eachModificationFile.getName().split("Sites")[0].replace("_", ">"), eachFileSeq);
        }
    }

    /**
     * Get parameters
     */
    public void getOriginalInfor(){

        if(parametersFile != null){
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(parametersFile));

                String line;
                String[] values;

                while ((line = bufferedReader.readLine()) != null) {
                    values = line.split("\t");
                    if(values.length == 2){

                        if (values[0].equals("Fixed modifications")){
                            for (String eachMod : values[1].split("; ")){
                                String aa = eachMod.split(" \\(")[1].replace(")", "");
                                fixedModificationMap.put(aa, eachMod.split(" \\(")[0]);

                            }
                        }

                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get all modification peptide
     */
    private void getModificationPeptides() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(modificationSpecificPeptidesFile));

        String line;
        String[] values;
        Integer count = 0;
        ArrayList<String> eachIDs;
        HashMap<String, ArrayList<String>> modificationFileToEachSite;
        ArrayList<String> eachMod;
        Integer IDsIndex = 0;
        String allIDs;
        HashMap<Integer, String> modificationIndexToFile = new HashMap<>();
        while ((line = bufferedReader.readLine())!=null){

            values = line.split("\t");
            if (count == 0){
                for (Integer index = 0 ; index<values.length; index ++){

                    if(values[index].equals("MS/MS IDs")) {
                        IDsIndex = index;
                    } else if (values[index].contains("site IDs")){
                        modificationIndexToFile.put(index, values[index].split(" site")[0]);
                    }
                }
            } else {

                eachIDs = new ArrayList<>();
                modificationFileToEachSite = new HashMap<>();
                allIDs = values[IDsIndex];

                for (String each : allIDs.split(";")){
                    eachIDs.add(each);
                }

                for (Integer eachModIndex : modificationIndexToFile.keySet()){
                    eachMod = new ArrayList<>();
                    if (eachModIndex < values.length && !values[eachModIndex].equals("") && values[eachModIndex] != null){
                        for (String each : values[eachModIndex].split(";")){
                            eachMod.add(each);
                        }
                        modificationFileToEachSite.put(modificationIndexToFile.get(eachModIndex), eachMod);
                    }
                }

                if (modificationFileToEachSite.size() != 0){
                    msIDSToModificationFileToEachSite.put(eachIDs, modificationFileToEachSite);
                }
            }

            count ++;
        }
    }

    /**
     * Generate all MGF files
     */
    public void generateNewMGFFile(){

        HashMap<String, BufferedWriter> fileNameToFileWriter = new HashMap<>();
        HashMap<String, ArrayList<String>> fileNameToScanNum = new HashMap<>();
        String parentDictory = maxQuantDirectory.getAbsolutePath() + PDVCLIMainClass.FILE_SEPARATOR + "generatesMGF" + PDVCLIMainClass.FILE_SEPARATOR;
        ArrayList<String> currentScanNumList;

        File mgfDirectory = new File(parentDictory);

        if (!mgfDirectory.exists()) {
            mgfDirectory.mkdir();
        }

        try {
            for (String fileName : fileToTitleToRTMap.keySet()) {
                BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(parentDictory + fileName + ".mgf")));
                fileNameToFileWriter.put(fileName, fileWriter);
                ArrayList<String> eachFileScanList = new ArrayList<>();
                fileNameToScanNum.put(fileName, eachFileScanList);
            }

            new MaxQuantGenMGF(allAPLFiles, fileNameToFileWriter, fileNameToScanNum, fileToTitleToRTMap);

            fileToTitleToRTMap.clear();

            for(String fileName : fileNameToFileWriter.keySet()){

                fileNameToFileWriter.get(fileName).close();

                System.out.println(fileName+" = "+fileNameToScanNum.get(fileName).size());
            }

            addAllSpectrumFiles();

        } catch (IOException |Error e) {

            if(e.getClass().getName().contains("Error")){
                System.err.println("Please increase JVM memory! ");
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add mgf into spectrumFactory
     */
    private void addAllSpectrumFiles(){
        spectrumFactory = SpectrumFactory.getInstance();

        String parentDictory = maxQuantDirectory.getAbsolutePath() + PDVCLIMainClass.FILE_SEPARATOR + "generatesMGF";
        File mgfDirectory = new File(parentDictory);

        if (!mgfDirectory.exists()) {
            System.err.println("There is no spectrum directory!");

        } else {

            FileFilter fileFilter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase().endsWith(".mgf");
                }
            };
            File[] allFiles = mgfDirectory.listFiles(fileFilter);

            if (allFiles == null) {
                System.err.println("There is no spectrum file!");
            } else {
                try {
                    for (File eachSpectrumFile : allFiles) {

                        spectrumFactory.addSpectra(eachSpectrumFile);

                    }

                    parseResults();
                } catch (IOException | Error e ) {
                    if(e.getClass().getName().contains("Error")){

                        System.err.println("Please increase JVM memory! ");
                    } else {
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (MzMLUnmarshallerException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * Read result from file
     * @throws IOException
     */
    private void parseResults() throws IOException, MzMLUnmarshallerException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(msResultFile));

        String line;
        String[] values;
        String spectrumTitle;
        String modificationName;
        String sequence;
        String modificationSequence;
        String scanNumber;
        String rawFileName;
        Integer peptideCharge;
        double expMass;
        double theroticMass;
        double spectrumMass;
        double massError;
        Double score;
        String utilitiesModificationName;
        String silacLabel = "";
        String[] splitAABy;
        String[] modifications;
        String[] splitNum;
        PeptideAssumption peptideAssumption;
        Peptide peptide;
        Charge charge;

        ArrayList<Integer> variableIndex;
        ArrayList<String> variableTerm;
        ArrayList<String> residues;
        HashMap<String, String> aAToModification;
        ArrayList<ModificationMatch> utilitiesModifications;
        SpectrumMatch currentMatch;

        HashMap<String, Integer> modificationNumIndex = new HashMap<>();
        HashMap<String, Integer> modificationPosIndex = new HashMap<>();
        HashMap<String, Integer> onesilacLabelMap = new HashMap<>();
        HashMap<Integer, Double> modIndexToPos;

        int lineCount = 0;

        while ((line = bufferedReader.readLine()) != null) {
            values = line.split("\t");

            if (lineCount == 0) {
                for (int index = 0; index < values.length; index++) {
                    if(values[index].equals("Raw file")){
                        rawFileIndex = index;
                    } else if(values[index].equals("Scan number")){
                        scanNumIndex = index;
                    } else if(values[index].equals("Scan index")){
                    } else if(values[index].equals("Retention time")){
                    } else if(values[index].equals("Sequence")){
                        sequenceIndex = index;
                    } else if(values[index].equals("Modifications")){
                        modificationIndex = index;
                    } else if(values[index].equals("Mass Error [ppm]")){
                        massErrorIndex = index;
                    } else if(values[index].equals("Mass")){
                        expMassIndex = index;
                    } else if(values[index].equals("Modified sequence")){
                        modificationSequenceIndex = index;
                    } else if(values[index].equals("Score")){
                        scoreIndex = index;
                    } else if(values[index].equals("Charge")){
                        chargeIndex = index;
                    } else if (variedModifications.contains(values[index])){
                        modificationNumIndex.put(values[index], index);
                    } else if (values[index].contains("Probabilities")){
                        modificationPosIndex.put(values[index].split(" Probabilities")[0], index);
                    }
                }
            } else {

                utilitiesModifications = new ArrayList<>();

                sequence = values[sequenceIndex];
                modificationName = values[modificationIndex];
                modificationSequence = values[modificationSequenceIndex];
                if(values[massErrorIndex].contains(".")){
                    massError = Double.valueOf(values[massErrorIndex]);
                } else {
                    massError = 0.0;
                }
                peptideCharge = Integer.valueOf(values[chargeIndex]);
                rawFileName = values[rawFileIndex];
                scanNumber = values[scanNumIndex];
                expMass = Double.parseDouble(values[expMassIndex]);
                score = Double.valueOf(values[scoreIndex]);

                spectrumTitle = "RawFile: " + rawFileName + " Index: " + scanNumber + " Charge: " + peptideCharge;
                spectrumMass = spectrumFactory.getPrecursor(rawFileName + ".mgf", spectrumTitle).getMz() * peptideCharge;

                if(spectrumTitleToRank.containsKey(spectrumTitle)){
                    int rank = spectrumTitleToRank.get(spectrumTitle) + 1;
                    spectrumTitleToRank.put(spectrumTitle, rank);

                    currentMatch = allSpectrumMatches.get(scanNumber + "_rank_" + rawFileName);

                } else{
                    spectrumTitleToRank.put(spectrumTitle, 1);

                    currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(rawFileName+".mgf", spectrumTitle));
                    currentMatch.setSpectrumNumber(Integer.valueOf(scanNumber));
                    allSpectrumMatches.put(scanNumber + "_rank_" + rawFileName, currentMatch);
                }

                variableIndex = new ArrayList<>();
                variableTerm = new ArrayList<>();
                try {
                    if (!modificationName.equals("Unmodified")) {

                        for (String modName : modificationNumIndex.keySet()){

                            Integer modNumIndex = modificationNumIndex.get(modName);

                            Integer modNum = Integer.valueOf(values[modNumIndex]);

                            Integer modPosIndex = modificationPosIndex.get(modName);

                            String modPosSequence = values[modPosIndex];

                            modIndexToPos = new HashMap<>();

                            if (modNum != 0){

                                String[] firstSplitList = modPosSequence.split("\\)");

                                Integer accumPosition = 0;
                                String splitSequence;
                                Double splitPos;

                                for (String firstSplit : firstSplitList){

                                    if (firstSplit.contains("(")){
                                        splitSequence = firstSplit.split("\\(")[0];
                                        splitPos = Double.valueOf(firstSplit.split("\\(")[1]);

                                        accumPosition += splitSequence.length();

                                        modIndexToPos.put(accumPosition, splitPos);
                                    }
                                }

                                List<Map.Entry<Integer, Double>> list = new ArrayList<>(modIndexToPos.entrySet());
                                Collections.sort(list, Comparator.comparing(Map.Entry::getKey));

                                Collections.sort(list, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

                                for (int i = 0; i < modNum; i ++){

                                    Integer position = list.get(i).getKey();
                                    variableIndex.add(position - 1);
                                    String modAA = String.valueOf(sequence.charAt(position - 1));
                                    utilitiesModificationName = modName.split(" \\(")[0].replace(">","&gt;") + " of " + modAA;

                                    if (fixedModificationMap.containsKey(modAA)){
                                        if (Math.abs(expMass - spectrumMass) > 10) {
                                            residues = new ArrayList<>();
                                            residues.add(modAA);
                                            String fixedModName = fixedModificationMap.get(modAA) + " of " + modAA;
                                            double combinedMass = ptmFactory.getPTM(utilitiesModificationName).getMass() + ptmFactory.getPTM(fixedModName).getMass();
                                            String combinedName = modName.split(" \\(")[0].replace(">", "&gt;") + " and " + fixedModName;
                                            if (!ptmFactory.containsPTM(combinedName)) {
                                                PTM ptm = new PTM(PTM.MODAA, combinedName, combinedMass, residues);
                                                ptm.setShortName(modName.split(" \\(")[0].replace(">", "&gt;") + " and " + fixedModificationMap.get(modAA));
                                                ptmFactory.addUserPTM(ptm);
                                            }

                                            utilitiesModifications.add(new ModificationMatch(combinedName, true, position));
                                        }
                                    } else {
                                        utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, position));
                                    }

                                }

                            }
                        }

                        aAToModification = new HashMap<>();

                        modifications = modificationName.split(",");

                        for (String eachModification : modifications) {
                            splitNum = eachModification.split(" ");
                            if (eachModification.contains("C-term")) {
                                if (eachModification.contains("Protein C-term")){
                                    aAToModification.put("Protein C-term", splitNum[0]);
                                } else {
                                    aAToModification.put("C-term", splitNum[0]);
                                }
                            } else if (eachModification.contains("N-term")){
                                if (eachModification.contains("Protein N-term")){
                                    aAToModification.put("Protein N-term", splitNum[0]);
                                } else {
                                    aAToModification.put("N-term", splitNum[0]);
                                }
                            }
                        }

                        splitAABy = modificationSequence.split("\\(");

                        for (String eachModificationPart : splitAABy) {
                            if (eachModificationPart.equals("_")) {
                                if (aAToModification.get("N-term") == null){
                                    utilitiesModificationName = aAToModification.get("Protein N-term") + " of " + "protein N-term";
                                } else {
                                    utilitiesModificationName = aAToModification.get("N-term") + " of " + "N-term";
                                }
                                variableTerm.add("N");

                                utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, 1));
                            } else if (eachModificationPart == splitAABy[splitAABy.length - 1] && !eachModificationPart.contains("_")) {
                                if (aAToModification.get("C-term") == null){
                                    utilitiesModificationName = aAToModification.get("Protein C-term") + " of " + "protein C-term";
                                } else {
                                    utilitiesModificationName = aAToModification.get("C-term") + " of " + "C-term";
                                }

                                variableTerm.add("C");

                                utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, sequence.length()));
                            }
                        }
                    }
                } catch (Exception e){
                    e.printStackTrace();
                }

                for (String key : fixedModificationMap.keySet()){
                    if (key.contains("N-term")){
                        if (!variableTerm.contains("N")){
                            utilitiesModificationName = fixedModificationMap.get(key) + " of " + key;
                            expMass = expMass + ptmFactory.getPTM(utilitiesModificationName).getMass();
                            utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, 1));
                        }
                    } else if (key.contains("C-term")){
                        if (!variableTerm.contains("C")){
                            utilitiesModificationName = fixedModificationMap.get(key) + " of " + key;
                            expMass = expMass + ptmFactory.getPTM(utilitiesModificationName).getMass();
                            utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, sequence.length()));
                        }
                    }
                }

                String[] sequenceArray = sequence.split("");
                int length =  sequenceArray.length;
                for (int index = 0; index < length; index ++){
                    String aa = sequenceArray[index];
                    if (fixedModificationMap.containsKey(aa)){
                        if (!variableIndex.contains(index)) {
                            utilitiesModificationName = fixedModificationMap.get(aa) + " of " + aa;
                            utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, index + 1));
                        } /**else {
                            if (Math.abs(expMass - spectrumMass) > 10) {
                                residues = new ArrayList<>();
                                residues.add(aa);

                                for (ModificationMatch modificationMatch : utilitiesModifications){
                                    if (modificationMatch.getModificationSite() == index + 1){
                                        String fixedModName = fixedModificationMap.get(aa) + " of " + aa;
                                        double combinedMass = ptmFactory.getPTM(modificationMatch.getTheoreticPtm()).getMass() + ptmFactory.getPTM(fixedModName).getMass();
                                        String combinedName = modificationMatch.getTheoreticPtm().split(" \\(")[0].replace(">", "&gt;") + " and " + fixedModName;
                                        if (!ptmFactory.containsPTM(combinedName)) {
                                            PTM ptm = new PTM(PTM.MODAA, combinedName, combinedMass, residues);
                                            ptm.setShortName(modificationMatch.getTheoreticPtm().split(" \\(")[0].replace(">", "&gt;") + " and " + fixedModificationMap.get(aa));
                                            ptmFactory.addUserPTM(ptm);
                                        }
                                        utilitiesModifications.add(new ModificationMatch(combinedName, true, index + 1));
                                    }
                                }
                            }
                        }**/
                    }
                }

                peptide = new Peptide(sequence, utilitiesModifications);

                charge = new Charge(+1, peptideCharge);

                peptideAssumption = new PeptideAssumption(peptide, spectrumTitleToRank.get(spectrumTitle), 0, charge, massError, "maxQuant");
                peptideAssumption.setRawScore(score);

                theroticMass = peptideAssumption.getTheoreticMass();

                if (silacLabelMap.size() != 0){
                    silacLabel = "";
                    onesilacLabelMap = new HashMap<>();
                    int deltaMass = (int) Math.round(expMass - theroticMass);
                    //System.out.println(sequence + " Delta mass is " + deltaMass);
                    int rCount = StringUtils.countMatches(sequence, "R");
                    int kCount = StringUtils.countMatches(sequence, "K");
                    int rMass = 0;
                    int kMass = 0;
                    if (deltaMass != 0) {
                        if (silacLabelMap.size() == 1) {
                            onesilacLabelMap = silacLabelMap.get("one");
                            silacLabel = silacLabelMap.get("one").toString();

                        } else {
                            HashMap<String, Integer> oneLabel = silacLabelMap.get("one");
                            HashMap<String, Integer> twoLabel = silacLabelMap.get("two");
                            for (String key : oneLabel.keySet()) {
                                if (key.equals("R")) {
                                    rMass = rCount * oneLabel.get(key);
                                } else if (key.equals("K")) {
                                    kMass = kCount * oneLabel.get(key);
                                }
                            }
                            if (rMass + kMass == deltaMass) {
                                onesilacLabelMap = silacLabelMap.get("one");
                                silacLabel = oneLabel.toString();
                            }
                            rMass = 0;
                            kMass = 0;
                            for (String key : twoLabel.keySet()) {
                                if (key.equals("R")) {
                                    rMass = rCount * twoLabel.get(key);
                                } else if (key.equals("K")) {
                                    kMass = kCount * twoLabel.get(key);
                                }
                            }
                            if (rMass + kMass == deltaMass) {
                                onesilacLabelMap = silacLabelMap.get("two");
                                silacLabel = twoLabel.toString();
                            }
                        }
                    }
                }

                if (onesilacLabelMap.size() != 0){

                    HashMap<Integer, ModificationMatch> modSiteToMod = new HashMap<>();
                    for (ModificationMatch modificationMatch : utilitiesModifications){
                        modSiteToMod.put(modificationMatch.getModificationSite(), modificationMatch);
                    }

                    for (int index = 0; index < length; index ++) {
                        String aa = sequenceArray[index];
                        residues = new ArrayList<>();
                        residues.add(aa);
                        if (onesilacLabelMap.containsKey(aa)){
                            String silacLabelIndex = aa + "=" + onesilacLabelMap.get(aa);
                            if (modSiteToMod.containsKey(index + 1)){
                                ModificationMatch modificationMatch = modSiteToMod.get(index + 1);
                                String combinedName = modificationMatch.getTheoreticPtm().split(" of ")[0] + " + " + silacModMap.get(silacLabelIndex);
                                double combinedMass = ptmFactory.getPTM(modificationMatch.getTheoreticPtm()).getMass() + onesilacLabelMap.get(aa);
                                peptideAssumption.getPeptide().addModificationMatch(new ModificationMatch(combinedName, true, index + 1));
                                if (!ptmFactory.containsPTM(combinedName)) {
                                    PTM ptm = new PTM(PTM.MODAA, combinedName, combinedMass, residues);
                                    ptm.setShortName(combinedName.split(" of ")[0]);
                                    ptmFactory.addUserPTM(ptm);
                                }

                            } else {
                                String combinedName = silacModMap.get(silacLabelIndex);
                                peptideAssumption.getPeptide().addModificationMatch(new ModificationMatch(combinedName, true, index + 1));

                            }
                        }
                    }
                }

                currentMatch.addHit(0, peptideAssumption, false);

                if(spectrumTitleToRank.get(spectrumTitle) == 1){
                    currentMatch.setBestPeptideAssumption(peptideAssumption);
                }
            }

            lineCount ++;
        }
        bufferedReader.close();

    }

    private void parseParameters() throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(parametersXMLFile));
        String line;
        int silacCount = 0;

        while ((line = bufferedReader.readLine()) != null){
            if (line.contains("fixedModifications")){
                while (!(line = bufferedReader.readLine()).contains("fixedModifications")){
                    String modificationName = line.split(">")[1].split("<")[0];
                    String mod = modificationName.split(" ")[0];
                    String aa = modificationName.split("\\(")[1].replace(")", "");
                    fixedModificationMap.put(aa, mod);
                }
            }
            if (line.contains("<IsobaricLabelInfo")){
                line = bufferedReader.readLine();
                if (line.contains("TMT10plex")){
                    fixedModificationMap.put("K", "TMT 10-plex");
                    fixedModificationMap.put("peptide N-term", "TMT 10-plex");
                } else if (line.contains("TMT8plex")){
                    fixedModificationMap.put("K", "TMT 8-plex");
                    fixedModificationMap.put("peptide N-term", "TMT 8-plex");
                } else if (line.contains("TMT6plex")){
                    fixedModificationMap.put("K", "TMT 6-plex");
                    fixedModificationMap.put("peptide N-term", "TMT 6-plex");
                } else if (line.contains("TMT2plex")){
                    fixedModificationMap.put("K", "TMT 2-plex");
                    fixedModificationMap.put("peptide N-term", "TMT 2-plex");
                } else if (line.contains("iTRAQ4plex")){
                    fixedModificationMap.put("K", "iTRAQ 4-plex");
                    fixedModificationMap.put("peptide N-term", "iTRAQ 4-plex");
                } else if (line.contains("iTRAQ8plex")){
                    fixedModificationMap.put("K", "iTRAQ 8-plex");
                    fixedModificationMap.put("peptide N-term", "iTRAQ 8-plex");
                }
            }
            if (line.contains("labelMods")){
                while (!(line = bufferedReader.readLine()).contains("labelMods")){
                    if (line.contains("<string></string>")){
                    } else if (line.contains("<string />")){
                    } else{
                        silacCount ++;
                        HashMap<String, Integer> labelDetailMap = new HashMap<>();
                        System.out.println("Line is " + line);
                        String label = line.split(">")[1].split("<")[0].replace(" ", "");
                        System.out.println("Label is "+ label);
                        for (String each : label.split(";")){
                            if (each.substring(0,3).equals("Arg")){
                                labelDetailMap.put("R", Integer.valueOf(each.substring(3)));
                            } else if (each.substring(0,3).equals("Lys")){
                                labelDetailMap.put("K", Integer.valueOf(each.substring(3)));
                            }
                        }
                        if (silacCount == 1){
                            silacLabelMap.put("one", labelDetailMap);
                        } else if (silacCount == 2){
                            silacLabelMap.put("two", labelDetailMap);
                        }
                    }
                }

                silacModMap.put("R=6", "Label:13C(6) of R");
                silacModMap.put("R=10", "Label:13C(6)15N(4) of R");
                silacModMap.put("K=4", "Label:15N(4) of K");
                silacModMap.put("K=6", "Label:13C(6) of K");
                silacModMap.put("K=8", "Label:13C(6)15N(2) of K");

            }
        }
    }

    /**
     * Get all spectrum matches
     * @return Hashmap
     */
    public HashMap<String, SpectrumMatch> getAllSpectrumMatches() {

        return allSpectrumMatches;
    }

    /**
     * Get spectrum factory
     * @return Spectrum factory
     */
    public SpectrumFactory getSpectrumFactory() {
        return spectrumFactory;
    }
}
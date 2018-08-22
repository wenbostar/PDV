package PDVCLI;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;

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
     * All sil apl files list
     */
    private ArrayList<File> allSilAPLFiles = new ArrayList<>();
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
     * Constructor
     * @param maxQuantDirectory MaxQuant results folder path
     * @throws IOException
     */
    public MaxQuantImport(File maxQuantDirectory) throws IOException {

        this.maxQuantDirectory = maxQuantDirectory;

        getAllFiles();
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
                        if(eachFileInAnd.getName().contains(".apl") && eachFileInAnd.getName().contains(".iso")){
                            allAPLFiles.add(eachFileInAnd);
                        } else if (eachFileInAnd.getName().contains(".apl") && !eachFileInAnd.getName().contains(".iso")){
                            allSilAPLFiles.add(eachFileInAnd);
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

        ArrayList<String> allTitle = new ArrayList<>();

        ArrayList<String> isoTitle = new ArrayList<>();
        ArrayList<String> otherTitle = new ArrayList<>();

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

            BufferedWriter fileWriter = null;
            String spectrumTitle;
            String spectrumFileName;
            String scanNum;
            String mass = null;
            String charge = null;
            boolean isFirstMentioned = true;
            for (File eachAPLFile : allAPLFiles) {

                BufferedReader bufferedReader = new BufferedReader(new FileReader(eachAPLFile));
                String line;
                boolean insideSpectrum = false;

                while ((line = bufferedReader.readLine()) != null) {

                    if (line.endsWith("\r")) {
                        line = line.replace("\r", "");
                    }

                    if (line.startsWith("peaklist start")) {
                        insideSpectrum = true;

                    } else if (line.startsWith("header")) {
                        spectrumTitle = line.split("=")[1];
                        spectrumFileName = spectrumTitle.split(" ")[1];
                        scanNum = spectrumTitle.split(" ")[3];

                        spectrumTitle = "RawFile: " + spectrumFileName + " Index: " + scanNum;

                            currentScanNumList = fileNameToScanNum.get(spectrumFileName);

                            if (currentScanNumList != null) {

                                if (currentScanNumList.contains(scanNum)) {
                                    isFirstMentioned = false;
                                } else {
                                    fileNameToScanNum.get(spectrumFileName).add(scanNum);
                                    isFirstMentioned = true;
                                }

                                if (isFirstMentioned) {
                                    fileWriter = fileNameToFileWriter.get(spectrumFileName);

                                    fileWriter.write("BEGIN IONS\n");
                                    fileWriter.write("TITLE=" + spectrumTitle + "\n");
                                    fileWriter.write("PEPMASS=" + mass + "\n");
                                    fileWriter.write("CHARGE=" + charge + "+\n");
                                    fileWriter.write("RTINSECONDS=" + Double.valueOf(fileToTitleToRTMap.get(spectrumFileName).get(spectrumTitle)) * 60 + "\n");
                                }
                            } else {
                                System.err.println("It can not find this file ");
                                isFirstMentioned = false;
                            }

                    } else if (line.startsWith("mz")) {
                        mass = line.split("=")[1];

                    } else if (line.startsWith("charge")) {
                        charge = line.split("=")[1];

                    } else if (line.startsWith("peaklist end")) {
                        if (isFirstMentioned) {
                            fileWriter.write("END IONS\n");
                        }

                    } else if (line.startsWith("fragmentation")) {

                    } else if (insideSpectrum && !line.equals("")) {
                        if (isFirstMentioned) {
                            fileWriter.write(line + "\n");
                        }
                    }
                }
                bufferedReader.close();
            }

            for (File file : allSilAPLFiles){
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                boolean insideSpectrum = false;

                while ((line = bufferedReader.readLine()) != null) {

                    if (line.endsWith("\r")) {
                        line = line.replace("\r", "");
                    }

                    if (line.startsWith("peaklist start")) {
                        insideSpectrum = true;

                    } else if (line.startsWith("header")) {
                        spectrumTitle = line.split("=")[1];
                        spectrumFileName = spectrumTitle.split(" ")[1];
                        scanNum = spectrumTitle.split(" ")[3];

                        spectrumTitle = "RawFile: " + spectrumFileName + " Index: " + scanNum;

                            currentScanNumList = fileNameToScanNum.get(spectrumFileName);

                            if (currentScanNumList != null) {

                                if (currentScanNumList.contains(scanNum)) {
                                    isFirstMentioned = false;
                                } else {
                                    fileNameToScanNum.get(spectrumFileName).add(scanNum);
                                    isFirstMentioned = true;
                                }

                                if (isFirstMentioned) {
                                    fileWriter = fileNameToFileWriter.get(spectrumFileName);

                                    fileWriter.write("BEGIN IONS\n");
                                    fileWriter.write("TITLE=" + spectrumTitle + "\n");
                                    fileWriter.write("PEPMASS=" + mass + "\n");
                                    fileWriter.write("CHARGE=" + charge + "+\n");
                                    fileWriter.write("RTINSECONDS=" + Double.valueOf(fileToTitleToRTMap.get(spectrumFileName).get(spectrumTitle)) * 60 + "\n");
                                }
                            } else {
                                System.err.println("It can not find this file ");
                                isFirstMentioned = false;
                            }

                    } else if (line.startsWith("mz")) {
                        mass = line.split("=")[1];

                    } else if (line.startsWith("charge")) {
                        charge = line.split("=")[1];

                    } else if (line.startsWith("peaklist end")) {
                        if (isFirstMentioned) {
                            fileWriter.write("END IONS\n");
                        }

                    } else if (line.startsWith("fragmentation")) {

                    } else if (insideSpectrum && !line.equals("")) {
                        if (isFirstMentioned) {
                            fileWriter.write(line + "\n");
                        }
                    }

                }
            }

            fileWriter.close();
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
                }

            }
        }
    }

    /**
     * Read result from file
     * @throws IOException
     */
    private void parseResults() throws IOException {

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
        Double massError;
        Double score;
        String utilitiesModificationName;
        String[] splitAABy;
        String[] modifications;
        String[] splitNum;
        PeptideAssumption peptideAssumption;
        Peptide peptide;
        Charge charge;

        HashMap<String, ArrayList<String>> fileNameToIDs;
        HashMap<String, String> aAToModification;
        ArrayList<ModificationMatch> utilitiesModifications;
        SpectrumMatch currentMatch;

        HashMap<String, Integer> modificationNumIndex = new HashMap<>();
        HashMap<String, Integer> modificationPosIndex = new HashMap<>();
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
                score = Double.valueOf(values[scoreIndex]);

                spectrumTitle = "RawFile: " + rawFileName + " Index: " + scanNumber;

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

                try {
                    if (!modificationName.equals("Unmodified")) {
                        /*
                        for (ArrayList<String> eachList : msIDSToModificationFileToEachSite.keySet()){
                            if (eachList.contains(String.valueOf(lineCount - 1))){
                                fileNameToIDs = msIDSToModificationFileToEachSite.get(eachList);

                                for (String fileName : fileNameToIDs.keySet()) {
                                    for (String eachID : fileNameToIDs.get(fileName)) {
                                        Integer site = Integer.valueOf(fileNameToPositionList.get(fileName).get(Integer.parseInt(eachID)));
                                        String modSeq = fileNameToSeqList.get(fileName).get(Integer.parseInt(eachID));
                                        if (! modSeq.equals(sequence)) {
                                            if (modSeq.length() > sequence.length()) {
                                                if (modSeq.contains(sequence)){
                                                    site = site - modSeq.split(sequence)[0].length();
                                                } else {
                                                    String origin = "";
                                                    for (int i = 0; i < sequence.length(); i++) {
                                                        for (int j = sequence.length(); j > i; j--) {
                                                            String s3 = sequence.substring(i, j);
                                                            if (modSeq.contains(s3)) {
                                                                if (s3.length() > origin.length()){
                                                                    origin = s3;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    site = site - modSeq.split(origin)[0].length() + sequence.split(origin)[0].length();
                                                }
                                            } else {
                                                if (sequence.contains(modSeq)){
                                                    site = site + sequence.split(modSeq)[0].length();
                                                } else {
                                                    String origin = "";
                                                    for (int i = 0; i < modSeq.length(); i++) {
                                                        for (int j = modSeq.length(); j > i; j--) {
                                                            String s3 = modSeq.substring(i, j);
                                                            if (sequence.contains(s3)) {
                                                                if (s3.length() > origin.length()){
                                                                    origin = s3;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    site = site - modSeq.split(origin)[0].length() + sequence.split(origin)[0].length();
                                                }
                                            }
                                        }
                                        utilitiesModificationName = fileName.split(" \\(")[0].replace(">","&gt;") + " of " + sequence.charAt(site - 1);
                                        utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, site));
                                    }
                                }
                            }
                        }*/

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
                                    utilitiesModificationName = modName.split(" \\(")[0].replace(">","&gt;") + " of " + sequence.charAt(position - 1);

                                    utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, position));
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
                                utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, 1));
                            } else if (eachModificationPart == splitAABy[splitAABy.length - 1] && !eachModificationPart.contains("_")) {
                                if (aAToModification.get("C-term") == null){
                                    utilitiesModificationName = aAToModification.get("Protein C-term") + " of " + "protein C-term";
                                } else {
                                    utilitiesModificationName = aAToModification.get("C-term") + " of " + "C-term";
                                }
                                utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, sequence.length()));
                            }
                        }
                    }
                } catch (Exception e){
                    System.err.println("Failed to parse modification.");
                    e.printStackTrace();
                }

                String[] sequenceArray = sequence.split("");
                int length =  sequenceArray.length;
                for (int index = 0; index < length; index ++){
                    String aa = sequenceArray[index];
                    if (fixedModificationMap.containsKey(aa)){
                        utilitiesModificationName = fixedModificationMap.get(aa) + " of " + aa;
                        utilitiesModifications.add(new ModificationMatch(utilitiesModificationName, true, index + 1));
                    }
                }

                peptide = new Peptide(sequence, utilitiesModifications);

                charge = new Charge(+1, peptideCharge);

                peptideAssumption = new PeptideAssumption(peptide, spectrumTitleToRank.get(spectrumTitle), 0, charge, massError, "maxQuant");
                peptideAssumption.setRawScore(score);

                currentMatch.addHit(0, peptideAssumption, false);

                if(spectrumTitleToRank.get(spectrumTitle) == 1){
                    currentMatch.setBestPeptideAssumption(peptideAssumption);
                }
            }

            lineCount ++;
        }
        bufferedReader.close();

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
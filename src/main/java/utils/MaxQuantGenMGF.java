package utils;

import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class MaxQuantGenMGF {

    /**
     * All apl files list
     */
    private ArrayList<File> allAPLFiles;
    /**
     * fileNameToFileWriter
     */
    private HashMap<String, BufferedWriter> fileNameToFileWriter = new HashMap<>();
    /**
     * Progress bar
     */
    private ProgressDialogX progressDialog = null;
    /**
     * File name to scan number
     */
    private HashMap<String, ArrayList<String>> fileNameToScanNum = new HashMap<>();
    /**
     * File to title to RT map
     */
    private HashMap<String, HashMap<String, String>> fileToTitleToRTMap = new HashMap<>();

    /**
     *
     * @param allAPLFiles
     * @param fileNameToFileWriter
     * @param fileNameToScanNum
     * @param fileToTitleToRTMap
     * @param progressDialog
     * @throws IOException
     */
    public MaxQuantGenMGF(ArrayList<File> allAPLFiles, HashMap<String, BufferedWriter> fileNameToFileWriter,
                          HashMap<String, ArrayList<String>> fileNameToScanNum, HashMap<String, HashMap<String, String>> fileToTitleToRTMap,
                          ProgressDialogX progressDialog) throws IOException {
        this.allAPLFiles = allAPLFiles;
        this.fileNameToFileWriter = fileNameToFileWriter;
        this.fileNameToScanNum = fileNameToScanNum;
        this.fileToTitleToRTMap = fileToTitleToRTMap;
        this.progressDialog = progressDialog;

        writeSpectrum();
    }

    public MaxQuantGenMGF(ArrayList<File> allAPLFiles, HashMap<String, BufferedWriter> fileNameToFileWriter,
                          HashMap<String, ArrayList<String>> fileNameToScanNum, HashMap<String, HashMap<String, String>> fileToTitleToRTMap) throws IOException {
        this.allAPLFiles = allAPLFiles;
        this.fileNameToFileWriter = fileNameToFileWriter;
        this.fileNameToScanNum = fileNameToScanNum;
        this.fileToTitleToRTMap = fileToTitleToRTMap;

        writeSpectrum();
    }


    public void writeSpectrum() throws IOException {

        ArrayList<String> currentScanNumList;
        BufferedWriter fileWriter = null;
        String spectrumTitle;
        String partTitle;
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

                    if (line.contains("Precursor")){
                        spectrumTitle = line.split(" Precursor")[0].split("=")[1];
                    } else {
                        spectrumTitle = line.split(" Silind")[0].split("=")[1];
                    }

                    spectrumFileName = spectrumTitle.split("RawFile: ")[1].split(" Index")[0];
                    scanNum = spectrumTitle.split("RawFile: ")[1].split(" Index: ")[1];

                    currentScanNumList = fileNameToScanNum.get(spectrumFileName);

                    if (currentScanNumList != null) {

                        if (currentScanNumList.contains(scanNum+"_"+charge)) {
                            isFirstMentioned = false;
                        } else {
                            fileNameToScanNum.get(spectrumFileName).add(scanNum+"_"+charge);
                            isFirstMentioned = true;
                        }

                        if (isFirstMentioned) {
                            fileWriter = fileNameToFileWriter.get(spectrumFileName);

                            spectrumTitle = "RawFile: " + spectrumFileName + " Index: " + scanNum + " Charge: " + charge;
                            partTitle = "RawFile: " + spectrumFileName + " Index: " + scanNum;

                            fileWriter.write("BEGIN IONS\n");
                            fileWriter.write("TITLE=" + spectrumTitle + "\n");
                            fileWriter.write("PEPMASS=" + mass + "\n");
                            fileWriter.write("CHARGE=" + charge + "+\n");
                            fileWriter.write("RTINSECONDS=" + Double.parseDouble(fileToTitleToRTMap.get(spectrumFileName).get(partTitle)) * 60 + "\n");

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

            if (progressDialog != null){
                progressDialog.increasePrimaryProgressCounter();
            }
        }
    }
}

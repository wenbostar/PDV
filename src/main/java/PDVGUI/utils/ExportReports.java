package PDVGUI.utils;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.SequenceMatchingPreferences;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Export the reports
 * Created by Ken on 12/19/2017.
 */
public class ExportReports {

    /**
     * Out folder path
     */
    private String outputPath;
    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings;
    /**
     * SQLite connection to get data
      */
    private SQLiteConnection sqLiteConnection;
    /**
     * All possible columns
     */
    private ArrayList<String> allColumn;
    /**
     * All export spectrum key
     */
    private ArrayList<String> exportSpectrumKey;
    /**
     * The extral parameter from file
     */
    private ArrayList<String> scoreName;
    /**
     * Spectrum annotator
     */
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * Spectrum tagSpectrumAnnotator
     */
    private TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();

    /**
     * Constructor
     * @param outputPath Output path
     * @param pdvMainClass Parent class
     * @param allColumns All column
     * @param exportSpectrumKey Export spectrum keys
     * @param annotationLevel Annotation level
     * @param fragmentIonMZTolerance Tolerance
     * @param unit Tolerance type
     */
    public ExportReports(String outputPath, PDVMainClass pdvMainClass, ArrayList<String> allColumns, ArrayList<String> exportSpectrumKey, Double annotationLevel,
                         Double fragmentIonMZTolerance, Integer unit){
        this.pdvMainClass = pdvMainClass;
        this.outputPath = outputPath;
        this.sqLiteConnection = pdvMainClass.getSqliteConnection();
        this.allColumn = allColumns;
        this.exportSpectrumKey = exportSpectrumKey;
        this.scoreName = pdvMainClass.getScoreName();
        this.annotationSettings = pdvMainClass.getAnnotationSettings();

        SearchParameters searchParameters = pdvMainClass.getSearchParameters();
        searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);
        if(unit == 0){
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.DA);
        }else {
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);
        }
        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        annotationSettings.setIntensityFilter(annotationLevel);

        exportIntoFile();
    }

    /**
     * Write the result file
     */
    private void exportIntoFile(){

        ProgressDialogX progressDialog = new ProgressDialogX(pdvMainClass,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Export Reports. Please Wait...");
        progressDialog.setUnstoppable(true);

        new Thread(() -> progressDialog.setVisible(true), "ProgressDialog").start();

        progressDialog.setMaxPrimaryProgressCounter(exportSpectrumKey.size());

        new Thread("Export") {

            @Override
            public void run() {
                try {
                    FileWriter fileWriter = new FileWriter(outputPath + "/PDVExportResult.txt");

                    Boolean read = false;
                    HashMap<String, Object> spectrumDetails = new HashMap<>();
                    HashMap<String, Object> scoreAndValue = new HashMap<>();

                    fileWriter.write("Spectrum_Title");

                    for (String eachColumn : allColumn) {
                        if (Objects.equals(eachColumn, "Annotations")){
                            fileWriter.write("\tions\tions_mz\tions_int");
                        } else {
                            fileWriter.write("\t" + eachColumn);
                        }

                    }
                    fileWriter.write("\n");

                    if (allColumn.contains("b ions") || allColumn.contains("y ions") || allColumn.contains("by pairs") ||
                            allColumn.contains("Annotations") || allColumn.contains("Sequence") || allColumn.contains("Ratio")){
                        read = true;
                    }

                    for (String eachKey : exportSpectrumKey){
                        if (!progressDialog.isRunCanceled()) {
                            MSnSpectrum mSnSpectrum;
                            SpectrumIdentificationAssumption spectrumIdentificationAssumption;

                            mSnSpectrum = pdvMainClass.getSpectrum(eachKey);
                            fileWriter.write(mSnSpectrum.getSpectrumTitle());

                            spectrumIdentificationAssumption = pdvMainClass.getspectrumIdentificationAssumption();

                            if (read){
                                spectrumDetails = getSpectrumDetails(eachKey, spectrumIdentificationAssumption, mSnSpectrum);
                            }

                            ArrayList<Object> oneItem = sqLiteConnection.getOneSpectrumItem(eachKey);

                            for (int i = 0; i<scoreName.size(); i ++){
                                scoreAndValue.put(scoreName.get(i), oneItem.get(i+1));
                            }

                            for (String eachColumn : allColumn){
                                if (Objects.equals(eachColumn, "Annotations")){
                                    fileWriter.write("\t" + spectrumDetails.get("ions"));
                                    fileWriter.write("\t" + spectrumDetails.get("ions_mz"));
                                    fileWriter.write("\t" + spectrumDetails.get("ions_int"));
                                }
                                if (spectrumDetails.containsKey(eachColumn)){
                                    fileWriter.write("\t" + spectrumDetails.get(eachColumn));
                                } else if (scoreAndValue.containsKey(eachColumn)){
                                    fileWriter.write("\t" + scoreAndValue.get(eachColumn));
                                }
                            }

                            fileWriter.write("\n");

                            progressDialog.increasePrimaryProgressCounter();
                        }
                    }

                    progressDialog.setRunFinished();
                    fileWriter.close();

                } catch (IOException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                } catch (SQLException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Get spectrum and match details
     * @param psmKey Spectrum key
     * @param spectrumIdentificationAssumption Spectrum identification
     * @param mSnSpectrum Spectrum
     * @return Details
     */
    private HashMap<String, Object> getSpectrumDetails(String psmKey, SpectrumIdentificationAssumption spectrumIdentificationAssumption, MSnSpectrum mSnSpectrum){

        HashMap<String, Object> spectrumDetails = new HashMap<>();

        try {
            String currentPeptideSequence;
            ArrayList<IonMatch> annotations;

            SpecificAnnotationSettings specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(psmKey, spectrumIdentificationAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);

            specificAnnotationSettings.setNeutralLossesAuto(false);
            specificAnnotationSettings.clearNeutralLosses();

            specificAnnotationSettings.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
            specificAnnotationSettings.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.B_ION);

            specificAnnotationSettings.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
            specificAnnotationSettings.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

            if (spectrumIdentificationAssumption instanceof TagAssumption) {
                TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                currentPeptideSequence = tagAssumption.getTag().asSequence();

                annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, mSnSpectrum, tagAssumption.getTag());
            } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                Peptide currentPeptide = peptideAssumption.getPeptide();
                currentPeptideSequence = currentPeptide.getSequence();

                annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, mSnSpectrum, peptideAssumption.getPeptide(), null, pdvMainClass.ptmFactory, true);
            } else {
                throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
            }

            DecimalFormat df = new DecimalFormat("#.00");

            Double allMatchInt = 0.0;
            Double allPeakInt = 0.0;
            HashMap<Integer, ArrayList<String>> bIonMap = new HashMap<>();
            HashMap<Integer, ArrayList<String>> yIonMap = new HashMap<>();
            ArrayList<String> bIonList;
            ArrayList<String> yIonList;
            ArrayList<String> ionMatches = new ArrayList<>();
            ArrayList<Double> ionsMzs = new ArrayList<>();
            ArrayList<Double> ionInts = new ArrayList<>();
            for (IonMatch ionMatch : annotations){
                String match = ionMatch.getPeakAnnotation();
                ionMatches.add(match);
                ionsMzs.add(ionMatch.peak.mz);
                ionInts.add(ionMatch.peak.intensity);

                Integer charge = ionMatch.charge;

                if (match.contains("b") && !match.contains("-")){

                    if (bIonMap.containsKey(charge)){
                        bIonMap.get(charge).add(match.replace("+",""));
                    } else {
                        bIonList = new ArrayList<>();
                        bIonList.add(match.replace("+",""));
                        bIonMap.put(charge, bIonList);
                    }

                } else if (match.contains("y") && !match.contains("-")){
                    if (yIonMap.containsKey(charge)){
                        yIonMap.get(charge).add(match.replace("+",""));
                    } else {
                        yIonList = new ArrayList<>();
                        yIonList.add(match.replace("+",""));
                        yIonMap.put(charge, yIonList);
                    }
                }

                allMatchInt += ionMatch.peak.getIntensity();
            }

            for (Double each : mSnSpectrum.getIntensityValuesAsArray()){
                allPeakInt += each;
            }

            Double ratio = allMatchInt/allPeakInt;

            Integer[] nums = getPair(bIonMap, yIonMap, currentPeptideSequence.length());

            if (allColumn.contains("b ions")){
                spectrumDetails.put("b ions", nums[0]);
            }
            if (allColumn.contains("y ions")){
                spectrumDetails.put("y ions", nums[1]);
            }
            if (allColumn.contains("by pairs")){
                spectrumDetails.put("by pairs", nums[2]);
            }
            if (allColumn.contains("Sequence")){
                spectrumDetails.put("Sequence", currentPeptideSequence);
            }
            if (allColumn.contains("Ratio")){
                spectrumDetails.put("Ratio", ratio);
            }
            if (allColumn.contains("Annotations")){
                spectrumDetails.put("ions", ionMatches);
                spectrumDetails.put("ions_mz", ionsMzs);
                spectrumDetails.put("ions_int", ionInts);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return spectrumDetails;

    }

    /**
     * Get by pairs
     * @param bIonMap B ion map
     * @param yIonMap Y ion map
     * @param length Peptide length
     * @return All details
     */
    private Integer[] getPair(HashMap<Integer, ArrayList<String>> bIonMap, HashMap<Integer, ArrayList<String>> yIonMap, Integer length){
        Integer[] nums = new Integer[3];
        Integer bIonNum = 0;
        Integer yIonNum = 0;
        Integer pairNum = 0;
        ArrayList<String> bIonList;
        ArrayList<String> yIonList;

        for (Integer eachCharge : bIonMap.keySet()){
            bIonList = bIonMap.get(eachCharge);

            bIonNum += bIonList.size();

            if (yIonMap.containsKey(eachCharge)){
                yIonList = yIonMap.get(eachCharge);

                for (String eachMatch : bIonList){
                    for (String eachYMatch : yIonList){
                        if (Integer.valueOf(eachMatch.substring(1, eachMatch.length())) + Integer.valueOf(eachYMatch.substring(1,eachYMatch.length())) == length){
                            pairNum ++;
                        }
                    }
                }
            }
        }
        for (Integer eachCharge : yIonMap.keySet()){
            yIonList = yIonMap.get(eachCharge);

            yIonNum += yIonList.size();
        }
        nums[0] = bIonNum;
        nums[1] = yIonNum;
        nums[2] = pairNum;

        return nums;
    }
}

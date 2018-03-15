package PDVGUI.gui.utils.Export;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.SpectrumLibDisplay;
import PDVGUI.utils.Export;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.TagSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.batik.transcoder.TranscoderException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Real time export batch spectrum
 * Created by Ken on 8/9/2017.
 */
public class RealTimeExportJDialog extends JDialog {

    private JPanel secondarySpectrumPlotsJPanel;
    private JPanel spectrumJPanel;
    private JPanel resizeJPanel;
    private JSplitPane spectrumSplitPane;

    /**
     * Forward ions searched for (a, b or c)
     */
    private Integer forwardIon;
    /**
     * Reward ions searched for (x, y or z)
     */
    private Integer rewindIon;
    /**
     * SpectrumPanel to paint spectrum import from utilities
     */
    private SpectrumPanel spectrumPanel;
    /**
     * Utilities user preferences
     */
    private UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings;
    /**
     * Spectrum annotator
     */
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator;
    /**
     * Spectrum tagSpectrumAnnotator
     */
    private TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();
    /**
     * SpecificAnnotationSettings
     */
    private SpecificAnnotationSettings specificAnnotationSettings;
    /**
     * Delta mass map
     */
    private HashMap<Double, String> deltaMassMap;
    /**
     * PTM settings
     */
    private PtmSettings ptmSettings;
    /**
     * Image type
     */
    private ImageType imageType;
    /**
     * Output path
     */
    private String outputPath;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Parent class
     */
    private SpectrumLibDisplay spectrumLibDisplay;
    /**
     * Spectrum panel height
     */
    private Integer height;
    /**
     * Spectrum panel width
     */
    private Integer width;
    /**
     * The length unit
     */
    private String unit;
    /**
     * Remove illegal pattern
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");
    /**
     * The resize panel width
     */
    private Integer resizeJPanelWidth;
    /**
     * The resize panel length
     */
    private Integer resizeJPanelHeight;
    /**
     * Parent jFrame
     */
    private JFrame parentFrame;

    /**
     * Constructor
     *@param forwardIon Forward Ion
     * @param rewindIon Rewind Ion
     * @param height Height
     * @param width Width
     * @param unit Unit
     * @param peptideSpectrumAnnotator Peptide spectrum annotator
     * @param specificAnnotationSettings specificAnnotationSettings
     * @param pdvMainClass Parent class
     * @param deltaMassMap deltaMassMap
     * @param ptmSettings ptmSettings
     * @param imageType imageType
     * @param outputPath outputPath
     */
    public RealTimeExportJDialog(Integer forwardIon, Integer rewindIon, Integer height, Integer width, String unit
            , PeptideSpectrumAnnotator peptideSpectrumAnnotator, SpecificAnnotationSettings specificAnnotationSettings, PDVMainClass pdvMainClass
            , HashMap<Double, String> deltaMassMap, PtmSettings ptmSettings, ImageType imageType, String outputPath){
        
        this.forwardIon = forwardIon;
        this.rewindIon = rewindIon;
        this.pdvMainClass = pdvMainClass;
        this.parentFrame = pdvMainClass;
        this.utilitiesUserPreferences = pdvMainClass.utilitiesUserPreferences;
        this.annotationSettings = pdvMainClass.getAnnotationSettings();
        this.peptideSpectrumAnnotator = peptideSpectrumAnnotator;
        this.specificAnnotationSettings = specificAnnotationSettings;
        this.deltaMassMap = deltaMassMap;
        this.ptmSettings = ptmSettings;
        this.imageType = imageType;
        this.outputPath = outputPath;
        this.height = height;
        this.width = width;
        this.unit = unit;

        initComponent();

        this.setVisible(true);
    }

    /**
     * Constructor
     * @param forwardIon Forward Ion
     * @param rewindIon Rewind Ion
     * @param height Height
     * @param width Width
     * @param unit Unit
     * @param peptideSpectrumAnnotator Peptide spectrum annotator
     * @param specificAnnotationSettings specificAnnotationSettings
     * @param spectrumLibDisplay Parent class
     * @param deltaMassMap deltaMassMap
     * @param ptmSettings ptmSettings
     * @param imageType imageType
     * @param outputPath outputPath
     */
    public RealTimeExportJDialog(Integer forwardIon, Integer rewindIon, Integer height, Integer width, String unit
            , PeptideSpectrumAnnotator peptideSpectrumAnnotator, SpecificAnnotationSettings specificAnnotationSettings, SpectrumLibDisplay spectrumLibDisplay
            , HashMap<Double, String> deltaMassMap, PtmSettings ptmSettings, ImageType imageType, String outputPath){

        this.forwardIon = forwardIon;
        this.rewindIon = rewindIon;
        this.spectrumLibDisplay = spectrumLibDisplay;
        this.parentFrame = spectrumLibDisplay;
        this.utilitiesUserPreferences = spectrumLibDisplay.utilitiesUserPreferences;
        this.annotationSettings = spectrumLibDisplay.annotationSettings;
        this.peptideSpectrumAnnotator = peptideSpectrumAnnotator;
        this.specificAnnotationSettings = specificAnnotationSettings;
        this.deltaMassMap = deltaMassMap;
        this.ptmSettings = ptmSettings;
        this.imageType = imageType;
        this.outputPath = outputPath;
        this.height = height;
        this.width = width;
        this.unit = unit;

        initComponent();

        this.setVisible(true);
    }

    /**
     * Init all GUI component
     */
    private void initComponent(){
        spectrumJPanel = new JPanel();
        secondarySpectrumPlotsJPanel = new JPanel();
        spectrumSplitPane = new JSplitPane();
        resizeJPanel = new JPanel();
        JPanel spectrumOuterJPanel = new JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Export");
        setBackground(new java.awt.Color(255, 255, 255));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        spectrumSplitPane.setBackground(Color.WHITE);
        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(90);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        secondarySpectrumPlotsJPanel.setMinimumSize(new Dimension(0, 80));
        secondarySpectrumPlotsJPanel.setOpaque(false);
        secondarySpectrumPlotsJPanel.setLayout(new BoxLayout(secondarySpectrumPlotsJPanel, BoxLayout.LINE_AXIS));

        spectrumSplitPane.setTopComponent(secondarySpectrumPlotsJPanel);

        spectrumOuterJPanel.setBackground(new Color(255, 255, 255));

        spectrumJPanel.setBackground(new Color(255, 255, 255));
        spectrumJPanel.setLayout(new BorderLayout());

        GroupLayout spectrumOuterJPanelLayout = new GroupLayout(spectrumOuterJPanel);
        spectrumOuterJPanel.setLayout(spectrumOuterJPanelLayout);
        spectrumOuterJPanelLayout.setHorizontalGroup(
                spectrumOuterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        spectrumOuterJPanelLayout.setVerticalGroup(
                spectrumOuterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumOuterJPanelLayout.createSequentialGroup()
                                .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );

        spectrumSplitPane.setRightComponent(spectrumOuterJPanel);

        resizeJPanel.add(spectrumSplitPane);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(resizeJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(resizeJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Closes the dialog
     * @param evt window event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Parsing all spectrum
     * @param allSpectrums All spectrum keys
     */
    public void readAllSpectrums(ArrayList<ArrayList<String>> allSpectrums){

        ProgressDialogX progressDialog = new ProgressDialogX(parentFrame,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Export Selected spectrum. Please Wait...");
        progressDialog.setUnstoppable(true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        int size = 0;
        for(ArrayList<String> countSize:allSpectrums){
            size = size + countSize.size();
        }
        progressDialog.setMaxPrimaryProgressCounter(size);

        new Thread("Export") {

            @Override
            public void run() {
                try {

                    FormLayout formLayout = new FormLayout(width + unit, height + unit);

                    resizeJPanel.setLayout(formLayout);
                    resizeJPanel.revalidate();
                    resizeJPanel.repaint();

                    resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
                    resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));

                    if (resizeJPanelWidth < 200) {
                        JOptionPane.showMessageDialog(null, "The width must be bigger than 200 pixels or " + 200 / Toolkit.getDefaultToolkit().getScreenResolution() + " inch.", "Warning", JOptionPane.WARNING_MESSAGE);

                        progressDialog.setRunFinished();

                    } else {

                        spectrumSplitPane.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);
                        spectrumSplitPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));

                        for (ArrayList<String> eachPage : allSpectrums) {
                            for (String spectrumKey : eachPage) {
                                if (!progressDialog.isRunCanceled()) {
                                    MSnSpectrum mSnSpectrum;
                                    SpectrumIdentificationAssumption spectrumIdentificationAssumption;

                                    if (pdvMainClass != null) {
                                        mSnSpectrum = pdvMainClass.getSpectrum(spectrumKey);
                                        spectrumIdentificationAssumption = pdvMainClass.getspectrumIdentificationAssumption();
                                    } else {
                                        mSnSpectrum = spectrumLibDisplay.getSpectrum(Integer.valueOf(spectrumKey));
                                        spectrumIdentificationAssumption = spectrumLibDisplay.getSpectrumIdentificationAssumption();
                                    }
                                    updateSpectrum(spectrumKey, mSnSpectrum, spectrumIdentificationAssumption);
                                    Thread.sleep(100);

                                    String outputPathName = outputPath + "/" + FilePattern.matcher(mSnSpectrum.getSpectrumTitle()).replaceAll("") + imageType.getExtension();
                                    exportFigure(outputPathName);

                                    progressDialog.increasePrimaryProgressCounter();
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e){
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(parentFrame, JOptionEditorPane.getJOptionEditorPane(
                            "Failed to export.<br>"
                                    + "Please <a href=\"https://github.com/wenbostar/PDV\">contact the developers</a>."),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
                progressDialog.setRunFinished();
                formWindowClosing(null);
            }
        }.start();
    }

    /**
     * Reading all selections
     * @param allSelections All selections
     */
    public void readAllSelections(ArrayList<String> allSelections){
        ProgressDialogX progressDialog = new ProgressDialogX(parentFrame,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Export Selected spectrum. Please Wait...");

        progressDialog.setUnstoppable(true);

        new Thread(new Runnable() {

            public void run() {
                progressDialog.setVisible(true);
            }
        }, "ProgressDialog").start();

        progressDialog.setMaxPrimaryProgressCounter(allSelections.size());
        new Thread("Export") {

            @Override
            public void run() {
                try {

                    FormLayout formLayout = new FormLayout(width+unit, height+unit);

                    resizeJPanel.setLayout(formLayout);
                    resizeJPanel.revalidate();
                    resizeJPanel.repaint();

                    resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
                    resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));

                    if(resizeJPanelWidth < 200){
                        JOptionPane.showMessageDialog(null, "The width must be bigger than 200 pixels or "+200/Toolkit.getDefaultToolkit().getScreenResolution()+" inch.", "Warning", JOptionPane.WARNING_MESSAGE);

                        progressDialog.setRunFinished();

                    } else {

                        spectrumSplitPane.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);
                        spectrumSplitPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));

                        for (String eachKey : allSelections) {
                            if (!progressDialog.isRunCanceled()) {
                                MSnSpectrum mSnSpectrum;
                                SpectrumIdentificationAssumption spectrumIdentificationAssumption;

                                if (pdvMainClass != null) {
                                    mSnSpectrum = pdvMainClass.getSpectrum(eachKey);
                                    spectrumIdentificationAssumption = pdvMainClass.getspectrumIdentificationAssumption();
                                } else {
                                    mSnSpectrum = spectrumLibDisplay.getSpectrum(Integer.valueOf(eachKey));
                                    spectrumIdentificationAssumption = spectrumLibDisplay.getSpectrumIdentificationAssumption();
                                }
                                updateSpectrum(eachKey, mSnSpectrum, spectrumIdentificationAssumption);

                                if (allSelections.indexOf(eachKey) == 0){
                                    Thread.sleep(200);
                                }
                                Thread.sleep(100);

                                String outputPathName = outputPath + "/" + FilePattern.matcher(mSnSpectrum.getSpectrumTitle()).replaceAll("") + imageType.getExtension();
                                exportFigure(outputPathName);

                                progressDialog.increasePrimaryProgressCounter();
                            } else {
                                break;
                            }
                        }
                    }
                }catch (Exception e){
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(parentFrame, JOptionEditorPane.getJOptionEditorPane(
                            "Failed to export.<br>"
                                    + "Please <a href=\"https://github.com/wenbostar/PDV\">contact the developers</a>."),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
                progressDialog.setRunFinished();
                formWindowClosing(null);
            }
        }.start();
    }

    /**
     * Update spectrum
     * @param spectrumKey Spectrum key
     * @param mSnSpectrum Msn spectrum
     * @param spectrumIdentificationAssumption Peptide assumption
     */
    private void updateSpectrum(String spectrumKey, MSnSpectrum mSnSpectrum, SpectrumIdentificationAssumption spectrumIdentificationAssumption){
        try {
            if (mSnSpectrum != null) {

                Collection<Peak> peaks = mSnSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {
                    // do nothing, peaks list not found
                } else {

                    boolean newMax = false;

                    double lastMzMaximum = 0;

                    try {
                        lastMzMaximum = mSnSpectrum.getMaxMz()*1.05;
                        newMax = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    double lowerMzZoomRange = 0;
                    double upperMzZoomRange = lastMzMaximum;
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) {
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = mSnSpectrum.getPrecursor();
                    spectrumPanel = new SpectrumPanel(
                            mSnSpectrum.getMzValuesAsArray(), mSnSpectrum.getIntensityValuesNormalizedAsArray(),
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false);

                    spectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    spectrumPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight - 90);
                    spectrumPanel.setKnownMassDeltas(deltaMassMap);
                    spectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

                    specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(spectrumKey, spectrumIdentificationAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);
                    specificAnnotationSettings.setNeutralLossesAuto(false);
                    specificAnnotationSettings.clearNeutralLosses();

                    specificAnnotationSettings.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                    specificAnnotationSettings.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.B_ION);

                    specificAnnotationSettings.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                    specificAnnotationSettings.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

                    specificAnnotationSettings.addIonType(Ion.IonType.PRECURSOR_ION);
                    specificAnnotationSettings.addIonType(Ion.IonType.IMMONIUM_ION);
                    specificAnnotationSettings.addIonType(Ion.IonType.RELATED_ION);

                    String modSequence;
                    ArrayList<IonMatch> annotations;

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        modSequence = tagAssumption.getTag().getTaggedModifiedSequence(ptmSettings, false, false, false, false);

                        annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, mSnSpectrum, tagAssumption.getTag());
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        Peptide currentPeptide = peptideAssumption.getPeptide();
                        modSequence = currentPeptide.getTaggedModifiedSequence(ptmSettings, false, false, false, false);

                        annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, mSnSpectrum, peptideAssumption.getPeptide(), null, true);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);

                    spectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        spectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        spectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    spectrumJPanel.removeAll();
                    spectrumJPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight - 90);
                    spectrumJPanel.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight-90));
                    spectrumJPanel.add(spectrumPanel);
                    spectrumJPanel.revalidate();
                    spectrumJPanel.repaint();

                    secondarySpectrumPlotsJPanel.removeAll();
                    SequenceFragmentationPanel sequenceFragmentationPanel = new SequenceFragmentationPanel(
                            modSequence, annotations, true, ptmSettings, forwardIon, rewindIon);
                    sequenceFragmentationPanel.setMinimumSize(new Dimension(sequenceFragmentationPanel.getPreferredSize().width, sequenceFragmentationPanel.getHeight()));
                    sequenceFragmentationPanel.setOpaque(true);
                    sequenceFragmentationPanel.setBackground(Color.WHITE);

                    secondarySpectrumPlotsJPanel.add(sequenceFragmentationPanel);

                    secondarySpectrumPlotsJPanel.revalidate();
                    secondarySpectrumPlotsJPanel.repaint();

                }
            }
        } catch (Exception e) {
            System.err.println("Update error");
        }
    }

    /**
     * Export single pic
     * @param outputPathName File name
     */
    private void exportFigure(String outputPathName){
        File imageFile = new File(outputPathName);

        try {
            Export.exportPic(spectrumSplitPane, spectrumSplitPane.getBounds(), imageFile, imageType);
        } catch (IOException | TranscoderException e) {
            e.printStackTrace();
        }
    }
}

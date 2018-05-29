package PDVCLI;

import PDVGUI.fileimport.MzXMLScanImport;
import PDVGUI.utils.ImportPTMsFromUnimod;
import com.compomics.util.Export;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.spectrum.GraphicsPanel;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.commons.cli.*;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

import static PDVGUI.gui.PDVMainClass.getJarFilePath;

public class PDVCLIMainClass extends JFrame {

    private JSplitPane spectrumSplitPane;
    private JPanel secondarySpectrumPlotsJPanel;
    private JPanel spectrumOuterJPanel;
    private JPanel spectrumJPanel;
    private JPanel resizeJPanel;

    /**
     * Results file
     */
    private File idFile;
    /**
     * Results file type
     */
    private Integer idFileType;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Spectrum file type
     */
    private Integer spectrumFileType;
    /**
     * Is spectrum key or peptide sequence
     */
    private Boolean isSpectrumKey = true;
    /**
     * Selected items file
     */
    private File indexFile;
    /**
     * Output path
     */
    private String outPutPath;
    /**
     * All spectrum match map
     */
    private HashMap<String, SpectrumMatch> spectrumMatchesMap = new HashMap<>();
    /**
     * Selected items from file
     */
    private ArrayList<String> indexesFromFile = new ArrayList<>();
    /**
     * Modification setting
     */
    private PtmSettings ptmSettings = new PtmSettings();
    /**
     * User preferences
     */
    private UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
    /**
     * Peptide spectrum annotator
     */
    private PeptideSpectrumAnnotator spectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * Error file
     */
    private File errorFile;
    /**
     * Modification factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Spectrum panel
     */
    private SpectrumPanel spectrumPanel;
    /**
     * Scan collection to save spectrum from mzML or mzXML
     */
    private ScanCollectionDefault scans;
    /**
     * The annotation preferences.
     */
    private AnnotationSettings annotationPreferences = new AnnotationSettings();
    /**
     * Object to save spectrum
     */
    private Object spectrumsFileFactory;
    /**
     * Output pic height
     */
    private Integer height;
    /**
     * Output pic width
     */
    private Integer width;
    /**
     * Annotate H2O or not
     */
    private Boolean isH2O = false;
    /**
     * Annotate NH3 or not
     */
    private Boolean isNH3 = false;
    /**
     * Spectrum peak width
     */
    private Float peahWidth;
    /**
     * The pics' size unit
     */
    private String unit;
    /**
     * The pics' format
     */
    private ImageType imageType = ImageType.PNG;
    /**
     * The resize panel width
     */
    private Integer resizeJPanelWidth;
    /**
     * The resize panel length
     */
    private Integer resizeJPanelHeight;
    /**
     * Pattern removing illegal
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * Constructor
     * @param args Input parameters
     * @throws ParseException
     */
    public PDVCLIMainClass(String[] args) throws ParseException, IOException {

        Options options = new Options();

        options.addOption("rt", true, "Identification file format (mzIdentML: 1, pepXML: 2, proBAM: 3, txt: 4, maxQuant: 5).");
        options.addOption("r", true, "Identification file.");
        options.addOption("st", true, "MS/MS data format (mgf: 1, mzML: 2, mzXML: 3).");
        options.addOption("s", true, "MS/MS data file");
        options.addOption("k", true, "The input data type for parameter -i (Spectrum ID: s, peptide sequence: p).");
        options.addOption("i", true, "A file containing peptide sequences or spectrum IDs. PDV will generate figures for these peptides or spectra.");
        options.addOption("o", true, "Output directory.");
        options.addOption("a", true, "Error window for MS/MS fragment ion mass values. Unit is Da. The default value is 0.5.");
        options.addOption("c", true, "The intensity percentile to consider for annotation. Default is 3 (3%), it means that the peaks with intensities >= (3% * max intensity) will be annotated.");
        options.addOption("fh", true, "Figure height. Default is 400");
        options.addOption("fw", true, "Figure width. Default is 800");
        options.addOption("fu", true, "The units in which ‘height’(fh) and ‘width’(fw) are given. Can be cm, mm or px. Default is px");
        options.addOption("ft", true, "Figure type. Can be png, pdf or tiff.");
        options.addOption("pw", true, "Peak width. Default is 1");
        options.addOption("ah", false, "Whether or not to consider neutral loss of H2O.");
        options.addOption("an", false, "Whether or not to consider neutral loss of NH3.");
        options.addOption("h", false, "Help");
        options.addOption("help", false, "Help");

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help") || cmd.hasOption("h") || args.length == 0) {
            HelpFormatter f = new HelpFormatter();
            f.printHelp("Options", options);
            System.exit(0);
        }

        runCMD(cmd);
    }

    /**
     * Import parameters and run CMD
     * @param commandLine Command line
     * @throws IOException
     */
    private void runCMD(CommandLine commandLine) throws IOException {

        this.idFile = new File(commandLine.getOptionValue("r"));
        this.idFileType = Integer.valueOf(commandLine.getOptionValue("rt"));
        this.spectrumFile  = new File(commandLine.getOptionValue("s"));
        this.spectrumFileType = Integer.valueOf(commandLine.getOptionValue("st"));
        this.indexFile = new File(commandLine.getOptionValue("i"));
        this.outPutPath = commandLine.getOptionValue("o");

        if(commandLine.getOptionValue("k").equals("p")){
            this.isSpectrumKey = false;
        }else {
            this.isSpectrumKey = true;
        }

        if (commandLine.getOptionValue("ft").equals("png")){
            this.imageType = ImageType.PNG;
        } else if (commandLine.getOptionValue("ft").equals("pdf")){
            this.imageType = ImageType.PDF;
        } else if (commandLine.getOptionValue("ft").equals("tiff")){
            this.imageType = ImageType.TIFF;
        }

        if (commandLine.getOptionValue("ah") != null){
            this.isH2O = true;
        }
        if (commandLine.getOptionValue("an") != null){
            this.isNH3 = true;
        }

        Double ionAccurracy;
        if (commandLine.getOptionValue("a") == null){
            ionAccurracy = 0.5;
        } else {
            ionAccurracy = Double.valueOf(commandLine.getOptionValue("a"));
        }

        Double intensityFilter = 0.03;
        if (commandLine.getOptionValue("c") == null){
            intensityFilter = 0.03;
        } else {
            intensityFilter = Double.valueOf(commandLine.getOptionValue("c")) * 0.01;
        }
        if (commandLine.getOptionValue("pw") == null){
            this.peahWidth = 1f;
        } else {
            this.peahWidth = Float.valueOf(commandLine.getOptionValue("pw"));
        }
        if (commandLine.getOptionValue("fh") == null){
            this.height = 400;
        } else {
            this.height = Integer.valueOf(commandLine.getOptionValue("fh"));
        }
        if (commandLine.getOptionValue("fw") == null){
            this.width = 800;
        } else {
            this.width = Integer.valueOf(commandLine.getOptionValue("fw"));
        }
        if (commandLine.getOptionValue("fu") == null){
            this.unit = "px";
        } else {
            this.unit = commandLine.getOptionValue("fu");
        }

        this.errorFile = new File(outPutPath+"/error.txt");

        this.annotationPreferences.setFragmentIonAccuracy(ionAccurracy);
        this.annotationPreferences.setIntensityFilter(intensityFilter);
        this.annotationPreferences.setFragmentIonPpm(false);

        initComponent();

        new ImportPTMsFromUnimod(new File(getJarFilePath() + "/resources/conf/unimod.xml"));

        this.setVisible(true);

        processIndexFile();

        importFile();

        displayResults();

    }

    /**
     * Init all GUI components
     */
    private void initComponent(){

        spectrumJPanel = new JPanel();
        spectrumSplitPane = new JSplitPane();
        secondarySpectrumPlotsJPanel = new JPanel();
        spectrumOuterJPanel = new JPanel();
        resizeJPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PsmViewer");
        setBackground(new Color(255, 255, 255));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        spectrumSplitPane.setBackground(new Color(255, 255, 255));
        spectrumSplitPane.setBorder(null);
        spectrumSplitPane.setDividerLocation(100);
        spectrumSplitPane.setDividerSize(0);
        spectrumSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);

        secondarySpectrumPlotsJPanel.setMinimumSize(new Dimension(0, 100));
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
                        .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumOuterJPanelLayout.setVerticalGroup(
                spectrumOuterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumOuterJPanelLayout.createSequentialGroup()
                                .addComponent(spectrumJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
     * Close
     * @param evt WindowEvent
     */
    private void formWindowClosing(WindowEvent evt) {
        final PDVCLIMainClass finalRef = this;
        SwingUtilities.invokeLater(() -> {

            finalRef.setVisible(false);

            System.exit(0);
        });
    }

    /**
     * Get selected items from file
     * @throws IOException
     */
    private void processIndexFile() throws IOException {
        String readLine;
        BufferedReader bufferedReader = new BufferedReader(new FileReader(indexFile));
        while ((readLine = bufferedReader.readLine())!=null) {

            if(isSpectrumKey){
                indexesFromFile.add(readLine);
            }else {
                indexesFromFile.add(readLine.toLowerCase());
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
     * Import results file
     */
    private void importFile() throws IOException {

        if(idFileType == 1){

            processSpectrumFile();

            MzIDImport mzIDImport = new MzIDImport(idFile, spectrumFile.getName(), spectrumsFileFactory);

            spectrumMatchesMap = mzIDImport.getSpectrumMatchesMap();

        } else if(idFileType == 2){

            processSpectrumFile();

            PepXMLImport pepXMLImport = new PepXMLImport(spectrumFile.getName(), idFile);

            spectrumMatchesMap = pepXMLImport.getSpectrumMatchesMap();

        } else if(idFileType == 3){

            processSpectrumFile();

            ProBAMImport proBAMImport = new ProBAMImport(idFile, spectrumFile.getName(), spectrumsFileFactory);

            spectrumMatchesMap = proBAMImport.getSpectrumMatchHashMap();

        } else if(idFileType == 4){

            processSpectrumFile();

            TextImport textImport = new TextImport(idFile, spectrumFile);

            spectrumMatchesMap = textImport.getSpectrumMatchHashMap();

        } else if (idFileType == 5){

            MaxQuantImport maxQUANTImport = new MaxQuantImport(idFile);

            spectrumMatchesMap = maxQUANTImport.getAllSpectrumMatches();

            spectrumsFileFactory = maxQUANTImport.getSpectrumFactory();
        }

    }

    /**
     * Get spectrum from file
     */
    private void processSpectrumFile(){

        if(spectrumFileType == 1){

            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

            try {
                spectrumFactory.addSpectra(spectrumFile);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            spectrumsFileFactory = spectrumFactory;

        } else if(spectrumFileType == 2){
            MZMLFile mzmlFile = new MZMLFile(spectrumFile.getAbsolutePath());

            scans = new ScanCollectionDefault();

            scans.setDefaultStorageStrategy(StorageStrategy.SOFT);

            scans.isAutoloadSpectra(true);

            scans.setDataSource(mzmlFile);

            mzmlFile.setNumThreadsForParsing(null);

            try {
                scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA);
            } catch (FileParsingException e) {
                e.printStackTrace();
            }

            spectrumsFileFactory = scans;

        } else if(spectrumFileType == 3){
            MzXMLScanImport mzXMLScanImport = new MzXMLScanImport(spectrumFile.getAbsolutePath());
            scans = mzXMLScanImport.getScans();

            spectrumsFileFactory = scans;

        }
    }

    /**
     * Get specific spectrum according to the spectrum key
     * @param spectrumKey spectrum key
     * @return
     */
    private MSnSpectrum getSpectrum(String spectrumKey){

        if(spectrumFileType == 1){

            System.out.println("The spectrum key is " + spectrumKey);

            SpectrumFactory spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

            try {
                return (MSnSpectrum) spectrumFactory.getSpectrum(spectrumKey.split("_cus_")[0], spectrumKey.split("_cus_")[1]);
            } catch (Exception e) {
                return null;
            }
        } else if(spectrumFileType == 2){
            System.out.println("The spectrum key is "+spectrumKey);

            IScan iScan = scans.getScanByNum(Integer.parseInt(spectrumKey));
            ISpectrum spectrum = iScan.getSpectrum();

            Charge charge = new Charge(iScan.getPolarity().getSign(), iScan.getPrecursor().getCharge());
            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(charge);
            PrecursorInfo precursor1 = iScan.getPrecursor();
            Double intensity;
            if (precursor1.getIntensity() != null){
                intensity = precursor1.getIntensity();
            } else {
                intensity = 0.0;
            }
            Precursor precursor = new Precursor(scans.getScanByNum(iScan.getPrecursor().getParentScanNum()).getRt(), iScan.getPrecursor().getMzTarget(),
                    intensity, charges);

            double[] mzs = spectrum.getMZs();
            double[] ins = spectrum.getIntensities();
            HashMap<Double, Peak> peakMap = new HashMap<>();
            for(int i = 0; i<mzs.length; i++){
                Peak peak = new Peak(mzs[i], ins[i]);
                peakMap.put(mzs[i], peak);
            }

            return new MSnSpectrum(2, precursor, spectrumKey, peakMap, spectrumFile.getName());

        } else if (spectrumFileType == 3){
            System.out.println("The spectrum key is "+spectrumKey);

            IScan iScan = scans.getScanByNum(Integer.parseInt(spectrumKey));

            ISpectrum spectrum = iScan.getSpectrum();

            Charge charge = new Charge(iScan.getPolarity().getSign(), iScan.getPrecursor().getCharge());
            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(charge);
            Precursor precursor = new Precursor(iScan.getRt(), iScan.getPrecursor().getMzTarget(),
                    iScan.getPrecursor().getIntensity(), charges);

            double[] mzs = spectrum.getMZs();
            double[] ins = spectrum.getIntensities();
            HashMap<Double, Peak> peakMap = new HashMap<>();
            for (int i = 0; i < mzs.length; i++) {
                Peak peak = new Peak(mzs[i], ins[i]);
                peakMap.put(mzs[i], peak);
            }

            MSnSpectrum mSnSpectrum = new MSnSpectrum(2, precursor, spectrumKey, peakMap, spectrumFile.getName());

            return mSnSpectrum;

        } else {
            return null;
        }
    }

    /**
     * Display the results
     */
    private void displayResults() throws IOException {
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));

        FileWriter fileWriter = new FileWriter(errorFile);

        FormLayout formLayout = new FormLayout(width + unit, height + unit);
        ArrayList<String> existItem = new ArrayList<>();

        resizeJPanel.setLayout(formLayout);
        resizeJPanel.revalidate();
        resizeJPanel.repaint();

        resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
        resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));
        if (resizeJPanelWidth < 200) {
            System.err.println("WARNING!\n The width must be bigger than 200 pixels or " + 200 / Toolkit.getDefaultToolkit().getScreenResolution() + " inch.");
        } else {

            spectrumSplitPane.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);
            spectrumSplitPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));
            for (String spectrumIndex : spectrumMatchesMap.keySet()) {
                SpectrumMatch spectrumMatch = spectrumMatchesMap.get(spectrumIndex);

                String spectrumKey = spectrumIndex.split("_rank_")[0];

                if (isSpectrumKey) {
                    if (indexesFromFile.contains(spectrumKey)){
                        existItem.add(spectrumKey);

                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        MSnSpectrum mSnSpectrum;
                        if (spectrumFileType == 1){
                            mSnSpectrum = getSpectrum(spectrumMatch.getKey());
                        } else {
                            mSnSpectrum = getSpectrum(spectrumKey);
                        }

                        if (mSnSpectrum != null) {

                            updateSpectrum(spectrumIndex, mSnSpectrum, peptideAssumption);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String outputFigurePath = outPutPath + "/" + peptideAssumption.getPeptide().getSequence() + FilePattern.matcher(spectrumIndex).replaceAll("").replace("rank_", "") + imageType.getExtension();
                            exportFigure(spectrumSplitPane, outputFigurePath);
                        } else {

                            fileWriter.write(spectrumMatch.getKey() + "\t No spectrum" + "\n");
                        }
                    }

                } else {

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (indexesFromFile.contains(peptideAssumption.getPeptide().getSequence().toLowerCase())) {
                        existItem.add(peptideAssumption.getPeptide().getSequence().toLowerCase());

                        MSnSpectrum mSnSpectrum;
                        if (spectrumFileType == 1){
                            mSnSpectrum = getSpectrum(spectrumMatch.getKey());
                        } else {
                            mSnSpectrum = getSpectrum(spectrumKey);
                        }

                        if (mSnSpectrum != null) {
                            updateSpectrum(spectrumIndex, mSnSpectrum, peptideAssumption);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String outputFigurePath = outPutPath + "\\" + peptideAssumption.getPeptide().getSequence() + FilePattern.matcher(spectrumIndex).replaceAll("").replace("rank_", "") + imageType.getExtension();
                            exportFigure(spectrumSplitPane, outputFigurePath);
                        } else {

                            fileWriter.write(spectrumMatch.getKey() + "\t No spectrum" + "\n");
                        }

                    }
                }
            }
        }

        for (String errorItem : indexesFromFile){
            if (!existItem.contains(errorItem)){
                fileWriter.write(errorItem + "\t No result" + "\n");
            }
        }

        fileWriter.close();
        formWindowClosing(null);
    }

    /**
     * Update spectrum panel according to the details
     * @param spectrumKey spectrum key
     * @param mSnSpectrum MSN spectrum
     * @param tempPeptideAssumption peptide assumption
     */
    private void updateSpectrum(String spectrumKey, MSnSpectrum mSnSpectrum, PeptideAssumption tempPeptideAssumption){

        try {
            if (mSnSpectrum != null) {

                Collection<Peak> peaks = mSnSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {

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
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) { // @TODO: sometimes the range is reset when is should not be...
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = mSnSpectrum.getPrecursor();
                    spectrumPanel = new SpectrumPanel(
                            mSnSpectrum.getMzValuesAsArray(), mSnSpectrum.getIntensityValuesNormalizedAsArray(),
                            precursor.getMz(), tempPeptideAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false);
                    spectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    spectrumPanel.setDeltaMassWindow(annotationPreferences.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(peahWidth);
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

                    Peptide currentPeptide = tempPeptideAssumption.getPeptide();

                    SpecificAnnotationSettings specificAnnotationPreferences = annotationPreferences.getSpecificAnnotationPreferences(spectrumKey, tempPeptideAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);

                    specificAnnotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
                    specificAnnotationPreferences.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.B_ION);

                    specificAnnotationPreferences.addIonType(Ion.IonType.PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
                    specificAnnotationPreferences.addIonType(Ion.IonType.TAG_FRAGMENT_ION, PeptideFragmentIon.Y_ION);

                    specificAnnotationPreferences.clearNeutralLosses();

                    if (isH2O){
                        specificAnnotationPreferences.addNeutralLoss(NeutralLoss.H2O);
                    }
                    if (isNH3){
                        specificAnnotationPreferences.addNeutralLoss(NeutralLoss.NH3);
                    }

                    ArrayList<IonMatch> annotations = spectrumAnnotator.getSpectrumAnnotationFiter(annotationPreferences, specificAnnotationPreferences, mSnSpectrum, currentPeptide, null, true);//在此计算匹配

                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationPreferences.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);

                    spectrumPanel.showAnnotatedPeaksOnly(!annotationPreferences.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    spectrumJPanel.removeAll();
                    spectrumJPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight - 80);
                    spectrumJPanel.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight-80));
                    spectrumJPanel.add(spectrumPanel);
                    spectrumJPanel.revalidate();
                    spectrumJPanel.repaint();

                    Integer forwardIon = PeptideFragmentIon.B_ION;

                    Integer rewindIon = PeptideFragmentIon.Y_ION;
                    SequenceFragmentationPanel sequenceFragmentationPanel = new SequenceFragmentationPanel(
                            currentPeptide.getTaggedModifiedSequence(ptmSettings, false, false, false, false),
                            annotations, true, ptmSettings, forwardIon, rewindIon);
                    sequenceFragmentationPanel.setOpaque(false);
                    sequenceFragmentationPanel.setBackground(Color.WHITE);
                    sequenceFragmentationPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    secondarySpectrumPlotsJPanel.removeAll();
                    secondarySpectrumPlotsJPanel.add(sequenceFragmentationPanel);
                    secondarySpectrumPlotsJPanel.revalidate();
                    secondarySpectrumPlotsJPanel.repaint();

                    spectrumSplitPane.revalidate();
                    spectrumSplitPane.repaint();

                    resizeJPanel.revalidate();
                    resizeJPanel.repaint();

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Update error");
        }
    }

    /**
     * Export spectra
     * @param graphicsPanel Spectrum and fragment panel
     * @param finalSelectedFile pic file path
     */
    private void exportFigure(Component graphicsPanel, String finalSelectedFile){

        try {
            Export.exportComponent(graphicsPanel, graphicsPanel.getBounds(), new File(finalSelectedFile), imageType);
        } catch (IOException | TranscoderException e) {
            e.printStackTrace();
        }

    }

    /**
     * Get the current delta masses map
     * @return Current delta masses map
     */
    private HashMap<Double, String> getCurrentMassDeltas() {
        HashMap<Double, String> currentMassDeltaMap = new HashMap<>();

        ArrayList<String> allModifications = ptmSettings.getAllModifications();
        Collections.sort(allModifications);

        currentMassDeltaMap.put(AminoAcid.A.getMonoisotopicMass(), "A");
        currentMassDeltaMap.put(AminoAcid.C.getMonoisotopicMass(), "C");
        currentMassDeltaMap.put(AminoAcid.D.getMonoisotopicMass(), "D");
        currentMassDeltaMap.put(AminoAcid.E.getMonoisotopicMass(), "E");
        currentMassDeltaMap.put(AminoAcid.F.getMonoisotopicMass(), "F");
        currentMassDeltaMap.put(AminoAcid.G.getMonoisotopicMass(), "G");
        currentMassDeltaMap.put(AminoAcid.H.getMonoisotopicMass(), "H");
        currentMassDeltaMap.put(AminoAcid.I.getMonoisotopicMass(), "I/L");
        currentMassDeltaMap.put(AminoAcid.K.getMonoisotopicMass(), "K");
        currentMassDeltaMap.put(AminoAcid.M.getMonoisotopicMass(), "M");
        currentMassDeltaMap.put(AminoAcid.N.getMonoisotopicMass(), "N");
        currentMassDeltaMap.put(AminoAcid.O.getMonoisotopicMass(), "O");
        currentMassDeltaMap.put(AminoAcid.P.getMonoisotopicMass(), "P");
        currentMassDeltaMap.put(AminoAcid.Q.getMonoisotopicMass(), "Q");
        currentMassDeltaMap.put(AminoAcid.R.getMonoisotopicMass(), "R");
        currentMassDeltaMap.put(AminoAcid.S.getMonoisotopicMass(), "S");
        currentMassDeltaMap.put(AminoAcid.T.getMonoisotopicMass(), "T");
        currentMassDeltaMap.put(AminoAcid.U.getMonoisotopicMass(), "U");
        currentMassDeltaMap.put(AminoAcid.V.getMonoisotopicMass(), "V");
        currentMassDeltaMap.put(AminoAcid.W.getMonoisotopicMass(), "W");
        currentMassDeltaMap.put(AminoAcid.Y.getMonoisotopicMass(), "Y");

        for (String modification : allModifications) {
            PTM ptm = ptmFactory.getPTM(modification);

            if (ptm != null) {

                String shortName = ptm.getShortName();
                AminoAcidPattern aminoAcidPattern = ptm.getPattern();
                double mass = ptm.getMass();

                // Get non-terminus modification
                if (ptm.getType() == PTM.MODAA && aminoAcidPattern != null) {
                    for (Character character : aminoAcidPattern.getAminoAcidsAtTarget()) {
                        if (!currentMassDeltaMap.containsValue(character + "<" + shortName + ">")) {
                            AminoAcid aminoAcid = AminoAcid.getAminoAcid(character);
                            currentMassDeltaMap.put(mass + aminoAcid.getMonoisotopicMass(),
                                    character + "<" + shortName + ">");
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, modification+" PTM not found", "PTM Error", JOptionPane.ERROR_MESSAGE);
                System.err.println("Error: PTM not found: " + modification);
            }
        }

        return currentMassDeltaMap;
    }

}

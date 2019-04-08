package PDVGUI.gui.utils;

import PDVGUI.gui.*;
import PDVGUI.gui.utils.Export.ExportExpectedSizeDialog;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.TagFragmentIon;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
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
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.spectrum.FragmentIonTable;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

import static com.compomics.util.experiment.biology.Ion.IonType.*;
import static com.compomics.util.experiment.biology.Ion.IonType.RELATED_ION;
import static com.compomics.util.experiment.biology.Ion.IonType.REPORTER_ION;
import static com.compomics.util.experiment.biology.NeutralLoss.H3PO4;

/**
 * Spectrum main panel
 * Created by Ken on 9/15/2017.
 */
public class SpectrumMainPanel extends JPanel {

    public JPanel spectrumShowPanel;
    private JPanel mainShowJPanel;
    private JPanel fragmentIonJPanel;
    private JScrollPane ionFragmentsJScrollPane;
    private JLayeredPane spectrumJLayeredPane;
    private JLayeredPane mirrorJLayeredPane;
    private JLayeredPane checkPeptideJLayeredPane;
    private JCheckBoxMenuItem defaultAnnotationCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardAIonCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardBIonCheckBoxMenuItem;
    private JCheckBoxMenuItem forwardCIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardXIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardYIonCheckBoxMenuItem;
    private JCheckBoxMenuItem rewardZIonCheckBoxMenuItem;
    private JCheckBoxMenuItem precursorCheckMenuItem;
    private JCheckBoxMenuItem immoniumIonsCheckMenuItem;
    private JCheckBoxMenuItem relatedIonsCheckMenuItem;
    private JCheckBoxMenuItem reporterIonsCheckMenuItem;
    private JCheckBoxMenuItem defaultLossCheckBoxMenuItem;
    private JCheckBoxMenuItem showAllPeaksMenuItem;
    private JCheckBoxMenuItem forwardIonsDeNovoCheckBoxMenuItem;
    private JCheckBoxMenuItem rewindIonsDeNovoCheckBoxMenuItem;
    private JRadioButtonMenuItem deNovoChargeOneJRadioButtonMenuItem;
    private JMenu lossMenu;
    private JMenu chargeMenu;
    private JMenu ionsMenu;
    private JMenu otherMenu;
    private JMenu deNovoMenu;
    private JMenu exportGraphicsMenu;
    private JMenu switchPaneMenu;
    private JMenu checkFileMenu;
    private JMenu lossSplitter;
    private JMenu splitterMenu6;
    private JMenu splitterMenu7;
    private JLabel contentJLabel;
    private JLabel bIonNumJLabel;
    private JLabel yIonNumJLabel;
    private JLabel matchNumJLabel;
    public JMenu settingsMenu;
    public JMenu peptideCheckMenu;

    /**
     * Mirror spectrum panel
     */
    private SpectrumContainer mirrorSpectrumPanel;
    /**
     * Check peptide spectrum panel
     */
    private SpectrumContainer checkPeptideSpectrumPanel;
    /**
     * Original sequence fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanel;
    /**
     * Mirror sequence fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanelMirror;
    /**
     * Check peptide fragment panel
     */
    private SequenceFragmentationPanel sequenceFragmentationPanelCheck;
    /**
     * Mirror fragment panel
     */
    private SequenceFragmentationPanel mirrorFragmentPanel;
    /**
     * Check fragment panel
     */
    private SequenceFragmentationPanel checkFragmentPanel;
    /**
     * Boolean show if spectrum check selected
     */
    private boolean mirrorSelected = false;
    /**
     * Boolean show if peptide check selected
     */
    private boolean peptideCheckSelected = false;
    /**
     * LastSelectFolder accessed easily
     */
    public LastSelectedFolder lastSelectedFolder;
    /**
     * Utilities user preferences
     */
    public UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    /**
     * SpectrumPanel to paint spectrum import from utilities
     */
    private SpectrumContainer spectrumPanel;
    /**
     * Original peptide sequence
     */
    private String currentPeptideSequence;
    /**
     * SpecificAnnotationSettings
     */
    private SpecificAnnotationSettings specificAnnotationSettings;
    /**
     * Current psmKey selected
     */
    private String selectedPsmKey = "";
    /**
     * Synthetic peptide spectra file map
     */
    private HashMap<String, File> checkSpectrumFileMaps = new HashMap<>();
    /**
     * Current assumptions
     */
    private SpectrumIdentificationAssumption spectrumIdentificationAssumption;

    private MSnSpectrum currentSpectrum;
    /**
     * Maximum mz to rescale spectrumPanel
     */
    private double lastMzMaximum = 0;
    /**
     * Forward ions searched for (a, b or c)
     */
    private Integer forwardIon = PeptideFragmentIon.B_ION;
    /**
     * Reward ions searched for (x, y or z)
     */
    private Integer rewindIon = PeptideFragmentIon.Y_ION;
    /**
     * PTMFactory containing all modifications import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Spectrum annotator
     */
    private PeptideSpectrumAnnotator peptideSpectrumAnnotator = new PeptideSpectrumAnnotator();
    /**
     * Spectrum tagSpectrumAnnotator
     */
    private TagSpectrumAnnotator tagSpectrumAnnotator = new TagSpectrumAnnotator();
    /**
     * SearchParameters
     */
    private SearchParameters searchParameters;
    /**
     * ChargeMenus according to the precursor charge state
     */
    private HashMap<Integer, JCheckBoxMenuItem> chargeMenuMap = new HashMap<>();
    /**
     * lossMenus
     */
    private HashMap<NeutralLoss, JCheckBoxMenuItem> lossMenuMap = new HashMap<>();
    /**
     * Check peptide map
     */
    public HashMap<String, Peptide> checkPeptideMap = new HashMap<>();
    /**
     * Parent frame
     */
    public JFrame parentFrame;
    /**
     * Show spectrum details or not
     */
    public Boolean showDetails = false;
    /**
     * Intensity details
     */
    public String matchedToAllPeakInt;
    /**
     * Spectrum setAction
     */
    private SetAction spectrumSetAction;
    /**
     * Check spectrum mirror spectrum setAction
     */
    private SetAction mirrorSetAction;
    /**
     * Check peptide mirror spectrum setAction
     */
    private SetAction checkSetAction;
    /**
     * If true, de novo function
     */
    private boolean isDenovo;

    /**
     * Constructor
     * @param prideXMLDisplay Parent class
     */
    public SpectrumMainPanel(PrideXMLDisplay prideXMLDisplay){

        this.searchParameters = prideXMLDisplay.searchParameters;

        this.parentFrame = prideXMLDisplay;

        annotationSettings.setFragmentIonAccuracy(0.5);

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        loadUserPreferences();

        initComponents();

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
    }

    /**
     * Constructor
     * @param spectrumLibDisplay Parent class
     */
    public SpectrumMainPanel(SpectrumLibDisplay spectrumLibDisplay){

        this.searchParameters = spectrumLibDisplay.searchParameters;

        this.parentFrame = spectrumLibDisplay;

        annotationSettings.setFragmentIonAccuracy(0.5);

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        loadUserPreferences();

        initComponents();

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
    }

    /**
     * Construtor
     * @param singlePeptideDisplay Parent class
     */
    public SpectrumMainPanel(SinglePeptideDisplay singlePeptideDisplay){
        this.searchParameters = singlePeptideDisplay.searchParameters;

        this.parentFrame = singlePeptideDisplay;

        annotationSettings.setFragmentIonAccuracy(0.5);

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        loadUserPreferences();

        initComponents();

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
    }

    /**
     * Construtor
     * @param pdvMainClass Parent class
     */
    public SpectrumMainPanel(PDVMainClass pdvMainClass){
        this.searchParameters = pdvMainClass.searchParameters;

        this.parentFrame = pdvMainClass;

        annotationSettings.setFragmentIonAccuracy(0.5);

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));
        NeutralLoss[] h3po4 = new NeutralLoss[1];
        h3po4[0] = H3PO4;
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 1, h3po4), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(PEPTIDE_FRAGMENT_ION, 4, h3po4), new Color(255, 102, 0));

        loadUserPreferences();

        initComponents();

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);

        annotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
        annotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
    }

    /**
     * Show details or not
     * @param showDetails Boolean
     */
    public void setShowDetails(Boolean showDetails){
        this.showDetails = showDetails;
    }

    /**
     * Set tolerance
     * @param massAccuracyType Type
     * @param fragmentIonAccuracy Value
     */
    public void setFragmentIonAccuracy(SearchParameters.MassAccuracyType massAccuracyType, Double fragmentIonAccuracy){
        searchParameters.setFragmentAccuracyType(massAccuracyType);
        searchParameters.setFragmentIonAccuracy(fragmentIonAccuracy);
        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        updateSpectrum();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){

        JToolBar allJToolBar = new JToolBar();
        JMenuBar annotationMenuBar = new JMenuBar();
        JPanel allAnnotationMenuPanel = new JPanel();
        ButtonGroup deNovoChargeButtonGroup = new ButtonGroup();
        JRadioButtonMenuItem deNovoChargeTwoJRadioButtonMenuItem = new JRadioButtonMenuItem();
        JMenuItem annotationSettingsJMenuItem = new JMenuItem();
        JMenuItem exportSpectrumGraphicsJMenuItem = new JMenuItem();
        JMenuItem showSpectrumJMenuItem = new JMenuItem();
        JMenuItem showIonTableJMenuItem = new JMenuItem();
        JMenuItem showMirrorJMenuItem = new JMenuItem();
        JMenuItem checkFileMenuItem = new JMenuItem();
        JMenuItem checkPeptideMenuItem = new JMenuItem();
        JMenuItem showCheckPeptideJMenuItem = new JMenuItem();
        JMenu exportSpectrumMenu = new JMenu();
        JMenu splitterMenu1 = new JMenu();
        JMenu splitterMenu2 = new JMenu();
        JMenu splitterMenu3 = new JMenu();
        JMenu splitterMenu4 = new JMenu();
        JMenu splitterMenu5 = new JMenu();
        JMenu splitterMenu8 = new JMenu();
        JMenu splitterMenu9 = new JMenu();
        JPopupMenu.Separator jSeparator1 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator2 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator3 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator4 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator5 = new JPopupMenu.Separator();
        JLabel matchedToAllJLabel = new JLabel();
        JLabel matchIonNumJLabel = new JLabel();
        JLabel matchBIonJLabel = new JLabel();
        JLabel matchYIonJLabel = new JLabel();

        Font menuFont = new Font("Arial", Font.PLAIN, 12);

        spectrumShowPanel = new JPanel();
        mainShowJPanel = new JPanel();
        fragmentIonJPanel = new JPanel();
        ionFragmentsJScrollPane = new JScrollPane();
        ionsMenu = new JMenu();
        chargeMenu = new JMenu();
        lossMenu = new JMenu();
        otherMenu = new JMenu();
        settingsMenu = new JMenu();
        exportGraphicsMenu = new JMenu();
        switchPaneMenu = new JMenu();
        checkFileMenu = new JMenu();
        deNovoMenu = new JMenu();
        peptideCheckMenu = new JMenu();
        lossSplitter = new JMenu();
        splitterMenu6 = new JMenu();
        splitterMenu7 = new JMenu();
        defaultAnnotationCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardAIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardBIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        forwardCIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardXIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardYIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewardZIonCheckBoxMenuItem = new JCheckBoxMenuItem();
        precursorCheckMenuItem = new JCheckBoxMenuItem();
        immoniumIonsCheckMenuItem = new JCheckBoxMenuItem();
        relatedIonsCheckMenuItem = new JCheckBoxMenuItem();
        reporterIonsCheckMenuItem = new JCheckBoxMenuItem();
        defaultLossCheckBoxMenuItem = new JCheckBoxMenuItem();
        showAllPeaksMenuItem = new JCheckBoxMenuItem();
        forwardIonsDeNovoCheckBoxMenuItem = new JCheckBoxMenuItem();
        rewindIonsDeNovoCheckBoxMenuItem = new JCheckBoxMenuItem();
        deNovoChargeOneJRadioButtonMenuItem = new JRadioButtonMenuItem();
        spectrumJLayeredPane = new JLayeredPane();
        mirrorJLayeredPane= new JLayeredPane();
        checkPeptideJLayeredPane = new JLayeredPane();
        contentJLabel = new JLabel();
        bIonNumJLabel = new JLabel();
        yIonNumJLabel = new JLabel();
        matchNumJLabel = new JLabel();
        spectrumShowPanel.setOpaque(false);

        annotationMenuBar.setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());
        annotationMenuBar.setOpaque(false);

        splitterMenu1.setText("|");
        splitterMenu1.setEnabled(false);
        annotationMenuBar.add(splitterMenu1);

        ionsMenu.setText("Ions");
        ionsMenu.setFont(menuFont);
        ionsMenu.setEnabled(false);

        forwardAIonCheckBoxMenuItem.setText("a");
        forwardAIonCheckBoxMenuItem.setFont(menuFont);
        forwardAIonCheckBoxMenuItem.addActionListener(this::forwardAIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardAIonCheckBoxMenuItem);

        forwardBIonCheckBoxMenuItem.setText("b");
        forwardBIonCheckBoxMenuItem.setFont(menuFont);
        forwardBIonCheckBoxMenuItem.addActionListener(this::forwardBIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardBIonCheckBoxMenuItem);

        forwardCIonCheckBoxMenuItem.setText("c");
        forwardCIonCheckBoxMenuItem.setFont(menuFont);
        forwardCIonCheckBoxMenuItem.addActionListener(this::forwardCIonCheckBoxMenuItemAction);
        ionsMenu.add(forwardCIonCheckBoxMenuItem);
        ionsMenu.add(jSeparator1);

        rewardXIonCheckBoxMenuItem.setText("x");
        rewardXIonCheckBoxMenuItem.setFont(menuFont);
        rewardXIonCheckBoxMenuItem.addActionListener(this::rewardXIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardXIonCheckBoxMenuItem);

        rewardYIonCheckBoxMenuItem.setText("y");
        rewardYIonCheckBoxMenuItem.setFont(menuFont);
        rewardYIonCheckBoxMenuItem.addActionListener(this::rewardYIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardYIonCheckBoxMenuItem);

        rewardZIonCheckBoxMenuItem.setText("z");
        rewardZIonCheckBoxMenuItem.setFont(menuFont);
        rewardZIonCheckBoxMenuItem.addActionListener(this::rewardZIonCheckBoxMenuItemAction);
        ionsMenu.add(rewardZIonCheckBoxMenuItem);

        annotationMenuBar.add(ionsMenu);

        splitterMenu2.setText("|");
        splitterMenu2.setEnabled(false);
        annotationMenuBar.add(splitterMenu2);

        otherMenu.setText("Other");
        otherMenu.setFont(menuFont);
        otherMenu.setEnabled(false);

        precursorCheckMenuItem.setSelected(true);
        precursorCheckMenuItem.setText("Precursor");
        precursorCheckMenuItem.setFont(menuFont);
        precursorCheckMenuItem.addActionListener(this::precursorCheckMenuItemAction);
        otherMenu.add(precursorCheckMenuItem);

        immoniumIonsCheckMenuItem.setSelected(true);
        immoniumIonsCheckMenuItem.setText("Immonium");
        immoniumIonsCheckMenuItem.setFont(menuFont);
        immoniumIonsCheckMenuItem.addActionListener(this::immoniumIonsCheckMenuItemAction);
        otherMenu.add(immoniumIonsCheckMenuItem);

        relatedIonsCheckMenuItem.setSelected(true);
        relatedIonsCheckMenuItem.setText("Related");
        relatedIonsCheckMenuItem.setFont(menuFont);
        relatedIonsCheckMenuItem.addActionListener(this::relatedIonsCheckMenuItemAction);
        otherMenu.add(relatedIonsCheckMenuItem);

        reporterIonsCheckMenuItem.setSelected(true);
        reporterIonsCheckMenuItem.setText("Reporter");
        reporterIonsCheckMenuItem.setFont(menuFont);
        reporterIonsCheckMenuItem.addActionListener(this::reporterIonsCheckMenuItemAction);
        otherMenu.add(reporterIonsCheckMenuItem);

        annotationMenuBar.add(otherMenu);

        lossSplitter.setText("|");
        lossSplitter.setEnabled(false);
        annotationMenuBar.add(lossSplitter);

        lossMenu.setText("Loss");
        lossMenu.setFont(menuFont);
        lossMenu.setEnabled(false);
        lossMenu.add(jSeparator2);

        defaultLossCheckBoxMenuItem.setText("Default");
        defaultLossCheckBoxMenuItem.setFont(menuFont);
        defaultLossCheckBoxMenuItem.setToolTipText("Adapt losses to sequence and modifications");
        defaultLossCheckBoxMenuItem.addActionListener(this::defaultLossCheckBoxMenuItemAction);
        lossMenu.add(defaultLossCheckBoxMenuItem);

        annotationMenuBar.add(lossMenu);

        splitterMenu9.setText("|");
        splitterMenu9.setEnabled(false);
        annotationMenuBar.add(splitterMenu9);

        chargeMenu.setText("Charge");
        chargeMenu.setFont(menuFont);
        chargeMenu.setEnabled(false);
        annotationMenuBar.add(chargeMenu);

        splitterMenu3.setText("|");
        splitterMenu3.setEnabled(false);
        annotationMenuBar.add(splitterMenu3);

        deNovoMenu.setText("De Novo");
        deNovoMenu.setFont(menuFont);

        forwardIonsDeNovoCheckBoxMenuItem.setText("b-ions");
        forwardIonsDeNovoCheckBoxMenuItem.setFont(menuFont);
        forwardIonsDeNovoCheckBoxMenuItem.addActionListener(this::forwardIonsDeNovoCheckBoxMenuItemAction);
        deNovoMenu.add(forwardIonsDeNovoCheckBoxMenuItem);

        rewindIonsDeNovoCheckBoxMenuItem.setText("y-ions");
        rewindIonsDeNovoCheckBoxMenuItem.setFont(menuFont);
        rewindIonsDeNovoCheckBoxMenuItem.addActionListener(this::rewindIonsDeNovoCheckBoxMenuItemAction);
        deNovoMenu.add(rewindIonsDeNovoCheckBoxMenuItem);
        deNovoMenu.add(jSeparator3);

        deNovoChargeButtonGroup.add(deNovoChargeOneJRadioButtonMenuItem);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(true);
        deNovoChargeOneJRadioButtonMenuItem.setText("Single Charge");
        deNovoChargeOneJRadioButtonMenuItem.setFont(menuFont);
        deNovoChargeOneJRadioButtonMenuItem.addActionListener(this::deNovoChargeOneJRadioButtonMenuItemAction);
        deNovoMenu.add(deNovoChargeOneJRadioButtonMenuItem);

        deNovoChargeButtonGroup.add(deNovoChargeTwoJRadioButtonMenuItem);
        deNovoChargeTwoJRadioButtonMenuItem.setText("Double Charge");
        deNovoChargeTwoJRadioButtonMenuItem.setFont(menuFont);
        deNovoChargeTwoJRadioButtonMenuItem.addActionListener(this::deNovoChargeTwoJRadioButtonMenuItemAction);
        deNovoMenu.add(deNovoChargeTwoJRadioButtonMenuItem);

        annotationMenuBar.add(deNovoMenu);

        splitterMenu4.setText("|");
        splitterMenu4.setEnabled(false);
        annotationMenuBar.add(splitterMenu4);

        settingsMenu.setText("Settings");
        settingsMenu.setFont(menuFont);
        settingsMenu.setEnabled(false);

        showAllPeaksMenuItem.setText("Show All Peaks");
        showAllPeaksMenuItem.setFont(menuFont);
        showAllPeaksMenuItem.setToolTipText("Show all peaks or just the annotated peaks");
        showAllPeaksMenuItem.addActionListener(this::showAllPeaksMenuItemAction);
        settingsMenu.add(showAllPeaksMenuItem);

        settingsMenu.add(jSeparator4);

        defaultAnnotationCheckBoxMenuItem.setSelected(true);
        defaultAnnotationCheckBoxMenuItem.setText("Automatic Annotation");
        defaultAnnotationCheckBoxMenuItem.setFont(menuFont);
        defaultAnnotationCheckBoxMenuItem.setToolTipText("Use automatic annotation");
        defaultAnnotationCheckBoxMenuItem.addActionListener(this::defaultAnnotationCheckBoxMenuItemAction);
        settingsMenu.add(defaultAnnotationCheckBoxMenuItem);

        settingsMenu.add(jSeparator5);

        annotationSettingsJMenuItem.setText("Annotation Setting");
        annotationSettingsJMenuItem.setFont(menuFont);
        annotationSettingsJMenuItem.addActionListener(this::annotationSettingsJMenuItemActionPerformed);
        settingsMenu.add(annotationSettingsJMenuItem);

        annotationMenuBar.add(settingsMenu);

        splitterMenu5.setText("|");
        splitterMenu5.setEnabled(false);
        annotationMenuBar.add(splitterMenu5);

        exportGraphicsMenu.setText("Export");
        exportGraphicsMenu.setFont(menuFont);
        exportGraphicsMenu.setEnabled(false);

        exportSpectrumMenu.setText("Figure");
        exportSpectrumMenu.setFont(menuFont);

        exportSpectrumGraphicsJMenuItem.setText("Spectra");
        exportSpectrumGraphicsJMenuItem.setFont(menuFont);
        exportSpectrumGraphicsJMenuItem.addActionListener(this::exportSpectrumGraphicsJMenuItemActionPerformed);
        exportSpectrumMenu.add(exportSpectrumGraphicsJMenuItem);

        exportGraphicsMenu.add(exportSpectrumGraphicsJMenuItem);

        annotationMenuBar.add(exportGraphicsMenu);

        splitterMenu6.setText("|");
        splitterMenu6.setEnabled(false);
        annotationMenuBar.add(splitterMenu6);

        switchPaneMenu.setText("Tools");
        switchPaneMenu.setFont(menuFont);
        switchPaneMenu.setEnabled(false);

        showSpectrumJMenuItem.setText("Show spectrum");
        showSpectrumJMenuItem.setFont(menuFont);
        showSpectrumJMenuItem.addActionListener(this::showSpectrumJMenuItemAction);

        switchPaneMenu.add(showSpectrumJMenuItem);

        showIonTableJMenuItem.setText("Ion Table");
        showIonTableJMenuItem.setFont(menuFont);
        showIonTableJMenuItem.addActionListener(this::showIonTableJMenuItemAction);

        switchPaneMenu.add(showIonTableJMenuItem);

        showMirrorJMenuItem.setText("Check spectrum");
        showMirrorJMenuItem.setFont(menuFont);
        showMirrorJMenuItem.addActionListener(this::showMirrorJMenuItemActionPerformed);

        switchPaneMenu.add(showMirrorJMenuItem);

        showCheckPeptideJMenuItem.setText("Check Peptide");
        showCheckPeptideJMenuItem.setFont(menuFont);
        showCheckPeptideJMenuItem.addActionListener(this::showCheckPeptideJMenuItemActionPerformed);

        switchPaneMenu.add(showCheckPeptideJMenuItem);

        annotationMenuBar.add(switchPaneMenu);

        splitterMenu7.setText("|");
        splitterMenu7.setEnabled(false);
        splitterMenu7.setVisible(false);
        annotationMenuBar.add(splitterMenu7);

        checkFileMenu.setText("Add");
        checkFileMenu.setFont(menuFont);
        checkFileMenu.setVisible(false);

        checkFileMenuItem.setText("Add Spectrum");
        checkFileMenuItem.setFont(menuFont);

        checkFileMenuItem.addActionListener(this::checkFileMenuItemPerform);

        checkFileMenu.add(checkFileMenuItem);

        annotationMenuBar.add(checkFileMenu);

        peptideCheckMenu.setText("Add");
        peptideCheckMenu.setFont(menuFont);
        peptideCheckMenu.setVisible(false);

        checkPeptideMenuItem.setText("Add Peptide");
        checkPeptideMenuItem.setFont(menuFont);
        checkPeptideMenuItem.addActionListener(this::checkPeptideMenuItemPerform);

        peptideCheckMenu.add(checkPeptideMenuItem);

        annotationMenuBar.add(peptideCheckMenu);

        splitterMenu8.setText("|");
        splitterMenu8.setEnabled(false);
        annotationMenuBar.add(splitterMenu8);

        allJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        allJToolBar.setBorder(null);
        allJToolBar.setFloatable(false);
        allJToolBar.setRollover(true);
        allJToolBar.setBorderPainted(false);

        allAnnotationMenuPanel.add(annotationMenuBar);

        allAnnotationMenuPanel.setLayout(new javax.swing.BoxLayout(allAnnotationMenuPanel, javax.swing.BoxLayout.LINE_AXIS));
        allJToolBar.add(allAnnotationMenuPanel);

        ionFragmentsJScrollPane.setOpaque(false);

        javax.swing.GroupLayout fragmentIonJPanelLayout = new javax.swing.GroupLayout(fragmentIonJPanel);
        fragmentIonJPanel.setLayout(fragmentIonJPanelLayout);
        fragmentIonJPanelLayout.setHorizontalGroup(
                fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(fragmentIonJPanelLayout.createSequentialGroup()
                                .addGroup(fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(ionFragmentsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 1240, 3000)
                                )
                                .addContainerGap())
        );
        fragmentIonJPanelLayout.setVerticalGroup(
                fragmentIonJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup( fragmentIonJPanelLayout.createSequentialGroup()
                                .addComponent(ionFragmentsJScrollPane, 100, 240, 1300)
                        )
        );

        mainShowJPanel.setOpaque(false);
        mainShowJPanel.setBackground(Color.WHITE);

        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );

        matchedToAllJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchBIonJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchYIonJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        matchIonNumJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        matchedToAllJLabel.setText("Matched Peaks Intensity / Total Intensity: ");
        matchBIonJLabel.setText("b ions: ");
        matchBIonJLabel.setToolTipText("The number of all b ions.");
        matchYIonJLabel.setText("y ions: ");
        matchYIonJLabel.setToolTipText("The number of all y ions.");
        matchIonNumJLabel.setText("by pairs: ");
        matchIonNumJLabel.setToolTipText("The number of all by pairs in same charge state.");

        spectrumShowPanel.setOpaque(false);
        spectrumShowPanel.setBackground(Color.white);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowPanel);
        spectrumShowPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(allJToolBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchedToAllJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(contentJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchBIonJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(bIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchYIonJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(yIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGap(5,5,10)
                                .addComponent(matchIonNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(matchNumJLabel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                        .addComponent(mainShowJPanel,100, 1240, Short.MAX_VALUE)
        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(mainShowJPanel, 100, 250, Short.MAX_VALUE)
                                .addGroup(spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(allJToolBar, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchedToAllJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(contentJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchBIonJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(bIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchYIonJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(yIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchIonNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(matchNumJLabel, 25, 25, GroupLayout.PREFERRED_SIZE)))
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(spectrumShowPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(spectrumShowPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
    }

    /**
     * No select of default annotation menu item
     */
    private void noSelectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(false);
    }

    /**
     * ForwardAIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardAIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardBIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardBIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardCIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardCIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardXIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardXIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardYIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardYIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * RewardZIonCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewardZIonCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * precursorCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void precursorCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * immoniumIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void immoniumIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * reporterIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void reporterIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * relatedIonsCheckMenuItemAction
     * @param evt Mouse click event
     */
    private void relatedIonsCheckMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * defaultLossCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void defaultLossCheckBoxMenuItemAction(ActionEvent evt) {
        noSelectDefaultAnnotationMenuItem();
        updateSpectrum();
    }

    /**
     * ForwardIonsDeNovoCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void forwardIonsDeNovoCheckBoxMenuItemAction(ActionEvent evt) {
        if (forwardIonsDeNovoCheckBoxMenuItem.isSelected()){
            isDenovo = true;
        } else {
            isDenovo = false;
        }
        updateSpectrum();
    }

    /**
     * RewindIonsDeNovoCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void rewindIonsDeNovoCheckBoxMenuItemAction(ActionEvent evt) {
        if (rewindIonsDeNovoCheckBoxMenuItem.isSelected()){
            isDenovo = true;
        } else {
            isDenovo = false;
        }
        updateSpectrum();
    }

    /**
     * DeNovoChargeOneJRadioButtonMenuItemAction
     * @param evt Mouse click event
     */
    private void deNovoChargeOneJRadioButtonMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }

    /**
     * DeNovoChargeTwoJRadioButtonMenuItemAction
     * @param evt Mouse click event
     */
    private void deNovoChargeTwoJRadioButtonMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }


    /**
     * showAllPeaksMenuItemAction
     * @param evt Mouse click event
     */
    private void showAllPeaksMenuItemAction(ActionEvent evt) {
        annotationSettings.setShowAllPeaks(showAllPeaksMenuItem.isSelected());
        updateSpectrum();
    }

    /**
     * DefaultAnnotationCheckBoxMenuItemAction
     * @param evt Mouse click event
     */
    private void defaultAnnotationCheckBoxMenuItemAction(ActionEvent evt) {
        updateSpectrum();
    }

    /**
     * Open annotation setting dialog
     * @param evt Mouse click event
     */
    private void annotationSettingsJMenuItemActionPerformed(ActionEvent evt) {

        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        new AnnotationSettingsDialog(this, searchParameters.getPtmSettings());
    }

    /**
     * ExportSpectrumGraphicsJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportSpectrumGraphicsJMenuItemActionPerformed(ActionEvent evt) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if(mirrorSelected){
            exportMirrorSpectrumAsFigure();
        } else if(peptideCheckSelected){
            exportCheckSpectrumAsFigure();
        } else {
            exportSpectrumAsFigure();
        }
    }

    /**
     * Update the export exacted size dialog
     */
    public void updateExportJDialog(){
        if(mirrorSelected){
            exportMirrorSpectrumAsFigure();
        } else if(peptideCheckSelected){
            exportCheckSpectrumAsFigure();
        } else {
            exportSpectrumAsFigure();
        }
    }

    /**
     * Export normal spectrum
     */
    private void exportSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, spectrumJLayeredPane, sequenceFragmentationPanel, null, spectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), false);

        spectrumSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Export mirror spectrum
     */
    private void exportMirrorSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, mirrorJLayeredPane, sequenceFragmentationPanelMirror, mirrorFragmentPanel, mirrorSpectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), false);

        mirrorSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Export check peptide spectrum
     */
    private void exportCheckSpectrumAsFigure() {

        ExportExpectedSizeDialog exportExpectedSizeDialog = new ExportExpectedSizeDialog(this, checkPeptideJLayeredPane, sequenceFragmentationPanelCheck, checkFragmentPanel, checkPeptideSpectrumPanel, currentPeptideSequence.length(), currentSpectrum.getSpectrumTitle(), true);

        checkSetAction.setExportDialog(exportExpectedSizeDialog);

        exportExpectedSizeDialog.setVisible(true);
    }

    /**
     * Switch pane to normal spectrum
     * @param evt Mosue click event
     */
    public void showSpectrumJMenuItemAction(ActionEvent evt) {
        mirrorSelected = false;
        peptideCheckSelected = false;

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();

        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(false);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to fragment table
     * @param evt Mouse click event
     */
    private void showIonTableJMenuItemAction(ActionEvent evt) {
        exportGraphicsMenu.setVisible(false);
        splitterMenu6.setVisible(false);

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(fragmentIonJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(fragmentIonJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(false);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to Mirror spectrum
     * @param evt Mouse click event
     */
    public void showMirrorJMenuItemActionPerformed(ActionEvent evt) {
        mirrorSelected = true;
        peptideCheckSelected = false;

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mirrorJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mirrorJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(true);
        checkFileMenu.setVisible(true);
        peptideCheckMenu.setVisible(false);
    }

    /**
     * Switch pane to Mirror spectrum
     * @param evt Mouse click event
     */
    public void showCheckPeptideJMenuItemActionPerformed(ActionEvent evt) {
        mirrorSelected = false;
        peptideCheckSelected = true;

        exportGraphicsMenu.setVisible(true);
        splitterMenu6.setVisible(true);

        mainShowJPanel.removeAll();
        GroupLayout mainShowJPanelLayout = new GroupLayout(mainShowJPanel);
        mainShowJPanel.setLayout(mainShowJPanelLayout);

        mainShowJPanelLayout.setHorizontalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(checkPeptideJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanelLayout.setVerticalGroup(
                mainShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(checkPeptideJLayeredPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        mainShowJPanel.revalidate();
        mainShowJPanel.repaint();
        splitterMenu7.setVisible(true);
        checkFileMenu.setVisible(false);
        peptideCheckMenu.setVisible(true);
    }

    /**
     * Select synthetic peptide spectrum
     * @param evt Menu click event
     */
    private void checkFileMenuItemPerform(ActionEvent evt) {
        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Mgf File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        javax.swing.filechooser.FileFilter filter = new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File checkSpectrumFile = fileChooser.getSelectedFile();

            if(checkSpectrumFileMaps.containsKey(selectedPsmKey)){
                checkSpectrumFileMaps.remove(selectedPsmKey);
            }

            checkSpectrumFileMaps.put(selectedPsmKey,checkSpectrumFile);

            lastSelectedFolder.setLastSelectedFolder(checkSpectrumFile.getParent());

            updateSpectrum();
        }
    }

    /**
     * Select synthetic peptide spectrum
     * @param evt Menu click event
     */
    private void checkPeptideMenuItemPerform(ActionEvent evt) {

        new CheckPeptideJDialog(this, selectedPsmKey);
    }

    /**
     * Update spectrum
     * @param mSnSpectrum MSNSpectrum
     */
    public void updateSpectrum(MSnSpectrum mSnSpectrum){

        switchPaneMenu.setVisible(false);
        deNovoMenu.setEnabled(false);

        spectrumJLayeredPane.removeAll();

        try{
            if (mSnSpectrum != null) {

                Collection<Peak> peaks = mSnSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {
                    // peaks list not found
                } else {

                    boolean newMax = false;

                    if (selectedPsmKey != null) {
                        try {
                            if (mSnSpectrum.getPeakList() != null) {
                                lastMzMaximum = mSnSpectrum.getMaxMz() * 1.05;
                                newMax = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    double lowerMzZoomRange = 0;
                    double upperMzZoomRange = lastMzMaximum;
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) {
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = mSnSpectrum.getPrecursor();

                    double[] intensitiesAsArray = mSnSpectrum.getIntensityValuesNormalizedAsArray();

                    spectrumPanel = new SpectrumContainer(
                            mSnSpectrum.getMzValuesAsArray(), intensitiesAsArray,
                            precursor.getMz(), precursor.getPossibleCharges().get(0).getChargeAsFormattedString(),
                            "", 40, false, false, false, 2, false, isDenovo);
                    spectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    spectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(1);
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);

                    spectrumPanel.showAnnotatedPeaksOnly(annotationSettings.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    spectrumJLayeredPane.add(spectrumPanel);
                    spectrumPanel.setBounds(0, 0, spectrumShowPanel.getWidth(), spectrumShowPanel.getHeight() - 25);

                    spectrumShowPanel.revalidate();
                    spectrumShowPanel.repaint();

                }
            }
        } catch (Exception e){
            JOptionPane.showMessageDialog(null, "An error occurred when Update spectrum.", "update error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Update spectrum
     */
    public void updateSpectrum(){
        spectrumJLayeredPane.removeAll();
        mirrorJLayeredPane.removeAll();
        checkPeptideJLayeredPane.removeAll();

        int maxCharge = 1;
        ArrayList<ModificationMatch> allModifications = new ArrayList<>();

        try {
            if (currentSpectrum != null) {

                Collection<Peak> peaks = currentSpectrum.getPeakList();

                if (peaks == null || peaks.isEmpty()) {

                } else {

                    boolean newMax = false;

                    if (selectedPsmKey != null) {
                        try {
                            MSnSpectrum tempSpectrum = currentSpectrum;
                            if (tempSpectrum.getPeakList() != null) {
                                lastMzMaximum = tempSpectrum.getMaxMz()*1.05;
                                newMax = true;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    double lowerMzZoomRange = 0;
                    double upperMzZoomRange = lastMzMaximum;
                    if (spectrumPanel != null && spectrumPanel.getXAxisZoomRangeLowerValue() != 0 && !newMax) {
                        lowerMzZoomRange = spectrumPanel.getXAxisZoomRangeLowerValue();
                        upperMzZoomRange = spectrumPanel.getXAxisZoomRangeUpperValue();
                    }

                    Precursor precursor = currentSpectrum.getPrecursor();

                    double[] intensitiesAsArray = currentSpectrum.getIntensityValuesNormalizedAsArray();

                    spectrumPanel = new SpectrumContainer(
                            currentSpectrum.getMzValuesAsArray(), intensitiesAsArray,
                            precursor.getMz(), (spectrumIdentificationAssumption.getIdentificationCharge().value) + "+",
                            "", 40, false, showDetails, false, 2, false, isDenovo);

                    spectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    spectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    spectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    spectrumPanel.setBorder(null);
                    spectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    spectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    spectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    spectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

                    specificAnnotationSettings = annotationSettings.getSpecificAnnotationPreferences(selectedPsmKey, spectrumIdentificationAssumption, SequenceMatchingPreferences.defaultStringMatching, SequenceMatchingPreferences.defaultStringMatching);
                    updateAnnotationSettings();

                    String modSequence;
                    ArrayList<IonMatch> annotations;

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        currentPeptideSequence = tagAssumption.getTag().asSequence();
                        modSequence = tagAssumption.getTag().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        annotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, currentSpectrum, tagAssumption.getTag());
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        Peptide currentPeptide = peptideAssumption.getPeptide();
                        currentPeptideSequence = currentPeptide.getSequence();
                        modSequence = currentPeptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        allModifications = currentPeptide.getModificationMatches();

                        annotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, currentSpectrum, currentPeptide, null, true);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    spectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);

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

                    spectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    spectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    spectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);

                    DecimalFormat df = new DecimalFormat("#.00");

                    Double allMatchInt = 0.0;
                    Double allPeakInt = 0.0;
                    HashMap<Integer, ArrayList<String>> bIonMap = new HashMap<>();
                    HashMap<Integer, ArrayList<String>> yIonMap = new HashMap<>();
                    ArrayList<String> bIonList;
                    ArrayList<String> yIonList;
                    ArrayList<Double> mzList = new ArrayList<>();
                    for (IonMatch ionMatch : annotations){
                        String match = ionMatch.getPeakAnnotation();

                        Integer charge = ionMatch.charge;

                        if (match.contains("b")) {

                            if (!match.contains("-")) {

                                if (bIonMap.containsKey(charge)) {
                                    bIonMap.get(charge).add(match.replace("+", ""));
                                } else {
                                    bIonList = new ArrayList<>();
                                    bIonList.add(match.replace("+", ""));
                                    bIonMap.put(charge, bIonList);
                                }
                            } else {

                                String chargeMatch = match.split("-")[0];

                                if (bIonMap.containsKey(charge)) {
                                    bIonMap.get(charge).add(chargeMatch.replace("+", ""));
                                } else {
                                    bIonList = new ArrayList<>();
                                    bIonList.add(chargeMatch.replace("+", ""));
                                    bIonMap.put(charge, bIonList);
                                }

                            }

                        } else if (match.contains("y")){

                            if (!match.contains("-")) {

                                if (yIonMap.containsKey(charge)) {
                                    yIonMap.get(charge).add(match.replace("+", ""));
                                } else {
                                    yIonList = new ArrayList<>();
                                    yIonList.add(match.replace("+", ""));
                                    yIonMap.put(charge, yIonList);
                                }
                            } else {

                                String chargeMatch = match.split("-")[0];

                                if (yIonMap.containsKey(charge)) {
                                    yIonMap.get(charge).add(chargeMatch.replace("+", ""));
                                } else {
                                    yIonList = new ArrayList<>();
                                    yIonList.add(chargeMatch.replace("+", ""));
                                    yIonMap.put(charge, yIonList);
                                }

                            }
                        }

                        if (!mzList.contains(ionMatch.peak.getMz())){
                            allMatchInt += ionMatch.peak.getIntensity();
                            mzList.add(ionMatch.peak.getMz());
                        }
                    }

                    for (Double each : currentSpectrum.getIntensityValuesAsArray()){
                        allPeakInt += each;
                    }

                    matchedToAllPeakInt = allMatchInt.intValue() + "/" + allPeakInt.intValue() + "  " + df.format((allMatchInt/allPeakInt) * 100) + "%";
                    contentJLabel.setText(matchedToAllPeakInt);
                    contentJLabel.setForeground(new Color(15, 22,255));

                    Integer[] nums = getPair(bIonMap, yIonMap, currentPeptideSequence.length());

                    bIonNumJLabel.setText(String.valueOf(nums[0]));
                    bIonNumJLabel.setForeground(new Color(15, 22,255));
                    yIonNumJLabel.setText(String.valueOf(nums[1]));
                    yIonNumJLabel.setForeground(new Color(15, 22,255));
                    matchNumJLabel.setText(String.valueOf(nums[2]));
                    matchNumJLabel.setForeground(new Color(15, 22,255));

                    int currentCharge = spectrumIdentificationAssumption.getIdentificationCharge().value;
                    if (currentCharge > maxCharge) {
                        maxCharge = currentCharge;
                    }

                    sequenceFragmentationPanel = new SequenceFragmentationPanel(
                            modSequence, annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                    sequenceFragmentationPanel.setOpaque(false);
                    sequenceFragmentationPanel.setBackground(Color.WHITE);

                    spectrumJLayeredPane.add(spectrumPanel);

                    spectrumPanel.setBounds(0, 75, spectrumShowPanel.getWidth(),spectrumShowPanel.getHeight()-85);

                    spectrumJLayeredPane.setLayer(sequenceFragmentationPanel, JLayeredPane.DRAG_LAYER);
                    spectrumJLayeredPane.add(sequenceFragmentationPanel);
                    zoomAction(sequenceFragmentationPanel, modSequence, false);

                    spectrumSetAction = new SetAction(this, spectrumJLayeredPane, sequenceFragmentationPanel, null, spectrumPanel, 0, 0, spectrumShowPanel);

                    mirrorSpectrumPanel = new SpectrumContainer(
                            currentSpectrum.getMzValuesAsArray(), intensitiesAsArray,
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false, isDenovo);

                    sequenceFragmentationPanelMirror = new SequenceFragmentationPanel(
                            modSequence, annotations, true, searchParameters.getPtmSettings(), PeptideFragmentIon.B_ION, PeptideFragmentIon.Y_ION);
                    sequenceFragmentationPanelMirror.setMinimumSize(new Dimension(sequenceFragmentationPanelMirror.getPreferredSize().width, sequenceFragmentationPanelMirror.getHeight()));
                    sequenceFragmentationPanelMirror.setOpaque(false);
                    sequenceFragmentationPanelMirror.setBackground(Color.WHITE);

                    if(checkSpectrumFileMaps.containsKey(selectedPsmKey)){
                        File checkSpectrumFile = checkSpectrumFileMaps.get(selectedPsmKey);
                        SpectrumFactory spectrumFactory1 = SpectrumFactory.getInstance();
                        spectrumFactory1.addSpectra(checkSpectrumFile);
                        MSnSpectrum mirrorSpectrum = (MSnSpectrum) spectrumFactory1.getSpectrum(checkSpectrumFile.getName(), spectrumFactory1.getSpectrumTitle(checkSpectrumFile.getName(),1));
                        Precursor mirrorPrecursor = mirrorSpectrum.getPrecursor();
                        mirrorSpectrumPanel.addMirroredSpectrum(
                                mirrorSpectrum.getMzValuesAsArray(), mirrorSpectrum.getIntensityValuesNormalizedAsArray(), mirrorPrecursor.getMz(),
                                "", "", false, utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor(),
                                utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor());

                        ArrayList<IonMatch> mirroredAnnotations;

                        if (spectrumIdentificationAssumption instanceof TagAssumption) {
                            TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                            mirroredAnnotations = tagSpectrumAnnotator.getSpectrumAnnotation(annotationSettings, specificAnnotationSettings, mirrorSpectrum, tagAssumption.getTag());
                        } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                            PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                            mirroredAnnotations = peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, mirrorSpectrum, peptideAssumption.getPeptide(), null, true);
                        } else {
                            throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                        }

                        mirrorSpectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(mirroredAnnotations));

                        mirrorFragmentPanel = new SequenceFragmentationPanel(
                                modSequence,
                                mirroredAnnotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        mirrorFragmentPanel.setMinimumSize(new Dimension(mirrorFragmentPanel.getPreferredSize().width, mirrorFragmentPanel.getHeight()));
                        mirrorFragmentPanel.setOpaque(false);
                        mirrorFragmentPanel.setBackground(Color.WHITE);

                        mirrorJLayeredPane.setLayer(mirrorFragmentPanel, JLayeredPane.DRAG_LAYER);
                        mirrorJLayeredPane.add(mirrorFragmentPanel);
                        zoomAction(mirrorFragmentPanel, modSequence, true);
                    }

                    mirrorSpectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    mirrorSpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    mirrorSpectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    mirrorSpectrumPanel.setBorder(null);
                    mirrorSpectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    mirrorSpectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    mirrorSpectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    mirrorSpectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    mirrorSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    mirrorSpectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    mirrorSpectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    mirrorSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);
                    mirrorSpectrumPanel.setMaxPadding(70);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        mirrorSpectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        mirrorSpectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    mirrorJLayeredPane.add(mirrorSpectrumPanel);
                    mirrorSpectrumPanel.setBounds(0,75,spectrumShowPanel.getWidth(),spectrumShowPanel.getHeight()-85);

                    mirrorJLayeredPane.setLayer(sequenceFragmentationPanelMirror, JLayeredPane.DRAG_LAYER);
                    mirrorJLayeredPane.add(sequenceFragmentationPanelMirror);
                    zoomAction(sequenceFragmentationPanelMirror, modSequence, false);

                    mirrorSetAction = new SetAction(this, mirrorJLayeredPane, sequenceFragmentationPanelMirror, mirrorFragmentPanel, mirrorSpectrumPanel, 0, 0, spectrumShowPanel);

                    checkPeptideSpectrumPanel = new SpectrumContainer(
                            currentSpectrum.getMzValuesAsArray(), intensitiesAsArray,
                            precursor.getMz(), spectrumIdentificationAssumption.getIdentificationCharge().toString(),
                            "", 40, false, false, false, 2, false, isDenovo);

                    sequenceFragmentationPanelCheck = new SequenceFragmentationPanel(
                            modSequence,
                            annotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                    sequenceFragmentationPanelCheck.setMinimumSize(new Dimension(sequenceFragmentationPanelCheck.getPreferredSize().width, sequenceFragmentationPanelCheck.getHeight()));
                    sequenceFragmentationPanelCheck.setOpaque(false);
                    sequenceFragmentationPanelCheck.setBackground(Color.WHITE);

                    if(checkPeptideMap.containsKey(selectedPsmKey)){
                        Peptide peptide = checkPeptideMap.get(selectedPsmKey);

                        ArrayList<ModificationMatch> checkModifications = peptide.getModificationMatches();

                        for (ModificationMatch modificationMatch : checkModifications){
                            if (!allModifications.contains(modificationMatch)){
                                allModifications.add(modificationMatch);
                            }
                        }

                        String checkModSequence = peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

                        checkPeptideSpectrumPanel.addMirroredSpectrum(currentSpectrum.getMzValuesAsArray(), intensitiesAsArray, precursor.getMz(),
                                "", "", false, utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor(),
                                utilitiesUserPreferences.getSpectrumAnnotatedMirroredPeakColor());

                        ArrayList<IonMatch> checkAnnotations =  peptideSpectrumAnnotator.getSpectrumAnnotationFiter(annotationSettings, specificAnnotationSettings, currentSpectrum, peptide, null, true);

                        checkPeptideSpectrumPanel.setAnnotationsMirrored(SpectrumAnnotator.getSpectrumAnnotation(checkAnnotations));

                        checkFragmentPanel = new SequenceFragmentationPanel(
                                checkModSequence,
                                checkAnnotations, true, searchParameters.getPtmSettings(), forwardIon, rewindIon);
                        checkFragmentPanel.setMinimumSize(new Dimension(checkFragmentPanel.getPreferredSize().width, checkFragmentPanel.getHeight()));
                        checkFragmentPanel.setOpaque(false);
                        checkFragmentPanel.setBackground(Color.WHITE);

                        checkPeptideJLayeredPane.setLayer(checkFragmentPanel, JLayeredPane.DRAG_LAYER);
                        checkPeptideJLayeredPane.add(checkFragmentPanel);
                        zoomAction(checkFragmentPanel, checkModSequence, true);

                    }

                    checkPeptideSpectrumPanel.setFont(new Font("Arial", Font.PLAIN, 13));

                    checkPeptideSpectrumPanel.setKnownMassDeltas(getCurrentMassDeltas());
                    checkPeptideSpectrumPanel.setDeltaMassWindow(annotationSettings.getFragmentIonAccuracy());
                    checkPeptideSpectrumPanel.setBorder(null);
                    checkPeptideSpectrumPanel.setDataPointAndLineColor(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor(), 0);
                    checkPeptideSpectrumPanel.setPeakWaterMarkColor(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
                    checkPeptideSpectrumPanel.setPeakWidth(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
                    checkPeptideSpectrumPanel.setBackgroundPeakWidth(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());
                    checkPeptideSpectrumPanel.setAnnotations(SpectrumAnnotator.getSpectrumAnnotation(annotations), annotationSettings.getTiesResolution() == SpectrumAnnotator.TiesResolution.mostAccurateMz);
                    checkPeptideSpectrumPanel.rescale(lowerMzZoomRange, upperMzZoomRange);
                    checkPeptideSpectrumPanel.showAnnotatedPeaksOnly(!annotationSettings.showAllPeaks());
                    checkPeptideSpectrumPanel.setYAxisZoomExcludesBackgroundPeaks(false);
                    checkPeptideSpectrumPanel.setMaxPadding(70);

                    if (spectrumIdentificationAssumption instanceof TagAssumption) {
                        TagAssumption tagAssumption = (TagAssumption) spectrumIdentificationAssumption;
                        checkPeptideSpectrumPanel.addAutomaticDeNovoSequencing(tagAssumption.getTag(), annotations,
                                TagFragmentIon.B_ION,
                                TagFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        checkPeptideSpectrumPanel.addAutomaticDeNovoSequencing(peptideAssumption.getPeptide(), annotations,
                                PeptideFragmentIon.B_ION,
                                PeptideFragmentIon.Y_ION,
                                annotationSettings.getDeNovoCharge(),
                                annotationSettings.showForwardIonDeNovoTags(),
                                annotationSettings.showRewindIonDeNovoTags(), false);
                    } else {
                        throw new UnsupportedOperationException("Operation not supported for spectrumIdentificationAssumption of type " + spectrumIdentificationAssumption.getClass() + ".");
                    }

                    checkPeptideJLayeredPane.add(checkPeptideSpectrumPanel);
                    checkPeptideSpectrumPanel.setBounds(0,75,spectrumShowPanel.getWidth(),spectrumShowPanel.getHeight()-85);

                    checkPeptideJLayeredPane.setLayer(sequenceFragmentationPanelCheck, JLayeredPane.DRAG_LAYER);
                    checkPeptideJLayeredPane.add(sequenceFragmentationPanelCheck);
                    zoomAction(sequenceFragmentationPanelCheck, modSequence, false);

                    checkSetAction = new SetAction(this, checkPeptideJLayeredPane, sequenceFragmentationPanelCheck, checkFragmentPanel, checkPeptideSpectrumPanel, 0, 0, spectrumShowPanel);

                    ArrayList<ArrayList<IonMatch>> allAnnotations = new ArrayList<>();
                    allAnnotations.add(annotations);

                    if (spectrumIdentificationAssumption instanceof PeptideAssumption) {
                        PeptideAssumption peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;
                        ionFragmentsJScrollPane.setViewportView(new FragmentIonTable(peptideAssumption.getPeptide(), allAnnotations, specificAnnotationSettings.getFragmentIonTypes(),
                                specificAnnotationSettings.getNeutralLossesMap(),
                                specificAnnotationSettings.getSelectedCharges().contains(1),
                                specificAnnotationSettings.getSelectedCharges().contains(2)));
                    }

                    spectrumShowPanel.revalidate();
                    spectrumShowPanel.repaint();

                    updateAnnotationMenus(maxCharge, allModifications);
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when Update spectrum.", "update error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Zoom in out
     * @param sequenceFragmentationPanel Sequence fragment panel
     * @param modSequence Peptide Sequence
     * @param isDown Down or not
     */
    private void zoomAction(SequenceFragmentationPanel sequenceFragmentationPanel, String modSequence, Boolean isDown){

        JPanel fragmentJPanel = new JPanel();
        fragmentJPanel.setOpaque(false);

        int fontHeight = sequenceFragmentationPanel.getFontMetrics(sequenceFragmentationPanel.getFont()).getHeight() + 3;
        int length = 0;

        String[] firstSplit = modSequence.split("<");

        for (String firstOne : firstSplit){
            String[] secondSplit = firstOne.split(">");
            if (secondSplit.length == 1){
                length += secondSplit[0].length();
            } else {
                length += secondSplit[1].length();
            }
        }

        final int peptideLength = length;

        if (!isDown){
            sequenceFragmentationPanel.setBounds(40,10, peptideLength*fontHeight*2 ,fontHeight * 5);
        } else {
            sequenceFragmentationPanel.setBounds(40,spectrumShowPanel.getHeight()/2, peptideLength*fontHeight*2 ,fontHeight * 5);
        }

        sequenceFragmentationPanel.addMouseWheelListener(e -> {
            if(e.getWheelRotation()==1){
                sequenceFragmentationPanel.updateFontSize(-1);

                sequenceFragmentationPanel.revalidate();
                sequenceFragmentationPanel.repaint();
            }
            if(e.getWheelRotation()==-1){
                sequenceFragmentationPanel.updateFontSize(1);

                sequenceFragmentationPanel.revalidate();
                sequenceFragmentationPanel.repaint();
            }
        });
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
        ArrayList<Integer> bIonLength = new ArrayList<>();
        ArrayList<Integer> yIonLength = new ArrayList<>();

        for (Integer eachCharge : bIonMap.keySet()){
            bIonList = bIonMap.get(eachCharge);

            bIonNum += bIonList.size();

            for (String eachMatch : bIonList){

                int ionLength = Integer.valueOf(eachMatch.substring(1, eachMatch.length()));

                if (!bIonLength.contains(ionLength)){
                    bIonLength.add(ionLength);
                }
            }
        }
        for (Integer eachCharge : yIonMap.keySet()){
            yIonList = yIonMap.get(eachCharge);

            yIonNum += yIonList.size();

            for (String eachYMatch : yIonList){

                int ionLength = length - Integer.valueOf(eachYMatch.substring(1,eachYMatch.length()));

                if (!yIonLength.contains(ionLength)) {
                    yIonLength.add(ionLength);
                }
            }
        }

        for (Integer bIon : bIonLength){
            if (yIonLength.contains(bIon)){
                pairNum ++;
            }
        }

        nums[0] = bIonNum;
        nums[1] = yIonNum;
        nums[2] = pairNum;

        return nums;
    }

    /**
     * Update the spectrum
     * @param spectrumIdentificationAssumption SpectrumIdentificationAssumption
     * @param spectrum MSNSpectrum
     * @param selectedPsmKey Spectrum key
     */
    public void updateSpectrum(SpectrumIdentificationAssumption spectrumIdentificationAssumption, MSnSpectrum spectrum, String selectedPsmKey){

        ionsMenu.setEnabled(true);
        otherMenu.setEnabled(true);
        lossMenu.setEnabled(true);
        chargeMenu.setEnabled(true);
        settingsMenu.setEnabled(true);
        exportGraphicsMenu.setEnabled(true);
        switchPaneMenu.setVisible(true);
        switchPaneMenu.setEnabled(true);

        this.spectrumIdentificationAssumption = spectrumIdentificationAssumption;
        this.currentSpectrum = spectrum;
        this.selectedPsmKey = selectedPsmKey;

        updateSpectrum();
    }

    /**
     * Update teh search parameter
     * @param searchParameters Parameter
     */
    public void updateSearchParameters(SearchParameters searchParameters){
        this.searchParameters = searchParameters;

        annotationSettings.setPreferencesFromSearchParameters(searchParameters);
    }

    /**
     * Get the current delta masses map
     * @return Current delta masses map
     */
    private HashMap<Double, String> getCurrentMassDeltas() {
        HashMap<Double, String> currentMassDeltaMap = new HashMap<>();

        PtmSettings ptmSettings = searchParameters.getPtmSettings();
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

    /**
     * Update the annotation menu bar
     * @param precursorCharge Precursor charges
     * @param modificationMatches Modification matches list
     */
    private void updateAnnotationMenus(int precursorCharge, ArrayList<ModificationMatch> modificationMatches) {

        forwardAIonCheckBoxMenuItem.setSelected(false);
        forwardBIonCheckBoxMenuItem.setSelected(false);
        forwardCIonCheckBoxMenuItem.setSelected(false);
        rewardXIonCheckBoxMenuItem.setSelected(false);
        rewardYIonCheckBoxMenuItem.setSelected(false);
        rewardZIonCheckBoxMenuItem.setSelected(false);
        deNovoChargeOneJRadioButtonMenuItem.setSelected(false);
        precursorCheckMenuItem.setSelected(false);
        immoniumIonsCheckMenuItem.setSelected(false);
        relatedIonsCheckMenuItem.setSelected(false);
        reporterIonsCheckMenuItem.setSelected(false);

        for (JCheckBoxMenuItem lossMenuItem : lossMenuMap.values()) {
            lossMenu.remove(lossMenuItem);
        }
        lossMenu.setVisible(true);
        lossSplitter.setVisible(true);
        lossMenuMap.clear();

        HashMap<String, NeutralLoss> neutralLossHashMap = new HashMap<>();

        for (NeutralLoss neutralLoss : IonFactory.getInstance().getDefaultNeutralLosses()) {
            neutralLossHashMap.put(neutralLoss.name, neutralLoss);
        }

        for (ModificationMatch modificationMatch : modificationMatches) {

            NeutralLoss neutralLoss = getPhosphyNeutralLoss(modificationMatch);

            if (neutralLoss != null){
                neutralLossHashMap.put(neutralLoss.name, neutralLoss);
            }
        }

        ArrayList<String> neutralLossNameList = new ArrayList<>(neutralLossHashMap.keySet());
        Collections.sort(neutralLossNameList);

        if (neutralLossHashMap.isEmpty()) {
            lossMenu.setVisible(false);
            lossSplitter.setVisible(false);
        } else {

            ArrayList<String> currentNeutralLosses;
            boolean neutralLossesAuto;
            if (specificAnnotationSettings != null) {
                currentNeutralLosses = specificAnnotationSettings.getNeutralLossesMap().getAccountedNeutralLosses();
                neutralLossesAuto = specificAnnotationSettings.isNeutralLossesAuto();
            } else {
                ArrayList<NeutralLoss> annotationNeutralLosses = annotationSettings.getNeutralLosses();
                if (annotationNeutralLosses != null) {
                    currentNeutralLosses = new ArrayList<>(annotationNeutralLosses.size());
                    for (NeutralLoss neutralLoss : annotationNeutralLosses) {
                        currentNeutralLosses.add(neutralLoss.name);
                    }
                } else {
                    currentNeutralLosses = new ArrayList<>(0);
                }
                neutralLossesAuto = true;
            }

            for (int i = 0; i < neutralLossNameList.size(); i++) {

                String neutralLossName = neutralLossNameList.get(i);
                NeutralLoss neutralLoss = neutralLossHashMap.get(neutralLossName);

                boolean selected = false;
                for (String specificNeutralLossName : currentNeutralLosses) {
                    NeutralLoss specificNeutralLoss = NeutralLoss.getNeutralLoss(specificNeutralLossName);
                    if (neutralLoss.isSameAs(specificNeutralLoss)) {
                        selected = true;
                        break;
                    }
                }

                JCheckBoxMenuItem lossMenuItem = new JCheckBoxMenuItem(neutralLossName);
                lossMenuItem.setSelected(selected);
                lossMenuItem.setEnabled(!neutralLossesAuto);
                lossMenuItem.addActionListener(evt -> updateSpectrum());
                lossMenuMap.put(neutralLossHashMap.get(neutralLossName), lossMenuItem);
                lossMenu.add(lossMenuItem, i);
            }
            defaultLossCheckBoxMenuItem.setSelected(neutralLossesAuto);
        }

        chargeMenuMap.clear();
        chargeMenu.removeAll();

        if (precursorCharge == 1) {
            precursorCharge = 2;
        }

        ArrayList<Integer> selectedCharges;
        if (specificAnnotationSettings != null) {
            selectedCharges = specificAnnotationSettings.getSelectedCharges();
        } else {
            selectedCharges = new ArrayList<>();
            selectedCharges.add(1);
        }

        for (Integer charge = 1; charge <= precursorCharge; charge++) {

            final JCheckBoxMenuItem chargeMenuItem = new JCheckBoxMenuItem(charge + "+");

            chargeMenuItem.setSelected(selectedCharges.contains(charge));
            chargeMenuItem.addActionListener(evt -> {
                noSelectDefaultAnnotationMenuItem();
                updateSpectrumAnnotations();
            });

            chargeMenuMap.put(charge, chargeMenuItem);
            chargeMenu.add(chargeMenuItem);
        }

        HashMap<Ion.IonType, HashSet<Integer>> ionTypes;
        if (specificAnnotationSettings != null) {
            ionTypes = specificAnnotationSettings.getIonTypes();
        } else {
            ionTypes = annotationSettings.getIonTypes();
        }

        for (Ion.IonType ionType : ionTypes.keySet()) {
            if (null != ionType) {
                switch (ionType) {
                    case IMMONIUM_ION:
                        immoniumIonsCheckMenuItem.setSelected(true);
                        break;
                    case RELATED_ION:
                        relatedIonsCheckMenuItem.setSelected(true);
                        break;
                    case PRECURSOR_ION:
                        precursorCheckMenuItem.setSelected(true);
                        break;
                    case REPORTER_ION:
                        reporterIonsCheckMenuItem.setSelected(true);
                        break;
                    case TAG_FRAGMENT_ION:
                        for (int subtype : ionTypes.get(ionType)) {
                            switch (subtype) {
                                case TagFragmentIon.A_ION:
                                    forwardAIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.B_ION:
                                    forwardBIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.C_ION:
                                    forwardCIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.X_ION:
                                    rewardXIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Y_ION:
                                    rewardYIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                case TagFragmentIon.Z_ION:
                                    rewardZIonCheckBoxMenuItem.setSelected(true);
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }

        showAllPeaksMenuItem.setSelected(annotationSettings.showAllPeaks());
    }

    private NeutralLoss getPhosphyNeutralLoss(ModificationMatch modificationMatch){

        String name = modificationMatch.getTheoreticPtm();
        String aa = name.split("of ")[1];

        if (aa.equals("T") || aa.equals("S")){
            double mass = ptmFactory.getPTM(name).getMass();

            if (mass < 80.01 && mass > 79.9){
                return H3PO4;
            }
        }
        return null;

    }

    /**
     * Save the current annotation preferences selected in the annotation menus.
     */
    private void updateAnnotationSettings() {

        if (!defaultAnnotationCheckBoxMenuItem.isSelected()) {

            specificAnnotationSettings.clearIonTypes();
            if (forwardAIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.A_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.A_ION);
            }
            if (forwardBIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.B_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.B_ION);
            }
            if (forwardCIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.C_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.C_ION);
            }
            if (rewardXIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.X_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.X_ION);
            }
            if (rewardYIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Y_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Y_ION);
            }
            if (rewardZIonCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(TAG_FRAGMENT_ION, TagFragmentIon.Z_ION);
                specificAnnotationSettings.addIonType(PEPTIDE_FRAGMENT_ION, PeptideFragmentIon.Z_ION);
            }
            if (precursorCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(PRECURSOR_ION);
            }
            if (immoniumIonsCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(IMMONIUM_ION);
            }
            if (relatedIonsCheckMenuItem.isSelected()) {
                specificAnnotationSettings.addIonType(RELATED_ION);
            }
            if (reporterIonsCheckMenuItem.isSelected()) {
                ArrayList<Integer> reporterIons = new ArrayList<>(IonFactory.getReporterIons(searchParameters.getPtmSettings()));
                for (int subtype : reporterIons) {
                    specificAnnotationSettings.addIonType(REPORTER_ION, subtype);
                }
            }

            if (!defaultLossCheckBoxMenuItem.isSelected()) {
                specificAnnotationSettings.setNeutralLossesAuto(false);
                specificAnnotationSettings.clearNeutralLosses();
                for (NeutralLoss neutralLoss : lossMenuMap.keySet()) {
                    if (lossMenuMap.get(neutralLoss).isSelected()) {
                        specificAnnotationSettings.addNeutralLoss(neutralLoss);
                    }
                }
            } else {
                specificAnnotationSettings.clearNeutralLosses();
            }

            specificAnnotationSettings.clearCharges();
            for (int charge : chargeMenuMap.keySet()) {
                if (chargeMenuMap.get(charge).isSelected()) {
                    specificAnnotationSettings.addSelectedCharge(charge);
                }
            }

        } else {
            specificAnnotationSettings.clearNeutralLosses();
            selectDefaultAnnotationMenuItem();
        }

        annotationSettings.setShowAllPeaks(showAllPeaksMenuItem.isSelected());
        annotationSettings.setShowForwardIonDeNovoTags(forwardIonsDeNovoCheckBoxMenuItem.isSelected());
        annotationSettings.setShowRewindIonDeNovoTags(rewindIonsDeNovoCheckBoxMenuItem.isSelected());

        if (deNovoChargeOneJRadioButtonMenuItem.isSelected()) {
            annotationSettings.setDeNovoCharge(1);
        } else {
            annotationSettings.setDeNovoCharge(2);
        }
    }

    /**
     * Update annotation settings
     */
    public void updateSpectrumAnnotations() {
        updateSpectrum();
    }

    /**
     * SelectDefaultAnnotationMenuItem
     */
    private void selectDefaultAnnotationMenuItem() {
        defaultAnnotationCheckBoxMenuItem.setSelected(true);
    }

    /**
     * load userPreference
     */
    public void loadUserPreferences() {
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
            lastSelectedFolder = new LastSelectedFolder("user.home");
            lastSelectedFolder = utilitiesUserPreferences.getLastSelectedFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
        }
    }

    /**
     * Set annotation setting
     * @param annotationSettings AnnotationSetting
     */
    public void setAnnotationSettings(AnnotationSettings annotationSettings) {
        this.annotationSettings = annotationSettings;
    }

    /**
     * Get annotation settings
     * @return AnnotationSettings
     */
    public AnnotationSettings getAnnotationSettings() {
        return annotationSettings;
    }

    /**
     * Set utilitiesUserPreferences
     */
    public void setUtilitiesUserPreferences(UtilitiesUserPreferences utilitiesUserPreferences){
        this.utilitiesUserPreferences = utilitiesUserPreferences;
    }

    /**
     * Get spectrum main panel all parameters
     * @return Object[]
     */
    public Object[] getParameters() {

        Object[] allParameters = new Object[6];
        allParameters[0] = forwardIon;
        allParameters[1] = rewindIon;
        allParameters[2] = peptideSpectrumAnnotator;
        allParameters[3] = specificAnnotationSettings;
        allParameters[4] = getCurrentMassDeltas();
        allParameters[5] = searchParameters.getPtmSettings();

        return allParameters;
    }
}

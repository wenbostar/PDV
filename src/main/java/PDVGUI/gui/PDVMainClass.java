package PDVGUI.gui;

import PDVCLI.PDVCLIMainClass;
import PDVGUI.fileimport.*;
import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.utils.*;
import PDVGUI.gui.utils.Export.ExportBatchDialog;
import PDVGUI.gui.utils.Export.ExportReportsJDialog;
import PDVGUI.gui.utils.Export.RealTimeExportJDialog;
import PDVGUI.gui.utils.FileImport.DatabaseImportDialog;
import PDVGUI.gui.utils.FileImport.DeNovoImportDialog;
import PDVGUI.gui.utils.TableModel.DatabaseTableModel;
import PDVGUI.gui.utils.TableModel.DeNovoTableModel;
import PDVGUI.gui.utils.TableModel.FrageTableModel;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.*;
import org.apache.commons.cli.ParseException;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.fileio.filetypes.mzml.MZMLFile;
import umich.ms.datatypes.spectrum.ISpectrum;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.MzIdentMLType;
import org.xmlpull.v1.XmlPullParserException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main class to start soft and show dataBase results
 * Created by Ken on 5/8/2017.
 */
public class PDVMainClass extends JFrame {

    public JButton searchButton;
    public JButton loadingJButton;
    /** Inline "Loading. Please Wait..." status shown at the bottom-left during a table page reload. */
    private JLabel movingStatusJLabel;
    public JButton settingColorJButton;
    public JTextField pageNumJTextField;
    public JTextField searchItemTextField;
    private JButton upSortJButton;
    private JButton downSortJButton;
    private JButton nextJButton;
    private JButton upJButton;
    private JButton openSidebarJButton;
    private JButton openSearchFileJButton;
    private JTable spectrumJTable;
    private JPanel psmsJPanel;
    private JPanel spectrumShowJPanel;
    private JPanel backgroundPanel;
    private JPanel detailsJPanel;
    private JPanel searchTextOrButtonJPanel;
    private JTextField pageSelectNumJTextField;
    private JTextField checkSpectrumJTextField;
    private JTextField fragmentIonAccuracyTxt;
    private JCheckBox allSelectedJCheckBox;
    private JSplitPane msAndTableJSplitPane;
    /** Outer vertical split: top = PSM-table region, bottom = spectrum. Held as a field so the
     *  "PSM table" toggle can collapse the top region for a spectrum-only view. */
    private JSplitPane allJSplitPane;
    /** Toolbar toggle; when unchecked the PSM-table region is hidden and the spectrum fills the view. */
    private JCheckBox showPsmTableJCheckBox;
    /** True while {@link #setPsmTableVisible} is moving the divider, to suppress the per-divider-move
     *  spectrum re-render (which would re-lay-out the split and loop, freezing the window). */
    private boolean adjustingPsmTable = false;
    private JComboBox precursorIonUnit;
    private JComboBox sortColumnJCombox;
    private JComboBox searchTypeComboBox;
    private JComboBox decimalPlacesJComboBox;
    /**
     * Number of decimal places shown for numeric columns in the PSM table
     * (-1 shows the original full-precision value)
     */
    private int tableDecimalPlaces = 2;
    private JToggleButton fitColumnsJToggleButton;
    /**
     * Column widths saved before fitting to content, keyed by column model index ->
     * {min, preferred, max}, so the fit-to-window layout can be restored
     */
    private final HashMap<Integer, int[]> savedColumnWidths = new HashMap<>();
    private String[] searchType = new String[]{"Peptide (String)","Spectrum (String)", "Peptide (File)", "Spectrum (File)"};

    /**
     * SpectrumTable tooltips list
     */
    private ArrayList<String> spectrumJTableToolTips;
    /**
     * SearchParameters
     */
    public SearchParameters searchParameters;
    /**
     * ExceptionHandler import from utilities
     */
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/wenbostar/PDV");
    /**
     * Current psmKey selected
     */
    private String selectedPsmKey;
    /**
     * Selected page spectrum index
     */
    private ArrayList<String> selectPageSpectrumIndex = new ArrayList<>();
    /**
     * Current page psm key to selected
     */
    private HashMap<String, Boolean> spectrumKeyToSelected = new HashMap<>();
    /**
     * Whole page selections
     */
    private ArrayList<Integer> pageToSelected = new ArrayList<>();
    /**
     * All selections
     */
    private ArrayList<String> allSelections = new ArrayList<>();
    /**
     * LastSelectFolder accessed easily
     */
    public static LastSelectedFolder lastSelectedFolder;
    /**
     * Utilities user preferences
     */
    public UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * Annotation setting
     */
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    /**
     * PTMFactory containing all modifications import from utilities
     */
    public PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Find Type
     */
    private String findType = "Peptide (String)";
    /**
     * Select index file
     */
    private File selectIndexFile;
    /**
     * All spectrum Index
     */
    public ArrayList<ArrayList<String>> allSpectrumIndex = new ArrayList<>();
    /**
     * Verbatim mzTab {@code spectra_ref} (e.g. "ms_run[1]:...scan=1882") -> the per-PSM row key
     * used by the spectrum table / SQLite. Filled during mzTab import; used only by the optional
     * {@link PDVGUI.gui.utils.PdvControlServer} to locate a PSM by its spectra_ref.
     */
    public final HashMap<String, String> spectraRefToSpectrumIndex = new HashMap<>();
    /**
     * True once an mzTab result has finished importing (controls the control server's readiness).
     */
    private volatile boolean loadComplete = false;
    /**
     * Optional loopback HTTP control server; null unless launched with {@code denovo-gui ... --port}.
     */
    private PdvControlServer pdvControlServer;
    /** {@link #selectBySpectraRef} result: the row was found, selected and rendered. */
    public static final int SELECT_OK = 0;
    /** {@link #selectBySpectraRef} result: no PSM matches the given spectra_ref. */
    public static final int SELECT_NOT_FOUND = 1;
    /** {@link #selectBySpectraRef} result: the result table is not loaded yet. */
    public static final int SELECT_NOT_READY = 2;
    /**
     * Current page default 1
     */
    public int selectedPageNum = 1;
    /**
     * SpectrumFactory load all spectrum files import from utilities
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * Database connection
     */
    private SQLiteConnection sqliteConnection;
    /**
     * Database absolute path
     */
    private String databasePath;
    /**
     * Objective to save mzXML and mzml file
     */
    private ScanCollectionDefault scans;
    /**
     * Objective to save map mzXML and mzml file
     */
    private HashMap<String, ScanCollectionDefault>  scansMap;
    /**
     * spectrum factory Objective
     */
    private Object spectrumsFileFactory;
    /**
     * Spectrum file type
     */
    private String  spectrumFileType;
    /**
     * All score names from Identification file
     */
    private ArrayList<String> scoreName = new ArrayList<>();
    /**
     * Name of the per-residue aa-score column (e.g. Casanovo mzTab opt_global_aa_scores),
     * or null when the current result has no such column. Used to draw the confidence track.
     */
    private String aaScoreColumn = null;
    /**
     * Boolean indicate if MaxQuant or not
     */
    private Boolean isMaxQuant = false;
    /**
     * Boolean indicate if MSFrager or not
     */
    private Boolean isFrage = false;
    /**
     * Boolean indicate if new soft or not
     */
    private Boolean isNewSoft = false;
    /**
     * Boolean indicate if mztab or not
     */
    private Boolean isMztab = false;
    /**
     * Boolean indicate if De Novo or not
     */
    private Boolean isDenovo = false;
    /**
     * Boolean indicate if pepXML or not
     */
    private Boolean isPepXML = false;
    /**
     * Boolean indicate if MS Amanda or not
     */
    private Boolean isMSAmanda = false;
    /**
     * Original information hash
     */
    private HashMap<String, Object> originalInfor = new HashMap<>();
    /**
     * Details list
     */
    private ArrayList<String> detailsList = new ArrayList<>();
    /**
     * Selected column map
     */
    public HashMap<String, Boolean> columnToSelected;
    /**
     * Current identification assumption
     */
    private SpectrumIdentificationAssumption spectrumIdentificationAssumption = null;
    /**
     * Database table model
     */
    private DatabaseTableModel databaseTableModel;
    /**
     * DeNovo table model
     */
    private DeNovoTableModel deNovoTableModel;
    /**
     * Frage table model
     */
    private FrageTableModel frageTableModel;
    /**
     * Spectrum match
     */
    private SpectrumMatch spectrumMatch;
    /**
     * Export all spectrum or not
     */
    private Boolean exportAll = false;
    /**
     * Export selected spectrum or not
     */
    private Boolean exportSelection = false;
    /**
     * Information panel
     */
    private InfoPanel infoPanel = new InfoPanel();
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum main Panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Get system separator
     */
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    /**
     * Version
     */
    private static final String VERSION = readVersion();

    /**
     * Maximum width (px) a column may reach when fitting to content
     */
    private static final int MAX_FIT_COLUMN_WIDTH = 400;
    /**
     * Number of rows sampled when measuring content width for column fitting
     */
    private static final int FIT_SAMPLE_ROWS = 50;

    /**
     * Main class
     * @param args the arguments
     */
    public static void main(String[] args) {

        if (args.length == 0){
            LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
            UIDefaults defaults = lookAndFeel.getDefaults();
            defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));

            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);

            new PDVMainClass("New");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("denovo-gui")) {
            launchDenovoGui(args);
        } else {
            try {
                new PDVCLIMainClass(args);
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Launch the GUI directly into the DeNovo (e.g. Casanovo) visualization with
     * spectrum and mzTab files supplied on the command line, bypassing the
     * welcome screen and import dialog. Usage:
     *   java -jar PDV.jar denovo-gui --mztab result.mztab --spectrum a.mzML[,b.mzML] [--tol 0.05]
     */
    public static void launchDenovoGui(String[] args) {
        java.util.List<File> spectra = new java.util.ArrayList<>();
        File mzTab = null;
        double tol = 0.05;
        String tolUnit = "Da";
        int port = -1;
        boolean hidePsmTable = false;
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--mztab") && i + 1 < args.length) {
                mzTab = new File(args[++i]);
            } else if (a.equals("--spectrum") && i + 1 < args.length) {
                for (String pth : args[++i].split(",")) {
                    String t = pth.trim();
                    if (t.isEmpty()) {
                        continue;
                    }
                    File f = new File(t);
                    if (f.isDirectory()) {
                        File[] kids = f.listFiles();
                        if (kids != null) {
                            java.util.Arrays.sort(kids);
                            for (File k : kids) {
                                String kn = k.getName().toLowerCase();
                                if (k.isFile() && (kn.endsWith(".mzml") || kn.endsWith(".mzxml") || kn.endsWith(".mgf"))) {
                                    spectra.add(k);
                                }
                            }
                        }
                    } else {
                        spectra.add(f);
                    }
                }
            } else if (a.equals("--tol") && i + 1 < args.length) {
                try { tol = Double.parseDouble(args[++i]); } catch (NumberFormatException ignored) {}
            } else if (a.equals("--tol-unit") && i + 1 < args.length) {
                tolUnit = args[++i];
            } else if (a.equals("--port") && i + 1 < args.length) {
                try { port = Integer.parseInt(args[++i]); } catch (NumberFormatException ignored) {}
            } else if (a.equals("--hide-psm-table")) {
                hidePsmTable = true;
            }
        }
        if (mzTab == null || spectra.isEmpty()) {
            System.err.println("Usage: java -jar PDV.jar denovo-gui --mztab <file.mztab> "
                    + "--spectrum <f1.mzML[,f2.mzML]> [--tol <value>] [--tol-unit Da|ppm] [--port <p>] [--hide-psm-table]");
            System.exit(1);
        }
        final File fMzTab = mzTab;
        final java.util.List<File> fSpectra = spectra;
        final double fTol = tol;
        final String fTolUnit = tolUnit;
        final int fPort = port;
        final boolean fHidePsmTable = hidePsmTable;
        javax.swing.SwingUtilities.invokeLater(() -> {
            PDVMainClass pdv = new PDVMainClass(true);
            // Open at a reasonable windowed size (not maximized) for the CasanovoGUI-driven viewer.
            pdv.setExtendedState(JFrame.NORMAL);
            pdv.setSize(1000, 600);
            pdv.setLocationRelativeTo(null);
            pdv.setVisible(true);
            // Collapse the PSM table BEFORE the result loads, so the spectrum always paints at full
            // height. With the table shown in a small window the spectrum area is a thin strip, and
            // compomics' GraphicsPanel axis-tag layout can hang there, wedging the EDT — so hide up
            // front rather than after load.
            if (fHidePsmTable) {
                pdv.applyHidePsmTable();
            }
            pdv.openDenovoFromCli(fSpectra, fMzTab, fTol, fTolUnit);
            // Start the control server off the EDT so binding the socket never perturbs the
            // initial table/spectrum paint timing (keeps default startup behaviour unchanged).
            if (fPort > 0) {
                new Thread(() -> pdv.startControlServer(fPort), "pdv-control-server").start();
            }
        });
    }

    /**
     * Programmatic equivalent of the DeNovo import dialog "Start" action for
     * mzTab (e.g. Casanovo) results: builds search parameters, loads the
     * spectrum file(s) (mgf or mzML) and shows the visualization panel. Mirrors
     * DeNovoImportDialog.startJButtonActionPerformed.
     */
    public void openDenovoFromCli(java.util.List<File> spectrumFiles, File mzTab, double fragTol, String tolUnit) {
        final String type = spectrumFiles.get(0).getName().toLowerCase().endsWith(".mgf") ? "mgf" : "mzml";
        new Thread("CliDenovoSetUp") {
            @Override
            public void run() {
                try {
                    int threads = Runtime.getRuntime().availableProcessors();

                    java.util.ArrayList<String> modification = ptmFactory.getPTMs();
                    PtmSettings ptmSettings = new PtmSettings();
                    for (String m : modification) {
                        ptmSettings.addFixedModification(ptmFactory.getPTM(m));
                    }
                    for (String m : modification) {
                        ptmSettings.addVariableModification(ptmFactory.getPTM(m));
                    }

                    boolean ppm = "ppm".equalsIgnoreCase(tolUnit);
                    SearchParameters searchParameters = new SearchParameters();
                    searchParameters.setPtmSettings(ptmSettings);
                    searchParameters.setFragmentAccuracyType(ppm
                            ? SearchParameters.MassAccuracyType.PPM
                            : SearchParameters.MassAccuracyType.DA);
                    searchParameters.setFragmentIonAccuracy(fragTol);

                    AnnotationSettings annotationPreferences = new AnnotationSettings();
                    annotationPreferences.setPreferencesFromSearchParameters(searchParameters);
                    annotationPreferences.setFragmentIonAccuracy(fragTol);
                    setAnnotationSettings(annotationPreferences);
                    setSearchParameters(searchParameters);
                    setFragmentAccuracyType(ppm ? MassAccuracyType.PPM : MassAccuracyType.DA);

                    Object spectrumsFileFactoryLocal;
                    if (type.equals("mgf")) {
                        SpectrumFactory spectrumFactoryLocal = SpectrumFactory.getInstance();
                        for (File sf : spectrumFiles) {
                            spectrumFactoryLocal.addSpectra(sf);
                        }
                        spectrumsFileFactoryLocal = spectrumFactoryLocal;
                    } else {
                        java.util.HashMap<String, ScanCollectionDefault> map = new java.util.HashMap<>();
                        for (File sf : spectrumFiles) {
                            MZMLFile mzmlFile = new MZMLFile(sf.getAbsolutePath());
                            ScanCollectionDefault scans = new ScanCollectionDefault();
                            scans.setDefaultStorageStrategy(StorageStrategy.SOFT);
                            scans.isAutoloadSpectra(true);
                            scans.setDataSource(mzmlFile);
                            mzmlFile.setNumThreadsForParsing(threads);
                            scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA);
                            map.put(sf.getName(), scans);
                        }
                        spectrumsFileFactoryLocal = map;
                    }

                    importMztabResults(spectrumsFileFactoryLocal, mzTab, type);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Start the optional loopback HTTP control server that lets an external launcher (e.g.
     * CasanovoGUI) drive this window via {@code /select?ref=<spectra_ref>}. Started only when
     * launched with {@code --port}; a failure here is logged and otherwise ignored so the GUI
     * still runs normally. The server is stopped when the window closes.
     *
     * @param port loopback TCP port to listen on
     */
    public void startControlServer(int port) {
        if (port < 1 || port > 65535) {
            System.err.println("Invalid PDV control port " + port + " (must be 1-65535); control server not started.");
            return;
        }
        try {
            pdvControlServer = new PdvControlServer(this, port);
            pdvControlServer.start();
            // Register the close hook on the EDT (this may run on a background starter thread).
            SwingUtilities.invokeLater(() -> addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    if (pdvControlServer != null) {
                        pdvControlServer.stop();
                    }
                }
            }));
            System.out.println("PDV control server listening on 127.0.0.1:" + port);
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not start PDV control server on port " + port + ": " + e.getMessage());
        }
    }

    /**
     * Flag that an mzTab result has finished importing. Called by the importer once
     * {@link #allSpectrumIndex} and {@link #spectraRefToSpectrumIndex} are fully populated.
     */
    public void setLoadComplete(boolean complete) {
        this.loadComplete = complete;
    }

    /** Uncheck the "PSM table" toggle and collapse it to a spectrum-only view (see {@code --hide-psm-table}). */
    private void applyHidePsmTable() {
        if (showPsmTableJCheckBox != null) {
            showPsmTableJCheckBox.setSelected(false);
        }
        setPsmTableVisible(false);
    }

    /**
     * @return true when an mzTab result is fully loaded and a {@code spectra_ref} select can be
     * served. Used by the control server's {@code /ready} endpoint.
     */
    public boolean isReadyForSelect() {
        return loadComplete
                && allSpectrumIndex != null && !allSpectrumIndex.isEmpty()
                && !spectraRefToSpectrumIndex.isEmpty()
                && spectrumJTable != null && spectrumJTable.getRowCount() > 0;
    }

    /**
     * Locate the PSM whose verbatim mzTab {@code spectra_ref} equals the argument, switch to its
     * page if needed, select its table row and render its annotated spectrum — i.e. the same effect
     * as the user clicking that row. Safe to call off the EDT (it marshals all Swing work onto it).
     *
     * @param spectraRef the verbatim spectra_ref string, exactly as it appears in the mzTab
     * @return {@link #SELECT_OK}, {@link #SELECT_NOT_FOUND} or {@link #SELECT_NOT_READY}
     */
    public int selectBySpectraRef(String spectraRef) {
        if (!isReadyForSelect()) {
            return SELECT_NOT_READY;
        }
        String key = spectraRefToSpectrumIndex.get(spectraRef);
        if (key == null) {
            return SELECT_NOT_FOUND;
        }
        // Find which page (1-based) holds this row key and its index within that page. A linear scan
        // of the current allSpectrumIndex is always correct even after sort/search rebuilds it; the
        // scan runs on the control-server thread, so snapshot the reference and tolerate a concurrent
        // rebuild (return NOT_FOUND, letting the caller retry) instead of throwing.
        int page = -1;
        int modelRow = -1;
        try {
            ArrayList<ArrayList<String>> pages = allSpectrumIndex;
            for (int p = 0; p < pages.size(); p++) {
                int r = pages.get(p).indexOf(key);
                if (r >= 0) {
                    page = p + 1;
                    modelRow = r;
                    break;
                }
            }
        } catch (RuntimeException raceWithRebuild) {
            return SELECT_NOT_FOUND;
        }
        if (page == -1) {
            return SELECT_NOT_FOUND;
        }
        final int fModelRow = modelRow;
        if (page == selectedPageNum) {
            SwingUtilities.invokeLater(() -> selectModelRowAndRender(fModelRow));
        } else {
            final int fPage = page;
            SwingUtilities.invokeLater(() -> {
                selectedPageNum = fPage;
                pageNumJTextField.setText(selectedPageNum + "/" + allSpectrumIndex.size());
                // Keep the "select all this page" checkbox in sync, like the manual page-nav handlers.
                allSelectedJCheckBox.setSelected(pageToSelected.contains(selectedPageNum));
                buttonCheck();
                // Reload the page, then select the target row once the new page is in the model.
                updateTable(() -> selectModelRowAndRender(fModelRow));
            });
        }
        return SELECT_OK;
    }

    /**
     * EDT-only: select {@code modelRow} of the current page (mapping through the row sorter) and
     * render its spectrum by reusing the existing programmatic render hook.
     */
    private void selectModelRowAndRender(int modelRow) {
        if (modelRow < 0 || modelRow >= spectrumJTable.getRowCount()) {
            return;
        }
        int viewRow = spectrumJTable.convertRowIndexToView(modelRow);
        if (viewRow < 0) {
            // The row is filtered out of the current view; don't fall back to selecting a different
            // (wrong) row. Currently unreachable — no RowFilter is installed — but safe if one is added.
            return;
        }
        spectrumJTable.requestFocus();
        spectrumJTable.setRowSelectionInterval(viewRow, viewRow);
        spectrumJTable.scrollRectToVisible(spectrumJTable.getCellRect(viewRow, 0, true));
        // Same call updateTable() uses to render the selected row (handles the evt == null case).
        spectrumJTableMouseReleased(null);
        toFront();
    }

    /**
     * Empty constructor
     */
    private PDVMainClass(){}

    /**
     * Main constructor
     * @param newStart Symbol show newStart
     */
    public PDVMainClass(String newStart){


        setUpLogFile();

        String user_preferences_folder = System.getProperty("user.home") + "/.pdv/";
        UtilitiesUserPreferences.setUserPreferencesFolder(user_preferences_folder);

        spectrumFactory = SpectrumFactory.getInstance();
        loadUserPreferences();

        if (!getJarFilePath().equalsIgnoreCase(".")) {
            UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
        }

        System.out.println("The local country is "+Locale.getDefault().getCountry());

        spectrumMainPanel = new SpectrumMainPanel(this);

        initComponents();

        checkSpectrumJTextField.setText("No Selected");

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        new PDVStart(this);
    }

    /**
     * Main constructor
     * @param restart Symbol show restart
     */
    public PDVMainClass(Boolean restart){

        spectrumFactory = SpectrumFactory.getInstance();
        loadUserPreferences();

        spectrumMainPanel = new SpectrumMainPanel(this);

        initComponents();

        checkSpectrumJTextField.setText("No Selected");

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setUpTableHeaderToolTips();

    }

    /**
     * Reads the application version from project.properties, which Maven populates
     * from pom.xml (${project.version}) via resource filtering at build time.
     * @return the version string
     */
    private static String readVersion(){
        Properties properties = new Properties();
        try (InputStream inputStream = PDVMainClass.class.getClassLoader().getResourceAsStream("project.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties.getProperty("version", "dev");
    }

    /**
     * Get JarFile path
     * @return path
     */
    public static String getJarFilePath(){
        try {
            // Resolve this class's code source location (the jar when packaged, or the
            // compiled-classes directory when run from an IDE) independently of the
            // process working directory, so the bundled resources load no matter which
            // folder the jar is launched from. new File(URI) handles URL decoding and
            // Windows drive letters correctly.
            File codeSource = new File(PDVMainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            if (codeSource.isFile()) { // packaged jar: the resources folder sits beside it
                return codeSource.getParentFile().getAbsolutePath();
            }

            // Run from compiled classes (IDE): resources are resolved against the working directory.
            return ".";
        } catch (Exception e) {
            e.printStackTrace();
            return ".";
        }
    }

    /**
     * Set spectrum table tooltips
     */
    private void setUpTableHeaderToolTips() {
        spectrumJTableToolTips = new ArrayList<>();
        spectrumJTableToolTips.add("Select it and output");
        spectrumJTableToolTips.add("Row number (current page)");
        spectrumJTableToolTips.add("Key of spectrum in Identification File");
        spectrumJTableToolTips.add("Retention Time");
        spectrumJTableToolTips.add("Spectrum Title in Spectrum File");
        spectrumJTableToolTips.add("Peptide Sequence");
        spectrumJTableToolTips.add("Precursor Charge");
        spectrumJTableToolTips.add("Precursor m/z");
        spectrumJTableToolTips.add("MS2 Mass Error");
        spectrumJTableToolTips.add("Next Rank Assumption");
        spectrumJTableToolTips.addAll(scoreName);
    }

    /**
     * Set spectrum table properties
     */
    private void setTableProperties(){
        spectrumJTable.setAutoCreateRowSorter(true);

        TableColumn indexColumn = spectrumJTable.getColumn(" ");
        indexColumn.setMaxWidth(40);
        indexColumn.setMinWidth(40);
        // Keep the identifier " " (used by getColumn(" ") lookups) but show "#" as the
        // header so the row-index column is labelled instead of appearing empty.
        indexColumn.setIdentifier(" ");
        indexColumn.setHeaderValue("#");

        TableColumn selectedColumn = spectrumJTable.getColumn("Selected");
        selectedColumn.setMinWidth(30);
        selectedColumn.setMaxWidth(30);
        // Keep the identifier "Selected" (used by getColumn("Selected") lookups) but show a
        // compact check mark glyph as the header so it is not truncated to "S..." in 30px.
        selectedColumn.setIdentifier("Selected");
        selectedColumn.setHeaderValue("✓");

        // These columns hold short values, so auto-resize squeezes them narrower than
        // their header; widen them so "RT (min)" and "Charge" are not truncated.
        fitColumnWidthToHeader("RT (min)");
        fitColumnWidthToHeader("Charge");

        spectrumJTable.setDefaultRenderer(Double.class, new DefaultTableCellRenderer(){
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){

                Object display = value;

                if (value instanceof Double){
                    display = formatTableNumber((Double) value);
                }

                return super.getTableCellRendererComponent(table, display, isSelected, hasFocus, row, column);
            }
        });

        ((DefaultTableCellRenderer)spectrumJTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

    }

    /**
     * Widens a column so its full header text stays visible (with room for the sort
     * arrow) instead of being truncated when the table auto-resizes its columns.
     * @param identifier the column identifier
     */
    private void fitColumnWidthToHeader(Object identifier){
        TableColumn column = spectrumJTable.getColumn(identifier);
        JTableHeader header = spectrumJTable.getTableHeader();
        // Measure with the actual (bold) header font rather than the table's cell font,
        // otherwise the header text is under-measured and still clips.
        java.awt.FontMetrics fm = header.getFontMetrics(header.getFont());
        int textWidth = fm.stringWidth(String.valueOf(column.getHeaderValue()));
        int width = textWidth + 30; // padding for the sort arrow and header insets
        column.setMinWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * Toggle between fitting columns to their content (with a horizontal scroll bar)
     * and fitting the table to the window.
     * @param evt Action event
     */
    private void fitColumnsJToggleButtonActionPerformed(ActionEvent evt){
        if (fitColumnsJToggleButton.isSelected()) {
            if (savedColumnWidths.isEmpty()) {
                saveColumnWidths();
            }
            spectrumJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            fitColumnsToContent();
        } else {
            spectrumJTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            restoreColumnWidths();
            savedColumnWidths.clear();
            spectrumJTable.revalidate();
            spectrumJTable.repaint();
        }
    }

    /**
     * Remembers the current min/preferred/max width of every column (keyed by model
     * index) so the fit-to-window layout can be restored later.
     */
    private void saveColumnWidths(){
        savedColumnWidths.clear();
        TableColumnModel columnModel = spectrumJTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            savedColumnWidths.put(column.getModelIndex(),
                    new int[]{column.getMinWidth(), column.getPreferredWidth(), column.getMaxWidth()});
        }
    }

    /**
     * Restores the column widths saved by {@link #saveColumnWidths()}.
     */
    private void restoreColumnWidths(){
        TableColumnModel columnModel = spectrumJTable.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            TableColumn column = columnModel.getColumn(i);
            int[] widths = savedColumnWidths.get(column.getModelIndex());
            if (widths == null) {
                continue;
            }
            column.setMinWidth(0);
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setPreferredWidth(widths[1]);
            column.setMinWidth(widths[0]);
            column.setMaxWidth(widths[2]);
        }
    }

    /**
     * Sizes each visible column to fit its header and a sample of its cell contents,
     * capped at {@link #MAX_FIT_COLUMN_WIDTH}. Hidden columns (max width 0) are left
     * collapsed.
     */
    private void fitColumnsToContent(){
        JTableHeader header = spectrumJTable.getTableHeader();
        java.awt.FontMetrics headerMetrics = header.getFontMetrics(header.getFont());
        TableColumnModel columnModel = spectrumJTable.getColumnModel();
        int sampleRows = Math.min(spectrumJTable.getRowCount(), FIT_SAMPLE_ROWS);

        for (int viewColumn = 0; viewColumn < columnModel.getColumnCount(); viewColumn++) {
            TableColumn column = columnModel.getColumn(viewColumn);

            if (column.getMaxWidth() == 0) {
                continue; // intentionally hidden column
            }

            int width = headerMetrics.stringWidth(String.valueOf(column.getHeaderValue())) + 30;

            for (int row = 0; row < sampleRows; row++) {
                TableCellRenderer renderer = spectrumJTable.getCellRenderer(row, viewColumn);
                Component cell = spectrumJTable.prepareRenderer(renderer, row, viewColumn);
                width = Math.max(width, cell.getPreferredSize().width + 6);
            }

            width = Math.min(width, MAX_FIT_COLUMN_WIDTH);

            column.setMinWidth(15);
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setPreferredWidth(width);
        }

        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     * Forces a control to a fixed height (keeping its preferred width) so the PSM
     * controls row lines up.
     * @param component the control to size
     * @param height the target height in pixels
     */
    private void matchRowControlHeight(JComponent component, int height){
        component.setPreferredSize(new Dimension(component.getPreferredSize().width, height));
        component.setMinimumSize(new Dimension(component.getMinimumSize().width, height));
        component.setMaximumSize(new Dimension(component.getMaximumSize().width, height));
    }

    /**
     * Render a combo box at a fixed compact {@code width} (so a long option cannot stretch the
     * toolbar) while still showing every option in full when the drop-down list is opened: the popup
     * is widened to the longest option's preferred width each time it appears.
     * @param combo  the combo box to pin
     * @param width  the fixed display width, in pixels
     * @param height the row height to match
     */
    private void fixComboWidthWidePopup(JComboBox combo, int width, int height){
        Dimension fixed = new Dimension(width, height);
        combo.setPreferredSize(fixed);
        combo.setMinimumSize(fixed);
        combo.setMaximumSize(fixed);
        combo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                javax.accessibility.Accessible child = combo.getUI().getAccessibleChild(combo, 0);
                if (!(child instanceof javax.swing.plaf.basic.ComboPopup)) {
                    return;
                }
                JList<?> list = ((javax.swing.plaf.basic.ComboPopup) child).getList();
                Container scroll = SwingUtilities.getAncestorOfClass(JScrollPane.class, list);
                if (scroll instanceof JScrollPane) {
                    int want = list.getPreferredSize().width + 24; // content + scrollbar/padding
                    int w = Math.max(want, combo.getWidth());
                    scroll.setPreferredSize(new Dimension(w, ((JScrollPane) scroll).getPreferredSize().height));
                }
            }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
        });
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JMenuBar menuBar = new JMenuBar();
        allJSplitPane = new JSplitPane();
        JScrollPane psmsScrollPane = new JScrollPane();
        JPopupMenu.Separator jSeparator1 = new JPopupMenu.Separator();
        JPopupMenu.Separator jSeparator2 = new JPopupMenu.Separator();
        JMenuItem homeJMenuItem = new JMenuItem();
        JMenuItem newJMenuItem = new JMenuItem();
        JMenuItem columnSelectionJMenuItem = new JMenuItem();
        JMenuItem openDenovoJMenuItem = new JMenuItem();
        JMenuItem exportAllMenuItem = new JMenuItem();
        JMenuItem exportSelectedJMenuItem = new JMenuItem();
        JMenuItem exportAllReportsJMenuItem = new JMenuItem();
        JMenuItem exportSelectedReportsJMenuItem = new JMenuItem();
        JMenuItem openSingleMenuItem = new JMenuItem();
        JMenuItem idenInforMenuItem = new JMenuItem();
        JMenuItem exitJMenuItem = new JMenuItem();
        JButton backJButton = new JButton();
        JButton setButton = new JButton();
        JMenu exportJMenu = new JMenu();
        JMenu fileJMenu = new JMenu();
        JMenu viewMenu = new JMenu();
        JPanel mainJPanel = new JPanel();
        JPanel sideJPanel = new JPanel();
        JPanel settingJPanel = new JPanel();
        JPanel loadingJPanel = new JPanel();
        JPanel searchJPanel = new JPanel();
        JLabel allSelectedJLabel = new JLabel();
        JLabel splitJLabel1 = new JLabel(" | ");
        JLabel splitJLabel2 = new JLabel("  ");
        JLabel splitJLabel3 = new JLabel(" | ");
        JLabel splitJLabel4 = new JLabel(" | ");
        JLabel fragmentIonAccuracyJLabel = new JLabel("Fragment m/z Tolerance: ");
        JLabel fragmentIonType1Lbl = new JLabel("Unit: ");
        JLabel sortJLabel = new JLabel("Sort: ");

        msAndTableJSplitPane = new JSplitPane();
        settingColorJButton = new JButton();
        upSortJButton = new JButton();
        downSortJButton = new JButton();
        detailsJPanel = new JPanel();
        allSelectedJCheckBox = new JCheckBox();
        openSidebarJButton = new JButton();
        loadingJButton = new JButton();
        fragmentIonAccuracyTxt = new JTextField();
        precursorIonUnit = new JComboBox();
        sortColumnJCombox = new JComboBox();
        nextJButton = new JButton();
        upJButton = new JButton();
        openSearchFileJButton = new JButton();
        pageNumJTextField = new JTextField();
        pageSelectNumJTextField = new JTextField();
        searchItemTextField = new JTextField();
        checkSpectrumJTextField = new JTextField();
        searchButton = new JButton();
        searchTypeComboBox = new JComboBox();
        decimalPlacesJComboBox = new JComboBox();
        fitColumnsJToggleButton = new JToggleButton();
        psmsJPanel = new JPanel();
        spectrumShowJPanel = new JPanel();
        backgroundPanel = new JPanel();
        searchTextOrButtonJPanel = new JPanel();

        spectrumJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return spectrumJTableToolTips.get(realIndex);}

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                return PDVTableStyle.applyRowStyle(this, super.prepareRenderer(renderer, row, column), row, column);
            }
        };

        PDVTableStyle.applyDefaults(spectrumJTable);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(760, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        sideJPanel.setBackground(new Color(217, 248, 255));
        sideJPanel.setMinimumSize(new Dimension(20, 0));
        sideJPanel.setOpaque(false);

        sideJPanel.setLayout(new BoxLayout(sideJPanel, BoxLayout.Y_AXIS));

        openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/fold.png")));
        openSidebarJButton.setToolTipText("Fold the side tree.");
        openSidebarJButton.setBorder(null);
        openSidebarJButton.setBorderPainted(false);
        openSidebarJButton.setContentAreaFilled(false);
        openSidebarJButton.setSelected(true);
        openSidebarJButton.addActionListener(this::openSidebarJButtonActionPerform);

        sideJPanel.add(openSidebarJButton);

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        homeJMenuItem.setMnemonic('H');
        homeJMenuItem.setText("Home");
        homeJMenuItem.addActionListener(this::homeJMenuItemActionPerformed);

        newJMenuItem.setMnemonic('B');
        newJMenuItem.setText("Open dataBase Result");
        newJMenuItem.addActionListener(this::newJMenuItemActionPerformed);

        //fileJMenu.add(newJMenuItem);

        openDenovoJMenuItem.setMnemonic('D');
        openDenovoJMenuItem.setText("Open DeNovo Result");
        openDenovoJMenuItem.addActionListener(this::openDenovoJMenuItemActionPerformed);

        //fileJMenu.add(openDenovoJMenuItem);

        openSingleMenuItem.setMnemonic('S');
        openSingleMenuItem.setText("Open Single Spectrum");
        openSingleMenuItem.addActionListener(this::openSingleMenuItemActionPerformed);

        //fileJMenu.add(openSingleMenuItem);

        fileJMenu.add(jSeparator1);

        idenInforMenuItem.setMnemonic('I');
        idenInforMenuItem.setText("Identification Details");
        idenInforMenuItem.addActionListener(this::idenInforMenuItemActionPerformed);

        fileJMenu.add(idenInforMenuItem);

        fileJMenu.add(jSeparator2);

        exitJMenuItem.setMnemonic('E');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);

        fileJMenu.add(exitJMenuItem);

        menuBar.add(fileJMenu);

        exportJMenu.setMnemonic('x');
        exportJMenu.setText("Export");

        exportAllMenuItem.setMnemonic('A');
        exportAllMenuItem.setText("Export All Spectra");
        exportAllMenuItem.addActionListener(this::exportAllMenuItemActionPerformed);
        exportJMenu.add(exportAllMenuItem);

        exportSelectedJMenuItem.setMnemonic('S');
        exportSelectedJMenuItem.setText("Export Selected Spectra");
        exportSelectedJMenuItem.addActionListener(this::exportSelectedJMenuItemActionPerformed);
        exportJMenu.add(exportSelectedJMenuItem);

        exportAllReportsJMenuItem.setMnemonic('A');
        exportAllReportsJMenuItem.setText("Export Reports(All)");
        exportAllReportsJMenuItem.setToolTipText("Export all results' expected reports.");
        exportAllReportsJMenuItem.addActionListener(this::exportAllReportsJMenuItemActionPerformed);
        exportJMenu.add(exportAllReportsJMenuItem);

        exportSelectedReportsJMenuItem.setMnemonic('S');
        exportSelectedReportsJMenuItem.setText("Export Reports(Selected)");
        exportSelectedReportsJMenuItem.setToolTipText("Export selected results' expected reports.");
        exportSelectedReportsJMenuItem.addActionListener(this::exportSelectedReportsJMenuItemActionPerformed);
        exportJMenu.add(exportSelectedReportsJMenuItem);

        menuBar.add(exportJMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");

        columnSelectionJMenuItem.setMnemonic('S');
        columnSelectionJMenuItem.setText("Column Selection");
        columnSelectionJMenuItem.addActionListener(this::columnSelectionJMenuItemActionPerformed);
        viewMenu.add(columnSelectionJMenuItem);

        menuBar.add(viewMenu);

        searchJPanel.setOpaque(false);

        searchTypeComboBox.setModel(new DefaultComboBoxModel(this.searchType));
        searchTypeComboBox.addItemListener(this::searchTypeComboBoxMouseClicked);

        searchItemTextField.setEditable(true);
        searchItemTextField.setHorizontalAlignment(SwingConstants.CENTER);
        searchItemTextField.setToolTipText("Data read unfinished!");

        openSearchFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        openSearchFileJButton.setBorder(null);
        openSearchFileJButton.setBorderPainted(false);
        openSearchFileJButton.setContentAreaFilled(false);
        openSearchFileJButton.addActionListener(this::openSearchFileJButtonActionPerformed);

        searchButton.setBackground(Color.BLACK);
        searchButton.setFont(searchButton.getFont().deriveFont(searchButton.getFont().getStyle() | Font.BOLD));
        searchButton.setForeground(Color.BLACK);
        searchButton.setIcon(new ImageIcon(getClass().getResource("/icons/search.png")));
        searchButton.setBorder(null);
        searchButton.setBorderPainted(false);
        searchButton.setContentAreaFilled(false);
        searchButton.setToolTipText("Data read unfinished!");
        searchButton.addActionListener(this::searchButtonActionPerformed);

        backJButton.setBackground(Color.BLACK);
        backJButton.setFont(backJButton.getFont().deriveFont(backJButton.getFont().getStyle() | Font.BOLD));
        backJButton.setForeground(Color.BLACK);
        backJButton.setIcon(new ImageIcon(getClass().getResource("/icons/back.png")));
        backJButton.setBorder(null);
        backJButton.setBorderPainted(false);
        backJButton.setContentAreaFilled(false);
        backJButton.setToolTipText("Refresh");
        backJButton.addActionListener(this::backJButtonActionPerformed);

        searchTextOrButtonJPanel.setOpaque(false);

        GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
        searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

        searchTextOrButtonJPanelLayout.setHorizontalGroup(
                searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
        );

        searchTextOrButtonJPanelLayout.setVerticalGroup(
                searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(searchItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );

        searchTextOrButtonJPanel.add(searchItemTextField);

        GroupLayout searchJPanelLayout = new GroupLayout(searchJPanel);
        searchJPanel.setLayout(searchJPanelLayout);
        searchJPanelLayout.setHorizontalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox,150,150,GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchTextOrButtonJPanel,150, 250, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchButton,GroupLayout.PREFERRED_SIZE,50,GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        searchJPanelLayout.setVerticalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(searchTextOrButtonJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(searchButton, 10, 25, 35))
        );

        JPanel blankJPanel = new JPanel();

        blankJPanel.setOpaque(false);
        GroupLayout blankJPanelLayout = new GroupLayout(blankJPanel);
        blankJPanel.setLayout(blankJPanelLayout);

        blankJPanelLayout.setHorizontalGroup(
                blankJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(100,800,Short.MAX_VALUE)
        );

        blankJPanelLayout.setVerticalGroup(
                blankJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

        );

        setJMenuBar(menuBar);

        // When the window is restored down from maximized, fold the file-detail side
        // panel if it is still open, to keep the smaller window uncluttered.
        addWindowStateListener(evt -> {
            boolean wasMaximized = (evt.getOldState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            boolean isMaximized = (evt.getNewState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
            if (wasMaximized && !isMaximized && detailsJPanel.isVisible()) {
                foldDetailsPanel();
            }
        });

        fragmentIonAccuracyTxt.setHorizontalAlignment(SwingConstants.CENTER);
        fragmentIonAccuracyTxt.setMaximumSize(new Dimension(100, 20));
        fragmentIonAccuracyTxt.setMinimumSize(new Dimension(50, 20));

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        precursorIonUnit.setMaximumSize(new Dimension(50, 20));
        precursorIonUnit.setBackground(Color.WHITE);

        setButton.setIcon(new ImageIcon(getClass().getResource("/icons/update.png")));
        setButton.setBorder(null);
        setButton.setBorderPainted(false);
        setButton.setContentAreaFilled(false);
        setButton.setToolTipText("Set New setting");
        setButton.addActionListener(this::setButtonActionPerform);

        sortColumnJCombox.setModel(new DefaultComboBoxModel(new String[]{}));

        sortColumnJCombox.setMaximumSize(new Dimension(100, 20));
        sortColumnJCombox.setBackground(Color.WHITE);

        upSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/upSort.png")));
        upSortJButton.setBorder(null);
        upSortJButton.setBorderPainted(false);
        upSortJButton.setContentAreaFilled(false);
        upSortJButton.setToolTipText("Sort results");
        upSortJButton.addActionListener(this::upSortJButtonActionPerform);

        downSortJButton.setIcon(new ImageIcon(getClass().getResource("/icons/downSort.png")));
        downSortJButton.setBorder(null);
        downSortJButton.setBorderPainted(false);
        downSortJButton.setContentAreaFilled(false);
        downSortJButton.setToolTipText("Sort results");
        downSortJButton.addActionListener(this::downSortJButtonActionPerform);

        settingColorJButton.setIcon(new ImageIcon(getClass().getResource("/icons/color.png")));
        settingColorJButton.setBorder(null);
        settingColorJButton.setBorderPainted(false);
        settingColorJButton.setContentAreaFilled(false);
        settingColorJButton.setToolTipText("Set PTM colors");
        settingColorJButton.addActionListener(this::settingColorJButtonActionPerform);

        showPsmTableJCheckBox = new JCheckBox("PSM table", true);
        showPsmTableJCheckBox.setOpaque(false);
        showPsmTableJCheckBox.setFont(PDVFonts.of(Font.PLAIN, 12f));
        showPsmTableJCheckBox.setToolTipText("Show or hide the PSM table (hide for a spectrum-only view)");
        showPsmTableJCheckBox.addActionListener(e -> setPsmTableVisible(showPsmTableJCheckBox.isSelected()));

        settingJPanel.setBackground(new Color(255, 255, 255));
        settingJPanel.setMinimumSize(new Dimension(20, 0));
        settingJPanel.setOpaque(false);

        settingJPanel.setLayout(new BoxLayout(settingJPanel, BoxLayout.X_AXIS));

        fragmentIonAccuracyJLabel.setFont(PDVFonts.of(Font.PLAIN, 12f));
        fragmentIonType1Lbl.setFont(PDVFonts.of(Font.PLAIN, 12f));
        sortJLabel.setFont(PDVFonts.of(Font.PLAIN, 12f));

        settingJPanel.add(splitJLabel1);
        settingJPanel.add(fragmentIonAccuracyJLabel);
        settingJPanel.add(fragmentIonAccuracyTxt);
        settingJPanel.add(splitJLabel2);
        settingJPanel.add(precursorIonUnit);
        settingJPanel.add(setButton);
        settingJPanel.add(splitJLabel3);
        settingJPanel.add(sortJLabel);
        settingJPanel.add(sortColumnJCombox);
        settingJPanel.add(upSortJButton);
        settingJPanel.add(downSortJButton);
        settingJPanel.add(splitJLabel4);
        settingJPanel.add(settingColorJButton);
        settingJPanel.add(new JLabel(" | "));
        settingJPanel.add(showPsmTableJCheckBox);

        settingJPanel.add(blankJPanel);
        settingJPanel.add(searchJPanel);

        loadingJPanel.setBackground(new Color(217, 248, 255));
        loadingJPanel.setMinimumSize(new Dimension(20, 0));
        loadingJPanel.setOpaque(false);

        loadingJPanel.setLayout(new BoxLayout(loadingJPanel, BoxLayout.X_AXIS));

        JPanel blankJPanel2 = new JPanel();

        blankJPanel2.setOpaque(false);
        GroupLayout blankJPanelLayout2 = new GroupLayout(blankJPanel2);
        blankJPanel2.setLayout(blankJPanelLayout2);

        blankJPanelLayout2.setHorizontalGroup(
                blankJPanelLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(100,800,Short.MAX_VALUE)
        );

        blankJPanelLayout2.setVerticalGroup(
                blankJPanelLayout2.createParallelGroup(GroupLayout.Alignment.LEADING)

        );

        loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/loading.gif")));
        loadingJButton.setBorder(null);
        loadingJButton.setBorderPainted(false);
        loadingJButton.setContentAreaFilled(false);
        loadingJButton.setEnabled(false);
        loadingJButton.setFont(PDVFonts.of(Font.PLAIN, 12f));
        loadingJButton.setText("Result importing");

        movingStatusJLabel = new JLabel();
        movingStatusJLabel.setFont(PDVFonts.of(Font.PLAIN, 12f));
        movingStatusJLabel.setIcon(new ImageIcon(getClass().getResource("/icons/loading.gif")));
        movingStatusJLabel.setVisible(false);

        loadingJPanel.add(movingStatusJLabel); // bottom-left page-move status
        loadingJPanel.add(blankJPanel2);
        loadingJPanel.add(loadingJButton);

        allJSplitPane.setBorder(null);
        allJSplitPane.setDividerLocation(370);
        allJSplitPane.setDividerSize(5);
        allJSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        allJSplitPane.setResizeWeight(0.5);
        allJSplitPane.setOpaque(false);
        allJSplitPane.setContinuousLayout(true);

        allJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> { if (!adjustingPsmTable) { spectrumMainPanel.updateSpectrum(); } });

        spectrumJTable.setModel(new DatabaseTableModel());
        spectrumJTable.setOpaque(false);
        spectrumJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        spectrumJTable.addMouseListener(new MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                spectrumJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                spectrumJTableMouseReleased(evt);
            }
        });
        spectrumJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                spectrumJTableKeyReleased(evt);
            }
        });
        psmsScrollPane.setBackground(Color.WHITE);
        psmsScrollPane.setViewportView(spectrumJTable);
        psmsScrollPane.setOpaque(false);
        spectrumJTable.getAccessibleContext().setAccessibleName("spectrumJTable");

        psmsJPanel.setOpaque(false);
        psmsJPanel.setLayout(new BorderLayout(0, 4));
        psmsJPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));

        JLabel psmTableTitleJLabel = new JLabel("PSM Table");
        psmTableTitleJLabel.setFont(PDVFonts.of(Font.BOLD, 12f));

        allSelectedJLabel.setText("Whole page");
        allSelectedJLabel.setFont(PDVFonts.of(Font.PLAIN, 12f));
        allSelectedJLabel.setToolTipText("Select all spectrum in this page");
        allSelectedJCheckBox.setToolTipText("Select all spectrum in this page");
        allSelectedJCheckBox.setSelected(false);
        allSelectedJCheckBox.setOpaque(false);
        allSelectedJCheckBox.setBackground(Color.white);
        allSelectedJCheckBox.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent evt){
                allSelectedJCheckBoxMouseClicked(evt);
            }
        });

        pageNumJTextField.setEditable(false);
        pageNumJTextField.setOpaque(false);
        pageNumJTextField.setBackground(Color.white);
        pageNumJTextField.setText(String.valueOf(selectedPageNum)+"/"+String.valueOf(allSpectrumIndex.size()));
        pageNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        pageNumJTextField.setColumns(5);

        pageSelectNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        pageSelectNumJTextField.setColumns(4);
        pageSelectNumJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                pageSelectNumJTextFieldKeyReleased(evt);
            }
        });

        upJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_back.png")));
        upJButton.setBorder(null);
        upJButton.setBorderPainted(false);
        upJButton.setContentAreaFilled(false);
        buttonCheck();
        upJButton.addActionListener(this::upJButtonActionPerformed);

        nextJButton.setIcon(new ImageIcon(getClass().getResource("/icons/arrow_forward.png")));
        nextJButton.setBorder(null);
        nextJButton.setBorderPainted(false);
        nextJButton.setContentAreaFilled(false);
        buttonCheck();
        nextJButton.addActionListener(this::nextJButtonActionPerformed);

        decimalPlacesJComboBox.setModel(new DefaultComboBoxModel(new String[]{"all", "1", "2", "3", "4"}));
        decimalPlacesJComboBox.setSelectedIndex(tableDecimalPlaces < 0 ? 0 : tableDecimalPlaces);
        decimalPlacesJComboBox.setToolTipText("Decimal places shown for numeric columns");
        decimalPlacesJComboBox.setMaximumSize(new Dimension(55, 20));
        decimalPlacesJComboBox.addActionListener(this::decimalPlacesJComboBoxActionPerformed);

        JLabel decimalPlacesJLabel = new JLabel("Decimals");
        decimalPlacesJLabel.setFont(PDVFonts.of(Font.PLAIN, 12f));
        decimalPlacesJLabel.setToolTipText("Decimal places shown for numeric columns");

        fitColumnsJToggleButton.setText("Fit columns");
        fitColumnsJToggleButton.setFont(PDVFonts.of(Font.PLAIN, 12f));
        fitColumnsJToggleButton.setFocusPainted(false);
        fitColumnsJToggleButton.setSelected(true);
        fitColumnsJToggleButton.setToolTipText("Fit each column to its content (up to 400px) with a horizontal scroll bar; click again to fit the window");
        fitColumnsJToggleButton.addActionListener(this::fitColumnsJToggleButtonActionPerformed);

        JPanel psmControlsJPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        psmControlsJPanel.setOpaque(false);
        psmControlsJPanel.add(decimalPlacesJLabel);
        psmControlsJPanel.add(decimalPlacesJComboBox);
        psmControlsJPanel.add(fitColumnsJToggleButton);
        psmControlsJPanel.add(allSelectedJCheckBox);
        psmControlsJPanel.add(allSelectedJLabel);
        psmControlsJPanel.add(pageSelectNumJTextField);
        psmControlsJPanel.add(upJButton);
        psmControlsJPanel.add(nextJButton);
        psmControlsJPanel.add(pageNumJTextField);

        JPanel psmHeaderJPanel = new JPanel(new BorderLayout());
        psmHeaderJPanel.setOpaque(false);
        psmHeaderJPanel.add(psmTableTitleJLabel, BorderLayout.WEST);
        psmHeaderJPanel.add(psmControlsJPanel, BorderLayout.EAST);
        psmHeaderJPanel.setPreferredSize(new Dimension(10, 30));

        psmsJPanel.add(psmHeaderJPanel, BorderLayout.NORTH);
        psmsJPanel.add(psmsScrollPane, BorderLayout.CENTER);

        msAndTableJSplitPane.setBorder(null);
        msAndTableJSplitPane.setDividerLocation(0.2);
        msAndTableJSplitPane.setDividerSize(5);
        msAndTableJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        msAndTableJSplitPane.setResizeWeight(0.5);
        msAndTableJSplitPane.setOpaque(false);
        msAndTableJSplitPane.setContinuousLayout(true);

        detailsJPanel.setOpaque(false);
        detailsJPanel.setBackground(Color.WHITE);
        detailsJPanel.setLayout(new BorderLayout(0, 4));
        detailsJPanel.setBorder(BorderFactory.createEmptyBorder(2, 6, 4, 6));
        infoPanel.setOpaque(false);

        JLabel detailTitleJLabel = new JLabel("File detail");
        detailTitleJLabel.setFont(PDVFonts.of(Font.BOLD, 12f));

        JPanel detailHeaderJPanel = new JPanel(new BorderLayout());
        detailHeaderJPanel.setOpaque(false);
        detailHeaderJPanel.add(detailTitleJLabel, BorderLayout.WEST);
        detailHeaderJPanel.setPreferredSize(new Dimension(100, psmHeaderJPanel.getPreferredSize().height));

        detailsJPanel.add(detailHeaderJPanel, BorderLayout.NORTH);
        detailsJPanel.add(infoPanel, BorderLayout.CENTER);

        msAndTableJSplitPane.setLeftComponent(detailsJPanel);
        msAndTableJSplitPane.setRightComponent(psmsJPanel);

        allJSplitPane.setTopComponent(msAndTableJSplitPane);

        spectrumShowJPanel.setOpaque(false);
        spectrumMainPanel.setOpaque(false);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowJPanel);
        spectrumShowJPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        // min 0 (not DEFAULT_SIZE) so the spectrum panel can shrink to a narrow window
                        // rather than overflowing it and clipping the de novo strip/track on the right.
                        .addComponent(spectrumMainPanel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(spectrumMainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        allJSplitPane.setRightComponent(spectrumShowJPanel);

        JPanel addSideJPanel = new JPanel();
        addSideJPanel.setOpaque(false);

        GroupLayout addSideJPanelLayout = new GroupLayout(addSideJPanel);
        addSideJPanel.setLayout(addSideJPanelLayout);

        addSideJPanelLayout.setHorizontalGroup(
                addSideJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(addSideJPanelLayout.createSequentialGroup()
                                .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, 20, 20)
                                .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        addSideJPanelLayout.setVerticalGroup(
                addSideJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        GroupLayout overviewJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
                overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(overviewJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(settingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(addSideJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(loadingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
                overviewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(overviewJPanelLayout.createSequentialGroup()
                                .addComponent(settingJPanel, 25, 25, 25)
                                .addComponent(addSideJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(loadingJPanel, 17, 17, 17)
                                .addGap(2,3,5))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        PDVLookAndFeel.setup();
        SwingUtilities.updateComponentTreeUI(this);

        // Align the box controls in both control rows to a common height (the combo's), so
        // the buttons/fields line up while keeping their standard look.
        int rowControlHeight = decimalPlacesJComboBox.getPreferredSize().height;
        // PSM table controls row
        matchRowControlHeight(fitColumnsJToggleButton, rowControlHeight);
        matchRowControlHeight(pageSelectNumJTextField, rowControlHeight);
        matchRowControlHeight(pageNumJTextField, rowControlHeight);
        // Top settings / search toolbar row
        matchRowControlHeight(fragmentIonAccuracyTxt, rowControlHeight);
        // Tolerance unit: snug fixed width. Its auto-computed minimum left the box much wider than the
        // short "Da"/"ppm" options need (a large empty gap before the arrow); matchRowControlHeight
        // would have preserved that width, so pin a compact size instead.
        Dimension unitSize = new Dimension(72, rowControlHeight);
        precursorIonUnit.setPreferredSize(unitSize);
        precursorIonUnit.setMinimumSize(unitSize);
        precursorIonUnit.setMaximumSize(unitSize);
        // Sort combo: pin it to a fixed compact width. Its auto-computed minimum width (from the
        // longest score-name option) was overriding the 100px maximum, so BoxLayout stretched it
        // across the toolbar and pushed the PSM-table checkbox off the right edge. The drop-down
        // popup is widened to the full option text so longer options stay readable when it is open.
        fixComboWidthWidePopup(sortColumnJCombox, 110, rowControlHeight);
        matchRowControlHeight(searchTypeComboBox, rowControlHeight);
        matchRowControlHeight(searchItemTextField, rowControlHeight);

        pack();
    }

    /**
     * Next and up button check
     */
    public void buttonCheck(){

        if (selectedPageNum == 1){
            upJButton.setEnabled(false);
        }else {
            upJButton.setEnabled(true);
        }

        if(pageNumJTextField.getText().contains(String.valueOf(allSpectrumIndex.size())+"/") || pageNumJTextField.getText().split("/")[0].equals(pageNumJTextField.getText().split("/")[1])){
            nextJButton.setEnabled(false);
        }else {
            nextJButton.setEnabled(true);
        }
    }

    /**
     * Update the annotation setting
     * @param evt Mouse click event
     */
    private void setButtonActionPerform(ActionEvent evt){
        Double fragmentIonMZTolerance = 0.05;
        if(fragmentIonAccuracyTxt.getText() != "" && fragmentIonAccuracyTxt.getText() != null){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        }

        if(precursorIonUnit.getSelectedIndex() == 0){
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.DA);
        }else {
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);
        }
        searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);

        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        spectrumMainPanel.setFragmentIonAccuracy(searchParameters.getFragmentAccuracyType(), fragmentIonMZTolerance);
    }

    /**
     * upSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void upSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("UpSorting"){
            @Override
            public void run() {
                allSpectrumIndex = new ArrayList<>();

                String selectedItem = (String) sortColumnJCombox.getSelectedItem();

                Connection connection = sqliteConnection.getConnection();

                try {
                    Statement statement = connection.createStatement();

                    String selectOrder = "SELECT PSMIndex FROM SpectrumMatch ORDER BY "+selectedItem;

                    ResultSet rs1 = statement.executeQuery(selectOrder);

                    String spectrumKey;
                    ArrayList<String> each = new ArrayList<>();

                    Integer count = 0;

                    while (rs1.next()){

                        spectrumKey = rs1.getString(1);

                        each.add(spectrumKey);
                        if(count == 1000){
                            allSpectrumIndex.add(each);

                            each = new ArrayList<>();

                            count = 0;
                        }
                        count ++;
                    }

                    if(count != 0){
                        allSpectrumIndex.add(each);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                selectedPageNum = 1;

                pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                buttonCheck();

                updateTable();

                orderProgressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * downSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void downSortJButtonActionPerform(ActionEvent evt){

        ProgressDialogX orderProgressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        orderProgressDialog.setPrimaryProgressCounterIndeterminate(true);
        orderProgressDialog.setTitle("Sorting. Please Wait...");

        new Thread(() -> {
            try {
                orderProgressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "orderProgressDialog").start();

        new Thread("DownSorting") {
            @Override
            public void run() {

                allSpectrumIndex = new ArrayList<>();

                String selectedItem = (String) sortColumnJCombox.getSelectedItem();

                Connection connection = sqliteConnection.getConnection();

                try {
                    Statement statement = connection.createStatement();

                    String selectOrder = "SELECT PSMIndex FROM SpectrumMatch ORDER BY "+selectedItem + " DESC ";

                    ResultSet rs1 = statement.executeQuery(selectOrder);

                    String spectrumKey;
                    ArrayList<String> each = new ArrayList<>();

                    Integer count = 0;

                    while (rs1.next()){

                        spectrumKey = rs1.getString(1);

                        each.add(spectrumKey);
                        if(count == 1000){
                            allSpectrumIndex.add(each);

                            each = new ArrayList<>();

                            count = 0;
                        }
                        count ++;
                    }

                    if(count != 0){
                        allSpectrumIndex.add(each);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                selectedPageNum = 1;

                pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                buttonCheck();

                updateTable();

                orderProgressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Setting the modification color
     * @param evt Mouse click event
     */
    private void settingColorJButtonActionPerform(ActionEvent evt){
        new PTMColorDialog(this, ptmFactory, allModifications);
    }

    /**
     * Show or hide the PSM-table region (the top half of the main split). Hiding it collapses the
     * top so the spectrum fills the window (a spectrum-only view); showing it restores the previous
     * height. Default is shown, so startup behaviour is unchanged.
     *
     * @param visible whether the PSM table should be shown
     */
    private void setPsmTableVisible(boolean visible){
        if (allJSplitPane == null || msAndTableJSplitPane == null) {
            return;
        }
        // Suppress the divider-location listener (which re-renders the whole spectrum) while we move
        // the divider. Otherwise each move fires an expensive updateSpectrum() that re-lays-out the
        // split and nudges the divider again — a non-settling feedback loop that freezes the window,
        // most visibly after a manual window resize. Lay out synchronously here, then render once.
        adjustingPsmTable = true;
        try {
            if (visible) {
                msAndTableJSplitPane.setMinimumSize(null); // restore the computed minimum
                allJSplitPane.setResizeWeight(0.5);
                allJSplitPane.setDividerSize(5);
                // Show as a 40/60 split: the PSM table gets the top 40%, the spectrum the bottom 60%.
                // Keeping the spectrum at ~60% of the window (~310px at the 1000x600 de novo window)
                // also keeps it above the height where compomics' GraphicsPanel.findOptimalTagDistance
                // spins forever trying to fit Y-axis tags into a near-zero-height plot -- that spin
                // wedged the EDT and froze the window when the table was shown over a thin spectrum.
                int paneH = allJSplitPane.getHeight();
                if (paneH > 0) {
                    allJSplitPane.setDividerLocation((int) ((paneH - allJSplitPane.getDividerSize()) * 0.4));
                } else {
                    allJSplitPane.setDividerLocation(0.4);
                }
            } else {
                // Force the top region's minimum height to 0 so the divider can fully collapse AND stay
                // collapsed across window resizes. Without this, the next layout (e.g. a maximize /
                // restore) honours the table header's minimum size and snaps the header row back into view.
                msAndTableJSplitPane.setMinimumSize(new Dimension(0, 0));
                allJSplitPane.setResizeWeight(0.0);
                allJSplitPane.setDividerSize(0);
                allJSplitPane.setDividerLocation(0);
            }
            allJSplitPane.validate();
        } finally {
            adjustingPsmTable = false;
        }
        spectrumMainPanel.updateSpectrum();
        allJSplitPane.repaint();
    }

    /**
     * Import pepXml file
     * @param spectrumFile Spectrum file
     * @param spectrumsFileFactory Spectrum saving objective
     * @param pepXMLFile PepXML File
     * @param spectrumFileType Type of spectrum file
     */
    public void importFilePep(File spectrumFile, Object spectrumsFileFactory, File pepXMLFile, String spectrumFileType, HashMap<String, HashMap<String, Integer>> spectrumIdAndNumber) {

        isMaxQuant = false;
        isNewSoft = false;
        isPepXML = true;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if (spectrumFileType.equals("mzml")){

            scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;

        }else if(spectrumFileType.equals("mgf")){

            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

        } else if (spectrumFileType.equals("mzxml")){
            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }

        databasePath = pepXMLFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run(){

                PepXMLFileImport pepXMLFileImport;

                try {
                    pepXMLFileImport = new PepXMLFileImport(PDVMainClass.this, spectrumFile.getName(), pepXMLFile, getModificationMass(), spectrumFactory,
                            spectrumFileType, progressDialog, spectrumIdAndNumber);

                    sqliteConnection = pepXMLFileImport.getSqLiteConnection();
                    originalInfor = pepXMLFileImport.getOriginalInfor();
                    detailsList = pepXMLFileImport.getDetailsList();
                    allModifications = pepXMLFileImport.getAllModifications();
                    scoreName = pepXMLFileImport.getSoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("MassError");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException | IOException | XmlPullParserException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import mzIdentML file
     * @param spectrumsFileFactory Object saving spectrum
     * @param mzIdentMLFile mzIdentML file
     * @param mzIdentMLType Object saving mzIdentML
     * @param spectrumFileType spectrum file type
     */
    public void importMzID(Object spectrumsFileFactory, File mzIdentMLFile, MzIdentMLType mzIdentMLType, String spectrumFileType, HashMap<String, Integer> spectrumIdAndNumber){

        isMaxQuant = false;
        isNewSoft = false;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if (spectrumFileType.equals("mzml")){

            scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;

        }else if(spectrumFileType.equals("mgf")){

            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

        } else if (spectrumFileType.equals("mzxml")){
            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }

        databasePath = mzIdentMLFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                MzIDFileImport mzIDFileImport;

                try {
                    mzIDFileImport = new MzIDFileImport(PDVMainClass.this, mzIdentMLFile, mzIdentMLType,
                            spectrumFactory, spectrumFileType, progressDialog, spectrumIdAndNumber);

                    sqliteConnection = mzIDFileImport.getSqLiteConnection();
                    originalInfor = mzIDFileImport.getOriginalInfor();
                    detailsList = mzIDFileImport.getDetailsList();
                    scoreName = mzIDFileImport.getScoreName();
                    allModifications = mzIDFileImport.getAllModifications();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("MassError");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));
                    fragmentIonAccuracyTxt.setText("  "+String.valueOf(annotationSettings.getFragmentIonAccuracy())+"  ");

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import new soft results
     * @param spectrumFile Spectrum file
     * @param spectrumsFileFactory Spectrum factory
     * @param textFile Text Id file
     */
    public void importTextResults(File spectrumFile, Object spectrumsFileFactory, File textFile, String spectrumFileType) {

        this.isNewSoft = true;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        switch (spectrumFileType) {
            case "mzml":
                scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;
                break;

            case "mgf":
                spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
                break;

            case "mzxml":
                scans = (ScanCollectionDefault) spectrumsFileFactory;
                break;
        }

        databasePath = textFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                TextFileImport textFileImport;

                try {
                    textFileImport = new TextFileImport(PDVMainClass.this, textFile, spectrumFile, progressDialog);

                    sqliteConnection = textFileImport.getSqLiteConnection();
                    allModifications = textFileImport.getAllModifications();
                    originalInfor = new HashMap<>();

                    scoreName = textFileImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import mztab results
     * @param spectrumsFileFactory Spectrum factory
     * @param textFile Text Id file
     */
    public void importMztabResults(Object spectrumsFileFactory, File textFile, String spectrumFileType) {

        this.isNewSoft = false;
        this.isMztab = true;

        // Fresh import: drop any state from a previous result so the control server can't serve
        // stale spectra_ref -> row mappings or report ready before this import finishes.
        this.loadComplete = false;
        this.spectraRefToSpectrumIndex.clear();

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if ("mgf".equals(spectrumFileType)) {
            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
        } else {
            scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;
        }

        databasePath = textFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                MztabImport mztabImport;

                try {
                    mztabImport = new MztabImport(PDVMainClass.this, textFile, spectrumFileType, progressDialog);

                    sqliteConnection = mztabImport.getSqLiteConnection();
                    allModifications = mztabImport.getAllModifications();
                    detailsList = mztabImport.getDetailsList();
                    originalInfor = mztabImport.getOriginalInfor();

                    scoreName = mztabImport.getScoreName();

                    aaScoreColumn = null;
                    for (String eachColumn : scoreName) {
                        if (eachColumn.toLowerCase().contains("aa_score")) {
                            aaScoreColumn = eachColumn;
                            break;
                        }
                    }

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    columnToSelected = new HashMap<>();
                    for (String eachColumn: scoreName){
                        columnToSelected.put(eachColumn, false);
                    }
                    columnToSelected.put("Mass error", false);
                    columnToSelected.put("m/z", false);
                    columnToSelected.put("Other Assumption", false);

                    ArrayList<String> proteinShowNames = (ArrayList<String>) Stream.of("`PSM_ID`", "`search_engine_score[1]`", "`exp_mass_to_charge`", "calc_mass_to_charge",
                            "spectra_ref").collect(Collectors.toList());

                    for (String eachColumn: proteinShowNames){
                        if (scoreName.contains(eachColumn)){
                            columnToSelected.put(eachColumn, true);
                        }
                    }

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import pFind results
     * @param spectrumFile Spectrum file
     * @param spectrumsFileFactory Spectrum factory
     * @param cnfFile pFind configure file
     */
    public void importPFindResults(File spectrumFile, Object spectrumsFileFactory, File cnfFile, String spectrumFileType) {

        this.isNewSoft = true;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        switch (spectrumFileType) {
            case "mzml":

                scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;
                break;

            case "mgf":

                spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
                break;

            case "mzxml":

                scans = (ScanCollectionDefault) spectrumsFileFactory;
                break;
        }

        databasePath = cnfFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                PFindImport findImport;

                try {
                    findImport = new PFindImport(PDVMainClass.this, cnfFile, spectrumFile, progressDialog);

                    sqliteConnection = findImport.getSqLiteConnection();
                    allModifications = findImport.getAllModifications();
                    originalInfor = new HashMap<>();

                    scoreName = findImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import MSAmanda identification result file
     * @param spectrumFile
     * @param spectrumsFileFactory
     * @param textFile
     * @param spectrumFileType
     * @param spectrumIdAndNumber
     */
    public void importMSAmandaResults(File spectrumFile, Object spectrumsFileFactory, File textFile, String spectrumFileType, HashMap<String, Integer> spectrumIdAndNumber){

        this.isNewSoft = true;
        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        switch (spectrumFileType) {
            case "mzml":

                scansMap = (HashMap<String, ScanCollectionDefault>)  spectrumsFileFactory;
                break;

            case "mgf":

                spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
                break;

            case "mzxml":

                scans = (ScanCollectionDefault) spectrumsFileFactory;
                break;
        }

        databasePath = textFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                MSAmandaImport msAmandaImport;

                try {
                    msAmandaImport = new MSAmandaImport(PDVMainClass.this, textFile, spectrumFileType, progressDialog, spectrumIdAndNumber);

                    sqliteConnection = msAmandaImport.getSqLiteConnection();
                    allModifications = msAmandaImport.getAllModifications();
                    originalInfor = new HashMap<>();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("AmandaScore");
                    orderName.add("WeightedProbability");
                    scoreName = new ArrayList<>();
                    scoreName.add("AmandaScore");
                    scoreName.add("WeightedProbability");
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    /**
     * Import MSFragger result file
     * @param fileToType File to file type
     * @param idFile Result file
     */
    public void importFragPipe(HashMap<File, String> fileToType, File idFile, String spectrumFileType){

        this.isFrage = true;

        databasePath = idFile.getAbsolutePath()+".db";
        this.spectrumFileType = spectrumFileType;

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                FragePipeImport fragePipeImport;

                try {

                    fragePipeImport = new FragePipeImport(fileToType, idFile, progressDialog, spectrumFileType, PDVMainClass.this);

                    sqliteConnection = fragePipeImport.getSqLiteConnection();
                    allModifications = fragePipeImport.getAllModifications();

                    scoreName = fragePipeImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    ArrayList<String> columnName = new ArrayList<>();
                    columnName.addAll(scoreName);

                    columnToSelected = new HashMap<>();
                    for (String eachColumn: columnName){
                        columnToSelected.put(eachColumn, false);
                    }

                    ArrayList<String> showNames = (ArrayList<String>) Stream.of("SpectrumFile", "Peptide", "ModifiedPeptide", "Retention", "ObservedMass", "ObservedMZ", "CalculatedPeptideMass", "DeltaMass", "Expectation",
                            "Hyperscore", "Nextscore", "PeptideProphetProbability", "Protein", "Gene").collect(Collectors.toList());

                    for (String eachColumn: showNames){

                        if (scoreName.contains(eachColumn)){
                            columnToSelected.put(eachColumn, true);
                        }
                    }

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import Mascot dat file
     * @param datFile Result file
     */
    public void importDatFile(File datFile){

        this.isFrage = true;
        spectrumFileType = "mgf";

        databasePath = datFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                DATFileImport datFileImport;

                try {

                    datFileImport = new DATFileImport(datFile, PDVMainClass.this, progressDialog);

                    sqliteConnection = datFileImport.getSqLiteConnection();
                    allModifications = datFileImport.getAllModifications();

                    scoreName = datFileImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import InstaNovo CSV results.
     * @param spectrumsFileFactory Spectrum factory
     * @param resultFile InstaNovo CSV result file
     * @param spectrumFiles Spectrum files
     * @param spectrumFileType Spectrum file type
     */
    public void importInstaNovoResults(Object spectrumsFileFactory, File resultFile, ArrayList<File> spectrumFiles, String spectrumFileType) {

        this.isNewSoft = false;
        this.isMztab = false;
        this.isDenovo = false;
        this.isMaxQuant = "mgf".equals(spectrumFileType);
        this.isPepXML = false;
        this.isFrage = false;
        this.isMSAmanda = false;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if ("mgf".equals(spectrumFileType)) {
            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
        } else if ("mzml".equals(spectrumFileType)) {
            scansMap = (HashMap<String, ScanCollectionDefault>) spectrumsFileFactory;
        }

        databasePath = resultFile.getAbsolutePath() + ".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                InstaNovoImport instaNovoImport;

                try {
                    instaNovoImport = new InstaNovoImport(PDVMainClass.this, resultFile, spectrumFiles, spectrumsFileFactory, spectrumFileType, progressDialog);

                    sqliteConnection = instaNovoImport.getSqLiteConnection();
                    allModifications = instaNovoImport.getAllModifications();
                    originalInfor = new HashMap<>();
                    detailsList = new ArrayList<>();
                    scoreName = instaNovoImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    columnToSelected = new HashMap<>();
                    for (String eachColumn: scoreName){
                        columnToSelected.put(eachColumn, true);
                    }
                    columnToSelected.put("Other Assumption", false);

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                    instaNovoImport.importResults();

                } catch (SQLException | ClassNotFoundException | IOException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import deep novo results
     * @param resultFile Result file
     * @param spectrumFile spectrum file
     * @param spectrumFactory Object saving spectrum
     */
    public void importDeepNovoResults(File resultFile, File spectrumFile, SpectrumFactory spectrumFactory){
        this.spectrumFileType = "mgf";

        isMaxQuant = true;
        isNewSoft = false;
        isDenovo = true;

        this.spectrumsFileFactory = spectrumFactory;
        this.spectrumFactory =  spectrumFactory;

        databasePath = resultFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                DeepNovoImport deepNovoImport;

                try {

                    deepNovoImport = new DeepNovoImport(PDVMainClass.this, resultFile, spectrumFile, spectrumFactory, progressDialog);

                    sqliteConnection = deepNovoImport.getSqLiteConnection();
                    allModifications = deepNovoImport.getAllModifications();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("Sequence");
                    orderName.add("PositionScore");
                    scoreName = new ArrayList<>();
                    scoreName.add("PositionScore");
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import pNovo results
     * @param pNovoResultFile pNovoResultFile
     * @param spectrumFile spectrum file
     * @param spectrumFactory spectrum factory
     */
    public void importPNovoResults(File pNovoResultFile, File spectrumFile, SpectrumFactory spectrumFactory){

        this.spectrumFileType = "mgf";

        isMaxQuant = true;
        isNewSoft = false;
        isDenovo = true;

        File parentFolder = pNovoResultFile.getParentFile();
        File[] firstLayer = parentFolder.listFiles();
        File paramFile = null;
        File resultFile = null;

        for (File firstFile : firstLayer){
            if (firstFile.getName().equals("param")){
                File[] secondLayer = firstFile.listFiles();

                for (File secondFile : secondLayer){
                    if (secondFile.getName().equals("pNovo.param")){
                        paramFile = secondFile;
                    }
                }
            } else if (firstFile.getName().equals("result")){
                File[] secondLayer = firstFile.listFiles();

                for (File secondFile : secondLayer){
                    if (secondFile.getName().equals("results.res")){
                        resultFile = secondFile;
                    }
                }
            }
        }

        if (paramFile != null && resultFile != null){
            databasePath = resultFile.getAbsolutePath()+".db";

            ProgressDialogX progressDialog = new ProgressDialogX(this,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                    true);
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading Results. Please Wait...");

            new Thread(() -> {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException ignored) {
                }
            }, "ProgressDialog").start();
            File finalParamFile = paramFile;
            File finalResultFile = resultFile;
            new Thread("DisplayThread") {
                @Override
                public void run() {

                    PNovoImport pNovoImport;

                    try {

                        pNovoImport = new PNovoImport(PDVMainClass.this, finalParamFile, finalResultFile, spectrumFile, spectrumFactory, progressDialog);

                        sqliteConnection = pNovoImport.getSqLiteConnection();
                        allModifications = pNovoImport.getAllModifications();

                        ArrayList<String> orderName = new ArrayList<>();
                        orderName.add("PSMIndex");
                        orderName.add("Sequence");
                        orderName.add("Score");
                        scoreName = new ArrayList<>();
                        scoreName.add("Score");
                        setUpTableHeaderToolTips();

                        buttonCheck();

                        sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                    } catch (SQLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        } else {
            JOptionPane.showMessageDialog(
                    this, "Failed to parse pNovo result, please check your file.",
                    "Error Parsing File", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Import de novo results
     * @param resultFile Results file
     * @param spectrumFile Spectrum file
     * @param spectrumFactory Object saving spectrum
     */
    public void importDeNovoResults(File resultFile, File spectrumFile, SpectrumFactory spectrumFactory){
        this.spectrumFileType = "mgf";

        isMaxQuant = true;
        isNewSoft = false;
        isDenovo = true;

        this.spectrumsFileFactory = spectrumFactory;
        this.spectrumFactory =  spectrumFactory;

        databasePath = resultFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                DeNovoFileImport deNovoFileImport;

                try {
                    deNovoFileImport = new DeNovoFileImport(PDVMainClass.this, resultFile, spectrumFile, searchParameters, getModificationMass(), progressDialog);

                    sqliteConnection = deNovoFileImport.getSqLiteConnection();
                    allModifications = deNovoFileImport.getAllModifications();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("Sequence");
                    orderName.add("Score");
                    scoreName = new ArrayList<>();
                    scoreName.add("Score");
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import MaxQuant results
     * @param maxQuantResultPath MaxQUANT results path
     * @param existMGF Boolean
     */
    public void importMaxQuantResults(String maxQuantResultPath, Boolean existMGF, String paraXMLPath) {

        this.spectrumFileType = "mgf";

        isMaxQuant = true;
        isNewSoft = false;

        databasePath = maxQuantResultPath+"/combined.db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                MaxQuantFileImport maxQuantFileImport;

                try {

                    maxQuantFileImport = new MaxQuantFileImport(PDVMainClass.this, new File(maxQuantResultPath), paraXMLPath, progressDialog);

                    originalInfor = maxQuantFileImport.getOriginalInfor();
                    detailsList = maxQuantFileImport.getDetailsList();
                    scoreName = maxQuantFileImport.getScoreName();

                    if (existMGF) {
                        maxQuantFileImport.getAllSpectrumRT(existMGF);
                        maxQuantFileImport.addAllSpectrumFiles();

                    } else {
                        maxQuantFileImport.getAllSpectrumRT(existMGF);

                        maxQuantFileImport.generateNewMGFFile();
                    }

                    maxQuantFileImport.getAllModificationSites();
                    maxQuantFileImport.getModificationPeptides();

                    sqliteConnection = maxQuantFileImport.getSqLiteConnection();
                    allModifications = maxQuantFileImport.getAllModifications();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("MassError_ppm");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (IOException | SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import proBAM file
     * @param proBAMFile proBAM file
     * @param spectrumFile Spectrum file
     * @param spectrumFileType  Spectrum file type
     * @param spectrumsFileFactory Object saving spectrum
     */
    public void importProBAMFile(File proBAMFile, File spectrumFile, String spectrumFileType, Object spectrumsFileFactory){

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if (spectrumFileType.equals("mzml")){

            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }else if(spectrumFileType.equals("mgf")){

            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

        } else if (spectrumFileType.equals("mzxml")){
            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }

        databasePath = proBAMFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                ProBamFileImport proBamFileImport;

                try {
                    proBamFileImport = new ProBamFileImport(PDVMainClass.this, proBAMFile,  spectrumFile.getName(), spectrumFileType, spectrumFactory, progressDialog);

                    sqliteConnection = proBamFileImport.getSqLiteConnection();
                    allModifications = proBamFileImport.getAllModifications();
                    scoreName = proBamFileImport.getScoreName();
                    originalInfor = new HashMap<>();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("MassError");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    if(detailsJPanel.isVisible()){
                        openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

                        detailsJPanel.setVisible(false);

                        msAndTableJSplitPane.setDividerSize(0);

                        msAndTableJSplitPane.revalidate();
                        msAndTableJSplitPane.repaint();

                    }

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Import proBAM file
     * @param proBedFile proBed file
     * @param spectrumFileType  Spectrum file type
     * @param spectrumsFileFactory Object saving spectrum
     */
    public void importProBedFile(File proBedFile, String spectrumFileType, Object spectrumsFileFactory, MzIdentMLType mzIdentMLType, HashMap<String, Integer> spectrumIdAndNumber){

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if (spectrumFileType.equals("mzml")){

            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }else if(spectrumFileType.equals("mgf")){

            spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

        } else if (spectrumFileType.equals("mzxml")){
            scans = (ScanCollectionDefault) spectrumsFileFactory;

        }

        databasePath = proBedFile.getAbsolutePath()+".db";

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Results. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();
        new Thread("DisplayThread") {
            @Override
            public void run() {

                ProBedFileImport proBedFileImport;

                try {
                    proBedFileImport = new ProBedFileImport(PDVMainClass.this, spectrumFactory, proBedFile, mzIdentMLType, progressDialog, spectrumFileType, spectrumIdAndNumber);

                    sqliteConnection = proBedFileImport.getSqLiteConnection();
                    allModifications = proBedFileImport.getAllModifications();
                    scoreName = proBedFileImport.getScoreName();
                    originalInfor = new HashMap<>();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
                    orderName.add("MZ");
                    orderName.add("Sequence");
                    orderName.add("MassError");
                    orderName.addAll(scoreName);
                    setUpTableHeaderToolTips();

                    buttonCheck();

                    if(detailsJPanel.isVisible()){
                        openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

                        detailsJPanel.setVisible(false);

                        msAndTableJSplitPane.setDividerSize(0);

                        msAndTableJSplitPane.revalidate();
                        msAndTableJSplitPane.repaint();
                    }

                    sortColumnJCombox.setModel(new DefaultComboBoxModel(orderName.toArray()));

                } catch (SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Set spectrum object
     * @param spectrumFactory Object saving spectrum
     */
    public void setSpectrumFactory(SpectrumFactory spectrumFactory){
        this.spectrumsFileFactory = spectrumFactory;
        this.spectrumFactory = spectrumFactory;
    }

    /**
     * Set log file in resource folder
     */
    private void setUpLogFile() {

        if ( !PDVMainClass.getJarFilePath().equalsIgnoreCase(".")) {
            try {

                File logsFolder = new File(PDVMainClass.getJarFilePath() + FILE_SEPARATOR + "logs" +FILE_SEPARATOR);

                if(!logsFolder.exists()){
                    logsFolder.mkdir();
                }

                String path = PDVMainClass.getJarFilePath() + FILE_SEPARATOR + "logs" + FILE_SEPARATOR + "PDV.log";

                File file = new File(path);
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));

                if (!file.exists()) {
                    boolean fileCreated = file.createNewFile();

                    if (fileCreated) {
                        FileWriter w = new FileWriter(file);
                        BufferedWriter bw = new BufferedWriter(w);
                        bw.close();
                        w.close();
                    } else {
                        JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                                "Failed to create the log file.<br>"
                                        + "Please <a href=\"https://github.com/wenbostar/PDV\">contact the developers</a>."),
                                "File Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                System.err.println(System.getProperty("line.separator")  + new Date()
                        + ": PDV-" + VERSION);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "Failed to create the log file.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Display results
     */
    public void displayResult(){

        msAndTableJSplitPane.setDividerLocation(0.2);
        sortColumnJCombox.setEnabled(true);
        downSortJButton.setVisible(true);
        upSortJButton.setVisible(true);
        HashMap<String, ArrayList<String>> input = new HashMap<>();
        input.put("", detailsList);
        infoPanel.updateInfo(input);

        if (originalInfor.size() == 0){
            if(detailsJPanel.isVisible()){
                openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

                detailsJPanel.setVisible(false);

                msAndTableJSplitPane.setDividerSize(0);

                msAndTableJSplitPane.revalidate();
                msAndTableJSplitPane.repaint();

            }
        }

        if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.DA){

            precursorIonUnit.setSelectedIndex(0);

        } else if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM){

            precursorIonUnit.setSelectedIndex(1);

        }

        fragmentIonAccuracyTxt.setText("  "+ annotationSettings.getFragmentIonAccuracy() +"  ");

        databaseTableModel = new DatabaseTableModel(searchParameters, scoreName, spectrumsFileFactory, spectrumFileType, spectrumKeyToSelected, isNewSoft, isMaxQuant, isPepXML);
        spectrumJTable.setModel(databaseTableModel);

        updateTable();

        // Give the wide text columns a readable default width in both the mzTab and the
        // plain database paths (previously only set for mzTab, which left the Sequence
        // column squeezed to its default width when many score columns were present).
        spectrumJTable.getColumn("Title").setPreferredWidth(250);
        spectrumJTable.getColumn("Title").setMinWidth(10);
        spectrumJTable.getColumn("Title").setMaxWidth(500);
        spectrumJTable.getColumn("Sequence").setPreferredWidth(250);
        spectrumJTable.getColumn("Sequence").setMinWidth(100);
        spectrumJTable.getColumn("Sequence").setMaxWidth(500);

        if (isMztab) {
            String key = "Key";
            spectrumJTable.getColumn(key).setPreferredWidth(50);
            spectrumJTable.getColumn(key).setMinWidth(50);
            spectrumJTable.getColumn(key).setMaxWidth(50);
            key = "RT (min)";
            spectrumJTable.getColumn(key).setPreferredWidth(50);
            spectrumJTable.getColumn(key).setMinWidth(40);
            spectrumJTable.getColumn(key).setMaxWidth(100);
            fitColumnWidthToHeader("Charge");

            for (String newKey : columnToSelected.keySet()) {
                if (!columnToSelected.get(newKey)) {
                    spectrumJTable.getColumn(newKey).setMinWidth(0);
                    spectrumJTable.getColumn(newKey).setMaxWidth(0);
                } else {
                    spectrumJTable.getColumn(newKey).setPreferredWidth(150);
                    spectrumJTable.getColumn(newKey).setMinWidth(20);
                    spectrumJTable.getColumn(newKey).setMaxWidth(400);
                }
            }
        }

        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     * Display de novo results
     */
    public void displayDenovo(){
        msAndTableJSplitPane.setDividerLocation(0);
        sortColumnJCombox.setEnabled(true);
        downSortJButton.setVisible(true);
        upSortJButton.setVisible(true);

        if (originalInfor.size() == 0){
            if(detailsJPanel.isVisible()){
                openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

                detailsJPanel.setVisible(false);

                msAndTableJSplitPane.setDividerSize(0);

                msAndTableJSplitPane.revalidate();
                msAndTableJSplitPane.repaint();
            }
        }

        if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.DA){

            precursorIonUnit.setSelectedIndex(0);

        } else if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM){

            precursorIonUnit.setSelectedIndex(1);

        }

        fragmentIonAccuracyTxt.setText(String.valueOf(annotationSettings.getFragmentIonAccuracy()));
        deNovoTableModel = new DeNovoTableModel(searchParameters, spectrumFactory, spectrumKeyToSelected);
        spectrumJTable.setModel(deNovoTableModel);

        updateTable();
    }

    /**
     * Display frage results
     */
    public void displayFrage(){
        msAndTableJSplitPane.setDividerLocation(0);
        sortColumnJCombox.setEnabled(true);
        downSortJButton.setVisible(true);
        upSortJButton.setVisible(true);

        if (originalInfor.size() == 0){
            if(detailsJPanel.isVisible()){
                openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

                detailsJPanel.setVisible(false);

                msAndTableJSplitPane.setDividerSize(0);

                msAndTableJSplitPane.revalidate();
                msAndTableJSplitPane.repaint();
            }
        }

        if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.DA){

            precursorIonUnit.setSelectedIndex(0);

        } else if (searchParameters.getFragmentAccuracyType() == SearchParameters.MassAccuracyType.PPM){

            precursorIonUnit.setSelectedIndex(1);

        }

        fragmentIonAccuracyTxt.setText(String.valueOf(annotationSettings.getFragmentIonAccuracy()));
        frageTableModel = new FrageTableModel(searchParameters, spectrumKeyToSelected, scoreName);
        spectrumJTable.setModel(frageTableModel);

        ArrayList<String> columnName = new ArrayList<>();
        if (isDenovo) {
            columnName.add("#Peaks");
            columnName.add("N-Gap");
            columnName.add("C-Gap");
            columnName.add("Score");
        } else if(!isFrage){
            columnName.add("Other Assumption");
        }
        columnName.addAll(scoreName);
        if(columnToSelected == null){
            columnToSelected = new HashMap<>();
            for (String eachColumn: columnName){
                columnToSelected.put(eachColumn, true);
            }
        }

        updateTable();

        for (String key: columnToSelected.keySet()){
            if(!columnToSelected.get(key)){
                spectrumJTable.getColumn(key).setMinWidth(0);
                spectrumJTable.getColumn(key).setMaxWidth(0);
            } else {
                for (int i = 0; i < 2; i++) {
                    if (key.equals("Other Assumption")) {
                        spectrumJTable.getColumn(key).setPreferredWidth(200);
                        spectrumJTable.getColumn(key).setMinWidth(20);
                        spectrumJTable.getColumn(key).setMaxWidth(400);
                    } else {
                        spectrumJTable.getColumn(key).setPreferredWidth(70);
                        spectrumJTable.getColumn(key).setMinWidth(20);
                        spectrumJTable.getColumn(key).setMaxWidth(400);
                    }
                }
            }
        }
        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     * Show or hide the inline "Loading. Please Wait..." status in the bottom-left of the status row
     * (the same row as the import-result indicator), used while a table page is being (re)loaded
     * instead of a floating modal dialog. Safe to call from any thread.
     * @param show true to show the status, false to clear it
     */
    private void showMovingStatus(boolean show){
        SwingUtilities.invokeLater(() -> {
            if (movingStatusJLabel != null) {
                movingStatusJLabel.setText(show ? "Loading. Please Wait..." : "");
                movingStatusJLabel.setVisible(show);
            }
        });
    }

    /**
     * Update table
     */
    public void updateTable(){
        updateTable((Runnable) null);
    }

    /**
     * Update table, then run {@code onComplete} on the EDT once the new page's rows are in the
     * model. Used to select a specific row after switching pages; {@code onComplete} may be null,
     * in which case this behaves exactly as the no-arg {@link #updateTable()}.
     */
    public void updateTable(Runnable onComplete){

        // Show the "Moving" status inline at the bottom-left status row (the same row as the
        // import-result indicator) instead of a floating modal dialog, so a quick page reload is far
        // less intrusive.
        showMovingStatus(true);

        new Thread("Moving") {
            @Override
            public void run() {

                selectPageSpectrumIndex = allSpectrumIndex.get(selectedPageNum - 1);

                ArrayList<ArrayList<Object>> selectedItem = new ArrayList<>();

                spectrumJTable.removeAll();

                try {
                    if (isDenovo) {
                        for (String spectrumIndex : selectPageSpectrumIndex) {
                            try {
                                selectedItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                            } catch (Exception e) {
                                showMovingStatus(false);
                                e.printStackTrace();
                                break;
                            }
                        }

                        deNovoTableModel.updateTable(selectedItem, selectPageSpectrumIndex, spectrumKeyToSelected);

                    } else if (isFrage){

                        for (String spectrumIndex : selectPageSpectrumIndex) {

                            try {
                                selectedItem.add(sqliteConnection.getOneFrageSpectrumItem(spectrumIndex));
                            } catch (Exception e) {
                                showMovingStatus(false);
                                e.printStackTrace();
                                break;
                            }
                        }

                        frageTableModel.updateTabel(selectedItem, selectPageSpectrumIndex, spectrumKeyToSelected);
                    } else {
                        for (String spectrumIndex : selectPageSpectrumIndex) {
                            try {
                                selectedItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                            } catch (Exception e) {
                                showMovingStatus(false);
                                e.printStackTrace();
                                break;
                            }
                        }

                        databaseTableModel.updateTable(selectedItem, selectPageSpectrumIndex, spectrumKeyToSelected);
                    }
                }catch (Exception e){
                    showMovingStatus(false);
                    e.printStackTrace();
                }

                showMovingStatus(false);

                setTableProperties();

                // When a completion hook will select a specific row (e.g. a spectra_ref select), skip
                // the default row-0 selection + render to avoid flashing the wrong spectrum and doing a
                // redundant DB load. The no-arg updateTable() path (onComplete == null) is unchanged.
                if (onComplete == null) {
                    spectrumJTable.setRowSelectionInterval(0, 0);
                    spectrumJTableMouseReleased(null);
                }

                buttonCheck();

                ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
                spectrumJTable.repaint();

                psmsJPanel.repaint();

                // Re-fit columns to the new page's content while fit-to-content mode is on.
                if (fitColumnsJToggleButton != null && fitColumnsJToggleButton.isSelected()) {
                    SwingUtilities.invokeLater(() -> {
                        if (savedColumnWidths.isEmpty()) {
                            saveColumnWidths();
                        }
                        spectrumJTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                        fitColumnsToContent();
                    });
                }

                // Run the optional completion hook last, on the EDT, so callers (e.g. a
                // spectra_ref select) act on the fully-loaded page.
                if (onComplete != null) {
                    SwingUtilities.invokeLater(onComplete);
                }

            }
        }.start();
    }

    /**
     * Update table
     * @param searchIDs Search ids
     */
    private void updateTable(ArrayList<String> searchIDs){
        ArrayList<ArrayList<Object>> selectedItem = new ArrayList<>();

        spectrumJTable.removeAll();

        if (isDenovo){
            for (String spectrumIndex : searchIDs){
                try {
                    selectedItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                } catch (SQLException e) {
                    e.printStackTrace();
                    break;
                }
            }

            deNovoTableModel.updateTable(selectedItem, searchIDs, spectrumKeyToSelected);
        } else if (isFrage){
            for (String spectrumIndex : searchIDs) {
                try {
                    selectedItem.add(sqliteConnection.getOneFrageSpectrumItem(spectrumIndex));
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            frageTableModel.updateTabel(selectedItem, searchIDs, spectrumKeyToSelected);
        } else {
            for (String spectrumIndex : searchIDs){
                try {
                    selectedItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
                } catch (SQLException e) {
                    e.printStackTrace();
                    break;
                }
            }

            databaseTableModel.updateTable(selectedItem, searchIDs, spectrumKeyToSelected);
        }

        setTableProperties();

        spectrumJTable.setRowSelectionInterval(0,0);

        selectedPageNum = 1;

        pageNumJTextField.setText("1/1");

        buttonCheck();

        spectrumJTableMouseReleased(null);

        ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
        spectrumJTable.repaint();

        psmsJPanel.repaint();
    }

    /**
     * Clear all data before existing and restart
     */
    private void clearData() {

        if(sqliteConnection != null){
            sqliteConnection.closeConnection();
            File dbFile = new File(databasePath);
            if (dbFile.isFile() && dbFile.exists()) {
                dbFile.delete();
            }
        }

        selectedPageNum = 1;

        scoreName.clear();

        spectrumMainPanel.removeAll();
        upSortJButton.setEnabled(false);
        downSortJButton.setEnabled(false);
        upJButton.setEnabled(false);
        nextJButton.setEnabled(false);

        try {
            spectrumFactory.closeFiles();
            spectrumFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        searchButton.setEnabled(false);

        psmsJPanel.removeAll();
        spectrumShowJPanel.removeAll();
        detailsJPanel.removeAll();
        this.revalidate();
        this.repaint();
    }

    /**
     * openSidebarJButtonActionPerform
     * @param evt Mouse click action
     */
    private void openSidebarJButtonActionPerform(ActionEvent evt){

        if(detailsJPanel.isVisible()){
            foldDetailsPanel();
        } else {
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/fold.png")));

            detailsJPanel.setVisible(true);

            msAndTableJSplitPane.setDividerSize(3);

            msAndTableJSplitPane.setDividerLocation(0.2);

            msAndTableJSplitPane.revalidate();
            msAndTableJSplitPane.repaint();
        }
    }

    /**
     * Folds (hides) the file-detail side panel, mirroring the sidebar toggle's fold action.
     */
    private void foldDetailsPanel(){
        openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

        detailsJPanel.setVisible(false);

        msAndTableJSplitPane.setDividerSize(0);

        msAndTableJSplitPane.revalidate();
        msAndTableJSplitPane.repaint();
    }

    /**
     * Close the program when click exist
     * @param evt Window event
     */
    private void formWindowClosing(WindowEvent evt) {
        close();
    }

    /**
     * Close dialog
     */
    public void close() {
        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to give up "  + " and close PDV"+"?",
                "Close PDV",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {
            closePDV();
        } else if (value == JOptionPane.NO_OPTION) {
            //Nothing
        }
    }

    /**
     * Close the program
     */
    private void closePDV() {

        exceptionHandler.setIgnoreExceptions(true);

        ProgressDialogX progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        final PDVMainClass finalRef = this;

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(() -> {

            try {

                if (!progressDialog.isRunCanceled()) {
                    spectrumFactory.closeFiles();
                    TempFilesManager.deleteTempFolders();
                }

            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            } finally {
                progressDialog.setRunFinished();

                finalRef.setVisible(false);

                clearData();

                System.exit(0);
            }
        });
    }

    /**
     * Find button action
     * @param evt Mouse click event
     */
    private void searchButtonActionPerformed(ActionEvent evt){

        ProgressDialogX progressDialogX = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Searching. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "searchProgressDialog").start();

        new Thread("Searching") {
            @Override
            public void run() {

                try {

                    ArrayList<String> searchIDs = getFoundResult();

                    if (searchIDs.size() > 0) {

                        updateTable(searchIDs);

                        selectPageSpectrumIndex = searchIDs;

                        upJButton.setEnabled(false);
                        nextJButton.setEnabled(false);

                    } else {
                        searchItemTextField.setText("No Match!");
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    progressDialogX.setRunFinished();
                    JOptionPane.showMessageDialog(PDVMainClass.this, JOptionEditorPane.getJOptionEditorPane(
                            "Failed to Search.<br>"
                                    + "Please search again."),
                            "Search Error", JOptionPane.ERROR_MESSAGE);
                }

                progressDialogX.setRunFinished();
            }
        }.start();
    }

    private ArrayList<String> getFoundResult() throws SQLException {

        ArrayList<String> searchIDs = new ArrayList<>();

        if (Objects.equals(findType, "Spectrum (String)")) {

            if (!searchItemTextField.getText().equals("")) {
                String findItem = searchItemTextField.getText();
                searchIDs = sqliteConnection.getSelectedTitleIndex(findItem);
            }

        } else if(Objects.equals(findType, "Peptide (String)")) {

            if (!searchItemTextField.getText().equals("")) {
                String findItem = searchItemTextField.getText();
                searchIDs = sqliteConnection.getSelectedPeptideIndex(findItem.toUpperCase());
            }

        } else if(Objects.equals(findType, "Spectrum (File)")) {
            if (!searchItemTextField.getText().equals("") && !searchItemTextField.getText().equals("null")) {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectIndexFile));

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.equals("")) {
                            ArrayList<String> idInFile = sqliteConnection.getSelectedTitleIndex(line.trim());
                            searchIDs.addAll(idInFile);
                        }
                    }

                    if (searchIDs.size() == 0){
                        searchItemTextField.setText("No Match!");
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else if(Objects.equals(findType, "Peptide (File)")) {

            if (!searchItemTextField.getText().equals("") && !searchItemTextField.getText().equals("null")) {
                try {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(selectIndexFile));

                    String line;

                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.equals("")) {
                            ArrayList<String> idInFile = sqliteConnection.getSelectedPeptideIndex(line.trim());
                            searchIDs.addAll(idInFile);
                        }
                    }

                    if (searchIDs.size() == 0){
                        searchItemTextField.setText("No Match!");
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return searchIDs;
    }

    /**
     * backJButtonActionPerformed
     * @param evt Mouse click action
     */
    private void backJButtonActionPerformed(ActionEvent evt){
        allSpectrumIndex = new ArrayList<>();
        ArrayList<String> each = new ArrayList<>();
        Integer count = 0;

        searchItemTextField.setText("");

        try {
            for (String spectrumKey : sqliteConnection.getAllIndex()){
                each.add(spectrumKey);
                if(count == 1000){
                    allSpectrumIndex.add(each);

                    each = new ArrayList<>();

                    count = 0;
                }
                count ++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if(count != 0){
            allSpectrumIndex.add(each);
        }

        selectedPageNum = 1;

        pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

        buttonCheck();

        updateTable();
    }

    /**
     * PageSelectNumJTextField event
     *  @param evt Key click event
     */
    private void pageSelectNumJTextFieldKeyReleased(KeyEvent evt){

        int keyChar = evt.getKeyChar();
        if (evt.getKeyCode() == 10 || keyChar >= KeyEvent.VK_0 && keyChar <= KeyEvent.VK_9) {
            if (evt.getKeyCode() == 10) {
                if (!pageSelectNumJTextField.getText().equals("")) {
                    if (Integer.parseInt(pageSelectNumJTextField.getText()) > allSpectrumIndex.size() || Integer.parseInt(pageSelectNumJTextField.getText()) < 1) {
                        pageSelectNumJTextField.setBackground(Color.GRAY);
                    } else {
                        pageSelectNumJTextField.setBackground(Color.WHITE);
                        selectedPageNum = Integer.parseInt(pageSelectNumJTextField.getText());
                        pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));

                        if (pageToSelected.contains(selectedPageNum)){
                            allSelectedJCheckBox.setSelected(true);
                        } else {
                            allSelectedJCheckBox.setSelected(false);
                        }

                        buttonCheck();
                        updateTable();

                        spectrumJTable.requestFocus();
                        spectrumJTable.setRowSelectionInterval(0, 0);
                    }
                }
            }
        } else {
            pageSelectNumJTextField.setText("");
            evt.consume();
        }
    }

    /**
     * UpButtonAction
     * @param evt Mouse click event
     */
    private void upJButtonActionPerformed(ActionEvent evt){

        selectedPageNum--;
        pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));
        buttonCheck();

        if (pageToSelected.contains(selectedPageNum)){
            allSelectedJCheckBox.setSelected(true);
        } else {
            allSelectedJCheckBox.setSelected(false);
        }

        updateTable();

        spectrumJTable.requestFocus();
        spectrumJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * NextJButtonAction
     * @param evt Mouse click event
     */
    private void nextJButtonActionPerformed(ActionEvent evt){

        selectedPageNum++;
        pageNumJTextField.setText(String.valueOf(selectedPageNum) + "/" + String.valueOf(allSpectrumIndex.size()));
        buttonCheck();

        if (pageToSelected.contains(selectedPageNum)){
            allSelectedJCheckBox.setSelected(true);
        } else {
            allSelectedJCheckBox.setSelected(false);
        }

        updateTable();

        spectrumJTable.requestFocus();
        spectrumJTable.setRowSelectionInterval(0, 0);
    }

    /**
     * Update the number of decimal places shown for numeric PSM-table columns
     * @param evt Action event
     */
    private void decimalPlacesJComboBoxActionPerformed(ActionEvent evt){
        int selected = decimalPlacesJComboBox.getSelectedIndex();
        tableDecimalPlaces = (selected == 0) ? -1 : selected;
        spectrumJTable.repaint();
    }

    /**
     * Formats a numeric PSM-table value using the user-selected number of decimal
     * places. Whole numbers (e.g. count columns surfaced as integer scores stored
     * as doubles) are shown without any decimals. Very small or very large
     * magnitudes fall back to scientific notation so that tiny scores (e.g.
     * e-values) are not collapsed to zero.
     * @param value the cell value
     * @return the formatted text
     */
    private String formatTableNumber(double value){
        if (Double.isNaN(value) || Double.isInfinite(value)){
            return String.valueOf(value);
        }
        // Whole-number counts (including integer scores normalized to doubles) show
        // without decimals in every mode (including "all").
        if (value == Math.rint(value) && Math.abs(value) < 1e15){
            return String.valueOf((long) value);
        }
        // "all": show the original full-precision value.
        if (tableDecimalPlaces < 0){
            return String.valueOf(value);
        }
        double abs = Math.abs(value);
        // Use fixed notation, unless the value is so small that it would round to
        // zero at the chosen precision, or so large that fixed notation is unwieldy.
        if (abs >= 0.5 * Math.pow(10, -tableDecimalPlaces) && abs < 1e7){
            return String.format(java.util.Locale.US, "%." + tableDecimalPlaces + "f", value);
        }
        return String.format(java.util.Locale.US, "%." + tableDecimalPlaces + "e", value);
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseReleased(MouseEvent evt) {

        spectrumJTable.requestFocus();
        int row = spectrumJTable.getSelectedRow();
        int column = spectrumJTable.getSelectedColumn();
        //String

        if (row != -1) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            selectedPsmKey = (String) spectrumJTable.getValueAt(row, 2);

            try {
                spectrumMatch = sqliteConnection.getSpectrumMatch(selectedPsmKey);
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (isFrage){
                updateSpectrum(getFragSpectrum(selectedPsmKey), spectrumMatch);
            } else {
                updateSpectrum(getSpectrum(spectrumMatch), spectrumMatch);
            }

            if (column == spectrumJTable.getColumn("Selected").getModelIndex()) {
                if(!spectrumKeyToSelected.containsKey(selectedPsmKey)){
                    allSelections.add(selectedPsmKey);
                    spectrumKeyToSelected.put(selectedPsmKey, true);
                } else {
                    Boolean isSelected = spectrumKeyToSelected.get(selectedPsmKey);
                    if(isSelected){
                        spectrumKeyToSelected.put(selectedPsmKey, false);
                        allSelections = remove(allSelections, selectedPsmKey);
                    } else {
                        allSelections.add(selectedPsmKey);
                        spectrumKeyToSelected.put(selectedPsmKey, true);
                    }
                }
                spectrumJTable.revalidate();
                spectrumJTable.repaint();
            }

            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Key event
     */
    private void spectrumJTableKeyReleased(KeyEvent evt) {

        spectrumJTable.requestFocus();

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int row = spectrumJTable.getSelectedRow();

            if (row != -1) {
                selectedPsmKey = (String) spectrumJTable.getValueAt(row, 2);

                try {
                    spectrumMatch = sqliteConnection.getSpectrumMatch(selectedPsmKey);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                if (isFrage){
                    updateSpectrum(getFragSpectrum(selectedPsmKey), spectrumMatch);
                } else {
                    updateSpectrum(getSpectrum(spectrumMatch), spectrumMatch);
                }

            }
        }
    }

    /**
     * Selected whole page
     * @param evt Mouse click event
     */
    private void allSelectedJCheckBoxMouseClicked(MouseEvent evt) {
        if (allSelectedJCheckBox.isSelected()) {
            for (String spectrumKey : selectPageSpectrumIndex) {
                spectrumKeyToSelected.put(spectrumKey, true);
                if (!allSelections.contains(spectrumKey)){
                    allSelections.add(spectrumKey);
                }

                pageToSelected.add(selectedPageNum);
            }
        } else {

            for (String spectrumKey : selectPageSpectrumIndex) {
                spectrumKeyToSelected.put(spectrumKey, false);
                allSelections = remove(allSelections, spectrumKey);

                pageToSelected = remove(pageToSelected, selectedPageNum);
            }
        }
        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     * homeJMenuItemActionPerformed
     * @param evt Mosue click action
     */
    private void homeJMenuItemActionPerformed(ActionEvent evt){
        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to open new one ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            clearData();

            PDVMainClass pdvMainClass = new PDVMainClass(true);

            new PDVStart(pdvMainClass, this);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        }
    }

    /**
     * Open a new DB results frame
     * @param evt Mouse click event
     */
    private void newJMenuItemActionPerformed(ActionEvent evt){

        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to open new one ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            clearData();

            PDVMainClass pdvMainClass = new PDVMainClass(true);

            new DatabaseImportDialog(pdvMainClass, true, this);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        }
    }

    /**
     * Open new DeNovo results display
     * @param evt Mouse click event
     */
    private void openDenovoJMenuItemActionPerformed(ActionEvent evt){
        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to open De novo  result ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            clearData();

            PDVMainClass pdvMainClass = new PDVMainClass(true);

            new DeNovoImportDialog(pdvMainClass, true, this);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        }
    }

    /**
     * Open new Single spectrum display
     * @param evt Mouse click event
     */
    private void openSingleMenuItemActionPerformed(ActionEvent evt){
        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to open De novo  result ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            clearData();

            new SinglePeptideDisplay();

            this.setVisible(false);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        } else {
            clearData();

            new SinglePeptideDisplay();

            this.setVisible(false);
        }
    }

    /**
     * Open identification details information
     * @param evt Mouse click event
     */
    private void idenInforMenuItemActionPerformed(ActionEvent evt){
        new OriginalInforDialog(this, originalInfor);
    }

    /**
     * Exist the frame
     * @param evt Mouse click event
     */
    private void exitJMenuItemActionPerformed(ActionEvent evt){
        formWindowClosing(null);
    }

    /**
     * exportAllMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportAllMenuItemActionPerformed(ActionEvent evt){

        exportAll = true;
        exportSelection = false;

        Integer size = 0;
        for (ArrayList<String> each : allSpectrumIndex){
            size += each.size();
        }
        new ExportBatchDialog(this, size);
    }

    /**
     * exportSelectedJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportSelectedJMenuItemActionPerformed(ActionEvent evt){

        exportSelection = true;
        exportAll = false;

        new ExportBatchDialog(this, allSelections.size());
    }

    /**
     * exportAllReportsJMenuItemActionPerformed
     */
    private void exportAllReportsJMenuItemActionPerformed(ActionEvent evt){

        exportSelection = false;
        exportAll = true;

        exportSelectedReport();
    }

    /**
     * exportSelectedReportsJMenuItemActionPerformed
     */
    private void exportSelectedReportsJMenuItemActionPerformed(ActionEvent evt){

        exportSelection = true;
        exportAll = false;

        exportSelectedReport();
    }

    /**
     * Select which columns need display
     * @param evt Mouse click event
     */
    private void columnSelectionJMenuItemActionPerformed(ActionEvent evt){
        ArrayList<String> columnName = new ArrayList<>();
        if (isDenovo) {
            columnName.add("#Peaks");
            columnName.add("N-Gap");
            columnName.add("C-Gap");
            columnName.add("Score");
        } else if(!isFrage){
            columnName.add("Other Assumption");
        }
        columnName.addAll(scoreName);

        if(columnToSelected == null){
            columnToSelected = new HashMap<>();
            for (String eachColumn: columnName){
                columnToSelected.put(eachColumn, true);
            }
        }

        new ColumnSelectionDialog(this, spectrumJTable, columnName);
    }

    /**
     * Search type select clicked
     * @param evt Item event
     */
    private void searchTypeComboBoxMouseClicked(ItemEvent evt){

        searchButton.setEnabled(false);

        if (searchTypeComboBox.getSelectedIndex() == 0) {

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(searchItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            );
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            searchButton.setEnabled(true);
            searchItemTextField.setEditable(true);
            findType = "Peptide (String)";
        } else if (searchTypeComboBox.getSelectedIndex() == 1) {

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(searchItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            );
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            searchButton.setEnabled(true);
            searchItemTextField.setEditable(true);
            findType = "Spectrum (String)";
        } else if (searchTypeComboBox.getSelectedIndex() == 3) {
            findType = "Spectrum (File)";

            searchItemTextField.setText("");
            selectIndexFile = null;

            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(searchTextOrButtonJPanelLayout.createSequentialGroup()
                                    .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(openSearchFileJButton, 50, 50, GroupLayout.PREFERRED_SIZE))
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(openSearchFileJButton, 10, 20, 20)
                            .addComponent(searchItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            );
            searchItemTextField.setEditable(false);
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

        } else if (searchTypeComboBox.getSelectedIndex() == 2) {
            findType = "Peptide (File)";

            searchItemTextField.setText("");
            searchTextOrButtonJPanel.removeAll();

            GroupLayout searchTextOrButtonJPanelLayout = new GroupLayout(searchTextOrButtonJPanel);
            searchTextOrButtonJPanel.setLayout(searchTextOrButtonJPanelLayout);

            searchTextOrButtonJPanelLayout.setHorizontalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(searchTextOrButtonJPanelLayout.createSequentialGroup()
                                    .addComponent(searchItemTextField, 80, 200, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(openSearchFileJButton, 50, 50, GroupLayout.PREFERRED_SIZE))
            );

            searchTextOrButtonJPanelLayout.setVerticalGroup(
                    searchTextOrButtonJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                            .addComponent(openSearchFileJButton, 10, 20, 20)
                            .addComponent(searchItemTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            );
            searchItemTextField.setEditable(false);
            searchTextOrButtonJPanel.repaint();
            searchTextOrButtonJPanel.revalidate();

            selectIndexFile = null;
        }

    }

    private void openSearchFileJButtonActionPerformed(ActionEvent evt){

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Input File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(false);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            selectIndexFile = fileChooser.getSelectedFile();

        }

        if (selectIndexFile != null){
            searchItemTextField.setText(selectIndexFile.getName());
            lastSelectedFolder.setLastSelectedFolder(selectIndexFile.getAbsolutePath());
            searchButton.setEnabled(true);
        }

    }

    /**
     * Get current spectrum according to the selected spectrum key
     * @return MSnSpectrum
     */
    public MSnSpectrum getSpectrum(SpectrumMatch spectrumMatch) {

        String matchKey = spectrumMatch.getKey();

        String spectrumFileName = matchKey.split("_cus_")[0];

        String spectrumKey = matchKey.split("_cus_")[1];

        return getSpectrum(spectrumKey, spectrumFileName);
    }

    public MSnSpectrum getFragSpectrum(String selectedPsmKey){

        MSnSpectrum selectedSpectrum = null;

        try {
            selectedSpectrum = sqliteConnection.getFragSpectrum(selectedPsmKey);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return selectedSpectrum;
    }

    /**
     * Get spectrum
     * @param spectrumKey
     * @param spectrumFileName
     * @return
     */
    private MSnSpectrum getSpectrum(String spectrumKey, String spectrumFileName){

        if(spectrumFileType.equals("mgf")){

            String spectrumTitle = null;
            if(!isMaxQuant && !isNewSoft && !isPepXML){
                spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(spectrumKey)+1);

            } else if(isMaxQuant || isPepXML) {
                spectrumTitle = spectrumKey;
            } else if(isNewSoft){
                spectrumTitle = spectrumKey.split("_rank_")[0];
            }
            try {
                return (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);
            } catch (Exception e) {
                catchException(e);
                return null;
            }
        }else if(spectrumFileType.equals("mzml")){

            ScanCollectionDefault currentScans = scansMap.get(spectrumFileName);
            int scanKey = spectrumMatch.getSpectrumNumber();

            IScan iScan = currentScans.getScanByNum(scanKey);
            ISpectrum spectrum = iScan.getSpectrum();

            Charge charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(charge);
            IScan precursorScan = currentScans.getScanByNum(iScan.getPrecursor().getParentScanNum());
            Double precursorInt = iScan.getPrecursor().getIntensity();

            if (precursorInt == null){
                precursorInt = precursorScan.getBasePeakIntensity();
            }

            Precursor precursor = new Precursor(precursorScan.getRt(), iScan.getPrecursor().getMzTarget(),
                    precursorInt, charges);

            double[] mzs = spectrum.getMZs();
            double[] ins = spectrum.getIntensities();
            HashMap<Double, Peak> peakMap = new HashMap<>();
            for(int i = 0; i<mzs.length; i++){
                Peak peak = new Peak(mzs[i], ins[i]);
                peakMap.put(mzs[i], peak);
            }

            return new MSnSpectrum(2, precursor, spectrumKey, peakMap, spectrumFileName);

        } else if (spectrumFileType.equals("mzxml")){
            int scanKey = spectrumMatch.getSpectrumNumber();

            IScan iScan = scans.getScanByNum(scanKey);

            ISpectrum spectrum = iScan.getSpectrum();

            Charge charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
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

            return new MSnSpectrum(2, precursor, spectrumKey, peakMap, spectrumFileName);

        } else{
            return null;
        }

    }

    /**
     * Sets MS2 ion tolerance type
     * @param fragmentAccuracyType MS2 tolerance type
     */
    public void setFragmentAccuracyType(MassAccuracyType fragmentAccuracyType) {
        Boolean usePPM = true;
        if (fragmentAccuracyType == MassAccuracyType.DA){
            usePPM = false;
        }

        annotationSettings.setFragmentIonPpm(usePPM);
    }

    /**
     * MS2 ion tolerance type
     */
    public enum MassAccuracyType {

        PPM, DA;

        @Override
        public String toString() {
            switch (this) {
                case PPM:
                    return "ppm";
                case DA:
                    return "Da";
                default:
                    throw new UnsupportedOperationException("Name of tolerance type " + this.name() + " not implemented.");
            }
        }
    }

    /**
     * Update spectrum
     * @param mSnSpectrum MSN spectrm
     * @param spectrumMatch Spectrum match
     */
    private void updateSpectrum(MSnSpectrum mSnSpectrum, SpectrumMatch spectrumMatch) {

        String matchKey = spectrumMatch.getKey();
        String spectrumFileName = matchKey.split("_cus_")[0];

        String spectrumTitle = "";
        if(spectrumFileType.toLowerCase().equals("mgf")) {
            SpectrumFactory spectrumFactory = (SpectrumFactory) spectrumsFileFactory;
            if (isNewSoft) {
                String spectrumKey = matchKey.split("_cus_")[1];
                spectrumTitle = spectrumKey.split("_rank_")[0];
            } else if (isMaxQuant || isPepXML) {
                spectrumTitle = matchKey.split("_cus_")[1];
            } else if (isFrage) {
                spectrumTitle = matchKey;
            } else {
                String spectrumKey = matchKey.split("_cus_")[1];
                spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(spectrumKey) + 1);
            }
        }else if (spectrumFileType.toLowerCase().equals("mzml") || spectrumFileType.toLowerCase().equals("mzxml")) {
            if (isFrage) {
                spectrumTitle = matchKey;
            } else {
                spectrumTitle = spectrumMatch.getKey().split("_cus_")[1];
            }
        }

        String modSequence;
        SpectrumIdentificationAssumption spectrumIdentificationAssumption;

        if (spectrumMatch.getBestPeptideAssumption() != null) {
            spectrumIdentificationAssumption = spectrumMatch.getBestPeptideAssumption();
            modSequence = spectrumMatch.getBestPeptideAssumption().getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, false, false);

        } else if (spectrumMatch.getBestTagAssumption() != null) {
            spectrumIdentificationAssumption = spectrumMatch.getBestTagAssumption();
            modSequence = spectrumMatch.getBestTagAssumption().getTag().getTaggedModifiedSequence(searchParameters.getPtmSettings(), false, false, true, false);
        } else {
            throw new IllegalArgumentException("No best assumption found for spectrum " + ".");
        }

        TitledBorder titledBorder = BorderFactory.createTitledBorder(modSequence + " \t ");

        titledBorder.setTitleFont(PDVFonts.of(Font.PLAIN, 12f));

        spectrumShowJPanel.setBorder(titledBorder);
        mSnSpectrum.setSpectrumTitle(spectrumTitle);

        double[] aaScores = null;
        if (aaScoreColumn != null) {
            try {
                aaScores = parseAaScores(sqliteConnection.getColumnValue(aaScoreColumn, String.valueOf(selectedPsmKey)));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        spectrumMainPanel.setAaScores(aaScores);

        spectrumMainPanel.updateSpectrum(spectrumIdentificationAssumption, mSnSpectrum, String.valueOf(selectedPsmKey));

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
    }

    /**
     * Parse a comma-separated per-residue score string (e.g. Casanovo opt_global_aa_scores)
     * into an array of scores.
     * @param raw comma-separated scores; may be null/empty
     * @return the parsed scores, or null if absent or not parseable
     */
    private double[] parseAaScores(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] parts = raw.trim().split(",");
        double[] scores = new double[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                scores[i] = Double.parseDouble(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return scores;
    }

    /**
     * CatchException
     * @param e Exception
     */
    private void catchException(Exception e) {
        exceptionHandler.catchException(e);
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
        this.annotationSettings = spectrumMainPanel.getAnnotationSettings();
        return annotationSettings;
    }

    /**
     * Set search parameters
     * @param searchParameters Search parameters
     */
    public void setSearchParameters(SearchParameters searchParameters) {
        this.searchParameters = searchParameters;

        spectrumMainPanel.updateSearchParameters(searchParameters);

        backgroundPanel.revalidate();
        backgroundPanel.repaint();
    }

    /**
     * load userPreference
     */
    private void loadUserPreferences() {
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
     * Get Modification mass map
     * @return HashMap
     */
    public HashMap<String,HashMap<Double, String >> getModificationMass(){

        HashMap<String,HashMap<Double, String > > modificationMass = new HashMap<>();
        ArrayList<String> orderedModifications = ptmFactory.getPTMs();
        for (String  modificationName : orderedModifications){
            String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
            String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
            if(modificationMass.containsKey(aminoAcidName)){
                modificationMass.get(aminoAcidName).put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
            }else {
                HashMap<Double, String> singleModi = new HashMap<>();
                singleModi.put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
                modificationMass.put(aminoAcidName, singleModi);
            }
        }
        return modificationMass;
    }

    /**
     * Update ptmSetting
     */
    public void updatePTMSetting(){
        ArrayList<String> modification = ptmFactory.getPTMs();
        PtmSettings ptmSettings = new PtmSettings();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }

        searchParameters.setPtmSettings(ptmSettings);
    }

    /**
     * Get current spectrum according to the page num and spectrum key
     * @param selectedSpectrumKey spectrum key
     * @return MSnSpectrum
     */
    public MSnSpectrum getSpectrum(String selectedSpectrumKey) {
        try {
            SpectrumMatch selectedMatch = sqliteConnection.getSpectrumMatch(selectedSpectrumKey);

            if (selectedMatch.getBestPeptideAssumption() != null){
                spectrumIdentificationAssumption = selectedMatch.getBestPeptideAssumption();

            } else if (selectedMatch.getBestTagAssumption() != null){
                spectrumIdentificationAssumption = selectedMatch.getBestTagAssumption();
            } else {
                throw new IllegalArgumentException("No best assumption found for spectrum " + ".");
            }

            String matchKey = selectedMatch.getKey();

            String spectrumFileName = matchKey.split("_cus_")[0];

            String spectrumKey = matchKey.split("_cus_")[1];

            if (isFrage){
                return getFragSpectrum(selectedSpectrumKey);
            } else {
                return getSpectrum(spectrumKey, spectrumFileName);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get current identification assumption
     * @return SpectrumIdentificationAssumption
     */
    public SpectrumIdentificationAssumption getspectrumIdentificationAssumption(){
        return spectrumIdentificationAssumption;
    }

    /**
     * Export selected spectral
     * @param finalImageType Image type
     * @param outputFolder Output folder
     * @param picHeight Picture height
     * @param picWidth Picture width
     * @param unit Length unit
     */
    public void exportSelectedSpectra(ImageType finalImageType, String outputFolder, Integer picHeight, Integer picWidth, String unit){

        Object[] allParameters = spectrumMainPanel.getParameters();

        RealTimeExportJDialog realTimeExportJDialog = new RealTimeExportJDialog((Integer) allParameters[0], (Integer) allParameters[1], picHeight, picWidth, unit,
                (PeptideSpectrumAnnotator) allParameters[2], (SpecificAnnotationSettings) allParameters[3], this, (HashMap<Double, String>)allParameters[4],
                (PtmSettings) allParameters[5], finalImageType, outputFolder, ptmFactory);

        if (exportAll){
            realTimeExportJDialog.readAllSpectrums(allSpectrumIndex);
        } else if (exportSelection){
            realTimeExportJDialog.readAllSelections(allSelections);
        } else {
            System.err.println("Exporting wrong");
        }
    }

    /**
     * Export selected spectrum reports
     */
    private void exportSelectedReport(){

        ExportReportsJDialog exportReportsJDialog;
        
        ArrayList<String> allColumn = new ArrayList<>();
        
        allColumn.add("b ions");
        allColumn.add("y ions");
        allColumn.add("by pairs");
        allColumn.add("Annotations");
        allColumn.add("Sequence");
        allColumn.add("Ratio");
        
        allColumn.addAll(scoreName);

        if (exportAll){
            exportReportsJDialog = new ExportReportsJDialog(this, allSpectrumIndex, allColumn, true);
        } else if (exportSelection){
            exportReportsJDialog = new ExportReportsJDialog(this, allSelections, allColumn);
        } else {
            System.err.println("Exporting wrong");
        }
    }

    /**
     * Return database connection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqliteConnection(){
        return sqliteConnection;
    }

    /**
     * Return extral para
     * @return ArrayList
     */
    public ArrayList<String> getScoreName(){
        return scoreName;
    }

    /**
     * Return SearchParameters
     * @return SearchParameters
     */
    public SearchParameters getSearchParameters(){
        return searchParameters;
    }

    /**
     * Remove target in list
     * @param list ArrayList<Integer>
     * @param target Integer
     * @return ArrayList<Integer>
     */
    private ArrayList<Integer> remove(ArrayList<Integer> list, Integer target){
        list.removeIf(item -> item.equals(target));

        return list;
    }

    /**
     * Remove target in list
     * @param list ArrayList<String>
     * @param target String
     * @return ArrayList<String>
     */
    private ArrayList<String> remove(ArrayList<String> list, String target){
        list.removeIf(item -> item.equals(target));

        return list;
    }
}

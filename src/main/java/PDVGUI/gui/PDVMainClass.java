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
import java.net.URLDecoder;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Main class to start soft and show dataBase results
 * Created by Ken on 5/8/2017.
 */
public class PDVMainClass extends JFrame {

    public JButton searchButton;
    public JButton loadingJButton;
    public JButton settingColorJButton;
    public JTextField pageNumJTextField;
    public JTextField searchItemTextField;
    private JButton upSortJButton;
    private JButton downSortJButton;
    private JButton nextJButton;
    private JButton upJButton;
    private JButton openSidebarJButton;
    private JTable spectrumJTable;
    private JPanel psmsJPanel;
    private JPanel spectrumShowJPanel;
    private JPanel backgroundPanel;
    private JPanel detailsJPanel;
    private JTextField pageSelectNumJTextField;
    private JTextField checkSpectrumJTextField;
    private JTextField fragmentIonAccuracyTxt;
    private JCheckBox allSelectedJCheckBox;
    private JSplitPane msAndTableJSplitPane;
    private JComboBox precursorIonUnit;
    private JComboBox sortColumnJCombox;
    private JComboBox searchTypeComboBox;
    private String[] searchType = new String[]{"Peptide","Spectrum"};

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
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Find Type
     */
    private String findType = "Peptide";
    /**
     * All spectrum Index
     */
    public ArrayList<ArrayList<String>> allSpectrumIndex = new ArrayList<>();
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
    private static final String VERSION = "1.3.1";

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
     * Empty constructor
     */
    private PDVMainClass(){}

    /**
     * Main constructor
     * @param newStart Symbol show newStart
     */
    public PDVMainClass(String newStart){

        setUpLogFile();

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
     * Get JarFile path
     * @return path
     */
    public static String getJarFilePath(){
        String jarPath = (new PDVMainClass()).getClass().getResource("PDVMainClass.class").getPath().split("!")[0];

        if (jarPath.lastIndexOf("/" + "PDV-" + VERSION) != -1) {
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring("file:".length(), jarPath.lastIndexOf("/" + "PDV-" + VERSION));
            } else {
                jarPath = jarPath.substring(0, jarPath.lastIndexOf("/" + "PDV-" + VERSION));
            }

            if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
                jarPath = jarPath.replace("/", "\\");
            }
        }else {
            jarPath = ".";
        }

        try {
            if (!new File(jarPath).exists()) {
                jarPath = URLDecoder.decode(jarPath, "UTF-8");
            }
            if (!new File(jarPath).exists()) {
                System.err.println(jarPath + " not found!");
                FileNotFoundException ex = new FileNotFoundException(jarPath + " not found!");
                ex.printStackTrace();
            }
        } catch (UnsupportedEncodingException ex) {
            System.err.println("Error reading file " + jarPath + "!");
            ex.printStackTrace();
        }

        return jarPath;
    }

    /**
     * Set spectrum table tooltips
     */
    private void setUpTableHeaderToolTips() {
        spectrumJTableToolTips = new ArrayList<>();
        spectrumJTableToolTips.add("Select it and output");
        spectrumJTableToolTips.add("Index");
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

        spectrumJTable.getColumn(" ").setMaxWidth(40);
        spectrumJTable.getColumn(" ").setMinWidth(40);

        spectrumJTable.getColumn("Selected").setMinWidth(30);
        spectrumJTable.getColumn("Selected").setMaxWidth(30);

        spectrumJTable.setDefaultRenderer(Double.class, new DefaultTableCellRenderer(){
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
                Component c = null;

                Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");

                String s = String.valueOf(value);

                if (pattern.matcher(s).matches()){
                    Double d = (Double)value;
                    s = String.valueOf(d);
                }

                c = super.getTableCellRendererComponent(table, s, isSelected, hasFocus, row, column);

                return c;
            }
        });

        ((DefaultTableCellRenderer)spectrumJTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JMenuBar menuBar = new JMenuBar();
        JSplitPane allJSplitPane = new JSplitPane();
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
        JPanel searchJPanel = new JPanel();
        JPanel sideJPanel = new JPanel();
        JPanel settingJPanel = new JPanel();
        JPanel loadingJPanel = new JPanel();
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
        pageNumJTextField = new JTextField();
        pageSelectNumJTextField = new JTextField();
        searchItemTextField = new JTextField();
        checkSpectrumJTextField = new JTextField();
        searchButton = new JButton();
        searchTypeComboBox = new JComboBox();
        psmsJPanel = new JPanel();
        spectrumShowJPanel = new JPanel();
        backgroundPanel = new JPanel();

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
                Component component = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    component.setBackground(Color.white);
                }else{
                    component.setBackground(new Color(164, 233, 255));
                }
                if(isRowSelected(row)){
                    component.setBackground(new Color(20,20,40));
                }
                if(String.valueOf(getValueAt(row, column)).contains(" Rank:"+"&nbsp<html>"+1)){
                    component.setBackground(new Color(255, 116, 135));
                }
                return component;
            }
        };

        spectrumJTable.setRowHeight(20);
        spectrumJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        spectrumJTable.getTableHeader().setFont(new Font("Dialog", 1, 13));

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

        searchItemTextField.setEditable(true);
        searchItemTextField.setHorizontalAlignment(SwingConstants.CENTER);
        searchItemTextField.setToolTipText("Data read unfinished!");

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

        GroupLayout searchJPanelLayout = new GroupLayout(searchJPanel);
        searchJPanel.setLayout(searchJPanelLayout);
        searchJPanelLayout.setHorizontalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox,100,100,GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchItemTextField,50, 200, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(searchButton,GroupLayout.PREFERRED_SIZE,50,GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        searchJPanelLayout.setVerticalGroup(
                searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(searchJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(backJButton)
                                .addComponent(searchTypeComboBox, 10, 20, 20)
                                .addComponent(searchItemTextField, 10, 20, 20)
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

        settingJPanel.setBackground(new Color(255, 255, 255));
        settingJPanel.setMinimumSize(new Dimension(20, 0));
        settingJPanel.setOpaque(false);

        settingJPanel.setLayout(new BoxLayout(settingJPanel, BoxLayout.X_AXIS));

        fragmentIonAccuracyJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        fragmentIonType1Lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        sortJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

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
        loadingJButton.setText("Result importing");

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
                evt -> spectrumMainPanel.updateSpectrum());

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
        TitledBorder titledBorder = BorderFactory.createTitledBorder("PSM Table" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        psmsJPanel.setBorder(titledBorder);

        allSelectedJLabel.setText("Whole page");
        allSelectedJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
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

        pageSelectNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
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

        GroupLayout psmssLayeredPanelLayout = new GroupLayout(psmsJPanel);
        psmsJPanel.setLayout(psmssLayeredPanelLayout);
        psmssLayeredPanelLayout.setHorizontalGroup(
                psmssLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(psmssLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(psmssLayeredPanelLayout.createSequentialGroup()
                                .addGap(10, 15, 15)
                                .addComponent(allSelectedJCheckBox)
                                .addComponent(allSelectedJLabel)
                                .addGap(100,1200,2000)
                                .addComponent(pageSelectNumJTextField,50, 50, 50)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(upJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nextJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addComponent(pageNumJTextField,50, 50, 70))
        );
        psmssLayeredPanelLayout.setVerticalGroup(
                psmssLayeredPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(psmssLayeredPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(psmssLayeredPanelLayout.createParallelGroup()
                                        .addComponent(allSelectedJCheckBox)
                                        .addComponent(allSelectedJLabel)
                                        .addComponent(pageSelectNumJTextField, 10, 20, 20)
                                        .addComponent(upJButton, 10, 20, 20)
                                        .addComponent(nextJButton, 10, 20, 20)
                                        .addComponent(pageNumJTextField, 10, 20, 20)))
        );

        msAndTableJSplitPane.setBorder(null);
        msAndTableJSplitPane.setDividerLocation(0.2);
        msAndTableJSplitPane.setDividerSize(5);
        msAndTableJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        msAndTableJSplitPane.setResizeWeight(0.5);
        msAndTableJSplitPane.setOpaque(false);
        msAndTableJSplitPane.setContinuousLayout(true);

        detailsJPanel.setOpaque(false);
        detailsJPanel.setBackground(Color.WHITE);
        titledBorder = BorderFactory.createTitledBorder("File detail" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 13));
        detailsJPanel.setBorder(titledBorder);

        GroupLayout msOneJPanelLayout = new GroupLayout(detailsJPanel);
        detailsJPanel.setLayout(msOneJPanelLayout);
        infoPanel.setOpaque(false);

        msOneJPanelLayout.setHorizontalGroup(
                msOneJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(infoPanel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
        );
        msOneJPanelLayout.setVerticalGroup(
                msOneJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(infoPanel, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
        );

        msAndTableJSplitPane.setLeftComponent(detailsJPanel);
        msAndTableJSplitPane.setRightComponent(psmsJPanel);

        allJSplitPane.setTopComponent(msAndTableJSplitPane);

        spectrumShowJPanel.setOpaque(false);
        spectrumMainPanel.setOpaque(false);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowJPanel);
        spectrumShowJPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumMainPanel,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
            //UIManager.setLookAndFeel(motif);
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);

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
     * Import pepXml file
     * @param spectrumFile Spectrum file
     * @param spectrumsFileFactory Spectrum saving objective
     * @param pepXMLFile PepXML File
     * @param spectrumFileType Type of spectrum file
     */
    public void importFilePep(File spectrumFile, Object spectrumsFileFactory, File pepXMLFile, String spectrumFileType, HashMap<String, Integer> spectrumIdAndNumber) {

        isMaxQuant = false;
        isNewSoft = false;
        isPepXML = true;

        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumFileType = spectrumFileType;

        if (spectrumFileType.equals("mzml")){

            scans = (ScanCollectionDefault) spectrumsFileFactory;

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
                            scans, spectrumFileType, progressDialog, spectrumIdAndNumber);

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

            scans = (ScanCollectionDefault) spectrumsFileFactory;

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

                scans = (ScanCollectionDefault) spectrumsFileFactory;
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

                scans = (ScanCollectionDefault) spectrumsFileFactory;
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
    public void importFragPipe(HashMap<File, String> fileToType, File idFile){

        this.isFrage = true;

        databasePath = idFile.getAbsolutePath()+".db";

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

                    fragePipeImport = new FragePipeImport(fileToType, idFile, progressDialog, PDVMainClass.this);

                    sqliteConnection = fragePipeImport.getSqLiteConnection();
                    allModifications = fragePipeImport.getAllModifications();

                    scoreName = fragePipeImport.getScoreName();

                    ArrayList<String> orderName = new ArrayList<>();
                    orderName.add("PSMIndex");
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
    public void importMaxQuantResults(String maxQuantResultPath, Boolean existMGF) {

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

                    maxQuantFileImport = new MaxQuantFileImport(PDVMainClass.this, new File(maxQuantResultPath), progressDialog);

                    originalInfor = maxQuantFileImport.getOriginalInfor();
                    detailsList = maxQuantFileImport.getDetailsList();

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
                    orderName.add("MassError");
                    orderName.add("Score");
                    scoreName.add("Score");
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

        fragmentIonAccuracyTxt.setText("  "+String.valueOf(annotationSettings.getFragmentIonAccuracy())+"  ");

        databaseTableModel = new DatabaseTableModel(searchParameters, scoreName, spectrumsFileFactory, spectrumFileType, spectrumKeyToSelected, isNewSoft, isMaxQuant, isPepXML);
        spectrumJTable.setModel(databaseTableModel);

        updateTable();
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

        updateTable();
    }

    /**
     * Update table
     */
    public void updateTable(){

        ProgressDialogX progressDialogX = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Moving. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "MovingPro").start();

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
                                progressDialogX.setRunFinished();
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
                                progressDialogX.setRunFinished();
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
                                progressDialogX.setRunFinished();
                                e.printStackTrace();
                                break;
                            }
                        }

                        databaseTableModel.updateTable(selectedItem, selectPageSpectrumIndex, spectrumKeyToSelected);
                    }
                }catch (Exception e){
                    progressDialogX.setRunFinished();
                    e.printStackTrace();
                }

                progressDialogX.setRunFinished();

                setTableProperties();

                spectrumJTable.setRowSelectionInterval(0, 0);

                spectrumJTableMouseReleased(null);

                buttonCheck();

                ((DefaultTableModel) spectrumJTable.getModel()).fireTableDataChanged();
                spectrumJTable.repaint();

                psmsJPanel.repaint();

            }
        }.start();
    }

    /**
     * Update table
     * @param searchIDs Search ids
     */
    private void updateTable(ArrayList<String> searchIDs){
        ArrayList<ArrayList<Object>> selectedItem = new ArrayList<>();

        for (String spectrumIndex : searchIDs){
            try {
                selectedItem.add(sqliteConnection.getOneSpectrumItem(spectrumIndex));
            } catch (SQLException e) {
                e.printStackTrace();
                break;
            }
        }

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
            for (String spectrumIndex : selectPageSpectrumIndex) {
                try {
                    selectedItem.add(sqliteConnection.getOneFrageSpectrumItem(spectrumIndex));
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            frageTableModel.updateTabel(selectedItem, selectPageSpectrumIndex, spectrumKeyToSelected);
        }
        else {
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
        if(scans != null){
            scans.reset();
        }
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
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

            detailsJPanel.setVisible(false);

            msAndTableJSplitPane.setDividerSize(0);

            msAndTableJSplitPane.revalidate();
            msAndTableJSplitPane.repaint();

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

                if (searchTypeComboBox.getSelectedItem().equals("Peptide")) {
                    findType = "Peptide";
                } else if (searchTypeComboBox.getSelectedItem().equals("Spectrum")) {
                    findType = "Spectrum";
                }

                try {

                    ArrayList<String> searchIDs = new ArrayList<>();
                    if (!searchItemTextField.getText().equals("")) {
                        String findItem = searchItemTextField.getText();
                        if (Objects.equals(findType, "Spectrum")) {
                            searchIDs = sqliteConnection.getSelectedTitleIndex(findItem);

                        } else if (Objects.equals(findType, "Peptide")) {
                            searchItemTextField.setText(findItem.toUpperCase());
                            searchIDs = sqliteConnection.getSelectedPeptideIndex(findItem.toUpperCase());
                        }
                    }

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

    /**
     * backJButtonActionPerformed
     * @param evt Mouse click action
     */
    private void backJButtonActionPerformed(ActionEvent evt){
        allSpectrumIndex = new ArrayList<>();
        ArrayList<String> each = new ArrayList<>();
        Integer count = 0;

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
        if (!isDenovo){
            columnName.add("Other Assumption");
        } else {
            columnName.add("#Peaks");
            columnName.add("N-Gap");
            columnName.add("C-Gap");
            columnName.add("Score");
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

            int scanKey = spectrumMatch.getSpectrumNumber();

            IScan iScan = scans.getScanByNum(scanKey);
            ISpectrum spectrum = iScan.getSpectrum();

            Charge charge = spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
            ArrayList<Charge> charges = new ArrayList<>();
            charges.add(charge);

            Precursor precursor = new Precursor(scans.getScanByNum(iScan.getPrecursor().getParentScanNum()).getRt(), iScan.getPrecursor().getMzTarget(),
                    iScan.getPrecursor().getIntensity(), charges);

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

        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        spectrumShowJPanel.setBorder(titledBorder);

        spectrumMainPanel.updateSpectrum(spectrumIdentificationAssumption, mSnSpectrum, String.valueOf(selectedPsmKey));

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
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
                (PtmSettings) allParameters[5], finalImageType, outputFolder);

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

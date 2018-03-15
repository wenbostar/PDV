package PDVGUI.gui;

import PDVGUI.gui.utils.FileImport.PrideXMLImportDialog;
import PDVGUI.gui.utils.SpectrumMainPanel;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.*;
import com.compomics.util.experiment.massspectrometry.Precursor;
import uk.ac.ebi.pride.jaxb.model.*;
import uk.ac.ebi.pride.jaxb.model.Spectrum;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * PrideXML result display
 * Created by Ken on 9/14/2017.
 */
public class PrideXMLDisplay extends JFrame {

    private JPanel psmJPanel;
    private JPanel spectrumShowJPanel;
    private JPanel spectrumTableJPanel;
    private JPanel treeJPanel;
    private JSplitPane allJSplitPane;
    private JSplitPane tabelAndChartJSplit;
    private JSplitPane treeAndDetailJSplit;
    private JTable psmJTable;
    private JTable spectrumJTable;
    private JButton openSidebarJButton;
    private JEditorPane inforJEditor;
    private JTabbedPane pSMSpectrumJTabbedpane;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;

    /**
     * PSMTable tooltips list
     */
    private ArrayList<String> psmJTableToolTips;
    /**
     * Spectrum table tooltips list
     */
    private ArrayList<String> spectrumJTableToolTips;
    /**
     * PrideXMLReader List
     */
    public HashMap<String, PrideXmlReader> titleToPrideXmlReader = new HashMap<>();
    /**
     * Accession to ID
     */
    private HashMap<String, String> accessionToId = new HashMap<>();
    /**
     * Current prideXML reader
     */
    private PrideXmlReader currentPrideXmlReader;
    /**
     * Current protein
     */
    private String currentProtein;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Current peptide list
     */
    private ArrayList<Peptide> currentPeptideList = new ArrayList<>();
    /**
     * Current peptide item list
     */
    private List<PeptideItem> currentPeptideItemList;
    /**
     * Search parameters
     */
    public SearchParameters searchParameters = new SearchParameters();
    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * New spectrum main panel
     */
    private SpectrumMainPanel onlySpectrumMainPanel;
    /**
     * Current table match list
     */
    private ArrayList< SpectrumMatch> currentTableMatchList = new ArrayList<>();
    /**
     * Current MZ list
     */
    private ArrayList<Double> currentMz = new ArrayList<>();
    /**
     * Current file count
     */
    private Integer currentFileCount = 0;
    /**
     * Current protein count
     */
    private Integer currentProteinCount = 0;
    /**
     * Selected psm key
     */
    private String selectedPSMKey;
    /**
     * Current spectrum
     */
    private MSnSpectrum currentSpectrum;
    /**
     * Current table psm key list
     */
    private ArrayList<String> currentTablePSMKeyList = new ArrayList<>();

    /**
     * Constructor
     * @param pdvStart Parent class
     */
    public PrideXMLDisplay(PDVStart pdvStart) {

        setUpTableHeaderToolTips();

        spectrumMainPanel = new SpectrumMainPanel(this);

        onlySpectrumMainPanel = new SpectrumMainPanel(this);

        initComponent();

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        setVisible(true);

        new PrideXMLImportDialog(this);

        ArrayList<String> modification =  ptmFactory.getPTMs();

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
     * Set spectrum table tooltips
     */
    private void setUpTableHeaderToolTips() {
        psmJTableToolTips = new ArrayList<>();
        psmJTableToolTips.add("Spectrum key");
        psmJTableToolTips.add("Peptide Sequence");
        psmJTableToolTips.add("Spectrum key in Spectrum File");
        psmJTableToolTips.add("Precursor Charge");
        psmJTableToolTips.add("Precursor m/z");
        psmJTableToolTips.add("Retention Time");
        psmJTableToolTips.add("Comments");

        spectrumJTableToolTips = new ArrayList<>();
        spectrumJTableToolTips.add("Spectrum key in spectrum file");
        spectrumJTableToolTips.add("MS level");
        spectrumJTableToolTips.add("Precursor charge");
        spectrumJTableToolTips.add("Precursor m/z");
        spectrumJTableToolTips.add("RT range start");
        spectrumJTableToolTips.add("RT rang stop");
        spectrumJTableToolTips.add("Peaks number");
        spectrumJTableToolTips.add("Intensity sum");
        spectrumJTableToolTips.add("Identified or not");
    }

    /**
     * Init all GUI components
     */
    private void initComponent() {

        JPanel mainJPanel = new JPanel();
        JPanel backJPanel = new JPanel();
        JPanel sideJPanel = new JPanel();
        JPanel settingJPanel = new JPanel();
        JButton setButton = new JButton();
        JLabel splitJLabel1 = new JLabel(" | ");
        JLabel splitJLabel2 = new JLabel(" | ");
        JMenuBar menuBar = new JMenuBar();
        JMenu fileJMenu = new JMenu();
        JMenuItem exitJMenuItem = new JMenuItem();
        JLabel fragmentIonAccuracyJLabel = new JLabel("Fragment m/z Tolerance: ");
        JLabel fragmentIonType1Lbl = new JLabel("Fragment Ion Types: ");
        JScrollPane inforJScrollPane = new JScrollPane();
        JScrollPane psmsScrollPane = new JScrollPane();
        JScrollPane spectrumTableScrollPane = new JScrollPane();
        JPanel detailsJPanel = new JPanel();
        JButton addNewJButton = new JButton();

        allJSplitPane = new JSplitPane();
        tabelAndChartJSplit = new JSplitPane();
        spectrumShowJPanel  = new JPanel();
        inforJEditor = new JEditorPane();
        pSMSpectrumJTabbedpane = new JTabbedPane();
        fragmentIonAccuracyTxt = new JTextField("0.5");
        precursorIonUnit = new JComboBox();
        psmJPanel = new JPanel();
        spectrumTableJPanel = new JPanel();
        treeAndDetailJSplit = new JSplitPane();
        treeJPanel = new JPanel();
        openSidebarJButton = new JButton();

        psmJTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return psmJTableToolTips.get(realIndex);
                    }

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    component.setBackground(Color.white);
                } else {
                    component.setBackground(new Color(164, 233, 255));
                }
                if (isRowSelected(row)) {
                    component.setBackground(new Color(20, 20, 40));
                }
                if (String.valueOf(getValueAt(row, column)).contains(" Rank:" + "&nbsp<html>" + 1)) {
                    component.setBackground(new Color(255, 116, 135));
                }
                return component;
            }
        };

        psmJTable.setRowHeight(20);
        psmJTable.setFont(new Font("Arial", Font.PLAIN, 10));
        psmJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));
        psmJTable.setAutoCreateRowSorter(true);

        spectrumJTable = new JTable() {

            protected JTableHeader createDefaultTableHeader() {

                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        return spectrumJTableToolTips.get(realIndex);
                    }

                };
            }

            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component component = super.prepareRenderer(renderer, row, column);
                if (row % 2 == 0) {
                    component.setBackground(Color.white);
                } else {
                    component.setBackground(new Color(164, 233, 255));
                }
                if (isRowSelected(row)) {
                    component.setBackground(new Color(20, 20, 40));
                }
                if (String.valueOf(getValueAt(row, column)).contains(" Rank:" + "&nbsp<html>" + 1)) {
                    component.setBackground(new Color(255, 116, 135));
                }
                return component;
            }
        };

        spectrumJTable.setRowHeight(20);
        spectrumJTable.setFont(new Font("Arial", Font.PLAIN, 10));
        spectrumJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));
        spectrumJTable.setAutoCreateRowSorter(true);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - PrideXML Display");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(760, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainJPanel.setOpaque(false);
        mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        exitJMenuItem.setMnemonic('E');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);

        fileJMenu.add(exitJMenuItem);

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        menuBar.add(fileJMenu);

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

        addNewJButton.setIcon(new ImageIcon(getClass().getResource("/icons/add.png")));
        addNewJButton.setBorder(null);
        addNewJButton.setBorderPainted(false);
        addNewJButton.setContentAreaFilled(false);
        addNewJButton.setToolTipText("Add more pride XML files.");
        addNewJButton.addActionListener(this::addNewJButtonActionPerform);

        sideJPanel.add(openSidebarJButton);
        sideJPanel.add(addNewJButton);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));

        precursorIonUnit.setMaximumSize(new Dimension(50, 20));
        precursorIonUnit.setBackground(Color.WHITE);

        fragmentIonAccuracyTxt.setHorizontalAlignment(SwingConstants.CENTER);
        fragmentIonAccuracyTxt.setMaximumSize(new Dimension(40, 20));

        setButton.setIcon(new ImageIcon(getClass().getResource("/icons/update.png")));
        setButton.setBorder(null);
        setButton.setBorderPainted(false);
        setButton.setContentAreaFilled(false);
        setButton.setToolTipText("Set New setting");
        setButton.addActionListener(this::setButtonActionPerform);

        settingJPanel.setBackground(new Color(217, 248, 255));
        settingJPanel.setMinimumSize(new Dimension(20, 0));
        settingJPanel.setOpaque(false);

        settingJPanel.setLayout(new BoxLayout(settingJPanel, BoxLayout.X_AXIS));

        settingJPanel.add(splitJLabel1);
        settingJPanel.add(fragmentIonType1Lbl);
        settingJPanel.add(precursorIonUnit);
        settingJPanel.add(splitJLabel2);
        settingJPanel.add(fragmentIonAccuracyJLabel);
        settingJPanel.add(fragmentIonAccuracyTxt);
        settingJPanel.add(setButton);

        allJSplitPane.setBorder(null);
        allJSplitPane.setDividerLocation(0.2);
        allJSplitPane.setDividerSize(5);
        allJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        allJSplitPane.setResizeWeight(0.5);
        allJSplitPane.setOpaque(false);
        allJSplitPane.setContinuousLayout(true);

        treeAndDetailJSplit.setBorder(null);
        treeAndDetailJSplit.setDividerLocation(0.4);
        treeAndDetailJSplit.setDividerSize(5);
        treeAndDetailJSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        treeAndDetailJSplit.setResizeWeight(0.5);
        treeAndDetailJSplit.setOpaque(false);
        treeAndDetailJSplit.setVisible(true);
        treeAndDetailJSplit.setContinuousLayout(true);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Pride XML file(s)" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        treeJPanel.setBorder(titledBorder);
        treeJPanel.setOpaque(false);

        treeAndDetailJSplit.setTopComponent(treeJPanel);

        detailsJPanel.setOpaque(false);
        detailsJPanel.setBorder(BorderFactory.createTitledBorder("Details"));

        inforJEditor.setContentType("text/html");
        inforJEditor.setEditable(false);
        inforJScrollPane.setViewportView(inforJEditor);

        GroupLayout detailsJPanelLayout = new GroupLayout(detailsJPanel);
        detailsJPanel.setLayout(detailsJPanelLayout);
        detailsJPanelLayout.setHorizontalGroup(
                detailsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );
        detailsJPanelLayout.setVerticalGroup(
                detailsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                                .addContainerGap())
        );

        treeAndDetailJSplit.setRightComponent(detailsJPanel);

        allJSplitPane.setTopComponent(treeAndDetailJSplit);

        titledBorder = BorderFactory.createTitledBorder("PSM Table" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        psmJPanel.setBorder(titledBorder);
        psmJPanel.setOpaque(false);

        psmJTable.setOpaque(true);
        psmJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        psmJTable.addMouseListener(new MouseAdapter() {

            public void mouseExited(MouseEvent evt) {
                psmJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                psmJTableMouseReleased(evt);
            }
        });
        psmJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                psmJTableKeyReleased(evt);
            }
        });

        psmsScrollPane.setViewportView(psmJTable);
        psmsScrollPane.setOpaque(true);
        psmsScrollPane.setBackground(Color.white);
        psmJTable.getAccessibleContext().setAccessibleName("psmJTable");

        GroupLayout psmJPanelLayout = new GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);

        psmJPanelLayout.setHorizontalGroup(
                psmJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
        );

        psmJPanelLayout.setVerticalGroup(
                psmJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(psmsScrollPane, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );

        pSMSpectrumJTabbedpane.addChangeListener(this::pSMSpectrumJTabbedpaneStateChanged);
        pSMSpectrumJTabbedpane.setOpaque(false);
        pSMSpectrumJTabbedpane.add(psmJPanel, "PSM");

        titledBorder = BorderFactory.createTitledBorder("Spectrum Table" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        spectrumTableJPanel.setBorder(titledBorder);
        spectrumTableJPanel.setOpaque(false);

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
        
        spectrumTableScrollPane.setViewportView(spectrumJTable);
        spectrumTableScrollPane.setOpaque(false);
        
        spectrumJTable.getAccessibleContext().setAccessibleName("spectrumJTable");

        GroupLayout spectrumTableJPanelLayout = new GroupLayout(spectrumTableJPanel);
        spectrumTableJPanel.setLayout(spectrumTableJPanelLayout);

        spectrumTableJPanelLayout.setHorizontalGroup(
                spectrumTableJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumTableScrollPane, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
        );

        spectrumTableJPanelLayout.setVerticalGroup(
                spectrumTableJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumTableScrollPane, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );

        pSMSpectrumJTabbedpane.add(spectrumTableScrollPane, "Spectrum");

        tabelAndChartJSplit.setBorder(null);
        tabelAndChartJSplit.setDividerLocation(0.4);
        tabelAndChartJSplit.setDividerSize(5);
        tabelAndChartJSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        tabelAndChartJSplit.setResizeWeight(0.5);
        tabelAndChartJSplit.setOpaque(false);
        tabelAndChartJSplit.setContinuousLayout(true);

        tabelAndChartJSplit.setTopComponent(pSMSpectrumJTabbedpane);

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

        tabelAndChartJSplit.setRightComponent(spectrumShowJPanel);

        allJSplitPane.setRightComponent(tabelAndChartJSplit);

        backJPanel.setBackground(Color.WHITE);

        GroupLayout backJPanelLayout = new GroupLayout(backJPanel);
        backJPanel.setLayout(backJPanelLayout);
        backJPanelLayout.setHorizontalGroup(
                backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, 20, 20)
                                .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        backJPanelLayout.setVerticalGroup(
                backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(settingJPanel)
                        .addComponent(backJPanel)
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(settingJPanel, GroupLayout.DEFAULT_SIZE, 30, 30)
                                .addComponent(backJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setJMenuBar(menuBar);

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

        String motif;
        String os = System.getProperty("os.name");
        if(os.toLowerCase().startsWith("win")){
            motif="com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else if (os.toLowerCase().startsWith("mac")){
            motif="com.sun.java.swing.plaf.mac.MacLookAndFeel";
        } else {
            motif=UIManager.getSystemLookAndFeelClassName();
        }
        try {
            UIManager.setLookAndFeel(motif);
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);

        pack();
    }

    /**
     * Close the program when click exist
     *
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
                "Do you want to give up " + " and close PDV" + "?",
                "Close PDV",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {
            closePDV();
        } else if (value == JOptionPane.NO_OPTION) {
        }
    }

    /**
     * Close the program
     */
    private void closePDV() {
        System.exit(0);
    }

    /**
     * exitJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exitJMenuItemActionPerformed(ActionEvent evt){
        close();
    }

    /**
     * openSidebarJButtonActionPerform
     * @param evt Mouse click event
     */
    private void openSidebarJButtonActionPerform(ActionEvent evt){

        if(treeAndDetailJSplit.isVisible()){
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

            treeAndDetailJSplit.setVisible(false);

            allJSplitPane.setDividerSize(0);

            allJSplitPane.revalidate();
            allJSplitPane.repaint();

        } else {
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/fold.png")));

            treeAndDetailJSplit.setVisible(true);

            allJSplitPane.setDividerSize(5);

            allJSplitPane.setDividerLocation(0.2);

            allJSplitPane.revalidate();
            allJSplitPane.repaint();
        }
    }

    /**
     * addNewJButtonActionPerform
     * @param evt Mouse click event
     */
    private void addNewJButtonActionPerform(ActionEvent evt){
        new PrideXMLImportDialog(this);
    }

    /**
     * setButtonActionPerform
     * @param evt Mouse click event
     */
    private void setButtonActionPerform(ActionEvent evt){
        Double fragmentIonMZTolerance = 0.5;
        if(fragmentIonAccuracyTxt.getText() != "" && fragmentIonAccuracyTxt.getText() != null){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        }

        if(precursorIonUnit.getSelectedIndex() == 0){
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.DA);
        }else {
            searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);
        }
        searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);

        spectrumMainPanel.setFragmentIonAccuracy(searchParameters.getFragmentAccuracyType(), fragmentIonMZTolerance);
    }

    /**
     * pSMSpectrumJTabbedpaneStateChanged
     * @param evt Mouse click event
     */
    private void pSMSpectrumJTabbedpaneStateChanged(ChangeEvent evt) {

        int index = pSMSpectrumJTabbedpane.getSelectedIndex();

        spectrumShowJPanel.removeAll();

        switch (index) {
            case 0:
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

                tabelAndChartJSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                        evt1 -> spectrumMainPanel.updateSpectrum());
                allJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                        evt12 -> spectrumMainPanel.updateSpectrum());
                break;

            case 1:
                GroupLayout spectrumMainPanelLayout1 = new GroupLayout(spectrumShowJPanel);
                spectrumShowJPanel.setLayout(spectrumMainPanelLayout1);
                spectrumMainPanelLayout1.setHorizontalGroup(
                        spectrumMainPanelLayout1.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(onlySpectrumMainPanel,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                );
                spectrumMainPanelLayout1.setVerticalGroup(
                        spectrumMainPanelLayout1.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addGroup(spectrumMainPanelLayout1.createSequentialGroup()
                                        .addComponent(onlySpectrumMainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                );

                tabelAndChartJSplit.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                        evt13 -> onlySpectrumMainPanel.updateSpectrum(currentSpectrum));

                allJSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                        evt14 -> onlySpectrumMainPanel.updateSpectrum(currentSpectrum));
                break;
        }

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void psmJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Mouse click event
     */
    private void psmJTableMouseReleased(MouseEvent evt) {

        psmJTable.requestFocus();
        int row = psmJTable.getSelectedRow();
        int column = psmJTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            Spectrum selectedSpectrum = currentPeptideItemList.get(row).getSpectrum();

            Number[] mzArray = selectedSpectrum.getMzNumberArray();
            Number[] intentArray = selectedSpectrum.getIntentArray();

            HashMap<Double, Peak> peakMap = new HashMap<>();

            for (int i = 0; i<mzArray.length; i++){

                Peak peak = new Peak(mzArray[i].doubleValue(), intentArray[i].doubleValue());

                peakMap.put(mzArray[i].doubleValue(), peak);

            }

            ArrayList<Charge> chargeArrayList = new ArrayList<>();

            chargeArrayList.add(currentTableMatchList.get(row).getBestPeptideAssumption().getIdentificationCharge());

            Precursor precursor = new Precursor(-1.0, currentMz.get(row), chargeArrayList);

            MSnSpectrum mSnSpectrum = new MSnSpectrum(2, precursor, String.valueOf(selectedSpectrum.getId()), peakMap, "");

            selectedPSMKey = currentTablePSMKeyList.get(row);

            updateSpectrum(mSnSpectrum, currentTableMatchList.get(row));

            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Key event
     */
    private void psmJTableKeyReleased(KeyEvent evt) {

        psmJTable.requestFocus();

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int row = psmJTable.getSelectedRow();

            if (row != -1) {
                Spectrum selectedSpectrum = currentPeptideItemList.get(row).getSpectrum();

                Number[] mzArray = selectedSpectrum.getMzNumberArray();
                Number[] intentArray = selectedSpectrum.getIntentArray();

                HashMap<Double, Peak> peakMap = new HashMap<>();

                for (int i = 0; i<mzArray.length; i++){

                    Peak peak = new Peak(mzArray[i].doubleValue(), intentArray[i].doubleValue());

                    peakMap.put(mzArray[i].doubleValue(), peak);

                }

                ArrayList<Charge> chargeArrayList = new ArrayList<>();

                chargeArrayList.add(currentTableMatchList.get(row).getBestPeptideAssumption().getIdentificationCharge());

                Precursor precursor = new Precursor(-1.0, currentMz.get(row), chargeArrayList);

                MSnSpectrum mSnSpectrum = new MSnSpectrum(2, precursor, String.valueOf(selectedSpectrum.getId()), peakMap, "");

                selectedPSMKey = currentTablePSMKeyList.get(row);

                updateSpectrum(mSnSpectrum, currentTableMatchList.get(row));
            }
        }
    }

    /**
     * Return normal
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * spectrumJTableMouseReleased
     * @param evt Mouse click event
     */
    private void spectrumJTableMouseReleased(MouseEvent evt){
        spectrumJTable.requestFocus();

        int row = spectrumJTable.getSelectedRow();
        int column = spectrumJTable.getSelectedColumn();

        if (row != -1) {
            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            String id =String.valueOf(spectrumJTable.getValueAt(row, 0));

            Spectrum selectedSpectrum = currentPrideXmlReader.getSpectrumById(id);

            Number[] mzArray = selectedSpectrum.getMzNumberArray();
            Number[] intentArray = selectedSpectrum.getIntentArray();

            HashMap<Double, Peak> peakMap = new HashMap<>();

            for (int i = 0; i<mzArray.length; i++){

                Peak peak = new Peak(mzArray[i].doubleValue(), intentArray[i].doubleValue());

                peakMap.put(mzArray[i].doubleValue(), peak);

            }

            ArrayList<Charge> chargeArrayList = new ArrayList<>();

            chargeArrayList.add(new Charge(+1, (Integer) spectrumJTable.getValueAt(row, 2)));

            Precursor precursor = new Precursor(-1.0, (Double) spectrumJTable.getValueAt(row, 3), chargeArrayList);

            currentSpectrum = new MSnSpectrum(2, precursor, id, peakMap, "");

            selectedPSMKey = String.valueOf(row);

            onlySpectrumMainPanel.updateSpectrum(currentSpectrum);

        }
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * spectrumJTableKeyReleased
     * @param evt Mouse click event
     */
    private void spectrumJTableKeyReleased(KeyEvent evt){
        spectrumJTable.requestFocus();

        if (evt == null || evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            final int row = spectrumJTable.getSelectedRow();

            if (row != -1) {
                this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                String id =String.valueOf(spectrumJTable.getValueAt(row, 0));

                Spectrum selectedSpectrum = currentPrideXmlReader.getSpectrumById(id);

                Number[] mzArray = selectedSpectrum.getMzNumberArray();
                Number[] intentArray = selectedSpectrum.getIntentArray();

                HashMap<Double, Peak> peakMap = new HashMap<>();

                for (int i = 0; i<mzArray.length; i++){

                    Peak peak = new Peak(mzArray[i].doubleValue(), intentArray[i].doubleValue());

                    peakMap.put(mzArray[i].doubleValue(), peak);

                }

                ArrayList<Charge> chargeArrayList = new ArrayList<>();

                chargeArrayList.add(new Charge(+1, (Integer) spectrumJTable.getValueAt(row, 2)));

                Precursor precursor = new Precursor(-1.0, (Double) spectrumJTable.getValueAt(row, 3), chargeArrayList);

                currentSpectrum = new MSnSpectrum(2, precursor, id, peakMap, "");

                selectedPSMKey = String.valueOf(row);

                onlySpectrumMainPanel.updateSpectrum(currentSpectrum);
            }
        }
    }

    /**
     * Update spectrum
     * @param mSnSpectrum MSN Spectrum
     * @param spectrumMatch Spectrum match
     */
    private void updateSpectrum(MSnSpectrum mSnSpectrum, SpectrumMatch spectrumMatch){

        spectrumShowJPanel.setBorder(BorderFactory.createTitledBorder(spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence()));

        spectrumMainPanel.updateSpectrum(spectrumMatch.getBestPeptideAssumption(), mSnSpectrum, selectedPSMKey);

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
    }

    /**
     * Update tree
     * @param prideXmlReader PrideXML reader
     */
    public void updateTree(PrideXmlReader prideXmlReader){

        if(!titleToPrideXmlReader.containsValue(prideXmlReader)){

            treeJPanel.removeAll();

            String title = prideXmlReader.getExpTitle();

            titleToPrideXmlReader.put(title, prideXmlReader);

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Files");

            DefaultTreeModel treeModel = new DefaultTreeModel(root);

            List<String> identificationList;

            for(String eachTtitle : titleToPrideXmlReader.keySet()){
                PrideXmlReader prideXmlReaderInList = titleToPrideXmlReader.get(eachTtitle);

                identificationList = prideXmlReaderInList.getIdentIds();

                DefaultMutableTreeNode prideFileRoot = new DefaultMutableTreeNode(eachTtitle + "( "+identificationList.size()+" )");
                treeModel.insertNodeInto(prideFileRoot, root, root.getChildCount());

                ArrayList<String> proteinAccessionList = new ArrayList<>();

                for(String identification : identificationList){

                    Identification identificationValue = prideXmlReaderInList.getIdentById(identification);

                    proteinAccessionList.add(identificationValue.getAccession() +"( "+ identificationValue.getPeptideItem().size()+" )");

                    accessionToId.put(identificationValue.getAccession(), identification);
                }

                Collections.sort(proteinAccessionList);

                for (String proteinName : proteinAccessionList){
                    DefaultMutableTreeNode proteinNode = new DefaultMutableTreeNode(proteinName);

                    treeModel.insertNodeInto(proteinNode, prideFileRoot, prideFileRoot.getChildCount());
                }
            }

            JScrollPane jScrollPane;

            JTree tree = new JTree(treeModel);
            tree.setOpaque(true);
            tree.setBackground(Color.white);
            tree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.SINGLE_TREE_SELECTION);

            tree.addTreeSelectionListener(this::valueChanged);

            tree.setSelectionInterval(1,1);

            jScrollPane = new JScrollPane(tree);
            jScrollPane.setOpaque(false);
            jScrollPane.setBackground(Color.white);

            GroupLayout treeJPanelLayout = new GroupLayout(treeJPanel);
            treeJPanel.setLayout(treeJPanelLayout);

            treeJPanelLayout.setHorizontalGroup(
                    treeJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
            );

            treeJPanelLayout.setVerticalGroup(
                    treeJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
            );

            allJSplitPane.setDividerLocation(0.2);

            tabelAndChartJSplit.setDividerLocation(0.4);

            treeJPanel.revalidate();
            treeJPanel.repaint();
        }
    }

    /**
     * Tree valueChanged
     * @param evt TreeSelectionEvent
     */
    private void valueChanged(TreeSelectionEvent evt) {

        JTree tree = (JTree)evt.getSource();
        DefaultMutableTreeNode selectionNode = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();

        TreeNode parentNode = selectionNode.getParent();

        String nodeName = selectionNode.toString().split("\\(")[0];

        if(titleToPrideXmlReader.containsKey(nodeName)){
            currentPrideXmlReader = titleToPrideXmlReader.get(nodeName);
            currentProtein = selectionNode.getFirstChild().toString().split("\\(")[0];

            currentFileCount = parentNode.getIndex(selectionNode) + 1;

            currentProteinCount = 1;

            updatePSMJTable();

            updateDetails();

            updateSpectrumTable();

        } else if(!nodeName.equals("Files")){
            if(currentPrideXmlReader != titleToPrideXmlReader.get(parentNode.toString().split("\\(")[0])){
                currentPrideXmlReader = titleToPrideXmlReader.get(parentNode.toString().split("\\(")[0]);
                updateDetails();

                updateSpectrumTable();
            }

            currentProtein = nodeName;

            TreeNode grandNode = parentNode.getParent();

            currentFileCount = grandNode.getIndex(parentNode) + 1;

            currentProteinCount = parentNode.getIndex(selectionNode) + 1;

            updatePSMJTable();
        }

        tree.requestFocus();
    }

    /**
     * updatePSMJTable
     */
    private void updatePSMJTable(){

        currentPeptideList.clear();
        currentTableMatchList.clear();
        currentMz.clear();
        currentTablePSMKeyList.clear();

        String id = accessionToId.get(currentProtein);

        Identification gelFreeIdentification = currentPrideXmlReader.getIdentById(id);

        String database = gelFreeIdentification.getDatabase();
        Double score = gelFreeIdentification.getScore();
        String searchEngine = gelFreeIdentification.getSearchEngine();

        psmJPanel.setBorder(new TitledBorder("PSM Table  ("+currentProtein+" in "+database + " Searched by "+searchEngine + " Score: "+score + ")"));

        currentPeptideItemList = gelFreeIdentification.getPeptideItem();

        ArrayList<String> additionPara = new ArrayList<>();
        ArrayList<HashMap<String, String>> additionalParaMapList = new ArrayList<>();

        String peptideSequence;
        List<ModificationItem> modificationItemList;
        List<CvParam> cvParamList;
        List<CvParam> precursorCvParamList;
        HashMap<String, String> eachAdditionalPara;
        ArrayList<ModificationMatch> utilitiesModifications;
        PeptideAssumption peptideAssumption;
        Peptide peptide;
        SpectrumMatch spectrumMatch;
        Spectrum spectrum;
        Integer psmCount = 0;
        for (PeptideItem peptideItem : currentPeptideItemList){
            psmCount ++;
            peptideSequence = peptideItem.getSequence();

            modificationItemList = peptideItem.getModificationItem();

            utilitiesModifications = new ArrayList<>();

            for (ModificationItem modificationItem : modificationItemList){
                Integer location = modificationItem.getModLocation().intValue();

                Double monoMassDelta = Double.valueOf(modificationItem.getModMonoDelta().get(0));

                HashMap<Double, String > massModification;

                if(location == 0){
                    massModification = getModificationMass().get("N-terminus");
                    location = 1;

                }else if(location == peptideSequence.length() + 1){
                    massModification = getModificationMass().get("C-terminus");
                    location = peptideSequence.length();

                }else {
                    massModification = getModificationMass().get(peptideSequence.charAt(location - 1)+"");
                }

                String modificationName = null;

                for(Double mass: massModification.keySet()){
                    if (Math.abs(mass-monoMassDelta)<0.05){//Mass error may cause problem
                        modificationName = massModification.get(mass);
                    }
                }

                utilitiesModifications.add(new ModificationMatch(modificationName, true, location));
            }

            peptide = new Peptide(peptideSequence, utilitiesModifications);

            spectrumMatch = new SpectrumMatch();

            spectrum = peptideItem.getSpectrum();

            precursorCvParamList = spectrum.getSpectrumDesc().getPrecursorList().getPrecursor().get(0).getIonSelection().getCvParam();

            Integer charge = -1;
            Double mz = 0.0;
            for (CvParam cvParam : precursorCvParamList){
                if (cvParam.getAccession().contains("1000041")){
                    charge = Integer.valueOf(cvParam.getValue());
                } else if (cvParam.getAccession().contains("1000744")){
                    mz = Double.valueOf(cvParam.getValue());
                } else if (cvParam.getAccession().contains("1000040")){
                    mz = Double.valueOf(cvParam.getValue());
                }
            }

            currentMz.add(mz);

            peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge+1), 0.0, "");

            spectrumMatch.setBestPeptideAssumption(peptideAssumption);

            currentTableMatchList.add(spectrumMatch);

            currentPeptideList.add(peptide);

            currentTablePSMKeyList.add(String.valueOf(currentFileCount)+String.valueOf(currentProteinCount)+String.valueOf(psmCount));

            cvParamList = peptideItem.getAdditional().getCvParam();
            eachAdditionalPara = new HashMap<>();
            for (CvParam cvParam : cvParamList){
                if (!additionPara.contains(cvParam.getName())){
                    additionPara.add(cvParam.getName());
                }
                eachAdditionalPara.put(cvParam.getName(), cvParam.getValue());
            }

            additionalParaMapList.add(eachAdditionalPara);
        }

        setUpTableHeaderToolTips();
        psmJTableToolTips.addAll(additionPara);

        PSMTableMode psmTableMode = new PSMTableMode(currentPeptideList, additionPara, additionalParaMapList, currentPeptideItemList, searchParameters, currentFileCount, currentProteinCount);

        psmJTable.setModel(psmTableMode);

        psmJPanel.repaint();

        psmJTable.addRowSelectionInterval(0,0);

        psmJTableMouseReleased(null);
    }

    /**
     * Update spectrum table
     */
    private void updateSpectrumTable(){
        List<String> spectrumIDs = currentPrideXmlReader.getSpectrumIds();

        spectrumTableJPanel.setBorder(BorderFactory.createTitledBorder("Spectrum Table (" + spectrumIDs.size() + ")"));

        SpectrumTableModel spectrumTableModel = new SpectrumTableModel(currentPrideXmlReader, spectrumIDs);

        spectrumJTable.setModel(spectrumTableModel);

        spectrumTableJPanel.repaint();

        spectrumJTable.addRowSelectionInterval(0,0);

        spectrumJTableMouseReleased(null);
    }

    /**
     * Update file details
     */
    private void updateDetails(){

        StringBuilder allInfor = new StringBuilder("<html>");

        allInfor.append("<br><b>Title</b> : <br>");

        allInfor.append(currentPrideXmlReader.getExpTitle() +"<br>");

        if(currentPrideXmlReader.getExpShortLabel() != null ){
            allInfor.append("<br><b>Short Label</b> : <br>");
            allInfor.append(currentPrideXmlReader.getExpShortLabel() +"<br>");
        }

        Protocol currentProtocol = currentPrideXmlReader.getProtocol();

        allInfor.append("<br><b>Protocol Name</b> :"+currentProtocol.getProtocolName());

        List<Param> protocolSteps = currentProtocol.getProtocolSteps().getStepDescription();

        allInfor.append("<table border=\"1\">\n");
        for (Param param : protocolSteps){

            for(CvParam cvParam : param.getCvParam()){
                allInfor.append(" <tr>\n" +
                        "        <td>"+ cvParam.getName() +"</td>\n" +
                        "        <td>"+ cvParam.getValue() +"</td>\n" +
                        "    </tr>");
            }
        }
        allInfor.append("</table>");

        Admin currentAdmin = currentPrideXmlReader.getAdmin();

        allInfor.append("<br><b>Sample Name</b> :"+currentAdmin.getSampleName());

        allInfor.append("<table border=\"1\">\n");
        for (CvParam cvParam : currentAdmin.getSampleDescription().getCvParam()){
            allInfor.append(" <tr>\n" +
                    "        <td>"+ cvParam.getCvLabel() +"</td>\n" +
                    "        <td>"+ cvParam.getName() +"</td>\n" +
                    "    </tr>");
        }
        allInfor.append("</table>");

        allInfor.append("<table border=\"1\">\n");
        for (Contact contact : currentAdmin.getContact()){
            if(contact.getName() != null){
                allInfor.append(" <tr>\n" +
                        "        <td>"+ "Name" +"</td>\n" +
                        "        <td>"+ contact.getName() +"</td>\n" +
                        "    </tr>");
            }
            if(contact.getInstitution() != null){
                allInfor.append(" <tr>\n" +
                        "        <td>"+ "Institution" +"</td>\n" +
                        "        <td>"+ contact.getInstitution() +"</td>\n" +
                        "    </tr>");
            }
            if(contact.getContactInfo() != null){
                allInfor.append(" <tr>\n" +
                        "        <td>"+ "Contact Info" +"</td>\n" +
                        "        <td>"+ contact.getContactInfo() +"</td>\n" +
                        "    </tr>");
            }
        }
        allInfor.append("</table>");

        Instrument instrument = currentPrideXmlReader.getInstrument();

        allInfor.append("<br><b>Instrument Name</b> :"+instrument.getInstrumentName());

        allInfor.append("<table border=\"1\">\n");
        for(CvParam cvParam : instrument.getSource().getCvParam()){
            allInfor.append(" <tr>\n" +
                    "        <td>"+ "Source" +"</td>\n" +
                    "        <td>"+ cvParam.getName() +"</td>\n" +
                    "    </tr>");
        }
        Integer count = 1;
        for (Param param : instrument.getAnalyzerList().getAnalyzer()){
            for(CvParam cvParam : param.getCvParam()){
                allInfor.append(" <tr>\n" +
                        "        <td>"+ "Analyzer"+ count +"</td>\n" +
                        "        <td>"+ cvParam.getName() +"</td>\n" +
                        "    </tr>");
            }
            count ++;
        }

        for (CvParam cvParam : instrument.getDetector().getCvParam()){
            allInfor.append(" <tr>\n" +
                    "        <td>"+ "Detector" +"</td>\n" +
                    "        <td>"+ cvParam.getName() +"</td>\n" +
                    "    </tr>");
        }
        allInfor.append("</table>");

        DataProcessing currentDataProcessing = currentPrideXmlReader.getDataProcessing();

        allInfor.append("<br><b>Software </b> :"+currentDataProcessing.getSoftware().getName() + "-"+currentDataProcessing.getSoftware().getVersion());

        allInfor.append("<table border=\"1\">\n");
        allInfor.append(" <tr>\n" +
                "        <td>"+ "Processing Method" +"</td>\n" +
                "    </tr>");
        for(CvParam cvParam : currentDataProcessing.getProcessingMethod().getCvParam()){
            allInfor.append(" <tr>\n" +
                    "        <td>"+ cvParam.getName() +"</td>\n" +
                    "        <td>"+ cvParam.getValue() +"</td>\n" +
                    "    </tr>");
        }
        allInfor.append("</table>");

        allInfor.append("<br><b>Additional Details</b>: <br>");
        allInfor.append("<table border=\"1\">\n");
        for(CvParam cvParam : currentPrideXmlReader.getAdditionalParams().getCvParam()){
            allInfor.append(" <tr>\n" +
                    "        <td>"+ cvParam.getName() +"</td>\n" +
                    "        <td>"+ cvParam.getValue() +"</td>\n" +
                    "    </tr>");
        }
        allInfor.append("</table>");

        allInfor.append("<br></html>");

        inforJEditor.setText(allInfor.toString());
        inforJEditor.setCaretPosition(0);
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
     * PSM table model
     */
    public class PSMTableMode extends DefaultTableModel{

        /**
         * Current peptide list
         */
        private ArrayList<Peptide> currentPeptideList;
        /**
         * Additional parameters
         */
        private ArrayList<String> additionalPara;
        /**
         * Search parameters
         */
        private SearchParameters searchParameters;
        /**
         * Peptide item list
         */
        private java.util.List<PeptideItem> peptideItemList;
        /**
         * Current file count
         */
        private String currentFileCount;
        /**
         * Current protein count
         */
        private String currentProteinCount;
        /**
         * Additional parameters list
         */
        private ArrayList<HashMap<String, String>> additionalParaMapList;

        /**
         * Constructor
         * @param currentPeptideList Current peptide list
         * @param additionalPara Additional parameters list
         * @param additionalParaMapList Additional parameters map
         * @param peptideItemList Peptide item list
         * @param searchParameters Search parameters
         * @param currentFileCount Current file count
         * @param currentProteinCount Current protein count
         */
        public PSMTableMode(ArrayList<Peptide> currentPeptideList, ArrayList<String> additionalPara, ArrayList<HashMap<String, String>> additionalParaMapList,
                            java.util.List<PeptideItem> peptideItemList, SearchParameters searchParameters, Integer currentFileCount, Integer currentProteinCount){

            this.currentPeptideList = currentPeptideList;
            this.additionalPara = additionalPara;
            this.searchParameters = searchParameters;
            this.peptideItemList = peptideItemList;
            this.currentFileCount = String.valueOf(currentFileCount);
            this.currentProteinCount = String.valueOf(currentProteinCount);
            this.additionalParaMapList = additionalParaMapList;
        }

        @Override
        public int getRowCount() {
            if (currentPeptideList != null) {
                return currentPeptideList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            if(additionalPara != null) {
                return additionalPara.size()+7;
            } else {
                return 7;
            }
        }

        @Override
        public String getColumnName(int column) {

            if(column == 0){
                return "Key";
            }else if(column == 1){
                return "Sequence";
            }else if(column == 2){
                return "Rf Spectrum";
            }else if(column == 3){
                return "Charge";
            }else if(column == 4){
                return "m/z";
            }else if(column == 5){
                return "Rt (min)";
            }else if(column == 6){
                return "Comments";
            }
            for(int index = 0; index < additionalPara.size(); index++){
                int newColumn = index + 7;
                if(column == newColumn){
                    return additionalPara.get(index);
                }
            }

            return "";
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if(row < currentPeptideList.size()){
                    Peptide peptide = currentPeptideList.get(row);
                    PeptideItem peptideItem = peptideItemList.get(row);
                    Spectrum spectrum = peptideItem.getSpectrum();
                    List<CvParam> cvParamList = spectrum.getSpectrumDesc().getPrecursorList().getPrecursor().get(0).getIonSelection().getCvParam();
                    HashMap<String, String> eachRowAddition = additionalParaMapList.get(row);
                    if (column == 0){
                        return currentFileCount+currentProteinCount +String.valueOf(row+1);
                    }
                    if (column == 1){
                        return peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                    }
                    if (column == 2){
                        return spectrum.getId();
                    }
                    if (column == 3){
                        Integer charge = -1;
                        for (CvParam cvParam : cvParamList){

                            if (cvParam.getAccession().contains("1000041")){
                                charge = Integer.valueOf(cvParam.getValue());
                            }
                        }
                        return charge;
                    }
                    if (column == 4){

                        Double mz = -1.0;
                        for (CvParam cvParam : cvParamList){
                            if (cvParam.getAccession().contains("1000744")){
                                mz = Double.valueOf(cvParam.getValue());
                            } else if (cvParam.getAccession().contains("1000040")){
                                mz = Double.valueOf(cvParam.getValue());
                            }
                        }

                        return mz;
                    }
                    if (column == 5){
                        return -1;
                    }
                    if (column == 6){
                        List<String> comments = spectrum.getSpectrumDesc().getComments();
                        if (comments.size() != 0){
                            return comments.get(0);
                        } else {
                            return "";
                        }
                    }

                    for(int index = 0; index < additionalPara.size(); index++){

                        int newColumn = index + 7;
                        if(column == newColumn){

                            return eachRowAddition.get(additionalPara.get(index));
                        }
                    }
                } else {
                  return "";
                }
            } catch (Exception e){
                e.printStackTrace();
                return "";
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }

            return String.class;
        }

    }

    /**
     * Spectrum table model
     */
    public class SpectrumTableModel extends DefaultTableModel{

        /**
         * PrideXML reader
         */
        private PrideXmlReader prideXmlReader;
        /**
         * Spectrum ID list
         */
        private List<String> spectrumIDs;

        /**
         * Constructor
         * @param prideXmlReader PrideXML reader
         * @param spectrumIDs Spectrum ID lsit
         */
        public SpectrumTableModel(PrideXmlReader prideXmlReader, List<String> spectrumIDs){
            this.spectrumIDs = spectrumIDs;
            this.prideXmlReader = prideXmlReader;
        }

        @Override
        public int getRowCount() {
            if (spectrumIDs != null) {
                return spectrumIDs.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 9;
        }

        @Override
        public String getColumnName(int column) {

            if(column == 0){
                return "Spectrum ID";
            }else if(column == 1){
                return "MS Level";
            }else if(column == 2){
                return "Precursor Charge";
            }else if(column == 3){
                return "Precursor m/z";
            }else if(column == 4){
                return "mzRange start";
            }else if(column == 5){
                return "mzRange stop";
            }else if(column == 6){
                return "Peaks Num";
            }else if(column == 7) {
                return "Intensity Sum";
            }else if(column == 8) {
                return "Identified";
            }

            return "";
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                if(row < spectrumIDs.size()){
                    String spectrumID = spectrumIDs.get(row);

                    Spectrum spectrum = prideXmlReader.getSpectrumById(spectrumID);
                    SpectrumDesc spectrumDesc = spectrum.getSpectrumDesc();
                    SpectrumInstrument spectrumInstrument = spectrumDesc.getSpectrumSettings().getSpectrumInstrument();
                    uk.ac.ebi.pride.jaxb.model.Precursor precursor = spectrumDesc.getPrecursorList().getPrecursor().get(0);
                    List<CvParam> ionSelection = precursor.getIonSelection().getCvParam();

                    Number[] intensityArray = spectrum.getIntentArray();

                    if (column == 0){
                        return spectrum.getId();
                    }
                    if (column == 1){
                        return spectrumInstrument.getMsLevel();
                    }
                    if (column == 2){
                        Integer charge = -1;
                        for (CvParam cvParam : ionSelection){

                            if (cvParam.getAccession().contains("1000041")){
                                charge = Integer.valueOf(cvParam.getValue());
                            }
                        }
                        return charge;
                    }
                    if (column == 3){
                        Double mz = -1.0;
                        for (CvParam cvParam : ionSelection){
                            if (cvParam.getAccession().contains("1000744")){
                                mz = Double.valueOf(cvParam.getValue());
                            } else if (cvParam.getAccession().contains("1000040")){
                                mz = Double.valueOf(cvParam.getValue());
                            }
                        }

                        return mz;
                    }
                    if (column == 4){
                        return spectrumInstrument.getMzRangeStart();
                    }
                    if (column == 5){
                        return spectrumInstrument.getMzRangeStop();
                    }
                    if (column == 6){
                        return spectrum.getMzArrayBinary().getData().getLength();
                    }
                    if (column == 7){
                        Double sum = 0.0;
                        for(Number number : intensityArray){
                            sum  += number.doubleValue();
                        }

                        return sum;
                    }
                    if (column == 8){
                        List<String> comments = spectrum.getSpectrumDesc().getComments();
                        if (comments.size() != 0){
                            return comments.get(0);
                        } else {
                            return "";
                        }
                    }

                } else {
                    return "";
                }
            } catch (Exception e){
                e.printStackTrace();
                return "";
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }

            return String.class;
        }
    }
}

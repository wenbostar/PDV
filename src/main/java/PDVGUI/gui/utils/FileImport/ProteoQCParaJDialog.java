package PDVGUI.gui.utils.FileImport;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.PDVStart;
import PDVGUI.gui.utils.WaitingDialog;
import PDVGUI.utils.ProteoQCCMD;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * ProteoQC JDialog to input parameters
 * Created by Ken on 12/4/2017.
 */
public class ProteoQCParaJDialog extends JDialog {

    private JLabel spectralListFileJLabel;
    private JLabel fastaFileJLabel;
    private JLabel outdirJLabel;
    private JLabel missJLabel;
    private JLabel tolJLabel;
    private JLabel itolJLabel;
    private JLabel thresholdJLabel;
    private JTextField tolJText;
    private JTextField itolJText;
    private JTextField missJText;
    private JTextField thresholdJText;
    private JComboBox modeJCombo;
    private JComboBox toluJCombo;
    private JComboBox nttJCombo;
    private JComboBox itoluJCombo;
    private JComboBox refineJCombo;
    private JSpinner cpuJSpinner;
    private JSpinner xmxJSpinner;
    private JTable modificationsTable;
    private JButton removeFixedModification;
    private JButton removeVariableModification;
    private JButton startJButton;
    private JTable variableModsTable;
    private JTable fixedModsTable;
    private JTextField spectralListFileJText;
    private JTextField fastaFileJText;
    private JTextField outdirJText;

    /**
     * Last selected folder
     */
    private String lastSelectedFolder;
    /**
     * Selected fixed modification index
     */
    private ArrayList<String> fixedModsIndex = new ArrayList<>();
    /**
     * Selected variable modification index
     */
    private ArrayList<String> variableModsIndex = new ArrayList<>();
    /**
     * All modification index
     */
    private ArrayList<String> modificationsIndex = new ArrayList<>();
    /**
     * All modification
     */
    private HashMap<String, String[]> allModification = new HashMap<>();
    /**
     * The list file path
     */
    private String listFilePath;
    /**
     * The fasta file path
     */
    private String fastaFilePath;
    /**
     * The output folder path
     */
    private String outputPath;
    /**
     * The tolerance
     */
    private String tol = "10";
    /**
     * The ion tolerance
     */
    private String iTol = "0.6";
    /**
     * The threshold
     */
    private String threshold = "0.01";
    /**
     * The miss clevage
     */
    private String miss = "6";

    /**
     * Constructor
     * @param pdvStart parent frame
     */
    public ProteoQCParaJDialog(PDVStart pdvStart){
        super(pdvStart, true);

        setUpGui();
        validateInput();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvStart);
        setVisible(true);
    }

    /**
     * Set up the GUI
     */
    private void setUpGui(){
        initComponents();

        tolJText.setText("10");
        itolJText.setText("0.6");
        thresholdJText.setText("0.01");
        missJText.setText("2");

        getAllMods();
    }

    /**
     * Add all modifications of proteoQC
     */
    private void getAllMods(){

        allModification.put("1", new String[] {"57.021464@C", "Carbamidomethyl (C)"});
        allModification.put("2", new String[] {"15.994915@M", "Oxidation (M)"});
        allModification.put("3", new String[] {"0.984016@N", "Deamidated (N)"});
        allModification.put("4", new String[] {"0.984016@Q", "Deamidated (Q)"});
        allModification.put("5", new String[] {"304.205360@K", "iTRAQ8plex (K)"});
        allModification.put("6", new String[] {"304.205360@[", "iTRAQ8plex (N-term)"});
        allModification.put("7", new String[] {"304.205360@Y", "iTRAQ8plex (Y)"});
        allModification.put("8", new String[] {"144.102063@K", "iTRAQ4plex (K)"});
        allModification.put("9", new String[] {"144.102063@[", "iTRAQ4plex (N-term)"});
        allModification.put("10", new String[] {"144.102063@Y", "iTRAQ4plex (Y)"});
        allModification.put("11", new String[] {"229.162932@K", "TMT10plex (K)"});
        allModification.put("12", new String[] {"229.162932@[", "TMT10plex (N-term)"});
        allModification.put("13", new String[] {"79.966331@Y", "PhosY"});
        allModification.put("14", new String[] {"79.966331@S", "PhosS"});
        allModification.put("15", new String[] {"79.966331@T", "PhosT"});

        for (int i = 1; i < 16; i ++){
            modificationsIndex.add(String.valueOf(i));
        }
        modificationsTable.setModel(new ModificationTableModel(modificationsIndex));
        setTableUI();

        modificationsTable.repaint();
    }

    /**
     * Set selection table
     */
    private void setTableUI(){
        modificationsTable.getColumn(" ").setMaxWidth(20);
        modificationsTable.getColumn("index").setMaxWidth(40);
        modificationsTable.getColumn("modstring").setMaxWidth(140);
        variableModsTable.getColumn(" ").setMaxWidth(20);
        variableModsTable.getColumn("index").setMaxWidth(40);
        variableModsTable.getColumn("modstring").setMaxWidth(140);
        fixedModsTable.getColumn(" ").setMaxWidth(20);
        fixedModsTable.getColumn("index").setMaxWidth(40);
        fixedModsTable.getColumn("modstring").setMaxWidth(140);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){

        JPanel mainJPanel = new JPanel();
        JPanel inputFileJPanel = new JPanel();
        JPanel outputJPanel = new JPanel();
        JPanel parameterJPanel = new JPanel();
        JSplitPane paraSplitPane = new JSplitPane();
        JPanel firstParaJPanel = new JPanel();
        JPanel secondParaJPanel = new JPanel();
        JButton spectralListFileJButton = new JButton();
        JButton fastaFileJButton = new JButton();
        JButton outdirJButton = new JButton();
        JLabel modeJLabel = new JLabel();
        JLabel cpuJLabel = new JLabel();
        JLabel xmxJLabel = new JLabel();
        JLabel refineJLabel = new JLabel();
        JLabel nttJLabel = new JLabel();
        JPanel variableModsPanel = new JPanel();
        JPanel fixedModsPanel = new JPanel();
        JPanel availableModsPanel = new JPanel();
        JSplitPane modificationTypesSplitPane = new JSplitPane();
        JSplitPane modsSplitPane = new JSplitPane();
        JScrollPane modificationsJScrollPane = new JScrollPane();
        JScrollPane fixedModsJScrollPane = new JScrollPane();
        JScrollPane variableModsJScrollPane = new JScrollPane();
        JLabel fixedModificationsLabel = new JLabel();
        JLabel variableModificationsLabel = new JLabel();
        JButton addFixedModification = new JButton();
        JButton addVariableModification = new JButton();
        
        spectralListFileJText = new JTextField();
        fastaFileJText = new JTextField();
        outdirJText = new JTextField();
        tolJText = new JTextField();
        itolJText = new JTextField();
        missJText = new JTextField();
        thresholdJText = new JTextField();
        modeJCombo = new JComboBox();
        toluJCombo = new JComboBox();
        itoluJCombo = new JComboBox();
        nttJCombo = new JComboBox();
        refineJCombo = new JComboBox();
        cpuJSpinner = new JSpinner();
        xmxJSpinner = new JSpinner();
        spectralListFileJLabel = new JLabel();
        fastaFileJLabel = new JLabel();
        outdirJLabel = new JLabel();
        missJLabel = new JLabel();
        tolJLabel = new JLabel();
        itolJLabel = new JLabel();
        thresholdJLabel = new JLabel();
        removeFixedModification = new JButton();
        removeVariableModification = new JButton();
        startJButton = new JButton();
        modificationsTable = new JTable();
        variableModsTable = new JTable();
        fixedModsTable = new JTable();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - ProteoQC");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setMinimumSize(new java.awt.Dimension(700, 500));

        mainJPanel.setBackground(new Color(250, 250, 250));

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Input Files" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        inputFileJPanel.setBorder(titledBorder);
        inputFileJPanel.setOpaque(false);

        spectralListFileJLabel.setText("List File *");
        spectralListFileJLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        spectralListFileJLabel.setForeground(new Color(255, 0, 0));
        spectralListFileJLabel.setToolTipText("A file contains the experiment design or a single mgf file");

        spectralListFileJText.setEditable(false);
        spectralListFileJText.setText("No File Selected");
        spectralListFileJText.setHorizontalAlignment(JTextField.CENTER);

        spectralListFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        spectralListFileJButton.setBorder(null);
        spectralListFileJButton.setBorderPainted(false);
        spectralListFileJButton.setContentAreaFilled(false);
        spectralListFileJButton.addActionListener(this::spectralListFileJButtonActionPerformed);

        fastaFileJLabel.setText("Fasta File *");
        fastaFileJLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        fastaFileJLabel.setForeground(new Color(255, 0, 0));
        fastaFileJLabel.setToolTipText("database file, must contain decoy sequences");

        fastaFileJText.setEditable(false);
        fastaFileJText.setText("No File Selected");
        fastaFileJText.setHorizontalAlignment(JTextField.CENTER);

        fastaFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        fastaFileJButton.setBorder(null);
        fastaFileJButton.setBorderPainted(false);
        fastaFileJButton.setContentAreaFilled(false);
        fastaFileJButton.addActionListener(this::fastaFileJButtonActionPerformed);

        GroupLayout inputFileJPanelLayout = new GroupLayout(inputFileJPanel);
        inputFileJPanel.setLayout(inputFileJPanelLayout);

        inputFileJPanelLayout.setHorizontalGroup(
                inputFileJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFileJPanelLayout.createSequentialGroup()
                                .addGap(10)
                                .addComponent(spectralListFileJLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                .addComponent(spectralListFileJText, GroupLayout.PREFERRED_SIZE, 260, Short.MAX_VALUE)
                                .addComponent(spectralListFileJButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
                        .addGroup(inputFileJPanelLayout.createSequentialGroup()
                                .addGap(10)
                                .addComponent(fastaFileJLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                .addComponent(fastaFileJText, GroupLayout.PREFERRED_SIZE, 260, Short.MAX_VALUE)
                                .addComponent(fastaFileJButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
        );

        inputFileJPanelLayout.setVerticalGroup(
                inputFileJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFileJPanelLayout.createSequentialGroup()
                                .addGroup(inputFileJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(spectralListFileJLabel)
                                        .addComponent(spectralListFileJText)
                                        .addComponent(spectralListFileJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(inputFileJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(fastaFileJLabel)
                                        .addComponent(fastaFileJText)
                                        .addComponent(fastaFileJButton)))
        );

        TitledBorder outputJPanelTitledBorder = BorderFactory.createTitledBorder("Out path" + " \t ");
        outputJPanelTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        outputJPanel.setBorder(outputJPanelTitledBorder);
        outputJPanel.setOpaque(false);
        
        outdirJLabel.setText("Output Path *");
        outdirJLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        outdirJLabel.setForeground(new Color(255, 0, 0));
        outdirJLabel.setToolTipText("output directory");

        outdirJText.setEditable(false);
        outdirJText.setText("No File Selected");
        outdirJText.setHorizontalAlignment(JTextField.CENTER);

        outdirJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        outdirJButton.setBorder(null);
        outdirJButton.setBorderPainted(false);
        outdirJButton.setContentAreaFilled(false);
        outdirJButton.addActionListener(this::outdirJButtonActionPerformed);

        GroupLayout outputJPanelLayout = new GroupLayout(outputJPanel);
        outputJPanel.setLayout(outputJPanelLayout);

        outputJPanelLayout.setHorizontalGroup(
                outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(outputJPanelLayout.createSequentialGroup()
                                .addGap(10)
                                .addComponent(outdirJLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                .addComponent(outdirJText, GroupLayout.PREFERRED_SIZE, 260, Short.MAX_VALUE)
                                .addComponent(outdirJButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
        );
        outputJPanelLayout.setVerticalGroup(
                outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(outputJPanelLayout.createSequentialGroup()
                                .addGroup(outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(outdirJLabel)
                                        .addComponent(outdirJText)
                                        .addComponent(outdirJButton)))
        );

        TitledBorder parameterJPanelTitledBorder = BorderFactory.createTitledBorder("Parameters" + " \t ");
        parameterJPanelTitledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        parameterJPanel.setBorder(parameterJPanelTitledBorder);
        parameterJPanel.setOpaque(false);

        modsSplitPane.setBorder(null);
        modsSplitPane.setDividerLocation(350);
        modsSplitPane.setDividerSize(-1);
        modsSplitPane.setResizeWeight(0.5);
        modsSplitPane.setOpaque(false);

        modificationTypesSplitPane.setBorder(null);
        modificationTypesSplitPane.setDividerSize(0);
        modificationTypesSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        modificationTypesSplitPane.setDividerLocation(150);
        modificationTypesSplitPane.setResizeWeight(0.5);
        modificationTypesSplitPane.setOpaque(false);

        fixedModsPanel.setOpaque(false);

        fixedModificationsLabel.setFont(fixedModificationsLabel.getFont().deriveFont((fixedModificationsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        fixedModificationsLabel.setText("Fixed Modifications");

        addFixedModification.setText("<<");
        addFixedModification.setToolTipText("Add as fixed modification");
        addFixedModification.addActionListener(this::addFixedModificationActionPerformed);

        removeFixedModification.setText(">>");
        removeFixedModification.setToolTipText("Remove as fixed modification");
        removeFixedModification.setEnabled(false);
        removeFixedModification.addActionListener(this::removeFixedModificationActionPerformed);

        fixedModsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        fixedModsTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        " ", "index", "modstring", "name"
                }
        ) {
            Class[] types = new Class [] {
                    java.lang.Object.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                    false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        fixedModsJScrollPane.setViewportView(fixedModsTable);

        fixedModsTable.setRowHeight(20);
        fixedModsTable.setFont(new Font("Arial", Font.PLAIN, 10));
        fixedModsTable.getTableHeader().setFont(new Font("Dialog", 0, 12));

        GroupLayout fixedModsPanelLayout = new GroupLayout(fixedModsPanel);
        fixedModsPanel.setLayout(fixedModsPanelLayout);
        fixedModsPanelLayout.setHorizontalGroup(
                fixedModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fixedModsPanelLayout.createSequentialGroup()
                                .addGroup(fixedModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(fixedModsPanelLayout.createSequentialGroup()
                                                .addComponent(fixedModificationsLabel, GroupLayout.PREFERRED_SIZE, 125, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE))
                                        .addGroup(fixedModsPanelLayout.createSequentialGroup()
                                                .addComponent(fixedModsJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(7, 7, 7)))
                                .addGroup(fixedModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(removeFixedModification, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(addFixedModification, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)))
        );
        fixedModsPanelLayout.setVerticalGroup(
                fixedModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fixedModsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(fixedModificationsLabel)
                                .addGap(6, 6, 6)
                                .addGroup(fixedModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(fixedModsPanelLayout.createSequentialGroup()
                                                .addComponent(addFixedModification)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(removeFixedModification)
                                                .addContainerGap(76, Short.MAX_VALUE))
                                        .addComponent(fixedModsJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );

        modificationTypesSplitPane.setLeftComponent(fixedModsPanel);

        variableModsPanel.setOpaque(false);

        variableModificationsLabel.setFont(variableModificationsLabel.getFont().deriveFont((variableModificationsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        variableModificationsLabel.setText("Variable Modifications");

        addVariableModification.setText("<<");
        addVariableModification.setToolTipText("Add as variable modification");
        addVariableModification.addActionListener(this::addVariableModificationActionPerformed);

        removeVariableModification.setText(">>");
        removeVariableModification.setToolTipText("Remove as variable modification");
        removeVariableModification.setEnabled(false);
        removeVariableModification.addActionListener(this::removeVariableModificationActionPerformed);

        variableModsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        variableModsTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        " ", "index", "modstring", "name"
                }
        ) {
            Class[] types = new Class [] {
                    java.lang.Object.class, java.lang.String.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                    false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        variableModsJScrollPane.setViewportView(variableModsTable);

        variableModsTable.setRowHeight(20);
        variableModsTable.setFont(new Font("Arial", Font.PLAIN, 10));
        variableModsTable.getTableHeader().setFont(new Font("Dialog", 0, 12));

        GroupLayout variableModsPanelLayout = new GroupLayout(variableModsPanel);
        variableModsPanel.setLayout(variableModsPanelLayout);
        variableModsPanelLayout.setHorizontalGroup(
                variableModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(variableModificationsLabel)
                        .addGroup(GroupLayout.Alignment.TRAILING, variableModsPanelLayout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addComponent(variableModsJScrollPane, GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(variableModsPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(addVariableModification, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(removeVariableModification, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)))
        );
        variableModsPanelLayout.setVerticalGroup(
                variableModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(variableModsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(variableModificationsLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(variableModsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(variableModsPanelLayout.createSequentialGroup()
                                                .addComponent(addVariableModification)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(removeVariableModification))
                                        .addComponent(variableModsJScrollPane, GroupLayout.DEFAULT_SIZE, 102, Short.MAX_VALUE))
                                .addContainerGap())
        );

        modificationTypesSplitPane.setRightComponent(variableModsPanel);

        modsSplitPane.setLeftComponent(modificationTypesSplitPane);

        availableModsPanel.setOpaque(false);

        modificationsJScrollPane.setPreferredSize(new java.awt.Dimension(100, 60));

        modificationsTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        " ", "index", "modstring", "name"
                }
        ) {
            Class[] types = new Class [] {
                    java.lang.Object.class, java.lang.String.class, java.lang.Double.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                    false, false, false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });

        modificationsTable.setRowHeight(20);
        modificationsTable.setFont(new Font("Arial", Font.PLAIN, 10));
        modificationsTable.getTableHeader().setFont(new Font("Dialog", 0, 12));

        modificationsJScrollPane.setViewportView(modificationsTable);

        javax.swing.GroupLayout availableModsPanelLayout = new javax.swing.GroupLayout(availableModsPanel);
        availableModsPanel.setLayout(availableModsPanelLayout);
        availableModsPanelLayout.setHorizontalGroup(
                availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(availableModsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(modificationsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        availableModsPanelLayout.setVerticalGroup(
                availableModsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(availableModsPanelLayout.createSequentialGroup()
                                .addComponent(modificationsJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                                .addContainerGap())
        );

        modsSplitPane.setRightComponent(availableModsPanel);

        JPanel lineJPanel = new JPanel(){
            public void paint(Graphics g){

                try {

                    Graphics2D g2 = (Graphics2D) g;

                    g2.drawLine(20,0, 650,0);

                } catch (Exception e) {

                    e.printStackTrace();

                }
            }
        };

        paraSplitPane.setBorder(null);
        paraSplitPane.setDividerLocation(350);
        paraSplitPane.setDividerSize(-1);
        paraSplitPane.setResizeWeight(0.5);
        paraSplitPane.setOpaque(false);

        tolJLabel.setText("Tol *");
        tolJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        tolJLabel.setToolTipText("The error window on experimental peptide mass values");
        tolJLabel.setForeground(Color.red);

        tolJText.setEditable(true);
        tolJText.setBackground(Color.white);
        tolJText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                tolJTextKeyReleased(evt);
            }
        });

        toluJCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "Daltons" }));
        toluJCombo.setBackground(Color.WHITE);
        toluJCombo.setOpaque(false);
        toluJCombo.setToolTipText("Units can be selected from: ppm, Daltons(also da or Da).");
        toluJCombo.setSelectedIndex(0);

        modeJLabel.setText("Mode");
        modeJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        modeJLabel.setToolTipText("identification or quantification");

        modeJCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "identification", "quantification" }));
        modeJCombo.setBackground(Color.WHITE);
        modeJCombo.setOpaque(false);
        modeJCombo.setToolTipText("identification or quantification");
        modeJCombo.setSelectedIndex(1);

        thresholdJLabel.setText("threshold");
        thresholdJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        thresholdJLabel.setToolTipText("FDR value for PSM");
        thresholdJLabel.setForeground(Color.red);

        thresholdJText.setEditable(true);
        thresholdJText.setBackground(Color.white);
        thresholdJText.setToolTipText("FDR value for PSM");
        thresholdJText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                thresholdJTextKeyReleased(evt);
            }
        });

        nttJLabel.setText("ntt");
        nttJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        nttJLabel.setToolTipText("Semi-tryptic, 1; fully-tryptic, 2.");

        nttJCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Semi-tryptic", "fully-tryptic" }));
        nttJCombo.setBackground(Color.WHITE);
        nttJCombo.setOpaque(false);
        nttJCombo.setToolTipText("Semi-tryptic, 1; fully-tryptic, 2.");
        nttJCombo.setSelectedIndex(0);

        firstParaJPanel.setOpaque(false);
        GroupLayout firstParaJPanelLayout = new GroupLayout(firstParaJPanel);
        firstParaJPanel.setLayout(firstParaJPanelLayout);

        firstParaJPanelLayout.setHorizontalGroup(
                firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                .addGap(10,10,10)
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                                .addComponent(tolJLabel, 20, 20, 70)
                                                .addComponent(tolJText, 20, 20, 35)
                                                .addGap(5,5,5)
                                                .addComponent(toluJCombo, 20, 60, 70))
                                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                                .addComponent(itolJLabel, 20, 20, 70)
                                                .addComponent(itolJText, 20, 20, 35)
                                                .addGap(5,5,5)
                                                .addComponent(itoluJCombo, 20, 60, 70))
                                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                                .addComponent(refineJLabel, GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                                                .addComponent(refineJCombo, 20, 20, 100)))
                                .addGap(10,10,10)
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                                .addComponent(nttJLabel, GroupLayout.DEFAULT_SIZE, 20, 50)
                                                .addComponent(nttJCombo, GroupLayout.DEFAULT_SIZE, 100, 100))
                                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                                .addComponent(modeJLabel, GroupLayout.DEFAULT_SIZE, 20, 50)
                                                .addComponent(modeJCombo, GroupLayout.DEFAULT_SIZE, 100, 100)))
                                .addGap(10,10,10))

        );
        firstParaJPanelLayout.setVerticalGroup(
                firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(tolJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tolJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(toluJCombo, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(itolJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(itolJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(itoluJCombo, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(refineJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(refineJCombo, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
                        .addGroup(firstParaJPanelLayout.createSequentialGroup()
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(nttJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(nttJCombo, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(firstParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(modeJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(modeJCombo, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
        );
        
        paraSplitPane.setLeftComponent(firstParaJPanel);

        itolJLabel.setText("iTol *");
        itolJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        itolJLabel.setToolTipText("Error window for MS/MS fragment ion mass values.");
        itolJLabel.setForeground(Color.red);

        itolJText.setEditable(true);
        itolJText.setBackground(Color.white);
        itolJText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                itolJTextKeyReleased(evt);
            }
        });

        itoluJCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Daltons" }));
        itoluJCombo.setBackground(Color.WHITE);
        itoluJCombo.setOpaque(false);
        itoluJCombo.setToolTipText("Units can be selected from: Daltons(also da or Da)");
        itoluJCombo.setSelectedIndex(0);

        missJLabel.setText("Miss *");
        missJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        missJLabel.setToolTipText("max miss clevage");
        missJLabel.setForeground(Color.red);

        missJText.setBackground(Color.WHITE);
        missJText.setEditable(true);
        missJText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                missJTextKeyReleased(evt);
            }
        });

        refineJLabel.setText("refine");
        refineJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        refineJLabel.setToolTipText("Refine search for X!Tandem, default is TRUE");

        refineJCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TRUE", "FALSE" }));
        refineJCombo.setBackground(Color.WHITE);
        refineJCombo.setOpaque(false);
        refineJCombo.setToolTipText("Refine search for X!Tandem, default is TRUE");
        refineJCombo.setSelectedIndex(0);

        secondParaJPanel.setOpaque(false);
        GroupLayout secondParaJPanelLayout = new GroupLayout(secondParaJPanel);
        secondParaJPanel.setLayout(secondParaJPanelLayout);

        secondParaJPanelLayout.setHorizontalGroup(
                secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                .addGap(10,10,10)
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                                .addComponent(thresholdJLabel, GroupLayout.DEFAULT_SIZE, 40, 70)
                                                .addComponent(thresholdJText, 20, 20, 35)
                                                )
                                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                                .addComponent(missJLabel, GroupLayout.DEFAULT_SIZE, 40, 70)
                                                .addComponent(missJText, GroupLayout.DEFAULT_SIZE, 20, 35)
                                                ))
                                .addGap(10,10,10)
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                                .addComponent(cpuJLabel, 50, 100, 100)
                                                .addComponent(cpuJSpinner, 20, 40, 40))
                                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                                .addComponent(xmxJLabel, 50, 100, 100)
                                                .addComponent(xmxJSpinner, 20, 40, 40)))
                                .addGap(10,10,10))

        );
        secondParaJPanelLayout.setVerticalGroup(
                secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(thresholdJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(thresholdJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(missJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(missJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
                        .addGroup(secondParaJPanelLayout.createSequentialGroup()
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(cpuJSpinner, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cpuJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(secondParaJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(xmxJSpinner, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(xmxJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
        );

        paraSplitPane.setRightComponent(secondParaJPanel);

        cpuJLabel.setText("CPU num");
        cpuJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        cpuJLabel.setToolTipText("Max number of cpu used");

        cpuJSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        cpuJSpinner.setToolTipText("Max number of cpu used");

        xmxJLabel.setText("Max memory");
        xmxJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        xmxJLabel.setToolTipText("JAVA -Xmx");

        xmxJSpinner.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        xmxJSpinner.setToolTipText("JAVA -Xmx");

        GroupLayout parameterJPanelLayout = new GroupLayout(parameterJPanel);
        parameterJPanel.setLayout(parameterJPanelLayout);

        parameterJPanelLayout.setHorizontalGroup(
                parameterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(modsSplitPane)
                        .addComponent(lineJPanel)
                        .addComponent(paraSplitPane)

        );
        parameterJPanelLayout.setVerticalGroup(
                parameterJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(parameterJPanelLayout.createSequentialGroup()
                                .addComponent(modsSplitPane)
                                .addComponent(lineJPanel)
                                .addComponent(paraSplitPane)
                                )
        );

        startJButton.setBackground(Color.green);
        startJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        startJButton.setText("Start");
        startJButton.setEnabled(false);
        startJButton.setOpaque(false);
        startJButton.addActionListener(this::startJButtonActionPerformed);

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(inputFileJPanel)
                                        .addComponent(outputJPanel)
                                        .addComponent(parameterJPanel)
                                                .addComponent(startJButton)
                                        )
                                .addContainerGap())
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inputFileJPanel)
                                .addComponent(outputJPanel)
                                .addComponent(parameterJPanel)
                                .addComponent(startJButton)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }

    /**
     * startJButtonActionPerformed
     * @param e Mouse click event
     */
    private void startJButtonActionPerformed(ActionEvent e){

        String tolu = (String) toluJCombo.getSelectedItem();
        String itolu = (String) itoluJCombo.getSelectedItem();
        String ntt = String.valueOf(nttJCombo.getSelectedIndex()+1);
        String mode = (String) modeJCombo.getSelectedItem();
        String refine = (String) refineJCombo.getSelectedItem();
        String cpuNum = String.valueOf(cpuJSpinner.getValue());
        String xmx = String.valueOf(xmxJSpinner.getValue());
        StringBuilder fixedModIndex = null;
        StringBuilder variableModIndex = null;

        ArrayList<String> procCommands = new ArrayList<>();

        if (fixedModsIndex.size() != 0){
            fixedModIndex = new StringBuilder("(c");
            for (String each : fixedModsIndex){
                fixedModIndex.append(each).append(",");
            }
            fixedModIndex.delete(fixedModIndex.length()-1, fixedModIndex.length()-1);
            fixedModIndex.append(")");
        }
        if (variableModsIndex.size() != 0){
            variableModIndex = new StringBuilder("(c");
            for (String each : variableModsIndex){
                variableModIndex.append(each).append(",");
            }
            variableModIndex.delete(variableModIndex.length()-1, variableModIndex.length()-1);
            variableModIndex.append(")");
        }

        procCommands.add("Rscript");
        procCommands.add(new File(PDVMainClass.getJarFilePath()+"/resources/conf/proteoQC.R").getAbsolutePath());
        procCommands.add(listFilePath);
        procCommands.add(fastaFilePath);
        procCommands.add(outputPath);
        procCommands.add(mode);
        procCommands.add(miss);
        procCommands.add("1");
        procCommands.add(String.valueOf(variableModIndex));
        procCommands.add(String.valueOf(fixedModIndex));
        procCommands.add(tol);
        procCommands.add(tolu);
        procCommands.add(iTol);
        procCommands.add(itolu);
        procCommands.add(threshold);
        procCommands.add(cpuNum);
        procCommands.add(xmx);
        procCommands.add(refine);
        procCommands.add(ntt);

        WaitingDialog waitingDialog = new WaitingDialog(this);

        waitingDialog.appendReport("Run Run", true, true);

        ProteoQCCMD proteoQCCMD = new ProteoQCCMD(waitingDialog, procCommands);
        proteoQCCMD.execute();

        waitingDialog.setVisible(true);

        if(waitingDialog.isRunCanceled()){
            proteoQCCMD.cancel(true);
        }
    }

    /**
     * removeVariableModificationActionPerformed
     * @param evt Mouse click event
     */
    private void removeVariableModificationActionPerformed(ActionEvent evt) {
        int[] selectedRows = variableModsTable.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows){
            if (row != -1 ){

                selectedItems.add(String.valueOf(variableModsTable.getValueAt(row, 1)));

            }
        }

        modificationsIndex.addAll(selectedItems);
        variableModsIndex.removeAll(selectedItems);

        variableModsTable.setModel(new ModificationTableModel(variableModsIndex));
        modificationsTable.setModel(new ModificationTableModel(modificationsIndex));

        setTableUI();
        variableModsTable.repaint();
        modificationsTable.repaint();

        if (variableModsIndex.size() == 0){
            removeVariableModification.setEnabled(false);
        }
    }

    /**
     * addVariableModificationActionPerformed
     * @param evt Mouse click event
     */
    private void addVariableModificationActionPerformed(ActionEvent evt) {
        int[] selectedRows = modificationsTable.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows){
            if (row != -1 ){

                selectedItems.add(String.valueOf(modificationsTable.getValueAt(row, 1)));

            }
        }

        variableModsIndex.addAll(selectedItems);
        modificationsIndex.removeAll(selectedItems);

        variableModsTable.setModel(new ModificationTableModel(variableModsIndex));
        modificationsTable.setModel(new ModificationTableModel(modificationsIndex));

        setTableUI();
        variableModsTable.repaint();
        modificationsTable.repaint();

        removeVariableModification.setEnabled(true);

        if (variableModsIndex.size() == 0){
            removeVariableModification.setEnabled(false);
        }
    }

    /**
     * addFixedModificationActionPerformed
     * @param evt Mouse click event
     */
    private void addFixedModificationActionPerformed(ActionEvent evt) {
        int[] selectedRows = modificationsTable.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows){
            if (row != -1 ){

                selectedItems.add(String.valueOf(modificationsTable.getValueAt(row, 1)));

            }
        }

        fixedModsIndex.addAll(selectedItems);
        modificationsIndex.removeAll(selectedItems);

        fixedModsTable.setModel(new ModificationTableModel(fixedModsIndex));
        modificationsTable.setModel(new ModificationTableModel(modificationsIndex));

        setTableUI();
        fixedModsTable.repaint();
        modificationsTable.repaint();

        removeFixedModification.setEnabled(true);

        if (fixedModsIndex.size() == 0){
            removeFixedModification.setEnabled(false);
        }
    }

    /**
     * removeFixedModificationActionPerformed
     * @param evt Mouse click event
     */
    private void removeFixedModificationActionPerformed(ActionEvent evt) {
        int[] selectedRows = fixedModsTable.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows){
            if (row != -1 ){

                selectedItems.add(String.valueOf(fixedModsTable.getValueAt(row, 1)));

            }
        }

        modificationsIndex.addAll(selectedItems);
        fixedModsIndex.removeAll(selectedItems);

        fixedModsTable.setModel(new ModificationTableModel(fixedModsIndex));
        modificationsTable.setModel(new ModificationTableModel(modificationsIndex));

        setTableUI();
        fixedModsTable.repaint();
        modificationsTable.repaint();

        if (fixedModsIndex.size() == 0){
            removeFixedModification.setEnabled(false);
        }
    }

    /**
     * outdirJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void outdirJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select output directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()){
                outputPath = selectedFile.getAbsolutePath();
                lastSelectedFolder = selectedFile.getParent();
            }

            outdirJText.setText(selectedFile.getAbsolutePath());
            validateInput();
        }
    }

    /**
     * fastaFileJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void fastaFileJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select fasta file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".fasta")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " database file (.fasta)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()){
                fastaFilePath = selectedFile.getAbsolutePath();
                lastSelectedFolder = selectedFile.getParent();
            }

            fastaFileJText.setText(selectedFile.getName());
            validateInput();
        }
    }

    /**
     * spectralListFileJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void spectralListFileJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select spectra list file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".txt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " spectra list file (.txt)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()){
                listFilePath = selectedFile.getAbsolutePath();
                lastSelectedFolder = selectedFile.getParent();
            }

            spectralListFileJText.setText(selectedFile.getName());
            validateInput();
        }
    }

    /**
     * Validate the input items.
     */
    private void validateInput(){

        boolean allValid = true;

        if (listFilePath != null){
            spectralListFileJLabel.setForeground(Color.BLACK);
        } else {
            spectralListFileJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (fastaFilePath != null){
            fastaFileJLabel.setForeground(Color.BLACK);
        } else {
            fastaFileJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (outputPath != null){
            outdirJLabel.setForeground(Color.BLACK);
        } else {
            outdirJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (tol != null){
            tolJLabel.setForeground(Color.BLACK);
        } else {
            tolJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (iTol != null){
            itolJLabel.setForeground(Color.BLACK);
        } else {
            itolJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (threshold != null){
            thresholdJLabel.setForeground(Color.BLACK);
        } else {
            thresholdJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (miss != null){
            missJLabel.setForeground(Color.BLACK);
        } else {
            missJLabel.setForeground(Color.red);
            allValid = false;
        }


        startJButton.setEnabled(allValid);
    }

    /**
     * tolJTextKeyReleased
     * @param evt key event
     */
    private void tolJTextKeyReleased (KeyEvent evt) {
        if(!Objects.equals(tolJText.getText(), "") && tolJText.getText() != null){
            tol = tolJText.getText();
        } else {
            tol = null;
        }
        validateInput();
    }

    /**
     * thresholdJTextKeyReleased
     * @param evt key event
     */
    private void thresholdJTextKeyReleased(KeyEvent evt) {
        if(!Objects.equals(thresholdJText.getText(), "") && thresholdJText.getText() != null){
            threshold = thresholdJText.getText();
        } else {
            threshold = null;
        }
        validateInput();
    }

    /**
     * itolJTextKeyReleased
     * @param evt key event
     */
    private void itolJTextKeyReleased(KeyEvent evt) {
        if(!Objects.equals(itolJText.getText(), "") && itolJText.getText() != null){
            iTol = itolJText.getText();
        } else {
            iTol = null;
        }
        validateInput();
    }

    /**
     * missJTextKeyReleased
     * @param evt key event
     */
    private void missJTextKeyReleased(KeyEvent evt) {
        if(!Objects.equals(missJText.getText(), "") && missJText.getText() != null){
            miss = missJText.getText();
        } else {
            miss = null;
        }
        validateInput();
    }


    /**
     * Closes the dialog
     * @param evt Window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Modification tabel model
     */
    private class ModificationTableModel extends DefaultTableModel{

        /**
         * Modification index list
         */
        private ArrayList<String> indexList;

        /**
         * Constructor
         * @param indexList Modification index list
         */
        public ModificationTableModel(ArrayList<String> indexList){
            this.indexList = indexList;
        }

        @Override
        public int getRowCount() {
            if (indexList != null) {
                return indexList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column){
                case 0:
                    return " ";
                case 1:
                    return "index";
                case 2:
                    return "modstring";
                case 3:
                    return "name";
                default:
                        return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (row < indexList.size()){
                String index = indexList.get(row);
                String[] items = allModification.get(index);

                switch (column){
                    case 0:
                        return row;
                    case 1:
                        return index;
                    case 2:
                        return items[0];
                    case 3:
                        return items[1];
                    default:
                        return "";
                }
            } else {
                return "";
            }

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

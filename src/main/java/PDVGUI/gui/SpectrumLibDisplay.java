package PDVGUI.gui;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.fileimport.SpectrumLibrarySplibImport;
import PDVGUI.gui.utils.Export.ExportBatchDialog;
import PDVGUI.gui.utils.Export.RealTimeExportJDialog;
import PDVGUI.gui.utils.SpectrumMainPanel;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.SpecificAnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.spectrum_annotators.PeptideSpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.spectrum.ISpectrum;

import javax.sql.rowset.serial.SerialBlob;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Display spectrum library
 * Created by Ken on 9/21/2017.
 */
public class SpectrumLibDisplay extends JFrame {

    private JPanel psmJPanel;
    private JPanel spectrumShowJPanel;
    private JTable psmJTable;
    private JButton nextJButton;
    private JButton upJButton;
    public JTextField pageNumJTextField;
    private JTextField pageSelectNumJTextField;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;
    private JComboBox sortColumnJCombox;

    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Search parameters
     */
    public SearchParameters searchParameters;
    /**
     * Spectrum library file path
     */
    private String spectrumLibFilePath;
    /**
     * Database name
     */
    private String dbName;
    /**
     * All library ID list
     */
    public ArrayList<ArrayList<String>> allLibIDList = new ArrayList<>();
    /**
     * Selected library ID
     */
    private String selectedLibID;
    /**
     * Spectrum key to selected
     */
    private HashMap<String, Boolean> spectrumKeyToSelected =new HashMap<>();
    /**
     * All selections
     */
    private ArrayList<String> allSelections = new ArrayList<>();
    /**
     * Current page default 1
     */
    public int selectedPageNum = 1;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Export all or not
     */
    private Boolean exportAll = false;
    /**
     * Export selected or not
     */
    private Boolean exportSelection = false;
    /**
     * LastSelectFolder accessed easily
     */
    public LastSelectedFolder lastSelectedFolder = new LastSelectedFolder("user.home");
    /**
     * SpectrumTable tooltips list
     */
    private ArrayList<String> psmJTableToolTips;
    /**
     * Current identification assumption
     */
    private SpectrumIdentificationAssumption spectrumIdentificationAssumption = null;
    /**
     * UtilitiesUserPreferences
     */
    public UtilitiesUserPreferences utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
    /**
     * AnnotationSettings
     */
    public AnnotationSettings annotationSettings;

    /**
     * Constructor
     * @param spectrumLibFilePath Spectrum library file path
     * @param searchParameters Search parameters
     */
    public SpectrumLibDisplay(String spectrumLibFilePath, SearchParameters searchParameters, AnnotationSettings annotationSettings){

        this.spectrumLibFilePath = spectrumLibFilePath;
        this.searchParameters = searchParameters;
        this.annotationSettings = annotationSettings;

        spectrumMainPanel = new SpectrumMainPanel(this);

        initComponent();

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        setVisible(true);

        importSpectrumLib();
    }

    /**
     * Setup GUI
     */
    private void setupGUI(){

        psmJTable.setAutoCreateRowSorter(true);
        psmJTableToolTips = new ArrayList<>();

        psmJTable.getColumn("libID").setMaxWidth(50);
        psmJTable.getColumn("libID").setMinWidth(50);
        psmJTable.getColumn("Charge").setMaxWidth(60);
        psmJTable.getColumn("Charge").setMinWidth(50);
        psmJTable.getColumn("precursor m/z").setMaxWidth(150);
        psmJTable.getColumn("precursor m/z").setMinWidth(150);
        psmJTable.getColumn("mass error").setMaxWidth(100);
        psmJTable.getColumn("mass error").setMinWidth(100);
        psmJTable.getColumn("peaks num").setMaxWidth(100);
        psmJTable.getColumn("peaks num").setMinWidth(100);
        psmJTable.getColumn("Selected").setMinWidth(30);
        psmJTable.getColumn("Selected").setMaxWidth(30);

        psmJTableToolTips.add("Selected");
        psmJTableToolTips.add("Library ID");
        psmJTableToolTips.add("Peptide sequence");
        psmJTableToolTips.add("Precursor m/z");
        psmJTableToolTips.add("Precursor charge");
        psmJTableToolTips.add("Mass error");
        psmJTableToolTips.add("Peaks number");
        psmJTableToolTips.add("Protein");
    }

    /**
     * Init all GUI components
     */
    private void initComponent(){
        psmJPanel = new JPanel();
        spectrumShowJPanel = new JPanel();
        nextJButton = new JButton();
        upJButton = new JButton();
        pageNumJTextField = new JTextField();
        pageSelectNumJTextField = new JTextField();
        fragmentIonAccuracyTxt = new JTextField("0.5");
        precursorIonUnit = new JComboBox();
        sortColumnJCombox = new JComboBox();

        JPanel mainJPanel = new JPanel();
        JSplitPane allSplitPane = new JSplitPane();
        JScrollPane psmJScrollPane = new JScrollPane();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileJMenu = new JMenu();
        JMenuItem exitJMenuItem = new JMenuItem();
        JMenu exportJMenu = new JMenu();
        JMenuItem exportAllMenuItem = new JMenuItem();
        JMenuItem exportSelectedJMenuItem = new JMenuItem();
        JPanel settingJPanel = new JPanel();
        JButton setButton = new JButton();
        JButton upSortJButton = new JButton();
        JButton downSortJButton = new JButton();
        JLabel splitJLabel1 = new JLabel(" | ");
        JLabel splitJLabel2 = new JLabel(" | ");
        JLabel splitJLabel3 = new JLabel(" | ");
        JLabel splitJLabel4 = new JLabel(" | ");
        JLabel fragmentIonAccuracyJLabel = new JLabel("Fragment m/z Tolerance: ");
        JLabel fragmentIonType1Lbl = new JLabel("Fragment Ion Types: ");
        JLabel sortJLabel = new JLabel("Sort results: ");

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

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - SpectrumLib");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(760, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        exitJMenuItem.setMnemonic('E');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);

        fileJMenu.add(exitJMenuItem);

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

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

        menuBar.add(exportJMenu);

        setJMenuBar(menuBar);

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

        sortColumnJCombox.setModel(new DefaultComboBoxModel(new String[]{"LibID", "MZ", "Charge", "NumPeaks", "MassError"}));

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

        settingJPanel.setBackground(new Color(255, 255, 255));
        settingJPanel.setMinimumSize(new Dimension(20, 0));
        settingJPanel.setLayout(new BoxLayout(settingJPanel, BoxLayout.X_AXIS));

        settingJPanel.add(splitJLabel1);
        settingJPanel.add(fragmentIonType1Lbl);
        settingJPanel.add(precursorIonUnit);
        settingJPanel.add(splitJLabel2);
        settingJPanel.add(fragmentIonAccuracyJLabel);
        settingJPanel.add(fragmentIonAccuracyTxt);
        settingJPanel.add(setButton);
        settingJPanel.add(splitJLabel3);
        settingJPanel.add(sortJLabel);
        settingJPanel.add(sortColumnJCombox);
        settingJPanel.add(upSortJButton);
        settingJPanel.add(downSortJButton);

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainJPanel.setOpaque(true);
        mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Psm Table" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        psmJPanel.setBorder(titledBorder);
        psmJPanel.setBackground(Color.white);
        psmJPanel.setOpaque(false);

        psmJTable.setOpaque(false);
        psmJTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        psmJTable.addMouseListener(new MouseAdapter() {

            public void mouseExited(MouseEvent evt) {
                psmJTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                psmJTableMouseReleased(evt);
            }
        });

        psmJTable.setRowHeight(20);
        psmJTable.setFont(new Font("Arial", Font.PLAIN, 10));
        psmJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));

        psmJTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                psmJTableKeyReleased(evt);
            }
        });

        psmJScrollPane.setViewportView(psmJTable);
        psmJScrollPane.setOpaque(false);

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

        pageNumJTextField.setEditable(false);
        pageNumJTextField.setOpaque(false);
        pageNumJTextField.setBackground(Color.white);
        pageNumJTextField.setText(String.valueOf(selectedPageNum)+"/"+String.valueOf(allLibIDList.size()));
        pageNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);

        pageSelectNumJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        pageSelectNumJTextField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                pageSelectNumJTextFieldKeyReleased(evt);
            }
        });

        GroupLayout psmJPanelLayout = new GroupLayout(psmJPanel);
        psmJPanel.setLayout(psmJPanelLayout);

        psmJPanelLayout.setHorizontalGroup(
                psmJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(psmJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(psmJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
                        .addGroup(psmJPanelLayout.createSequentialGroup()
                                .addGap(100,1200,5000)
                                .addComponent(pageSelectNumJTextField,20, 30, 50)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(upJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nextJButton,GroupLayout.DEFAULT_SIZE, 10, 10)
                                .addComponent(pageNumJTextField,30, 50, 70))
        );
        psmJPanelLayout.setVerticalGroup(
                psmJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(psmJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(psmJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(psmJPanelLayout.createParallelGroup()
                                        .addComponent(pageSelectNumJTextField, 20, 20, 20)
                                        .addComponent(upJButton, 20, 20, 20)
                                        .addComponent(nextJButton, 20, 20, 20)
                                        .addComponent(pageNumJTextField, 20, 20, 20)))
        );

        allSplitPane.setBorder(null);
        allSplitPane.setDividerLocation(0.5);
        allSplitPane.setDividerSize(5);
        allSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
        allSplitPane.setResizeWeight(0.5);
        allSplitPane.setBackground(Color.white);
        allSplitPane.setOpaque(false);
        allSplitPane.setContinuousLayout(true);

        allSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
                evt -> spectrumMainPanel.updateSpectrum());

        allSplitPane.setTopComponent(psmJPanel);

        spectrumShowJPanel.setOpaque(false);
        spectrumMainPanel.setOpaque(false);
        spectrumShowJPanel.setBackground(Color.white);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowJPanel);
        spectrumShowJPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(spectrumMainPanel,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())

        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(spectrumMainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        allSplitPane.setRightComponent(spectrumShowJPanel);

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(settingJPanel)
                        .addComponent(allSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(settingJPanel, GroupLayout.DEFAULT_SIZE, 20, 20)
                                .addComponent(allSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);

        pack();
    }

    /**
     * Next and up button check
     */
    private void buttonCheck(){

        if (selectedPageNum == 1){
            upJButton.setEnabled(false);
        }else {
            upJButton.setEnabled(true);
        }

        if(pageNumJTextField.getText().contains(String.valueOf(allLibIDList.size())+"/")){
            nextJButton.setEnabled(false);
        }else {
            nextJButton.setEnabled(true);
        }
    }

    /**
     * upJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void upJButtonActionPerformed(ActionEvent evt){
        selectedPageNum --;
        pageNumJTextField.setText(String.valueOf(selectedPageNum)+"/"+String.valueOf(allLibIDList.size()));
        buttonCheck();

        updatePSMJTable();

        psmJTable.requestFocus();
        psmJTable.setRowSelectionInterval(0,0);
    }

    /**
     * nextJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void nextJButtonActionPerformed(ActionEvent evt){

        selectedPageNum ++;
        pageNumJTextField.setText(String.valueOf(selectedPageNum)+"/"+String.valueOf(allLibIDList.size()));
        buttonCheck();

        updatePSMJTable();

        psmJTable.requestFocus();
        psmJTable.setRowSelectionInterval(0,0);
    }

    /**
     * pageSelectNumJTextFieldKeyReleased
     * @param evt Mouse click event
     */
    private void pageSelectNumJTextFieldKeyReleased(KeyEvent evt){
        int keyChar = evt.getKeyChar();
        if(evt.getKeyCode()==10 || keyChar >= KeyEvent.VK_0 && keyChar <= KeyEvent.VK_9){
            if(evt.getKeyCode()==10){
                if(Integer.parseInt(pageSelectNumJTextField.getText())>allLibIDList.size() ||Integer.parseInt(pageSelectNumJTextField.getText())<1 ){
                    pageSelectNumJTextField.setBackground(Color.GRAY);
                }else {
                    pageSelectNumJTextField.setBackground(Color.WHITE);
                    selectedPageNum = Integer.parseInt(pageSelectNumJTextField.getText());
                    pageNumJTextField.setText(String.valueOf(selectedPageNum)+"/"+String.valueOf(allLibIDList.size()));
                    buttonCheck();

                    updatePSMJTable();

                    psmJTable.requestFocus();
                    psmJTable.setRowSelectionInterval(0,0);
                }
            }
        }else{
            pageSelectNumJTextField.setText("");
            evt.consume();
        }
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

        if(sqLiteConnection != null){
            sqLiteConnection.closeConnection();
            File dbFile = new File(dbName);
            if (dbFile.isFile() && dbFile.exists()) {
                dbFile.delete();
            }
        }

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
     * exportAllMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exportAllMenuItemActionPerformed(ActionEvent evt){

        exportAll = true;
        exportSelection = false;

        Integer size = 0;
        for (ArrayList<String> each : allLibIDList){
            size += each.size();
        }
        new ExportBatchDialog(this, size, true);
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
     * upSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void upSortJButtonActionPerform(ActionEvent evt){

        allLibIDList = new ArrayList<>();

        String selectedItem = (String) sortColumnJCombox.getSelectedItem();

        Connection connection = sqLiteConnection.getConnection();

        try {
            Statement statement = connection.createStatement();

            String selectOrder = "SELECT LibID FROM SpectrumMatch ORDER BY "+selectedItem;

            ResultSet rs1 = statement.executeQuery(selectOrder);

            String spectrumKey;
            ArrayList<String> each = new ArrayList<>();

            Integer count = 0;

            while (rs1.next()){

                spectrumKey = String.valueOf(rs1.getInt(1));

                each.add(spectrumKey);
                if(count == 1000){
                    allLibIDList.add(each);

                    each = new ArrayList<>();

                    count = 0;
                }
                count ++;
            }

            if(count != 0){
                allLibIDList.add(each);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        selectedPageNum = 1;

        updatePSMJTable();

        psmJTable.requestFocus();
        psmJTable.setRowSelectionInterval(0,0);
    }

    /**
     * downSortJButtonActionPerform
     * @param evt Mouse click event
     */
    private void downSortJButtonActionPerform(ActionEvent evt){

        allLibIDList = new ArrayList<>();

        String selectedItem = (String) sortColumnJCombox.getSelectedItem();

        Connection connection = sqLiteConnection.getConnection();

        try {
            Statement statement = connection.createStatement();

            String selectOrder = "SELECT LibID FROM SpectrumMatch ORDER BY "+selectedItem + " DESC ";

            ResultSet rs1 = statement.executeQuery(selectOrder);

            String spectrumKey;
            ArrayList<String> each = new ArrayList<>();

            Integer count = 0;

            while (rs1.next()){

                spectrumKey = String.valueOf(rs1.getInt(1));

                each.add(spectrumKey);
                if(count == 1000){
                    allLibIDList.add(each);

                    each = new ArrayList<>();

                    count = 0;
                }
                count ++;
            }

            if(count != 0){
                allLibIDList.add(each);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        selectedPageNum = 1;

        updatePSMJTable();

        psmJTable.requestFocus();
        psmJTable.setRowSelectionInterval(0,0);
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

            String libID = (String) psmJTable.getValueAt(row, 1);

            selectedLibID = String.valueOf(libID);

            MSnSpectrum currentSpectrum = null;

            SpectrumMatch currentSpectrumMatch = null;

            Connection connection = sqLiteConnection.getConnection();

            Statement statement = null;
            try {
                statement = connection.createStatement();

                String query = "SELECT Match , Spectrum FROM SpectrumMatch WHERE LibID = "+ libID;

                ResultSet rs1 = statement.executeQuery(query);

                while (rs1.next()){

                    Blob tempBlob;
                    byte[] bytes = rs1.getBytes(1);
                    tempBlob = new SerialBlob(bytes);
                    BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
                    try {
                        ObjectInputStream in = new ObjectInputStream(bis);
                        try {
                            currentSpectrumMatch = (SpectrumMatch) in.readObject();
                        } finally {
                            in.close();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    Blob scoreBlob;
                    byte[] scoreBytes = rs1.getBytes(2);
                    scoreBlob = new SerialBlob(scoreBytes);
                    BufferedInputStream scoreBis = new BufferedInputStream(scoreBlob.getBinaryStream());
                    try {
                        ObjectInputStream in = new ObjectInputStream(scoreBis);
                        try {
                            currentSpectrum = (MSnSpectrum) in.readObject();
                        } finally {
                            in.close();
                        }
                    } catch (ClassNotFoundException | IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            scoreBis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (column == psmJTable.getColumn("Selected").getModelIndex()) {
                if(!spectrumKeyToSelected.containsKey(libID)){
                    allSelections.add(libID);
                    spectrumKeyToSelected.put(libID, true);
                } else {
                    Boolean isSelected = spectrumKeyToSelected.get(libID);
                    if(isSelected){
                        spectrumKeyToSelected.put(libID, false);
                        allSelections.remove(libID);
                    } else {
                        allSelections.add(libID);
                        spectrumKeyToSelected.put(libID, true);
                    }
                }
                psmJTable.revalidate();
                psmJTable.repaint();
            }


            updateSpectrum(currentSpectrum, currentSpectrumMatch);

            this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    /**
     * Update spectrum according to the select psmKey
     * @param evt Key event
     */
    private void psmJTableKeyReleased(KeyEvent evt) {
        psmJTableMouseReleased(null);
    }

    /**
     * Update spectrum
     * @param mSnSpectrum MSN spectrum
     * @param spectrumMatch Spectrum match
     */
    private void updateSpectrum(MSnSpectrum mSnSpectrum, SpectrumMatch spectrumMatch){
        TitledBorder titledBorder = BorderFactory.createTitledBorder(spectrumMatch.getBestPeptideAssumption().getPeptide().getSequence() + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        spectrumShowJPanel.setBorder(titledBorder);

        spectrumMainPanel.updateSpectrum(spectrumMatch.getBestPeptideAssumption(), mSnSpectrum, selectedLibID);

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
    }

    /**
     * Import spectrum library
     */
    private void importSpectrumLib(){

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

                SpectrumLibrarySplibImport spectrumLibrarySplibImport;
                try {
                    spectrumLibrarySplibImport = new SpectrumLibrarySplibImport(new File(spectrumLibFilePath), SpectrumLibDisplay.this, progressDialog);

                    sqLiteConnection = spectrumLibrarySplibImport.getSqLiteConnection();

                    dbName = spectrumLibrarySplibImport.getDbName();

                } catch ( SQLException | ClassNotFoundException e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Display results
     */
    public void displayResults(){

        updatePSMJTable();

        psmJTableMouseExited(null);
    }

    /**
     * Update PSM table
     */
    private void updatePSMJTable(){

        buttonCheck();

        ArrayList<String> selectPageLibIDList = allLibIDList.get(selectedPageNum - 1);

        ArrayList<ArrayList<Object>> selectedItem = new ArrayList<>();

        for(String libID : selectPageLibIDList){
            try {
                selectedItem.add(sqLiteConnection.getOneItem(Integer.valueOf(libID)));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        psmJTable.setModel(new SpectrumLibTableModel(selectedItem, selectPageLibIDList, searchParameters, spectrumKeyToSelected));

        setupGUI();

        psmJTable.setRowSelectionInterval(0, 0);

        psmJTableMouseReleased(null);

        ((DefaultTableModel) psmJTable.getModel()).fireTableDataChanged();
        psmJPanel.repaint();
    }

    /**
     * Spectrum library table model
     */
    public class SpectrumLibTableModel extends DefaultTableModel{

        /**
         * Library ID list
         */
        private ArrayList<String> libIDList;
        /**
         * Search parameters
         */
        private SearchParameters searchParameters;
        /**
         * Selected item
         */
        private ArrayList<ArrayList<Object>> selectedItem;
        /**
         * Spectrum key to selected
         */
        private HashMap<String, Boolean> spectrumKeyToSelected;

        /**
         * Constructor
         * @param selectedItem Selected item
         * @param libIDList Library ID list
         * @param searchParameters Search parameters
         * @param spectrumKeyToSelected Spectrum key to selected
         */
        public SpectrumLibTableModel(ArrayList<ArrayList<Object>> selectedItem, ArrayList<String> libIDList, SearchParameters searchParameters, HashMap<String, Boolean> spectrumKeyToSelected){

            this.libIDList = libIDList;
            this.searchParameters = searchParameters;
            this.selectedItem = selectedItem;
            this.spectrumKeyToSelected = spectrumKeyToSelected;
        }

        @Override
        public int getRowCount() {
            if (libIDList != null) {
                return libIDList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 8;
        }

        @Override
        public String getColumnName(int column) {

            if(column == 0){
                return "Selected";
            }else if(column == 1){
                return "libID";
            }else if(column == 2){
                return "Sequence";
            }else if(column == 3){
                return "precursor m/z";
            }else if(column == 4){
                return "Charge";
            }else if(column == 5){
                return "mass error";
            }else if(column == 6){
                return "peaks num";
            }else if(column == 7){
                return "protein";
            }

            return "";
        }

        @Override
        public Object getValueAt(int row, int column) {

            try {

                if (row < libIDList.size()){
                    String libID = libIDList.get(row);

                    ArrayList<Object> oneItem = selectedItem.get(row);

                    SpectrumMatch spectrumMatch = (SpectrumMatch) oneItem.get(4);
                    Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

                    switch (column){
                        case 0:
                            return spectrumKeyToSelected.getOrDefault(libID, false);
                        case 1:
                            return libID;
                        case 2:
                            return peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                        case 3:
                            return oneItem.get(0);
                        case 4:
                            return oneItem.get(1);
                        case 5:
                            return oneItem.get(3);
                        case 6:
                            return oneItem.get(2);
                        case 7:
                            ArrayList<String> proteinList = (ArrayList<String>) oneItem.get(6);
                            StringBuilder allProteins = new StringBuilder();
                            for (String protein : proteinList){
                                 allProteins.append(protein);
                                 allProteins.append(" ");
                            }

                            return allProteins.toString();
                    }
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
     * Get spectrum
     * @param libID Library ID
     * @return MSnSpectrum
     */
    public MSnSpectrum getSpectrum(Integer libID){
        MSnSpectrum currentSpectrum = null;

        SpectrumMatch currentSpectrumMatch;

        Connection connection = sqLiteConnection.getConnection();

        Statement statement;
        try {
            statement = connection.createStatement();

            String query = "SELECT Match , Spectrum FROM SpectrumMatch WHERE LibID = "+ libID;

            ResultSet rs1 = statement.executeQuery(query);

            while (rs1.next()){

                Blob tempBlob;
                byte[] bytes = rs1.getBytes(1);
                tempBlob = new SerialBlob(bytes);
                BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
                try {
                    ObjectInputStream in = new ObjectInputStream(bis);
                    try {
                        currentSpectrumMatch = (SpectrumMatch) in.readObject();

                        spectrumIdentificationAssumption = currentSpectrumMatch.getBestPeptideAssumption();
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Blob scoreBlob;
                byte[] scoreBytes = rs1.getBytes(2);
                scoreBlob = new SerialBlob(scoreBytes);
                BufferedInputStream scoreBis = new BufferedInputStream(scoreBlob.getBinaryStream());
                try {
                    ObjectInputStream in = new ObjectInputStream(scoreBis);
                    try {
                        currentSpectrum = (MSnSpectrum) in.readObject();
                    } finally {
                        in.close();
                    }
                } catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        scoreBis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return currentSpectrum;
    }

    /**
     * Get SpectrumIdentificationAssumption
     * @return SpectrumIdentificationAssumption
     */
    public SpectrumIdentificationAssumption getSpectrumIdentificationAssumption(){
        return spectrumIdentificationAssumption;
    }

    /**
     * Export selected spectral
     * @param finalImageType Image type
     * @param outputFolder Output folder
     * @param picHeight Pic height
     * @param picWidth Pic width
     * @param unit Unit
     */
    public void exportSelectedSpectra(ImageType finalImageType, String outputFolder, Integer picHeight, Integer picWidth, String unit){

        Object[] allParameters = spectrumMainPanel.getParameters();

        RealTimeExportJDialog realTimeExportJDialog = new RealTimeExportJDialog((Integer) allParameters[0], (Integer) allParameters[1], picHeight, picWidth, unit,
                (PeptideSpectrumAnnotator) allParameters[2], (SpecificAnnotationSettings) allParameters[3], this, (HashMap<Double, String>)allParameters[4],
                (PtmSettings) allParameters[5], finalImageType, outputFolder);

        if (exportAll){
            realTimeExportJDialog.readAllSpectrums(allLibIDList);
        } else if (exportSelection){
            realTimeExportJDialog.readAllSelections(allSelections);
        } else {
            System.err.println("Exporting wrong");
        }
    }
}

package PDVGUI.gui.utils.Export;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.utils.ExportReports;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Export reports JDialog
 * Created by Ken on 12/19/2017.
 */
public class ExportReportsJDialog extends JDialog {

    private JLabel selectColumnJLabel;
    private JLabel outputJLabel;
    private JTable allColumnJTabel;
    private JTable selectedJTabel;
    private JButton exportJButton;
    private JButton removeColumnJButton;
    private JSpinner annotationLevelJSpinner;
    private JComboBox precursorIonUnit;
    private JTextField pathJText;
    private JTextField fragmentIonAccuracyTxt;

    /**
     * Selection size
     */
    private Integer selectionSize;
    /**
     * Last selected folder
     */
    private String lastFolder;
    /**
     * Output folder path
     */
    private String outputFolder;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * MS2 ion tolerance
     */
    private Double fragmentIonMZTolerance = 0.5;
    /**
     * Intensity annotation level
     */
    private Double annotationLevel = 0.05;
    /**
     * All export spectrum key
     */
    private ArrayList<String> exportSpectrumKey = new ArrayList<>();
    /**
     * All possible column
     */
    private ArrayList<String> allColumn;
    /**
     * Selected to output column
     */
    private ArrayList<String> selectedColumn = new ArrayList<>();

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param selectedSpetrums Selected spectrum keys
     * @param allColumn All column
     */
    public ExportReportsJDialog(PDVMainClass pdvMainClass, ArrayList<String> selectedSpetrums, ArrayList<String> allColumn){
        super(pdvMainClass, true);

        this.pdvMainClass = pdvMainClass;
        this.selectionSize = selectedSpetrums.size();
        this.exportSpectrumKey = selectedSpetrums;
        this.fragmentIonMZTolerance = pdvMainClass.getAnnotationSettings().getFragmentIonAccuracy();
        this.annotationLevel = pdvMainClass.getAnnotationSettings().getAnnotationIntensityFilter();

        this.allColumn = allColumn;

        setUpGui();

        setLocationRelativeTo(pdvMainClass);

        setVisible(true);
    }

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param allSpectrums All spectrum keys
     * @param allColumn All column
     * @param all Boolean
     */
    public ExportReportsJDialog(PDVMainClass pdvMainClass, ArrayList<ArrayList<String>> allSpectrums, ArrayList<String> allColumn, Boolean all){
        super(pdvMainClass, true);

        this.pdvMainClass = pdvMainClass;
        this.allColumn = allColumn;

        for (ArrayList<String> eachList : allSpectrums){
            exportSpectrumKey.addAll(eachList);
        }

        this.selectionSize = exportSpectrumKey.size();

        setUpGui();

        setLocationRelativeTo(pdvMainClass);

        setVisible(true);
    }

    /**
     * Set up the GUI
     */
    private void setUpGui() {
        initComponents();
        validateInput();

        pathJText.setText(" No Selection");
        fragmentIonAccuracyTxt.setText(String.valueOf(fragmentIonMZTolerance));
        annotationLevelJSpinner.setValue(annotationLevel);

        allColumnJTabel.setModel(new ColumnTableModel(allColumn));

        allColumnJTabel.getColumn(" ").setMaxWidth(20);

        allColumnJTabel.repaint();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){

        JPanel mainJPanel = new JPanel();
        JPanel outputJPanel = new JPanel();
        JPanel selectColumnJPanel = new JPanel();
        JPanel allColumnJPanel = new JPanel();
        JPanel parametersJPanel = new JPanel();
        JSplitPane columnJSplitPane = new JSplitPane();
        JButton pathBrowseJButton = new JButton();
        JButton addColumnJButton = new JButton();
        JScrollPane allColumnJScrollPane = new JScrollPane();
        JScrollPane selectedColumnJScrollPane = new JScrollPane();
        JLabel allColumnJLabel = new JLabel();
        JLabel inforJLabel = new JLabel();
        JLabel fragmentIonJLablel = new JLabel();
        JLabel annotationLevelJLabel = new JLabel();

        fragmentIonAccuracyTxt = new JTextField();
        precursorIonUnit = new JComboBox();
        annotationLevelJSpinner = new JSpinner();
        exportJButton = new JButton();
        removeColumnJButton = new JButton();
        pathJText = new JTextField();
        outputJLabel = new JLabel();
        allColumnJTabel = new JTable();
        selectedJTabel = new JTable();
        selectColumnJLabel = new JLabel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Export");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setMinimumSize(new java.awt.Dimension(500, 300));

        mainJPanel.setBackground(new Color(250, 250, 250));

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Output Path" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        outputJPanel.setBorder(titledBorder);
        outputJPanel.setOpaque(false);

        outputJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        outputJLabel.setText("Output Path");
        outputJLabel.setForeground(new Color(255, 0, 0));
        outputJLabel.setToolTipText("Select the output path. Result file named as input file and ends with .txt");

        pathJText.setEditable(false);
        pathJText.setHorizontalAlignment(JTextField.CENTER);

        pathBrowseJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        pathBrowseJButton.setBorder(null);
        pathBrowseJButton.setBorderPainted(false);
        pathBrowseJButton.setContentAreaFilled(false);
        pathBrowseJButton.addActionListener(this::pathBrowseJButtonActionPerformed);

        GroupLayout outputJPanelLayout = new GroupLayout(outputJPanel);
        outputJPanel.setLayout(outputJPanelLayout);

        outputJPanelLayout.setHorizontalGroup(
                outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(outputJPanelLayout.createSequentialGroup()
                                .addGap(10)
                                .addComponent(outputJLabel, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                .addComponent(pathJText, GroupLayout.PREFERRED_SIZE, 260, Short.MAX_VALUE)
                                .addComponent(pathBrowseJButton, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
        );
        outputJPanelLayout.setVerticalGroup(
                outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(outputJPanelLayout.createSequentialGroup()
                                .addGroup(outputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(outputJLabel)
                                        .addComponent(pathJText)
                                        .addComponent(pathBrowseJButton))
                                .addContainerGap())
        );

        titledBorder = BorderFactory.createTitledBorder("Parameters" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        parametersJPanel.setBorder(titledBorder);
        parametersJPanel.setOpaque(false);

        columnJSplitPane.setBorder(null);
        columnJSplitPane.setDividerLocation(0.5);
        columnJSplitPane.setDividerSize(-1);
        columnJSplitPane.setResizeWeight(0.5);
        columnJSplitPane.setOpaque(false);
        
        selectColumnJLabel.setFont(new Font("Console", Font.ITALIC, 12));
        selectColumnJLabel.setText("Selected column to export");

        selectColumnJPanel.setBorder(null);
        selectColumnJPanel.setOpaque(false);

        selectedColumnJScrollPane.setPreferredSize(new Dimension(100,60));

        selectedJTabel.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        " ", "Column Name"
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
        selectedColumnJScrollPane.setViewportView(selectedJTabel);

        selectedJTabel.setRowHeight(20);
        selectedJTabel.setFont(new Font("Arial", Font.PLAIN, 12));
        selectedJTabel.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));

        addColumnJButton.setText("<<");
        addColumnJButton.setToolTipText("Add as output reports column");
        addColumnJButton.addActionListener(this::addColumnJButtonnActionPerformed);

        removeColumnJButton.setText(">>");
        removeColumnJButton.setToolTipText("Remove as output reports column");
        removeColumnJButton.setEnabled(false);
        removeColumnJButton.addActionListener(this::removeColumnJButtonActionPerformed);

        GroupLayout selectColumnJPanelLayout = new GroupLayout(selectColumnJPanel);
        selectColumnJPanel.setLayout(selectColumnJPanelLayout);
        selectColumnJPanelLayout.setHorizontalGroup(
                selectColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(selectColumnJPanelLayout.createSequentialGroup()
                                .addComponent(selectColumnJLabel, GroupLayout.PREFERRED_SIZE, 125, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 50, Short.MAX_VALUE))
                        .addGroup(selectColumnJPanelLayout.createSequentialGroup()
                                .addGroup(selectColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(selectColumnJPanelLayout.createSequentialGroup()
                                                .addComponent(selectedColumnJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(7, 7, 7)))
                                .addGroup(selectColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(addColumnJButton, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(removeColumnJButton, GroupLayout.PREFERRED_SIZE, 54, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap())
        );
        selectColumnJPanelLayout.setVerticalGroup(
                selectColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(selectColumnJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(selectColumnJLabel)
                                .addGap(6, 6, 6)
                                .addGroup(selectColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(selectColumnJPanelLayout.createSequentialGroup()
                                                .addComponent(addColumnJButton)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(removeColumnJButton)
                                                .addContainerGap(76, Short.MAX_VALUE))
                                        .addComponent(selectedColumnJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        
        columnJSplitPane.setLeftComponent(selectColumnJPanel);

        allColumnJLabel.setFont(new Font("Console", Font.ITALIC, 12));
        allColumnJLabel.setText("All Columns");

        allColumnJPanel.setBorder(null);
        allColumnJPanel.setOpaque(false);

        allColumnJScrollPane.setPreferredSize(new Dimension(100,60));

        allColumnJTabel.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        " ", "Column Name"
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
        allColumnJScrollPane.setViewportView(allColumnJTabel);

        allColumnJTabel.setRowHeight(20);
        allColumnJTabel.setFont(new Font("Arial", Font.PLAIN, 12));
        allColumnJTabel.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));

        GroupLayout allColumnJPanelLayout = new GroupLayout(allColumnJPanel);
        allColumnJPanel.setLayout(allColumnJPanelLayout);

        allColumnJPanelLayout.setHorizontalGroup(
                allColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(allColumnJLabel, GroupLayout.PREFERRED_SIZE, 125, GroupLayout.PREFERRED_SIZE)
                        .addComponent(allColumnJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        allColumnJPanelLayout.setVerticalGroup(
                allColumnJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(allColumnJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(allColumnJLabel)
                                .addGap(6, 6, 6)
                                .addComponent(allColumnJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        columnJSplitPane.setRightComponent(allColumnJPanel);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        fragmentIonJLablel.setFont(new Font("Arial", Font.PLAIN, 12));
        fragmentIonJLablel.setText("Fragment m/z Tolerance");
        fragmentIonAccuracyTxt.setHorizontalAlignment(0);
        fragmentIonAccuracyTxt.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                fragmentIonAccuracyTxtKeyReleased(evt);
            }
        });

        annotationLevelJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        annotationLevelJLabel.setText("Annotation limit");
        annotationLevelJLabel.setToolTipText("Intensity percentile to ignore for annotation. e.g. 0.03 means that the 3% least intense peaks will be ignored.");
        annotationLevelJSpinner.setModel(new SpinnerNumberModel(Double.valueOf(0.75f), Double.valueOf(0.0f), Double.valueOf(1.0f), Double.valueOf(0.01f)));
        annotationLevelJSpinner.addChangeListener(this::annotationLevelJSpinnerValueChanged);

        GroupLayout parametersJPanelLayout = new GroupLayout(parametersJPanel);
        parametersJPanel.setLayout(parametersJPanelLayout);

        parametersJPanelLayout.setHorizontalGroup(
                parametersJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(columnJSplitPane)
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                                .addGroup(parametersJPanelLayout.createSequentialGroup()
                                        .addComponent(fragmentIonJLablel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                        .addComponent(fragmentIonAccuracyTxt, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                        .addComponent(precursorIonUnit, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED))
                                .addGroup(parametersJPanelLayout.createSequentialGroup()
                                        .addComponent(annotationLevelJLabel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                        .addGap(10, 100, 300)
                                        .addComponent(annotationLevelJSpinner, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                        .addGap(5,5,5)))
        );

        parametersJPanelLayout.setVerticalGroup(
                parametersJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(parametersJPanelLayout.createSequentialGroup()
                                .addComponent(columnJSplitPane)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(parametersJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addGroup(parametersJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(fragmentIonJLablel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(fragmentIonAccuracyTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(precursorIonUnit, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(parametersJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(annotationLevelJLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(annotationLevelJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))))
        );

        exportJButton.setBackground(Color.green);
        exportJButton.setFont(exportJButton.getFont().deriveFont(exportJButton.getFont().getStyle() | Font.BOLD));
        exportJButton.setText("Export");
        exportJButton.setEnabled(false);
        exportJButton.addActionListener(this::exportJButtonActionPerformed);

        inforJLabel.setText("There are "+selectionSize +" spectral you selected to export");

        inforJLabel.setFont(new Font("Arial", Font.ITALIC,12));

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(outputJPanel)
                                        .addComponent(parametersJPanel)
                                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                                .addComponent(inforJLabel, GroupLayout.DEFAULT_SIZE, 260, 500)
                                                .addComponent(exportJButton))
                                )
                                .addContainerGap())
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(outputJPanel)
                                .addComponent(parametersJPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(inforJLabel)
                                        .addComponent(exportJButton))
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
     * Closes the dialog
     * @param evt Window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Browse output folder path
     * @param evt Mouse click event
     */
    private void pathBrowseJButtonActionPerformed(ActionEvent evt){

        JFileChooser fileChooser = new JFileChooser(lastFolder);
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
                outputFolder = selectedFile.getAbsolutePath();
                lastFolder = selectedFile.getParent();
            }

            pathJText.setText(selectedFile.getAbsolutePath());
            validateInput();
        }

    }

    /**
     * addColumnJButtonnActionPerformed
     * @param evt Mouse click event
     */
    private void addColumnJButtonnActionPerformed(ActionEvent evt){

        int[] selectedRows = allColumnJTabel.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows) {
            if (row != -1) {
                selectedItems.add(String.valueOf(allColumnJTabel.getValueAt(row, 1)));
            }
        }

        selectedColumn.addAll(selectedItems);
        allColumn.removeAll(selectedItems);

        allColumnJTabel.setModel(new ColumnTableModel(allColumn));
        selectedJTabel.setModel(new ColumnTableModel(selectedColumn));

        allColumnJTabel.getColumn(" ").setMaxWidth(30);
        selectedJTabel.getColumn(" ").setMaxWidth(30);

        allColumnJTabel.repaint();
        selectedJTabel.repaint();

        removeColumnJButton.setEnabled(true);

        if (selectedColumn.size() == 0){
            removeColumnJButton.setEnabled(false);
        }

        validateInput();
    }

    /**
     * removeColumnJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void removeColumnJButtonActionPerformed(ActionEvent evt){

        int[] selectedRows = selectedJTabel.getSelectedRows();

        ArrayList<String> selectedItems = new ArrayList<>();

        for (int row : selectedRows) {
            if (row != -1) {
                selectedItems.add(String.valueOf(selectedJTabel.getValueAt(row, 1)));
            }
        }

        allColumn.addAll(selectedItems);
        selectedColumn.removeAll(selectedItems);

        allColumnJTabel.setModel(new ColumnTableModel(allColumn));
        selectedJTabel.setModel(new ColumnTableModel(selectedColumn));

        allColumnJTabel.getColumn(" ").setMaxWidth(30);
        selectedJTabel.getColumn(" ").setMaxWidth(30);

        allColumnJTabel.repaint();
        selectedJTabel.repaint();

        if (selectedColumn.size() == 0){
            removeColumnJButton.setEnabled(false);
        }

        validateInput();
    }

    /**
     * fragmentIonAccuracyTxtKeyReleased
     * @param evt key event
     */
    private void fragmentIonAccuracyTxtKeyReleased(KeyEvent evt) {
        if(!Objects.equals(fragmentIonAccuracyTxt.getText(), "") && fragmentIonAccuracyTxt.getText() != null){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        } else {
            fragmentIonMZTolerance = null;
        }
        validateInput();
    }

    /**
     * Select limit
     * @param evt Spinner click event
     */
    private void annotationLevelJSpinnerValueChanged(ChangeEvent evt) {
        annotationLevel = (Double) annotationLevelJSpinner.getValue();
    }

    /**
     * exportJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void exportJButtonActionPerformed(ActionEvent evt){
        new ExportReports(outputFolder, pdvMainClass, selectedColumn, exportSpectrumKey, annotationLevel, fragmentIonMZTolerance, precursorIonUnit.getSelectedIndex());
    }

    /**
     * Validate Input
     */
    private void validateInput(){

        boolean allValid = true;

        if (outputFolder != null){
            outputJLabel.setForeground(Color.BLACK);
        } else {
            outputJLabel.setForeground(Color.red);
            allValid = false;
        }
        if (selectedColumn.size() != 0){
            selectColumnJLabel.setForeground(Color.BLACK);
        } else {
            selectColumnJLabel.setForeground(Color.RED);
            allValid = false;
        }

        exportJButton.setEnabled(allValid);
    }

    /**
     * Column table
     */
    private class ColumnTableModel extends DefaultTableModel {

        /**
         * All column in table
         */
        private ArrayList<String> allColumnIn;

        /**
         * Constructor
         * @param allColumn All column
         */
        public ColumnTableModel(ArrayList<String> allColumn){
            this.allColumnIn = allColumn;
        }

        @Override
        public int getRowCount() {
            if (allColumnIn != null) {
                return allColumnIn.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column){
                case 0:
                    return " ";
                case 1:
                    return "Column Name";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            if (row < allColumnIn.size()){
                String index = allColumnIn.get(row);

                switch (column){
                    case 0:
                        return row;
                    case 1:
                        return index;
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

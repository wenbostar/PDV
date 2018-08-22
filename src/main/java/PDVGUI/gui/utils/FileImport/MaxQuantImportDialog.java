package PDVGUI.gui.utils.FileImport;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.PDVStart;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Select maxQuant file folder
 * Created by Ken on 8/4/2017.
 */
public class MaxQuantImportDialog extends JDialog {

    private JLabel maxQuantResultLabel;
    private JLabel fragmentIonJLable;
    private JTextField maxQuantResultTxt;
    private JButton startJButton;
    private JComboBox settingsComboBox;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;
    private JCheckBox existMGFJCheckBox;

    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * PTM factory import form utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Welcome dialog parent
     */
    private PDVStart pdvStart;
    /**
     * MS2 ion tolerance
     */
    private Double fragmentIonMZTolerance = 0.05;
    /**
     * MS2 ion tolerance type (ppm or dal)
     */
    private PDVMainClass.MassAccuracyType fragmentAccuracyType;
    /**
     * MaxQuant directory
     */
    private String maxQuantResultPath;
    /**
     * LastSelectedFolder for opening easily
     */
    private String lastSelectedFolder;

    /**
     * Constructor
     * @param pdvStart Welcome dialog
     * @param pdvMainClass Parent class
     */
    public MaxQuantImportDialog(PDVStart pdvStart, PDVMainClass pdvMainClass){
        super(pdvStart, true);
        this.pdvStart = pdvStart;
        this.pdvMainClass = pdvMainClass;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvStart);
        setVisible(true);
    }

    /**
     * Set up the GUI
     */
    private void setUpGui() {

        initComponents();
        this.precursorIonUnit.setEnabled(true);
        this.precursorIonUnit.setRenderer(new AlignedListCellRenderer(0));
        fragmentIonAccuracyTxt.setText(String.valueOf(0.05));

        settingsComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        maxQuantResultTxt.setText( "No selected");

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        precursorIonUnit = new JComboBox();
        fragmentIonAccuracyTxt = new JTextField();
        startJButton = new JButton();
        settingsComboBox = new JComboBox();
        maxQuantResultLabel = new JLabel();
        maxQuantResultTxt = new JTextField();
        existMGFJCheckBox = new JCheckBox();
        fragmentIonJLable = new JLabel();
        JPanel mainJPanel = new JPanel();
        JPanel annotationSettingJPanel = new JPanel();
        JPanel inputFilesPanel = new JPanel();
        JButton browseIdJButton = new JButton();
        JLabel existMGFJLabel = new JLabel("Exist mgf");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - MaxQuant Result Display");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainJPanel.setBackground(new Color(250, 250, 250));

        startJButton.setBackground(Color.green);
        startJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        startJButton.setText("Start");
        startJButton.setEnabled(false);
        startJButton.setOpaque(false);
        startJButton.addActionListener(this::startJButtonActionPerformed);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Project Settings" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        annotationSettingJPanel.setBorder(titledBorder);
        annotationSettingJPanel.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        precursorIonUnit.setOpaque(false);
        fragmentIonJLable.setFont(new Font("Arial", Font.PLAIN, 12));
        fragmentIonJLable.setText("Fragment m/z Tolerance");
        fragmentIonAccuracyTxt.setHorizontalAlignment(0);
        fragmentIonAccuracyTxt.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                fragmentIonAccuracyTxtKeyReleased(evt);
            }
        });

        GroupLayout annotationSettingJPanelLayout = new GroupLayout(annotationSettingJPanel);
        annotationSettingJPanel.setLayout(annotationSettingJPanelLayout);
        annotationSettingJPanelLayout.setHorizontalGroup(
                annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                                .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                                        .addComponent(fragmentIonJLable,  GroupLayout.PREFERRED_SIZE, 220, Short.MAX_VALUE))))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                                .addComponent(fragmentIonAccuracyTxt, GroupLayout.PREFERRED_SIZE, 69, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(precursorIonUnit, GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        annotationSettingJPanelLayout.setVerticalGroup(
                annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(fragmentIonJLable, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fragmentIonAccuracyTxt)
                                        .addComponent(precursorIonUnit))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titledBorder = BorderFactory.createTitledBorder("Input Files" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        inputFilesPanel.setBorder(titledBorder);
        inputFilesPanel.setOpaque(false);

        maxQuantResultLabel.setForeground(new Color(255, 0, 0));
        maxQuantResultLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        maxQuantResultLabel.setText("MaxQuant result *");

        maxQuantResultTxt.setHorizontalAlignment(JTextField.CENTER);

        browseIdJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseIdJButton.setBorder(null);
        browseIdJButton.setBorderPainted(false);
        browseIdJButton.setContentAreaFilled(false);
        browseIdJButton.addActionListener(this::browseIdJButtonActionPerformed);

        existMGFJLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        existMGFJLabel.setToolTipText("If you converted mgf files and had generatesMGF folder, select it.");
        existMGFJCheckBox.setOpaque(false);
        existMGFJCheckBox.setToolTipText("If you converted mgf files and had generatesMGF folder, select it.");

        GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(maxQuantResultLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(maxQuantResultTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(browseIdJButton)))
                                .addContainerGap())
        );

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseIdJButton});

        inputFilesPanelLayout.setVerticalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(maxQuantResultTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(maxQuantResultLabel)
                                        .addComponent(browseIdJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                                .addGap(5,8,10)
                                                .addComponent(existMGFJLabel)
                                                .addGap(5,8,10)
                                                .addComponent(existMGFJCheckBox)
                                                .addGap(100, 220, 500)
                                                .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15))
                                        .addComponent(inputFilesPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(annotationSettingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inputFilesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(annotationSettingJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(existMGFJLabel)
                                        .addComponent(existMGFJCheckBox))
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
     * @param evt window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Import all files and open newMain frame
     * @param evt mouse click event
     */
    private void startJButtonActionPerformed(java.awt.event.ActionEvent evt) {

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }
            File mgfDirectory = new File(maxQuantResultPath);
            Boolean existMGF = existMGFJCheckBox.isSelected();

            Boolean alreadyHas = false;

            for (File eachFile : mgfDirectory.listFiles()){
                if(eachFile.getName().equals("generatesMGF")){
                    alreadyHas = true;
                }
            }

            if(!existMGF && alreadyHas){
                int value = JOptionPane.showConfirmDialog(this,
                        "There are mgf files existed, do you want to generate again? ",
                        "Generate new MGF",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (value == JOptionPane.YES_OPTION) {
                    existMGF = false;
                    importResults(existMGF);
                } else if (value == JOptionPane.NO_OPTION) {
                    existMGF = true;
                    importResults(existMGF);
                }
            } else if (existMGF && !alreadyHas){
                existMGF = false;
                importResults(existMGF);

            } else {
                importResults(existMGF);
            }
    }

    /**
     * Import results
     * @param existMGF Boolean
     */
    private void importResults(Boolean existMGF){
        this.setVisible(false);
        pdvMainClass.setVisible(true);

        ProgressDialogX progressDialog = new ProgressDialogX(pdvMainClass,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Settings. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException e) {
            }
        }, "ProgressDialog").start();

        new Thread("SetUp"){
            @Override
            public void run(){

                ArrayList<String> modification =  ptmFactory.getPTMs();
                PtmSettings ptmSettings = new PtmSettings();

                for(String fixedModification:modification){
                    ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
                }

                for(String variableModification:modification){
                    ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
                }

                SearchParameters searchParameters = new SearchParameters();
                searchParameters.setPtmSettings(ptmSettings);
                if(precursorIonUnit.getSelectedIndex() == 0){
                    searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.DA);
                }else {
                    searchParameters.setFragmentAccuracyType(SearchParameters.MassAccuracyType.PPM);
                }
                searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);

                AnnotationSettings annotationPreferences = new AnnotationSettings();
                annotationPreferences.setPreferencesFromSearchParameters(searchParameters);
                annotationPreferences.setFragmentIonAccuracy(fragmentIonMZTolerance);
                pdvMainClass.setAnnotationSettings(annotationPreferences);
                pdvMainClass.setSearchParameters(searchParameters);

                if(precursorIonUnit.getSelectedIndex() == 0){
                    fragmentAccuracyType = PDVMainClass.MassAccuracyType.DA;
                }else {
                    fragmentAccuracyType = PDVMainClass.MassAccuracyType.PPM;
                }
                pdvMainClass.setFragmentAccuracyType(fragmentAccuracyType);

                progressDialog.setRunFinished();

                pdvMainClass.importMaxQuantResults(maxQuantResultPath, existMGF);

            }
        }.start();
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
     * Select all spectrum files
     * @param evt mouse click event
     */
    private void browseIdJButtonActionPerformed(ActionEvent evt) {

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Results Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        int returnValue = fileChooser.showDialog(this, "Add");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            maxQuantResultPath = selectedFile.getAbsolutePath();

            lastSelectedFolder = selectedFile.getParent();

            maxQuantResultTxt.setText(" File selected");
            validateInput();
        }
    }

    /**
     * Validates input information and enable start button
     */
    private void validateInput() {

        boolean allValid = true;

        if (maxQuantResultPath != null) {
            maxQuantResultLabel.setForeground(Color.BLACK);
            maxQuantResultLabel.setToolTipText(null);
        } else {
            maxQuantResultLabel.setForeground(Color.RED);
            maxQuantResultLabel.setToolTipText("Please select at least one identification file");
            maxQuantResultTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (fragmentIonMZTolerance != null) {
            fragmentIonJLable.setForeground(Color.BLACK);
            fragmentIonJLable.setToolTipText(null);
        } else {
            fragmentIonJLable.setForeground(Color.RED);
            fragmentIonJLable.setToolTipText("Please input MS2 ion tolerance!");
            allValid = false;
        }
        startJButton.setEnabled(allValid);
    }
}

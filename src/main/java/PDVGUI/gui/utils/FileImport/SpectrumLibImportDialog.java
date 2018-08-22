package PDVGUI.gui.utils.FileImport;

import PDVGUI.gui.PDVStart;
import PDVGUI.gui.SpectrumLibDisplay;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.preferences.DigestionPreferences;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Import spectrum library dialog
 * Created by Ken on 9/22/2017.
 */
public class SpectrumLibImportDialog extends JDialog {

    private JLabel spectrumLibResultLabel;
    private JTextField spectrumLibResultTxt;
    private JButton startJButton;
    private JComboBox settingsComboBox;
    private JLabel fragmentIonJLable;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;
    
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
     * spectrumLib directory
     */
    private String spectrumLibResultFilePath;
    /**
     * LastSelectedFolder for opening easily
     */
    private String lastSelectedFolder;

    /**
     * Constructor
     * @param pdvStart Parent class
     */
    public SpectrumLibImportDialog(PDVStart pdvStart){
        super(pdvStart, true);
        this.pdvStart = pdvStart;

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
        spectrumLibResultTxt.setText( "No selected");

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        fragmentIonJLable = new JLabel();
        precursorIonUnit = new JComboBox();
        fragmentIonAccuracyTxt = new JTextField();
        startJButton = new JButton();
        settingsComboBox = new JComboBox();
        spectrumLibResultLabel = new JLabel();
        spectrumLibResultTxt = new JTextField();
        JPanel mainJPanel = new JPanel();
        JPanel annotationSettingJPanel = new JPanel();
        JPanel inputFilesPanel = new JPanel();
        JButton browseIdJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - SpectrumLib Result Display");
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
        startJButton.setOpaque(false);
        startJButton.setEnabled(false);
        startJButton.addActionListener(evt -> startJButtonActionPerformed(evt));

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Project Settings" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        annotationSettingJPanel.setBorder(titledBorder);
        annotationSettingJPanel.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        fragmentIonJLable.setFont(new Font("Console", Font.PLAIN, 12));
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

        spectrumLibResultLabel.setForeground(new Color(255, 0, 0));
        spectrumLibResultLabel.setFont(new Font("Console", Font.PLAIN, 12));
        spectrumLibResultLabel.setText("Spectrum lib File *");

        spectrumLibResultTxt.setHorizontalAlignment(JTextField.CENTER);

        browseIdJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseIdJButton.setBorder(null);
        browseIdJButton.setBorderPainted(false);
        browseIdJButton.setContentAreaFilled(false);
        browseIdJButton.addActionListener(this::browseIdJButtonActionPerformed);


        GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(spectrumLibResultLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spectrumLibResultTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
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
                                        .addComponent(spectrumLibResultTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(spectrumLibResultLabel)
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
                                                .addGap(150, 220, 500)
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
                                        .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
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


        if (fragmentIonAccuracyTxt.getText()!= ""){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        }else {
            fragmentIonMZTolerance = 0.5;
        }

        SearchParameters searchParameters = new SearchParameters();

        if (searchParameters == null) {
            searchParameters = new SearchParameters();
            searchParameters.setDigestionPreferences(DigestionPreferences.getDefaultPreferences());
        }

        PtmSettings ptmSettings = new PtmSettings();

        ArrayList<String> modification =  ptmFactory.getPTMs();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }

        searchParameters.setPtmSettings(ptmSettings);

        SearchParameters.MassAccuracyType fragmentAccuracyType;
        if(precursorIonUnit.getSelectedIndex() == 0){
            fragmentAccuracyType = SearchParameters.MassAccuracyType.DA;
        }else {
            fragmentAccuracyType = SearchParameters.MassAccuracyType.PPM;
        }

        searchParameters.setFragmentAccuracyType(fragmentAccuracyType);
        searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);

        AnnotationSettings annotationPreferences = new AnnotationSettings();
        annotationPreferences.setPreferencesFromSearchParameters(searchParameters);
        annotationPreferences.setFragmentIonAccuracy(fragmentIonMZTolerance);

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

        new SpectrumLibDisplay(spectrumLibResultFilePath, searchParameters, annotationPreferences);
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
        fileChooser.setDialogTitle("Select Spectrum library");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".sptxt")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "spectrum library txt file .sptxt";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "Add");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            spectrumLibResultFilePath = selectedFile.getAbsolutePath();

            lastSelectedFolder = selectedFile.getParent();

            spectrumLibResultTxt.setText("File selected");
            validateInput();
        }
    }

    /**
     * Validates input information and enable start button
     */
    private void validateInput() {

        boolean allValid = true;

        if (spectrumLibResultFilePath != null) {
            spectrumLibResultLabel.setForeground(Color.BLACK);
            spectrumLibResultLabel.setToolTipText(null);
        } else {
            spectrumLibResultLabel.setForeground(Color.RED);
            spectrumLibResultLabel.setToolTipText("Please select at least one identification file");
            spectrumLibResultTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
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

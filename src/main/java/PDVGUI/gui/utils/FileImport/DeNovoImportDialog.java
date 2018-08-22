package PDVGUI.gui.utils.FileImport;

import PDVGUI.gui.*;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Selecting de novo identification file and spectrum file, then putting them into DB 
 * Created by Ken on 2016/11/11.
 */
public class DeNovoImportDialog extends JDialog {

    private JLabel idFileLabel;
    private JLabel spectrumFileLabel;
    private JTextField idFileJTextField;
    private JTextField spectrumFileJTextField;
    private JButton startJButton;
    private JComboBox precursorIonUnit;
    private JLabel fragmentIonLbl;
    private JTextField fragmentIonAccuracyTxt;

    /**
     * Identification file
     */
    private File identificationFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
    *  welcome dialog parent, can be null
     */
    private PDVStart pdvStart;
    /**
     * Old frame to restart
     */
    private JFrame oldJFrame;
    /**
     * LastSelectedFolder for opening easily
     */
    private String lastSelectedFolder;
    /**
     * MS2 ion tolerance
     */
    private Double fragmentIonMZTolerance = 0.05;
    /**
     * MS2 ion tolerance type (ppm or dal)
     */
    private PDVMainClass.MassAccuracyType fragmentAccuracyType;
    /**
     * PTMFactory import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;

    /**
     * New open dialog
     * @param pdvStart welcome dialog parent frame
     * @param pdvMainClass pdvMainClass
     */
    public DeNovoImportDialog(PDVStart pdvStart, PDVMainClass pdvMainClass) {
        super(pdvStart, true);
        this.pdvStart = pdvStart;
        this.pdvMainClass = pdvMainClass;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvStart);
        setVisible(true);
    }

    /**
     * New open dialog
     * @param pdvMainClass pdvMainClass dialog parent frame
     * @param modal boolean
     */
    public DeNovoImportDialog(PDVMainClass pdvMainClass, boolean modal, JFrame oldJFrame) {
        super(pdvMainClass, modal);
        this.pdvMainClass = pdvMainClass;
        this.pdvStart = null;
        this.oldJFrame = oldJFrame;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvMainClass);
        setVisible(true);
    }

    /**
     * New open dialog.
     * @param singlePeptideDisplay singlePeptideDisplay dialog parent frame
     * @param modal boolean
     */
    public DeNovoImportDialog(SinglePeptideDisplay singlePeptideDisplay, boolean modal, JFrame oldJFrame) {
        super(singlePeptideDisplay, modal);
        this.pdvStart = null;
        this.oldJFrame = oldJFrame;

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
        idFileJTextField.setText("No file selected");
        spectrumFileJTextField.setText("No file selected");
        fragmentIonAccuracyTxt.setText("0.05");
        precursorIonUnit.setEnabled(true);
        precursorIonUnit.setRenderer(new AlignedListCellRenderer(0));

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     * Init all GUI components
     */
    private void initComponents() {

        startJButton = new JButton();
        idFileLabel = new JLabel();
        spectrumFileLabel = new JLabel();
        idFileJTextField = new JTextField();
        spectrumFileJTextField = new JTextField();
        precursorIonUnit = new JComboBox();
        fragmentIonLbl = new JLabel();
        fragmentIonAccuracyTxt = new JTextField();
        JPanel inputJPanel = new JPanel();
        JPanel annotationSettingJPanel = new JPanel();
        JPanel mainJPanel = new JPanel();
        JButton browseIdJButton = new JButton();
        JButton browseSpectraJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - DeNovo Result Display");
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

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Input Files" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        inputJPanel.setBorder(titledBorder);
        inputJPanel.setOpaque(false);

        idFileLabel.setForeground(new Color(255, 0, 0));
        idFileLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        idFileLabel.setText("Identification File *");

        idFileJTextField.setEditable(false);
        idFileJTextField.setBackground(Color.WHITE);
        idFileJTextField.setOpaque(false);
        idFileJTextField.setHorizontalAlignment(JTextField.CENTER);

        browseIdJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseIdJButton.setBorder(null);
        browseIdJButton.setBorderPainted(false);
        browseIdJButton.setContentAreaFilled(false);
        browseIdJButton.addActionListener(this::browseIdJButtonActionPerformed);

        spectrumFileLabel.setForeground(new Color(255, 0, 0));
        spectrumFileLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        spectrumFileLabel.setText("Spectrum File *");

        spectrumFileJTextField.setEditable(false);
        spectrumFileJTextField.setBackground(Color.WHITE);
        spectrumFileJTextField.setOpaque(false);
        spectrumFileJTextField.setHorizontalAlignment(JTextField.CENTER);

        browseSpectraJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseSpectraJButton.setBorder(null);
        browseSpectraJButton.setBorderPainted(false);
        browseSpectraJButton.setContentAreaFilled(false);
        browseSpectraJButton.addActionListener(this::browseSpectraJButtonActionPerformed);

        titledBorder = BorderFactory.createTitledBorder("Project Settings" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        annotationSettingJPanel.setBorder(titledBorder);
        annotationSettingJPanel.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        precursorIonUnit.setBackground(Color.WHITE);
        precursorIonUnit.setOpaque(false);
        fragmentIonLbl.setText("Fragment m/z Tolerance");
        fragmentIonLbl.setFont(new Font("Arial", Font.PLAIN, 12));
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
                                                .addComponent(fragmentIonLbl,GroupLayout.PREFERRED_SIZE, 220, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(fragmentIonAccuracyTxt,GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(precursorIonUnit,GroupLayout.PREFERRED_SIZE, 70, GroupLayout.PREFERRED_SIZE))
                                       )
                                .addContainerGap())
        );

        annotationSettingJPanelLayout.setVerticalGroup(
                annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGroup(annotationSettingJPanelLayout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(annotationSettingJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addComponent(fragmentIonLbl)
                                    .addComponent(fragmentIonAccuracyTxt)
                                    .addComponent(precursorIonUnit))
                           )
        );
        
        GroupLayout inputFilesPanelLayout = new GroupLayout(inputJPanel);
        inputJPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(idFileLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(idFileJTextField,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(browseIdJButton))
                                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(spectrumFileLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spectrumFileJTextField,GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(browseSpectraJButton)))
                                .addContainerGap())
        );

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseIdJButton});

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseSpectraJButton});

        inputFilesPanelLayout.setVerticalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(idFileJTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(idFileLabel)
                                        .addComponent(browseIdJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(spectrumFileLabel)
                                        .addComponent(browseSpectraJButton)
                                        .addComponent(spectrumFileJTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
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
                                                .addGap(150, 220, 730)
                                                .addComponent(startJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15))
                                        .addComponent(inputJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(annotationSettingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        )
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inputJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(annotationSettingJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
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
     * @param evt Window click event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Import all files and open new De novo frame
     * @param evt mouse click event
     */
    private void startJButtonActionPerformed(ActionEvent evt) {

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

        if(oldJFrame != null){
            oldJFrame.setVisible(false);
        }

        this.setVisible(false);
        pdvMainClass.setVisible(true);

        ProgressDialogX progressDialog = new ProgressDialogX(pdvMainClass,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Data. Please Wait...");

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException e) {
            }
        }, "ProgressDialog").start();

        new Thread("SetUp") {
            @Override
            public void run() {

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

                if (identificationFile != null) {


                    SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
                    try {

                        if (spectrumFile.getName().endsWith(".dup")){
                            if (new File(spectrumFile.getAbsolutePath()+".mgf").exists()){
                                spectrumFile = new File(spectrumFile.getAbsolutePath()+".mgf");
                            } else {
                                FileUtils.copyFile(spectrumFile, new File(spectrumFile.getAbsolutePath()+".mgf"));
                                spectrumFile = new File(spectrumFile.getAbsolutePath()+".mgf");
                            }
                        }
                         spectrumFactory.addSpectra(spectrumFile);

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(DeNovoImportDialog.this, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        System.exit(1);
                    }

                    progressDialog.setRunFinished();

                    if (identificationFile.getName().endsWith(".tab")){
                        pdvMainClass.importDeepNovoResults(identificationFile, spectrumFile, spectrumFactory);
                    } else if (identificationFile.getName().endsWith(".tsk")){
                        pdvMainClass.importPNovoResults(identificationFile, spectrumFile, spectrumFactory);
                    } else {
                        pdvMainClass.importDeNovoResults(identificationFile, spectrumFile, spectrumFactory);
                    }

                }
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
     * Validates input information and enable start button
     */
    private void validateInput() {

        boolean allValid = true;
        
        if (identificationFile != null) {
            idFileLabel.setForeground(Color.BLACK);
            idFileLabel.setToolTipText(null);
        } else {
            idFileLabel.setForeground(Color.RED);
            idFileLabel.setToolTipText("Please select identification file");
            idFileJTextField.setToolTipText("Please select identification file");
            idFileJTextField.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (spectrumFile != null) {
            spectrumFileLabel.setForeground(Color.BLACK);
            spectrumFileLabel.setToolTipText(null);
        } else {
            spectrumFileLabel.setForeground(Color.RED);
            spectrumFileLabel.setToolTipText("Please select the spectrum file for the identification file");
            spectrumFileJTextField.setToolTipText("Please select the spectrum file for the identification file");
            spectrumFileJTextField.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (fragmentIonMZTolerance != null) {
            fragmentIonLbl.setForeground(Color.BLACK);
            fragmentIonLbl.setToolTipText(null);
        } else {
            fragmentIonLbl.setForeground(Color.RED);
            fragmentIonLbl.setToolTipText("Please input MS2 ion tolerance!");
            allValid = false;
        }

        startJButton.setEnabled(allValid);
    }

    /**
     * Import identification file
     * @param evt mouse click event
     */
    private void browseIdJButtonActionPerformed(ActionEvent evt) {

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Identification File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return   myFile.getName().toLowerCase().endsWith(".tags")
                        || myFile.getName().toLowerCase().endsWith("novor.csv")
                        || myFile.getName().toLowerCase().endsWith(".out")
                        || myFile.getName().toLowerCase().endsWith(".tab")
                        || myFile.getName().toLowerCase().endsWith(".tsk")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " pepNovo+ (.out), DirecTag (.tags), DeepNovo (.tab), pNovo (.tsk) and Novor (.novor.csv)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null) {
                lastSelectedFolder = selectedFile.getParent();
                identificationFile = selectedFile;

                idFileJTextField.setText(identificationFile.getName() + " selected");

                validateInput();
            }
        }
    }

    /**
     * Import spectrum file
     * @param evt mouse click event
     */
    private void browseSpectraJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Spectrum File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.getName().toLowerCase().endsWith(".dup")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File selectedFile : selectedFiles) {
                if (selectedFile.exists()) {
                    spectrumFile = selectedFile;
                    lastSelectedFolder = selectedFile.getParent();
                }

            }
            spectrumFileJTextField.setText(spectrumFile.getName() + " selected.");
            validateInput();
        }
    }
}

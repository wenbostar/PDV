package PDVGUI.gui.utils.FileImport;

import PDVGUI.fileimport.MzXMLScanImport;
import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.PDVStart;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzidentml.MzIdentMLParser;
import umich.ms.fileio.filetypes.mzidentml.jaxb.standard.MzIdentMLType;
import umich.ms.fileio.filetypes.mzml.MZMLFile;
import umich.ms.fileio.filetypes.mzxml.MZXMLFile;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Dialog to import probam file and spectrum file
 * Created by Ken on 7/20/2017.
 */
public class ProBamImportDialog extends JDialog{

    private JLabel proGenomicsFileLabel;
    private JLabel fragmentIonLbl;
    private JLabel mzIDFileJLabel;
    private JLabel spectrumFilesLabel;
    private JTextField proGenomicsFileTxt;
    private JTextField mzIDFileJText;
    private JTextField fragmentIonAccuracyTxt;
    private JTextField spectrumFilesTxt;
    private JButton startJButton;
    private JButton mzIDFileBrowseJButton;
    private JButton browseProBAMJButton;
    private JButton browseSpectraJButton;
    private JComboBox settingsComboBox;
    private JComboBox fileTypeCombox;
    private JComboBox precursorIonUnit;
    private JPanel inputFilesPanel;
    private JPanel mainJPanel;

    /**
     *Identification file
     */
    private File proGenomicsFile;
    /**
     * Spectrum file.
     */
    private File spectrumFile;
    /**
     * MzID file
     */
    private File mzIDFile;
    /**
     * LastSelectedFolder for opening easily
     */
    private String lastSelectedFolder;
    /**
     * Welcome dialog parent, can be null
     */
    private PDVStart pdvStart;
    /**
     * MS2 ion tolerance
     */
    private Double fragmentIonMZTolerance = 0.5;
    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * PTM factory import form utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * MS2 ion tolerance type (ppm or dal)
     */
    private PDVMainClass.MassAccuracyType fragmentAccuracyType;
    /**
     * Spectrum file type
     */
    private String spectrumFileType;
    /**
     * Spectrum Object
     */
    private Object spectrumsFileFactory;
    /**
     * File type
     */
    private String typeSelection = "proBAM";
    /**
     * Object to saving identification results
     */
    private MzIdentMLType mzIdentMLType = null;
    /**
     * Check if all identification imported done or not
     */
    private int mzIdentMLCheck = 0;

    /**
     * New dialog constructor
     * @param pdvStart welcome Dialog parent frame
     */
    public ProBamImportDialog(PDVStart pdvStart, PDVMainClass pdvMainClass){

        super(pdvStart, true);
        this.pdvStart = pdvStart;
        this.pdvMainClass = pdvMainClass;

        setUpGui();
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        this.setLocationRelativeTo(pdvStart);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {

        initComponents();
        this.precursorIonUnit.setEnabled(true);
        this.precursorIonUnit.setRenderer(new AlignedListCellRenderer(0));
        fragmentIonAccuracyTxt.setText(String.valueOf(0.5));
        settingsComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        fileTypeCombox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        proGenomicsFileTxt.setText( "No selected");
        spectrumFilesTxt.setText("No selected");
        mzIDFileJText.setText("No selected");

        validateInput();
        GuiUtilities.installEscapeCloseOperation(this);
    }

    /**
     *Init all GUI components
     */
    private void initComponents() {
        precursorIonUnit = new JComboBox();
        fragmentIonAccuracyTxt = new JTextField();
        startJButton = new JButton();
        browseProBAMJButton = new JButton();
        browseSpectraJButton = new JButton();
        settingsComboBox = new JComboBox();
        fileTypeCombox = new JComboBox();
        proGenomicsFileLabel = new JLabel();
        proGenomicsFileTxt = new JTextField();
        spectrumFilesLabel = new JLabel();
        spectrumFilesTxt = new JTextField();
        fragmentIonLbl = new JLabel();
        mzIDFileJLabel = new JLabel();
        mzIDFileJText = new JTextField();
        mzIDFileBrowseJButton = new JButton();
        inputFilesPanel = new JPanel();
        mainJPanel = new JPanel();
        JPanel annotationSettingJPanel = new JPanel();
        JPanel fileTypSelectionPanel = new JPanel();
        JLabel fileTypeJLabel = new JLabel("File types");

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - ProBam File Display");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        mainJPanel.setBackground(new Color(250, 250, 250));

        startJButton.setBackground(Color.GREEN);
        startJButton.setFont( new Font("Lucida", Font.BOLD, 13));
        startJButton.setText("Start");
        startJButton.setOpaque(false);
        startJButton.setEnabled(false);
        startJButton.addActionListener(this::startJButtonActionPerformed);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Project Settings" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        annotationSettingJPanel.setBorder(titledBorder);
        annotationSettingJPanel.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        precursorIonUnit.setBackground(Color.WHITE);
        precursorIonUnit.setOpaque(false);
        fragmentIonLbl.setFont(new Font("Arial", Font.PLAIN, 12));
        fragmentIonLbl.setText("Fragment m/z Tolerance");
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
                                                        .addComponent(fragmentIonLbl,  GroupLayout.PREFERRED_SIZE, 220, Short.MAX_VALUE))))
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
                                        .addComponent(fragmentIonLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fragmentIonAccuracyTxt)
                                        .addComponent(precursorIonUnit))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titledBorder = BorderFactory.createTitledBorder("Input Files" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        inputFilesPanel.setBorder(titledBorder);
        inputFilesPanel.setOpaque(false);

        mzIDFileJLabel.setForeground(new Color(255, 0, 0));
        mzIDFileJLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mzIDFileJLabel.setText("mzID Results File *");

        mzIDFileJText.setHorizontalAlignment(JTextField.CENTER);
        mzIDFileJText.setEditable(false);
        mzIDFileJText.setBackground(Color.white);

        mzIDFileBrowseJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        mzIDFileBrowseJButton.setBorder(null);
        mzIDFileBrowseJButton.setBorderPainted(false);
        mzIDFileBrowseJButton.setContentAreaFilled(false);
        mzIDFileBrowseJButton.addActionListener(this::mzIDFileBrowseJButtonActionPerformed);

        proGenomicsFileLabel.setForeground(new Color(255, 0, 0));
        proGenomicsFileLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        proGenomicsFileLabel.setText("proBAM File *");

        proGenomicsFileTxt.setHorizontalAlignment(JTextField.CENTER);

        browseProBAMJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseProBAMJButton.setBorder(null);
        browseProBAMJButton.setBorderPainted(false);
        browseProBAMJButton.setContentAreaFilled(false);
        browseProBAMJButton.addActionListener(this::browseProBAMJButtonActionPerformed);

        spectrumFilesLabel.setForeground(new Color(255, 0, 0));
        spectrumFilesLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        spectrumFilesLabel.setText("Spectrum File *");

        spectrumFilesTxt.setEditable(false);
        spectrumFilesTxt.setOpaque(false);
        spectrumFilesTxt.setBackground(Color.white);
        spectrumFilesTxt.setHorizontalAlignment(JTextField.CENTER);

        browseSpectraJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        browseSpectraJButton.setBorder(null);
        browseSpectraJButton.setBorderPainted(false);
        browseSpectraJButton.setContentAreaFilled(false);
        browseSpectraJButton.addActionListener(this::browseSpectraJButtonActionPerformed);

        GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
        inputFilesPanel.setLayout(inputFilesPanelLayout);
        inputFilesPanelLayout.setHorizontalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(proGenomicsFileLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(proGenomicsFileTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(browseProBAMJButton))
                                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                                .addComponent(spectrumFilesLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(spectrumFilesTxt,GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(browseSpectraJButton)))
                                .addContainerGap())
        );

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseProBAMJButton});

        inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseSpectraJButton});

        inputFilesPanelLayout.setVerticalGroup(
                inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(proGenomicsFileTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(proGenomicsFileLabel)
                                        .addComponent(browseProBAMJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(spectrumFilesLabel)
                                        .addComponent(browseSpectraJButton)
                                        .addComponent(spectrumFilesTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        titledBorder = BorderFactory.createTitledBorder("Types selection" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        fileTypSelectionPanel.setBorder(titledBorder);
        fileTypSelectionPanel.setOpaque(false);

        fileTypeJLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        fileTypeCombox.setModel(new DefaultComboBoxModel(new String[]{"proBAM", "proBed"}));
        fileTypeCombox.setBackground(Color.WHITE);
        fileTypeCombox.setOpaque(false);
        fileTypeCombox.addItemListener(this::fileTypeComboxMouseClicked);

        GroupLayout fileTypeSelectionPanelLayout = new GroupLayout(fileTypSelectionPanel);
        fileTypSelectionPanel.setLayout(fileTypeSelectionPanelLayout);

        fileTypeSelectionPanelLayout.setHorizontalGroup(
                fileTypeSelectionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fileTypeSelectionPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(fileTypeJLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fileTypeCombox, GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        fileTypeSelectionPanelLayout.setVerticalGroup(
                fileTypeSelectionPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fileTypeSelectionPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fileTypeSelectionPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(fileTypeJLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fileTypeCombox))
                                .addContainerGap())
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
                                                .addComponent( startJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                                .addGap(15, 15, 15))
                                        .addComponent(fileTypSelectionPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(inputFilesPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(annotationSettingJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(fileTypSelectionPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addComponent(inputFilesPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(annotationSettingJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent( startJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
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
     * File type combox mouse click action
     * @param evt Item event
     */
    private void fileTypeComboxMouseClicked(ItemEvent evt){
        typeSelection = String.valueOf(fileTypeCombox.getSelectedItem());

        if (typeSelection.equals("proBAM")){

            inputFilesPanel.removeAll();

            proGenomicsFileLabel.setText("proBAM File *");

            GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
            inputFilesPanel.setLayout(inputFilesPanelLayout);
            inputFilesPanelLayout.setHorizontalGroup(
                    inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                            .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                    .addComponent(proGenomicsFileLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(proGenomicsFileTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(browseProBAMJButton))
                                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                                    .addComponent(spectrumFilesLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(spectrumFilesTxt,GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(browseSpectraJButton)))
                                    .addContainerGap())
            );

            inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseProBAMJButton});

            inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseSpectraJButton});

            inputFilesPanelLayout.setVerticalGroup(
                    inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(proGenomicsFileTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addComponent(proGenomicsFileLabel)
                                            .addComponent(browseProBAMJButton))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(spectrumFilesLabel)
                                            .addComponent(browseSpectraJButton)
                                            .addComponent(spectrumFilesTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );

            inputFilesPanel.revalidate();
            inputFilesPanel.repaint();

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


        } else if (typeSelection.equals("proBed")){
            inputFilesPanel.removeAll();

            proGenomicsFileLabel.setText("proBed File *");

            GroupLayout inputFilesPanelLayout = new GroupLayout(inputFilesPanel);
            inputFilesPanel.setLayout(inputFilesPanelLayout);
            inputFilesPanelLayout.setHorizontalGroup(
                    inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                            .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                    .addComponent(mzIDFileJLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(mzIDFileJText,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(mzIDFileBrowseJButton))
                                            .addGroup(GroupLayout.Alignment.TRAILING, inputFilesPanelLayout.createSequentialGroup()
                                                    .addComponent(proGenomicsFileLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(proGenomicsFileTxt,GroupLayout.PREFERRED_SIZE,260, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(browseProBAMJButton))
                                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                                    .addComponent(spectrumFilesLabel, GroupLayout.PREFERRED_SIZE, 160, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                    .addComponent(spectrumFilesTxt,GroupLayout.PREFERRED_SIZE, 260, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                    .addComponent(browseSpectraJButton)))
                                    .addContainerGap())
            );

            inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseProBAMJButton});

            inputFilesPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {browseSpectraJButton});

            inputFilesPanelLayout.setVerticalGroup(
                    inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addGroup(inputFilesPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(mzIDFileJText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addComponent(mzIDFileJLabel)
                                            .addComponent(mzIDFileBrowseJButton))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(proGenomicsFileTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                            .addComponent(proGenomicsFileLabel)
                                            .addComponent(browseProBAMJButton))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(inputFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                            .addComponent(spectrumFilesLabel)
                                            .addComponent(browseSpectraJButton)
                                            .addComponent(spectrumFilesTxt, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            );

            inputFilesPanel.revalidate();
            inputFilesPanel.repaint();

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

    }

    /**
     * MzIdentML file browse button action
     * @param evt Action event
     */
    private void mzIDFileBrowseJButtonActionPerformed(ActionEvent evt){

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select mzIdentML File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter mzidFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mzid")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "mzIdentML (.mzid)";
            }
        };

        fileChooser.setFileFilter(mzidFilter);

        int returnValue = fileChooser.showDialog(this, "Add");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            mzIDFile = fileChooser.getSelectedFile();

            lastSelectedFolder = mzIDFile.getParent();

            mzIDFileJText.setText(mzIDFile.getName() + " selected");

            Thread importIDThread = new Thread(() -> {
                try {
                    mzIdentMLType = MzIdentMLParser.parse(Paths.get(mzIDFile.getAbsolutePath()));
                    mzIdentMLCheck = 1;
                } catch (FileParsingException |Error e ) {
                    if(e.getClass().getName().contains("Error")){
                        JOptionPane.showMessageDialog(pdvMainClass, "Please increase JVM memory! ", "Memory Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(ProBamImportDialog.this, "Failed to pares mzIdentML file", "File Error", JOptionPane.WARNING_MESSAGE);
                        e.printStackTrace();
                    }
                }
            }, "importThread");

            importIDThread.start();
        }

        validateInput();
    }

    /**
     * Import all files and show probam frame
     * @param evt mouse click event
     */
    private void  startJButtonActionPerformed(ActionEvent evt) {

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

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

                if(spectrumFileType.equals("mgf")){
                    SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
                    try {

                        spectrumFactory.addSpectra(spectrumFile);

                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ProBamImportDialog.this, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        System.exit(1);
                    }

                    spectrumsFileFactory = spectrumFactory;

                }else if(spectrumFileType.equals("mzml")){

                    MZMLFile mzmlFile = new MZMLFile(spectrumFile.getAbsolutePath());

                    ScanCollectionDefault scans = new ScanCollectionDefault();

                    scans.setDefaultStorageStrategy(StorageStrategy.SOFT);

                    scans.isAutoloadSpectra(true);

                    scans.setDataSource(mzmlFile);

                    mzmlFile.setNumThreadsForParsing(null);

                    try {
                        scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA);
                    } catch (FileParsingException e) {
                        JOptionPane.showMessageDialog(ProBamImportDialog.this, "Failed to pares Spectrum file. Please check your spectrum file!", "File Error", JOptionPane.WARNING_MESSAGE);
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        System.exit(1);
                    }

                    spectrumsFileFactory = scans;
                }else if(spectrumFileType.equals("mzxml")){
                    MzXMLScanImport mzXMLScanImport = new MzXMLScanImport(spectrumFile.getAbsolutePath());

                    spectrumsFileFactory = mzXMLScanImport.getScans();
                }

                if (typeSelection.equals("proBAM")) {
                    progressDialog.setRunFinished();
                    pdvMainClass.importProBAMFile(proGenomicsFile, spectrumFile, spectrumFileType, spectrumsFileFactory);
                } else {
                    for (int i = 0;i<300;i++){
                        if (mzIdentMLCheck ==1){

                            progressDialog.setRunFinished();

                            pdvMainClass.importProBedFile(proGenomicsFile, spectrumFile, spectrumFileType, spectrumsFileFactory, mzIdentMLType);

                            mzIDFile = null;

                            break;
                        }else{
                            try {Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            }
        }.start();
    }

    /**
     * Input MS2 ion tolerance
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
     * Import spectrum file
     * @param evt mouse click event
     */
    private void browseSpectraJButtonActionPerformed(ActionEvent evt) {

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Spectrum File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        ||myFile.getName().toLowerCase().endsWith(".mzml")
                        ||myFile.getName().toLowerCase().endsWith(".mzxml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf), mzml and mzXML";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            spectrumFile = fileChooser.getSelectedFile();

            if (spectrumFile.getName().toLowerCase().endsWith(".mgf")){
                spectrumFileType = "mgf";
            } else if (spectrumFile.getName().toLowerCase().endsWith(".mzml")){
                spectrumFileType = "mzml";
            } else if (spectrumFile.getName().toLowerCase().endsWith(".mzxml")){
                spectrumFileType = "mzxml";
            }

            lastSelectedFolder = spectrumFile.getParent();
            spectrumFilesTxt.setText(spectrumFile.getName() + " selected");
            validateInput();
        }
    }

    /**
     * Import probam file
     * @param evt mouse click event
     */
    private void browseProBAMJButtonActionPerformed(ActionEvent evt) {

        final JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select proGenomics File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter mzidFilter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                
                if (typeSelection.equals("proBAM")) {

                    return myFile.getName().toLowerCase().endsWith(".*probam")
                            || myFile.getName().toLowerCase().endsWith(".bam")
                            || myFile.getName().toLowerCase().endsWith(".sam")
                            || myFile.isDirectory();
                } else {
                    return myFile.getName().toLowerCase().endsWith(".*probed")
                            || myFile.getName().toLowerCase().endsWith(".bed")
                            || myFile.isDirectory();
                }
            }

            @Override
            public String getDescription() {
                if (typeSelection.equals("proBAM")) {
                    return "probam file (.probam .bam .sam)";
                } else {
                    return "probed file (.probed)";
                }
            }
        };

        fileChooser.setFileFilter(mzidFilter);

        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            proGenomicsFile = fileChooser.getSelectedFile();

            lastSelectedFolder =  proGenomicsFile.getParent();

            proGenomicsFileTxt.setText( proGenomicsFile.getName() + " selected");

            validateInput();

        }
    }

    /**
     * Closes the dialog
     * @param evt window event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Validates the input and enable start button
     */
    private void validateInput() {

        boolean allValid = true;

        if (proGenomicsFile != null) {
            proGenomicsFileLabel.setForeground(Color.BLACK);
            proGenomicsFileLabel.setToolTipText(null);
        } else {
            proGenomicsFileLabel.setForeground(Color.RED);
            proGenomicsFileLabel.setToolTipText("Please input proBAM file");
            proGenomicsFileTxt.setToolTipText("Please input proBAM file");
            proGenomicsFileTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (spectrumFile != null) {
            spectrumFilesLabel.setForeground(Color.BLACK);
            spectrumFilesLabel.setToolTipText(null);
        } else {
            spectrumFilesLabel.setForeground(Color.RED);
            spectrumFilesLabel.setToolTipText("Please select the spectrum file for the proBAM file");
            spectrumFilesTxt.setToolTipText("Please select the spectrum file(s) for the proBAM file");
            spectrumFilesTxt.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if (fragmentIonMZTolerance != null) {
            fragmentIonLbl.setForeground(Color.BLACK);
            fragmentIonLbl.setToolTipText(null);
        } else {
            fragmentIonLbl.setForeground(Color.RED);
            fragmentIonLbl.setToolTipText("Please input MS2 ion tolerance");
            allValid = false;
        }
        
        if (typeSelection.equals("proBed")){
            if (mzIDFile != null) {
                mzIDFileJLabel.setForeground(Color.BLACK);
                mzIDFileJLabel.setToolTipText(null);
            } else {
                mzIDFileJLabel.setForeground(Color.RED);
                mzIDFileJLabel.setToolTipText("Please input mzID file");
                mzIDFileJText.setToolTipText("Please input mzID file");
                mzIDFileJText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                allValid = false;
            }
        }

        startJButton.setEnabled(allValid);
    }

    @Override
    public void setIconImage(Image image) {
        super.setIconImage(image);
        if (pdvStart != null) {
            pdvStart.setIconImage(image);
        }
    }
}

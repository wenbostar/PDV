package PDVGUI.gui.utils.Export;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.utils.SpectrumContainer;
import PDVGUI.gui.utils.SpectrumMainPanel;
import PDVGUI.utils.Export;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.batik.transcoder.TranscoderException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Export excepted size dialog
 * Created by Ken on 9/4/2017.
 */
public class ExportExpectedSizeDialog extends JDialog {

    private JLabel pathJLabel;
    private JTextField pathJText;
    private JButton exportJButton;
    private JComboBox typeJComboBox;
    private JTextField picHeightJText;
    private JTextField picWidthJText;
    private JPanel viewerJPanel;
    private JComboBox unitJCombox;
    private JPanel resizeJPanel = new JPanel();
    private JPanel mainJPanel;
    private JLayeredPane jLayeredPane;
    private JPanel mirrorSequenceFragmentationPanel;

    /**
     * Sequence fragment pannel
     */
    private SequenceFragmentationPanel secondarySpectrumPlotsJPanel;
    /**
     * Spectrum panel
     */
    private SpectrumContainer spectrumJPanel;
    /**
     * Peptide size
     */
    private Integer peptideSize;
    /**
     * Picture type
     */
    private String[] picType = new String[]{"PNG", "TIFF", "PDF"};
    /**
     * Output folder path
     */
    private String outputFolder;
    /**
     * Spectrum main panel show spectral
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Spectrum key
     */
    private String spectrumKey;
    /**
     * Parent frame
     */
    private JFrame parentJFrame;
    /**
     * Check peptide or not
     */
    private Boolean isCheckPeptide;
    /**
     * Pattern removing illegal
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");

    private Integer oldHeight;

    private Integer oldWidth;

    public Integer resizeJPanelWidth;
    public Integer resizeJPanelHeight;

    /**
     * Constructor
     * @param spectrumMainPanel Parent class
     * @param jLayeredPane JLayered Panel
     * @param secondarySpectrumPlotsJPanel Peptide fragments panel
     * @param mirrorSequenceFragmentationPanel Mirror fragments panel
     * @param spectrumJPanel Spectrum Panel
     * @param peptideSize Peptide length
     * @param spectrumKey Spectrum Key
     */
    public ExportExpectedSizeDialog(SpectrumMainPanel spectrumMainPanel, JLayeredPane jLayeredPane, SequenceFragmentationPanel secondarySpectrumPlotsJPanel, JPanel mirrorSequenceFragmentationPanel,
                                    SpectrumContainer spectrumJPanel, Integer peptideSize, String spectrumKey, Boolean isCheckPeptide){
        super(spectrumMainPanel.parentFrame, true);

        this.jLayeredPane = jLayeredPane;
        this.secondarySpectrumPlotsJPanel = secondarySpectrumPlotsJPanel;
        this.mirrorSequenceFragmentationPanel = mirrorSequenceFragmentationPanel;
        this.spectrumJPanel = spectrumJPanel;
        this.isCheckPeptide = isCheckPeptide;
        this.spectrumMainPanel = spectrumMainPanel;
        this.parentJFrame = spectrumMainPanel.parentFrame;
        this.peptideSize = peptideSize;
        this.spectrumKey = spectrumKey;
        this.oldHeight = spectrumMainPanel.getHeight();
        this.oldWidth = spectrumMainPanel.getWidth();

        outputFolder = spectrumMainPanel.lastSelectedFolder.getLastSelectedFolder();

        setupGUI();

        setLocationRelativeTo(spectrumMainPanel.parentFrame);

    }

    /**
     * Setup GUI
     */
    private void setupGUI(){
        initComponents();

        picHeightJText.setText(String.valueOf(500));
        picWidthJText.setText(String.valueOf(900));

        previewJButtonActionPerformed(null);

        validateInput();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel detailJPanel = new JPanel();
        JButton pathBrowseJButton = new JButton();
        JLabel typeJLabel = new JLabel();
        JScrollPane viewerJScrollPane = new JScrollPane();
        JLabel picHeightJLabel = new JLabel("Height");
        JLabel picWidthJLabel = new JLabel("Width");
        JButton previewJButton = new JButton("Preview");

        mainJPanel = new JPanel();
        exportJButton = new JButton();
        typeJComboBox = new JComboBox();
        pathJLabel = new JLabel();
        pathJText = new JTextField();
        picHeightJText = new JTextField();
        picWidthJText = new JTextField();
        viewerJPanel = new JPanel();
        unitJCombox = new JComboBox();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Export");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setMinimumSize(new Dimension(93, 679));
        setResizable(true);

        mainJPanel.setBackground(Color.white);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Path & Type" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        detailJPanel.setBorder(titledBorder);

        detailJPanel.setOpaque(false);
        detailJPanel.setBackground(Color.white);

        pathJLabel.setText("Output Path");
        pathJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        pathJLabel.setBackground(new Color(255, 0, 0));

        pathJText.setHorizontalAlignment(SwingConstants.CENTER);
        pathJText.setEditable(false);

        pathBrowseJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        pathBrowseJButton.setBorder(null);
        pathBrowseJButton.setBorderPainted(false);
        pathBrowseJButton.setContentAreaFilled(false);
        pathBrowseJButton.addActionListener(this::pathBrowseJButtonActionPerformed);

        typeJLabel.setText("Type");
        typeJLabel.setFont(new Font("Console", Font.PLAIN, 12));

        typeJComboBox.setModel(new DefaultComboBoxModel(this.picType));
        typeJComboBox.addItemListener(this::typeJComboBoxdMouseClicked);

        picHeightJText.setHorizontalAlignment(SwingConstants.CENTER);

        picWidthJText.setHorizontalAlignment(SwingConstants.CENTER);

        picHeightJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        picWidthJLabel.setFont(new Font("Console", Font.PLAIN, 12));

        previewJButton.setOpaque(true);
        previewJButton.setFont(new Font("Lucida", Font.BOLD, 12));
        previewJButton.setBackground(new Color(76, 191, 255));
        previewJButton.addActionListener(this::previewJButtonActionPerformed);

        exportJButton.setText("Export");
        exportJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        exportJButton.setBackground(Color.GREEN);
        exportJButton.setOpaque(false);
        exportJButton.setEnabled(false);
        exportJButton.addActionListener(this::exportJButtonActionPerformed);

        unitJCombox.setModel(new DefaultComboBoxModel(new String[]{"px", "mm", "cm", "in"}));
        unitJCombox.addItemListener(this::unitJComboxdMouseClicked);

        GroupLayout detailJPanelLayout = new GroupLayout(detailJPanel);
        detailJPanel.setLayout(detailJPanelLayout);

        detailJPanelLayout.setHorizontalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addComponent(pathJLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(pathBrowseJButton)
                                .addGap(10,40,80)
                                .addComponent(typeJLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(typeJComboBox, GroupLayout.DEFAULT_SIZE, 50, 80)
                                .addGap(10, 20, 380)
                                .addComponent(picHeightJLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(picHeightJText, 40, 40, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(picWidthJLabel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(picWidthJText, 40, 40, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(unitJCombox, 80, 80, 90)
                                .addGap(10, 20,100)
                                .addComponent(previewJButton))
        );

        detailJPanelLayout.setVerticalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGap(5)
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(pathBrowseJButton)
                                        .addComponent(typeJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(typeJComboBox, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picHeightJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picHeightJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picWidthJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picWidthJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(unitJCombox, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(previewJButton, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addGap(5))

        );

        titledBorder = BorderFactory.createTitledBorder("Preview" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        viewerJPanel.setBorder(titledBorder);
        viewerJPanel.setBackground(Color.white);
        viewerJPanel.setOpaque(false);

        jLayeredPane.setPreferredSize(spectrumMainPanel.getSize());
        jLayeredPane.setBackground(Color.white);
        jLayeredPane.setOpaque(true);

        resizeJPanel.add(jLayeredPane);

        viewerJScrollPane.setOpaque(false);
        viewerJScrollPane.setViewportView(resizeJPanel);

        viewerJScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        viewerJScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        GroupLayout viewerJPanelLayout = new GroupLayout(viewerJPanel);
        viewerJPanel.setLayout(viewerJPanelLayout);

        viewerJPanelLayout.setHorizontalGroup(
                viewerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(viewerJScrollPane)
        );

        viewerJPanelLayout.setVerticalGroup(
                viewerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(viewerJScrollPane)
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(detailJPanel)
                        .addComponent(viewerJPanel, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(detailJPanel, 50, 50, 60)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(viewerJPanel, 50, 300, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, 915, 915, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, 640, 640, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Closes the dialog
     * @param evt window event
     */
    private void formWindowClosing(WindowEvent evt) {
        if(mirrorSequenceFragmentationPanel != null){
            spectrumMainPanel.showMirrorJMenuItemActionPerformed(null);
        } else if(isCheckPeptide){
            spectrumMainPanel.showCheckPeptideJMenuItemActionPerformed(null);
        } else {
            spectrumMainPanel.showSpectrumJMenuItemAction(null);
        }
        spectrumMainPanel.setBounds(0, 0, oldWidth, oldHeight);
        spectrumMainPanel.spectrumShowPanel.setBounds(0, 0, oldWidth, oldHeight);
        spectrumMainPanel.updateSpectrum();
        this.dispose();
    }

    /**
     * File type selection action
     * @param evt Item event
     */
    private void typeJComboBoxdMouseClicked(ItemEvent evt){
        Integer selectIndex = typeJComboBox.getSelectedIndex();

        if(selectIndex == 2){

            unitJCombox.setModel(new DefaultComboBoxModel(new String[]{"mm", "cm", "in"}));
        } else {
            unitJCombox.setModel(new DefaultComboBoxModel(new String[]{"px", "mm", "cm", "in"}));
        }
    }

    private void unitJComboxdMouseClicked(ItemEvent evt){

    }

    /**
     * Preview the output image.
     * @param evt Mouse click event
     */
    public void previewJButtonActionPerformed(ActionEvent evt){

        String unitName = (String) unitJCombox.getSelectedItem();

        if(!picHeightJText.getText().equals("") && !picWidthJText.getText().equals("")){
            Integer currentHeight = (Integer.valueOf(picHeightJText.getText()));
            Integer currentWidth = (Integer.valueOf(picWidthJText.getText()));

            FormLayout layout = new FormLayout(currentWidth + unitName, currentHeight + unitName);

            resizeJPanel.setLayout(layout);
            resizeJPanel.revalidate();
            resizeJPanel.repaint();

            resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
            resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));

            if(resizeJPanelWidth <= 200 || resizeJPanelHeight<= 200){
                JOptionPane.showMessageDialog(null, "Please set a bigger size.", "Warning", JOptionPane.WARNING_MESSAGE);

            } else {

                spectrumJPanel.setBounds(0,75, resizeJPanelWidth, resizeJPanelHeight - 85);

                //spectrumMainPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);
                spectrumMainPanel.spectrumShowPanel.setBounds(0, 0, resizeJPanelWidth, resizeJPanelHeight);

                spectrumJPanel.requestFocus();

                jLayeredPane.setBounds(0,0, resizeJPanelWidth, resizeJPanelHeight);
                jLayeredPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));

                viewerJPanel.revalidate();
                viewerJPanel.repaint();

            }
        }
    }

    /**
     * Select output path
     * @param evt mouse click event
     */
    private void pathBrowseJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(outputFolder);
        fileChooser.setDialogTitle("Select Output Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnValue = fileChooser.showDialog(this, "Ok");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            outputFolder = selectedFile.getAbsolutePath();

            if (!selectedFile.exists()){ // Avoid bug in Mac
                outputFolder = outputFolder.substring(0, outputFolder.lastIndexOf("/"));
                if (!new File(outputFolder).exists()){
                    JOptionPane.showMessageDialog(null, "Please check your output path.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    spectrumMainPanel.lastSelectedFolder.setLastSelectedFolder(outputFolder);

                    pathJText.setText(outputFolder + " selected");
                }
            } else {
                spectrumMainPanel.lastSelectedFolder.setLastSelectedFolder(outputFolder);

                pathJText.setText(outputFolder + " selected");
            }

            validateInput();
        }
    }

    /**
     * Validates input information and enable start button
     */
    private void validateInput() {
        boolean allValid = true;

        if(pathJText.getText()!=null && !pathJText.getText().equals("")){
            pathJLabel.setForeground(Color.BLACK);
            pathJLabel.setToolTipText(null);
        }else {
            pathJLabel.setForeground(Color.RED);
            pathJLabel.setToolTipText("Please select output directory");
            pathJText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }
        exportJButton.setEnabled(allValid);
    }

    /**
     * Export selected spectrum
     * @param evt mouse click event
     */
    private void exportJButtonActionPerformed(ActionEvent evt) {

        ImageType finalImageType = null;

        Integer selectIndex = typeJComboBox.getSelectedIndex();

        switch (selectIndex){
            case 0:
                finalImageType = ImageType.PNG;
                break;
            case 1:
                finalImageType = ImageType.TIFF;
                break;
            case 2:
                finalImageType = ImageType.PDF;
                break;
        }

        File imageFile = new File(outputFolder + PDVMainClass.FILE_SEPARATOR + FilePattern.matcher(spectrumKey).replaceAll("")+ finalImageType.getExtension());

        if(imageFile.exists()){
            int value = JOptionPane.showConfirmDialog(this,
                    "The file have exist. Overwrite it?",
                    "File check",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {

                try {

                    Thread.sleep(200);

                    Export.exportPic(jLayeredPane, jLayeredPane.getBounds(), imageFile, finalImageType);

                    JOptionPane.showMessageDialog(this, "The spectrum has been exported.\nFile name: "+imageFile.getName(),
                            "Export Finished", JOptionPane.PLAIN_MESSAGE);

                } catch (IOException | TranscoderException | InterruptedException e) {
                    JOptionPane.showMessageDialog(this, "Spectrum export error Please contact developer.\nError: "+e.toString(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

            } else if (value == JOptionPane.NO_OPTION) {

            }
        } else {

            try {

                Thread.sleep(200);

                Export.exportPic(jLayeredPane, jLayeredPane.getBounds(), imageFile, finalImageType);

                JOptionPane.showMessageDialog(this, "The spectrum has been exported.\nFile name: "+imageFile.getName(),
                        "Export Finished", JOptionPane.PLAIN_MESSAGE);
            } catch (IOException | TranscoderException | InterruptedException e) {
                JOptionPane.showMessageDialog(this, "Spectrum export error Please contact developer.\nError: "+e.toString(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

    }
}

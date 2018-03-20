package PDVGUI.gui.utils.Export;

import PDVGUI.gui.utils.SetAction;
import PDVGUI.gui.utils.SpectrumMainPanel;
import PDVGUI.utils.Export;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.gui.spectrum.SequenceFragmentationPanel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.io.File;
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
    private JLayeredPane jLayeredPane;
    private JPanel mirrorSequenceFragmentationPanel;

    /**
     * Sequence fragment pannel
     */
    private SequenceFragmentationPanel secondarySpectrumPlotsJPanel;
    /**
     * Spectrum panel
     */
    private SpectrumPanel spectrumJPanel;
    /**
     * Peptide size
     */
    private Integer peptideSize;
    /**
     * Picture type
     */
    private String[] picType = new String[]{"PNG (Portable Network Graphics)", "TIFF (Tagged Image File Format)", "PDF (Portable Document Format)"};
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
                                    SpectrumPanel spectrumJPanel, Integer peptideSize, String spectrumKey, Boolean isCheckPeptide){
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

        outputFolder = spectrumMainPanel.lastSelectedFolder.getLastSelectedFolder();

        setupGUI();

        setLocationRelativeTo(spectrumMainPanel.parentFrame);

    }

    /**
     * Setup GUI
     */
    private void setupGUI(){
        initComponents();

        picHeightJText.setText(String.valueOf(spectrumJPanel.getHeight()));
        picWidthJText.setText(String.valueOf(spectrumJPanel.getWidth()));

        validateInput();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();
        JPanel detailJPanel = new JPanel();
        JButton pathBrowseJButton = new JButton();
        JLabel typeJLabel = new JLabel();
        JScrollPane viewerJScrollPane = new JScrollPane();
        JLabel picHeightJLabel = new JLabel("Height");
        JLabel picWidthJLabel = new JLabel("Width");
        JButton previewJButton = new JButton("Preview");

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

        setResizable(false);

        mainJPanel.setBackground(Color.white);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Path & Type" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        detailJPanel.setBorder(titledBorder);

        detailJPanel.setOpaque(false);
        detailJPanel.setBackground(Color.white);

        pathJLabel.setText("Output Path");
        pathJLabel.setFont(new Font("Console", Font.PLAIN, 11));
        pathJLabel.setBackground(new Color(255, 0, 0));

        pathJText.setHorizontalAlignment(SwingConstants.CENTER);
        pathJText.setEditable(false);

        pathBrowseJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        pathBrowseJButton.setBorder(null);
        pathBrowseJButton.setBorderPainted(false);
        pathBrowseJButton.setContentAreaFilled(false);
        pathBrowseJButton.addActionListener(this::pathBrowseJButtonActionPerformed);

        typeJLabel.setText("Export type");
        typeJLabel.setFont(new Font("Console", Font.PLAIN, 11));

        typeJComboBox.setModel(new DefaultComboBoxModel(this.picType));
        typeJComboBox.addItemListener(this::typeJComboBoxdMouseClicked);

        picHeightJText.setHorizontalAlignment(SwingConstants.CENTER);

        picWidthJText.setHorizontalAlignment(SwingConstants.CENTER);

        picHeightJLabel.setFont(new Font("Console", Font.PLAIN, 11));
        picWidthJLabel.setFont(new Font("Console", Font.PLAIN, 11));

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

        unitJCombox.setModel(new DefaultComboBoxModel(new String[]{"mm", "cm", "in", "px"}));

        GroupLayout detailJPanelLayout = new GroupLayout(detailJPanel);
        detailJPanel.setLayout(detailJPanelLayout);;

        detailJPanelLayout.setHorizontalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE )
                                                .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                .addComponent(pathBrowseJButton))
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                .addComponent(picHeightJLabel, GroupLayout.DEFAULT_SIZE, 110, Short.MAX_VALUE)
                                                .addComponent(picHeightJText, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                                .addGap(10, 40, 60)
                                                .addComponent(picWidthJLabel, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(picWidthJText, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                                .addGap(10, 40, 60)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(unitJCombox)))
                                .addGap(10, 30, 50)
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                .addComponent(typeJLabel, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                                .addComponent(typeJComboBox, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                        .addComponent(previewJButton)
                                                )))
        );

        detailJPanelLayout.setVerticalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(pathBrowseJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(picHeightJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picHeightJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picWidthJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(picWidthJText, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(unitJCombox, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(typeJLabel, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(typeJComboBox, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(previewJButton, GroupLayout.DEFAULT_SIZE, 25, GroupLayout.PREFERRED_SIZE)))
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
                        .addComponent(viewerJPanel, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(detailJPanel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(viewerJPanel, 50, 400, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, (int) (spectrumMainPanel.parentFrame.getWidth() * 0.8), Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, (int) (spectrumMainPanel.parentFrame.getHeight() * 0.8), Short.MAX_VALUE)
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
            unitJCombox.setModel(new DefaultComboBoxModel(new String[]{"mm", "cm", "in", "px"}));
        }
    }

    /**
     * Preview the output image.
     * @param evt Mouse click event
     */
    private void previewJButtonActionPerformed(ActionEvent evt){

        String unitName = (String) unitJCombox.getSelectedItem();

        if(!picHeightJText.getText().equals("") && !picWidthJText.getText().equals("")){
            Integer height = (Integer.valueOf(picHeightJText.getText()));
            Integer width = (Integer.valueOf(picWidthJText.getText()));

            FormLayout layout = new FormLayout(width+unitName, height+unitName);

            resizeJPanel.setLayout(layout);
            resizeJPanel.revalidate();
            resizeJPanel.repaint();

            Integer resizeJPanelWidth = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getWidth()));
            Integer resizeJPanelHeight = Math.toIntExact(Math.round(resizeJPanel.getPreferredSize().getHeight()));

            if(resizeJPanelWidth < 200){
                JOptionPane.showMessageDialog(null, "The width must be bigger than 200 pixels.", "Warning", JOptionPane.WARNING_MESSAGE);

            } else {

                spectrumJPanel.setBounds(0,75, resizeJPanelWidth, resizeJPanelHeight - 85);

                if(mirrorSequenceFragmentationPanel != null){
                    mirrorSequenceFragmentationPanel.setBounds(40, resizeJPanelHeight -40,(peptideSize+4)*20,100);
                }

                jLayeredPane.setBounds(0,0, resizeJPanelWidth, resizeJPanelHeight);
                jLayeredPane.setPreferredSize(new Dimension(resizeJPanelWidth, resizeJPanelHeight));

                viewerJPanel.revalidate();
                viewerJPanel.repaint();

                new SetAction(jLayeredPane, secondarySpectrumPlotsJPanel, mirrorSequenceFragmentationPanel, spectrumJPanel, resizeJPanelHeight, resizeJPanelWidth);

                spectrumJPanel.requestFocus(true);
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

            if (selectedFile.isDirectory()) {
                outputFolder = selectedFile.getAbsolutePath();
            } else {
                JOptionPane.showMessageDialog(this, "Please select one directory", "File Format Error", JOptionPane.WARNING_MESSAGE);
            }

            pathJText.setText(outputFolder+" selected");
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

        File imageFile = new File(outputFolder+"/"+FilePattern.matcher(spectrumKey).replaceAll("")+ finalImageType.getExtension());

        ProgressDialogX progressDialog = new ProgressDialogX(parentJFrame,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Export Selected spectrum. Please Wait...");

        if(imageFile.exists()){
            int value = JOptionPane.showConfirmDialog(this,
                    "The file have exist. Overwrite it?",
                    "File check",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {

                new Thread(new Runnable() {

                    public void run() {
                        progressDialog.setVisible(true);
                    }
                }, "ProgressDialog").start();

                ImageType finalImageType1 = finalImageType;
                new Thread("Export") {

                    @Override
                    public void run() {

                        try {

                            Export.exportPic(jLayeredPane, jLayeredPane.getBounds(), imageFile, finalImageType1);

                        } catch (Exception e) {
                            progressDialog.setRunFinished();
                            e.printStackTrace();
                        }
                        progressDialog.setRunFinished();
                    }
                }.start();

            } else if (value == JOptionPane.NO_OPTION) {
            }
        } else {

            new Thread(new Runnable() {

                public void run() {
                    progressDialog.setVisible(true);
                }
            }, "ProgressDialog").start();

            ImageType finalImageType2 = finalImageType;
            new Thread("Export") {

                @Override
                public void run() {

                    try {

                        Export.exportPic(jLayeredPane, jLayeredPane.getBounds(), imageFile, finalImageType2);

                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                    }
                    progressDialog.setRunFinished();
                }
            }.start();
        }
    }

    /**
     * Return last folder
     * @return String
     */
    public String getLastFolder(){

        return outputFolder;
    }
}

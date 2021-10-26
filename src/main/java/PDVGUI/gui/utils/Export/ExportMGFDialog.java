package PDVGUI.gui.utils.Export;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.utils.SpectrumMainPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Export excepted spectrum in MGF format
 * Created by Ken on 8/4/2020.
 */
public class ExportMGFDialog extends JDialog {
    private JLabel pathJLabel;
    private JTextField pathJText;
    private JButton exportJButton;

    /**
     * Last selected folder
     */
    private String lastFolder;
    /**
     * Output folder path
     */
    private String outputFolder;
    /**
     * spectrum Title
     */
    private String spectrumTitle;
    /**
     * MGF
     */
    private String mgf;
    /**
     * Remove illegal pattern
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * Constructor
     * @param spectrumMainPanel Parent class
     * @param spectrumTitle Spectrum title
     * @param mgf MGF
     */
    public ExportMGFDialog(SpectrumMainPanel spectrumMainPanel, String spectrumTitle, String mgf){
        super(spectrumMainPanel.parentFrame, true);

        this.lastFolder = PDVMainClass.lastSelectedFolder.getLastSelectedFolder();
        this.spectrumTitle = FilePattern.matcher(spectrumTitle).replaceAll("");
        this.mgf = mgf;

        setUpGui();

        setLocationRelativeTo(spectrumMainPanel);

        setVisible(true);
    }

    /**
     * Set up the GUI
     */
    private void setUpGui() {
        initComponents();
        validateInput();

        pathJText.setText("No Selection");
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();
        JPanel detailJPanel = new JPanel();
        JButton pathBrowseJButton = new JButton();
        JLabel blankJLabel = new JLabel(" ");
        exportJButton = new JButton();
        pathJLabel = new JLabel();
        pathJText = new JTextField();

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
        pathJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        pathJLabel.setBackground(new Color(255, 0, 0));

        pathJText.setHorizontalAlignment(SwingConstants.CENTER);
        pathJText.setEditable(false);

        pathBrowseJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        pathBrowseJButton.setBorder(null);
        pathBrowseJButton.setBorderPainted(false);
        pathBrowseJButton.setContentAreaFilled(false);
        pathBrowseJButton.addActionListener(this::pathBrowseJButtonActionPerformed);

        exportJButton.setText("Export");
        exportJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        exportJButton.setBackground(Color.GREEN);
        exportJButton.setOpaque(false);
        exportJButton.setEnabled(false);
        exportJButton.addActionListener(this::exportJButtonActionPerformed);

        GroupLayout detailJPanelLayout = new GroupLayout(detailJPanel);
        detailJPanel.setLayout(detailJPanelLayout);;

        detailJPanelLayout.setHorizontalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE)
                                        .addComponent(blankJLabel, GroupLayout.DEFAULT_SIZE, 70, Short.MAX_VALUE))
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(pathBrowseJButton))))
        );

        detailJPanelLayout.setVerticalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addGroup(detailJPanelLayout.createSequentialGroup()
                                        .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(blankJLabel, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                        )
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(pathJText)
                                        .addComponent(pathBrowseJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED))
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(detailJPanel)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addGap(20)
                                .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addGap(10))

        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(detailJPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(exportJButton, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
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
     * @param evt window event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Validates input information and enable start button
     */
    private void validateInput() {
        boolean allValid = true;

        if(outputFolder!=null){
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
     * Select output path
     * @param evt mouse click event
     */
    private void pathBrowseJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastFolder);
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

                    pathJText.setText(outputFolder + " selected");
                }
            } else {

                pathJText.setText(outputFolder + " selected");
            }
            PDVMainClass.lastSelectedFolder.setLastSelectedFolder(outputFolder);

            validateInput();
        }
    }

    /**
     * Export selected spectrum
     * @param evt mouse click event
     */
    private void exportJButtonActionPerformed(ActionEvent evt) {

        String filePath = outputFolder + "/" + spectrumTitle + ".mgf";
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File(filePath)));
            fileWriter.write(mgf);
            fileWriter.close();
            JOptionPane.showMessageDialog(this, "The spectrum has been exported.\nFile name: "+spectrumTitle,
                    "Export Finished", JOptionPane.PLAIN_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package PDVGUI.gui.utils.Export;

import PDVGUI.gui.PDVMainClass;
import PDVGUI.gui.MSDataDisplay;
import PDVGUI.utils.Export;
import com.compomics.util.enumeration.ImageType;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.regex.Pattern;

public class ExportTICDialog extends JDialog {

    private JLabel pathJLabel;
    private JLabel fileNameJLabel;
    private JTextField pathJText;
    private JTextField fileNameJText;
    private JButton exportJButton;
    private JComboBox typeJComboBox;
    private JFreeChart chart;
    private JPanel chartPanel;

    /**
     * Picture type
     */
    private String[] picType = new String[]{"PNG (Portable Network Graphics)", "TIFF (Tagged Image File Format)", "PDF (Portable Document Format)"};
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
    private MSDataDisplay msDataDisplay;
    /**
     * Output file name
     */
    private String picName;
    /**
     * Pattern removing illegal
     */
    private static Pattern FilePattern = Pattern.compile("[\\\\/:*?\"<>|]");

    /**
     * Main constructor
     * @param msDataDisplay parent class
     * @param chart Jfree chart
     * @param chartPanel Chart panel
     */
    public ExportTICDialog(MSDataDisplay msDataDisplay, JFreeChart chart, JPanel chartPanel){
        super(msDataDisplay, true);

        this.msDataDisplay = msDataDisplay;
        this.chart = chart;
        this.chartPanel = chartPanel;

        setupGUI();

        setLocationRelativeTo(chartPanel);

        setVisible(true);
    }

    /**
     * Set up GUI
     */
    private void setupGUI(){

        initComponents();
        validateInput();

        typeJComboBox.setEnabled(true);
        typeJComboBox.setRenderer(new AlignedListCellRenderer(0));
        pathJText.setText(" No Selection");
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();
        JPanel detailJPanel = new JPanel();
        JButton pathBrowseJButton = new JButton();
        JLabel typeJLabel = new JLabel();

        exportJButton = new JButton();
        typeJComboBox = new JComboBox();
        pathJLabel = new JLabel();
        pathJText = new JTextField();
        fileNameJLabel = new JLabel();
        fileNameJText = new JTextField();

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

        fileNameJLabel.setText("File Name");
        fileNameJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        fileNameJLabel.setBackground(new Color(255, 0, 0));

        fileNameJText.setHorizontalAlignment(SwingConstants.CENTER);
        fileNameJText.setEditable(true);

        fileNameJText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                fileNameJTextKeyReleased(evt);
            }
        });

        typeJLabel.setText("Type");
        typeJLabel.setFont(new Font("Console", Font.PLAIN, 12));

        typeJComboBox.setModel(new DefaultComboBoxModel(this.picType));

        exportJButton.setText("Export");
        exportJButton.setFont(new Font("Lucida", Font.BOLD, 13));
        exportJButton.setBackground(Color.GREEN);
        exportJButton.setOpaque(false);
        exportJButton.setEnabled(false);
        exportJButton.addActionListener(this::exportJButtonActionPerformed);

        GroupLayout detailJPanelLayout = new GroupLayout(detailJPanel);
        detailJPanel.setLayout(detailJPanelLayout);

        detailJPanelLayout.setHorizontalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 100, 100)
                                        .addComponent(fileNameJLabel, GroupLayout.DEFAULT_SIZE, 100, 100)
                                        .addComponent(typeJLabel, GroupLayout.DEFAULT_SIZE, 100, 100))
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                                .addComponent(pathJText, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(pathBrowseJButton))
                                        .addComponent(fileNameJText, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)
                                        .addComponent(typeJComboBox, GroupLayout.DEFAULT_SIZE, 210, Short.MAX_VALUE)))
        );

        detailJPanelLayout.setVerticalGroup(
                detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addComponent(pathJLabel, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fileNameJLabel, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeJLabel, GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE))
                        .addGroup(detailJPanelLayout.createSequentialGroup()
                                .addGroup(detailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(pathJText)
                                        .addComponent(pathBrowseJButton))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fileNameJText)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(typeJComboBox))
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
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
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
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

        if (picName != null && !picName.equals("")){

            fileNameJLabel.setForeground(Color.BLACK);
            fileNameJLabel.setToolTipText(null);
        } else {
            fileNameJLabel.setForeground(Color.red);
            fileNameJLabel.setToolTipText("Please input file name.");
            fileNameJText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        exportJButton.setEnabled(allValid);
    }

    /**
     * Closes the dialog
     * @param evt window event
     */
    private void formWindowClosing(WindowEvent evt) {
        this.dispose();
    }

    /**
     * Select output path
     * @param evt mouse click event
     */
    private void pathBrowseJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastFolder);
        fileChooser.setDialogTitle("Select Output Directory");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        int returnValue = fileChooser.showDialog(this, "Ok");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile.isDirectory()) {
                outputFolder = selectedFile.getAbsolutePath();
            } else {
                JOptionPane.showMessageDialog(this, "Please select one directory", "File Format Error", JOptionPane.WARNING_MESSAGE);
            }

            lastFolder = outputFolder;

            pathJText.setText(outputFolder+" selected");
            validateInput();
        }
    }

    /**
     * fileNameJTextKeyReleased
     * @param evt Key event
     */
    private void fileNameJTextKeyReleased(KeyEvent evt) {

        picName = fileNameJText.getText();
        picName = FilePattern.matcher(picName).replaceAll("");
        fileNameJText.setText(picName);

        validateInput();
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

        File imageFile = new File(outputFolder + PDVMainClass.FILE_SEPARATOR + picName + finalImageType.getExtension());


        ProgressDialogX progressDialog = new ProgressDialogX(msDataDisplay,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(false);
        progressDialog.setTitle("Export TIC figure. Please Wait...");

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

                            Export.exportPic(chart, chartPanel.getBounds(), imageFile, finalImageType1);

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

                        Export.exportPic(chart, chartPanel.getBounds(), imageFile, finalImageType2);

                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                    }
                    progressDialog.setRunFinished();
                }
            }.start();
        }
    }
}

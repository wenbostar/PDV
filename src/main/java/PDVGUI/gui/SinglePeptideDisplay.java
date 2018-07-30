package PDVGUI.gui;

import PDVGUI.gui.utils.*;
import PDVGUI.gui.utils.FileImport.DatabaseImportDialog;
import PDVGUI.gui.utils.FileImport.DeNovoImportDialog;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.biology.*;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.DigestionPreferences;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

/**
 * Single peptide check
 * Created by Ken on 7/5/2017.
 */
public class SinglePeptideDisplay extends JFrame {

    private File spectrumFile;
    private String peptideSequence;
    private MSnSpectrum currentSpectrum;
    private PeptideAssumption peptideAssumption;
    private JPanel inputJPanel;
    private JPanel peptideSequenceJPanel;
    private JScrollPane peptideSequenceJScrollPane;
    private JLabel spectrumFileJLabel;
    private JLabel peptideSequenceJLabel;
    private JTextField spectrumFileJText;
    private JTextField peptideSequenceJText;
    private JButton startJButton;
    private JPanel spectrumAnnotationMenuPanel;
    private JPanel spectrumShowJPanel;
    private JMenuBar annotationMenuBar;
    private JCheckBox showDetailsJCheckBox;
    private JMenu switchPaneMenu;
    private JComboBox precursorIonUnit;
    private JTextField fragmentIonAccuracyTxt;

    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * ExceptionHandler import from utilities
     */
    private FrameExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/wenbostar/PDV");
    /**
     * PTMFactory import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Annotation settings
     */
    private AnnotationSettings annotationSettings = new AnnotationSettings();
    /**
     * Sequencing parameters
     */
    public SearchParameters searchParameters;
    /**
     * Utilities user preferences
     */
    public UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * All modifications map
     */
    private HashMap<String, ArrayList<String>> aASingleModification;
    /**
     * peptide Modifications
     */
    private ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();
    /**
     * All modifications sites map
     */
    private ArrayList<Integer> indexOfModification = new ArrayList<>();
    /**
     * Button list
     */
    private ArrayList<JButton> allButtons;
    /**
     * Last selected folder
     */
    public LastSelectedFolder lastSelectedFolder = new LastSelectedFolder();
    /**
     * Spectrum main Panel
     */
    private SpectrumMainPanel spectrumMainPanel;

    /**
     * Main constructor
     */
    public SinglePeptideDisplay(){
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            e.printStackTrace();
        }

        annotationSettings.setIntensityLimit(0.3);
        setUpGUI();
        setExtendedState(MAXIMIZED_BOTH);
        this.setVisible(true);

        aASingleModification = getModification();
    }

    /**
     * Set up GUI
     */
    private void setUpGUI(){

        spectrumMainPanel = new SpectrumMainPanel(this);

        initComponents();
        fragmentIonAccuracyTxt.setText("0.5");
        spectrumAnnotationMenuPanel.add(annotationMenuBar);
        this.precursorIonUnit.setEnabled(true);
        this.precursorIonUnit.setRenderer(new AlignedListCellRenderer(0));
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        inputJPanel = new JPanel();
        peptideSequenceJPanel = new JPanel();
        peptideSequenceJScrollPane = new JScrollPane();
        spectrumAnnotationMenuPanel = new JPanel();
        spectrumShowJPanel = new JPanel();
        spectrumFileJLabel = new JLabel();
        peptideSequenceJLabel = new JLabel();
        spectrumFileJText = new JTextField();
        peptideSequenceJText = new JTextField();
        startJButton = new JButton();
        precursorIonUnit = new JComboBox();
        fragmentIonAccuracyTxt = new JTextField();
        annotationMenuBar = new JMenuBar();
        switchPaneMenu = new JMenu();
        showDetailsJCheckBox = new JCheckBox();

        JPanel mainJPanel = new JPanel();
        JToolBar allJToolBar = new JToolBar();
        JButton spectrumFileJButton = new JButton();
        JLabel fragmentIonLbl = new JLabel();
        JMenuBar menuBar = new JMenuBar();
        JMenu fileJMenu = new JMenu();
        JMenuItem newJMenuItem = new JMenuItem();
        JMenuItem openDenovoJMenuItem = new JMenuItem();
        JMenuItem exitJMenuItem = new JMenuItem();
        JLabel showDetailsJLabel = new JLabel();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Single Display");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(760, 600));
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent evt) {
                formWindowClosing(evt);
            }

        });

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        menuBar.setBackground(new java.awt.Color(255, 255, 255));

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        newJMenuItem.setMnemonic('N');
        newJMenuItem.setText("Open Database Result");
        newJMenuItem.addActionListener(this::newJMenuItemActionPerformed);

        //fileJMenu.add(newJMenuItem);

        openDenovoJMenuItem.setMnemonic('D');
        openDenovoJMenuItem.setText("Open DeNovo Result");
        openDenovoJMenuItem.addActionListener(this::openDenovoJMenuItemActionPerformed);

        //fileJMenu.add(openDenovoJMenuItem);

        exitJMenuItem.setMnemonic('E');
        exitJMenuItem.setText("Exist");
        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);

        fileJMenu.add(exitJMenuItem);

        menuBar.add(fileJMenu);

        setJMenuBar(menuBar);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Input File and Peptide" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        inputJPanel.setBorder(titledBorder);
        inputJPanel.setOpaque(false);
        inputJPanel.setBackground(Color.white);

        spectrumFileJLabel.setText("Spectrum File(s) *");
        spectrumFileJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        spectrumFileJLabel.setForeground(new Color(255, 0, 0));

        spectrumFileJText.setEditable(false);
        spectrumFileJText.setHorizontalAlignment(SwingConstants.CENTER);

        spectrumFileJButton.setIcon(new ImageIcon(getClass().getResource("/icons/open.png")));
        spectrumFileJButton.setBorder(null);
        spectrumFileJButton.setBorderPainted(false);
        spectrumFileJButton.setContentAreaFilled(false);
        spectrumFileJButton.addActionListener(this::spectrumFileJButtonActionPerformed);

        peptideSequenceJLabel.setText("Peptide *");
        peptideSequenceJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        peptideSequenceJLabel.setForeground(new Color(255, 0, 0));

        peptideSequenceJText.setEditable(true);
        peptideSequenceJText.setHorizontalAlignment(SwingConstants.CENTER);
        peptideSequenceJText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                peptideSequenceJTextKeyReleased(evt);
            }
        });

        startJButton.setText("Start Annotating");
        startJButton.setBackground(Color.green);
        startJButton.setEnabled(false);
        startJButton.addActionListener(this::startJButtonActionPerformed);

        showDetailsJLabel.setText("Show details");
        showDetailsJLabel.setFont(new Font("Console", Font.PLAIN, 12));
        showDetailsJLabel.setToolTipText("Show spectrum details or not in spectrum panel");

        showDetailsJCheckBox.setSelected(false);
        showDetailsJCheckBox.setOpaque(false);

        precursorIonUnit.setModel(new DefaultComboBoxModel(new String[]{"Da", "ppm"}));
        fragmentIonLbl.setText("Fragment m/z Tolerance");
        fragmentIonLbl.setFont(new Font("Console", Font.PLAIN, 12));
        fragmentIonAccuracyTxt.setHorizontalAlignment(0);

        peptideSequenceJScrollPane.setVisible(false);
        peptideSequenceJScrollPane.setToolTipText("Click the amino acid to select modification");

        GroupLayout inputJPanelLayout  = new GroupLayout(inputJPanel);
        inputJPanel.setLayout(inputJPanelLayout);

        inputJPanelLayout.setHorizontalGroup(
                inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                .addGap(21,21,21)
                                .addGroup(inputJPanelLayout.createSequentialGroup()
                                        .addComponent(spectrumFileJLabel, 150, 150, 200)
                                        .addComponent(spectrumFileJText, GroupLayout.DEFAULT_SIZE, 280, 600)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(spectrumFileJButton)
                                        .addGap(20, 50, 100))
                                .addGroup(inputJPanelLayout.createSequentialGroup()
                                        .addComponent(peptideSequenceJLabel, 150, 150, 200)
                                        .addComponent(peptideSequenceJText,  GroupLayout.DEFAULT_SIZE, 150, 600))
                                .addGap(21,21,21))
                        .addComponent(peptideSequenceJScrollPane)
                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                .addGap(21,21,21)
                                .addGroup(inputJPanelLayout.createSequentialGroup()
                                        .addComponent(fragmentIonLbl, 100, 150, 200)
                                        .addComponent(fragmentIonAccuracyTxt, 30, 30, 50)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(precursorIonUnit, GroupLayout.DEFAULT_SIZE, 50, 70))
                                .addGap(10, 150, 500)
                                .addComponent(showDetailsJLabel, 100, 100, 100)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(showDetailsJCheckBox)
                                .addGap(20, 50, 200)
                                .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                                .addGap(100,600,2000)
                                                .addComponent(startJButton)))
                                .addGap(21,21,21))
        );

        inputJPanelLayout.linkSize(SwingConstants.HORIZONTAL, new java.awt.Component[] {spectrumFileJLabel, fragmentIonLbl, peptideSequenceJLabel});

        inputJPanelLayout.setVerticalGroup(
                inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                .addGap(15,15,15)
                                .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(spectrumFileJLabel)
                                                .addComponent(spectrumFileJText, 20, 30 ,30)
                                                .addComponent(spectrumFileJButton, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(peptideSequenceJLabel)
                                                .addComponent(peptideSequenceJText, 20, 30 ,30)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(peptideSequenceJScrollPane, 20, 40 ,65)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(fragmentIonLbl)
                                                .addComponent(fragmentIonAccuracyTxt, 20, 30 ,30)
                                                .addComponent(precursorIonUnit, 20, 30 ,30)
                                                .addComponent(showDetailsJLabel)
                                                .addComponent(showDetailsJCheckBox))
                                        .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                .addComponent(startJButton)))
                                .addGap(15,15,15))

        );

        allJToolBar.setBackground(new java.awt.Color(255, 255, 255));
        allJToolBar.setBorder(null);
        allJToolBar.setFloatable(false);
        allJToolBar.setRollover(true);
        allJToolBar.setBorderPainted(false);

        spectrumAnnotationMenuPanel.setLayout(new BoxLayout(spectrumAnnotationMenuPanel, BoxLayout.LINE_AXIS));
        allJToolBar.add(spectrumAnnotationMenuPanel);

        spectrumShowJPanel.setOpaque(false);
        spectrumMainPanel.setOpaque(false);

        GroupLayout spectrumMainPanelLayout = new GroupLayout(spectrumShowJPanel);
        spectrumShowJPanel.setLayout(spectrumMainPanelLayout);
        spectrumMainPanelLayout.setHorizontalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(spectrumMainPanel,GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        spectrumMainPanelLayout.setVerticalGroup(
                spectrumMainPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(spectrumMainPanelLayout.createSequentialGroup()
                                .addComponent(spectrumMainPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(spectrumShowJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(inputJPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inputJPanel, GroupLayout.DEFAULT_SIZE, 140, GroupLayout.DEFAULT_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(spectrumShowJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 1065, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 831, Short.MAX_VALUE)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }

    /**
     * Open a new database results display frame
     * @param evt Mouse click event
     */
    private void newJMenuItemActionPerformed(ActionEvent evt){
        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to display database results ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            PDVMainClass pdvMainClass = new PDVMainClass(true);

            new DatabaseImportDialog(pdvMainClass, true, this);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        }
    }

    /**
     * Open new De Novo results display
     * @param evt Mouse click event
     */
    private void openDenovoJMenuItemActionPerformed(ActionEvent evt){
        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to display De novo  results ?",
                "Give up current",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {

            new DeNovoImportDialog(this, true, this);

        } else if (value == JOptionPane.NO_OPTION || value == JOptionPane.CLOSED_OPTION) {
        }
    }

    /**
     * Exist
     * @param evt Mouse click event
     */
    private void exitJMenuItemActionPerformed(ActionEvent evt){
        formWindowClosing(null);
    }

    /**
     * Select a mgf file with only one single spectral
     * @param evt mouse click event
     */
    private void spectrumFileJButtonActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectrum File");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        if(modificationMatches != null){
            modificationMatches.clear();
        }
        if(indexOfModification != null){
            indexOfModification.clear();
        }

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Mascot Generic Format (.mgf)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(this, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            for (File selectedFile : selectedFiles) {
                if (selectedFile.exists()) {

                    spectrumFile = selectedFile;

                    lastSelectedFolder.setLastSelectedFolder(selectedFile.getParent());
                }

            }
            spectrumFileJText.setText(spectrumFile.getName() + " file selected.");
            validateInput();
        }
    }

    /**
     * PeptideSequenceJTextKeyReleased
     * @param evt Key event
     */
    private void peptideSequenceJTextKeyReleased(KeyEvent evt){
        peptideSequence = peptideSequenceJText.getText();
        if (peptideSequence.matches("[A-Za-z]+")){
            Font font = new Font("Arial bold",Font.PLAIN,16);
            if(peptideSequence != null && peptideSequence.length() != 0 && !peptideSequence.contains(" ")){
                peptideSequence = peptideSequenceJText.getText().toUpperCase();
                peptideSequenceJText.setText(peptideSequence);

                allButtons = new ArrayList<>();

                peptideSequenceJPanel.removeAll();
                peptideSequenceJPanel.setOpaque(true);
                peptideSequenceJPanel.setBackground(Color.WHITE);
                peptideSequenceJScrollPane.setBackground(Color.WHITE);
                peptideSequenceJScrollPane.setVisible(true);
                String[] peptideArray = peptideSequence.split("");

                ArrayList<String> peptideList = new ArrayList<>();
                peptideList.add("NH-");
                for(String eachOriAA: peptideArray){
                    peptideList.add(eachOriAA);
                }

                peptideList.add("-COOH");

                for(String eachAA: peptideList){
                    JButton eachAAJButton = new JButton(eachAA);
                    eachAAJButton.setBorder(null);
                    eachAAJButton.setBorderPainted(false);
                    eachAAJButton.setContentAreaFilled(false);
                    eachAAJButton.setOpaque(false);
                    eachAAJButton.setPreferredSize(new Dimension(40,40));
                    if(peptideList.indexOf(eachAA) == peptideList.size()-1){
                        eachAAJButton.setPreferredSize(new Dimension(60,40));
                    }
                    eachAAJButton.setFont(font);
                    allButtons.add(eachAAJButton);
                    peptideSequenceJPanel.add(eachAAJButton);
                    eachAAJButton.addActionListener(this::eachAAJButtonActionPerformed);
                }
                peptideSequenceJScrollPane.setOpaque(true);
                peptideSequenceJScrollPane.setViewportView(peptideSequenceJPanel);
                inputJPanel.revalidate();
                inputJPanel.repaint();
            }
        } else {
            JOptionPane.showMessageDialog(this, JOptionEditorPane.getJOptionEditorPane(
                    "Please input correct peptide sequence."),
                    "Format Error", JOptionPane.WARNING_MESSAGE);
        }

        validateInput();
    }

    /**
     * EachAAJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void eachAAJButtonActionPerformed(ActionEvent evt) {
        JButton jButton = (JButton) evt.getSource();

        showSingleAAModification(jButton);
    }

    /**
     * Set modification for selected amino acid
     * @param singleModification Modification
     * @param aAJButton JButton
     */
    public void setSingleModification(String singleModification, JButton aAJButton){
        Integer aASite = allButtons.indexOf(aAJButton);

        String aAName = aAJButton.getText();

        if(singleModification != "null"){

            ModificationMatch modificationMatch;

            if(aAName == "NH-"){
                modificationMatch = new ModificationMatch(singleModification, false, 1);
            }else if(aAName == "-COOH"){
                modificationMatch = new ModificationMatch(singleModification, false, aASite - 1);
            }else {
                modificationMatch = new ModificationMatch(singleModification, false, aASite);
            }

            aAJButton.setBackground(Color.blue);
            aAJButton.setForeground(Color.blue);
            aAJButton.setToolTipText(modificationMatch.getTheoreticPtm());
            if(indexOfModification.contains(aASite)){
                modificationMatches.remove(indexOfModification.indexOf(aASite));
                indexOfModification.remove(aASite);
                indexOfModification.add(aASite);
                modificationMatches.add(modificationMatch);
            }else {
                indexOfModification.add(aASite);
                modificationMatches.add(modificationMatch);
            }
        }else {
            aAJButton.setBackground(Color.BLACK);
            aAJButton.setForeground(Color.BLACK);
            aAJButton.setToolTipText("Click the amino acid to select modification");
            if(indexOfModification.contains(aASite)){
                modificationMatches.remove(indexOfModification.indexOf(aASite));
                indexOfModification.remove(aASite);
            }
        }
    }

    /**
     * Show all modifications of one single amino acid for selected button
     * @param aAJButton JButton
     */
    private void showSingleAAModification(JButton aAJButton){
        String aAName = aAJButton.getText();

        ArrayList<String> singleModifications = new ArrayList<>();
        if (aAName == "NH-"){
            singleModifications.addAll(aASingleModification.get("N-terminus"));
        }else if(aAName == "-COOH"){
            singleModifications.addAll(aASingleModification.get("C-terminus"));
        }else {
            singleModifications = aASingleModification.get(aAName);
        }

        if(singleModifications != null){

            Collections.sort(singleModifications);

            String[] singleModificationsArray = singleModifications.toArray(new String[0]);

            new ModificationDialog(this, aAJButton, singleModificationsArray);
        }
    }

    /**
     * Get all modifications as map
     * @return HashMap
     */
    public HashMap<String, ArrayList< String >> getModification(){
        HashMap<String, ArrayList< String > > modificationMass = new HashMap<>();

        for (int i = 0;i<300;i++){
            ArrayList<String> orderedModifications = ptmFactory.getPTMs();
            if (orderedModifications.size() >300){
                for (String  modificationName : orderedModifications){

                    String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
                    String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
                    if(modificationMass.containsKey(aminoAcidName)){
                        modificationMass.get(aminoAcidName).add(modificationName);
                    }else {
                        ArrayList< String > singleModi = new ArrayList<>();
                        singleModi.add(modificationName);
                        modificationMass.put(aminoAcidName, singleModi);
                    }
                }

                break;
            }else{
                ptmFactory = PTMFactory.getInstance();
                try {Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return modificationMass;
    }

    /**
     * Start annotating according to spectrum and peptide details
     * @param evt Mouse click event
     */
    private void startJButtonActionPerformed(ActionEvent evt) {
        SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
        ArrayList<String > spectrumTitle;

        Double fragmentIonMZTolerance = 0.5;
        if (fragmentIonAccuracyTxt.getText()!= ""){
            fragmentIonMZTolerance = Double.valueOf(fragmentIonAccuracyTxt.getText());
        }else {
            fragmentIonMZTolerance = 0.5;
        }

        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 1), new Color(0, 153, 0));
        SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.PEPTIDE_FRAGMENT_ION, 4), new Color(255, 102, 0));

        SearchParameters.MassAccuracyType fragmentAccuracyType = SearchParameters.MassAccuracyType.DA;
        if(precursorIonUnit.getSelectedIndex() == 0){
            fragmentAccuracyType = SearchParameters.MassAccuracyType.DA;
        }else {
            fragmentAccuracyType = SearchParameters.MassAccuracyType.PPM;
        }

        if (searchParameters == null) {
            searchParameters = new SearchParameters();
            searchParameters.setDigestionPreferences(DigestionPreferences.getDefaultPreferences());
        }

        ArrayList<String> modification =  ptmFactory.getPTMs();

        PtmSettings ptmSettings = new PtmSettings();

        for(String fixedModification: modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification: modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }

        searchParameters.setPtmSettings(ptmSettings);

        searchParameters.setFragmentAccuracyType(fragmentAccuracyType);
        searchParameters.setFragmentIonAccuracy(fragmentIonMZTolerance);

        annotationSettings.setPreferencesFromSearchParameters(searchParameters);

        spectrumMainPanel.updateSearchParameters(searchParameters);
        spectrumMainPanel.setShowDetails(showDetailsJCheckBox.isSelected());
        spectrumMainPanel.setAnnotationSettings(annotationSettings);

        Peptide peptide = new Peptide(peptideSequence, modificationMatches);
        peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1,1), 0, null);

        try {
            spectrumFactory.addSpectra(spectrumFile);
            spectrumTitle = spectrumFactory.getSpectrumTitles(spectrumFile.getName());
            currentSpectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile.getName(), spectrumTitle.get(0));

            int charge = currentSpectrum.getPrecursor().getPossibleCharges().get(0).value;

            peptideAssumption.setIdentificationCharge(new Charge(1, charge));

            switchPaneMenu.setEnabled(true);

            updateSpectrum();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (MzMLUnmarshallerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update spectrum
     */
    private void updateSpectrum(){

        TitledBorder titledBorder = BorderFactory.createTitledBorder(peptideAssumption.getPeptide().getSequence() + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));

        spectrumShowJPanel.setBorder(titledBorder);

        SpectrumMatch spectrumMatch = new SpectrumMatch();

        spectrumMatch.setBestPeptideAssumption(peptideAssumption);

        spectrumMainPanel.updateSpectrum(spectrumMatch.getBestPeptideAssumption(), currentSpectrum, "1");

        spectrumShowJPanel.revalidate();
        spectrumShowJPanel.repaint();
    }

    /**
     * Validates the input and enable start button
     */
    private void validateInput() {
        boolean allValid = true;

        if (spectrumFile != null) {
            spectrumFileJLabel.setForeground(Color.BLACK);
            spectrumFileJLabel.setToolTipText(null);
        } else {
            spectrumFileJLabel.setForeground(Color.RED);
            spectrumFileJLabel.setToolTipText("Please select the spectrum file");
            spectrumFileJText.setToolTipText("Please select the spectrum file");
            spectrumFileJText.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            allValid = false;
        }

        if(peptideSequence != null && peptideSequence.length() != 0 && !peptideSequence.contains(" ")){
            peptideSequenceJLabel.setForeground(Color.BLACK);
            peptideSequenceJLabel.setToolTipText(null);
        }else {
            peptideSequenceJLabel.setForeground(Color.RED);
            peptideSequenceJLabel.setToolTipText("Please input one peptide");
            peptideSequenceJText.setToolTipText("Please input one peptide");
            allValid = false;
        }

        startJButton.setEnabled(allValid);
    }

    /**
     * Close frame
     * @param evt Window event
     */
    private void formWindowClosing(WindowEvent evt) {
        close();
    }

    /**
     * Close
     */
    public void close() {
        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to give up it and close PDV? ",
                "Close PDV",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {
            closePDV();
        } else if (value == JOptionPane.NO_OPTION) {

        }
    }

    /**
     * Close PDV
     */
    private void closePDV(){
        exceptionHandler.setIgnoreExceptions(true);

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        final SinglePeptideDisplay singlePeptideDisplay = this;

        new Thread(() -> {
            try {
                progressDialog.setVisible(true);
            } catch (IndexOutOfBoundsException e) {
                // ignore
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(() -> {
            try {

                if (!progressDialog.isRunCanceled()) {
                    TempFilesManager.deleteTempFolders();
                }

            } catch (Exception e) {
                e.printStackTrace();
                catchException(e);
            } finally {
                progressDialog.setRunFinished();

                singlePeptideDisplay.setVisible(false);

                System.exit(0);
            }
        });
    }

    /**
     * Catch exception
     * @param e Exception
     */
    private void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }
}
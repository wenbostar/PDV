package PDVGUI.gui.utils;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Check peptide dialog
 * Created by Ken on 9/7/2017.
 */
public class CheckPeptideJDialog extends JDialog {

    private JPanel inputJPanel;
    private JPanel peptideSequenceJPanel;
    private JScrollPane peptideSequenceJScrollPane;
    private JLabel peptideSequenceJLabel;
    private JTextField peptideSequenceJText;
    private JButton setJButton;

    /**
     * Button list
     */
    private ArrayList<JButton> allButtons;
    /**
     * All modifications map
     */
    private HashMap<String, ArrayList<String>> aASingleModification;
    /**
     * PTMFactory import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * All modifications sites map
     */
    private ArrayList<Integer> indexOfModification = new ArrayList<>();
    /**
     * peptide Modifications
     */
    private ArrayList<ModificationMatch> modificationMatches = new ArrayList<>();
    /**
     * Spectrum main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * Spectrum key
     */
    private String spectrumKey;
    /**
     * Peptide sequence
     */
    private String peptideSequence;

    /**
     * Constructor
     * @param spectrumMainPanel Spectrum main panel
     * @param spectrumKey Spectrum key
     */
    public CheckPeptideJDialog(SpectrumMainPanel spectrumMainPanel, String spectrumKey){

        super(spectrumMainPanel.parentFrame, true);

        this.spectrumMainPanel = spectrumMainPanel;

        this.spectrumKey = spectrumKey;

        getModification();

        setupGUI();

        setLocationRelativeTo(spectrumMainPanel.peptideCheckMenu);

        setVisible(true);
    }

    /**
     * Setup GUI
     */
    private void setupGUI(){
        initComponents();
        if(spectrumMainPanel.checkPeptideMap.containsKey(spectrumKey)){
            peptideSequenceJText.setText(spectrumMainPanel.checkPeptideMap.get(spectrumKey).getSequence());
            peptideSequenceJTextKeyReleased(null);
        }

        validateInput();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();

        inputJPanel = new JPanel();
        peptideSequenceJPanel = new JPanel();
        peptideSequenceJScrollPane = new JScrollPane();
        peptideSequenceJLabel = new JLabel();
        peptideSequenceJText = new JTextField();
        setJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - Check Peptide Input");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        setPreferredSize(new Dimension(800, 180));

        setMinimumSize(new Dimension(800, 180));

        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));

        inputJPanel.setBackground(new Color(221, 250, 255));
        inputJPanel.setOpaque(false);

        peptideSequenceJLabel.setText("Peptide Sequence");
        peptideSequenceJLabel.setFont(new Font("Console", Font.PLAIN, 11));

        peptideSequenceJText.setEditable(true);
        peptideSequenceJText.setHorizontalAlignment(SwingConstants.CENTER);
        peptideSequenceJText.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(KeyEvent evt) {
                peptideSequenceJTextKeyReleased(evt);
            }
        });

        peptideSequenceJScrollPane.setOpaque(false);
        peptideSequenceJScrollPane.setVisible(false);
        peptideSequenceJScrollPane.setToolTipText("Click the amino acid to select modification");

        GroupLayout inputJPanelLayout  = new GroupLayout(inputJPanel);
        inputJPanel.setLayout(inputJPanelLayout);

        inputJPanelLayout.setHorizontalGroup(
                inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(peptideSequenceJLabel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(peptideSequenceJText, GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
                                .addContainerGap())
                        .addComponent(peptideSequenceJScrollPane)
        );

        inputJPanelLayout.setVerticalGroup(
                inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inputJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(inputJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(peptideSequenceJLabel, 30, 30, 60)
                                        .addComponent(peptideSequenceJText, 30, 30, 30))
                                .addGap(5,5,5)
                                .addComponent(peptideSequenceJScrollPane, 60, 60 ,80))
        );

        setJButton.setEnabled(false);
        setJButton.setText("Set Peptide");
        setJButton.setFont(new Font("Lucida", Font.BOLD, 12));
        setJButton.addActionListener(this::setJButtonActionPerformed);

        javax.swing.GroupLayout mainJPanelLayout = new javax.swing.GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(setJButton, 30, 60, 60)
                                        .addComponent(inputJPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inputJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 140, GroupLayout.DEFAULT_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(setJButton, 20, 25 ,30)
                                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
     * PeptideSequenceJTextKeyReleased
     * @param evt Key event
     */
    private void peptideSequenceJTextKeyReleased(KeyEvent evt){
        peptideSequence = peptideSequenceJText.getText();
        Font font = new Font("Arial bold",Font.PLAIN,16);
        if(peptideSequence != null && peptideSequence.length() != 0 && !peptideSequence.contains(" ")){
            peptideSequence = peptideSequenceJText.getText().toUpperCase();
            peptideSequenceJText.setText(peptideSequence);

            allButtons = new ArrayList<>();

            peptideSequenceJPanel.removeAll();
            peptideSequenceJPanel.setOpaque(false);
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
            peptideSequenceJScrollPane.setOpaque(false);
            peptideSequenceJScrollPane.setViewportView(peptideSequenceJPanel);
            inputJPanel.revalidate();
            inputJPanel.repaint();
        }
        validateInput();
    }

    /**
     * Set peptide
     * @param evt Mouse click event
     */
    private void setJButtonActionPerformed(ActionEvent evt){

        Peptide peptide = new Peptide(peptideSequence, modificationMatches);

        spectrumMainPanel.checkPeptideMap.put(spectrumKey, peptide);

        spectrumMainPanel.updateSpectrum();

        formWindowClosing(null);
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
    private void getModification(){
        HashMap<String, ArrayList< String > > modificationMass = new HashMap<>();

            ArrayList<String> orderedModifications = ptmFactory.getPTMs();

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

        aASingleModification = modificationMass;
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
     * Validates the input and enable start button
     */
    private void validateInput() {
        boolean allValid = true;

        if(peptideSequence != null && peptideSequence.length() != 0 && !peptideSequence.contains(" ")){
            peptideSequenceJLabel.setForeground(Color.BLACK);
            peptideSequenceJLabel.setToolTipText(null);
        }else {
            peptideSequenceJLabel.setForeground(Color.RED);
            peptideSequenceJLabel.setToolTipText("Please input one peptide");
            peptideSequenceJText.setToolTipText("Please input one peptide");
            allValid = false;
        }

        setJButton.setEnabled(allValid);
    }
}

package PDVGUI.gui;

import com.compomics.util.experiment.biology.AminoAcid;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import PDVGUI.gui.utils.VariantPeptideShowPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Check variant peptide
 * Created by Ken on 6/15/2017.
 */
public class VariantPeptideDialog extends JDialog{

    private JTextField variantPeptideJTextField;
    private JTextField originalPeptideJTextField;
    private JPanel peptideContainerJPanel;
    private JTable modificationJTable = new JTable();
    private JTextField toleranceJTextField;
    private JScrollPane modificationJScrollPanel = new JScrollPane();
    private JComboBox toleranceUntJComBox;

    /**
     * Original peptide sequence
     */
    private String oldPeptideSequence;
    /**
     * Variant peptide sequence
     */
    private String variantPeptideSequence;
    /**
     * PTMFactory import from utilities
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * All peptide variant peptide maps list
     */
    private HashMap<String, String> variantPeptideMap;
    /**
     * Current peptide psm key
     */
    private String selectPsmKey;
    /**
     * Original amino acid
     */
    private String oldAminoAcid;
    /**
     * Variant amino acid
     */
    private String variantAminoAcid;
    /**
     * Variant amino acid site
     */
    private int variantLocation;
    /**
     * All modification mass maps list
     */
    private HashMap<String, HashMap<Double, String>> modificationAAMass;
    /**
     * Tolerance
     */
    private Double tolerance = 10.0;

    /**
     * New constructor
     * @param pdvMainClass parent frame
     * @param oldPeptideSequence original peptide sequence
     * @param variantPeptideMap variant peptide sequence
     * @param selectPsmKey current psm key
     */
    public VariantPeptideDialog(PDVMainClass pdvMainClass, String oldPeptideSequence, HashMap<String, String> variantPeptideMap, String selectPsmKey){
        super(pdvMainClass, true);
        this.oldPeptideSequence = oldPeptideSequence;
        this.variantPeptideMap = variantPeptideMap;
        this.selectPsmKey = selectPsmKey;

        modificationAAMass = getModificationMass();

        setUpGUI();
        this.setLocationRelativeTo(pdvMainClass);

        this.setTitle(oldPeptideSequence);
        this.setVisible(true);
    }

    /**
     * Set up GUI
     */
    private void setUpGUI(){

        initComponents();

        this.toleranceUntJComBox.setEnabled(true);
        this.toleranceUntJComBox.setRenderer(new AlignedListCellRenderer(0));

        originalPeptideJTextField.setText(oldPeptideSequence.toUpperCase());

        if(variantPeptideMap.containsKey(selectPsmKey)){
            variantPeptideSequence = variantPeptideMap.get(selectPsmKey);
            variantPeptideJTextField.setText(variantPeptideSequence);
        }else {
            variantPeptideJTextField.setText(oldPeptideSequence.toUpperCase());
        }

        toleranceJTextField.setText(String.valueOf(tolerance));
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){

        toleranceJTextField = new JTextField();
        toleranceUntJComBox = new JComboBox();
        variantPeptideJTextField = new JTextField();
        originalPeptideJTextField = new JTextField();
        peptideContainerJPanel = new JPanel();
        JPanel backgroundJPanel = new JPanel();
        JPanel peptideDetailJPanel = new JPanel();
        JPanel displayJPanel = new JPanel();
        JLabel variantPeptideJLabel = new JLabel();
        JLabel originalPeptideJLabel = new JLabel();
        JLabel toleranceJLabel = new JLabel();
        JButton okJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(Color.white);

        variantPeptideJLabel.setText("Variant Peptide");
        variantPeptideJLabel.setFont(new Font("Console", Font.PLAIN, 11));

        originalPeptideJLabel.setText("Original Peptide");
        originalPeptideJLabel.setFont(new Font("Console", Font.PLAIN, 11));

        variantPeptideJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        variantPeptideJTextField.setEditable(true);

        originalPeptideJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        originalPeptideJTextField.setEditable(true);

        toleranceJLabel.setText("Tolerance");
        toleranceJLabel.setFont(new Font("Console", Font.PLAIN, 11));

        toleranceUntJComBox.setModel(new DefaultComboBoxModel(new String[]{"ppm", "Da"}));

        toleranceJTextField.setHorizontalAlignment(SwingConstants.CENTER);
        toleranceJTextField.setEditable(true);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Input Peptide" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        peptideDetailJPanel.setBorder(titledBorder);
        peptideDetailJPanel.setOpaque(false);
        peptideDetailJPanel.setBackground(Color.white);

        okJButton.setText("OK");
        okJButton.setBackground(Color.GREEN);
        okJButton.addActionListener(this::okJButtonActionPerformed);

        GroupLayout peptideDetailJPanelLayout = new GroupLayout(peptideDetailJPanel);
        peptideDetailJPanel.setLayout(peptideDetailJPanelLayout);

        peptideDetailJPanelLayout.setHorizontalGroup(
                peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(peptideDetailJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGap(20,20,100)
                                .addGroup(peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(peptideDetailJPanelLayout.createSequentialGroup()
                                                .addComponent(originalPeptideJLabel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                                .addComponent(originalPeptideJTextField, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                                        .addGroup(peptideDetailJPanelLayout.createSequentialGroup()
                                                .addComponent(variantPeptideJLabel, GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                                                .addComponent(variantPeptideJTextField, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(toleranceJLabel, GroupLayout.DEFAULT_SIZE, 50, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(toleranceJTextField)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(toleranceUntJComBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(okJButton)
                                .addGap(20,20,400)
                                .addContainerGap())
        );

        peptideDetailJPanelLayout.setVerticalGroup(
                peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(peptideDetailJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addGroup(peptideDetailJPanelLayout.createSequentialGroup()
                                                .addGroup(peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(originalPeptideJLabel, 5, 10, 30)
                                                        .addComponent(originalPeptideJTextField))
                                                .addGroup(peptideDetailJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                        .addComponent(variantPeptideJLabel, 5, 10, 30)
                                                        .addComponent(variantPeptideJTextField)))
                                        .addComponent(toleranceJLabel, 5, 10, 30)
                                        .addComponent(toleranceJTextField, 5, 10, 30)
                                        .addComponent(toleranceUntJComBox, 5, 10, 30)
                                        .addComponent(okJButton))
                                .addContainerGap())
        );

        titledBorder = BorderFactory.createTitledBorder("Variant Peptide display" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        displayJPanel.setBorder(titledBorder);
        displayJPanel.setOpaque(false);
        displayJPanel.setBackground(Color.white);

        peptideContainerJPanel.setBackground(Color.white);
        peptideContainerJPanel.setOpaque(false);

        GroupLayout displayJPanelLayout = new GroupLayout(displayJPanel);
        displayJPanel.setLayout(displayJPanelLayout);

        displayJPanelLayout.setHorizontalGroup(
                displayJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(displayJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(displayJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(peptideContainerJPanel, GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE))
                                .addContainerGap())
        );

        displayJPanelLayout.setVerticalGroup(
                displayJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(displayJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(displayJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(peptideContainerJPanel, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                                .addContainerGap())
        );

        GroupLayout backgroundJPanelLayout = new GroupLayout(backgroundJPanel);
        backgroundJPanel.setLayout(backgroundJPanelLayout);

        backgroundJPanelLayout.setHorizontalGroup(
                backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(peptideDetailJPanel)
                                        .addComponent(displayJPanel))
                                .addContainerGap())
        );

        backgroundJPanelLayout.setVerticalGroup(
                backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(peptideDetailJPanel)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(displayJPanel)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
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
     * Start calculating
     * @param evt mouse click event
     */
    private void okJButtonActionPerformed(ActionEvent evt){
        variantPeptideSequence = variantPeptideJTextField.getText().toUpperCase();
        variantPeptideJTextField.setText(variantPeptideSequence);

        tolerance = Double.valueOf(toleranceJTextField.getText());
        toleranceJTextField.setText(String.valueOf(tolerance));

        String toleranceUnit = (String) toleranceUntJComBox.getSelectedItem();
        if(Objects.equals(toleranceUnit, "ppm")){
            tolerance = tolerance * (Math.pow(10, -6));
        }

        if(variantPeptideSequence.length() != oldPeptideSequence.length()){
            JOptionPane.showMessageDialog(null, "The variant peptide has different length with current one.", "Warning", JOptionPane.WARNING_MESSAGE);
        }else {

            char[] oldOne = oldPeptideSequence.toCharArray();
            char[] variantOne = variantPeptideSequence.toCharArray();
            int num = 0;
            for(int i=0;i<oldOne.length;i++){
                if(oldOne[i] != variantOne[i]){
                    num ++;
                    variantLocation = i;
                    oldAminoAcid = String.valueOf(oldOne[i]);
                    variantAminoAcid = String.valueOf(variantOne[i]);
                }
            }
            if ( num == 1 ){

                Double massDifference = AminoAcid.getAminoAcid(variantAminoAcid).getMonoisotopicMass() - AminoAcid.getAminoAcid(oldAminoAcid).getMonoisotopicMass();

                HashMap<Double, String> normalAminoAcidModi;
                HashMap<Double, String> nTermAminoAcidModi;
                HashMap<Double, String> cTermAminoAcidModi;

                ArrayList<String> modificationName = new ArrayList<>();
                HashMap<String, Double> normalModificationsMap = new HashMap<>();

                if(modificationAAMass.get(oldAminoAcid) != null){
                    normalAminoAcidModi = modificationAAMass.get(oldAminoAcid);
                    for(HashMap.Entry<Double, String> met: normalAminoAcidModi.entrySet()){
                        if(Math.abs(met.getKey() - massDifference) < tolerance ){
                            normalModificationsMap.put(met.getValue(), met.getKey());
                            modificationName.add(met.getValue());
                        }
                    }
                }

                if(variantLocation == 0){
                    nTermAminoAcidModi = modificationAAMass.get("N-terminus");
                    for(HashMap.Entry<Double, String> met: nTermAminoAcidModi.entrySet()){
                        if(Math.abs(met.getKey() - massDifference) < tolerance ){
                            normalModificationsMap.put(met.getValue(), met.getKey());
                            modificationName.add(met.getValue());
                        }
                    }
                }else if(variantLocation == (oldOne.length-1)){
                    cTermAminoAcidModi = modificationAAMass.get("C-terminus");
                    for(HashMap.Entry<Double, String> met: cTermAminoAcidModi.entrySet()){
                        if(Math.abs(met.getKey() - massDifference) < tolerance ){
                            normalModificationsMap.put(met.getValue(), met.getKey());
                            modificationName.add(met.getValue());
                        }
                    }
                }

                VariantPeptideShowPanel variantPeptideShowPanel = new VariantPeptideShowPanel(oldPeptideSequence, variantPeptideSequence,  variantLocation);
                variantPeptideShowPanel.setBackground(Color.white);

                JScrollPane peptideJScrollPane = new JScrollPane(variantPeptideShowPanel);
                peptideJScrollPane.setOpaque(false);
                peptideJScrollPane.setBackground(Color.white);
                peptideJScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_NEVER);

                if(modificationName.size() != 0){

                    peptideContainerJPanel.removeAll();

                    modificationJTable.setRowHeight(50);
                    modificationJScrollPanel.setViewportView(modificationJTable);
                    modificationJScrollPanel.setBackground(Color.white);
                    modificationJScrollPanel.setOpaque(false);

                    modificationJTable.setModel(new ModificationDefaultModel(variantLocation, oldAminoAcid, variantAminoAcid, modificationName, normalModificationsMap));
                    modificationJTable.getColumn("Location").setMinWidth(60);
                    modificationJTable.getColumn("Location").setMaxWidth(60);
                    DefaultTableCellRenderer r = new DefaultTableCellRenderer();
                    r.setHorizontalAlignment(JLabel.CENTER);
                    modificationJTable.setDefaultRenderer(Object.class, r);

                    ((DefaultTableCellRenderer)modificationJTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);

                    GroupLayout peptideContainerJPanelLayout  = new GroupLayout(peptideContainerJPanel);
                    peptideContainerJPanel.setLayout(peptideContainerJPanelLayout);

                    peptideContainerJPanelLayout.setHorizontalGroup(
                            peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(peptideContainerJPanelLayout.createSequentialGroup()
                                            .addContainerGap()
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addGroup(peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                                    .addComponent(peptideJScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                                    .addComponent(modificationJScrollPanel, GroupLayout.DEFAULT_SIZE,270, Short.MAX_VALUE))
                                            .addContainerGap())
                    );

                    peptideContainerJPanelLayout.setVerticalGroup(
                            peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(peptideContainerJPanelLayout.createSequentialGroup()
                                            .addContainerGap()
                                            .addGroup(peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addGroup(peptideContainerJPanelLayout.createSequentialGroup()
                                                            .addComponent(peptideJScrollPane, GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                                            .addComponent(modificationJScrollPanel, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))
                                            )
                                            .addContainerGap())
                    );

                    peptideContainerJPanel.revalidate();
                    peptideContainerJPanel.repaint();

                }else {
                    JPanel tempPanel = new JPanel();
                    tempPanel.setOpaque(false);
                    JLabel tempLabel = new JLabel("<html><body>There are no possible for <br> this variant peptide!</body></html>");
                    tempLabel.setFont(tempLabel.getFont().deriveFont(tempLabel.getFont().getStyle() | java.awt.Font.BOLD));
                    tempPanel.setLayout(new GridBagLayout());
                    tempPanel.add(tempLabel, new GridBagConstraints());
                    peptideContainerJPanel.removeAll();

                    GroupLayout peptideContainerJPanelLayout  = new GroupLayout(peptideContainerJPanel);
                    peptideContainerJPanel.setLayout(peptideContainerJPanelLayout);

                    peptideContainerJPanelLayout.setHorizontalGroup(
                            peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(peptideContainerJPanelLayout.createSequentialGroup()
                                            .addContainerGap()
                                            .addGroup(peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                                    .addComponent(peptideJScrollPane, GroupLayout.DEFAULT_SIZE,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                                    .addComponent(tempLabel))

                                            .addContainerGap())
                    );

                    peptideContainerJPanelLayout.setVerticalGroup(
                            peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                    .addGroup(peptideContainerJPanelLayout.createSequentialGroup()
                                            .addContainerGap()
                                            .addComponent(tempLabel)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(peptideJScrollPane, GroupLayout.DEFAULT_SIZE,GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                            .addContainerGap())
                    );

                    peptideContainerJPanel.revalidate();
                    peptideContainerJPanel.repaint();

                }

                if (variantPeptideMap.containsKey(selectPsmKey)){
                    variantPeptideMap.remove(selectPsmKey);
                    variantPeptideMap.put(selectPsmKey,variantPeptideSequence);
                }else {
                    variantPeptideMap.put(selectPsmKey,variantPeptideSequence);
                }

            }else {

                JLabel tempLabel = new JLabel("Please input peptide with only one variant residue...");
                tempLabel.setFont(tempLabel.getFont().deriveFont(tempLabel.getFont().getStyle() | java.awt.Font.BOLD));

                peptideContainerJPanel.removeAll();

                GroupLayout peptideContainerJPanelLayout  = new GroupLayout(peptideContainerJPanel);
                peptideContainerJPanel.setLayout(peptideContainerJPanelLayout);

                peptideContainerJPanelLayout.setHorizontalGroup(
                        peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(tempLabel)
                );
                peptideContainerJPanelLayout.setVerticalGroup(
                        peptideContainerJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(tempLabel)
                );

                peptideContainerJPanel.revalidate();
                peptideContainerJPanel.repaint();

            }
        }
    }

    /**
     * Get all variant peptide list
     * @return HashMap
     */
    public HashMap<String, String> getVariantPeptideMap(){
        return variantPeptideMap;
    }

    /**
     * Get all modification mass map
     * @return hashMap
     */
    public HashMap<String,HashMap<Double, String >> getModificationMass(){

        HashMap<String,HashMap<Double, String > > modificationMass = new HashMap<>();
        ArrayList<String> orderedModifications = ptmFactory.getPTMs();
        for (String  modificationName : orderedModifications){
            String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
            String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
            if(modificationMass.containsKey(aminoAcidName)){
                modificationMass.get(aminoAcidName).put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
            }else {
                HashMap<Double, String> singleModi = new HashMap<>();
                singleModi.put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
                modificationMass.put(aminoAcidName, singleModi);
            }
        }
        return modificationMass;
    }

    /**
     * Modification table model
     */
    public class ModificationDefaultModel extends DefaultTableModel {

        /**
         * Variant amino acid site
         */
        private int location;
        /**
         * Original amino acid
         */
        private String oldAminoAcid;
        /**
         * Variant amino acid
         */
        private String variantAminoAcid;
        /**
         * Modification map
         */
        private HashMap<String, Double> normalModificationsMap;
        /**
         * Modification name list
         */
        private ArrayList<String> modificationName;
        /**
         * DecimalFormat
         */
        private DecimalFormat df = new DecimalFormat("#.00000");

        /**
         * New constructor
         * @param location variant amino acid site
         * @param oldAminoAcid original amino acid
         * @param variantAminoAcid variant amino acid
         * @param modificationName modification name list
         * @param normalModificationsMap modification map
         */
        public ModificationDefaultModel(int location, String oldAminoAcid, String variantAminoAcid, ArrayList<String> modificationName, HashMap<String, Double> normalModificationsMap) {
            this.location = location;
            this.oldAminoAcid = oldAminoAcid;
            this.variantAminoAcid = variantAminoAcid;
            this.modificationName = modificationName;
            this.normalModificationsMap = normalModificationsMap;
            DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
            decimalFormatSymbols.setDecimalSeparator('.');
            df.setDecimalFormatSymbols(decimalFormatSymbols);
        }

        @Override
        public int getRowCount() {
            if(modificationName == null){
                return 0;
            }
            return modificationName.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Location";
                case 1:
                    return "Original AA";
                case 2:
                    return "Modification";
                case 3:
                    return "Variant AA";
                case 4:
                    return "Mass Difference";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {

            String name = modificationName.get(row);

            Double massDifference = AminoAcid.getAminoAcid(variantAminoAcid).getMonoisotopicMass() - AminoAcid.getAminoAcid(oldAminoAcid).getMonoisotopicMass();

            switch (column) {
                case 0:
                    return location;
                case 1:
                    return "<html><body>" + oldAminoAcid + "<br>" + df.format(AminoAcid.getAminoAcid(oldAminoAcid).getMonoisotopicMass()) + "</body></html>";
                case 2:
                    return "<html><body>" + name + "<br>" + df.format(normalModificationsMap.get(name)) + "</body></html>";
                case 3:
                    return "<html><body>" + variantAminoAcid + "<br>" + df.format(AminoAcid.getAminoAcid(variantAminoAcid).getMonoisotopicMass()) + "</body></html>";
                case 4:
                    return "<html><body>" + df.format(normalModificationsMap.get(name) - massDifference) + "</body></html>";
                default:
                    return null;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}

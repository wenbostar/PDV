package PDVGUI.gui.utils;

import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import PDVGUI.gui.SinglePeptideDisplay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;

/**
 * Modification dialog in single peptide part
 * Created by Ken on 7/6/2017.
 */
public class ModificationDialog extends JDialog {

    private JComboBox aAModificationComBox;
    private String[] singleModificationsArray;
    private String selectedModification;
    private SinglePeptideDisplay singlePeptideDisplay;
    private CheckPeptideJDialog checkPeptideJDialog;
    private JButton aAJButton;

    /**
     * Constructor.
     * @param singlePeptideDisplay Parent class
     * @param aAJButton AA button
     * @param singleModificationsArray Modification array
     */
    public ModificationDialog(SinglePeptideDisplay singlePeptideDisplay, JButton aAJButton, String[] singleModificationsArray){
        super(singlePeptideDisplay, true);
        this.singleModificationsArray = singleModificationsArray;
        this.singlePeptideDisplay = singlePeptideDisplay;
        this.aAJButton = aAJButton;
        this.setLocationRelativeTo(aAJButton);
        initComponents();
        this.aAModificationComBox.setRenderer(new AlignedListCellRenderer(0));
        this.aAModificationComBox.setSelectedItem(null);
        this.setVisible(true);
    }

    /**
     * Constructor.
     * @param checkPeptideJDialog Parent class
     * @param aAJButton AA button
     * @param singleModificationsArray Modification array
     */
    public ModificationDialog(CheckPeptideJDialog checkPeptideJDialog, JButton aAJButton, String[] singleModificationsArray){
        super(checkPeptideJDialog, true);
        this.singleModificationsArray = singleModificationsArray;
        this.checkPeptideJDialog = checkPeptideJDialog;
        this.aAJButton = aAJButton;
        this.setLocationRelativeTo(aAJButton);
        initComponents();
        this.aAModificationComBox.setRenderer(new AlignedListCellRenderer(0));
        this.aAModificationComBox.setSelectedItem(null);
        this.setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel backgroundJPanel = new JPanel();
        aAModificationComBox = new JComboBox();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Modification of "+ aAJButton.getText());
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(Color.white);

        aAModificationComBox.setModel(new DefaultComboBoxModel(singleModificationsArray));
        aAModificationComBox.addItemListener(this::aAModificationComBoxdMouseClicked);

        backgroundJPanel.add(aAModificationComBox);

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
     * Modification selections
     * @param event Item event
     */
    private void aAModificationComBoxdMouseClicked(ItemEvent event) {
        selectedModification = String.valueOf(aAModificationComBox.getSelectedItem());
        formWindowClosing(null);
    }

    /**
     * Closes the dialog.
     * @param evt Windows event
     */
    private void formWindowClosing(WindowEvent evt) {

        if(singlePeptideDisplay != null){
            singlePeptideDisplay.setSingleModification(selectedModification, aAJButton);
        } else if (checkPeptideJDialog != null){
            checkPeptideJDialog.setSingleModification(selectedModification, aAJButton);
        }
        this.dispose();
    }
}

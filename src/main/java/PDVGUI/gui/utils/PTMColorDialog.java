package PDVGUI.gui.utils;

import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 * PTM color dialog
 * Created by Ken on 12/7/2017.
 */
public class PTMColorDialog extends JDialog {
    
    private JTable colorJTable;

    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory;
    /**
     * All modifications
     */
    private ArrayList<String> allModification;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param ptmFactory PTM factory
     * @param allModification All modifications
     */
    public PTMColorDialog(PDVMainClass pdvMainClass, PTMFactory ptmFactory, ArrayList<String> allModification) {
        super(pdvMainClass, true);

        this.pdvMainClass = pdvMainClass;
        this.ptmFactory = ptmFactory;
        this.allModification = allModification;

        initComponents();

        colorJTable.setModel(new PTMColorTableModel());
        colorJTable.getColumn("Color").setMinWidth(30);
        colorJTable.getColumn("Color").setMaxWidth(50);
        colorJTable.getColumn("Color").setCellRenderer(new JSparklinesColorTableCellRenderer());
        setLocationRelativeTo(pdvMainClass.settingColorJButton);
        setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        colorJTable = new JTable();
        JPanel mainJPanel = new JPanel();
        JScrollPane colorJScrollPane = new JScrollPane();
        JPanel colorJPanel = new JPanel();
        JButton okJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        mainJPanel.setBackground(new java.awt.Color(250, 250, 250));

        okJButton.setText("OK");
        okJButton.addActionListener(this::okJButtonActionPerformed);

        colorJPanel.setOpaque(false);

        colorJScrollPane.setOpaque(false);

        colorJTable.setRowHeight(20);
        colorJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        colorJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));
        colorJTable.setOpaque(false);
        colorJTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        colorJTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                colorJTableMouseReleased(evt);
            }

            public void mouseExited(MouseEvent evt) {
                colorJTableMouseExited(evt);
            }

            public void mouseEntered(MouseEvent evt) {
                colorJTableMouseEntered(evt);
            }
        });

        colorJScrollPane.setViewportView(colorJTable);

        GroupLayout colorJPanelLayout = new GroupLayout(colorJPanel);
        colorJPanel.setLayout(colorJPanelLayout);
        colorJPanelLayout.setHorizontalGroup(
                colorJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(colorJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(colorJScrollPane, 10, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );
        colorJPanelLayout.setVerticalGroup(
                colorJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(colorJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(colorJScrollPane, 10, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(colorJPanel, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                        .addComponent(okJButton))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(colorJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okJButton)
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * okJButtonActionPerformed
     * @param evt Mouse click event
     */
    private void okJButtonActionPerformed(ActionEvent evt){

        ArrayList<String> modification =  ptmFactory.getPTMs();
        PtmSettings ptmSettings = new PtmSettings();

        for(String fixedModification:modification){
            ptmSettings.addFixedModification(ptmFactory.getPTM(fixedModification));
        }

        for(String variableModification:modification){
            ptmSettings.addVariableModification(ptmFactory.getPTM(variableModification));
        }

        SearchParameters searchParameters = pdvMainClass.searchParameters;
        searchParameters.setPtmSettings(ptmSettings);

        pdvMainClass.setSearchParameters(searchParameters);

        pdvMainClass.updateTable();

        dispose();
    }

    /**
     * colorJTableMouseExited
     * @param evt Mouse move event
     */
    private void colorJTableMouseExited(MouseEvent evt){
        this.setCursor(new java.awt.Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * colorJTableMouseEntered
     * @param evt Mouse move event
     */
    private void colorJTableMouseEntered(MouseEvent evt){
        int column = colorJTable.columnAtPoint(evt.getPoint());

        if (column == 1){
            this.setCursor(new java.awt.Cursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * colorJTableMouseReleased
     * @param evt Mouse click event
     */
    private void colorJTableMouseReleased(MouseEvent evt){
        int row = colorJTable.rowAtPoint(evt.getPoint());
        int column = colorJTable.columnAtPoint(evt.getPoint());

        if (row != -1 && column == 1 ){
            Color newColor = JColorChooser.showDialog(this, "Pick a Color", (Color) colorJTable.getValueAt(row, column));

            if (newColor != null) {
                ptmFactory.setColor((String) colorJTable.getValueAt(row, 0), newColor);
                ptmFactory.setColor("<"+ colorJTable.getValueAt(row, 0) +">", newColor);
                colorJTable.setValueAt(newColor, row, 1);
                ((DefaultTableModel) colorJTable.getModel()).fireTableDataChanged();
                colorJTable.repaint();
            }
        }
    }

    /**
     * PTM color table model
     */
    private class PTMColorTableModel extends DefaultTableModel{

        /**
         * Constructor
         */
        public PTMColorTableModel(){
            System.out.println(allModification);
        }

        @Override
        public int getRowCount() {
            if(allModification == null){
                return 0;
            }
            return allModification.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Modification";
                case 1:
                    return "Color";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String name = allModification.get(row);

            switch (column) {
                case 0:
                    return name;
                case 1:
                    return ptmFactory.getColor(name);
                default:
                    return "";
            }

        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 0){
                return String.class;
            } else {
                return Object.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}

package PDVGUI.gui.utils;

import PDVGUI.gui.PDVMainClass;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Column display selection
 * Created by Ken on 9/1/2017.
 */
public class ColumnSelectionDialog extends JDialog{
    
    private JTable columnSelectionJTable;

    /**
     * Column name
     */
    private ArrayList<String> columnName;
    /**
     * PDV table
     */
    private JTable spectrumJTable;
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * Selected column map
     */
    private HashMap<String, Boolean> columnToSelected;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param spectrumJTable PDV table
     * @param columnName Column name
     */
    public ColumnSelectionDialog(PDVMainClass pdvMainClass, JTable spectrumJTable, ArrayList<String> columnName){
        super(pdvMainClass, true);
        this.spectrumJTable = spectrumJTable;
        this.columnName = columnName;
        this.pdvMainClass = pdvMainClass;
        this.columnToSelected = pdvMainClass.columnToSelected;

        initComponents();

        columnSelectionJTable.setModel(new ColumnSelectionTableModel(columnName, columnToSelected));
        columnSelectionJTable.getColumn("Display").setMinWidth(30);
        columnSelectionJTable.getColumn("Display").setMaxWidth(50);
        setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        columnSelectionJTable = new JTable();
        JPanel mainJPanel = new JPanel();
        JScrollPane columnSelectionJScrollPane = new JScrollPane();
        JPanel columnSelectionJPanel = new JPanel();
        JButton closeJButton = new JButton();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        mainJPanel.setBackground(new java.awt.Color(250, 250, 250));

        closeJButton.setText("Close");
        closeJButton.addActionListener(this::closeJButtonActionPerformed);

        columnSelectionJPanel.setOpaque(false);
        
        columnSelectionJScrollPane.setOpaque(false);

        columnSelectionJTable.setModel(new ColumnSelectionTableModel());
        columnSelectionJTable.setRowHeight(40);
        columnSelectionJTable.setOpaque(false);
        columnSelectionJTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        columnSelectionJTable.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent evt) {
                columnSelectionJTableMouseReleased(evt);
            }
        });
        columnSelectionJScrollPane.setViewportView(columnSelectionJTable);

        GroupLayout columnSelectionJPanelLayout = new GroupLayout(columnSelectionJPanel);
        columnSelectionJPanel.setLayout(columnSelectionJPanelLayout);
        columnSelectionJPanelLayout.setHorizontalGroup(
                columnSelectionJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(columnSelectionJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(columnSelectionJScrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );
        columnSelectionJPanelLayout.setVerticalGroup(
                columnSelectionJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(columnSelectionJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(columnSelectionJScrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(columnSelectionJPanel, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(closeJButton))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(columnSelectionJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(closeJButton)
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
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Close the dialog
     * @param evt Mouse click event
     */
    private void closeJButtonActionPerformed(ActionEvent evt) {
        dispose();
    }

    /**
     * ColumnSelectionJTableMouseReleased
     * @param evt Mouse click event
     */
    private void columnSelectionJTableMouseReleased(MouseEvent evt){
        int[] selectedRows = columnSelectionJTable.getSelectedRows();
        int column = columnSelectionJTable.getSelectedColumn();

        if (selectedRows != null) {
            if (column == columnSelectionJTable.getColumn("Display").getModelIndex()) {
                for (int row: selectedRows) {
                    Boolean isSelected = columnToSelected.get(columnName.get(row));
                    if (isSelected) {
                        columnToSelected.put(columnName.get(row), false);
                    } else {
                        columnToSelected.put(columnName.get(row), true);
                    }
                }

            }

            for (String key: columnToSelected.keySet()){
                if(!columnToSelected.get(key)){
                    spectrumJTable.getColumn(key).setMinWidth(0);
                    spectrumJTable.getColumn(key).setMaxWidth(0);
                } else {
                    for (int i = 0; i < 2; i++) {
                        if (key.equals("Other Assumption")) {
                            spectrumJTable.getColumn(key).setPreferredWidth(200);
                            spectrumJTable.getColumn(key).setMinWidth(20);
                            spectrumJTable.getColumn(key).setMaxWidth(400);
                        } else {
                            spectrumJTable.getColumn(key).setPreferredWidth(70);
                            spectrumJTable.getColumn(key).setMinWidth(20);
                            spectrumJTable.getColumn(key).setMaxWidth(400);
                        }
                    }
                }
            }

            pdvMainClass.columnToSelected = columnToSelected;
        }
        columnSelectionJTable.revalidate();
        columnSelectionJTable.repaint();
        spectrumJTable.revalidate();
        spectrumJTable.repaint();
    }

    /**
     *  ColumnSelectionTableModel
     */
    public class ColumnSelectionTableModel extends DefaultTableModel{

        /**
         * Column name
         */
        private ArrayList<String> columnName;
        /**
         * Selected column map
         */
        private HashMap<String, Boolean> columnToSelected;

        /**
         * Empty model
         */
        public ColumnSelectionTableModel(){}

        /**
         * Constructor
         * @param columnName Column name
         * @param columnToSelected Column selected map
         */
        public ColumnSelectionTableModel(ArrayList<String> columnName, HashMap<String, Boolean> columnToSelected){

            this.columnName = columnName;
            this.columnToSelected = columnToSelected;
        }

        @Override
        public int getRowCount() {
            if(columnName == null){
                return 0;
            }
            return columnName.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Display";
                case 1:
                    return "Column";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String name = columnName.get(row);

            switch (column) {
                case 0:
                    return columnToSelected.get(name);
                case 1:
                    return name;
                default:
                    return "";
            }
            
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if(columnIndex == 0){
                return Boolean.class;
            } else {
                return String.class;
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}

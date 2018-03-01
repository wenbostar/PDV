package PDVGUI.gui.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Information details panel
 * Created by Ken on 12/26/2017.
 */
public class InfoPanel extends JPanel {

    /**
     * JPanel containing all details table
     */
    private JPanel tableJPanel;
    /**
     * All details list hap
     */
    private HashMap<String, ArrayList<String>> detailsListMap = new HashMap<>();

    /**
     * Constructor
     */
    public InfoPanel(){

        initComponents();
    }

    /**
     * Update information jpanel
     * @param detailsListMap Details map
     */
    public void updateInfo(HashMap<String, ArrayList<String>> detailsListMap){

        this.detailsListMap = detailsListMap;

        readInfo();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();
        JScrollPane inforJScrollPane = new JScrollPane();

        tableJPanel = new JPanel();

        tableJPanel.setLayout(new BoxLayout(tableJPanel, BoxLayout.Y_AXIS));
        tableJPanel.setOpaque(true);
        tableJPanel.setBackground(Color.white);

        inforJScrollPane.setViewportView(tableJPanel);
        inforJScrollPane.setOpaque(false);
        inforJScrollPane.setBackground(Color.white);

        mainJPanel.setBackground(Color.white);
        mainJPanel.setOpaque(false);

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)

                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }

    /**
     * Putting all information into panel
     */
    private void readInfo(){

        tableJPanel.removeAll();

        for (String fileName : detailsListMap.keySet()){

            JLabel fileJLabel = new JLabel(fileName);
            fileJLabel.setOpaque(false);
            fileJLabel.setFont(new Font("Dialog", Font.BOLD, 15));

            tableJPanel.add(fileJLabel);

            JTable detailsJTable = new JTable(){
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column){
                    Component component = super.prepareRenderer(renderer, row, column);
                    if (row % 2 == 0) {
                        component.setBackground(Color.white);
                    }else{
                        component.setBackground(new Color(200, 203, 207));
                    }
                    if(isRowSelected(row)){
                        component.setBackground(new Color(20,20,40));
                    }
                    return component;
                }
            };
            ((DefaultTableCellRenderer)detailsJTable.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
            detailsJTable.setRowHeight(30);
            detailsJTable.setFont(new Font("Arial", Font.PLAIN, 13));
            detailsJTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 14));
            detailsJTable.setShowHorizontalLines(false);
            detailsJTable.setShowVerticalLines(false);
            detailsJTable.setPreferredScrollableViewportSize(new Dimension(300,300));

            DetailTableModel detailTableModel = new DetailTableModel(detailsListMap.get(fileName));

            detailsJTable.setModel(detailTableModel);

            tableJPanel.add(detailsJTable.getTableHeader(),BorderLayout.NORTH);
            tableJPanel.add(detailsJTable);

            tableJPanel.repaint();
            tableJPanel.revalidate();
        }
    }

    /**
     * Details table model
     */
    private class DetailTableModel extends DefaultTableModel{

        /**
         * Details information list
         */
        private ArrayList<String> detailsList;

        /**
         * Constructor
         * @param detailsList Details list
         */
        public DetailTableModel(ArrayList<String> detailsList){

            this.detailsList = detailsList;
        }

        @Override
        public int getRowCount() {
            if (detailsList != null) {
                return detailsList.size();
            } else {
                return 0;
            }
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column){
                case 0:
                    return "Attribute name";
                case 1:
                    return "Value";
            }
            return "";
        }

        @Override
        public Object getValueAt(int row, int column) {

            String[] eachLine = detailsList.get(row).split("/t/");

            switch (column){
                case 0:
                    return eachLine[0];
                case 1:
                    if (eachLine.length == 1){
                        return "";
                    } else {
                        return eachLine[1];
                    }
            }
            return "";
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }

            return String.class;
        }
    }
}

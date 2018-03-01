package PDVGUI.gui.utils;

import PDVGUI.gui.PDVMainClass;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Identification original information
 * Created by Ken on 8/31/2017.
 */
public class OriginalInforDialog extends JDialog{

    private JEditorPane inforJEditor;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param originalInfor Identification details
     */
    public OriginalInforDialog(PDVMainClass pdvMainClass, HashMap<String, Object> originalInfor){

        super(pdvMainClass, true);

        initComponents();

        setTitle("Identification Details Information");

        setInforJEditor(originalInfor);

        setLocationRelativeTo(pdvMainClass);
        setVisible(true);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        inforJEditor = new JEditorPane();
        JPanel mainJPanel = new JPanel();
        JButton closeJButton = new JButton();
        JPanel inforJPanel = new JPanel();
        JScrollPane inforJScrollPane = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        mainJPanel.setBackground(new java.awt.Color(250, 250, 250));

        closeJButton.setText("Close");
        closeJButton.addActionListener(this::closeJButtonActionPerformed);

        inforJPanel.setOpaque(false);

        inforJEditor.setContentType("text/html");
        inforJEditor.setEditable(false);
        inforJScrollPane.setViewportView(inforJEditor);

        GroupLayout inforJPanelLayout = new GroupLayout(inforJPanel);
        inforJPanel.setLayout(inforJPanelLayout);
        inforJPanelLayout.setHorizontalGroup(
                inforJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inforJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
                                .addContainerGap())
        );
        inforJPanelLayout.setVerticalGroup(
                inforJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(inforJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inforJScrollPane, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);
        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(inforJPanel, GroupLayout.Alignment.LEADING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(closeJButton))
                                .addContainerGap())
        );
        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, mainJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(inforJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
     * Adding details into pane
     * @param originalInfor Identification details
     */
    private void setInforJEditor(HashMap<String, Object> originalInfor){
        StringBuilder allInfor = new StringBuilder("<html><br>");

        for (String inforKey: originalInfor.keySet()){
            allInfor.append("<br><b>"+inforKey+"</b>: <br>");
            if(originalInfor.get(inforKey) instanceof String){
                allInfor.append(originalInfor.get(inforKey)+"<br>");
            } else if(originalInfor.get(inforKey) instanceof ArrayList){
                for(String eachItem: (ArrayList<String>)originalInfor.get(inforKey)){
                    allInfor.append(eachItem+"<br>");
                }
            } else if(originalInfor.get(inforKey) instanceof HashMap){
                allInfor.append("<table border=\"1\">\n");
                for(String eachKey: ((HashMap<String, String>) (originalInfor.get(inforKey))).keySet()){
                    allInfor.append(" <tr>\n" +
                            "        <td>"+ eachKey +"</td>\n" +
                            "        <td>"+ ((HashMap) originalInfor.get(inforKey)).get(eachKey) +"</td>\n" +
                            "    </tr>");
                }
                allInfor.append("</table>");
            }
        }
        allInfor.append("<br></html>");

        inforJEditor.setText(allInfor.toString());
        inforJEditor.setCaretPosition(0);
    }
}

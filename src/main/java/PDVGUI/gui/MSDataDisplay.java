package PDVGUI.gui;

import PDVGUI.gui.utils.FileImport.MSDataImportDialog;
import PDVGUI.gui.utils.InfoPanel;
import PDVGUI.gui.utils.TICPanel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.WindowEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Display MS data
 * Created by Ken on 10/26/2017.
 */
public class MSDataDisplay extends JFrame {

    private JPanel ticShowJPanel;
    private JPanel treeJPanel;
    private JSplitPane allJSplitPane;
    private JSplitPane treeAndDetailJSplit;
    private JButton openSidebarJButton;
    private JComboBox selectShow;

    /**
     * TIC panel
     */
    private TICPanel ticPanel;
    /**
     * File to hash
     */
    private HashMap<String, HashMap<String, ArrayList<float[]>>> fileToHash = new HashMap<>();
    /**
     * File to biggest num
     */
    private HashMap<String, Integer> fileToBiggestNum = new HashMap<>();
    /**
     * Top num
     */
    private Integer topNum = 0;
    /**
     * Selected nodes
     */
    private ArrayList<String> selectedNodeList;
    /**
     * File name to spectrum key to RT and intensity
     */
    private HashMap<String, HashMap<String, ArrayList<float[]>>> nameToKeyToRtAndInt;
    /**
     * Selected file name
     */
    private String selectFileName;
    /**
     * Original information hash
     */
    private HashMap<String, ArrayList<String>> originalInforMap = new HashMap<>();
    /**
     * Information panel
     */
    private InfoPanel infoPanel = new InfoPanel();

    /**
     * Constructor
     * @param pdvStart Start panel
     */
    public MSDataDisplay(PDVStart pdvStart){

        initComponents();

        if (pdvStart != null) {
            pdvStart.setVisible(false);
        }

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")));
        setVisible(true);

        new MSDataImportDialog(this);
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        JPanel mainJPanel = new JPanel();
        JPanel backJPanel = new JPanel();
        JPanel sideJPanel = new JPanel();
        JPanel detailsJPanel = new JPanel();
        JScrollPane inforJScrollPane = new JScrollPane();
        JMenuBar menuBar = new JMenuBar();
        JMenuItem exitJMenuItem = new JMenuItem();
        JMenu fileJMenu = new JMenu();
        JButton addNewJButton = new JButton();
        JEditorPane inforJEditor = new JEditorPane();

        allJSplitPane = new JSplitPane();
        ticShowJPanel = new JPanel();
        treeAndDetailJSplit = new JSplitPane();
        treeJPanel = new JPanel();
        openSidebarJButton = new JButton();
        selectShow = new JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("PDV - MS file(s) Display");
        setBackground(new java.awt.Color(255, 255, 255));
        setMinimumSize(new java.awt.Dimension(760, 600));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        exitJMenuItem.setMnemonic('E');
        exitJMenuItem.setText("Exit");
        exitJMenuItem.addActionListener(this::exitJMenuItemActionPerformed);

        fileJMenu.add(exitJMenuItem);

        fileJMenu.setMnemonic('F');
        fileJMenu.setText("File");

        menuBar.add(fileJMenu);

        mainJPanel.setBackground(new java.awt.Color(255, 255, 255));
        mainJPanel.setOpaque(false);
        mainJPanel.setPreferredSize(new java.awt.Dimension(1260, 800));

        sideJPanel.setBackground(new Color(219, 222, 226));
        sideJPanel.setMinimumSize(new Dimension(20, 0));
        sideJPanel.setOpaque(false);

        sideJPanel.setLayout(new BoxLayout(sideJPanel, BoxLayout.Y_AXIS));

        openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/fold.png")));
        openSidebarJButton.setToolTipText("Fold the side tree.");
        openSidebarJButton.setBorder(null);
        openSidebarJButton.setBorderPainted(false);
        openSidebarJButton.setContentAreaFilled(false);
        openSidebarJButton.setSelected(true);
        openSidebarJButton.addActionListener(this::openSidebarJButtonActionPerform);

        addNewJButton.setIcon(new ImageIcon(getClass().getResource("/icons/add.png")));
        addNewJButton.setBorder(null);
        addNewJButton.setBorderPainted(false);
        addNewJButton.setContentAreaFilled(false);
        addNewJButton.setToolTipText("Add more pride XML files.");
        addNewJButton.addActionListener(this::addNewJButtonActionPerform);

        sideJPanel.add(openSidebarJButton);
        sideJPanel.add(addNewJButton);

        treeAndDetailJSplit.setBorder(null);
        treeAndDetailJSplit.setDividerLocation(0.4);
        treeAndDetailJSplit.setDividerSize(5);
        treeAndDetailJSplit.setOrientation(JSplitPane.VERTICAL_SPLIT);
        treeAndDetailJSplit.setResizeWeight(0.5);
        treeAndDetailJSplit.setOpaque(false);
        treeAndDetailJSplit.setVisible(true);
        treeAndDetailJSplit.setContinuousLayout(true);

        allJSplitPane.setBorder(null);
        allJSplitPane.setDividerLocation(0.3);
        allJSplitPane.setDividerSize(5);
        allJSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        allJSplitPane.setResizeWeight(0.5);
        allJSplitPane.setOpaque(false);
        allJSplitPane.setContinuousLayout(true);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Spectrum file(s)" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        treeJPanel.setBorder(titledBorder);
        treeJPanel.setBackground(Color.WHITE);
        treeJPanel.setOpaque(false);

        treeAndDetailJSplit.setTopComponent(treeJPanel);

        detailsJPanel.setOpaque(false);
        detailsJPanel.setBackground(Color.WHITE);

        titledBorder = BorderFactory.createTitledBorder("Details" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        detailsJPanel.setBorder(titledBorder);

        inforJEditor.setContentType("text/html");
        inforJEditor.setEditable(false);
        inforJScrollPane.setViewportView(inforJEditor);
        infoPanel.setOpaque(false);

        GroupLayout detailsJPanelLayout = new GroupLayout(detailsJPanel);
        detailsJPanel.setLayout(detailsJPanelLayout);
        detailsJPanelLayout.setHorizontalGroup(
                detailsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(infoPanel, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );
        detailsJPanelLayout.setVerticalGroup(
                detailsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(detailsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(infoPanel, GroupLayout.DEFAULT_SIZE, 490, Short.MAX_VALUE)
                                .addContainerGap())
        );

        treeAndDetailJSplit.setRightComponent(detailsJPanel);

        allJSplitPane.setTopComponent(treeAndDetailJSplit);

        BorderFactory.createTitledBorder("Show" + " \t ");
        titledBorder.setTitleFont(new Font("Console", Font.PLAIN, 12));
        ticShowJPanel.setBorder(titledBorder);
        ticShowJPanel.setBackground(Color.WHITE);
        ticShowJPanel.setOpaque(false);

        GroupLayout ticShowJPanelLayout = new GroupLayout(ticShowJPanel);
        ticShowJPanel.setLayout(ticShowJPanelLayout);

        ticPanel = new TICPanel(this);
        ticPanel.setBackground(Color.WHITE);
        ticPanel.setOpaque(false);

        selectShow.setModel(new DefaultComboBoxModel(new String[]{"All", "TIC", "BP"}));
        selectShow.addItemListener(this::selectShowListenser);

        ticShowJPanelLayout.setHorizontalGroup(
                ticShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ticShowJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(ticPanel, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                                .addContainerGap())
        );

        ticShowJPanelLayout.setVerticalGroup(
                ticShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(ticShowJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(ticShowJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(ticPanel, GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE)
                                .addContainerGap()))
        );

        allJSplitPane.setRightComponent(ticShowJPanel);

        backJPanel.setBackground(Color.WHITE);

        GroupLayout backJPanelLayout = new GroupLayout(backJPanel);
        backJPanel.setLayout(backJPanelLayout);
        backJPanelLayout.setHorizontalGroup(
                backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, 20, 20)
                                .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        backJPanelLayout.setVerticalGroup(
                backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(sideJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(allJSplitPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );

        GroupLayout mainJPanelLayout = new GroupLayout(mainJPanel);
        mainJPanel.setLayout(mainJPanelLayout);

        mainJPanelLayout.setHorizontalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backJPanel)
        );

        mainJPanelLayout.setVerticalGroup(
                mainJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(mainJPanelLayout.createSequentialGroup()
                                .addComponent(backJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        setJMenuBar(menuBar);

        GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(mainJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        String motif;
        String os = System.getProperty("os.name");
        if(os.toLowerCase().startsWith("win")){
            motif="com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        } else if (os.toLowerCase().startsWith("mac")){
            motif="com.sun.java.swing.plaf.mac.MacLookAndFeel";
        } else {
            motif=UIManager.getSystemLookAndFeelClassName();
        }

        try {
            UIManager.setLookAndFeel(motif);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        SwingUtilities.updateComponentTreeUI(this);

        pack();
    }

    /**
     * Close the program when click exist
     *
     * @param evt Window event
     */
    private void formWindowClosing(WindowEvent evt) {
        close();
    }

    /**
     * Close dialog
     */
    public void close() {
        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        int value = JOptionPane.showConfirmDialog(this,
                "Do you want to give up " + " and close PDV" + "?",
                "Close PDV",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (value == JOptionPane.YES_OPTION) {
            closePDV();
        } else if (value == JOptionPane.NO_OPTION) {
        }
    }

    /**
     * Close the program
     */
    private void closePDV() {

        System.exit(0);
    }

    /**
     * exitJMenuItemActionPerformed
     * @param evt Mouse click event
     */
    private void exitJMenuItemActionPerformed(ActionEvent evt){
        close();
    }

    /**
     * Open side panel button action
     * @param event Mouse click event
     */
    private void openSidebarJButtonActionPerform(ActionEvent event){

        if(treeAndDetailJSplit.isVisible()){
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/unfold.png")));

            treeAndDetailJSplit.setVisible(false);

            allJSplitPane.setDividerSize(0);

            allJSplitPane.revalidate();
            allJSplitPane.repaint();

        } else {
            openSidebarJButton.setIcon(new ImageIcon(getClass().getResource("/icons/fold.png")));

            treeAndDetailJSplit.setVisible(true);

            allJSplitPane.setDividerSize(5);

            allJSplitPane.setDividerLocation(0.3);

            allJSplitPane.revalidate();
            allJSplitPane.repaint();
        }
    }

    /**
     * addNewJButtonActionPerform
     * @param evt Mouse click event
     */
    private void addNewJButtonActionPerform(ActionEvent evt){
        new MSDataImportDialog(this);
    }

    /**
     * selectShowListenser
     * @param evt Item select event
     */
    private void selectShowListenser(ItemEvent evt) {

        if (selectedNodeList.size() == 1) {
            updateTic();
        } else {
            updateTic(nameToKeyToRtAndInt);
        }
    }

    /**
     * Update tree
     * @param spectrumFileName Spectrum file name
     * @param keyToRtAndInt Spectrum key to RT and intensity
     * @param originalInfor Original information
     */
    public synchronized void updateTree(String spectrumFileName, HashMap<String, ArrayList<float[]>> keyToRtAndInt, ArrayList<String> originalInfor, BigInteger biggestNum){

        allJSplitPane.setDividerLocation(0.3);

        if (!fileToHash.containsKey(spectrumFileName)){

            treeJPanel.removeAll();

            int exp = 1;
            int length = String.valueOf(biggestNum).length() - 3;

            fileToHash.put(spectrumFileName, keyToRtAndInt);

            for (int i = 0; i < length; i++){
                exp *= 10;
            }

            fileToBiggestNum.put(spectrumFileName, exp);

            originalInforMap.put(spectrumFileName, originalInfor);

            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Files");

            DefaultTreeModel treeModel = new DefaultTreeModel(root);

            for (String fileName : fileToHash.keySet()){

                DefaultMutableTreeNode prideFileRoot = new DefaultMutableTreeNode(fileName + "( "+fileToHash.get(fileName).size()+" )");
                treeModel.insertNodeInto(prideFileRoot, root, root.getChildCount());
            }

            JScrollPane jScrollPane;

            JTree tree = new JTree(treeModel);
            tree.setOpaque(true);
            tree.setFont(new Font("Arial", Font.PLAIN, 12));
            tree.setBackground(Color.white);
            tree.getSelectionModel().setSelectionMode(
                    TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

            tree.addTreeSelectionListener(this::valueChanged);

            tree.setSelectionInterval(1,1);

            jScrollPane = new JScrollPane(tree);
            jScrollPane.setOpaque(false);
            jScrollPane.setBackground(Color.white);

            GroupLayout treeJPanelLayout = new GroupLayout(treeJPanel);
            treeJPanel.setLayout(treeJPanelLayout);

            treeJPanelLayout.setHorizontalGroup(
                    treeJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
            );

            treeJPanelLayout.setVerticalGroup(
                    treeJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane, GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
            );

            treeJPanel.revalidate();
            treeJPanel.repaint();
        }
    }

    /**
     * Tree value changed
     * @param event TreeSelectionEvent
     */
    private void valueChanged(TreeSelectionEvent event) {
        JTree tree = (JTree)event.getSource();

        HashMap<String, ArrayList<String>> selectedOri = new HashMap<>();
        selectedNodeList = new ArrayList<>();

        nameToKeyToRtAndInt = new HashMap<>();

        if (tree.getSelectionPaths() != null){
            for (TreePath treePath : tree.getSelectionPaths()){

                selectedNodeList.add(treePath.getLastPathComponent().toString().split("\\(")[0]);
            }
        } else {
            tree.removeSelectionInterval(tree.getLeadSelectionRow(), tree.getLeadSelectionRow());
            valueChanged(event);
        }

        if (selectedNodeList.size() == 1){

            String nodeName = selectedNodeList.get(0);

            if (fileToHash.containsKey(nodeName)) {

                topNum = fileToBiggestNum.get(nodeName);

                selectedOri.put(nodeName, originalInforMap.get(nodeName));

                updateDetails(selectedOri);

                selectFileName = nodeName;

                updateTic();
            }
        } else {

            int numHere = 0;

            for (String nodeName : selectedNodeList) {

                if (fileToHash.containsKey(nodeName)){

                    selectedOri.put(nodeName, originalInforMap.get(nodeName));

                    nameToKeyToRtAndInt.put(nodeName, fileToHash.get(nodeName));
                }

                if (fileToBiggestNum.get(nodeName) > numHere){
                    numHere = fileToBiggestNum.get(nodeName);
                }
            }

            topNum = numHere;

            updateDetails(selectedOri);

            updateTic(nameToKeyToRtAndInt);
        }
    }

    /**
     * Update TIC
     */
    private void updateTic(){
        ticPanel.updatePanel(selectFileName, fileToHash.get(selectFileName), selectShow.getSelectedIndex(), topNum);

        ticShowJPanel.revalidate();
        ticShowJPanel.repaint();
    }

    /**
     * Update TIC
     * @param nameToKeyToRtAndInt File name to spectrum key to RT and intensity
     */
    private void updateTic(HashMap<String, HashMap<String, ArrayList<float[]>>> nameToKeyToRtAndInt){
        ticPanel.updatePanel(nameToKeyToRtAndInt, selectShow.getSelectedIndex(), topNum);

        ticShowJPanel.revalidate();
        ticShowJPanel.repaint();
    }

    /**
     * Update file details
     * @param selectedOri Hash map
     */
    private void updateDetails(HashMap<String, ArrayList<String>> selectedOri) {

        infoPanel.updateInfo(selectedOri);
    }
}

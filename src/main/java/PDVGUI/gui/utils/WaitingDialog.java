package PDVGUI.gui.utils;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.util.Date;

/**
 * Waiting dialog
 * Created by Ken on 12/16/2016.
 */
public class WaitingDialog extends javax.swing.JDialog {

    private JTextArea processJTextArea;
    private JProgressBar progressBar;
    private JButton okJButton;

    /**
     * The line break type.
     */
    private String lineBreak = System.getProperty("line.separator");
    /**
     * Boolean indicating whether the process is finished.
     */
    private boolean runFinished = false;
    /**
     * Boolean indicating whether the process is canceled.
     */
    private boolean runCanceled = false;
    /**
     * The tab space to add when not using HTML.
     */
    private static final String TAB_NON_HTML = "        ";

    public WaitingDialog(JDialog parent){
        super(parent,true);
        initComponents();
        ((DefaultCaret) this.processJTextArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        this.setLocationRelativeTo(parent);
    }

    private void initComponents(){
        JPanel backgroundJPanel = new JPanel();
        JPanel processJPanel = new JPanel();
        JScrollPane processJScrollPane = new JScrollPane();
        JProgressBar mzDBBar = new JProgressBar();
        progressBar = new JProgressBar();
        okJButton = new JButton();
        processJTextArea = new JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Importing Data - Please Wait...");
        setMinimumSize(new java.awt.Dimension(700, 500));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundJPanel.setBackground(new java.awt.Color(255, 255, 255));

        processJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Progress"));
        processJPanel.setOpaque(false);

        progressBar.setToolTipText("Total Progress");
        progressBar.setStringPainted(true);

        mzDBBar.setToolTipText("MzDB progress");
        mzDBBar.setStringPainted(true);

        processJTextArea.setEditable(false);
        processJTextArea.setColumns(20);
        processJTextArea.setLineWrap(true);
        processJTextArea.setRows(5);
        processJTextArea.setWrapStyleWord(true);
        processJScrollPane.setViewportView(processJTextArea);

        GroupLayout processJPanelLayout = new GroupLayout(processJPanel);
        processJPanel.setLayout(processJPanelLayout);

        processJPanelLayout.setHorizontalGroup(
                processJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(processJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(processJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(processJScrollPane))
                                .addContainerGap())
        );

        processJPanelLayout.setVerticalGroup(
                processJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(processJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(processJScrollPane)
                                .addContainerGap())
        );

        okJButton.setText("Cancel");
        okJButton.setFont(new Font("Dialog", Font.BOLD, 13));
        okJButton.setBackground(Color.white);
        okJButton.setIcon(new ImageIcon(getClass().getResource("/icons/loading.gif")));
        okJButton.setIconTextGap(3);
        okJButton.addActionListener(this::okJButtonActionPerformed);

        GroupLayout backgroundJPanelLayout = new GroupLayout(backgroundJPanel);
        backgroundJPanel.setLayout(backgroundJPanelLayout);

        backgroundJPanelLayout.setHorizontalGroup(
                backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundJPanelLayout.createSequentialGroup()
                                .addGap(10,10,10)
                                .addGroup(backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(processJPanel)
                                        .addGroup(backgroundJPanelLayout.createSequentialGroup()
                                                .addGap(100,600, 1000)
                                                .addComponent(okJButton,GroupLayout.DEFAULT_SIZE, 100, 100)
                                        ))
                                .addGap(10,10,10)
                        )
        );

        backgroundJPanelLayout.setVerticalGroup(
                backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundJPanelLayout.createSequentialGroup()
                                .addGap(10,10,10)
                                .addComponent(processJPanel)
                                .addGroup(backgroundJPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(okJButton))
                                .addGap(10,10,10))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Append reports
     * @param report Reports
     * @param includeDate Include data or not
     * @param addNewLine Add new line or not
     */
    public synchronized void appendReport(String report, boolean includeDate, boolean addNewLine) {

        if (includeDate) {
            Date date = new Date();
            if (addNewLine) {
                processJTextArea.append(date + TAB_NON_HTML + report + lineBreak);
            } else {
                processJTextArea.append(date + TAB_NON_HTML + report);
            }
        } else {
            if (addNewLine) {
                processJTextArea.append(report + lineBreak);
            } else {
                processJTextArea.append(report);
            }
        }
    }

    /**
     * Append a new line to the report.
     */
    public synchronized void appendReportEndLine() {

        processJTextArea.append(lineBreak);
    }

    /**
     * Set dialog finished
      */
    public void setRunFinished() {
        runFinished = true;
        okJButton.setText("ok");
        okJButton.setFont(new Font("Dialog", Font.BOLD, 13));
        okJButton.setBackground(Color.white);
        okJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        okJButton.setIconTextGap(3);
        this.setTitle( "Project - Completed!");
    }

    /**
     * Set dialog canceled
     */
    public void setRunCanceled() {
        if (!runCanceled) {
            runCanceled = true;

            appendReportEndLine();
            appendReport( "Progress Canceled!", true, true);
            okJButton.setText("Ok");
            okJButton.setFont(new Font("Dialog", Font.BOLD, 13));
            okJButton.setBackground(Color.white);
            okJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
            okJButton.setIconTextGap(3);
            okJButton.setEnabled(true);
        }
        progressBar.setIndeterminate(false);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        this.setTitle("Progress - Canceled");
    }

    /**
     * Return is canceled or not
     * @return Boolean
     */
    public boolean isRunCanceled() {
        return runCanceled;
    }

    /**
     * Increase the progress bar value by one "counter".
     */
    public synchronized void increasePrimaryProgressCounter() {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    /**
     * Close the dialog.
     * @param evt Windows click event
     */
    private void formWindowClosing(WindowEvent evt) {
        okJButtonActionPerformed(null);
    }

    /**
     * Cancels the process
     * @param evt Mouse click event
     */
    private void okJButtonActionPerformed(ActionEvent evt) {
        if (runFinished || runCanceled) {
            this.dispose();
        } else {
            setRunCanceled();
        }
    }
}


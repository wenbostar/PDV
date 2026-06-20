package PDVGUI.gui.utils;

import com.compomics.util.experiment.biology.Ion;
import com.compomics.util.experiment.biology.IonFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationSettings;
import com.compomics.util.gui.spectrum.IonLabelColorTableModel;
import com.compomics.util.gui.spectrum.SpectrumPanel;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static com.compomics.util.experiment.biology.Ion.IonType.IMMONIUM_ION;

/**
 * Annotation setting dialog
 * Created by Ken on 9/15/2017.
 */
public class AnnotationSettingsDialog extends JDialog {

    private JPanel annotatedPeakColorJPanel;
    private JSpinner annotatedPeakWidthJSpinner;
    private JPanel backgroundPeakColorJPanel;
    private JSpinner backgroundPeakWidthJSpinner;
    private JSpinner limitJSpinner;
    private JSpinner sequenceFontSizeJSpinner;
    private JCheckBox showConfidenceTrackCheckBox;
    private JCheckBox showTrackResiduesCheckBox;
    private JScrollPane colorsScrollPane;
    private JTable colorsJTable;

    /**
     * Ion types map
     */
    private HashMap<Ion.IonType, HashSet<Integer>> ionTypeMap;
    /**
     * Neutral losses list
     */
    private ArrayList<NeutralLoss> neutralLossList;
    /**
     * Spectrum Main panel
     */
    private SpectrumMainPanel spectrumMainPanel;
    /**
     * UtilitiesUserPreferences
     */
    private UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * Annotation settings
     */
    private AnnotationSettings annotationSettings;

    /**
     * Creates a new AnnotationSettingsDialog
     * @param spectrumMainPanel spectrumMainPanel parent
     */
    public AnnotationSettingsDialog(SpectrumMainPanel spectrumMainPanel, PtmSettings ptmSettings) {
        super(spectrumMainPanel.parentFrame, true);
        this.spectrumMainPanel = spectrumMainPanel;

        annotationSettings = spectrumMainPanel.getAnnotationSettings();
        utilitiesUserPreferences = spectrumMainPanel.utilitiesUserPreferences;
        ionTypeMap = annotationSettings.getIonTypes();
        neutralLossList = IonFactory.getNeutralLosses(ptmSettings);
        initComponents();
        setUpGUI();
        setLocationRelativeTo(spectrumMainPanel.settingsMenu);
        setVisible(true);
    }

    /**
     * Set up GUI
     */
    private void setUpGUI() {

        annotatedPeakColorJPanel.setBackground(utilitiesUserPreferences.getSpectrumAnnotatedPeakColor());
        backgroundPeakColorJPanel.setBackground(utilitiesUserPreferences.getSpectrumBackgroundPeakColor());
        annotatedPeakWidthJSpinner.setValue(utilitiesUserPreferences.getSpectrumAnnotatedPeakWidth());
        backgroundPeakWidthJSpinner.setValue(utilitiesUserPreferences.getSpectrumBackgroundPeakWidth());

        limitJSpinner.setValue(annotationSettings.getAnnotationIntensityFilter());

        sequenceFontSizeJSpinner.setValue(spectrumMainPanel.getSequenceFontSize());
        showConfidenceTrackCheckBox.setSelected(spectrumMainPanel.isShowConfidenceTrack());
        showTrackResiduesCheckBox.setSelected(spectrumMainPanel.isShowConfidenceResidues());
        showTrackResiduesCheckBox.setEnabled(spectrumMainPanel.isShowConfidenceTrack());

        colorsScrollPane.getViewport().setOpaque(false);

        colorsJTable.getColumn(" ").setMaxWidth(40);
        colorsJTable.getColumn(" ").setMinWidth(40);
        colorsJTable.getColumn("  ").setMaxWidth(40);
        colorsJTable.getColumn("  ").setMinWidth(40);

        colorsJTable.getTableHeader().setReorderingAllowed(false);
        colorsJTable.getColumn("  ").setCellRenderer(new JSparklinesColorTableCellRenderer());
    }

    /**
     * Init all GUI components
     */
    private void initComponents() {

        colorsScrollPane = new JScrollPane();
        colorsJTable = new JTable();
        annotatedPeakColorJPanel = new JPanel();
        backgroundPeakColorJPanel = new JPanel();
        annotatedPeakWidthJSpinner = new JSpinner();
        backgroundPeakWidthJSpinner = new JSpinner();
        limitJSpinner = new JSpinner();
        sequenceFontSizeJSpinner = new JSpinner();
        showConfidenceTrackCheckBox = new JCheckBox();
        showTrackResiduesCheckBox = new JCheckBox();
        JPanel peakSettingsJPanel = new JPanel();
        JLabel annotatedPeakJLabel = new JLabel();
        JLabel backgroundPeakJLabel = new JLabel();
        JLabel annotatedWidthLabel = new JLabel();
        JLabel backgroundWidthLabel = new JLabel();
        JPanel annotationLevelJPanel = new JPanel();
        JLabel limitJLabel = new JLabel();
        JPanel fontSettingsJPanel = new JPanel();
        JLabel sequenceFontLabel = new JLabel();
        JPanel backgroundPanel = new JPanel();
        JButton okButton = new JButton();
        JPanel annotationColorsPanel = new JPanel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("PDV - Annotation settings");
        setMinimumSize(new Dimension(520, 500));

        backgroundPanel.setBackground(new Color(255, 255, 255));

        okButton.setText("OK");
        okButton.setFont(PDVFonts.of(Font.BOLD, 13f));
        okButton.addActionListener(this::okButtonActionPerformed);

        TitledBorder titledBorder = BorderFactory.createTitledBorder("Annotation Colors" + " \t ");
        titledBorder.setTitleFont(PDVFonts.of(Font.PLAIN, 12f));
        annotationColorsPanel.setBorder(titledBorder);
        annotationColorsPanel.setOpaque(false);

        colorsScrollPane.setOpaque(false);

        colorsJTable.setModel(new IonLabelColorTableModel(ionTypeMap, neutralLossList));
        colorsJTable.setOpaque(false);
        colorsJTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(MouseEvent evt) {
                colorsTableMouseExited(evt);
            }
            public void mouseReleased(MouseEvent evt) {
                colorsTableMouseReleased(evt);
            }
        });
        colorsJTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(MouseEvent evt) {
                colorsTableMouseMoved(evt);
            }
        });
        colorsScrollPane.setViewportView(colorsJTable);

        GroupLayout annotationColorsJPanelLayout = new GroupLayout(annotationColorsPanel);
        annotationColorsPanel.setLayout(annotationColorsJPanelLayout);
        annotationColorsJPanelLayout.setHorizontalGroup(
                annotationColorsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationColorsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(colorsScrollPane, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addContainerGap())
        );
        annotationColorsJPanelLayout.setVerticalGroup(
                annotationColorsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationColorsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(colorsScrollPane, GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                                .addContainerGap())
        );

        titledBorder = BorderFactory.createTitledBorder("Peak Settings" + " \t ");
        titledBorder.setTitleFont(PDVFonts.of(Font.PLAIN, 12f));
        peakSettingsJPanel.setBorder(titledBorder);
        peakSettingsJPanel.setOpaque(false);

        annotatedPeakJLabel.setHorizontalAlignment(SwingConstants.LEFT);
        annotatedPeakJLabel.setText("Annotated Peak Color");
        annotatedPeakJLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));

        backgroundPeakJLabel.setText("Background Peak Color");
        backgroundPeakJLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));

        annotatedPeakColorJPanel.setBorder(BorderFactory.createEtchedBorder());
        annotatedPeakColorJPanel.setForeground(new Color(255, 255, 255));
        annotatedPeakColorJPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                annotatedPeakColorJPanelMouseClicked(evt);
            }
            public void mouseEntered(MouseEvent evt) {
                annotatedPeakColorJPanelMouseEntered(evt);
            }
            public void mouseExited(MouseEvent evt) {
                annotatedPeakColorJPanelMouseExited(evt);
            }
        });

        GroupLayout annotatedPeakColorJPanelLayout = new GroupLayout(annotatedPeakColorJPanel);
        annotatedPeakColorJPanel.setLayout(annotatedPeakColorJPanelLayout);
        annotatedPeakColorJPanelLayout.setHorizontalGroup(
                annotatedPeakColorJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 27, Short.MAX_VALUE)
        );
        annotatedPeakColorJPanelLayout.setVerticalGroup(
                annotatedPeakColorJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 20, Short.MAX_VALUE)
        );

        backgroundPeakColorJPanel.setBorder(BorderFactory.createEtchedBorder());
        backgroundPeakColorJPanel.setForeground(new Color(255, 255, 255));
        backgroundPeakColorJPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                backgroundPeakColorPanelMouseClicked(evt);
            }
            public void mouseEntered(MouseEvent evt) {
                backgroundPeakColorPanelMouseEntered(evt);
            }
            public void mouseExited(MouseEvent evt) {
                backgroundPeakColorPanelMouseExited(evt);
            }
        });

        GroupLayout backgroundPeakColorPanelLayout = new GroupLayout(backgroundPeakColorJPanel);
        backgroundPeakColorJPanel.setLayout(backgroundPeakColorPanelLayout);
        backgroundPeakColorPanelLayout.setHorizontalGroup(
                backgroundPeakColorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 27, Short.MAX_VALUE)
        );
        backgroundPeakColorPanelLayout.setVerticalGroup(
                backgroundPeakColorPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGap(0, 20, Short.MAX_VALUE)
        );

        annotatedWidthLabel.setText("Annotated Peak Width");
        annotatedWidthLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));

        backgroundWidthLabel.setText("Background Peak Width");
        backgroundWidthLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));

        annotatedPeakWidthJSpinner.setModel(new SpinnerNumberModel(1.0f, 1.0f, null, 1.0f));
        annotatedPeakWidthJSpinner.addChangeListener(this::annotatedPeakWidthValueChanged);

        backgroundPeakWidthJSpinner.setModel(new SpinnerNumberModel(1.0f, 1.0f, null, 1.0f));
        backgroundPeakWidthJSpinner.addChangeListener(this::backgroundPeakWidthValueChanged);

        GroupLayout peakSettingsJPanelLayout = new GroupLayout(peakSettingsJPanel);
        peakSettingsJPanel.setLayout(peakSettingsJPanelLayout);
        peakSettingsJPanelLayout.setHorizontalGroup(
                peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(peakSettingsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                                        .addGroup(peakSettingsJPanelLayout.createSequentialGroup()
                                                .addComponent(backgroundPeakJLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(backgroundPeakColorJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(GroupLayout.Alignment.LEADING, peakSettingsJPanelLayout.createSequentialGroup()
                                                .addComponent(annotatedPeakJLabel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(annotatedPeakColorJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addGap(50, 50, 50)
                                .addGroup(peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(peakSettingsJPanelLayout.createSequentialGroup()
                                                .addComponent(annotatedWidthLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(annotatedPeakWidthJSpinner, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(peakSettingsJPanelLayout.createSequentialGroup()
                                                .addComponent(backgroundWidthLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(backgroundPeakWidthJSpinner, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );

        peakSettingsJPanelLayout.linkSize(SwingConstants.HORIZONTAL, new Component[] {annotatedPeakJLabel, annotatedWidthLabel, backgroundPeakJLabel, backgroundWidthLabel});

        peakSettingsJPanelLayout.setVerticalGroup(
                peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(peakSettingsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(annotatedPeakJLabel)
                                        .addComponent(annotatedPeakColorJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(annotatedWidthLabel)
                                        .addComponent(annotatedPeakWidthJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addGroup(peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(backgroundPeakColorJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addGroup(peakSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(backgroundWidthLabel)
                                                .addComponent(backgroundPeakWidthJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(backgroundPeakJLabel))
                                .addContainerGap())
        );

        peakSettingsJPanelLayout.linkSize(SwingConstants.VERTICAL, new Component[] {annotatedPeakJLabel, annotatedPeakColorJPanel, annotatedPeakWidthJSpinner, annotatedWidthLabel, backgroundPeakJLabel, backgroundPeakColorJPanel, backgroundPeakWidthJSpinner, backgroundWidthLabel});

        titledBorder = BorderFactory.createTitledBorder("Annotation Level" + " \t ");
        titledBorder.setTitleFont(PDVFonts.of(Font.PLAIN, 12f));
        annotationLevelJPanel.setBorder(titledBorder);
        annotationLevelJPanel.setOpaque(false);

        limitJLabel.setText("Annotation Limit");
        limitJLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));
        limitJLabel.setToolTipText("Intensity percentile to ignore for annotation. e.g. 0.03 means that the 3% least intense peaks will be ignored.");

        limitJSpinner.setModel(new SpinnerNumberModel(Double.valueOf(0.75f), Double.valueOf(0.0f), Double.valueOf(1.0f), Double.valueOf(0.01f)));
        limitJSpinner.addChangeListener(this::limitSpinnerValueChanged);

        GroupLayout annotationLevelJPanelLayout = new GroupLayout(annotationLevelJPanel);
        annotationLevelJPanel.setLayout(annotationLevelJPanelLayout);

        annotationLevelJPanelLayout.setHorizontalGroup(
                annotationLevelJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationLevelJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(annotationLevelJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addGroup(annotationLevelJPanelLayout.createSequentialGroup()
                                                .addComponent(limitJLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(limitJSpinner, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        annotationLevelJPanelLayout.setVerticalGroup(
                annotationLevelJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(annotationLevelJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(annotationLevelJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(limitJLabel)
                                        .addComponent(limitJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
        );

        titledBorder = BorderFactory.createTitledBorder("Sequence & Confidence Track" + " \t ");
        titledBorder.setTitleFont(PDVFonts.of(Font.PLAIN, 12f));
        fontSettingsJPanel.setBorder(titledBorder);
        fontSettingsJPanel.setOpaque(false);

        sequenceFontLabel.setText("Sequence Font Size");
        sequenceFontLabel.setFont(PDVFonts.of(Font.PLAIN, 11f));
        sequenceFontLabel.setToolTipText("Font size of the peptide sequence strip (the confidence track uses the same size).");
        sequenceFontSizeJSpinner.setModel(new SpinnerNumberModel(16, 14, 30, 1));
        sequenceFontSizeJSpinner.addChangeListener(this::sequenceFontSizeValueChanged);

        showConfidenceTrackCheckBox.setText("Show confidence track");
        showConfidenceTrackCheckBox.setFont(PDVFonts.of(Font.PLAIN, 11f));
        showConfidenceTrackCheckBox.setOpaque(false);
        showConfidenceTrackCheckBox.setToolTipText("Show the per-residue confidence bar track (when scores are available).");
        showConfidenceTrackCheckBox.addActionListener(this::showConfidenceTrackActionPerformed);

        showTrackResiduesCheckBox.setText("Show amino acids in confidence track");
        showTrackResiduesCheckBox.setFont(PDVFonts.of(Font.PLAIN, 11f));
        showTrackResiduesCheckBox.setOpaque(false);
        showTrackResiduesCheckBox.setToolTipText("Show the amino-acid letter under each confidence bar.");
        showTrackResiduesCheckBox.addActionListener(this::showTrackResiduesActionPerformed);

        GroupLayout fontSettingsJPanelLayout = new GroupLayout(fontSettingsJPanel);
        fontSettingsJPanel.setLayout(fontSettingsJPanelLayout);
        fontSettingsJPanelLayout.setHorizontalGroup(
                fontSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fontSettingsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fontSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(fontSettingsJPanelLayout.createSequentialGroup()
                                                .addComponent(sequenceFontLabel)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(sequenceFontSizeJSpinner, GroupLayout.PREFERRED_SIZE, 55, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(showConfidenceTrackCheckBox)
                                        .addComponent(showTrackResiduesCheckBox))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        fontSettingsJPanelLayout.setVerticalGroup(
                fontSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(fontSettingsJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(fontSettingsJPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                        .addComponent(sequenceFontLabel)
                                        .addComponent(sequenceFontSizeJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(showConfidenceTrackCheckBox)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(showTrackResiduesCheckBox)
                                .addContainerGap())
        );

        GroupLayout backgroundPanelLayout = new GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                                .addGap(10, 10, 10)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 65, GroupLayout.PREFERRED_SIZE))
                                        .addComponent(peakSettingsJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(annotationColorsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(annotationLevelJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(fontSettingsJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(annotationColorsPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(peakSettingsJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(annotationLevelJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fontSettingsJPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(okButton))
                                .addContainerGap())
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(backgroundPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }

    /**
     * Saving settings and close dialog
     * @param evt Mouse click event
     */
    private void okButtonActionPerformed(ActionEvent evt) {
        UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
        this.dispose();
    }

    /**
     * Changes cursor to a hand cursor
     * @param evt Mouse move event
     */
    private void colorsTableMouseMoved(MouseEvent evt) {
        int row = colorsJTable.rowAtPoint(evt.getPoint());
        int column = colorsJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {

            if (column == colorsJTable.getColumn("  ").getModelIndex()) {
                this.setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /**
     * Changes cursor back to default cursor
     * @param evt Mouse move event
     */
    private void colorsTableMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Select color
     * @param evt Mouse click event
     */
    private void colorsTableMouseReleased(MouseEvent evt) {
        int row = colorsJTable.rowAtPoint(evt.getPoint());
        int column = colorsJTable.columnAtPoint(evt.getPoint());

        if (row != -1) {
            if (column == colorsJTable.getColumn("  ").getModelIndex()) {
                Color newColor = JColorChooser.showDialog(this, "Select a Color", (Color) colorsJTable.getValueAt(row, column));

                if (newColor != null) {

                    int[] selectedRows = colorsJTable.getSelectedRows();

                    for (int selectedRow : selectedRows) {

                        if (((IonLabelColorTableModel) colorsJTable.getModel()).getIonAtRow(selectedRow).getName() == "rA"){
                            for (int i = 1; i < 21; i ++){
                                SpectrumPanel.setIonColor(Ion.getGenericIon(Ion.IonType.RELATED_ION, i), newColor);
                            }
                        } else if (((IonLabelColorTableModel) colorsJTable.getModel()).getIonAtRow(selectedRow).getName() == "iA"){
                            for (int i = 0; i < 21; i ++){
                                SpectrumPanel.setIonColor(Ion.getGenericIon(IMMONIUM_ION, i), newColor);
                            }
                        } else {
                            SpectrumPanel.setIonColor(((IonLabelColorTableModel) colorsJTable.getModel()).getIonAtRow(selectedRow), newColor);
                        }
                    }

                    ((IonLabelColorTableModel) colorsJTable.getModel()).fireTableDataChanged();
                    updateSpectrumAnnotations();
                }
            }
        }
    }

    /**
     * Select an annotation peak color and update spectrum
     * @param evt Mouse click event
     */
    private void annotatedPeakColorJPanelMouseClicked(MouseEvent evt) {
        Color newColor = JColorChooser.showDialog(this, "Select a Color", annotatedPeakColorJPanel.getBackground());

        if (newColor != null) {
            annotatedPeakColorJPanel.setBackground(newColor);
            utilitiesUserPreferences.setSpectrumAnnotatedPeakColor(annotatedPeakColorJPanel.getBackground());
            updateSpectrumAnnotations();
            annotatedPeakColorJPanel.repaint();
        }
    }

    /**
     * Change cursor to a hand cursor
     * @param evt Mouse move event
     */
    private void annotatedPeakColorJPanelMouseEntered(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Change cursor back to default cursor
     * @param evt
     */
    private void annotatedPeakColorJPanelMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Select a background color and update spectrum
     * @param evt
     */
    private void backgroundPeakColorPanelMouseClicked(MouseEvent evt) {
        Color newColor = JColorChooser.showDialog(this, "Select a Color", backgroundPeakColorJPanel.getBackground());

        if (newColor != null) {
            newColor = new Color(newColor.getRed(), newColor.getGreen(), newColor.getBlue(), 100); // add the default alpha level
            backgroundPeakColorJPanel.setBackground(newColor);
            utilitiesUserPreferences.setSpectrumBackgroundPeakColor(backgroundPeakColorJPanel.getBackground());
            updateSpectrumAnnotations();
            backgroundPeakColorJPanel.repaint();
        }
    }

    /**
     * Change cursor to a hand cursor
     * @param evt Mouse move event
     */
    private void backgroundPeakColorPanelMouseEntered(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Change cursor back to default cursor
     * @param evt Mouse move event
     */
    private void backgroundPeakColorPanelMouseExited(MouseEvent evt) {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Select annotation peaks width
     * @param evt Spinner click event
     */
    private void annotatedPeakWidthValueChanged(ChangeEvent evt) {
        utilitiesUserPreferences.setSpectrumAnnotatedPeakWidth((Float) annotatedPeakWidthJSpinner.getValue());
        updateSpectrumAnnotations();
    }

    /**
     * Select background peaks width
     * @param evt Spinner click event
     */
    private void backgroundPeakWidthValueChanged(ChangeEvent evt) {
        utilitiesUserPreferences.setSpectrumBackgroundPeakWidth((Float) backgroundPeakWidthJSpinner.getValue());
        updateSpectrumAnnotations();
    }

    /**
     * Select limit
     * @param evt Spinner click event
     */
    private void limitSpinnerValueChanged(ChangeEvent evt) {
        annotationSettings.setIntensityFilter((Double) limitJSpinner.getValue());
        updateSpectrumAnnotations();
    }

    /**
     * Sequence strip font size changed.
     * @param evt change event
     */
    private void sequenceFontSizeValueChanged(ChangeEvent evt) {
        spectrumMainPanel.setSequenceFontSize((Integer) sequenceFontSizeJSpinner.getValue());
    }

    /**
     * Toggle showing the confidence bar track.
     * @param evt action event
     */
    private void showConfidenceTrackActionPerformed(ActionEvent evt) {
        boolean show = showConfidenceTrackCheckBox.isSelected();
        showTrackResiduesCheckBox.setEnabled(show);
        spectrumMainPanel.setShowConfidenceTrack(show);
    }

    /**
     * Toggle showing the amino-acid letters in the confidence track.
     * @param evt action event
     */
    private void showTrackResiduesActionPerformed(ActionEvent evt) {
        spectrumMainPanel.setShowConfidenceResidues(showTrackResiduesCheckBox.isSelected());
    }

    /**
     * Update spectrum annotation
     */
    private void updateSpectrumAnnotations(){
        spectrumMainPanel.setUtilitiesUserPreferences(utilitiesUserPreferences);
        spectrumMainPanel.setAnnotationSettings(annotationSettings);
        spectrumMainPanel.updateSpectrumAnnotations();
    }
}

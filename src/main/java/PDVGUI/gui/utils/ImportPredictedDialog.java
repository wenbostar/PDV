package PDVGUI.gui.utils;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ImportPredictedDialog extends JDialog {

    private SpectrumMainPanel spectrumMainPanel;
    private JComboBox modelListComboBox;
    private JComboBox instrumentListComboBox;
    private JSpinner precursorChargeJSpinner;
    private JSpinner collisionEnergyJSpinner;
    private JTable modificationJTable;
    private PeptideAssumption peptideAssumption;
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    private MSnSpectrum predictedSpectrum;
    private String selectedPsmKey;

    private String[] instrumentList = new String[]{"ThermoTOF: ThermoTOF",
            "Astral: ThermoTOF",
            "Lumos: Lumos",
            "QE: QE",
            "timsTOF: timsTOF",
            "SciexTOF: SciexTOF",
            "Fusion: Lumos",
            "Eclipse: Lumos",
            "Velos: Lumos",
            "Elite: Lumos",
            "OrbitrapTribrid: Lumos",
            "ThermoTribrid: Lumos",
            "QE+: QE",
            "QEHF: QE",
            "QEHFX: QE",
            "Exploris: QE",
            "Exploris480:QE"};

    private String[] modelList = new String[]{"AlphaPept_ms2_generic",
            "ms2pip_2021_HCD",
            "Prosit_2019_intensity",
            "Prosit_2023_intensity_timsTOF",
            "Prosit_2020_intensity_CID",
            "Prosit_2020_intensity_HCD",
            "Prosit_2023_intensity_XL_CMS2",
            "Prosit_2020_intensity_TMT"};

    public ImportPredictedDialog(SpectrumMainPanel spectrumMainPanel, String selectedPsmKey,
                                 SpectrumIdentificationAssumption spectrumIdentificationAssumption) {
        super(spectrumMainPanel.parentFrame, true);
        this.spectrumMainPanel = spectrumMainPanel;
        this.setTitle("Import predicted spectra");
        this.selectedPsmKey = selectedPsmKey;
        this.peptideAssumption = (PeptideAssumption) spectrumIdentificationAssumption;

        this.setSize(500, 500);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initComponents();

        setLocationRelativeTo(spectrumMainPanel);
        setVisible(true);
    }

    private void initComponents() {
        modificationJTable = new JTable();
        modelListComboBox = new JComboBox();
        instrumentListComboBox = new JComboBox();
        precursorChargeJSpinner = new JSpinner();
        collisionEnergyJSpinner = new JSpinner();
        JButton submissionButton = new JButton();
        JPanel modificationJPanel = new JPanel();
        JPanel backgroundPanel = new JPanel();
        JScrollPane modificationJScrollPane = new JScrollPane();

        submissionButton.setText("Submit");
        submissionButton.setFont(new Font("Lucida", Font.BOLD, 13));
        submissionButton.addActionListener(this::submissionButtonActionPerformed);

        modelListComboBox.setModel(new DefaultComboBoxModel(this.modelList));
        modelListComboBox.setSelectedIndex(0);
        modelListComboBox.addItemListener(this::modelListComboBoxMouseClicked);

        precursorChargeJSpinner.setModel(new SpinnerNumberModel(peptideAssumption.getIdentificationCharge().value, 1, 6, 1));
        collisionEnergyJSpinner.setModel(new SpinnerNumberModel(25, 0, 100, 1));


        instrumentListComboBox.setModel(new DefaultComboBoxModel(this.instrumentList));
        instrumentListComboBox.setSelectedIndex(0);

        modificationJTable.setModel(new ModificationTableModel());
        modificationJScrollPane.setOpaque(false);

        modificationJTable.setRowHeight(20);
        modificationJTable.setFont(new Font("Arial", Font.PLAIN, 12));
        modificationJTable.getTableHeader().setFont(new Font("Dialog", 0, 12));
        modificationJTable.setOpaque(false);
        modificationJTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        modificationJTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        modificationJScrollPane.setViewportView(modificationJTable);

        modificationJPanel.setOpaque(false);
        GroupLayout modificationJPanelLayout = new GroupLayout(modificationJPanel);
        modificationJPanel.setLayout(modificationJPanelLayout);
        modificationJPanelLayout.setHorizontalGroup(
                modificationJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(modificationJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(modificationJScrollPane, 10, 500, Short.MAX_VALUE)
                                .addContainerGap())
        );
        modificationJPanelLayout.setVerticalGroup(
                modificationJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(modificationJPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(modificationJScrollPane, 10, 300, Short.MAX_VALUE)
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
                                                .addContainerGap()
                                                .addComponent(modelListComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(precursorChargeJSpinner, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(collisionEnergyJSpinner, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(instrumentListComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(modificationJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(submissionButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap()));

        backgroundPanelLayout.setVerticalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(modelListComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(precursorChargeJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(collisionEnergyJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(instrumentListComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(modificationJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(submissionButton)
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

    private String getModificationString(String originalMod) {
        String modificationString = "";

        if (modelListComboBox.getSelectedIndex() == 0 || modelListComboBox.getSelectedIndex() == 7) {
            if (originalMod.contains("of C")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -57.021464) < 0.01) {
                    modificationString = "[UNIMOD:4]";
                }
            }
            if (originalMod.contains("of M")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -15.994915) < 0.01) {
                    modificationString = "[UNIMOD:35]";
                }
            } if (originalMod.contains("of S") || originalMod.contains("of T") || originalMod.contains("of Y") || originalMod.contains("of STY")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -79.966331) < 0.01) {
                    modificationString = "[UNIMOD:21]";
                }
            }

        }

        return modificationString;
    }

    private void submissionButtonActionPerformed(java.awt.event.ActionEvent evt) {

        String model = modelListComboBox.getSelectedItem().toString();
        String pepSeq = peptideAssumption.getPeptide().getSequence();
        int precursorCharge = (int) precursorChargeJSpinner.getValue();
        int collisionEnergy = (int) collisionEnergyJSpinner.getValue();
        String instrument = instrumentListComboBox.getSelectedItem().toString().split(": ")[1];

        HashMap<Integer, String> newMods = new HashMap<>();
        for (int i = 0; i<modificationJTable.getRowCount(); i ++){
            String mod = modificationJTable.getValueAt(i, 2).toString();
            if (!Objects.equals(mod, "")){
                newMods.put((int) modificationJTable.getValueAt(i, 1), mod);
            }
        }

        ProgressDialogX progressDialogX = new ProgressDialogX(spectrumMainPanel.parentFrame,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialogX.setPrimaryProgressCounterIndeterminate(true);
        progressDialogX.setTitle("Searching. Please Wait...");

        new Thread(() -> {
            try {
                progressDialogX.setVisible(true);
            } catch (IndexOutOfBoundsException ignored) {
            }
        }, "searchProgressDialog").start();

        new Thread("Searching") {
            @Override
            public void run() {

                try {
                    GetPredictedOnline getPredictedOnline = new GetPredictedOnline(model, pepSeq, newMods,
                            precursorCharge, collisionEnergy, instrument, peptideAssumption.getTheoreticMz());
                    predictedSpectrum = getPredictedOnline.getSpectra();

                    if (predictedSpectrum == null){
                        progressDialogX.setRunFinished();
                        JOptionPane.showMessageDialog(spectrumMainPanel.parentFrame, JOptionEditorPane.getJOptionEditorPane(
                                        "Failed to fetch predicted spectra from the sever.<br>"
                                                + "Please check the input."),
                                "Prediction error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        spectrumMainPanel.checkSpectrumFileMaps.put(selectedPsmKey, predictedSpectrum);
                        spectrumMainPanel.updateSpectrum();
                    }

                } catch (Exception e){
                    e.printStackTrace();
                    progressDialogX.setRunFinished();
                    JOptionPane.showMessageDialog(spectrumMainPanel.parentFrame, JOptionEditorPane.getJOptionEditorPane(
                                    "Failed to fetch predicted spectra from the sever.<br>"
                                            + "Please check the input."),
                            "Prediction error", JOptionPane.ERROR_MESSAGE);
                }

                progressDialogX.setRunFinished();
            }
        }.start();

        dispose();

    }

    private void modelListComboBoxMouseClicked(java.awt.event.ItemEvent evt) {
        if (modelListComboBox.getSelectedIndex() == 0) {
            instrumentListComboBox.setVisible(true);
        } else {
            instrumentListComboBox.setVisible(false);
        }
    }

    private class ModificationTableModel extends DefaultTableModel {

        ArrayList<ModificationMatch> modificationMatches;
        public ModificationTableModel() {
            this.modificationMatches = peptideAssumption.getPeptide().getModificationMatches();
        }

        @Override
        public int getRowCount() {
            if(modificationMatches == null){
                return 0;
            }
            return modificationMatches.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Modification";
                case 1:
                    return "Site";
                case 2:
                    return "Assigned Mod";
                case 3:
                    return "Supported";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            ModificationMatch modificationMatch = modificationMatches.get(row);
            switch (column) {
                case 0:
                    return modificationMatch.getTheoreticPtm();
                case 1:
                    return modificationMatch.getModificationSite();
                case 2:
                    return getModificationString(modificationMatch.getTheoreticPtm());
                case 3:
                    return !Objects.equals(getModificationString(modificationMatch.getTheoreticPtm()), "");
                default:
                    return "";
            }

        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 2;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 1 ? Integer.class : String.class;
        }
    }

}

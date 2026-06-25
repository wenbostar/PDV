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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class ImportPredictedDialog extends JDialog {

    private SpectrumMainPanel spectrumMainPanel;
    private JComboBox modelListComboBox;
    private JComboBox instrumentListComboBox;
    private JComboBox fragmentMethodComboBox;
    private JSpinner precursorChargeJSpinner;
    private JSpinner collisionEnergyJSpinner;
    private JLabel instrumentListLabel;
    private JLabel fragmentMethodLabel;
    private JCheckBox autoPredictCheckBox;
    private JTable modificationJTable;
    private PeptideAssumption peptideAssumption;
    private MSnSpectrum predictedSpectrum;
    private String selectedPsmKey;

    private String[] instrumentList = new String[]{"QE: QE",
            "Lumos: LUMOS",
            "timsTOF: TIMSTOF",
            "SciexTOF: SCIEXTOF",
            "Fusion: LUMOS",
            "Eclipse: LUMOS",
            "Velos: LUMOS",
            "Elite: LUMOS",
            "OrbitrapTribrid: LUMOS",
            "ThermoTribrid: LUMOS",
            "QE+: QE",
            "QEHF: QE",
            "QEHFX: QE",
            "Exploris: QE",
            "Exploris480: QE"};

    private String[] modelList = new String[]{"AlphaPept_ms2_generic",
            "ms2pip_2021_HCD",
            "Prosit_2019_intensity",
            "Prosit_2023_intensity_timsTOF",
            "Prosit_2020_intensity_CID",
            "Prosit_2020_intensity_HCD",
//            "Prosit_2023_intensity_XL_CMS2",
            "Prosit_2020_intensity_TMT"};

    private String[] fragmentMethodList = new String[]{"HCD", "CID", "ETD"};

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
        fragmentMethodLabel.setVisible(false);
        fragmentMethodComboBox.setVisible(false);
        restoreAutoPredictState();

        setLocationRelativeTo(spectrumMainPanel);
        setVisible(true);
    }

    /**
     * When auto-predict mode is already on, reflect it in the dialog: tick the checkbox and restore
     * the remembered model / collision energy / fragment method / instrument so reopening the dialog
     * shows the active settings instead of the defaults.
     */
    private void restoreAutoPredictState() {
        if (!spectrumMainPanel.isAutoPredictEnabled()) {
            return;
        }
        autoPredictCheckBox.setSelected(true);

        if (spectrumMainPanel.getAutoPredictModel() != null) {
            // Setting the model fires its item listener, which sets the correct instrument/fragment visibility.
            modelListComboBox.setSelectedItem(spectrumMainPanel.getAutoPredictModel());
        }
        collisionEnergyJSpinner.setValue(spectrumMainPanel.getAutoPredictCollisionEnergy());
        if (spectrumMainPanel.getAutoPredictFragmentMethod() != null) {
            fragmentMethodComboBox.setSelectedItem(spectrumMainPanel.getAutoPredictFragmentMethod());
        }
        // Instrument was stored as the suffix after ": "; match it back to a full combo entry.
        String instrument = spectrumMainPanel.getAutoPredictInstrument();
        if (instrument != null) {
            for (int i = 0; i < instrumentListComboBox.getItemCount(); i++) {
                String item = instrumentListComboBox.getItemAt(i).toString();
                if (item.contains(": ") && item.split(": ")[1].equals(instrument)) {
                    instrumentListComboBox.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void initComponents() {
        modificationJTable = new JTable();
        modelListComboBox = new JComboBox();
        instrumentListComboBox = new JComboBox();
        fragmentMethodComboBox = new JComboBox();
        precursorChargeJSpinner = new JSpinner();
        collisionEnergyJSpinner = new JSpinner();
        instrumentListLabel = new JLabel("Instrument: ");
        fragmentMethodLabel = new JLabel("Fragment Method: ");
        JLabel modelListLabel = new JLabel("Model: ");
        JLabel precursorChargeLabel = new JLabel("Precursor Charge: ");
        JLabel collisionEnergyLabel = new JLabel("Collision Energy: ");
        JButton submissionButton = new JButton();
        autoPredictCheckBox = new JCheckBox("Auto-generate for all PSMs");
        autoPredictCheckBox.setToolTipText("Keep these settings and automatically predict a mirror spectrum for every PSM you open, "
                + "without re-opening this dialog. Predictions are fetched on demand as you navigate.");
        autoPredictCheckBox.setOpaque(false);
        JPanel modificationJPanel = new JPanel();
        JPanel backgroundPanel = new JPanel();
        JScrollPane modificationJScrollPane = new JScrollPane();

        submissionButton.setText("Submit");
        submissionButton.setFont(PDVFonts.of(Font.BOLD, 13f));
        submissionButton.addActionListener(this::submissionButtonActionPerformed);

        modelListComboBox.setModel(new DefaultComboBoxModel(this.modelList));
        modelListComboBox.setSelectedIndex(0);
        modelListComboBox.addItemListener(this::modelListComboBoxMouseClicked);

        precursorChargeJSpinner.setModel(new SpinnerNumberModel(peptideAssumption.getIdentificationCharge().value, 1, 6, 1));
        collisionEnergyJSpinner.setModel(new SpinnerNumberModel(27, 0, 100, 1));

        instrumentListComboBox.setModel(new DefaultComboBoxModel(this.instrumentList));
        instrumentListComboBox.setSelectedIndex(0);

        fragmentMethodComboBox.setModel(new DefaultComboBoxModel(this.fragmentMethodList));
        fragmentMethodComboBox.setSelectedIndex(0);

        modificationJTable.setModel(new ModificationTableModel());
        modificationJScrollPane.setOpaque(false);

        PDVTableStyle.applyDefaults(modificationJTable);
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
                                .addComponent(modificationJScrollPane, 10, 150, Short.MAX_VALUE)
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
                                                .addComponent(modelListLabel, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(modelListComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(precursorChargeLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(precursorChargeJSpinner, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(collisionEnergyLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(collisionEnergyJSpinner, GroupLayout.PREFERRED_SIZE, 60, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                                .addComponent(instrumentListLabel, GroupLayout.PREFERRED_SIZE, 80, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(instrumentListComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(fragmentMethodLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(fragmentMethodComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addGap(0, 0, Short.MAX_VALUE))
                                        .addComponent(modificationJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                                .addComponent(submissionButton, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(autoPredictCheckBox)))
                                .addContainerGap()));

        backgroundPanelLayout.setVerticalGroup(
                backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(backgroundPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(modelListLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(modelListComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(precursorChargeLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(precursorChargeJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(collisionEnergyLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(collisionEnergyJSpinner, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(instrumentListLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(instrumentListComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fragmentMethodLabel, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fragmentMethodComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(modificationJPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(backgroundPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(submissionButton)
                                        .addComponent(autoPredictCheckBox))
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
        return getModificationString(modelListComboBox.getSelectedItem().toString(), originalMod);
    }

    /**
     * Map a PDV modification name to the UNIMOD string expected by the given prediction model.
     * Returns "" when the modification is not supported by the model.
     */
    static String getModificationString(String model, String originalMod) {
        PTMFactory ptmFactory = PTMFactory.getInstance();
        String modificationString = "";
        boolean isTMT = model.equals("Prosit_2020_intensity_TMT");

        if (model.equals("AlphaPept_ms2_generic")) {
            // AlphaPept_ms2_generic model
            if (originalMod.contains("of C")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -57.021464) < 0.01) {
                    modificationString = "[UNIMOD:4]";
                }else{
                    System.out.println("This modification is not supported:"+originalMod);
                }
            } else if (originalMod.contains("of M")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -15.994915) < 0.01) {
                    modificationString = "[UNIMOD:35]";
                }else{
                    System.out.println("This modification is not supported:"+originalMod);
                }
            } else if (originalMod.contains("of S") || originalMod.contains("of T") || originalMod.contains("of Y") || originalMod.contains("of STY")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -79.966331) < 0.01) {
                    modificationString = "[UNIMOD:21]";
                }else{
                    System.out.println("This modification is not supported:"+originalMod);
                }
            } else if (originalMod.contains("of N-term")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 42.010565) < 0.01) {
                    modificationString = "[UNIMOD:1]";
                }else if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 43.005814) < 0.01) {
                    modificationString = "[UNIMOD:5]";
                }else{
                    System.out.println("This modification is not supported:"+originalMod);
                }
            } else if (originalMod.contains("of Q")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 0.984016) < 0.01) {
                    modificationString = "[UNIMOD:7]";
                }
            } else if (originalMod.contains("of N")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 0.984016) < 0.01) {
                    modificationString = "[UNIMOD:7]";
                }
            } else if(originalMod.contains("of K")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 229.162932) < 0.01) {
                    modificationString = "[UNIMOD:737]";
                }
            } else if(originalMod.contains("of N-term") || originalMod.contains("of peptide N-term")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 229.162932) < 0.01) {
                    modificationString = "[UNIMOD:737]";
                }
            } else{
                System.out.println("This modification is not supported:"+originalMod);
            }
        } else {
            // Prosit / ms2pip models
            if (originalMod.contains("of C")){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() -57.021464) < 0.01) {
                    modificationString = "[UNIMOD:4]";
                }
            } else if (originalMod.contains("of M")) {
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 15.994915) < 0.01) {
                    modificationString = "[UNIMOD:35]";
                }
            } else if(originalMod.contains("of K") && isTMT){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 229.162932) < 0.01) {
                    modificationString = "[UNIMOD:737]";
                }
            } else if((originalMod.contains("of N-term") || originalMod.contains("of peptide N-term")) && isTMT){
                if (Math.abs(ptmFactory.getPTM(originalMod).getMass() - 229.162932) < 0.01) {
                    modificationString = "[UNIMOD:737]";
                }
            } else{
                System.out.println("This modification is not supported:"+originalMod);
            }

        }

        return modificationString;
    }

    /**
     * Build the site→UNIMOD modification map for a peptide assumption under the given model.
     * Mirrors the per-site logic of the modification table so it can be reused outside the dialog
     * (e.g. by the auto-predict mode). Unsupported modifications are dropped.
     */
    static HashMap<Integer, String> buildModMap(String model, PeptideAssumption peptideAssumption) {
        HashMap<Integer, String> newMods = new HashMap<>();
        ArrayList<ModificationMatch> modificationMatches = peptideAssumption.getPeptide().getModificationMatches();
        if (modificationMatches == null) {
            return newMods;
        }
        for (ModificationMatch modificationMatch : modificationMatches) {
            String assigned = getModificationString(model, modificationMatch.getTheoreticPtm());
            if (Objects.equals(assigned, "")) {
                continue;
            }
            int site;
            if (modificationMatch.getTheoreticPtm().contains("of N-term") || modificationMatch.getTheoreticPtm().contains("of peptide N-term")) {
                site = 0;
            } else {
                site = modificationMatch.getModificationSite();
            }
            newMods.put(site, assigned);
        }
        return newMods;
    }

    private void submissionButtonActionPerformed(java.awt.event.ActionEvent evt) {

        String model = modelListComboBox.getSelectedItem().toString();
        String pepSeq = peptideAssumption.getPeptide().getSequence();
        int precursorCharge = (int) precursorChargeJSpinner.getValue();
        int collisionEnergy = (int) collisionEnergyJSpinner.getValue();
        String instrument = instrumentListComboBox.getSelectedItem().toString().split(": ")[1];
        String fragmentMethod = fragmentMethodComboBox.getSelectedItem().toString();

        // Remember the chosen settings and turn on auto-predict for every PSM, or clear it when unchecked.
        final boolean autoEnabled = autoPredictCheckBox.isSelected();
        spectrumMainPanel.setAutoPredict(autoEnabled, model, instrument, fragmentMethod, collisionEnergy);

        HashMap<Integer, String> newMods = new HashMap<>();
        for (int i = 0; i<modificationJTable.getRowCount(); i ++){
            String mod = modificationJTable.getValueAt(i, 2).toString();
            if (!Objects.equals(mod, "")){
                newMods.put((int) modificationJTable.getValueAt(i, 1), mod);
            }
        }
        //if (modelListComboBox.getSelectedIndex() == 6) {
        //    JOptionPane.showMessageDialog(
        //            null, "This model will add a TMT tag to the N-term of the peptide.",
        //            "Warning", JOptionPane.WARNING_MESSAGE);
        //}

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
                    GetPredictedOnline getPredictedOnline = new GetPredictedOnline(model, pepSeq, newMods,fragmentMethod,
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
                        // With auto mode on, this prediction shares the global settings, so track it for
                        // invalidation when those settings change (a plain manual import is left untracked).
                        if (autoEnabled) {
                            spectrumMainPanel.markAutoPredicted(selectedPsmKey);
                        }
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
            instrumentListLabel.setVisible(true);
            instrumentListComboBox.setVisible(true);
            fragmentMethodLabel.setVisible(false);
            fragmentMethodComboBox.setVisible(false);
        } else {
            if (modelListComboBox.getSelectedIndex() == 6) {
                fragmentMethodLabel.setVisible(true);
                fragmentMethodComboBox.setVisible(true);
            } else {
                fragmentMethodLabel.setVisible(false);
                fragmentMethodComboBox.setVisible(false);
            }
            instrumentListLabel.setVisible(false);
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
                    if(modificationMatch.getTheoreticPtm().contains("of N-term") || modificationMatch.getTheoreticPtm().contains("of peptide N-term")){
                        return 0;
                    }else{
                        return modificationMatch.getModificationSite();
                    }
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

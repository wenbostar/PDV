package PDVGUI.gui.utils.TableModel;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;

import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Denovo results table
 * Created by Ken on 12/1/2017.
 */
public class DeNovoTableModel extends DefaultTableModel {

    /**
     * Selected item
     */
    private ArrayList<ArrayList<Object>> selectedItem;
    /**
     * Spectrum ID list
     */
    private ArrayList<String> spectrumIDList;
    /**
     * Search porameters
     */
    private SearchParameters searchParameters;
    /**
     * Object saving all spectrum
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Decimal format
     */
    private DecimalFormat df = new DecimalFormat("#.0000");
    /**
     * Spectrum key to selected
     */
    private HashMap<String, Boolean> spectrumKeyToSelected;

    /**
     * Constructor
     * @param searchParameters Search parameters
     * @param spectrumFactory Spectrum factory
     * @param spectrumKeyToSelected Spectrum key to selected
     */
    public DeNovoTableModel(SearchParameters searchParameters,  SpectrumFactory spectrumFactory,
                              HashMap<String, Boolean> spectrumKeyToSelected){

        this.searchParameters = searchParameters;
        this.spectrumFactory = spectrumFactory;
        this.spectrumKeyToSelected = spectrumKeyToSelected;
    }

    /**
     * Update table
     * @param selectedItem Selected item
     * @param spectrumIDList Spectrum ID list
     * @param spectrumKeyToSelected Spectrum key to selected
     */
    public void updateTable(ArrayList<ArrayList<Object>> selectedItem, ArrayList<String> spectrumIDList, HashMap<String, Boolean> spectrumKeyToSelected){
        this.selectedItem = selectedItem;
        this.spectrumIDList = spectrumIDList;
        this.spectrumKeyToSelected = spectrumKeyToSelected;
    }

    @Override
    public int getRowCount() {
        if (spectrumIDList != null) {
            return spectrumIDList.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 13;
    }

    @Override
    public String getColumnName(int column) {

        if(column == 0){
            return "Selected";
        }else if(column == 1){
            return " ";
        }else if(column == 2){
            return "Key";
        }else if(column == 3){
            return "RT (min)";
        }else if(column == 4){
            return "Title";
        }else if(column == 5){
            return "Sequence";
        }else if(column == 6){
            return "Charge";
        }else if(column == 7){
            return "m/z";
        }else if(column == 8){
            return "Mass error";
        }else if(column == 9) {
            return "#Peaks";
        }else if(column == 10){
            return "N-Gap";
        }else if(column == 11) {
            return "C-Gap";
        }else if(column == 12){
            return "Score";
        }

        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            if(row < spectrumIDList.size()) {
                String spectrumIndex = spectrumIDList.get(row);

                ArrayList<Object> oneItem = selectedItem.get(row);

                SpectrumMatch spectrumMatch = (SpectrumMatch) oneItem.get(0);

                String matchKey = spectrumMatch.getKey();

                String spectrumTitle = matchKey.split("_cus_")[1];

                MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(matchKey);
                Precursor precursor = spectrum.getPrecursor();

                if(column == 0){
                    return spectrumKeyToSelected.getOrDefault(spectrumIndex, false);
                }
                if(column == 1) {
                    return row + 1;
                }
                if(column == 2) {
                    return spectrumIndex;
                }
                if(column == 3) {
                    Double rt;

                    try {
                        rt = precursor.getRtInMinutes();

                        if (rt < 0) {
                            rt = -1.0;
                        }

                        rt = Double.valueOf(df.format(rt));
                    } catch (Exception e){
                        rt = -0.0;
                    }

                    return rt;
                }
                if(column == 4) {
                    if (spectrumTitle == null) {
                        return "No Title";
                    }
                    return spectrumTitle;
                }
                if (column == 5){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        return spectrumMatch.getBestPeptideAssumption().getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                    } else if (spectrumMatch.getBestTagAssumption() != null){
                        return spectrumMatch.getBestTagAssumption().getTag().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if (column == 6){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                    } else if (spectrumMatch.getBestTagAssumption() != null){
                        return spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if(column == 7) {
                    Double mz;

                    try{

                        mz = Double.valueOf(df.format(precursor.getMz()));

                    } catch (Exception e){
                        mz = -0.0;
                    }

                    return mz;
                }
                if (column == 8){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();
                        return precursor.getMz() - (peptide.getMass()/(precursor.getPossibleCharges().get(0).value));
                    } else if (spectrumMatch.getBestTagAssumption() != null){
                        TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                        return "";
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if (column == 9){
                    try {
                        return spectrum.getPeakList().size();
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                if (column == 10){
                    if (spectrumMatch.getBestTagAssumption() != null){
                        TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                        return tagAssumption.getTag().getNTerminalGap();
                    } else {
                        return null;
                    }
                }
                if (column == 11){
                    if (spectrumMatch.getBestTagAssumption() != null){
                        TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                        return tagAssumption.getTag().getCTerminalGap();
                    } else {
                        return null;
                    }
                }
                if (column == 12){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        return spectrumMatch.getBestPeptideAssumption().getScore();
                    } else if (spectrumMatch.getBestTagAssumption() != null){
                        return spectrumMatch.getBestTagAssumption().getScore();
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }

            }

        return null;

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
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

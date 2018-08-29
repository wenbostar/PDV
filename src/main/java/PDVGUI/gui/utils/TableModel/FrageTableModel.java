package PDVGUI.gui.utils.TableModel;

import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;

import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class FrageTableModel extends DefaultTableModel {

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
     * Decimal format
     */
    private DecimalFormat df = new DecimalFormat("#.0000");
    /**
     * Spectrum key to selected
     */
    private HashMap<String, Boolean> spectrumKeyToSelected;
    /**
     * Extra parameters
     */
    private ArrayList<String> scoreName;

    /**
     * Main constructor
     * @param searchParameters Search parameters
     * @param spectrumKeyToSelected Spectrum key to selected
     * @param scoreName Extra parameters
     */
    public FrageTableModel(SearchParameters searchParameters, HashMap<String, Boolean> spectrumKeyToSelected, ArrayList<String> scoreName){

        this.searchParameters = searchParameters;
        this.spectrumKeyToSelected = spectrumKeyToSelected;
        this.scoreName = scoreName;
    }

    /**
     * Update table according to the given records
     * @param selectedItem Selected items
     * @param spectrumIDList Spectrum Id list
     * @param spectrumKeyToSelected Spectrum key to selected
     */
    public void updateTabel(ArrayList<ArrayList<Object>> selectedItem, ArrayList<String> spectrumIDList, HashMap<String, Boolean> spectrumKeyToSelected){

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
        if(scoreName != null) {
            return scoreName.size()+9;
        } else {
            return 9;
        }
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
        }
        for(int index = 0; index < scoreName.size(); index++){
            int newColumn = index + 9;
            if(column == newColumn){
                return scoreName.get(index);
            }
        }

        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            if (row < spectrumIDList.size()) {
                String spectrumIndex = spectrumIDList.get(row);

                ArrayList<Object> oneItem = selectedItem.get(row);

                SpectrumMatch spectrumMatch = (SpectrumMatch) oneItem.get(0);

                MSnSpectrum spectrum = (MSnSpectrum) oneItem.get(1);
                Precursor precursor = spectrum.getPrecursor();

                String matchKey = spectrumMatch.getKey();

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
                    if (matchKey == null) {
                        return "No Title";
                    }
                    return matchKey;
                }
                if (column == 5){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        return spectrumMatch.getBestPeptideAssumption().getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                    }
                }
                if (column == 6){
                    if (spectrumMatch.getBestPeptideAssumption() != null){
                        return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
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
                if(column == 8){
                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if(peptideAssumption.getScore() != null){
                        return peptideAssumption.getScore();
                    } else {
                        return 0.0;
                    }
                }
                for(int index = 0; index < scoreName.size(); index++){
                    int newColumn = index + 9;
                    if(column == newColumn){
                        Object object = oneItem.get(index+2);
                        if(object.getClass() == String.class){
                            return object;
                        } else if (object.getClass() == Integer.class){
                            return Double.valueOf((Integer)object);
                        } else if (object.getClass() == Double.class){
                            return object;
                        }
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

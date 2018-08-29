package PDVGUI.gui.utils.TableModel;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.amino_acid_tags.Tag;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.props.PrecursorInfo;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;

import javax.swing.table.DefaultTableModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Ken on 9/29/2017.
 */
public class DatabaseTableModel extends DefaultTableModel {

    /**
     * Selected item
     */
    private ArrayList<ArrayList<Object>> selectedItem;
    /**
     * Spectrum ID list
     */
    private ArrayList<String> spectrumIDList;
    /**
     * Search parameters
     */
    private SearchParameters searchParameters;
    /**
     * Extra parameters
     */
    private ArrayList<String> scoreName;
    /**
     * Object saving all spectrum
     */
    private Object spectrumsFileFactory;
    /**
     * Spectrum file type
     */
    private String spectrumFileType;
    /**
     * Decimal format
     */
    private DecimalFormat df = new DecimalFormat("#.0000");
    /**
     * Is new soft or not
     */
    private Boolean isNewSoft = false;
    /**
     * Is maxQUANT or not
     */
    private Boolean isMaxQuant = false;
    /**
     * Is pepXML or not
     */
    private Boolean isPepXML = false;
    /**
     * Spectrum key and it's selected boolean
     */
    private HashMap<String, Boolean> spectrumKeyToSelected;

    /**
     * Empty constructor
     */
    public DatabaseTableModel(){}

    /**
     * Constructor
     * @param searchParameters Search parameters
     * @param scoreName Extral parameters
     * @param spectrumsFileFactory Object saving spectrum
     * @param spectrumFileType Spectrum file type
     * @param spectrumKeyToSelected Spectrum key to selected
     * @param isNewSoft Is news soft or not
     * @param isMaxQuant Is maxQUANT or not
     */
    public DatabaseTableModel(SearchParameters searchParameters, ArrayList<String> scoreName, Object spectrumsFileFactory, String spectrumFileType,
                              HashMap<String, Boolean> spectrumKeyToSelected, Boolean isNewSoft, Boolean isMaxQuant, Boolean isPepXML){

        this.searchParameters = searchParameters;
        this.scoreName = scoreName;
        this.spectrumFileType = spectrumFileType;
        this.spectrumsFileFactory = spectrumsFileFactory;
        this.spectrumKeyToSelected = spectrumKeyToSelected;
        this.isNewSoft = isNewSoft;
        this.isMaxQuant = isMaxQuant;
        this.isPepXML = isPepXML;
    }

    /**
     * Update the table
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
        if(scoreName != null) {
            return scoreName.size()+10;
        } else {
            return 10;
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
        }else if(column == 8) {
            return "Mass error";
        }else if(column == 9){
            return "Other Assumption";
        }
        for(int index = 0; index < scoreName.size(); index++){
            int newColumn = index + 10;
            if(column == newColumn){
                return scoreName.get(index);
            }
        }

        return "";
    }

    @Override
    public Object getValueAt(int row, int column) {

        try{

            if(row < spectrumIDList.size()){
                String spectrumIndex = spectrumIDList.get(row);

                ArrayList<Object> oneItem = selectedItem.get(row);

                SpectrumMatch spectrumMatch = (SpectrumMatch) oneItem.get(0);

                String matchKey = spectrumMatch.getKey();

                Peptide peptide = spectrumMatch.getBestPeptideAssumption().getPeptide();

                if(spectrumFileType.toLowerCase().equals("mgf")) {
                    SpectrumFactory spectrumFactory = (SpectrumFactory) spectrumsFileFactory;

                    String spectrumFileName = matchKey.split("_cus_")[0];
                    String spectrumKey = matchKey.split("_cus_")[1];

                    String spectrumTitle;
                    if( isNewSoft ){
                        spectrumTitle = spectrumKey.split("_rank_")[0];
                    } else if (isMaxQuant || isPepXML){
                        spectrumTitle = spectrumKey;
                    } else {
                        spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, Integer.parseInt(spectrumKey)+1);
                    }

                    MSnSpectrum spectrum = (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle);

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
                            Precursor precursor = spectrum.getPrecursor();
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
                    if(column == 5) {
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                        }
                    }
                    if(column == 6) {
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return  spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + matchKey + ".");
                        }
                    }
                    if(column == 7) {
                        Double mz;

                        try{
                            Precursor precursor = spectrum.getPrecursor();

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
                    if(column == 9) {
                        if(spectrumMatch.getAllAssumptions() != null){
                            if(spectrumMatch.getAllAssumptions().size()>1){
                                int rank1num = 0;
                                for(SpectrumIdentificationAssumption tempAssumption : spectrumMatch.getAllAssumptions()){

                                    if(tempAssumption.getRank() == 1 && rank1num>0){

                                        PeptideAssumption tempPeptideAssumption = (PeptideAssumption) tempAssumption;
                                        return "<html>"+" Rank:"+"&nbsp<html>"+tempPeptideAssumption.getRank() + "&nbsp" +tempPeptideAssumption.getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false)+" </html>";
                                    }

                                    if(tempAssumption.getRank() == 1){
                                        rank1num ++;
                                    }
                                }
                                if(rank1num == 1){
                                    for(SpectrumIdentificationAssumption tempAssumption : spectrumMatch.getAllAssumptions()){

                                        if(tempAssumption.getRank() == 2){

                                            PeptideAssumption tempPeptideAssumption = (PeptideAssumption) tempAssumption;
                                            return "<html>"+" Rank:"+"&nbsp<html>"+tempPeptideAssumption.getRank() + "&nbsp" +tempPeptideAssumption.getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false)+" </html>";
                                        }
                                    }
                                }

                            }else {
                                return "No other assumption";
                            }
                        }else {
                            return "No other assumption";
                        }
                    }
                    for(int index = 0; index < scoreName.size(); index++){
                        int newColumn = index + 10;
                        if(column == newColumn){
                            Object object = oneItem.get(index+1);
                            if(object.getClass() == String.class){
                                return object;
                            } else if (object.getClass() == Integer.class){
                                return Double.valueOf((Integer)object);
                            } else if (object.getClass() == Double.class){
                                return object;
                            }
                        }
                    }

                } else if (spectrumFileType.toLowerCase().equals("mzml") || spectrumFileType.toLowerCase().equals("mzxml")) {
                    ScanCollectionDefault scanCollectionDefault = (ScanCollectionDefault) spectrumsFileFactory;

                    int spectrumNumber = spectrumMatch.getSpectrumNumber();

                    IScan iScan = scanCollectionDefault.getScanByNum(spectrumNumber);

                    PrecursorInfo precursor = iScan.getPrecursor();

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
                        if (precursor != null) {
                            double rt = iScan.getRt();
                            if (rt < 0) {
                                rt = -1;
                            }
                            return   Double.valueOf(df.format(rt));
                        } else {
                            return null;
                        }
                    }
                    if(column == 4) {
                        return spectrumMatch.getKey().split("_cus_")[1];
                    }
                    if(column == 5) {
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return peptide.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            Tag tag = spectrumMatch.getBestTagAssumption().getTag();
                            return tag.getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, true, false, false);
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + spectrumNumber + ".");
                        }
                    }
                    if(column == 6) {
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            return (Integer) spectrumMatch.getBestPeptideAssumption().getIdentificationCharge().value;
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            return (Integer) spectrumMatch.getBestTagAssumption().getIdentificationCharge().value;
                        } else {
                            throw new IllegalArgumentException("No best assumption found for spectrum " + spectrumNumber + ".");
                        }
                    }
                    if(column == 7) {
                        if (precursor != null) {
                            return  Double.valueOf(df.format(precursor.getMzTarget()));
                        } else {
                            return null;
                        }
                    }
                    if(column == 8){
                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        if(peptideAssumption.getScore() != null){
                            return peptideAssumption.getScore();
                        } else {
                            return 0;
                        }
                    }
                    if(column == 9) {
                        if(spectrumMatch.getAllAssumptions() != null){
                            if(spectrumMatch.getAllAssumptions().size()>1){

                                int rank1num = 0;
                                for(SpectrumIdentificationAssumption tempAssumption : spectrumMatch.getAllAssumptions()){

                                    if(tempAssumption.getRank() == 1 && rank1num>0){

                                        PeptideAssumption tempPeptideAssumption = (PeptideAssumption) tempAssumption;
                                        return "<html>"+" Rank:"+"&nbsp<html>"+tempPeptideAssumption.getRank() + "&nbsp" +tempPeptideAssumption.getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false)+" </html>";
                                    }

                                    if(tempAssumption.getRank() == 1){
                                        rank1num ++;
                                    }
                                }
                                if(rank1num == 1){
                                    for(SpectrumIdentificationAssumption tempAssumption : spectrumMatch.getAllAssumptions()){

                                        if(tempAssumption.getRank() == 2){

                                            PeptideAssumption tempPeptideAssumption = (PeptideAssumption) tempAssumption;
                                            return "<html>"+" Rank:"+"&nbsp<html>"+tempPeptideAssumption.getRank() + "&nbsp" +tempPeptideAssumption.getPeptide().getTaggedModifiedSequence(searchParameters.getPtmSettings(), true, true, false, false)+" </html>";
                                        }
                                    }
                                }
                            }else {
                                return "No other assumption";
                            }
                        }else {
                            return "No other assumption";
                        }

                    }
                    for(int index = 0; index < scoreName.size(); index++){
                        int newColumn = index + 10;
                        if(column == newColumn){
                            Object object = oneItem.get(index+1);
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

            } else {
                return null;
            }


        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Table not instantiated.");

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

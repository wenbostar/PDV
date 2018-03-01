package PDVGUI.fileimport;

import com.google.common.collect.Range;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import io.github.msdk.io.mzml.data.MzMLRawDataFile;
import io.github.msdk.io.mzxml.MzXMLFileParser;
import io.github.msdk.io.mzxml.MzXMLRawDataFile;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;

import java.io.File;
import java.util.*;

/**
 * Import spectrum file
 * Created by Ken on 10/24/2017.
 */
public class MSOneImport {

    /**
     * Spectrum file path
     */
    private String spectrumFilePath;
    /**
     * Spectrum file type
     */
    private String spectrumFileType;
    /**
     * Object saving spectrum
     */
    private ScanCollectionDefault scans = new ScanCollectionDefault();
    /**
     * Spectrum key to Rt and Int
     */
    private HashMap<String, ArrayList<float[]>> keyToRtAndInt = new HashMap<>();
    /**
     * Detail list
     */
    private ArrayList<String> detailsList = new ArrayList<>();

    /**
     * Constructor
     * @param spectrumFilePath Spectrum file path
     * @param spectrumFileType Spectrum file type
     */
    public MSOneImport(String spectrumFilePath, String spectrumFileType){

        this.spectrumFilePath = spectrumFilePath;
        this.spectrumFileType = spectrumFileType;

        parseSpectrum();
    }

    /**
     * Read file
     */
    private void parseSpectrum(){

        int ms1Count = 0;
        int ms2Count = 0;

        Range<Float> rtRange;

        float[] rtAndItem;
        ArrayList<float[]> rtToItem = new ArrayList<>();

        scans.setDefaultStorageStrategy(StorageStrategy.SOFT);
        scans.isAutoloadSpectra(true);

        if (spectrumFileType.equals("mzml")){

            File spectrumFile = new File(spectrumFilePath);

            MzMLInfoImport mzMLInfoImport = new MzMLInfoImport(spectrumFile);

            detailsList.addAll(mzMLInfoImport.getDetailsList());

            MzMLFileImportMethod mzMLFileImportMethod = new MzMLFileImportMethod(spectrumFilePath);

            try {
                mzMLFileImportMethod.execute();
            } catch (MSDKException e) {
                e.printStackTrace();
            }

            MzMLRawDataFile mzMLRawDataFile = (MzMLRawDataFile) mzMLFileImportMethod.getResult();

            if (spectrumFile.length() > 524288000) {

                for (Chromatogram chromatogram : mzMLRawDataFile.getChromatograms()) {
                    rtToItem = new ArrayList<>();

                    for (int index = 0; index < chromatogram.getRetentionTimes().length; index++) {

                        rtAndItem = new float[2];
                        rtAndItem[0] = chromatogram.getRetentionTimes()[index];
                        rtAndItem[1] = chromatogram.getIntensityValues()[index];
                        rtToItem.add(rtAndItem);
                    }

                    keyToRtAndInt.put(chromatogram.getChromatogramType().toString(), rtToItem);

                    rtRange = chromatogram.getRtRange();

                    detailsList.add("RT/t/Start:"+rtRange.lowerEndpoint()+" End:"+rtRange.upperEndpoint());
                }

            } else {

                for (MsScan msScan : mzMLRawDataFile.getScans()) {

                    if (msScan.getMsLevel() == 1) {
                        rtAndItem = new float[2];
                        ms1Count++;

                        rtAndItem[0] = msScan.getRetentionTime();
                        rtAndItem[1] = msScan.getTIC();
                        rtToItem.add(rtAndItem);

                    } else if (msScan.getMsLevel() == 2) {
                        ms2Count++;
                    }
                }

                keyToRtAndInt.put("TIC", rtToItem);

                detailsList.add("RT/t/Start:-1"+" End:-1");
            }

        } else if (spectrumFileType.equals("mzxml")){

            MzXMLInfoImport mzXMLInfoImport = new MzXMLInfoImport(spectrumFilePath);

            detailsList.addAll(mzXMLInfoImport.getDetailsList());

            MzXMLFileParser mzXMLFileParser = new MzXMLFileParser(spectrumFilePath);

            try {
                mzXMLFileParser.execute();
            } catch (MSDKException e) {
                e.printStackTrace();
            }

            MzXMLRawDataFile mzXMLRawDataFile = (MzXMLRawDataFile) mzXMLFileParser.getResult();

            for (MsScan msScan : mzXMLRawDataFile.getScans()){
                if (msScan.getMsLevel() == 1){
                    rtAndItem = new float[2];

                    ms1Count ++;

                    rtAndItem[0] = msScan.getRetentionTime();
                    rtAndItem[1] = msScan.getTIC();

                    rtToItem.add(rtAndItem);

                } else if (msScan.getMsLevel() == 2){
                    ms2Count ++;
                }
            }
            keyToRtAndInt.put("TIC", rtToItem);

            detailsList.add("RT/t/Start:-1"+" End:-1");
        }

         String[] msNum = new String[3];

         msNum[0] = "MS Num";
         msNum[1] = String.valueOf(ms1Count);
         msNum[2] = String.valueOf(ms2Count);

         detailsList.add("MS Num/t/MS1:"+ms1Count+" MS2:"+ms2Count);
     }

    /**
     * Return key to RT and Int
     * @return Hash map
     */
     public HashMap<String, ArrayList<float[]>> getKeyToRtAndInt(){
         return keyToRtAndInt;
     }

    /**
     * Get details list
     * @return ArrayList<String>
     */
     public ArrayList<String> getDetailsList() {
        return detailsList;
    }
}

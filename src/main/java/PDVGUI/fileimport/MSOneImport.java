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
import java.math.BigDecimal;
import java.math.BigInteger;
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
     * The biggest number.
     */
    private float biggestNum = 0;

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
                    float[] rtArray = chromatogram.getRetentionTimes();
                    float[] intArray = chromatogram.getIntensityValues();

                    for (int index = 0; index < rtArray.length; index++) {

                        rtAndItem = new float[2];
                        rtAndItem[0] = rtArray[index];
                        rtAndItem[1] = intArray[index];
                        rtToItem.add(rtAndItem);

                        if (rtAndItem[1] > biggestNum){
                            biggestNum = rtAndItem[1];
                        }
                    }

                    keyToRtAndInt.put(chromatogram.getChromatogramType().toString(), rtToItem);

                    rtRange = chromatogram.getRtRange();

                    detailsList.add("RT/t/Start:"+rtRange.lowerEndpoint()+" End:"+rtRange.upperEndpoint());
                }

            } else {

                float startRT = 10000;
                float endRT = 0f;

                for (MsScan msScan : mzMLRawDataFile.getScans()) {

                    if (msScan.getMsLevel() == 1) {
                        rtAndItem = new float[2];
                        ms1Count++;

                        rtAndItem[0] = msScan.getRetentionTime()/60;
                        rtAndItem[1] = msScan.getTIC();
                        rtToItem.add(rtAndItem);

                        if (rtAndItem[1] > biggestNum){
                            biggestNum = rtAndItem[1];
                        }

                        if (rtAndItem[0] > endRT){
                            endRT = rtAndItem[0];
                        }
                        if (rtAndItem[0] < startRT){
                            startRT = rtAndItem[0];
                        }

                    } else if (msScan.getMsLevel() == 2) {
                        ms2Count++;
                    }
                }

                keyToRtAndInt.put("TIC", rtToItem);

                detailsList.add("RT (min)/t/Start:" + startRT +" End:" + endRT);
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

                    rtAndItem[0] = msScan.getRetentionTime()/60;
                    rtAndItem[1] = msScan.getTIC();

                    if (rtAndItem[1] > biggestNum){
                        biggestNum = rtAndItem[1];
                    }

                    rtToItem.add(rtAndItem);

                } else if (msScan.getMsLevel() == 2){
                    ms2Count ++;
                }
            }
            keyToRtAndInt.put("TIC", rtToItem);

            //detailsList.add("RT/t/Start:-1"+" End:-1");
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

    /**
     * Get biggest num
     */
    public BigInteger getBiggestNum(){
        return BigInteger.valueOf((long) biggestNum);
    }
}

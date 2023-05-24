package PDVGUI.fileimport;

import com.google.common.collect.Range;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import io.github.msdk.io.mzml.data.MzMLRawDataFile;
import io.github.msdk.io.mzxml.MzXMLFileParser;
import io.github.msdk.io.mzxml.MzXMLRawDataFile;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.lcmsrun.LCMSRunInfo;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scancollection.IScanCollection;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.fileio.exceptions.FileParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;
import umich.ms.fileio.filetypes.mzml.MZMLIndex;

import java.io.File;
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

        if (spectrumFileType.equals("mzml")){

            File spectrumFile = new File(spectrumFilePath);

            MzMLInfoImport mzMLInfoImport = new MzMLInfoImport(spectrumFile);

            detailsList.addAll(mzMLInfoImport.getDetailsList());

            MzMLFileImportMethod mzMLFileImportMethod = new MzMLFileImportMethod(spectrumFilePath);

            try {
                mzMLFileImportMethod.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            MzMLRawDataFile mzMLRawDataFile = (MzMLRawDataFile) mzMLFileImportMethod.getResult();

            List<MsScan> msScans = mzMLRawDataFile.getScans();

            System.out.println("Load file:"+spectrumFile.getAbsolutePath());

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

                    detailsList.add("LC gradient length/t/"+String.format("%.0f",rtRange.lowerEndpoint())+" - "+String.format("%.0f",rtRange.upperEndpoint())+" min");
                }

                for (MsScan msScan : msScans) {
                    if (msScan.getMsLevel() == 1) {
                        ms1Count++;
                    } else if (msScan.getMsLevel() == 2) {
                        ms2Count++;
                    }
                }

            } else {
                System.out.println("Extract meta information from mzML ...");

                float startRT = 10000;
                float endRT = 0f;

                for (MsScan msScan : msScans) {

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
                detailsList.add("LC gradient length/t/"+String.format("%.0f",startRT)+" - "+String.format("%.0f",endRT)+" min");

                //detailsList.add("RT (min)/t/Start:" + startRT +" End:" + endRT);
            }

            HashMap<String,String> ms2meta = get_ms2_meta(spectrumFilePath);
            if(ms2meta.size()>=1) {
                for(String item: ms2meta.keySet()){
                    detailsList.add(item+"/t/" + ms2meta.get(item));
                }
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
         detailsList.add("MS1 Spectra/t/"+ms1Count);
         detailsList.add("MS2 Spectra/t/"+ms2Count);

         // detailsList.add("MS Num/t/MS1:"+ms1Count+" MS2:"+ms2Count);
         System.out.println("Load MS data: done!");
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


    public HashMap<String, String> get_ms2_meta(String fullMsFilePath){

        HashMap<String,String> ms2meta = new HashMap<>();
        String frag_method = "-";
        MZMLFile source = new MZMLFile(fullMsFilePath);

        LCMSRunInfo lcmsRunInfo = null;
        try {
            lcmsRunInfo = source.fetchRunInfo();
        } catch (FileParsingException e) {
            e.printStackTrace();
        }
        source.setNumThreadsForParsing(1);
        ms2meta.put("MS Instrument",source.getRunInfo().getDefaultInstrument().getModel());

        MZMLIndex mzMLindex = null;
        try {
            mzMLindex = source.fetchIndex();
        } catch (FileParsingException e) {
            e.printStackTrace();
        }

        if (mzMLindex.size() > 0) {

        } else {
            System.err.println("Parsed index was empty!");
        }

        IScanCollection scans;

        scans = new ScanCollectionDefault(true);
        scans.setDataSource(source);
        try {
            scans.loadData(LCMSDataSubset.MS2_WITH_SPECTRA, StorageStrategy.STRONG);
        } catch (FileParsingException e) {
            e.printStackTrace();
        }

        TreeMap<Integer, IScan> num2scanMap = scans.getMapNum2scan();
        Set<Map.Entry<Integer, IScan>> num2scanEntries = num2scanMap.entrySet();



        for (Map.Entry<Integer, IScan> next : num2scanEntries) {
            IScan scan = next.getValue();
            if (scan.getSpectrum() != null) {
                //System.out.println(scan.getNum());
                if (scan.getMsLevel() == 2) {
                    frag_method = scan.getPrecursor().getActivationInfo().getActivationMethod();
                    double ce_h = scan.getPrecursor().getActivationInfo().getActivationEnergyHi();
                    double ce_l = scan.getPrecursor().getActivationInfo().getActivationEnergyLo();
                    if(ce_h == ce_l){
                        ms2meta.put("MS2 CE",String.valueOf(ce_h));
                    }else{
                        ms2meta.put("MS2 CE",String.valueOf(ce_l)+":"+String.valueOf(ce_h));
                    }
                    ms2meta.put("MS2 Fragmentation",frag_method);
                    // ms2meta.put("MS2 Analyzer",scan.getInstrument().getAnalyzer());
                    // ms2meta.put("MS2 Detector",scan.getInstrument().getDetector());
                    break;
                }
            }
        }
        return ms2meta;
    }
}

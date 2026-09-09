package PDVGUI.fileimport;

import com.google.common.collect.Range;
import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.Chromatogram;
import io.github.msdk.datamodel.ChromatogramType;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.io.mzml.MzMLFileImportMethod;
import io.github.msdk.io.mzml.data.MzMLRawDataFile;
import io.github.msdk.io.mzxml.MzXMLFileParser;
import io.github.msdk.io.mzxml.MzXMLRawDataFile;
import umich.ms.datatypes.LCMSDataSubset;
import umich.ms.datatypes.lcmsrun.LCMSRunInfo;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.ActivationInfo;
import umich.ms.datatypes.scan.props.Instrument;
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
     * Number of scans whose headers are parsed in the first round of the search for the first MS2
     * scan. Almost every run answers within that first round.
     */
    private static final int FIRST_SCAN_WINDOW = 200;
    /**
     * Largest number of scans parsed in one round. The window doubles after every round that finds
     * no MS2 scan, so a run that has none at all costs a handful of rounds rather than hundreds.
     */
    private static final int MAX_SCAN_WINDOW = 1 << 16;

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

                float chromStartRT = Float.MAX_VALUE;
                float chromEndRT = 0f;

                for (Chromatogram chromatogram : selectIonCurrentChromatograms(mzMLRawDataFile.getChromatograms())) {
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

                    // A run can hold several chromatograms of the same type (one per SRM
                    // transition, for example), so the type alone is not a unique key.
                    String chromKey = chromatogram.getChromatogramType().toString();
                    if (keyToRtAndInt.containsKey(chromKey)){
                        chromKey = chromKey + " " + chromatogram.getChromatogramNumber();
                    }
                    keyToRtAndInt.put(chromKey, rtToItem);

                    rtRange = chromatogram.getRtRange();

                    if (rtRange.lowerEndpoint() < chromStartRT){
                        chromStartRT = rtRange.lowerEndpoint();
                    }
                    if (rtRange.upperEndpoint() > chromEndRT){
                        chromEndRT = rtRange.upperEndpoint();
                    }
                }

                if (!keyToRtAndInt.isEmpty()){
                    detailsList.add("LC gradient length/t/"+String.format("%.0f",chromStartRT)+" - "+String.format("%.0f",chromEndRT)+" min");
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
                if (ms1Count > 0){
                    detailsList.add("LC gradient length/t/"+String.format("%.0f",startRT)+" - "+String.format("%.0f",endRT)+" min");
                }

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
     * Keep only the ion current chromatograms of a run. Runs also carry chromatograms that are not
     * ion currents at all, such as the pump pressure traces written by msconvert, and MSDK reports
     * every one of those as UNKNOWN. Plotting them on the intensity axis is meaningless, so they
     * are dropped unless they are all the run has.
     * @param chromatograms All chromatograms of the run
     * @return The ion current chromatograms, or the input list if there are none
     */
    private List<Chromatogram> selectIonCurrentChromatograms(List<Chromatogram> chromatograms){

        List<Chromatogram> ionCurrentChromatograms = new ArrayList<>();

        for (Chromatogram chromatogram : chromatograms){
            if (chromatogram.getChromatogramType() != ChromatogramType.UNKNOWN){
                ionCurrentChromatograms.add(chromatogram);
            }
        }

        return ionCurrentChromatograms.isEmpty() ? chromatograms : ionCurrentChromatograms;
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
        MZMLFile source = new MZMLFile(fullMsFilePath);
        source.setNumThreadsForParsing(1);

        try {
            LCMSRunInfo lcmsRunInfo = source.fetchRunInfo();

            Instrument instrument = lcmsRunInfo.getDefaultInstrument();
            if (instrument != null && instrument.getModel() != null) {
                ms2meta.put("MS Instrument", instrument.getModel());
            }

            MZMLIndex mzMLindex = source.fetchIndex();

            if (mzMLindex.size() == 0) {
                System.err.println("Parsed index was empty!");
                return ms2meta;
            }

            // Only the first MS2 scan is inspected, so parse scan headers in small windows from the
            // start of the run. Loading every MS2 spectrum reads and decodes the whole file for
            // nothing, which costs minutes and gigabytes of memory on a large run.
            int scanNumLo = mzMLindex.getMapByNum().firstKey();
            int lastScanNum = mzMLindex.getMapByNum().lastKey();
            int scanWindow = FIRST_SCAN_WINDOW;

            while (scanNumLo <= lastScanNum) {

                int scanNumHi = (int) Math.min((long) scanNumLo + scanWindow - 1, (long) lastScanNum);

                IScanCollection scans = new ScanCollectionDefault(true);
                scans.setDataSource(source);
                scans.loadData(new LCMSDataSubset(scanNumLo, scanNumHi, Collections.<Integer>emptySet(), null),
                        StorageStrategy.SOFT);

                for (IScan scan : scans.getMapNum2scan().values()) {

                    if (scan.getMsLevel() == null || scan.getMsLevel() != 2 || scan.getPrecursor() == null) {
                        continue;
                    }

                    ActivationInfo activationInfo = scan.getPrecursor().getActivationInfo();

                    if (activationInfo.getActivationMethod() != null) {
                        ms2meta.put("MS2 Fragmentation", activationInfo.getActivationMethod());
                    }

                    // Activation energies are optional in mzML and are absent from some files.
                    Double ce_l = activationInfo.getActivationEnergyLo();
                    Double ce_h = activationInfo.getActivationEnergyHi();
                    if (ce_l != null && ce_h != null) {
                        if (ce_h.equals(ce_l)) {
                            ms2meta.put("MS2 CE", String.valueOf(ce_h));
                        } else {
                            ms2meta.put("MS2 CE", ce_l + ":" + ce_h);
                        }
                    } else if (ce_h != null) {
                        ms2meta.put("MS2 CE", String.valueOf(ce_h));
                    } else if (ce_l != null) {
                        ms2meta.put("MS2 CE", String.valueOf(ce_l));
                    }

                    // ms2meta.put("MS2 Analyzer",scan.getInstrument().getAnalyzer());
                    // ms2meta.put("MS2 Detector",scan.getInstrument().getDetector());
                    return ms2meta;
                }

                scanNumLo = scanNumHi + 1;
                scanWindow = Math.min(scanWindow * 2, MAX_SCAN_WINDOW);
            }

        } catch (Exception e) {
            // Metadata is a nice-to-have: a run whose header or index cannot be read should still
            // open, without its instrument and fragmentation rows.
            e.printStackTrace();
        }

        return ms2meta;
    }
}

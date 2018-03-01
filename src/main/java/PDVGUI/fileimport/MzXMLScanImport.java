package PDVGUI.fileimport;

import io.github.msdk.MSDKException;
import io.github.msdk.datamodel.IsolationInfo;
import io.github.msdk.datamodel.MsScan;
import io.github.msdk.io.mzxml.MzXMLFileImportMethod;
import io.github.msdk.io.mzxml.MzXMLFileParser;
import io.github.msdk.io.mzxml.MzXMLRawDataFile;
import umich.ms.datatypes.scan.IScan;
import umich.ms.datatypes.scan.StorageStrategy;
import umich.ms.datatypes.scan.props.*;
import umich.ms.datatypes.scancollection.IScanCollection;
import umich.ms.datatypes.scancollection.impl.ScanCollectionDefault;
import umich.ms.datatypes.spectrum.ISpectrum;

import java.io.File;
import java.util.List;

/**
 * MzXML spectral import
 * Created by Ken on 12/26/2017.
 */
public class MzXMLScanImport {

    /**
     * MzXML file
     */
    private String mzXMLFile;
    /**
     * Spectral object
     */
    private ScanCollectionDefault scans = new ScanCollectionDefault();

    /**
     * Constructor
     * @param mzXMLFile mzXML file
     */
    public MzXMLScanImport(String mzXMLFile){
        this.mzXMLFile = mzXMLFile;

        parsingFile();
    }

    /**
     * Reading file get spectral
     */
    private void parsingFile(){

        MzXMLFileImportMethod mzXMLFileImportMethod = new MzXMLFileImportMethod(new File(mzXMLFile));

        try {
            mzXMLFileImportMethod.execute();
        } catch (MSDKException e) {
            e.printStackTrace();
        }

        MzXMLFileParser mzXMLFileParser = new MzXMLFileParser(mzXMLFile);

        try {
            mzXMLFileParser.execute();
        } catch (MSDKException e) {
            e.printStackTrace();
        }

        MzXMLRawDataFile mzXMLRawDataFile = (MzXMLRawDataFile) mzXMLFileParser.getResult();
        IScan iScan;

        for (MsScan msScan : mzXMLRawDataFile.getScans()){

            iScan = new IScan() {
                @Override
                public int getNum() {
                    return msScan.getScanNumber();
                }

                @Override
                public IScanCollection getScanCollection() {
                    return null;
                }

                @Override
                public Double getRt() {
                    return Double.valueOf(msScan.getRetentionTime());
                }

                @Override
                public Integer getMsLevel() {
                    return msScan.getMsLevel();
                }

                @Override
                public Boolean isCentroided() {
                    return null;
                }

                @Override
                public Polarity getPolarity() {
                    return null;
                }

                @Override
                public ScanType getScanType() {
                    return null;
                }

                @Override
                public PrecursorInfo getPrecursor() {

                    PrecursorInfo precursorInfo = new PrecursorInfo();

                    IsolationInfo isolationInfo = msScan.getIsolations().get(0);

                    precursorInfo.setCharge(isolationInfo.getPrecursorCharge());
                    precursorInfo.setMzTarget(isolationInfo.getPrecursorMz());
                    precursorInfo.setIntensity(0.0);

                    return precursorInfo;
                }

                @Override
                public List<Integer> getChildScans() {
                    return null;
                }

                @Override
                public Instrument getInstrument() {
                    return null;
                }

                @Override
                public InjectionInfo getInjectionInfo() {
                    return null;
                }

                @Override
                public Double getBasePeakIntensity() {
                    return null;
                }

                @Override
                public Double getBasePeakMz() {
                    return null;
                }

                @Override
                public Double getTic() {
                    return Double.valueOf(msScan.getTIC());
                }

                @Override
                public Double getScanMzWindowLower() {
                    return null;
                }

                @Override
                public Double getScanMzWindowUpper() {
                    return null;
                }

                @Override
                public ISpectrum getSpectrum() {

                    ISpectrum spectrum = new ISpectrum() {
                        @Override
                        public double getMinMZ() {
                            return 0;
                        }

                        @Override
                        public double getMaxMZ() {
                            return 0;
                        }

                        @Override
                        public double getMinInt() {
                            return 0;
                        }

                        @Override
                        public double getMinIntNonZero() {
                            return 0;
                        }

                        @Override
                        public double getMaxInt() {
                            return 0;
                        }

                        @Override
                        public double getMaxIntMz() {
                            return 0;
                        }

                        @Override
                        public double getSumInt() {
                            return 0;
                        }

                        @Override
                        public double[] getMZs() {
                            return msScan.getMzValues();
                        }

                        @Override
                        public double[] getIntensities() {
                            float[] intensities = msScan.getIntensityValues();
                            double[] intensity = new double[intensities.length];
                            for (int index = 0; index < intensities.length; index ++){
                                intensity[index] = (double) intensities[index];
                            }
                            return intensity;
                        }

                        @Override
                        public Integer findMzIdxFloor(double v) {
                            return null;
                        }

                        @Override
                        public Integer findMzIdxCeiling(double v) {
                            return null;
                        }

                        @Override
                        public Integer findClosestMzIdx(double v) {
                            return null;
                        }

                        @Override
                        public int[] findMzIdxs(double v, double v1) {
                            return new int[0];
                        }

                        @Override
                        public int[] findMzIdxsWithinPpm(double v, double v1) {
                            return new int[0];
                        }
                    };

                    return spectrum;
                }

                @Override
                public ISpectrum fetchSpectrum() {
                    return null;
                }

                @Override
                public StorageStrategy getStorageStrategy() {
                    return null;
                }

                @Override
                public void setRt(Double aDouble) {

                }

                @Override
                public void setScanCollection(IScanCollection iScanCollection) {

                }

                @Override
                public void setMsLevel(Integer integer) {

                }

                @Override
                public void setCentroided(Boolean aBoolean) {

                }

                @Override
                public void setScanType(ScanType scanType) {

                }

                @Override
                public void setPolarity(Polarity polarity) {

                }

                @Override
                public void setPrecursor(PrecursorInfo precursorInfo) {

                }

                @Override
                public void setChildScans(List<Integer> list) {

                }

                @Override
                public void setInstrument(Instrument instrument) {

                }

                @Override
                public void setInjectionInfo(InjectionInfo injectionInfo) {

                }

                @Override
                public void setBasePeakIntensity(Double aDouble) {

                }

                @Override
                public void setBasePeakMz(Double aDouble) {

                }

                @Override
                public void setTic(Double aDouble) {

                }

                @Override
                public void setScanMzWindowLower(Double aDouble) {

                }

                @Override
                public void setScanMzWindowUpper(Double aDouble) {

                }

                @Override
                public void setSpectrum(ISpectrum iSpectrum, boolean b) {

                }

                @Override
                public void setStorageStrategy(StorageStrategy storageStrategy) {

                }
            };

            scans.addScan(iScan);
        }
    }

    /**
     * Get spectral object
     * @return ScanCollectionDefault
     */
    public ScanCollectionDefault getScans(){
        return scans;
    }
}

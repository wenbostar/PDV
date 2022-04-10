package PDVGUI.fileimport;

import umich.ms.fileio.exceptions.RunHeaderParsingException;
import umich.ms.fileio.filetypes.mzml.MZMLFile;
import umich.ms.fileio.filetypes.mzml.MZMLRunHeaderParser;
import umich.ms.fileio.filetypes.mzml.MZMLRunInfo;
import umich.ms.fileio.filetypes.mzml.jaxb.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MzML information import
 * Created by Ken on 12/26/2017.
 */
public class MzMLInfoImport {

    /**
     * MzML file
     */
    private File mzMLFile;
    /**
     * Original information hash
     */
    private ArrayList<String> detailsList = new ArrayList<>();

    /**
     * Constructor
     * @param mzMLFile MzMLFile
     */
    public MzMLInfoImport(File mzMLFile){
        this.mzMLFile = mzMLFile;
        getInfo();
    }

    /**
     * Get all details
     */
    private void getInfo(){
        MZMLFile mzmlFile = new MZMLFile(mzMLFile.getAbsolutePath());

        MZMLRunHeaderParser mzmlRunHeaderParser = new MZMLRunHeaderParser(mzmlFile);

        try {
            MZMLRunInfo mzmlRunInfo = mzmlRunHeaderParser.parse();

            MzMLType mzMLType = mzmlRunInfo.getParsedInfo();
            //CV list
            CVListType cvListType = mzMLType.getCvList();
            String[] items;
            ArrayList<String[]> itemsList;
            for (CVType cvType : cvListType.getCv()){

                detailsList.add(cvType.getId() + "/t/" + cvType.getFullName() + "(" +cvType.getVersion() + ")");
            }

            FileDescriptionType fileDescriptionType = mzMLType.getFileDescription();
            //fileContent
            ParamGroupType paramGroupType = fileDescriptionType.getFileContent();
            itemsList = new ArrayList<>();
            for (UserParamType userParamType : paramGroupType.getUserParam()){
                items = new String[3];
                items[0] = "User para";
                items[1] = userParamType.getName();
                items[2] = userParamType.getValue();
                itemsList.add(items);
            }
            for (CVParamType cvParamType : paramGroupType.getCvParam()){
                items = new String[3];
                items[0] = cvParamType.getAccession();
                items[1] = cvParamType.getName();
                items[2] = cvParamType.getValue();
                itemsList.add(items);
            }

            //getContact
            for (ParamGroupType paramGroupType1 : fileDescriptionType.getContact()){
                for (CVParamType cvParamType : paramGroupType1.getCvParam()){

                    detailsList.add(cvParamType.getName() + "/t/" + cvParamType.getValue());
                }
                for (UserParamType userParamType : paramGroupType1.getUserParam()){

                    detailsList.add(userParamType.getName() + "/t/" + userParamType.getValue());
                }
            }

            //sourceFileList
            SourceFileListType sourceFileListType = fileDescriptionType.getSourceFileList();
            for (SourceFileType sourceFileType : sourceFileListType.getSourceFile()){

                detailsList.add("Raw file:"+sourceFileType.getId() + "/t/" + sourceFileType.getName());
            }

            //referenceableParamGroup
            Map<String, List<CVParamType>> stringListHashMap =  mzmlRunInfo.getRefParamGroups();
            for (String key : stringListHashMap.keySet()){
                StringBuilder wholeName = new StringBuilder();
                for (CVParamType cvParamType : stringListHashMap.get(key)){
                    wholeName.append("/").append(cvParamType.getName()).append("(").append(cvParamType.getValue()).append(")");
                }
                detailsList.add(key + "/t/" + wholeName.substring(1, wholeName.length()));

            }

            //referenceableParamGroupList
            ReferenceableParamGroupListType referenceableParamGroupListType = mzMLType.getReferenceableParamGroupList();
            itemsList = new ArrayList<>();
             for(ReferenceableParamGroupType referenceableParamGroupType : referenceableParamGroupListType.getReferenceableParamGroup()){
                 for (CVParamType cvParamType : referenceableParamGroupType.getCvParam()){
                     items = new String[3];
                     items[0] = cvParamType.getAccession();
                     items[1] = cvParamType.getName();
                     items[2] = cvParamType.getValue();
                     itemsList.add(items);
                 }
                 for (UserParamType userParamType : referenceableParamGroupType.getUserParam()){
                     items = new String[3];
                     items[0] = "User para";
                     items[1] = userParamType.getName();
                     items[2] = userParamType.getValue();
                     itemsList.add(items);
                 }
             }

            //softwareList
            InstrumentConfigurationListType instrumentConfigurationListType = mzMLType.getInstrumentConfigurationList();
            SoftwareListType softwareListType = mzMLType.getSoftwareList();
            StringBuilder softWholeName = new StringBuilder();
            for (SoftwareType softwareType : softwareListType.getSoftware()){
                softWholeName.append("/").append(softwareType.getId()).append("(").append(softwareType.getVersion()).append(")");
            }
            detailsList.add("Software/t/" + softWholeName.substring(1, softWholeName.length()));

            //instrumentConfigurationList
             for (InstrumentConfigurationType instrumentConfigurationType : instrumentConfigurationListType.getInstrumentConfiguration()){

                 for (AnalyzerComponentType analyzerComponentType : instrumentConfigurationType.getComponentList().getAnalyzer()){
                     StringBuilder wholeName = new StringBuilder();
                     for (CVParamType cvParamType : analyzerComponentType.getCvParam()){

                         wholeName.append("/").append(cvParamType.getName());
                     }
                     detailsList.add(instrumentConfigurationType.getId()+": analyzer/t/" + wholeName.substring(1, wholeName.length()));
                 }
                 for (DetectorComponentType detectorComponentType : instrumentConfigurationType.getComponentList().getDetector()){
                     StringBuilder wholeName = new StringBuilder();
                     for (CVParamType cvParamType : detectorComponentType.getCvParam()){

                         wholeName.append("/").append(cvParamType.getName());
                     }
                     detailsList.add(instrumentConfigurationType.getId()+": detector/t/" + wholeName.substring(1, wholeName.length()));
                 }
                 for (SourceComponentType sourceComponentType : instrumentConfigurationType.getComponentList().getSource()){
                     StringBuilder wholeName = new StringBuilder();
                     for (CVParamType cvParamType : sourceComponentType.getCvParam()){

                         wholeName.append("/").append(cvParamType.getName());
                     }
                     detailsList.add(instrumentConfigurationType.getId()+": source/t/" + wholeName.substring(1, wholeName.length()));
                 }
            }

            // processingMethod
            DataProcessingListType dataProcessingListType = mzMLType.getDataProcessingList();
            for (DataProcessingType dataProcessingType : dataProcessingListType.getDataProcessing()){
                for (ProcessingMethodType processingMethodType : dataProcessingType.getProcessingMethod()){
                    StringBuilder wholeName = new StringBuilder();
                    for (CVParamType cvParamType : processingMethodType.getCvParam()){
                        wholeName.append("/").append(cvParamType.getName());
                    }
                    detailsList.add(dataProcessingType.getId() + " :" +processingMethodType.getOrder()+"/t/"+wholeName.substring(1, wholeName.length()));
                }
            }

            // scan start time
            detailsList.add("Start Time Stamp/t/" + mzmlRunInfo.getRunStartTime());

        } catch (RunHeaderParsingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get details list
     * @return ArrayList<String>
     */
    public ArrayList<String> getDetailsList() {
        return detailsList;
    }
}

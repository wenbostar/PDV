package PDVGUI.fileimport;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.util.ArrayList;

/**
 * MzXML file information import
 * Created by Ken on 12/26/2017.
 */
public class MzXMLInfoImport {

    /**
     * mzXML file path
     */
    private String filePath;
    /**
     * Details list
     */
    private ArrayList<String> detailsList = new ArrayList<>();

    /**
     * Constructor
     * @param filePath mzXML file path
     */
    public MzXMLInfoImport(String filePath){
        this.filePath = filePath;
        parsingFile();
    }

    /**
     * Reading file get all details
     */
    private void parsingFile(){
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance(System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
            factory.setNamespaceAware(true);
            XmlPullParser xmlPullParser = factory.newPullParser();

            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath)));

            xmlPullParser.setInput(bufferedReader);

            int tagNum;
            while ((tagNum = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                String tagName = xmlPullParser.getName();
                //get spectrum file detail
                if (tagNum == XmlPullParser.START_TAG && tagName.equals("msRun")) {
                    StringBuilder time = new StringBuilder();
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);
                        if (name.equals("scanCount")){
                            detailsList.add("MS Num/t/"+xmlPullParser.getAttributeValue(i));
                        } else if (name.equals("startTime")){
                            time.append("Start:").append(xmlPullParser.getAttributeValue(i));
                        } else if (name.equals("endTime")){
                            time.append(" End:").append(xmlPullParser.getAttributeValue(i));
                        }
                    }
                    detailsList.add("RT/t/"+time);
                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("parentFile")) {
                    StringBuilder fileItem = new StringBuilder();
                    String fileValue = "";
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);
                        if (name.equals("fileName")){
                            fileValue = xmlPullParser.getAttributeValue(i);
                        } else if (name.equals("fileType")){
                            fileItem.append("File: ").append(xmlPullParser.getAttributeValue(i));
                        }
                    }
                    detailsList.add(fileItem+"/t/"+fileValue);
                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("msInstrument")) {

                    String id = "";
                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);

                        if (name.equals("msInstrumentID")){
                            id = xmlPullParser.getAttributeValue(i);
                        }
                    }

                    int xmlType;
                    while ((xmlType = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                        if (xmlType == XmlPullParser.END_TAG && xmlPullParser.getName() != null) {
                            if (xmlPullParser.getName().equals("msInstrument")) {
                                break;
                            }
                        }

                        if (xmlType == XmlPullParser.START_TAG) {
                            String tagName1 = xmlPullParser.getName();
                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("msManufacturer")) {
                                String value = "";

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("category")){
                                    } else if (name.equals("value")){
                                        value = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                detailsList.add(id + ": msManufacturer/t/"+value);
                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("msModel")) {
                                String value = "";

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("category")){
                                    } else if (name.equals("value")){
                                        value = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                detailsList.add(id + ": msModel/t/"+value);
                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("msIonisation")) {
                                String value = "";

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("category")){

                                    } else if (name.equals("value")){
                                        value = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                detailsList.add(id + ": msIonisation/t/"+value);

                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("msMassAnalyzer")) {
                                String value = "";

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("category")){
                                    } else if (name.equals("value")){
                                        value = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                detailsList.add(id + ": msMassAnalyzer/t/"+value);
                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("msDetector")) {
                                String value = "";

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("category")){

                                    } else if (name.equals("value")){
                                        value = xmlPullParser.getAttributeValue(i);
                                    }
                                }
                                detailsList.add(id + ": msDetector/t/"+value);
                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("software")) {
                                StringBuilder value = new StringBuilder();

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("type")){
                                        value.append(xmlPullParser.getAttributeValue(i)).append(": ");
                                    } else if (name.equals("name")){
                                        value.append(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("version")){
                                        value.append("(").append(xmlPullParser.getAttributeValue(i)).append(")");
                                    }
                                }
                                detailsList.add(id + ": Software/t/"+value);
                            }
                        }
                    }
                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("dataProcessing")) {

                    for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                        String name = xmlPullParser.getAttributeName(i);

                        if (name.equals("centroided")){
                            if (xmlPullParser.getAttributeValue(i).equals("1")){
                                detailsList.add("centroided/t/Yes");
                            } else {
                                detailsList.add("centroided/t/No");
                            }
                        }
                    }

                    int xmlType;
                    while ((xmlType = xmlPullParser.next()) != XmlPullParser.END_DOCUMENT) {

                        if (xmlType == XmlPullParser.END_TAG && xmlPullParser.getName() != null) {
                            if (xmlPullParser.getName().equals("dataProcessing")) {
                                break;
                            }
                        }

                        if (xmlType == XmlPullParser.START_TAG) {
                            String tagName1 = xmlPullParser.getName();

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("processingOperation")) {

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("name")){
                                        //System.out.println("The name is "+xmlPullParser.getAttributeValue(i));
                                    }
                                }
                            }

                            if (xmlType == XmlPullParser.START_TAG && tagName1.equals("software")) {
                                StringBuilder item = new StringBuilder();
                                StringBuilder value = new StringBuilder();

                                for (int i = 0; i < xmlPullParser.getAttributeCount(); i++) {
                                    String name = xmlPullParser.getAttributeName(i);

                                    if (name.equals("type")){
                                        item.append("dataProcessing: ").append(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("name")){
                                        value.append(xmlPullParser.getAttributeValue(i));
                                    } else if (name.equals("version")){
                                        value.append("(").append(xmlPullParser.getAttributeValue(i)).append(")");
                                    }
                                }
                                detailsList.add(item+"/t/"+value);
                            }
                        }
                    }
                }

                if (tagNum == XmlPullParser.START_TAG && tagName.equals("scan")){
                    break;
                }

            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get details list
     * @return ArrayList<String>
     */
    public ArrayList<String> getDetailsList(){
        return detailsList;
    }
}

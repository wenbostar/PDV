package PDVGUI.gui.utils.FileImport;

import PDVGUI.gui.PrideXMLDisplay;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import uk.ac.ebi.pride.jaxb.xml.PrideXmlReader;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

/**
 * Import pride XML file
 * Created by Ken on 9/14/2017.
 */
public class PrideXMLImportDialog {

    /**
     * Last selected folder
     */
    private String lastSelectedFolder;
    /**
     * Pride XML file
     */
    private File prideXMLFile;
    /**
     * Parent class
     */
    private PrideXMLDisplay prideXMLDisplay;
    /**
     * Object saving results
     */
    private PrideXmlReader prideXmlReader;

    /**
     * Constructor
     * @param prideXMLDisplay Parent class
     */
    public PrideXMLImportDialog(PrideXMLDisplay prideXMLDisplay){

        this.prideXMLDisplay = prideXMLDisplay;

        addFile();

        if(prideXMLFile != null){
            importFile();
        }
    }

    /**
     * Add new file
     */
    private void addFile(){

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select PRIDE XML File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return   myFile.getName().toLowerCase().endsWith("xml")
                        || myFile.getName().toLowerCase().endsWith("pridexml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " Pride XML(.xml)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(prideXMLDisplay, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (selectedFile != null) {
                lastSelectedFolder = selectedFile.getParent();
                prideXMLFile = selectedFile;
            }
        }
    }

    /**
     * Import new file
     */
    private void importFile(){

        ProgressDialogX progressDialog = new ProgressDialogX(prideXMLDisplay,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMass.png")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/SeaGullMassWait.png")),
                true);
        progressDialog.setTitle("Loading Data. Please Wait...");

        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);

        new Thread("ParseXMLProgress") {
            @Override
            public void run() {
                progressDialog.setVisible(true);
            }
        }.start();

        new Thread("ParseXML") {
            @Override
            public void run() {
                try {
                    prideXmlReader = new PrideXmlReader(prideXMLFile);

                    prideXMLDisplay.updateTree(prideXmlReader);

                    progressDialog.setRunFinished();
                } catch (Exception e){
                    e.printStackTrace();
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }
}

package PDVGUI.gui.utils.FileImport;

import PDVGUI.fileimport.MSOneImport;
import PDVGUI.gui.RawDataDisplay;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Import raw spectrum file
 * Created by Ken on 10/26/2017.
 */
public class RawDataImportDialog extends JDialog {

    /**
     * Parent class
     */
    private RawDataDisplay rawDataDisplay;
    /**
     * Spectrum file and it's type
     */
    private HashMap<File, String> spectrumFileToType = new HashMap<>();
    /**
     * Last selected folder
     */
    private String lastSelectedFolder;

    /**
     * Constructor
     * @param rawDataDisplay Parent class
     */
    public RawDataImportDialog(RawDataDisplay rawDataDisplay){

        this.rawDataDisplay = rawDataDisplay;

        addFile();

        if( !spectrumFileToType.isEmpty()){
            importFile();
        }
    }

    /**
     * Add new file
     */
    private void addFile(){

        JFileChooser fileChooser = new JFileChooser(lastSelectedFolder);
        fileChooser.setDialogTitle("Select Raw File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {

                return   myFile.getName().toLowerCase().endsWith("mzml")
                        || myFile.getName().toLowerCase().endsWith("mzxml")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return " mzml , mzxml";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showDialog(rawDataDisplay, "OK");

        if (returnValue == JFileChooser.APPROVE_OPTION) {

            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] selectedFiles = newFile.listFiles();
                    for (File file : selectedFiles) {
                        if(newFile.getName().toLowerCase().endsWith(".mzml")){

                            spectrumFileToType.put(file, "mzml");

                        }else {
                            spectrumFileToType.put(file, "mzxml");
                        }
                    }
                    lastSelectedFolder = newFile.getParent();
                } else {
                    if(newFile.getName().toLowerCase().endsWith(".mzml")){
                        spectrumFileToType.put(newFile, "mzml");
                    }else {
                        spectrumFileToType.put(newFile, "mzxml");
                    }
                    lastSelectedFolder = newFile.getParent();
                }
            }
        }
    }

    /**
     * Import file
     */
    private void importFile(){

        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ProgressDialogX progressDialog = new ProgressDialogX(rawDataDisplay,
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

                    for (File spectrumFile : spectrumFileToType.keySet() ){

                        ReadJob readJob = new ReadJob(spectrumFile, spectrumFileToType.get(spectrumFile), progressDialog);

                        threadPool.submit(readJob);
                    }

                } catch (Exception e){
                    e.printStackTrace();
                    threadPool.shutdown();
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Read file job
     */
    private class ReadJob implements Runnable{

        /**
         * Spectrum file
         */
        private File spectrumFile;
        /**
         * Spectrum file type
         */
        private String fileType;
        /**
         * Progress dialog
         */
        private ProgressDialogX progressDialog;

        /**
         * Read job
         * @param spectrumFile Spectrum file
         * @param fileType File type
         * @param progressDialog Progress dialog
         */
        public ReadJob(File spectrumFile, String fileType, ProgressDialogX progressDialog){
            this.spectrumFile = spectrumFile;
            this.fileType = fileType;
            this.progressDialog = progressDialog;
        }

        @Override
        public void run() {
            MSOneImport msOneImport = new MSOneImport(spectrumFile.getAbsolutePath(), fileType);
            rawDataDisplay.updateTree(spectrumFile.getName(), msOneImport.getKeyToRtAndInt(), msOneImport.getDetailsList());

            if (!progressDialog.isRunFinished()){
                progressDialog.setRunFinished();
            }
        }
    }
}

package PDVGUI.gui.utils.FileImport;

import PDVGUI.fileimport.MSOneImport;
import PDVGUI.gui.MSDataDisplay;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Import MS spectrum file
 * Created by Ken on 10/26/2017.
 */
public class MSDataImportDialog extends JDialog {

    /**
     * Parent class
     */
    private MSDataDisplay msDataDisplay;
    /**
     * Spectrum file and it's type
     */
    private HashMap<File, String> spectrumFileToType = new HashMap<>();
    /**
     * Last selected folder
     */
    private String lastSelectedFolder;
    /**
     * Names of the files that could not be read
     */
    private final List<String> failedFiles = Collections.synchronizedList(new ArrayList<>());

    /**
     * Constructor
     * @param msDataDisplay Parent class
     */
    public MSDataImportDialog(MSDataDisplay msDataDisplay, String lastSelectedFolder){

        this.msDataDisplay = msDataDisplay;
        this.lastSelectedFolder = lastSelectedFolder;

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
        fileChooser.setDialogTitle("Select Spectrum File(s)");
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

        int returnValue = fileChooser.showDialog(msDataDisplay, "OK");

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

        msDataDisplay.lastSelectedFolder = lastSelectedFolder;

        ExecutorService threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        ProgressDialogX progressDialog = new ProgressDialogX(msDataDisplay,
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

                        ReadJob readJob = new ReadJob(spectrumFile, spectrumFileToType.get(spectrumFile));

                        threadPool.submit(readJob);
                    }

                    // Wait for every file, otherwise the progress dialog is dismissed as soon as
                    // the first of several files is done. Shutting the pool down also releases its
                    // threads, which are not daemons.
                    threadPool.shutdown();
                    threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                } catch (Exception e){
                    e.printStackTrace();
                    threadPool.shutdownNow();

                } finally {
                    dismissProgressDialog(progressDialog);
                    reportFailedFiles();
                }
            }
        }.start();
    }

    /**
     * Dismiss the progress dialog. It is made visible from another thread, and disposing it before
     * it gets there does nothing at all, which would leave a modal dialog on screen that nothing
     * can ever close, so wait for it to come up first.
     * @param progressDialog Progress dialog
     */
    private void dismissProgressDialog(ProgressDialogX progressDialog){

        long deadline = System.currentTimeMillis() + 5000;

        while (!progressDialog.isVisible() && System.currentTimeMillis() < deadline){
            try {
                Thread.sleep(20);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }
        }

        progressDialog.setRunFinished();
    }

    /**
     * Tell the user which files could not be read. Called only once the progress dialog is gone:
     * it is modal, so a message dialog raised while it is still up cannot be dismissed.
     */
    private void reportFailedFiles(){

        List<String> failed;
        synchronized (failedFiles){
            failed = new ArrayList<>(failedFiles);
        }

        if (failed.isEmpty()){
            return;
        }

        StringBuilder message = new StringBuilder("Failed to parse:");
        for (String failedFile : failed){
            message.append("\n").append(failedFile);
        }
        message.append("\nPlease check your spectrum file(s)!");

        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(msDataDisplay, message.toString(),
                "File Error", JOptionPane.WARNING_MESSAGE));
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
         * Read job
         * @param spectrumFile Spectrum file
         * @param fileType File type
         */
        public ReadJob(File spectrumFile, String fileType){
            this.spectrumFile = spectrumFile;
            this.fileType = fileType;
        }

        @Override
        public void run() {
            try {
                MSOneImport msOneImport = new MSOneImport(spectrumFile.getAbsolutePath(), fileType);

                // updateTree() builds and swaps Swing components, so it has to run on the event
                // dispatch thread. Waiting for it also keeps the file counted as still loading
                // until its tree node is actually there.
                SwingUtilities.invokeAndWait(() -> msDataDisplay.updateTree(spectrumFile.getName(),
                        msOneImport.getKeyToRtAndInt(), msOneImport.getDetailsList(), msOneImport.getBiggestNum()));

            } catch (Exception | Error e){
                // The thread pool swallows anything thrown here, so a failed file would otherwise
                // disappear with no indication of what went wrong. Error is caught too because
                // running out of memory is the likeliest way a large file fails.
                e.printStackTrace();
                failedFiles.add(spectrumFile.getName());
            }
        }
    }
}

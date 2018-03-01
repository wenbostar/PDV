package PDVGUI.utils;

import PDVGUI.gui.utils.WaitingDialog;

import javax.swing.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Ken on 12/4/2017.
 */
public class ProteoQCCMD extends SwingWorker{

    private WaitingDialog waitingDialog;
    /**
     * The thread executor.
     */
    private ExecutorService threadExecutor = null;
    /**
     * Commands List
     */
    private ArrayList<String> procCommands;

    /**
     * Constructor
     * @param waitingDialog Waiting dialog
     * @param procCommands Commands List
     */
    public ProteoQCCMD(WaitingDialog waitingDialog, ArrayList<String> procCommands){

        this.waitingDialog = waitingDialog;
        this.procCommands = procCommands;
    }

    /**
     * Process commands and run
     * @return Integer
     */
    private int runQC(){
        System.out.println("The command is "+procCommands);

        waitingDialog.appendReport(String.valueOf(procCommands),true,true);
        threadExecutor = Executors.newFixedThreadPool(1);
        RunJob runJob = new RunJob(procCommands, waitingDialog);

        threadExecutor.submit(runJob);

        threadExecutor.shutdown();
        try {
            threadExecutor.awaitTermination(12, TimeUnit.HOURS);//The thread run here
        } catch (InterruptedException ex) {
            if (!waitingDialog.isRunCanceled()) {
                threadExecutor.shutdownNow();
            }
        }
        waitingDialog.increasePrimaryProgressCounter();
        waitingDialog.setRunFinished();

        return 0;
    }

    @Override
    protected Object doInBackground(){
        return runQC();
    }
}

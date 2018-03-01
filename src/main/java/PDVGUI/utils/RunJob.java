package PDVGUI.utils;

import PDVGUI.gui.utils.WaitingDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Ken on 12/4/2017.
 */
public class RunJob implements Runnable {

    /**
     * The ProcessBuilder object.
     */
    private ProcessBuilder procBuilder;
    /**
     * The Process object.
     */
    protected Process process;
    /**
     * Waiting handler displaying feedback to the user.
     */
    private WaitingDialog waitingDialog;

    /**
     * Constructor
     * @param procCommands Commands list
     * @param waitingDialog Waiting dialog
     */
    public RunJob (ArrayList<String> procCommands, WaitingDialog waitingDialog){
        this.waitingDialog = waitingDialog;
        procBuilder = new ProcessBuilder(procCommands);
        procBuilder.redirectErrorStream(true);
    }

    @Override
    public void run() {
        process = null;
        try {
            process = procBuilder.start();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            waitingDialog.appendReport("Could not start!", true, true);
            waitingDialog.appendReportEndLine();
            waitingDialog.setRunCanceled();
        }

        // Retrieve input stream from process.
        Scanner scan = new Scanner(process.getInputStream());
        scan.useDelimiter(System.getProperty("line.separator"));

        // get input from scanner and send to stdout
        while (scan.hasNextLine() && !waitingDialog.isRunCanceled()) {
            String temp = scan.nextLine();
            waitingDialog.appendReport(temp, false, true);
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cancel the process
     */
    public void cancel() {
        if (process != null) {
            process.destroy();
        }
    }
}

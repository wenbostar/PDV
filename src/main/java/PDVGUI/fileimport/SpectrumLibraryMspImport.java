package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.SpectrumLibDisplay;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SpectrumLibraryMspImport {

    /**
     * parent class
     */
    private SpectrumLibDisplay spectrumLibDisplay;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * msp file
     */
    private File mspFile;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Databse name
     */
    private String dbName;

    /**
     * Constructor
     * @param mspFile Spectrum library file path
     * @param spectrumLibDisplay Parent class
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public SpectrumLibraryMspImport(File mspFile, SpectrumLibDisplay spectrumLibDisplay, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.mspFile = mspFile;
        this.spectrumLibDisplay = spectrumLibDisplay;
        this.progressDialog = progressDialog;

        dbName = mspFile.getAbsolutePath() + ".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseFile();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse spectrum library file, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();

    }

    private void parseFile() throws IOException, SQLException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(mspFile));


        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);

        Statement statement = connection.createStatement();
        String matchTableQuery = "CREATE TABLE SpectrumMatch (LibID INT(8), MZ DOUBLE, Charge INT(2), NumPeaks INT(4), Match Object, Spectrum Object, Comment Object, PRIMARY KEY(LibID))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            System.out.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String readLine;

        String name = "";
        Integer charge;
        String mW;
        Double precursorMZ = 0.0;
        String comment = "";
        Integer numPeaks = 0;
        String[] peaks;
        PreparedStatement preparedStatement = null;

        MSnSpectrum currentSpectrum;
        SpectrumMatch currentSpectrumMatch;
        ArrayList<Double> mzList = new ArrayList<>();
        ArrayList<Double> intensityList = new ArrayList<>();
        ArrayList<String> libIDList = new ArrayList<>();

        Integer libID = 0;
        Integer countRound = 0;
        Integer countImport = 0;
        Integer peakCount = 1;

        while ((readLine=bufferedReader.readLine()) != null){


            if(countRound == 0){
                String addDataIntoTable = "INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?,?)";
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            if (readLine.split(": ")[0].equalsIgnoreCase("name")){
                name = readLine.split(": ")[1];
            } else if (readLine.split(": ")[0].equalsIgnoreCase("mw")){
                precursorMZ = Double.valueOf(readLine.split(": ")[1]);
            } else if (readLine.split(": ")[0].equalsIgnoreCase("comment")){
                comment = readLine.split(": ")[1];
            } else if (readLine.split(": ")[0].equalsIgnoreCase("num peaks")){
                numPeaks = Integer.valueOf(readLine.split(": ")[1]) + 1;
            } else {
                peaks = readLine.split("\t");
                mzList.add(Double.valueOf(peaks[0]));
                intensityList.add(Double.valueOf(peaks[1]));
                peakCount ++;

            }

            if (peakCount.equals(numPeaks)){
                libID ++;
                peakCount = 1;
                countRound ++;

                libIDList.add(String.valueOf(libID));
                currentSpectrum = getSpectrum(mzList, intensityList, name, libID, precursorMZ);
                currentSpectrumMatch = getSpectrumMatch(name, comment, precursorMZ, libID);

                charge = Integer.parseInt(name.split("/")[1]);

                preparedStatement.setInt(1, libID);
                preparedStatement.setDouble(2, precursorMZ);
                preparedStatement.setInt(3, charge);
                preparedStatement.setInt(4, numPeaks - 1);

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(bos);
                    try {
                        oos.writeObject(currentSpectrumMatch);
                    } finally {
                        oos.close();
                    }
                } finally {
                    bos.close();
                }
                ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(bos1);
                    try {
                        oos.writeObject(currentSpectrum);
                    } finally {
                        oos.close();
                    }
                } finally {
                    bos1.close();
                }

                preparedStatement.setBytes(5, bos.toByteArray());
                preparedStatement.setBytes(6, bos1.toByteArray());
                preparedStatement.setString(7, comment);

                preparedStatement.addBatch();

                if(countRound == 1000){
                    int[] counts = preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    spectrumLibDisplay.allLibIDList.add(libIDList);

                    if(countImport == 0){
                        spectrumLibDisplay.displayResults();
                        spectrumLibDisplay.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();
                    } else {
                        spectrumLibDisplay.pageNumJTextField.setText(spectrumLibDisplay.selectedPageNum + "/" + spectrumLibDisplay.allLibIDList.size());
                        spectrumLibDisplay.buttonCheck();
                    }

                    countRound = 0;
                    libIDList = new ArrayList<>();
                    countImport ++;
                }

                mzList = new ArrayList<>();
                intensityList = new ArrayList<>();
            }


        }bufferedReader.close();


        if(countRound != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            spectrumLibDisplay.allLibIDList.add(libIDList);

            if(countImport == 0){
                spectrumLibDisplay.displayResults();
                spectrumLibDisplay.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();
            } else {
                spectrumLibDisplay.pageNumJTextField.setText(String.valueOf(spectrumLibDisplay.selectedPageNum) + "/" + String.valueOf(spectrumLibDisplay.allLibIDList.size()));
                spectrumLibDisplay.buttonCheck();
            }
        }


    }

    /**
     * Return the spectrum
     * @param mzList mz list
     * @param intensityList intensity list
     * @param name Spectrum name
     * @param libID Library ID
     * @param precursorMZ Precursor mz
     * @return MSNSpectrum
     */
    private MSnSpectrum getSpectrum(ArrayList<Double> mzList, ArrayList<Double> intensityList, String name, Integer libID, Double precursorMZ){

        HashMap<Double, Peak> peakMap = new HashMap<>();

        for (int i = 0; i<mzList.size(); i++){

            peakMap.put(mzList.get(i), new Peak(mzList.get(i), intensityList.get(i)));
        }

        ArrayList<Charge> chargeArrayList = new ArrayList<>();
        chargeArrayList.add(new Charge(+1, Integer.valueOf(name.split("/")[1])));

        Precursor precursor = new Precursor(-1.0, precursorMZ, chargeArrayList);

        return new MSnSpectrum(2, precursor, String.valueOf(libID), peakMap, "");
    }

    /**
     * Return spectrum match
     * @param name Spectrum match name
     * @param comment Comment
     * @param precursorMZ Precursor MZ
     * @param libID Library ID
     * @return SpectrumMatch
     */
    private SpectrumMatch getSpectrumMatch(String name, String comment, Double precursorMZ, Integer libID){

        String[] commentArray = comment.split(" ");

        String seqeunce = name.split("/")[0].replaceAll("[^a-zA-Z]", "");
        Integer charge = Integer.valueOf(name.split("/")[1]);

        String modifications;

        ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

        for (String eachInfor : commentArray){
            if (eachInfor.contains("Mods")){
                if(!eachInfor.split("=")[1].equals("0")){
                    modifications = eachInfor.split("=")[1];

                    for (String eachOne : modifications.split("/")){
                        String[] modificationElements = eachOne.split(",");
                        if (modificationElements.length > 1){

                            if (!allModifications.contains(modificationElements[2]+" of "+modificationElements[1])){
                                allModifications.add(modificationElements[2]+" of "+modificationElements[1]);
                            }

                            utilitiesModifications.add(new ModificationMatch(modificationElements[2]+" of "+modificationElements[1], true,
                                    Integer.valueOf(modificationElements[0])+1));
                        }
                    }
                }

            }
        }

        Peptide peptide = new Peptide(seqeunce, utilitiesModifications);

        PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge), 0);

        SpectrumMatch spectrumMatch = new SpectrumMatch(String.valueOf(libID));

        spectrumMatch.addHit(0, peptideAssumption, false);

        spectrumMatch.setBestPeptideAssumption(peptideAssumption);

        return spectrumMatch;
    }

    /**
     * Return database name
     * @return String
     */
    public String getDbName(){
        return dbName;
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }

}

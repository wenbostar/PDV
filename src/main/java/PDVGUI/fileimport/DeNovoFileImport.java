package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.SpectrumIdentificationAssumption;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.identification_parameters.tool_specific.DirecTagParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.io.identifications.idfilereaders.DirecTagIdfileReader;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Import DeNovo soft results
 * Created by Ken on 11/28/2017.
 */
public class DeNovoFileImport {

    /**
     * Parent frame
     */
    private PDVMainClass pdvMainClass;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Result file
     */
    private File resultFile;
    /**
     * Spectrum file
     */
    private File spectrumFile;
    /**
     * Search parameters
     */
    private SearchParameters searchParameters;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Modification mass map
     */
    private HashMap<String,HashMap<Double, String >> modificationMassMap;
    /**
     * All modification name
     */
    private ArrayList<String> allModifications = new ArrayList<>();

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param resultFile Result file
     * @param spectrumFile Spectrum file
     * @param searchParameters Search parameter
     * @param modificationMassMap Modification mass map
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public DeNovoFileImport(PDVMainClass pdvMainClass, File resultFile, File spectrumFile, SearchParameters searchParameters,
                            HashMap<String,HashMap<Double, String >> modificationMassMap , ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.resultFile = resultFile;
        this.spectrumFile = spectrumFile;
        this.searchParameters = searchParameters;
        this.progressDialog = progressDialog;
        this.modificationMassMap = modificationMassMap;

        String dbName = resultFile.getParentFile().getAbsolutePath()+"/"+ resultFile.getName()+".db";

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(0);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    pareFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing file
     * @throws Exception
     */
    private void pareFile() throws Exception {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        String addTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, PRIMARY KEY(PSMIndex))";
        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch VALUES(?,?,?,?,?,?)";

        PreparedStatement preparedStatement = null;

        IdfileReader idfileReader = IdfileReaderFactory.getInstance().getFileReader(resultFile);

        HashMap<String, ArrayList<String>> softs = idfileReader.getSoftwareVersions();

        if (softs.keySet().contains("DirecTag")){
            DirecTagIdfileReader direcTagIdfileReader = (DirecTagIdfileReader) idfileReader;

            String dynamicMods = direcTagIdfileReader.getTagParameter("DynamicMods");

            String[] modItems = dynamicMods.split(" ");

            int countRe = 0;
            String residue = "";
            Double mass;
            HashMap<Double, String> massToName;

            ArrayList<String> modificationName = new ArrayList<>();

            for (String eachItem : modItems){

                countRe ++;

                if (countRe % 3 == 1){
                    residue = eachItem;
                } else if (countRe % 3 == 0){
                    mass = Double.valueOf(eachItem);

                    massToName = modificationMassMap.get(residue);

                    for (Double eachMass : massToName.keySet()){
                        if (Math.abs(eachMass - mass) < 0.0005){
                            if (!massToName.get(eachMass).contains("term")) {
                                modificationName.add(massToName.get(eachMass));
                            }
                        }
                    }
                }
            }

            DirecTagParameters direcTagParameters = new DirecTagParameters();

            direcTagParameters.setPtms(modificationName);

            searchParameters.setIdentificationAlgorithmParameter(Advocate.direcTag.getIndex(), direcTagParameters);
        }

        LinkedList<SpectrumMatch> spectrumMatches = idfileReader.getAllSpectrumMatches(null, searchParameters);

        if (!spectrumFile.getName().equals(spectrumMatches.get(0).getKey().split("_cus_")[0])){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                    "Result file can not match spectrum file."),
                    "File Error", JOptionPane.ERROR_MESSAGE);
        } else {

            Integer psmIndex = 0;
            Integer count = 0;
            Integer countRound = 0;

            SpectrumMatch newSpectrumMatch;

            ArrayList<String> spectrumList = new ArrayList<>();

            for (SpectrumMatch spectrumMatch : spectrumMatches) {

                if (count == 0) {
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }
                for (SpectrumIdentificationAssumption assumption : spectrumMatch.getAllAssumptions()) {

                    if (assumption instanceof PeptideAssumption) {

                        spectrumList.add(String.valueOf(psmIndex));

                        newSpectrumMatch = new SpectrumMatch();
                        PeptideAssumption peptideAssumption = (PeptideAssumption) assumption;

                        newSpectrumMatch.setSpectrumNumber(psmIndex);
                        newSpectrumMatch.setKey(spectrumMatch.getKey());
                        newSpectrumMatch.setBestPeptideAssumption(peptideAssumption);

                        for (ModificationMatch modificationMatch : peptideAssumption.getPeptide().getModificationMatches()) {

                            if (!allModifications.contains(modificationMatch.getTheoreticPtm())) {
                                allModifications.add(modificationMatch.getTheoreticPtm());
                            }
                        }

                        preparedStatement.setInt(1, psmIndex);
                        preparedStatement.setDouble(2, 0.0);
                        preparedStatement.setString(3, spectrumMatch.getKey().split("_cus_")[1]);
                        preparedStatement.setString(4, peptideAssumption.getPeptide().getSequence());
                        preparedStatement.setDouble(5, 0.0);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            try {
                                oos.writeObject(newSpectrumMatch);
                            } finally {
                                oos.close();
                            }
                        } finally {
                            bos.close();
                        }
                        preparedStatement.setBytes(6, bos.toByteArray());

                        preparedStatement.addBatch();

                        count++;

                        psmIndex++;
                    } else if (assumption instanceof TagAssumption) {

                        spectrumList.add(String.valueOf(psmIndex));

                        newSpectrumMatch = new SpectrumMatch();
                        TagAssumption tagAssumption = (TagAssumption) assumption;

                        newSpectrumMatch.setSpectrumNumber(psmIndex);
                        newSpectrumMatch.setKey(spectrumMatch.getKey());
                        newSpectrumMatch.setBestTagAssumption(tagAssumption);

                        preparedStatement.setInt(1, psmIndex);
                        preparedStatement.setDouble(2, 0.0);
                        preparedStatement.setString(3, spectrumMatch.getKey().split("_cus_")[1]);
                        preparedStatement.setString(4, tagAssumption.getTag().asSequence());
                        preparedStatement.setDouble(5, 0.0);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            ObjectOutputStream oos = new ObjectOutputStream(bos);
                            try {
                                oos.writeObject(newSpectrumMatch);
                            } finally {
                                oos.close();
                            }
                        } finally {
                            bos.close();
                        }
                        preparedStatement.setBytes(6, bos.toByteArray());

                        preparedStatement.addBatch();

                        count++;
                        psmIndex++;
                    }
                }

                if (count >= 1000) {

                    int[] counts = preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(spectrumList);

                    count = 0;

                    if (countRound == 0) {

                        pdvMainClass.displayDenovo();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                        countRound++;
                    }

                    spectrumList = new ArrayList<>();
                }
            }

            if (count != 0) {

                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumList);

                if (countRound == 0) {

                    pdvMainClass.displayDenovo();
                    pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                    progressDialog.setRunFinished();

                } else {

                    pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));

                }
            } else {
                if (countRound == 0) {
                    progressDialog.setRunFinished();

                    JOptionPane.showMessageDialog(pdvMainClass, JOptionEditorPane.getJOptionEditorPane(
                            "No PSM found in file, please check your file."),
                            "File Error", JOptionPane.ERROR_MESSAGE);
                    throw new IOException("No PSM found");
                }
            }
            pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
            pdvMainClass.loadingJButton.setText("Import done");
            pdvMainClass.searchButton.setToolTipText("Find items");
            pdvMainClass.searchItemTextField.setToolTipText("Find items");

        }
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

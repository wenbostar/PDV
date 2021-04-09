package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.mascotdatfile.util.interfaces.MascotDatfileInf;
import com.compomics.mascotdatfile.util.interfaces.Modification;
import com.compomics.mascotdatfile.util.interfaces.QueryToPeptideMapInf;
import com.compomics.mascotdatfile.util.mascot.PeptideHit;
import com.compomics.mascotdatfile.util.mascot.ProteinHit;
import com.compomics.mascotdatfile.util.mascot.Query;
import com.compomics.mascotdatfile.util.mascot.enumeration.MascotDatfileType;
import com.compomics.mascotdatfile.util.mascot.factory.MascotDatfileFactory;
import com.compomics.util.experiment.biology.NeutralLoss;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
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
import java.util.Vector;

public class DATFileImport {

    /**
     * Mascot dat file
     */
    private File datFile;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Progress dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Database name
     */
    private String dbName;
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Modification ID an name map
     */
    private HashMap<Integer, String> modIDToModNam = new HashMap<>();
    /**
     * Fixed modification AA and name
     */
    private HashMap<String, String> fixedModAAAndName = new HashMap<>();
    /**
     * PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;

    /**
     * Main constructor
     * @param datFile dat file
     * @param pdvMainClass Parent class
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public DATFileImport(File datFile, PDVMainClass pdvMainClass, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {

        this.datFile = datFile;
        this.pdvMainClass = pdvMainClass;
        this.progressDialog = progressDialog;
        this.dbName = datFile.getAbsolutePath() + ".db";

        File dbFile = new File(dbName);
        File dbJournalFile = new File(dbName + "-journal");
        if (dbFile.isFile() && dbFile.exists()) {
            dbFile.delete();
        }
        if (dbJournalFile.isFile() && dbJournalFile.exists()) {
            dbJournalFile.delete();
        }

        sqLiteConnection = new SQLiteConnection(dbName);
        sqLiteConnection.setScoreNum(6);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseFile();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse dat file, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parse file
     * @throws SQLException
     * @throws IOException
     */
    private void parseFile() throws SQLException, IOException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);

        Statement statement = connection.createStatement();
        String matchTableQuery = "CREATE TABLE SpectrumMatch (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object, " +
                "Spectrum Object, EValue Double, IdentityThreshold Double, IonScore Double, CalcPeptideMass Double, " +
                 "ModSeq Char, ProteinID Char, " + "PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(matchTableQuery);
        }catch (SQLException e){
            System.out.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        PreparedStatement preparedStatement = null;

        MascotDatfileInf iMascotDatfile = null;
        iMascotDatfile = MascotDatfileFactory.create(datFile.getAbsolutePath(), MascotDatfileType.INDEX);

        for (Object object : iMascotDatfile.getModificationList().getVariableModifications()) {
            Modification modification = (Modification) object;

            addModification(modification, true);

        }

        for (Object object : iMascotDatfile.getModificationList().getFixedModifications()) {
            Modification modification = (Modification) object;

            addModification(modification, false);
        }

        QueryToPeptideMapInf lQueryToPeptideMap = iMascotDatfile.getQueryToPeptideMap();

        Vector lQueryList = iMascotDatfile.getQueryList();

        Query query;
        Integer queryNum;
        Integer lineCount = 0;
        Integer count = 0;
        Integer countRound = 0;
        String peptideSeq;
        String modSeq;
        StringBuilder proteinDetail;
        PeptideHit peptideHit;
        Peptide peptide;
        PeptideAssumption peptideAssumption;
        SpectrumMatch spectrumMatch;
        ArrayList<int[]> indexAndIDList;
        ArrayList<String> spectrumList = new ArrayList<>();

        for (int i = 0; i < lQueryList.size(); i ++){
            query = (Query) lQueryList.get(i);

            queryNum = query.getQueryNumber();

            peptideHit = lQueryToPeptideMap.getPeptideHitOfOneQuery(queryNum, 1);

            if (peptideHit == null){
                continue;
            }

            ArrayList<ModificationMatch> utilitiesModifications = new ArrayList<>();

            if (count == 0){
                preparedStatement = connection.prepareStatement(addDataIntoTable);
            }

            int[] modsArray = peptideHit.getVariableModificationsArray();

            peptideSeq = peptideHit.getSequence();
            modSeq = peptideHit.getModifiedSequence();

            indexAndIDList = getIndexAndID(modsArray);

            if (indexAndIDList.size() != 0){
                for (int[] eachModIndexAndID : indexAndIDList){
                    int modIndex = eachModIndexAndID[0];

                    String modName;
                    if (modIDToModNam.containsKey(eachModIndexAndID[1])){

                        if (modIndex == 0){
                            modName = modIDToModNam.get(eachModIndexAndID[1]) + " of N-term";
                        } else if (modIndex == peptideSeq.length() + 1){
                            modName = modIDToModNam.get(eachModIndexAndID[1]) + " of C-term";
                        } else {
                            String modAA = peptideSeq.substring(modIndex - 1, modIndex);
                            modName = modIDToModNam.get(eachModIndexAndID[1]) + " of " + modAA;
                        }
                    } else {
                        progressDialog.setRunFinished();
                        JOptionPane.showMessageDialog(
                                null, "Failed to parse dat file, please check your file.",
                                "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                        break;
                    }

                    if (modIndex == 0){
                        utilitiesModifications.add(new ModificationMatch(modName, true, 1));
                    } else if (peptideSeq.length() + 1 == modIndex){
                        utilitiesModifications.add(new ModificationMatch(modName, true, peptideSeq.length()));
                    } else {
                        utilitiesModifications.add(new ModificationMatch(modName, true, modIndex));
                    }
                }
            }

            for (String aa : fixedModAAAndName.keySet()){
                String modName = fixedModAAAndName.get(aa);

                if (aa.equals("N-term") | aa.equals("N_term")){
                    utilitiesModifications.add(new ModificationMatch(modName, true, 1));
                } else if (aa.equals("C-term") | aa.equals("C_term")){
                    utilitiesModifications.add(new ModificationMatch(modName, true, peptideSeq.length()));
                } else {
                    for (int modIndex = 0; modIndex < peptideSeq.length(); modIndex ++){
                        String modAA = String.valueOf(peptideSeq.charAt(modIndex));
                        if (modAA.equals(aa)){
                            utilitiesModifications.add(new ModificationMatch(modName, true, modIndex + 1));
                        }
                    }
                }
            }

            peptide = new Peptide(peptideSeq, utilitiesModifications);

            int charge = Integer.parseInt(query.getChargeString().split("\\+")[0]);

            peptideAssumption = new PeptideAssumption(peptide, 1, 0, new Charge(+1, charge), peptideHit.getDeltaMass(), "*");

            spectrumMatch = new SpectrumMatch(query.getTitle());

            spectrumMatch.addHit(0, peptideAssumption, false);

            spectrumMatch.setBestPeptideAssumption(peptideAssumption);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                try {
                    oos.writeObject(spectrumMatch);
                } finally {
                    oos.close();
                }
            } finally {
                bos.close();
            }

            MSnSpectrum currentSpectrum = getSpectrum(query);

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

            proteinDetail = new StringBuilder();
            ArrayList<ProteinHit> proteinHitList = peptideHit.getProteinHits();
            for (Object object : proteinHitList){
                ProteinHit proteinHit = (ProteinHit) object;

                proteinDetail.append(proteinHit.getAccession());
                proteinDetail.append(":");
                proteinDetail.append(proteinHit.getStart());
                proteinDetail.append("-");
                proteinDetail.append(proteinHit.getStop());
                proteinDetail.append(";");

            }

            spectrumList.add(String.valueOf(lineCount));

            preparedStatement.setInt(1, lineCount);
            preparedStatement.setDouble(2, query.getPrecursorMZ());
            preparedStatement.setString(3, query.getTitle());
            preparedStatement.setString(4, peptideSeq);
            preparedStatement.setDouble(5, peptideHit.getDeltaMass());
            preparedStatement.setBytes(6, bos.toByteArray());
            preparedStatement.setBytes(7, bos1.toByteArray());
            preparedStatement.setDouble(8, peptideHit.getExpectancy());
            preparedStatement.setDouble(9, peptideHit.calculateIdentityThreshold());
            preparedStatement.setDouble(10, peptideHit.getIonsScore());
            preparedStatement.setDouble(11, peptideHit.getPeptideMr());
            preparedStatement.setString(12, modSeq);
            preparedStatement.setString(13, String.valueOf(proteinDetail));

            preparedStatement.addBatch();

            count ++;

            if(count == 1000){
                int[] counts = preparedStatement.executeBatch();
                connection.commit();
                preparedStatement.close();

                pdvMainClass.allSpectrumIndex.add(spectrumList);
                pdvMainClass.updatePTMSetting();

                count = 0;

                if(countRound == 0){
                    pdvMainClass.displayFrage();
                    pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                    progressDialog.setRunFinished();

                    countRound ++;
                } else {
                    pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                    countRound ++;
                }

                spectrumList = new ArrayList<>();
            }
            lineCount ++;

        }

        if(count != 0){
            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.allSpectrumIndex.add(spectrumList);
            pdvMainClass.updatePTMSetting();

            if(countRound == 0){
                pdvMainClass.displayFrage();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();
            } else {
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }
        }

        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");

    }

    /**
     * Add modification
     * @param modification Modification from dat file
     * @param isVariable Is variable modification or not
     */
    private void addModification(Modification modification, Boolean isVariable){

        String location = modification.getLocation();
        String singleModName = modification.getType();
        String shortMod = modification.getShortType();
        int modID = modification.getModificationID();
        Double singleMass = modification.getMass();
        ArrayList<String> residues = new ArrayList<>();

        if (location.contains("N-term") | location.contains("N_term")){

            residues.add(location);
            String modName = singleModName + " of N-term";
            PTM ptm = new PTM(PTM.MODNPAA, modName, singleMass, residues);
            ptm.setShortName(singleModName);
            ptmFactory.addUserPTM(ptm);

            if (!allModifications.contains(modName)) {
                allModifications.add(modName);
            }
            if (!isVariable){
                fixedModAAAndName.put(location, modName);
            }

        } else if (location.contains("C-term") | location.contains("C_term")){

            residues.add(location);
            String modName = singleModName + " of C-term";
            PTM ptm = new PTM(PTM.MODCP, modName, singleMass, residues);
            ptm.setShortName(singleModName);
            ptmFactory.addUserPTM(ptm);

            if (!allModifications.contains(modName)) {
                allModifications.add(modName);
            }
            if (!isVariable){
                fixedModAAAndName.put(location, modName);
            }

        } else {
            for (String singleAA : location.split("")){
                residues.add(singleAA);
                String modName = singleModName + "of "+ singleAA;
                PTM ptm = new PTM(PTM.MODAA, modName, singleMass, residues);
                ptm.setShortName(shortMod);
                if (singleAA.equals("T") || singleAA.equals("S")){
                    if (singleMass < 80.01 && singleMass > 79.9){
                        ptm.addNeutralLoss(NeutralLoss.H3PO4);
                    }
                }
                ptmFactory.addUserPTM(ptm);

                if (!allModifications.contains(modName)) {
                    allModifications.add(modName);
                }
                if (!isVariable){
                    fixedModAAAndName.put(singleAA, modName);
                }
            }
        }

        if (isVariable){
            modIDToModNam.put(modID, singleModName);
        }
    }

    /**
     * Get modification index and ID
     * @param modsArray Modification array
     * @return ArrayList
     */
    private ArrayList<int[]> getIndexAndID(int[] modsArray){

        int index;
        int ID;
        int[] indexAndID;
        ArrayList<int[]> indexAndIDList = new ArrayList<>();

        for (int i = 0; i < modsArray.length; i ++){
            indexAndID = new int[2];
            if (modsArray[i] != 0){
                indexAndID[0] = i;
                indexAndID[1] = modsArray[i];
                indexAndIDList.add(indexAndID);
            }
        }

        return indexAndIDList;
    }

    /**
     * Get spectrum in MSNSpectrum type
     * @param query Query
     * @return MSNSpectrum
     */
    private MSnSpectrum getSpectrum(Query query){

        double precursorMZ = query.getPrecursorMZ();

        double precursorInt = query.getPrecursorIntensity();

        Charge charge = new Charge(1, Integer.valueOf(query.getChargeString().split("\\+")[0]));
        ArrayList<Charge> charges = new ArrayList<>();
        charges.add(charge);

        Double rt;

        if (query.getRetentionTimeInSeconds() != null){
            rt = Double.valueOf(query.getRetentionTimeInSeconds().split("-")[0]);
        } else {
            rt = 0.0;
        }

        Precursor precursor = new Precursor(rt, precursorMZ, precursorInt, charges);

        double[] mzValues = query.getMZArray();
        double[] intensityValues = query.getIntensityArray();
        HashMap<Double, Peak> peakMap = new HashMap<>();
        for(int i = 0; i<mzValues.length; i++){
            Peak peak = new Peak(mzValues[i], intensityValues[i]);
            peakMap.put(mzValues[i], peak);
        }

        return new MSnSpectrum(2, precursor, String.valueOf(query.getQueryNumber()), peakMap, query.getFilename());
    }

    /**
     * Return SQLiteConnection
     * @return SQLiteConnection
     */
    public SQLiteConnection getSqLiteConnection(){
        return sqLiteConnection;
    }

    /**
     * Return additional parameters
     * @return ArrayList
     */
    public ArrayList<String> getScoreName(){

        ArrayList<String> scoreName = new ArrayList<>();

        scoreName.add("EValue");
        scoreName.add("IdentityThreshold");
        scoreName.add("IonScore");
        scoreName.add("CalcPeptideMass");
        scoreName.add("ModSeq");
        scoreName.add("ProteinID");

        return scoreName;
    }

    /**
     * Return all modification
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }
}

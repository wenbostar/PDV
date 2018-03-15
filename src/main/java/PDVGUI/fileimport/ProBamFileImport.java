package PDVGUI.fileimport;

import PDVGUI.DB.SQLiteConnection;
import PDVGUI.gui.PDVMainClass;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import htsjdk.samtools.*;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Import proBAM file
 * Created by Ken on 10/23/2017.
 */
public class ProBamFileImport {

    /**
     * Parent class
     */
    private PDVMainClass pdvMainClass;
    /**
     * ProBAM file
     */
    private File proBAMFile;
    /**
     * Progress Dialog
     */
    private ProgressDialogX progressDialog;
    /**
     * Database connection
     */
    private SQLiteConnection sqLiteConnection;
    /**
     * Spectrum file name
     */
    private String spectrumFileName;
    /**
     * Spectrum factory
     */
    private SpectrumFactory spectrumFactory;
    /**
     * Extral parameters
     */
    private ArrayList<String> scoreName = new ArrayList<>();
    /**
     * All modifications
     */
    private ArrayList<String> allModifications = new ArrayList<>();
    /**
     * Spectrum file type
     */
    private String spectrumFileType;

    /**
     * Constructor
     * @param pdvMainClass Parent class
     * @param proBAMFile ProBAM file
     * @param spectrumFileName Spectrum file name
     * @param spectrumFactory Spectrum factory
     * @param progressDialog Progress dialog
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public ProBamFileImport(PDVMainClass pdvMainClass, File proBAMFile, String spectrumFileName, String spectrumFileType, SpectrumFactory spectrumFactory, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.proBAMFile = proBAMFile;
        this.spectrumFactory = spectrumFactory;
        this.progressDialog = progressDialog;
        this.spectrumFileType = spectrumFileType;
        this.spectrumFileName = spectrumFileName;

        scoreName.add("Score");
        scoreName.add("FDR");
        scoreName.add("isAnnotated");
        scoreName.add("followingAA");
        scoreName.add("precedingAA");
        scoreName.add("enzyme");
        scoreName.add("peptideType");
        scoreName.add("geneMapNum");
        scoreName.add("peptideInt");
        scoreName.add("peptideMapNum");
        scoreName.add("missedCNum");
        scoreName.add("uniqueType");
        scoreName.add("originalSeq");
        scoreName.add("enzymeInSearch");
        scoreName.add("URI");

        String dbName = proBAMFile.getParentFile().getAbsolutePath()+"/"+ proBAMFile.getName()+".db";

        sqLiteConnection = new SQLiteConnection(dbName);

        sqLiteConnection.setScoreNum(15);

        new Thread("DisplayThread") {
            @Override
            public void run() {
                try {
                    parseProBAM();

                    pdvMainClass.searchButton.setEnabled(true);
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(
                            null, "Failed to parse proBAM file, please check your file.",
                            "Error Parsing File", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Parsing proBAM file
     * @throws SQLException
     * @throws IOException
     * @throws MzMLUnmarshallerException
     */
    private void parseProBAM() throws SQLException, IOException {

        Connection connection = sqLiteConnection.getConnection();

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();

        StringBuilder addQuery = new StringBuilder();
        StringBuilder addValuesQuery = new StringBuilder("VALUES(?,?,?,?,?,?");

        for (String name : scoreName){
            addQuery.append(", ").append(name).append(" OBJECT(50)");
            addValuesQuery.append(",?");
        }
        addValuesQuery.append(")");

        String addTableQuery = "CREATE TABLE SpectrumMatch" + " (PSMIndex INT(10), MZ DOUBLE, Title Char, Sequence Char, MassError DOUBLE, Match Object" + addQuery +", PRIMARY KEY(PSMIndex))";

        try {
            statement.execute(addTableQuery);
        }catch (SQLException e){
            progressDialog.setRunFinished();
            JOptionPane.showMessageDialog(
                    null, "Failed to connect database.",
                    "Error Connecting to DB", JOptionPane.ERROR_MESSAGE);
            System.err.println("An error occurred while creating table SpectrumMatch");
            throw (e);
        }finally {
            statement.close();
        }

        String addDataIntoTable = "INSERT INTO SpectrumMatch "+addValuesQuery;
        PreparedStatement preparedStatement = null;

        PTMFactory ptmFactory = PTMFactory.getInstance();
        HashMap<String, String> accessionToModification = new HashMap<>();
        for(String modificationName: ptmFactory.getPTMs()){
            PTM ptm = ptmFactory.getPTM(modificationName);
            if(ptm.getCvTerm()!= null){
                accessionToModification.put(ptm.getCvTerm().getAccession(), ptm.getCvTerm().getName());
            }
        }

        String fileName = "";
        int dot = spectrumFileName.lastIndexOf('.');
        if ((dot >-1) && (dot < (spectrumFileName.length()))) {
            fileName = spectrumFileName.substring(0, dot);
        }

        SamReader samFileReader;

        final SamReaderFactory srf = htsjdk.samtools.SamReaderFactory.makeDefault().validationStringency(htsjdk.samtools.ValidationStringency.LENIENT);

        samFileReader = srf.open(SamInputResource.of(proBAMFile));
        String version = samFileReader.getFileHeader().getVersion();

        SAMRecordIterator iter = samFileReader.iterator();
        ArrayList<ModificationMatch> modificationMatches;
        HashMap<String, String> locationToModificationName = new HashMap<>();
        ArrayList<String> spectrumList = new ArrayList<>();

        int count = 0;
        int countRound = 0;
        int countAll = 0;

        if (version.equals("1.0")) {

            HashMap<String, Double> locationToMass = new HashMap<>();
            HashMap<String, HashMap<Double, String>> modificationMassMap = getModificationMass();

            while (iter.hasNext()) {

                Integer spectrumIndex = null;
                Peptide peptide = null;
                Peptide referencePeptide = null;

                String peptideSequence;
                int peptideCharge = 0;
                String referenceSequence;
                double score = 0.0;
                double fdr = 0.0;
                double massError = 0.0;
                String spectrumTitle;
                String isAnnotated = "*";
                String followingAA = "*";
                String precedingAA = "*";
                String enzyme = "*";
                String peptideType = "*";
                Integer gengMapNum = -1;
                Double peptideInt = 0.0;
                Integer peptideMapNum = -1;
                Integer missedCNum = -1;
                String uniqueType = "*";
                String originalSeq = "*";
                String enzymeInSearch = "*";
                String URI = "*";

                SAMRecord rec = iter.next();
                List<SAMRecord.SAMTagAndValue> list = rec.getAttributes();

                String spectrumName = rec.getReadName();

                try {

                    if (spectrumName.contains("index")) {

                        if (spectrumName.contains(":")) {
                            spectrumIndex = Integer.valueOf(spectrumName.split(":")[1].split("=")[1]);
                        } else {
                            spectrumIndex = Integer.valueOf(spectrumName.split("=")[1]);
                        }

                    } else if (spectrumName.contains(fileName)) {

                        int dot1 = spectrumName.lastIndexOf('.');
                        if ((dot1 > -1) && (dot1 < (spectrumName.length()))) {
                            spectrumIndex = Integer.valueOf(spectrumName.substring(dot1 + 1));
                        }

                    } else {
                        JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file", "File Error", JOptionPane.WARNING_MESSAGE);
                        progressDialog.setRunFinished();
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file\n", "File Error", JOptionPane.WARNING_MESSAGE);
                    progressDialog.setRunFinished();
                    break;
                }


                SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex)));

                if (spectrumFileType.equals("mgf")) {
                    try {
                        spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, spectrumIndex + 1);
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                                null, "The spectrum title cannot match it in proBAM",
                                "Error Matching", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                } else {
                    spectrumTitle = String.valueOf(spectrumIndex);
                }

                currentMatch.setSpectrumNumber(spectrumIndex);

                for (SAMRecord.SAMTagAndValue samTagAndValue : list) {
                    String modificationName = null;

                    if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        modificationMatches = new ArrayList<>();
                        peptideSequence = (String) samTagAndValue.value;
                        for (String location : locationToMass.keySet()) {
                            if (location.equals("0") || location.equals("-2147483648")) {
                                String aA = peptideSequence.split("")[0];
                                Double modificationMass = locationToMass.get(location);

                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }

                                if (modificationName == null) {
                                    for (Double mass : modificationMassMap.get("N-terminus").keySet()) {
                                        if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                            modificationName = modificationMassMap.get("N-terminus").get(mass);
                                        }
                                    }
                                }

                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }

                                modificationMatches.add(new ModificationMatch(modificationName, true, 1));
                                modificationName = null;
                            } else if (location.equals(String.valueOf(peptideSequence.length() + 1))) {
                                String aA = peptideSequence.split("")[peptideSequence.length() - 1];
                                Double modificationMass = locationToMass.get(location);

                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }

                                if (modificationName == null) {
                                    for (Double mass : modificationMassMap.get("C-terminus").keySet()) {
                                        if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                            modificationName = modificationMassMap.get("C-terminus").get(mass);
                                        }
                                    }
                                }

                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }

                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                                modificationName = null;
                            } else {
                                String aA = peptideSequence.split("")[Integer.parseInt(location)];
                                Double modificationMass = locationToMass.get(location);
                                for (Double mass : modificationMassMap.get(aA).keySet()) {
                                    if (Math.abs(mass - modificationMass) < 0.005) {//Mass error may cause problem
                                        modificationName = modificationMassMap.get(aA).get(mass);
                                    }
                                }

                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }

                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location) + 1));
                                modificationName = null;
                            }
                        }
                        peptide = new Peptide(peptideSequence, modificationMatches);
                        locationToMass = new HashMap<>();

                    } else if (samTagAndValue.tag.equals("XA") && !samTagAndValue.value.equals("*")) {
                        if (samTagAndValue.value.equals("0")) {
                            isAnnotated = "yes";
                        } else if (samTagAndValue.value.equals("1")) {
                            isAnnotated = "partially unknown";
                        } else if (samTagAndValue.value.equals("2")) {
                            isAnnotated = "totally unknown";
                        }
                    } else if (samTagAndValue.tag.equals("YA") && !samTagAndValue.value.equals("*")) {
                        followingAA = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("YB") && !samTagAndValue.value.equals("*")) {
                        precedingAA = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XE") && !samTagAndValue.value.equals("*")) {
                        enzyme = getEnzyme("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("XG") && !samTagAndValue.value.equals("*")) {
                        peptideType = getPeptideType("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("NH") && !samTagAndValue.value.equals("*")) {
                        gengMapNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XI") && !samTagAndValue.value.equals("*")) {
                        peptideInt = (double) (float) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XL") && !samTagAndValue.value.equals("*")) {
                        peptideMapNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XN") && !samTagAndValue.value.equals("*")) {
                        missedCNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XO") && !samTagAndValue.value.equals("*")) {
                        uniqueType = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        originalSeq = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XT") && !samTagAndValue.value.equals("*")) {
                        enzymeInSearch = getEnzymeInSear("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("XU") && !samTagAndValue.value.equals("*")) {
                        URI = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XM") && !samTagAndValue.value.equals("*") && !samTagAndValue.value.equals("-")) {
                        String modificationAccessionList = (String) samTagAndValue.value;
                        String[] allModifications = modificationAccessionList.split(",");
                        for (String eachModification : allModifications) {
                            String[] locationAndName = eachModification.split("@");
                            locationToMass.put(locationAndName[1], Double.valueOf(locationAndName[0]));

                        }
                    } else if (samTagAndValue.tag.equals("XC") && !samTagAndValue.value.equals("*")) {
                        peptideCharge = (int) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XR") && !samTagAndValue.value.equals("*")) {
                        referenceSequence = (String) samTagAndValue.value;
                        referencePeptide = new Peptide(referenceSequence, null);
                    } else if (samTagAndValue.tag.equals("XS") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        score = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XB") && !samTagAndValue.value.equals("*")) {
                        massError = (double) (float) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XQ") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        fdr = bigDecimal.doubleValue();
                    }
                }

                if (peptide != null) {

                    spectrumList.add(String.valueOf(countAll));

                    if (count == 0) {
                        preparedStatement = connection.prepareStatement(addDataIntoTable);
                    }

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, peptideCharge), massError, null);

                    if (referencePeptide != null) {
                        PeptideAssumption peptideAssumptionR = new PeptideAssumption(referencePeptide, 2, 1, new Charge(1, peptideCharge), 0, null);

                        currentMatch.addHit(1, peptideAssumptionR, false);
                    }

                    peptideAssumption.setRawScore(fdr);

                    currentMatch.addHit(1, peptideAssumption, false);

                    currentMatch.setBestPeptideAssumption(peptideAssumption);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        try {
                            oos.writeObject(currentMatch);
                        } finally {
                            oos.close();
                        }
                    } finally {
                        bos.close();
                    }

                    preparedStatement.setInt(1, countAll);
                    preparedStatement.setDouble(2, peptideAssumption.getTheoreticMass() / peptideCharge);
                    preparedStatement.setString(3, spectrumTitle);
                    preparedStatement.setString(4, peptide.getSequence());
                    preparedStatement.setDouble(5, Math.abs(massError));
                    preparedStatement.setBytes(6, bos.toByteArray());
                    preparedStatement.setDouble(7, score);
                    preparedStatement.setDouble(8, fdr);
                    preparedStatement.setString(9, isAnnotated);
                    preparedStatement.setString(10, followingAA);
                    preparedStatement.setString(11, precedingAA);
                    preparedStatement.setString(12, enzyme);
                    preparedStatement.setString(13, peptideType);
                    preparedStatement.setInt(14, gengMapNum);
                    preparedStatement.setDouble(15, peptideInt);
                    preparedStatement.setInt(16, peptideMapNum);
                    preparedStatement.setInt(17, missedCNum);
                    preparedStatement.setString(18, uniqueType);
                    preparedStatement.setString(19, originalSeq);
                    preparedStatement.setString(20, enzymeInSearch);
                    preparedStatement.setString(21, URI);

                    preparedStatement.addBatch();

                    count++;
                    countAll++;

                    if (count == 1000) {

                        int[] counts = preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        pdvMainClass.allSpectrumIndex.add(spectrumList);

                        count = 0;

                        if (countRound == 0) {
                            System.out.println("The count round is " + countRound);

                            pdvMainClass.displayResult();
                            pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                            progressDialog.setRunFinished();

                            countRound++;

                        } else {
                            pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                            countRound++;
                        }
                        pdvMainClass.buttonCheck();

                        spectrumList = new ArrayList<>();

                    }
                }
            }
        } else {
            while (iter.hasNext()) {

                Integer spectrumIndex = null;
                Peptide peptide = null;
                Peptide referencePeptide = null;

                String peptideSequence;
                int peptideCharge = 0;
                String referenceSequence;
                double score = 0.0;
                double fdr = 0.0;
                double massError = 0.0;
                String spectrumTitle;
                String isAnnotated = "*";
                String followingAA = "*";
                String precedingAA = "*";
                String enzyme = "*";
                String peptideType = "*";
                Integer gengMapNum = -1;
                Double peptideInt = 0.0;
                Integer peptideMapNum = -1;
                Integer missedCNum = -1;
                String uniqueType = "*";
                String originalSeq = "*";
                String enzymeInSearch = "*";
                String URI = "*";

                SAMRecord rec = iter.next();
                List<SAMRecord.SAMTagAndValue> list = rec.getAttributes();

                String spectrumName = rec.getReadName();

                try {

                    if (spectrumName.contains("index")) {

                        if (spectrumName.contains(":")) {
                            spectrumIndex = Integer.valueOf(spectrumName.split(":")[1].split("=")[1]);
                        } else {
                            spectrumIndex = Integer.valueOf(spectrumName.split("=")[1]);
                        }

                    } else if (spectrumName.contains(fileName)) {

                        int dot1 = spectrumName.lastIndexOf('.');
                        if ((dot1 > -1) && (dot1 < (spectrumName.length()))) {
                            spectrumIndex = Integer.valueOf(spectrumName.substring(dot1 + 1));
                        }

                    } else {
                        JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file", "File Error", JOptionPane.WARNING_MESSAGE);
                        progressDialog.setRunFinished();
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file\n", "File Error", JOptionPane.WARNING_MESSAGE);
                    progressDialog.setRunFinished();
                    break;
                }

                SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex)));

                if (spectrumFileType.equals("mgf")) {
                    try {
                        spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, spectrumIndex + 1);
                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                                null, "The spectrum title cannot match it in proBAM",
                                "Error Matching", JOptionPane.ERROR_MESSAGE);
                        break;
                    }
                } else {
                    spectrumTitle = String.valueOf(spectrumIndex);
                }

                currentMatch.setSpectrumNumber(spectrumIndex);

                for (SAMRecord.SAMTagAndValue samTagAndValue : list) {

                    if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        modificationMatches = new ArrayList<>();
                        peptideSequence = ((String) samTagAndValue.value).replaceAll("[^a-z^A-Z]", "");
                        for (String location : locationToModificationName.keySet()) {
                            if (location.equals("0")) {
                                String modificationName = locationToModificationName.get(location) + " of N-term";
                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                            } else if (location.equals(String.valueOf(peptideSequence.length() + 1))) {
                                String modificationName = locationToModificationName.get(location) + " of C-term";
                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location) - 1));
                            } else {
                                String aA = peptideSequence.split("")[Integer.parseInt(location) - 1];
                                String modificationName = locationToModificationName.get(location) + " of " + aA;
                                if (!allModifications.contains(modificationName)) {
                                    allModifications.add(modificationName);
                                }
                                modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                            }
                        }
                        peptide = new Peptide(peptideSequence, modificationMatches);
                        locationToModificationName = new HashMap<>();
                    } else if (samTagAndValue.tag.equals("XC") && !samTagAndValue.value.equals("*")) {
                        peptideCharge = (int) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XR") && !samTagAndValue.value.equals("*")) {
                        referenceSequence = (String) samTagAndValue.value;
                        referencePeptide = new Peptide(referenceSequence, null);
                    } else if (samTagAndValue.tag.equals("XS") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        score = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XB") && !samTagAndValue.value.equals("*")) {
                        massError = (double) (float) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XQ") && !samTagAndValue.value.equals("*")) {
                        BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                        fdr = bigDecimal.doubleValue();
                    } else if (samTagAndValue.tag.equals("XM") && !samTagAndValue.value.equals("*")) {
                        locationToModificationName = new HashMap<>();
                        String modificationAccessionList = (String) samTagAndValue.value;
                        String[] allModifications = modificationAccessionList.split(";");
                        for (String eachModification : allModifications) {
                            String[] locationAndName = eachModification.split("-");
                            String modificationName = accessionToModification.get(locationAndName[1]);
                            locationToModificationName.put(locationAndName[0], modificationName);
                        }
                    } else if (samTagAndValue.tag.equals("XA") && !samTagAndValue.value.equals("*")) {
                        if (samTagAndValue.value.equals("0")) {
                            isAnnotated = "yes";
                        } else if (samTagAndValue.value.equals("1")) {
                            isAnnotated = "partially unknown";
                        } else if (samTagAndValue.value.equals("2")) {
                            isAnnotated = "totally unknown";
                        }
                    } else if (samTagAndValue.tag.equals("YA") && !samTagAndValue.value.equals("*")) {
                        followingAA = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("YB") && !samTagAndValue.value.equals("*")) {
                        precedingAA = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XE") && !samTagAndValue.value.equals("*")) {
                        enzyme = getEnzyme("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("XG") && !samTagAndValue.value.equals("*")) {
                        peptideType = getPeptideType("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("NH") && !samTagAndValue.value.equals("*")) {
                        gengMapNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XI") && !samTagAndValue.value.equals("*")) {
                        peptideInt = (double) (float) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XL") && !samTagAndValue.value.equals("*")) {
                        peptideMapNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XN") && !samTagAndValue.value.equals("*")) {
                        missedCNum = (Integer) samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XO") && !samTagAndValue.value.equals("*")) {
                        uniqueType = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                        originalSeq = "" + samTagAndValue.value;
                    } else if (samTagAndValue.tag.equals("XT") && !samTagAndValue.value.equals("*")) {
                        enzymeInSearch = getEnzymeInSear("" + samTagAndValue.value);
                    } else if (samTagAndValue.tag.equals("XU") && !samTagAndValue.value.equals("*")) {
                        URI = "" + samTagAndValue.value;
                    }
                }

                if (peptide != null) {

                    spectrumList.add(String.valueOf(countAll));

                    if (count == 0) {
                        preparedStatement = connection.prepareStatement(addDataIntoTable);
                    }

                    PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, peptideCharge), massError, null);

                    if (referencePeptide != null) {
                        PeptideAssumption peptideAssumptionR = new PeptideAssumption(referencePeptide, 2, 1, new Charge(1, peptideCharge), 0, null);

                        currentMatch.addHit(1, peptideAssumptionR, false);
                    }

                    peptideAssumption.setRawScore(fdr);

                    currentMatch.addHit(1, peptideAssumption, false);

                    currentMatch.setBestPeptideAssumption(peptideAssumption);

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        try {
                            oos.writeObject(currentMatch);
                        } finally {
                            oos.close();
                        }
                    } finally {
                        bos.close();
                    }

                    preparedStatement.setInt(1, countAll);
                    preparedStatement.setDouble(2, peptideAssumption.getTheoreticMass() / peptideCharge);
                    preparedStatement.setString(3, spectrumTitle);
                    preparedStatement.setString(4, peptide.getSequence());
                    preparedStatement.setDouble(5, Math.abs(massError));
                    preparedStatement.setBytes(6, bos.toByteArray());
                    preparedStatement.setDouble(7, score);
                    preparedStatement.setDouble(8, fdr);
                    preparedStatement.setString(9, isAnnotated);
                    preparedStatement.setString(10, followingAA);
                    preparedStatement.setString(11, precedingAA);
                    preparedStatement.setString(12, enzyme);
                    preparedStatement.setString(13, peptideType);
                    preparedStatement.setInt(14, gengMapNum);
                    preparedStatement.setDouble(15, peptideInt);
                    preparedStatement.setInt(16, peptideMapNum);
                    preparedStatement.setInt(17, missedCNum);
                    preparedStatement.setString(18, uniqueType);
                    preparedStatement.setString(19, originalSeq);
                    preparedStatement.setString(20, enzymeInSearch);
                    preparedStatement.setString(21, URI);

                    preparedStatement.addBatch();

                    count++;
                    countAll++;

                    if (count == 1000) {

                        int[] counts = preparedStatement.executeBatch();
                        connection.commit();
                        preparedStatement.close();

                        pdvMainClass.allSpectrumIndex.add(spectrumList);

                        count = 0;

                        if (countRound == 0) {
                            System.out.println("The count round is " + countRound);

                            pdvMainClass.displayResult();
                            pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                            progressDialog.setRunFinished();

                            countRound++;

                        } else {
                            pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                            countRound++;
                        }
                        pdvMainClass.buttonCheck();

                        spectrumList = new ArrayList<>();

                    }
                }
            }
        }

        if(count != 0){

            int[] counts = preparedStatement.executeBatch();
            connection.commit();
            preparedStatement.close();

            pdvMainClass.allSpectrumIndex.add(spectrumList);

            if(countRound == 0){

                pdvMainClass.displayResult();
                pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                progressDialog.setRunFinished();

            } else {
                pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
            }
            pdvMainClass.buttonCheck();

        }
        pdvMainClass.loadingJButton.setIcon(new ImageIcon(getClass().getResource("/icons/done.png")));
        pdvMainClass.loadingJButton.setText("Import done");
        pdvMainClass.searchButton.setToolTipText("Find items");
        pdvMainClass.searchItemTextField.setToolTipText("Find items");
    }

    /**
     * Return enzymes name
     * @param index Enzyme index
     * @return String
     */
    private String getEnzyme(String index){
        switch (index) {
            case "0":
                return "No_enzyme";
            case "1":
                return "Trypsin";
            case "2":
                return "Trypsin/P";
            case "3":
                return "Lys_C";
            case "4":
                return "Lys_N";
            case "5":
                return "Arg_C";
            case "6":
                return "Asp_N";
            case "7":
                return "CNBr";
            case "8":
                return "Glu_C";
            case "9":
                return "PepsinA";
            case "10":
                return "Chymotrypsin";
            default:
                return "*";
        }
    }

    /**
     * Return peptide type name
     * @param index Peptide type index
     * @return String
     */
    private String getPeptideType(String index){
        switch (index) {
            case "N":
                return "normal peptide";
            case "V":
                return "variant peptide";
            case "W":
                return "indel peptide";
            case "J":
                return "novel junction peptide";
            case "A":
                return "alternative junction peptide";
            case "M":
                return "novel exon peptide";
            case "C":
                return "cross junction peptide";
            case "E":
                return "extension peptide";
            case "B":
                return "3’UTR peptide";
            case "O":
                return "out-of-frame peptide";
            case "T":
                return "truncation peptide";
            case "R":
                return "reverse strand peptide";
            case "I":
                return "intron peptide";
            case "G":
                return "gene fusion peptide";
            case "D":
                return "decoy peptide";
            case "U":
                return "unmapped";
            case "X":
                return "unknown";
            default:
                return "*";
        }
    }

    /**
     * Return enzyme name
     * @param index Enzyme index
     * @return String
     */
    private String getEnzymeInSear(String index){

        switch (index) {
            case "o":
                return "non- enzymatic";
            case "1":
                return "semi- enzymatic";
            case "2":
                return "fully-enzymatic";
            case "3":
                return "unknown";
            default:
                return "*";
        }
    }

    /**
     * Get Modification mass map
     * @return HashMap
     */
    private HashMap<String,HashMap<Double, String >> getModificationMass(){

        PTMFactory ptmFactory = PTMFactory.getInstance();

        HashMap<String,HashMap<Double, String > > modificationMass = new HashMap<>();
        ArrayList<String> orderedModifications = ptmFactory.getPTMs();
        for (String  modificationName : orderedModifications){
            String[] modificationNameSplit = String.valueOf(ptmFactory.getPTM(modificationName)).split(" ");
            String aminoAcidName  = modificationNameSplit[modificationNameSplit.length-1];
            if(modificationMass.containsKey(aminoAcidName)){
                modificationMass.get(aminoAcidName).put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
            }else {
                HashMap<Double, String> singleModi = new HashMap<>();
                singleModi.put(ptmFactory.getPTM(modificationName).getMass(), modificationName);
                modificationMass.put(aminoAcidName, singleModi);
            }
        }
        return modificationMass;
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
        return scoreName;
    }

    /**
     * Return all modifications
     * @return ArrayList
     */
    public ArrayList<String> getAllModifications(){
        return allModifications;
    }

}

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
     * Tag to show if parsing success or not
     */
    private Boolean parseFaild = false;

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
    public ProBamFileImport(PDVMainClass pdvMainClass, File proBAMFile, String spectrumFileName, SpectrumFactory spectrumFactory, ProgressDialogX progressDialog) throws SQLException, ClassNotFoundException {
        this.pdvMainClass = pdvMainClass;
        this.proBAMFile = proBAMFile;
        this.spectrumFactory = spectrumFactory;
        this.progressDialog = progressDialog;

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
    private void parseProBAM() throws SQLException, IOException, MzMLUnmarshallerException {

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

        SamReader samFileReader;

        final SamReaderFactory srf = htsjdk.samtools.SamReaderFactory.makeDefault().validationStringency(htsjdk.samtools.ValidationStringency.LENIENT);

        samFileReader = srf.open(SamInputResource.of(proBAMFile));

        SAMRecordIterator iter = samFileReader.iterator();
        ArrayList<ModificationMatch> modificationMatches;
        HashMap<String, String> locationToModificationName = new HashMap<>();
        ArrayList<String> spectrumList = new ArrayList<>();

        int count = 0;
        int countRound = 0;
        int countAll = 0;

        while(iter.hasNext()) {

            Integer spectrumIndex = null;
            Peptide peptide = null;
            Peptide referencePeptide = null;

            String peptideSequence;
            int peptideCharge = 0;
            String referenceSequence;
            double score = 0.0;
            double fdr = 0.0;
            double massError = 0.0;
            Double rt = 0.0;
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

                    spectrumIndex = Integer.valueOf(spectrumName.split(":")[1].split("=")[1]);

                } else if (spectrumName.contains(spectrumFileName)) {

                    int dot = spectrumName.lastIndexOf('.');
                    if ((dot > -1) && (dot < (spectrumName.length()))) {
                        spectrumIndex = Integer.valueOf(spectrumName.substring(dot + 1));
                    }

                } else {
                    JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file", "File Error", JOptionPane.WARNING_MESSAGE);
                    progressDialog.setRunFinished();
                    parseFaild = true;
                    break;
                }
            } catch (Exception e){
                e.printStackTrace();
                JOptionPane.showMessageDialog(pdvMainClass, "Failed to pares proBAM file\n" , "File Error", JOptionPane.WARNING_MESSAGE);
                progressDialog.setRunFinished();
                parseFaild = true;
                break;
            }

            SpectrumMatch currentMatch = new SpectrumMatch(Spectrum.getSpectrumKey(spectrumFileName, String.valueOf(spectrumIndex)));

            try {
                spectrumTitle = spectrumFactory.getSpectrumTitle(spectrumFileName, spectrumIndex + 1);
            }catch (Exception e){
                progressDialog.setRunFinished();
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        null, "The spectrum title cannot match it in proBAM",
                        "Error Matching", JOptionPane.ERROR_MESSAGE);
                break;
            }

            rt = ((MSnSpectrum)spectrumFactory.getSpectrum(spectrumFileName, spectrumTitle)).getPrecursor().getRtInMinutes();

            currentMatch.setSpectrumNumber(spectrumIndex);

            for (SAMRecord.SAMTagAndValue samTagAndValue: list) {

                if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")) {
                    modificationMatches = new ArrayList<>();
                    peptideSequence = (String) samTagAndValue.value;
                    for(String location: locationToModificationName.keySet()){
                        if(location.equals("0")){
                            String modificationName = locationToModificationName.get(location) + " of N-term";
                            if (!allModifications.contains(modificationName)){
                                allModifications.add(modificationName);
                            }
                            modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                        } else if(location.equals(String.valueOf(peptideSequence.length()+1))){
                            String modificationName = locationToModificationName.get(location) + " of C-term";
                            if (!allModifications.contains(modificationName)){
                                allModifications.add(modificationName);
                            }
                            modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)-1));
                        } else {
                            String aA = peptideSequence.split("")[Integer.parseInt(location)-1];
                            String modificationName = locationToModificationName.get(location) + " of " + aA;
                            if (!allModifications.contains(modificationName)){
                                allModifications.add(modificationName);
                            }
                            modificationMatches.add(new ModificationMatch(modificationName, true, Integer.parseInt(location)));
                        }
                    }
                    peptide = new Peptide(peptideSequence, modificationMatches);
                    locationToModificationName = new HashMap<>();
                } else if (samTagAndValue.tag.equals("XC") && !samTagAndValue.value.equals("*")) {
                    peptideCharge = (int) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XR") && !samTagAndValue.value.equals("*")){
                    referenceSequence = (String) samTagAndValue.value;
                    referencePeptide = new Peptide(referenceSequence, null);
                } else if (samTagAndValue.tag.equals("XS") && !samTagAndValue.value.equals("*")){
                    BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                    score = bigDecimal.doubleValue();
                } else if (samTagAndValue.tag.equals("XB") && !samTagAndValue.value.equals("*")){
                    massError = (double) (float) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XQ") && !samTagAndValue.value.equals("*")){
                    BigDecimal bigDecimal = new BigDecimal(Float.toString((Float) samTagAndValue.value));
                    fdr = bigDecimal.doubleValue();
                } else if (samTagAndValue.tag.equals("XM") && !samTagAndValue.value.equals("*")){
                    locationToModificationName = new HashMap<>();
                    String modificationAccessionList = (String) samTagAndValue.value;
                    String[] allModifications = modificationAccessionList.split(";");
                    for (String eachModification: allModifications){
                        String[] locationAndName = eachModification.split("-");
                        String modificationName = accessionToModification.get(locationAndName[1]);
                        locationToModificationName.put(locationAndName[0], modificationName);
                    }
                } else if (samTagAndValue.tag.equals("XA") && !samTagAndValue.value.equals("*")){
                    if (samTagAndValue.value.equals("0")){
                        isAnnotated = "yes";
                    } else if (samTagAndValue.value.equals("1")){
                        isAnnotated = "partially unknown";
                    } else if (samTagAndValue.value.equals("2")){
                        isAnnotated = "totally unknown";
                    }
                } else if (samTagAndValue.tag.equals("YA") && !samTagAndValue.value.equals("*")){
                    followingAA = "" + samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("YB") && !samTagAndValue.value.equals("*")){
                    precedingAA = "" + samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XE") && !samTagAndValue.value.equals("*")){
                    enzyme = getEnzyme(""+samTagAndValue.value);
                } else if (samTagAndValue.tag.equals("XG") && !samTagAndValue.value.equals("*")){
                    peptideType = getPeptideType(""+samTagAndValue.value);
                } else if (samTagAndValue.tag.equals("NH") && !samTagAndValue.value.equals("*")){
                    gengMapNum = (Integer) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XI") && !samTagAndValue.value.equals("*")){
                    peptideInt = (double) (float) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XL") && !samTagAndValue.value.equals("*")){
                    peptideMapNum = (Integer) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XN") && !samTagAndValue.value.equals("*")){
                    missedCNum = (Integer) samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XO") && !samTagAndValue.value.equals("*")){
                    uniqueType = "" + samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XP") && !samTagAndValue.value.equals("*")){
                    originalSeq = "" + samTagAndValue.value;
                } else if (samTagAndValue.tag.equals("XT") && !samTagAndValue.value.equals("*")){
                    enzymeInSearch = getEnzymeInSear("" + samTagAndValue.value);
                } else if (samTagAndValue.tag.equals("XU") && !samTagAndValue.value.equals("*")){
                    URI = "" + samTagAndValue.value;
                }
            }

            if(peptide != null){

                spectrumList.add(String.valueOf(countAll));

                if (count == 0){
                    preparedStatement = connection.prepareStatement(addDataIntoTable);
                }

                PeptideAssumption peptideAssumption = new PeptideAssumption(peptide, 1, 1, new Charge(1, peptideCharge), massError, null);

                if(referencePeptide != null){
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
                preparedStatement.setDouble(2, peptideAssumption.getTheoreticMass()/peptideCharge);
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

                count ++;
                countAll ++;

                if(count == 1000){

                    int[] counts = preparedStatement.executeBatch();
                    connection.commit();
                    preparedStatement.close();

                    pdvMainClass.allSpectrumIndex.add(spectrumList);

                    count = 0;

                    if(countRound == 0){
                        System.out.println("The count round is "+countRound);

                        pdvMainClass.displayResult();
                        pdvMainClass.pageNumJTextField.setText(1 + "/" + 1);
                        progressDialog.setRunFinished();

                        countRound ++;

                    } else {
                        pdvMainClass.pageNumJTextField.setText(String.valueOf(pdvMainClass.selectedPageNum) + "/" + String.valueOf(pdvMainClass.allSpectrumIndex.size()));
                        countRound ++;
                    }
                    pdvMainClass.buttonCheck();

                    spectrumList = new ArrayList<>();

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

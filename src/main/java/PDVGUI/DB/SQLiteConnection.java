package PDVGUI.DB;

import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;

import javax.sql.rowset.serial.SerialBlob;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.*;
import java.util.ArrayList;

/**
 * DB class
 * Created by Ken on 5/25/2017.
 */
public class SQLiteConnection {

    /**
     * Database connection
     */
    private Connection connection;
    /**
     * Score num
     */
    private Integer scoreNum;

    /**
     * Constructor of DB
     * @param name DB name
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public SQLiteConnection(String name) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + name );
    }

    /**
     * Set additional attributes number
     * @param scoreNum Additional attributes number
     */
    public void setScoreNum(Integer scoreNum){
        this.scoreNum = scoreNum;
    }

    /**
     * Get DB connection
     * @return Connection
     */
    public Connection getConnection(){
        return connection;
    }

    /**
     * Close DB connection
     */
    public void closeConnection(){
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get one SpectrumMach
     * @param psmKey Spectrum key
     * @return SpectrumMatch
     * @throws SQLException
     */
    public SpectrumMatch getSpectrumMatch(String psmKey) throws SQLException {
        SpectrumMatch spectrumMatch = null;
        Statement statement = connection.createStatement();
        String query1 = "SELECT Match FROM SpectrumMatch WHERE PSMIndex = '" + psmKey + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        while (rs1.next()){
            Blob tempBlob;
            byte[] bytes = rs1.getBytes(1);
            tempBlob = new SerialBlob(bytes);
            BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(bis);
                try {
                    spectrumMatch = (SpectrumMatch) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return spectrumMatch;
    }

    /**
     * Get one record
     * @param libID Library ID
     * @return ArrayList
     * @throws SQLException
     */
    public ArrayList<Object> getOneItem(Integer libID) throws SQLException {

        ArrayList<Object> oneItem = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT * FROM SpectrumMatch WHERE LibID = "+libID;
        ResultSet rs1 = statement.executeQuery(query1);
        SpectrumMatch spectrumMatch = null;
        Spectrum spectrum = null;

        while (rs1.next()){
            oneItem.add(rs1.getDouble(2));
            oneItem.add(rs1.getInt(3));
            oneItem.add(rs1.getInt(4));
            oneItem.add(rs1.getDouble(5));
            Blob tempBlob;
            byte[] bytes = rs1.getBytes(6);
            tempBlob = new SerialBlob(bytes);
            BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(bis);
                try {
                    spectrumMatch = (SpectrumMatch) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            oneItem.add(spectrumMatch);

            Blob scoreBlob;
            byte[] scoreBytes = rs1.getBytes(7);
            scoreBlob = new SerialBlob(scoreBytes);
            BufferedInputStream scoreBis = new BufferedInputStream(scoreBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(scoreBis);
                try {
                    spectrum = (Spectrum) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    scoreBis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            oneItem.add(spectrum);

            ArrayList<String> proteinList = new ArrayList<>();
            Blob proteinBlob;
            byte[] proteinBytes = rs1.getBytes(8);
            proteinBlob = new SerialBlob(proteinBytes);
            BufferedInputStream proteinBis = new BufferedInputStream(proteinBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(proteinBis);
                try {
                    proteinList = (ArrayList<String>) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    proteinBis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            oneItem.add(proteinList);


        } rs1.close();

        return oneItem;
    }

    /**
     * Get one record
     * @param spectrumID Spectrum ID
     * @return ArrayList
     * @throws SQLException
     */
    public ArrayList<Object> getOneSpectrumItem(String spectrumID) throws SQLException {

        ArrayList<Object> oneItem = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT * FROM SpectrumMatch WHERE PSMIndex = '" + spectrumID + "'";
        ResultSet rs1 = statement.executeQuery(query1);
        SpectrumMatch spectrumMatch = null;

        byte[] bytes;
        Blob tempBlob;

        while (rs1.next()){
            bytes = rs1.getBytes(6);
            tempBlob = new SerialBlob(bytes);
            BufferedInputStream bis = new BufferedInputStream(tempBlob.getBinaryStream());
            try {
                ObjectInputStream in = new ObjectInputStream(bis);
                try {
                    spectrumMatch = (SpectrumMatch) in.readObject();
                } finally {
                    in.close();
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            oneItem.add(spectrumMatch);

            for (int i = 0; i<scoreNum; i++){
                oneItem.add(rs1.getObject(7+i));
            }
        }

        return oneItem;
    }

    /**
     * Get all index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String> getAllIndex() throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT PSMIndex FROM SpectrumMatch";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){
            selectedIndexList.add(rs1.getString(1));
        }

        return selectedIndexList;
    }

    /**
     * Get all searched index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String> getSelectedPeptideIndex(String peptide) throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT PSMIndex FROM SpectrumMatch WHERE Sequence LIKE '%" + peptide + "%'";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){
            selectedIndexList.add(rs1.getString(1));
        }

        return selectedIndexList;
    }

    /**
     * Get all searched index
     * @return ArrayList<String></>
     * @throws SQLException
     */
    public ArrayList<String> getSelectedTitleIndex(String title) throws SQLException {
        ArrayList<String> selectedIndexList = new ArrayList<>();

        Statement statement = connection.createStatement();
        String query1 = "SELECT PSMIndex FROM SpectrumMatch WHERE Title LIKE '%" + title + "%'";
        ResultSet rs1 = statement.executeQuery(query1);

        while (rs1.next()){
            selectedIndexList.add(rs1.getString(1));
        }

        return selectedIndexList;
    }
}

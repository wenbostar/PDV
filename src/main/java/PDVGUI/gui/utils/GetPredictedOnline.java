package PDVGUI.gui.utils;

import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.rngom.util.Uri;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GetPredictedOnline {

    private URL usedURL;
    private String model;
    private String pepSeq;
    private int precursorCharge;
    private int collisionEnergy;
    private String instrument;
    private HashMap<Integer, String> newMods;
    private String inputString;
    private double precursorMZ;

    public static void main(String[] args) throws IOException {

        new GetPredictedOnline();
    }

    public GetPredictedOnline() throws IOException {
        getSpectra();
    }

    public GetPredictedOnline(String model, String pepSeq, HashMap<Integer, String> newMods,
                              int precursorCharge, int collisionEnergy, String instrument, double precursorMZ) throws IOException {

        this.model = model;
        this.pepSeq = pepSeq;
        this.newMods = newMods;
        this.precursorCharge = precursorCharge;
        this.collisionEnergy = collisionEnergy;
        this.instrument = instrument;
        this.precursorMZ = precursorMZ;

        setURL();
        generateInputString();
    }

    private void setURL() throws MalformedURLException {
        if (model.equals("AlphaPept_ms2_generic")) {
            String url = "https://koina.proteomicsdb.org/v2/models/AlphaPept_ms2_generic/infer";
            usedURL = new URL(url);
        } else if (model.equals("ms2pip_2021_HCD")){
            String url = "https://koina.proteomicsdb.org/v2/models/ms2pip_2021_HCD/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2019_intensity")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2019_intensity/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2023_intensity_timsTOF")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2023_intensity_timsTOF/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2020_intensity_CID")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2020_intensity_CID/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2020_intensity_HCD")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2020_intensity_HCD/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2023_intensity_XL_CMS2")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2023_intensity_XL_CMS2/infer";
            usedURL = new URL(url);
        } else if (model.equals("Prosit_2020_intensity_TMT")){
            String url = "https://koina.proteomicsdb.org/v2/models/Prosit_2020_intensity_TMT/infer";
            usedURL = new URL(url);
        }
    }

    private void generateInputString(){
        String modPep = "";
        if (newMods.isEmpty()){
            modPep = pepSeq;
        } else {
            // Add the modifications to the peptide sequence
            // (the modifications are added in the order of their position in the peptide sequence)
            int lastIndex = 0;
            for (int index : newMods.keySet()){
                modPep += pepSeq.substring(lastIndex, index) + newMods.get(index);
            }
            modPep += pepSeq.substring(lastIndex);
        }

        if (model.equals("AlphaPept_ms2_generic")){

            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }, { \"name\": \"instrument_types\", \"shape\": [ 1, 1 ], \"datatype\": \"BYTES\", \"data\": [ \"" +
                    instrument + "\" ] } ] }";
        } else if (model.equals("ms2pip_2021_HCD")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }] }";
        } else if (model.equals("Prosit_2019_intensity")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }] }";
        } else if (model.equals("Prosit_2023_intensity_timsTOF")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }] }";
        } else if (model.equals("Prosit_2020_intensity_CID")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }] }";
        } else if (model.equals("Prosit_2020_intensity_HCD")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }] }";
        } else if (model.equals("Prosit_2023_intensity_XL_CMS2")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }] }";
        } else if (model.equals("Prosit_2020_intensity_TMT")){
            inputString = "{ \"id\": \"0\", \"inputs\": [ { \"name\": \"peptide_sequences\", \"shape\": [ 1, 1 ], " +
                    "\"datatype\": \"BYTES\", \"data\": [ \"" + modPep + "\" ] }, { \"name\": \"precursor_charges\", " +
                    "\"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ "+ precursorCharge + " ] }, { \"name\": " +
                    "\"collision_energies\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ "+ collisionEnergy +
                    " ] }] }";
        }

    }

    public MSnSpectrum getSpectra() throws IOException {

        MSnSpectrum spectrum = null;

        HttpURLConnection conn = (HttpURLConnection) usedURL.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = inputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();

        if (responseCode == 200) {

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }

                JSONObject jsonObject = new JSONObject(response.toString());
                JSONArray output = jsonObject.getJSONArray("outputs");
                JSONObject intObject = (JSONObject) output.get(0);
                JSONObject mzObject = (JSONObject) output.get(1);
                List<Object> intensity = intObject.getJSONArray("data").toList();
                List<Object> mzs = mzObject.getJSONArray("data").toList();

                HashMap<Double, Peak> peakHashMap = new HashMap<>();
                for (int i = 0; i < intensity.size(); i++) {
                    if ((double) intensity.get(i) > 0) {
                        Peak peak = new Peak((double) mzs.get(i), (double) intensity.get(i));
                        peakHashMap.put((double) mzs.get(i), peak);
                    }
                }
                Charge charge = new Charge(1, precursorCharge);
                ArrayList<Charge> charges = new ArrayList<>();
                charges.add(charge);

                Precursor precursor = new Precursor(0, precursorMZ, 0, charges);
                spectrum = new MSnSpectrum(2, precursor, "", peakHashMap, "");

            }
        } else {
            System.out.println("Response Code : " + responseCode);
        }

        return spectrum;
    }

//    private void getSpectra() throws IOException {
//        String url = "https://koina.proteomicsdb.org/v2/models/AlphaPept_ms2_generic/infer";
//        usedURL = new URL(url);
//
//        HttpURLConnection conn = (HttpURLConnection) usedURL.openConnection();
//        conn.setRequestMethod("POST");
//        conn.setRequestProperty("Content-Type", "application/json; utf-8");
//        conn.setRequestProperty("Accept", "application/json");
//        conn.setDoOutput(true);
//
//        String jsonInputString = "{\n" +
//                "  \"id\": \"0\",\n" +
//                "  \"inputs\": [\n" +
//                "    {\n" +
//                "      \"name\": \"peptide_sequences\",\n" +
//                "      \"shape\": [\n" +
//                "        1,\n" +
//                "        1\n" +
//                "      ],\n" +
//                "      \"datatype\": \"BYTES\",\n" +
//                "      \"data\": [\n" +
//                "        \"AAAAAKAKM[UNIMOD:35]\"\n" +
//                "      ]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"name\": \"precursor_charges\",\n" +
//                "      \"shape\": [\n" +
//                "        1,\n" +
//                "        1\n" +
//                "      ],\n" +
//                "      \"datatype\": \"INT32\",\n" +
//                "      \"data\": [\n" +
//                "        2\n" +
//                "      ]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"name\": \"collision_energies\",\n" +
//                "      \"shape\": [\n" +
//                "        1,\n" +
//                "        1\n" +
//                "      ],\n" +
//                "      \"datatype\": \"FP32\",\n" +
//                "      \"data\": [\n" +
//                "        25\n" +
//                "      ]\n" +
//                "    },\n" +
//                "    {\n" +
//                "      \"name\": \"instrument_types\",\n" +
//                "      \"shape\": [\n" +
//                "        1,\n" +
//                "        1\n" +
//                "      ],\n" +
//                "      \"datatype\": \"BYTES\",\n" +
//                "      \"data\": [\n" +
//                "        \"QE\"\n" +
//                "      ]\n" +
//                "    }\n" +
//                "  ]\n" +
//                "}";
//
//        try(OutputStream os = conn.getOutputStream()) {
//            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
//            os.write(input, 0, input.length);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        int responseCode = conn.getResponseCode();
//        System.out.println("Response Code : " + responseCode);
//
//        try(BufferedReader br = new BufferedReader(
//                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
//
//            StringBuilder response = new StringBuilder();
//            String responseLine = null;
//            while ((responseLine = br.readLine()) != null) {
//                response.append(responseLine.trim());
//            }
//
//            JSONObject jsonObject = new JSONObject(response.toString());
//            JSONArray output = jsonObject.getJSONArray("outputs");
//            JSONObject intObject = (JSONObject) output.get(0);
//            JSONObject mzObject = (JSONObject) output.get(1);
//            List<Object> intensity = intObject.getJSONArray("data").toList();
//            List<Object> mzs = mzObject.getJSONArray("data").toList();
//
//            for (int i = 0; i < intensity.size(); i++) {
//                if ((double) intensity.get(i) > 0) {
//                    System.out.println(mzs.get(i) + " " + intensity.get(i));
//                }
//            }
//
//        }
//
//
//    }

}

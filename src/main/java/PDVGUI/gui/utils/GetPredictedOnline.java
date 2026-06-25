package PDVGUI.gui.utils;

import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
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
    private String fragmentMethod;

    /** Public Koina inference server. Models are reached at KOINA_BASE + "<model>/infer". */
    private static final String KOINA_BASE = "https://koina.wilhelmlab.org/v2/models/";

    // Which extra inputs each model's request needs (every model also takes peptide + charge). A new
    // model is added by categorising it here; the request body and the dialog's combo visibility both
    // derive from these, so there is no per-model copy of the request to keep in sync.
    private static final java.util.Set<String> INSTRUMENT_MODELS =
            new java.util.HashSet<>(java.util.Arrays.asList("AlphaPept_ms2_generic", "UniSpec"));
    private static final java.util.Set<String> FRAGMENTATION_MODELS =
            new java.util.HashSet<>(java.util.Arrays.asList("Prosit_2020_intensity_TMT"));
    private static final java.util.Set<String> CHARGE_ONLY_MODELS =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "ms2pip_2021_HCD", "Prosit_2020_intensity_CID", "ms2pip_Immuno_HCD", "ms2pip_timsTOF2024"));

    /** @return true if the model's request needs the collision_energies input. */
    static boolean needsCollisionEnergy(String model) { return !CHARGE_ONLY_MODELS.contains(model); }

    /** @return true if the model's request needs the instrument_types input. */
    static boolean needsInstrument(String model) { return INSTRUMENT_MODELS.contains(model); }

    /** @return true if the model's request needs the fragmentation_types input. */
    static boolean needsFragmentation(String model) { return FRAGMENTATION_MODELS.contains(model); }

    // Instrument tokens each instrument-using model accepts. A model absent here accepts every
    // instrument in the dialog's list (e.g. the generic AlphaPept model); UniSpec is an Orbitrap-HCD
    // model whose NCE->eV step only knows QE and LUMOS (timsTOF / SciexTOF are rejected by the server).
    private static final java.util.Map<String, java.util.Set<String>> SUPPORTED_INSTRUMENTS = buildSupportedInstruments();

    private static java.util.Map<String, java.util.Set<String>> buildSupportedInstruments() {
        java.util.Map<String, java.util.Set<String>> m = new java.util.HashMap<>();
        m.put("UniSpec", new java.util.HashSet<>(java.util.Arrays.asList("QE", "LUMOS")));
        return m;
    }

    /**
     * The instrument tokens a model accepts, or null when the model is unrestricted (accepts every
     * instrument in the dialog's list). Used to filter the dialog's instrument dropdown.
     * @param model the model name
     * @return the set of accepted instrument tokens, or null for no restriction
     */
    static java.util.Set<String> supportedInstruments(String model) {
        return SUPPORTED_INSTRUMENTS.get(model);
    }

    /**
     * Whether a model accepts a given instrument token (models not otherwise restricted accept all).
     * @param model the model name
     * @param instrumentSuffix the instrument token (the part after ": " in the dialog's instrument list)
     * @return true if the model accepts that instrument
     */
    static boolean supportsInstrument(String model, String instrumentSuffix) {
        java.util.Set<String> allowed = SUPPORTED_INSTRUMENTS.get(model);
        return allowed == null || allowed.contains(instrumentSuffix);
    }

    public static void main(String[] args) throws IOException {

        new GetPredictedOnline();
    }

    public GetPredictedOnline() throws IOException {
        getSpectra("");
    }

    public GetPredictedOnline(String model, String pepSeq, HashMap<Integer, String> newMods, String fragmentMethod,
                              int precursorCharge, int collisionEnergy, String instrument, double precursorMZ) throws IOException {

        this.model = model;
        this.pepSeq = pepSeq;
        this.newMods = newMods;
        this.fragmentMethod = fragmentMethod;
        this.precursorCharge = precursorCharge;
        this.collisionEnergy = collisionEnergy;
        this.instrument = instrument;
        this.precursorMZ = precursorMZ;

        setURL();
        generateInputString();
    }

    /** @return the JSON request body generated for this model (package-visible for testing). */
    String getInputString() {
        return inputString;
    }

    private void setURL() throws MalformedURLException {
        usedURL = new URL(buildInferUrl(model));
    }

    /**
     * Build the Koina inference endpoint URL for a model. All models share the same endpoint shape,
     * so this is derived from the model name instead of branching per model; adding a model only
     * needs an entry in the dialog's list. Package-visible for testing.
     * @param model the Koina model name
     * @return the full /infer URL on the public Koina server
     */
    static String buildInferUrl(String model) {
        return KOINA_BASE + model + "/infer";
    }

    private void generateInputString(){
        String modPep = buildModifiedPeptide();

        // Assemble the request from named input fragments based on what the model needs (see the
        // *_MODELS sets). Every model takes peptide + charge; collision energy / instrument /
        // fragmentation are appended per category, so adding a model needs no new request body.
        StringBuilder inputs = new StringBuilder();
        inputs.append(bytesInput("peptide_sequences", modPep));
        inputs.append(", ").append(intInput("precursor_charges", precursorCharge));
        if (needsCollisionEnergy(model)) {
            inputs.append(", ").append(floatInput("collision_energies", collisionEnergy));
        }
        if (needsInstrument(model)) {
            inputs.append(", ").append(bytesInput("instrument_types", instrument));
        }
        if (needsFragmentation(model)) {
            inputs.append(", ").append(bytesInput("fragmentation_types", fragmentMethod));
        }
        inputString = "{ \"id\": \"0\", \"inputs\": [ " + inputs + " ] }";
    }

    /**
     * Apply the site-&gt;UNIMOD modifications to the bare sequence: an N-term modification (site 0)
     * prefixes the peptide, an internal modification follows its residue.
     * @return the ProForma-style modified peptide string
     */
    private String buildModifiedPeptide() {
        if (newMods.isEmpty()) {
            return pepSeq;
        }
        // pos 0 = N-term; pos n = the n-th residue (1-based).
        String[] aa_list = pepSeq.split("");
        for (int index : newMods.keySet()) {
            // TODO: C-term modification
            if (index == 0) {
                aa_list[0] = newMods.get(index) + "-" + aa_list[0];
            } else {
                aa_list[index - 1] = aa_list[index - 1] + newMods.get(index);
            }
        }
        return StringUtils.join(aa_list, "");
    }

    private static String bytesInput(String name, String value) {
        return "{ \"name\": \"" + name + "\", \"shape\": [ 1, 1 ], \"datatype\": \"BYTES\", \"data\": [ \"" + value + "\" ] }";
    }

    private static String intInput(String name, int value) {
        return "{ \"name\": \"" + name + "\", \"shape\": [ 1, 1 ], \"datatype\": \"INT32\", \"data\": [ " + value + " ] }";
    }

    private static String floatInput(String name, int value) {
        return "{ \"name\": \"" + name + "\", \"shape\": [ 1, 1 ], \"datatype\": \"FP32\", \"data\": [ " + value + " ] }";
    }

    /**
     * Find a Koina inference output by its "name" field (e.g. "mz", "intensities", "annotation").
     * @param outputs the "outputs" JSON array from the response
     * @param name the output name to look for
     * @return the matching output object, or null if none has that name
     */
    private static JSONObject getOutputByName(JSONArray outputs, String name) {
        for (int i = 0; i < outputs.length(); i++) {
            JSONObject o = outputs.getJSONObject(i);
            if (name.equals(o.optString("name"))) {
                return o;
            }
        }
        return null;
    }

    /**
     * Parse a Koina KServe-v2 inference response body into an {@link MSnSpectrum}. The "mz" and
     * "intensities" outputs are read by name (so the result is independent of output ordering), and
     * only peaks with intensity &gt; 0 are kept. Returns null when either expected output is absent.
     * Package-visible so it can be unit tested without a network call.
     *
     * @param responseString the raw response body from the /infer endpoint
     * @param precursorCharge the precursor charge to attach to the spectrum
     * @param precursorMZ the precursor m/z to attach to the spectrum
     * @return the parsed spectrum, or null if the response lacks the mz/intensities outputs
     */
    static MSnSpectrum parseSpectrum(String responseString, int precursorCharge, double precursorMZ) {
        String collapsed = responseString;
        if (responseString.contains("parameters")) {
            collapsed = responseString.substring(0, responseString.indexOf("\"parameters\":")) + responseString.substring(responseString.indexOf("},") + 2);
        }

        JSONObject jsonObject = new JSONObject(collapsed);
        JSONArray output = jsonObject.getJSONArray("outputs");
        // Look the outputs up by name rather than by a per-model fixed index: every Koina intensity
        // model returns named "mz" and "intensities" arrays, so this works for all models and does
        // not break if the server changes the output order.
        JSONObject intObject = getOutputByName(output, "intensities");
        JSONObject mzObject = getOutputByName(output, "mz");
        if (intObject == null || mzObject == null) {
            System.out.println("Unexpected Koina response: missing 'intensities' or 'mz' output.");
            return null;
        }
        List<Object> intensity = intObject.getJSONArray("data").toList();
        List<Object> mzs = mzObject.getJSONArray("data").toList();

        HashMap<Double, Peak> peakHashMap = new HashMap<>();
        for (int i = 0; i < intensity.size(); i++) {
            // Cast to Number, not BigDecimal: org.json yields Integer/Long for numbers without a
            // decimal point, so a BigDecimal cast would ClassCastException on integer-formatted values.
            double intensityValue = ((Number) intensity.get(i)).doubleValue();
            if (intensityValue > 0) {
                double mzValue = ((Number) mzs.get(i)).doubleValue();
                peakHashMap.put(mzValue, new Peak(mzValue, intensityValue));
            }
        }
        Charge charge = new Charge(1, precursorCharge);
        ArrayList<Charge> charges = new ArrayList<>();
        charges.add(charge);

        Precursor precursor = new Precursor(0, precursorMZ, 0, charges);
        return new MSnSpectrum(2, precursor, "", peakHashMap, "");
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
                spectrum = parseSpectrum(response.toString(), precursorCharge, precursorMZ);

            }
        } else {
            System.out.println("Response Code : " + responseCode);
        }

        return spectrum;
    }

    private void getSpectra(String test) throws IOException {
        String url = KOINA_BASE + "AlphaPept_ms2_generic/infer";
        usedURL = new URL(url);

        HttpURLConnection conn = (HttpURLConnection) usedURL.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        String jsonInputString = "{\n" +
                "  \"id\": \"0\",\n" +
                "  \"inputs\": [\n" +
                "    {\n" +
                "      \"name\": \"peptide_sequences\",\n" +
                "      \"shape\": [\n" +
                "        1,\n" +
                "        1\n" +
                "      ],\n" +
                "      \"datatype\": \"BYTES\",\n" +
                "      \"data\": [\n" +
                "        \"SKSEEAHAEDSVM[UNIMOD:35]DHHFR\"\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"precursor_charges\",\n" +
                "      \"shape\": [\n" +
                "        1,\n" +
                "        1\n" +
                "      ],\n" +
                "      \"datatype\": \"INT32\",\n" +
                "      \"data\": [\n" +
                "        2\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"collision_energies\",\n" +
                "      \"shape\": [\n" +
                "        1,\n" +
                "        1\n" +
                "      ],\n" +
                "      \"datatype\": \"FP32\",\n" +
                "      \"data\": [\n" +
                "        27\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"instrument_types\",\n" +
                "      \"shape\": [\n" +
                "        1,\n" +
                "        1\n" +
                "      ],\n" +
                "      \"datatype\": \"BYTES\",\n" +
                "      \"data\": [\n" +
                "        \"QE\"\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        try(OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int responseCode = conn.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        try(BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            String responseString = response.toString();
            String collapsed = responseString;
            if (responseString.contains("parameters")){
                collapsed = responseString.substring(0, responseString.indexOf("\"parameters\":")) + responseString.substring(responseString.indexOf("},") + 2);
            }
//            System.out.println(response.toString());

            JSONObject jsonObject = new JSONObject(collapsed);
            JSONArray output = jsonObject.getJSONArray("outputs");
            JSONObject intObject = (JSONObject) output.get(0);
            JSONObject mzObject = (JSONObject) output.get(1);
            JSONObject annoObject = (JSONObject) output.get(2);
            List<Object> intensity = intObject.getJSONArray("data").toList();
            List<Object> mzs = mzObject.getJSONArray("data").toList();
            List<Object> anno = annoObject.getJSONArray("data").toList();

            for (int i = 0; i < intensity.size(); i++) {
                if ((double) intensity.get(i) > 0) {
                    System.out.println(mzs.get(i) + " " + intensity.get(i) + " " + anno.get(i));
                }
            }

        }


    }

}

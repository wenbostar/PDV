package PDVGUI.gui.utils;

import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import org.junit.Assume;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the Koina spectrum-prediction helper {@link GetPredictedOnline}.
 *
 * <p>The fast unit tests cover URL building, request-body generation (modification placement) and
 * response parsing with canned JSON, so they need no network. The integration test at the end hits
 * the live Koina server for every model PDV exposes and is skipped (not failed) when the server is
 * unreachable, so the build still passes offline.</p>
 */
public class GetPredictedOnlineTest {

    // ---- #1 host migration + #2 single base URL --------------------------------------------------

    @Test
    public void buildInferUrl_usesWilhelmlabHostForAnyModel() {
        assertEquals("https://koina.wilhelmlab.org/v2/models/Prosit_2019_intensity/infer",
                GetPredictedOnline.buildInferUrl("Prosit_2019_intensity"));
        assertEquals("https://koina.wilhelmlab.org/v2/models/AlphaPept_ms2_generic/infer",
                GetPredictedOnline.buildInferUrl("AlphaPept_ms2_generic"));
        // A model not yet wired anywhere still produces a well-formed URL (no per-model branch needed).
        assertEquals("https://koina.wilhelmlab.org/v2/models/UniSpec/infer",
                GetPredictedOnline.buildInferUrl("UniSpec"));
    }

    // ---- request body generation -----------------------------------------------------------------

    @Test
    public void generateInputString_placesNtermAndInternalMods() throws Exception {
        HashMap<Integer, String> mods = new HashMap<>();
        mods.put(0, "[UNIMOD:1]");   // N-term
        mods.put(3, "[UNIMOD:35]");  // on the 3rd residue (P of PEPTIDE)

        GetPredictedOnline g = new GetPredictedOnline(
                "AlphaPept_ms2_generic", "PEPTIDE", mods, "HCD", 2, 27, "QE", 700.0);
        String body = g.getInputString();

        // N-term mod prefixes the sequence; internal mod follows its residue.
        assertTrue("body should contain the modified peptide, was: " + body,
                body.contains("[UNIMOD:1]-PEP[UNIMOD:35]TIDE"));
        // AlphaPept request carries instrument + charge + collision energy fields.
        assertTrue(body.contains("\"instrument_types\""));
        assertTrue(body.contains("\"precursor_charges\""));
        assertTrue(body.contains("\"collision_energies\""));
    }

    @Test
    public void generateInputString_unmodifiedPeptideIsVerbatim() throws Exception {
        GetPredictedOnline g = new GetPredictedOnline(
                "Prosit_2019_intensity", "LGEYGFQNALIVR", new HashMap<Integer, String>(), "HCD", 2, 27, "QE", 700.0);
        assertTrue(g.getInputString().contains("\"data\": [ \"LGEYGFQNALIVR\" ]"));
    }

    // ---- per-model input categories (drive both the request body and the dialog combos) ----------

    @Test
    public void modelCategories_matchKoinaInputSchemas() {
        // Models that take an instrument input.
        assertTrue(GetPredictedOnline.needsInstrument("AlphaPept_ms2_generic"));
        assertTrue(GetPredictedOnline.needsInstrument("UniSpec"));
        assertFalse(GetPredictedOnline.needsInstrument("Prosit_2019_intensity"));
        // Models that take a fragmentation_types input.
        assertTrue(GetPredictedOnline.needsFragmentation("Prosit_2020_intensity_TMT"));
        assertFalse(GetPredictedOnline.needsFragmentation("Prosit_2019_intensity"));
        // Charge-only models must NOT send collision energy...
        assertFalse(GetPredictedOnline.needsCollisionEnergy("ms2pip_2021_HCD"));
        assertFalse(GetPredictedOnline.needsCollisionEnergy("Prosit_2020_intensity_CID"));
        assertFalse(GetPredictedOnline.needsCollisionEnergy("ms2pip_Immuno_HCD"));
        assertFalse(GetPredictedOnline.needsCollisionEnergy("ms2pip_timsTOF2024"));
        // ...while the rest do.
        assertTrue(GetPredictedOnline.needsCollisionEnergy("Prosit_2019_intensity"));
        assertTrue(GetPredictedOnline.needsCollisionEnergy("AlphaPept_ms2_generic"));
        assertTrue(GetPredictedOnline.needsCollisionEnergy("Prosit_2020_intensity_TMT"));
    }

    @Test
    public void supportsInstrument_uniSpecRestrictedToOrbitraps() {
        assertTrue(GetPredictedOnline.supportsInstrument("UniSpec", "QE"));
        assertTrue(GetPredictedOnline.supportsInstrument("UniSpec", "LUMOS"));
        assertFalse(GetPredictedOnline.supportsInstrument("UniSpec", "TIMSTOF"));
        assertFalse(GetPredictedOnline.supportsInstrument("UniSpec", "SCIEXTOF"));
        // The generic AlphaPept model accepts every instrument in the dialog list.
        assertTrue(GetPredictedOnline.supportsInstrument("AlphaPept_ms2_generic", "TIMSTOF"));
        assertTrue(GetPredictedOnline.supportsInstrument("AlphaPept_ms2_generic", "SCIEXTOF"));
        // The dialog filters its instrument dropdown from this set: UniSpec -> exactly {QE, LUMOS}.
        assertEquals(new java.util.HashSet<String>(java.util.Arrays.asList("QE", "LUMOS")),
                GetPredictedOnline.supportedInstruments("UniSpec"));
        assertNull(GetPredictedOnline.supportedInstruments("AlphaPept_ms2_generic"));
    }

    @Test
    public void generateInputString_uniSpecCarriesInstrumentAndCe() throws Exception {
        String body = new GetPredictedOnline(
                "UniSpec", "LGEYGFQNALIVR", new HashMap<Integer, String>(), "HCD", 2, 27, "QE", 700.0).getInputString();
        assertTrue(body.contains("\"instrument_types\""));
        assertTrue(body.contains("\"collision_energies\""));
        assertTrue(body.contains("\"precursor_charges\""));
        assertFalse(body.contains("\"fragmentation_types\""));
    }

    @Test
    public void generateInputString_ms2pipImmunoIsChargeOnly() throws Exception {
        String body = new GetPredictedOnline(
                "ms2pip_Immuno_HCD", "LGEYGFQNALIVR", new HashMap<Integer, String>(), "HCD", 2, 27, "QE", 700.0).getInputString();
        assertTrue(body.contains("\"peptide_sequences\""));
        assertTrue(body.contains("\"precursor_charges\""));
        assertFalse(body.contains("\"collision_energies\""));
        assertFalse(body.contains("\"instrument_types\""));
        assertFalse(body.contains("\"fragmentation_types\""));
    }

    @Test
    public void generateInputString_tmtCarriesFragmentation() throws Exception {
        String body = new GetPredictedOnline(
                "Prosit_2020_intensity_TMT", "LGEYGFQNALIVR", new HashMap<Integer, String>(), "HCD", 2, 27, "QE", 700.0).getInputString();
        assertTrue(body.contains("\"fragmentation_types\""));
        assertTrue(body.contains("\"collision_energies\""));
    }

    // ---- response parsing (#2 name-based, order independent) --------------------------------------

    private static String response(String outputsJson) {
        return "{\"id\":\"0\",\"model_name\":\"M\",\"model_version\":\"1\",\"outputs\":[" + outputsJson + "]}";
    }

    private static final String MZ_OUT =
            "{\"name\":\"mz\",\"datatype\":\"FP32\",\"shape\":[1,3],\"data\":[147.1,234.2,500.5]}";
    private static final String INT_OUT =
            "{\"name\":\"intensities\",\"datatype\":\"FP32\",\"shape\":[1,3],\"data\":[0.5,0.0,0.8]}";
    private static final String ANNO_OUT =
            "{\"name\":\"annotation\",\"datatype\":\"BYTES\",\"shape\":[1,3],\"data\":[\"y1\",\"y2\",\"b3\"]}";

    @Test
    public void parseSpectrum_keepsPositiveIntensitiesAndMapsMzCorrectly() throws Exception {
        MSnSpectrum s = GetPredictedOnline.parseSpectrum(response(MZ_OUT + "," + INT_OUT + "," + ANNO_OUT), 2, 700.0);
        assertNotNull(s);
        // The middle peak (intensity 0.0) is dropped; the other two remain.
        assertEquals(2, s.getPeakList().size());
        assertEquals(500.5, s.getMaxMz(), 1e-6);
        // m/z must map to the "mz" array, not "intensities" (anti-swap check).
        Peak p = s.getPeakMap().get(147.1);
        assertNotNull("expected a peak at m/z 147.1", p);
        assertEquals(0.5, p.intensity, 1e-6);
    }

    @Test
    public void parseSpectrum_isIndependentOfOutputOrder() throws Exception {
        // Same data, outputs in a different order: result must be identical to the canonical order.
        MSnSpectrum reordered = GetPredictedOnline.parseSpectrum(response(INT_OUT + "," + ANNO_OUT + "," + MZ_OUT), 2, 700.0);
        assertNotNull(reordered);
        assertEquals(2, reordered.getPeakList().size());
        assertEquals(500.5, reordered.getMaxMz(), 1e-6);
        assertNotNull(reordered.getPeakMap().get(147.1));
    }

    @Test
    public void parseSpectrum_handlesLeadingParametersBlock() {
        String withParams = "{\"id\":\"0\",\"model_name\":\"M\",\"model_version\":\"1\","
                + "\"parameters\":{\"sequence_id\":0,\"sequence_start\":false},"
                + "\"outputs\":[" + MZ_OUT + "," + INT_OUT + "," + ANNO_OUT + "]}";
        MSnSpectrum s = GetPredictedOnline.parseSpectrum(withParams, 2, 700.0);
        assertNotNull(s);
        assertEquals(2, s.getPeakList().size());
    }

    @Test
    public void parseSpectrum_handlesIntegerFormattedNumbers() {
        // Numbers without a decimal point are parsed by org.json as Integer, not BigDecimal; the
        // parser must still read them (a BigDecimal cast would throw ClassCastException here).
        String outputs =
                "{\"name\":\"mz\",\"datatype\":\"FP32\",\"shape\":[1,3],\"data\":[100,234.2,500]},"
                + "{\"name\":\"intensities\",\"datatype\":\"FP32\",\"shape\":[1,3],\"data\":[1,0,0.8]},"
                + ANNO_OUT;
        MSnSpectrum s = GetPredictedOnline.parseSpectrum(response(outputs), 2, 700.0);
        assertNotNull(s);
        assertEquals(2, s.getPeakList().size());                 // integer intensity 0 dropped
        assertNotNull(s.getPeakMap().get(100.0));                // integer m/z parsed
        assertEquals(1.0, s.getPeakMap().get(100.0).intensity, 1e-6); // integer intensity parsed
    }

    @Test
    public void parseSpectrum_returnsNullWhenIntensitiesMissing() {
        assertNull(GetPredictedOnline.parseSpectrum(response(MZ_OUT + "," + ANNO_OUT), 2, 700.0));
    }

    @Test
    public void parseSpectrum_returnsNullWhenMzMissing() {
        assertNull(GetPredictedOnline.parseSpectrum(response(INT_OUT + "," + ANNO_OUT), 2, 700.0));
    }

    // ---- live server integration (skipped when offline) ------------------------------------------

    @Test
    public void liveServer_predictsForEveryExposedModel() throws Exception {
        String pep = "LGEYGFQNALIVR";
        String[] models = {
                "AlphaPept_ms2_generic", "UniSpec", "ms2pip_2021_HCD", "ms2pip_Immuno_HCD",
                "ms2pip_timsTOF2024", "Prosit_2019_intensity", "Prosit_2023_intensity_timsTOF",
                "Prosit_2020_intensity_CID", "Prosit_2020_intensity_HCD", "Prosit_2020_intensity_TMT"
        };
        for (String model : models) {
            HashMap<Integer, String> mods = new HashMap<>();
            if (model.equals("Prosit_2020_intensity_TMT")) {
                mods.put(0, "[UNIMOD:737]"); // N-term TMT, required by the TMT model
            }
            MSnSpectrum s;
            try {
                s = new GetPredictedOnline(model, pep, mods, "HCD", 2, 27, "QE", 700.0).getSpectra();
            } catch (Exception e) {
                Assume.assumeNoException("Koina server unreachable; skipping live test for " + model, e);
                return;
            }
            assertNotNull("null spectrum for " + model, s);
            assertTrue("no peaks for " + model, s.getPeakList().size() > 0);
            assertTrue("m/z looks like an intensity for " + model + " (mz/intensities swapped?)",
                    s.getMaxMz() > 100.0);
        }
    }
}

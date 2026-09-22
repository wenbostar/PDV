package PDVGUI.fileimport;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for the three things {@link SpectrumLibraryTsvImport} has to get right before it can read a
 * row: the header, whose layout varies, the modified peptide grammar, and turning a modification
 * label into a PTM of the factory.
 *
 * <p>Carafe writes its tsv library in one of three notations, picked with {@code -lf_type}, and all
 * of them put the modification label next to the residue it belongs to: UniMod accessions for the
 * DIA-NN format, UniMod titles for the default one and site-qualified names for the EncyclopeDIA
 * one. The tests cover the three of them plus the terminal label every TMT-labelled peptide
 * carries. They are offline: no library file, and the modifications built into the PTM factory
 * rather than the ones PDV loads from unimod.xml at startup.</p>
 */
public class SpectrumLibraryTsvImportTest {

    private static final String CARAFE_HEADER = "ModifiedPeptide\tStrippedPeptide\tPrecursorMz\tPrecursorCharge\t"
            + "Tr_recalibrated\tProteinID\tDecoy\tFragmentMz\tRelativeIntensity\tFragmentType\tFragmentNumber\t"
            + "FragmentCharge\tFragmentLossType";

    // ---- header ----------------------------------------------------------------------------------

    @Test
    public void getColumnIndex_carafeHeader() {
        HashMap<String, Integer> columns = SpectrumLibraryTsvImport.getColumnIndexes(CARAFE_HEADER);

        assertEquals(0, index(columns, SpectrumLibraryTsvImport.MODIFIED_PEPTIDE_COLUMNS));
        assertEquals(2, index(columns, SpectrumLibraryTsvImport.PRECURSOR_MZ_COLUMNS));
        assertEquals(3, index(columns, SpectrumLibraryTsvImport.PRECURSOR_CHARGE_COLUMNS));
        assertEquals(4, index(columns, SpectrumLibraryTsvImport.RETENTION_TIME_COLUMNS));
        assertEquals(5, index(columns, SpectrumLibraryTsvImport.PROTEIN_COLUMNS));
        assertEquals(6, index(columns, SpectrumLibraryTsvImport.DECOY_COLUMNS));
        assertEquals(7, index(columns, SpectrumLibraryTsvImport.FRAGMENT_MZ_COLUMNS));
        assertEquals(8, index(columns, SpectrumLibraryTsvImport.INTENSITY_COLUMNS));
    }

    @Test
    public void getColumnIndex_optionalIonMobilityColumnShiftsTheOnesAfterIt() {
        // Carafe adds IonMobility when CCS is enabled, which is why the columns are resolved by name
        HashMap<String, Integer> columns = SpectrumLibraryTsvImport.getColumnIndexes(
                CARAFE_HEADER.replace("Tr_recalibrated\tProteinID", "Tr_recalibrated\tIonMobility\tProteinID"));

        assertEquals(4, index(columns, SpectrumLibraryTsvImport.RETENTION_TIME_COLUMNS));
        assertEquals(6, index(columns, SpectrumLibraryTsvImport.PROTEIN_COLUMNS));
        assertEquals(8, index(columns, SpectrumLibraryTsvImport.FRAGMENT_MZ_COLUMNS));
        assertEquals(9, index(columns, SpectrumLibraryTsvImport.INTENSITY_COLUMNS));
    }

    @Test
    public void getColumnIndex_diannSpellingsAreAccepted() {
        HashMap<String, Integer> columns = SpectrumLibraryTsvImport.getColumnIndexes(
                "ModifiedPeptideSequence\tPrecursorMz\tPrecursorCharge\tNormalizedRetentionTime\tProductMz\tLibraryIntensity");

        assertEquals(0, index(columns, SpectrumLibraryTsvImport.MODIFIED_PEPTIDE_COLUMNS));
        assertEquals(3, index(columns, SpectrumLibraryTsvImport.RETENTION_TIME_COLUMNS));
        assertEquals(4, index(columns, SpectrumLibraryTsvImport.FRAGMENT_MZ_COLUMNS));
        assertEquals(5, index(columns, SpectrumLibraryTsvImport.INTENSITY_COLUMNS));
    }

    @Test
    public void getColumnIndex_absentOptionalColumnIsReportedAsMinusOne() {
        HashMap<String, Integer> columns = SpectrumLibraryTsvImport.getColumnIndexes(
                "ModifiedPeptide\tPrecursorMz\tPrecursorCharge\tFragmentMz\tRelativeIntensity");

        assertEquals(-1, index(columns, SpectrumLibraryTsvImport.RETENTION_TIME_COLUMNS));
        assertEquals(-1, index(columns, SpectrumLibraryTsvImport.PROTEIN_COLUMNS));
        assertEquals(-1, index(columns, SpectrumLibraryTsvImport.DECOY_COLUMNS));
    }

    @Test
    public void getRequiredColumnIndex_missingColumnIsNamedInTheError() {
        HashMap<String, Integer> columns = SpectrumLibraryTsvImport.getColumnIndexes(
                "ModifiedPeptide\tPrecursorMz\tPrecursorCharge");

        try {
            SpectrumLibraryTsvImport.getRequiredColumnIndex(columns, SpectrumLibraryTsvImport.FRAGMENT_MZ_COLUMNS);
            fail("a library without a fragment m/z column has to be rejected");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("FragmentMz"));
        }
    }

    private static int index(HashMap<String, Integer> columns, String[] names) {
        return SpectrumLibraryTsvImport.getColumnIndex(columns, names);
    }

    // ---- modification resolution -----------------------------------------------------------------
    //
    // Against the modifications built into PTMFactory rather than the ones PDV loads from
    // unimod.xml at startup, so that the tests stay offline. The two sets name their PTMs
    // differently ("Carbamidomethylation of C" here, "Carbamidomethyl of C" from unimod.xml, and
    // "of peptide N-term" against "of N-term"), which is the point: resolution goes through the
    // cv terms, so a label has to land on a PTM under either naming.

    @Test
    public void resolvePtmName_accessionOnAResidue() {
        assertEquals("Carbamidomethylation of C", resolve("UniMod:4", 2, "ACDEK"));
        assertEquals("Oxidation of M", resolve("UniMod:35", 3, "ACMEK"));
    }

    @Test
    public void resolvePtmName_accessionOnThePeptideTerminus() {
        // the built-in modifications call this site "peptide N-term", unimod.xml calls it "N-term"
        String ptmName = resolve("UniMod:737", 0, "SGGGGGR");

        assertNotNull("the TMT label of every Carafe TMT library has to resolve", ptmName);
        assertTrue(ptmName, ptmName.endsWith(" of peptide N-term"));
    }

    @Test
    public void resolvePtmName_unimodTitleInsteadOfAnAccession() {
        // the notation Carafe's default and EncyclopeDIA formats write
        assertEquals("Oxidation of M", resolve("Oxidation", 3, "ACMEK"));
        assertEquals("Carbamidomethylation of C", resolve("Carbamidomethyl (C)", 2, "ACDEK"));
    }

    @Test
    public void resolvePtmName_encyclopediaNameThatIsNotTheUnimodTitle() {
        // "Phosphorylation (ST)" has to reach the factory's Phospho entry
        assertEquals("Phosphorylation of S", resolve("Phosphorylation (ST)", 4, "PEPSTIDEK"));
    }

    @Test
    public void resolvePtmName_unknownLabelIsLeftOutRatherThanGuessed() {
        assertNull(resolve("UniMod:999999", 2, "ACDEK"));
    }

    private static String resolve(String label, int site, String sequence) {
        return new SpectrumLibraryTsvImport()
                .resolvePtmName(new SpectrumLibraryTsvImport.LibraryModification(label, site), sequence);
    }

    // ---- peptide grammar -------------------------------------------------------------------------

    @Test
    public void parseModifiedPeptide_unmodifiedPeptideKeepsOnlyTheSequence() {
        SpectrumLibraryTsvImport.PeptideData peptideData = SpectrumLibraryTsvImport.parseModifiedPeptide("_SGGGGGR_");

        assertEquals("SGGGGGR", peptideData.sequence);
        assertEquals(0, peptideData.modifications.size());
    }

    @Test
    public void parseModifiedPeptide_terminalLabelGetsSiteZero() {
        SpectrumLibraryTsvImport.PeptideData peptideData = SpectrumLibraryTsvImport.parseModifiedPeptide("_(UniMod:737)SGGGGGR_");

        assertEquals("SGGGGGR", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals("UniMod:737", peptideData.modifications.get(0).label);
        assertEquals(0, peptideData.modifications.get(0).site);
    }

    @Test
    public void parseModifiedPeptide_residueLabelGetsTheOneBasedPosition() {
        SpectrumLibraryTsvImport.PeptideData peptideData =
                SpectrumLibraryTsvImport.parseModifiedPeptide("_(UniMod:737)AAADAK(UniMod:737)K(UniMod:737)_");

        assertEquals("AAADAKK", peptideData.sequence);
        assertEquals(3, peptideData.modifications.size());
        assertEquals(0, peptideData.modifications.get(0).site);
        assertEquals("UniMod:737", peptideData.modifications.get(1).label);
        assertEquals(6, peptideData.modifications.get(1).site);
        assertEquals("UniMod:737", peptideData.modifications.get(2).label);
        assertEquals(7, peptideData.modifications.get(2).site);
    }

    @Test
    public void parseModifiedPeptide_squareBracketsAreReadLikeRoundOnes() {
        // the DIA-NN format Carafe writes mixes the two: the modifications it hard-codes use square
        // brackets, the ones it takes from its modification table use round ones
        SpectrumLibraryTsvImport.PeptideData peptideData =
                SpectrumLibraryTsvImport.parseModifiedPeptide("_AC[UniMod:4]DM[UniMod:35]EK(UniMod:737)_");

        assertEquals("ACDMEK", peptideData.sequence);
        assertEquals(3, peptideData.modifications.size());
        assertEquals("UniMod:4", peptideData.modifications.get(0).label);
        assertEquals(2, peptideData.modifications.get(0).site);
        assertEquals("UniMod:35", peptideData.modifications.get(1).label);
        assertEquals(4, peptideData.modifications.get(1).site);
        assertEquals("UniMod:737", peptideData.modifications.get(2).label);
        assertEquals(6, peptideData.modifications.get(2).site);
    }

    @Test
    public void parseModifiedPeptide_modificationNamesAreKeptAsTheyAreWritten() {
        // default and EncyclopeDIA formats, which label with a name instead of an accession
        SpectrumLibraryTsvImport.PeptideData peptideData =
                SpectrumLibraryTsvImport.parseModifiedPeptide("_[TMT6plex]ACM[Oxidation]EK_");

        assertEquals("ACMEK", peptideData.sequence);
        assertEquals(2, peptideData.modifications.size());
        assertEquals("TMT6plex", peptideData.modifications.get(0).label);
        assertEquals(0, peptideData.modifications.get(0).site);
        assertEquals("Oxidation", peptideData.modifications.get(1).label);
        assertEquals(3, peptideData.modifications.get(1).site);
    }

    @Test
    public void parseModifiedPeptide_siteQualifiedNameKeepsItsSite() {
        SpectrumLibraryTsvImport.PeptideData peptideData =
                SpectrumLibraryTsvImport.parseModifiedPeptide("_AC[Carbamidomethyl (C)]DEK_");

        assertEquals("ACDEK", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals("Carbamidomethyl (C)", peptideData.modifications.get(0).label);
        assertEquals(2, peptideData.modifications.get(0).site);
    }

    @Test
    public void parseModifiedPeptide_underscoresAreOptional() {
        SpectrumLibraryTsvImport.PeptideData peptideData = SpectrumLibraryTsvImport.parseModifiedPeptide("AC(UniMod:4)DEK");

        assertEquals("ACDEK", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals(2, peptideData.modifications.get(0).site);
    }
}

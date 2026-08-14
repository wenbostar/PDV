package PDVGUI.fileimport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the parsing helpers of {@link SageImport}.
 *
 * <p>They cover the two Sage-specific formats PDV has to understand: the peptide grammar with inline
 * modification masses (residue, N-terminal and C-terminal) and the CSV-quoted fields Sage writes for
 * the mgf spectrum titles. Both are offline and need no spectrum file.</p>
 */
public class SageImportTest {

    // ---- peptide grammar -------------------------------------------------------------------------

    @Test
    public void parsePeptide_unmodifiedPeptide() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("DFAAVEEVAPAKPPR");

        assertEquals("DFAAVEEVAPAKPPR", peptideData.sequence);
        assertEquals(0, peptideData.modifications.size());
    }

    @Test
    public void parsePeptide_residueModificationKeepsOneBasedSite() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("NDTPC[+57.0215]GTTIGPILASR");

        assertEquals("NDTPCGTTIGPILASR", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals(57.0215, peptideData.modifications.get(0).mass, 1e-9);
        assertEquals("C", peptideData.modifications.get(0).residue);
        assertEquals(5, peptideData.modifications.get(0).position);
    }

    @Test
    public void parsePeptide_severalModificationsIncludingNegativeMass() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("Q[-17.0265]WEM[+15.9949]RTY");

        assertEquals("QWEMRTY", peptideData.sequence);
        assertEquals(2, peptideData.modifications.size());
        assertEquals(-17.0265, peptideData.modifications.get(0).mass, 1e-9);
        assertEquals(1, peptideData.modifications.get(0).position);
        assertEquals(15.9949, peptideData.modifications.get(1).mass, 1e-9);
        assertEquals("M", peptideData.modifications.get(1).residue);
        assertEquals(4, peptideData.modifications.get(1).position);
    }

    @Test
    public void parsePeptide_nTerminalModificationIsAnchoredOnTheFirstResidue() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("[+229.1629]-PEPTIDEK");

        assertEquals("PEPTIDEK", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals(229.1629, peptideData.modifications.get(0).mass, 1e-9);
        assertEquals("N-term", peptideData.modifications.get(0).residue);
        assertEquals(1, peptideData.modifications.get(0).position);
    }

    @Test
    public void parsePeptide_cTerminalModificationIsAnchoredOnTheLastResidue() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("PEPTIDEK-[+12.3456]");

        assertEquals("PEPTIDEK", peptideData.sequence);
        assertEquals(1, peptideData.modifications.size());
        assertEquals(12.3456, peptideData.modifications.get(0).mass, 1e-9);
        assertEquals("C-term", peptideData.modifications.get(0).residue);
        assertEquals(8, peptideData.modifications.get(0).position);
    }

    @Test
    public void parsePeptide_bothTerminiAndOneResidue() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("[+42.0106]-AC[+57.0215]DEK-[+0.9840]");

        assertEquals("ACDEK", peptideData.sequence);
        assertEquals(3, peptideData.modifications.size());
        // The residue modification is emitted while walking the sequence, the terminal ones at the end.
        assertEquals("C", peptideData.modifications.get(0).residue);
        assertEquals(2, peptideData.modifications.get(0).position);
        assertEquals("N-term", peptideData.modifications.get(1).residue);
        assertEquals(1, peptideData.modifications.get(1).position);
        assertEquals("C-term", peptideData.modifications.get(2).residue);
        assertEquals(5, peptideData.modifications.get(2).position);
    }

    // ---- CSV quoting of the scannr / filename fields ----------------------------------------------

    @Test
    public void unquote_plainFieldIsReturnedAsIs() {
        assertEquals("controllerType=0 controllerNumber=1 scan=36182",
                SageImport.unquote("controllerType=0 controllerNumber=1 scan=36182"));
    }

    @Test
    public void unquote_mgfTitleKeepsItsInnerQuotes() {
        String raw = "\"b1947.37069.37069.2 File:\"\"b1947.raw\"\", NativeID:\"\"controllerType=0 controllerNumber=1 scan=37069\"\"\"";
        String expected = "b1947.37069.37069.2 File:\"b1947.raw\", NativeID:\"controllerType=0 controllerNumber=1 scan=37069\"";

        assertEquals(expected, SageImport.unquote(raw));
    }

    // ---- spectrum file name matching --------------------------------------------------------------

    @Test
    public void normalizeFileName_dropsTheGzSuffix() {
        assertEquals("b1922.mzml", SageImport.normalizeFileName("b1922.mzML.gz"));
        assertEquals("b1947.mgf", SageImport.normalizeFileName("b1947.mgf"));
    }

    @Test
    public void normalizeFileName_reducesAPathToItsFileName() {
        assertEquals("b1922.mzml", SageImport.normalizeFileName("/data/raw/b1922.mzML.gz"));
        assertEquals("b1922.mzml", SageImport.normalizeFileName("D:\\data\\raw\\b1922.mzML.gz"));
        assertEquals("b1947.mgf", SageImport.normalizeFileName("./b1947.mgf"));
    }

    @Test
    public void removeExtension_keepsTheNameOfAnExtensionLessFile() {
        assertEquals("b1922", SageImport.removeExtension("b1922.mzml"));
        assertEquals("b1922", SageImport.removeExtension("b1922"));
    }

    // ---- malformed input --------------------------------------------------------------------------

    @Test
    public void parsePeptide_unclosedBracketDoesNotThrow() {
        SageImport.PeptideData peptideData = SageImport.parsePeptide("PEPC[+57.0215");

        assertEquals("PEPC", peptideData.sequence);
        assertEquals(0, peptideData.modifications.size());
    }
}

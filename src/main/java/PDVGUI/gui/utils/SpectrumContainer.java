package PDVGUI.gui.utils;

import com.compomics.util.gui.spectrum.SpectrumPanel;

/**
 * Extend spectrum panel from utilities
 */
public class SpectrumContainer extends SpectrumPanel {

    /**
     * Main consrtuctor
     * @param aXAxisData x axis data set
     * @param aYAxisData y axis data set
     * @param aPrecursorMZ precursor m/z
     * @param aPrecursorCharge precursor charge
     * @param aFileName file name
     * @param aMaxPadding max padding size
     * @param aShowFileName show file name or not
     * @param aShowPrecursorDetails show precursor details or not
     * @param aShowResolution show resolution or not
     * @param aMSLevel ms level
     * @param aProfileMode profile mode
     */
    public SpectrumContainer(double[] aXAxisData, double[] aYAxisData, double aPrecursorMZ, String aPrecursorCharge, String aFileName, int aMaxPadding, boolean aShowFileName, boolean aShowPrecursorDetails, boolean aShowResolution, int aMSLevel, boolean aProfileMode, boolean isDenovo) {
        super(aXAxisData, aYAxisData, aPrecursorMZ, aPrecursorCharge, aFileName, aMaxPadding, aShowFileName, aShowPrecursorDetails, aShowResolution, aMSLevel, aProfileMode);
        this.isDenovo = isDenovo;
        this.iYAxisLabel = "Int (%)";
    }
}

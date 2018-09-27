package PDVGUI.gui.utils;

import com.compomics.util.gui.spectrum.SpectrumPanel;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Vector;

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

    /**
     * This method adds the event listeners to the panel.
     */
    protected void addListeners() {
        this.addMouseListener(new MouseAdapter() {
            /**
             * Invoked when a mouse button has been released on a component.
             */
            public void mouseReleased(MouseEvent e) {
                if (iXAxisData != null) {
                    if (e.getButton() == MouseEvent.BUTTON3 || e.getButton() == MouseEvent.BUTTON2) {
                        if (iXAxisStartAtZero) {
                            rescale(0.0, getMaxXAxisValue());
                        } else {

                            double tempMinXValue = getMinXAxisValue();

                            if (currentGraphicsPanelType.equals(GraphicsPanelType.isotopicDistributionProfile)
                                    || currentGraphicsPanelType.equals(GraphicsPanelType.isotopicDistributionCentroid)) {
                                tempMinXValue -= 1;

                                if (tempMinXValue < 0) {
                                    tempMinXValue = 0;
                                }
                            }

                            rescale(tempMinXValue, getMaxXAxisValue());
                        }
                        iDragged = false;
                        repaint();
                    } else if (e.getButton() == MouseEvent.BUTTON1) {
                        iEndXLoc = e.getX();
                        int min = Math.min(iEndXLoc, iStartXLoc);
                        int max = Math.max(iEndXLoc, iStartXLoc);
                        double start = iXAxisMin + ((min - iXPadding) * iXScaleUnit);
                        double end = iXAxisMin + ((max - iXPadding) * iXScaleUnit);
                        if (iDragged) {
                            iDragged = false;
                            // Rescale.
                            if ((max - min) > iMinDrag) {
                                rescale(start, end);
                            }
                            iDragXLoc = 0;
                            repaint();
                        }
                    }
                }
            }

            /**
             * Invoked when a mouse button has been pressed on a component.
             */
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    iStartXLoc = e.getX();
                    iStartYLoc = e.getY();
                }
            }
        });

        this.addMouseMotionListener(new MouseMotionAdapter() {
            /**
             * Invoked when a mouse button is pressed on a component and then
             * dragged. Mouse drag events will continue to be delivered to the
             * component where they first originated until the mouse button is
             * released (regardless of whether the mouse position is within the
             * bounds of the component).
             */
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    iDragged = true;
                    iDragXLoc = e.getX();
                    repaint();
                }
            }

            /**
             * Invoked when the mouse button has been moved on a component (with
             * no buttons no down).
             */
            public void mouseMoved(MouseEvent e) {

                // see if we're above or below the x-axis
                int y = e.getY();

                int xAxisYLocation = (getHeight() - currentPadding) / 2;
                boolean aboveXAxis = y < xAxisYLocation;

                if (dataSetCounterMirroredSpectra == 0) {
                    aboveXAxis = true;
                }

                ArrayList<double[]> xAxisData;
                ArrayList<int[]> xAxisDataInPixels;
                ArrayList<int[]> yAxisDataInPixels;

                if (aboveXAxis) {
                    xAxisData = iXAxisData;
                    xAxisDataInPixels = iXAxisDataInPixels;
                    yAxisDataInPixels = iYAxisDataInPixels;
                } else {
                    xAxisData = iXAxisDataMirroredSpectrum;
                    xAxisDataInPixels = iXAxisDataInPixelsMirroredSpectrum;
                    yAxisDataInPixels = iYAxisDataInPixelsMirroredSpectrum;
                }

                if (xAxisData != null && xAxisDataInPixels != null) {
                    int x = e.getX();

                    // this variable is used to make sure that the most intense peak within range is highlighted
                    int highestPeakInRange = 0;

                    for (int j = 0; j < xAxisDataInPixels.size(); j++) {
                        for (int i = 0; i < xAxisDataInPixels.get(j).length; i++) {
                            int delta = xAxisDataInPixels.get(j)[i] - x;
                            if (Math.abs(delta) < iPointDetectionTolerance) {
                                if (aboveXAxis) {
                                    int deltaYPixels = y - yAxisDataInPixels.get(j)[i];
                                    if (deltaYPixels < 0
                                            && Math.abs(deltaYPixels) < (getHeight() - yAxisDataInPixels.get(j)[i])
                                            && highestPeakInRange < (getHeight() - yAxisDataInPixels.get(j)[i])) {
                                        iHighLight = true;
                                        iHighLightIndex = i;
                                        iHighLightDatasetIndex = j;
                                        highestPeakInRange = (getHeight() - yAxisDataInPixels.get(j)[i]);
                                        repaint();
                                    }
                                } else {
                                    int deltaYPixels = yAxisDataInPixels.get(j)[i] - y;
                                    if (deltaYPixels < 0
                                            && Math.abs(deltaYPixels) < yAxisDataInPixels.get(j)[i]
                                            && highestPeakInRange < yAxisDataInPixels.get(j)[i]) {
                                        iHighLightMirrored = true;
                                        iHighLightIndexMirrored = i;
                                        iHighLightDatasetIndexMirrored = j;
                                        highestPeakInRange = yAxisDataInPixels.get(j)[i];
                                        repaint();
                                    }
                                }
                            } else if (delta >= iPointDetectionTolerance) {
                                break;
                            }
                        }
                    }
                    repaint();
                }
            }
        });
    }
}

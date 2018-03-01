package PDVGUI.gui.utils;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.*;

import org.jfree.chart.*;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

/**
 * Line chart
 * Created by Ken on 10/15/2017.
 */
public class LineChart {

    /**
     * X cross hair
     */
    private Crosshair xCrosshair;
    /**
     * Y cross hair
     */
    private Crosshair yCrosshair;

    /**
     * Constructor
     */
    public LineChart() {
    }

    /**
     * Create chart
     * @param xyDataset XY data set
     * @param spectrumFileName Spectrum file name
     * @return Chart JPanel
     */
    public ChartPanel createChart(XYDataset xyDataset,String spectrumFileName) {
        JFreeChart chart = ChartFactory.createXYLineChart(spectrumFileName,"Elution Time","Intensity",xyDataset,PlotOrientation.VERTICAL,true,false,false);
        ChartUtils.setAntiAlias(chart);
        ChartUtils.setLineRender(chart.getXYPlot());

        CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        this.xCrosshair.setLabelVisible(true);
        this.yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        this.yCrosshair.setLabelVisible(true);
        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);

        chart.getLegend().setFrame(new BlockBorder(Color.WHITE));
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.addOverlay(crosshairOverlay);

        chartPanel.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent e) {
                if (e.getScrollType() != MouseWheelEvent.WHEEL_UNIT_SCROLL) return;
                if (e.getWheelRotation() < 0) increaseZoom((ChartPanel) e.getComponent(), true);
                else decreaseZoom((ChartPanel) e.getComponent(), true);
            }

            public synchronized void increaseZoom(JComponent chart, boolean saveAction) {
                ChartPanel ch = (ChartPanel) chart;
                zoomChartAxis(ch, true);
            }

            public synchronized void decreaseZoom(JComponent chart, boolean saveAction) {
                ChartPanel ch = (ChartPanel) chart;
                zoomChartAxis(ch, false);
            }

            private void zoomChartAxis(ChartPanel chartP, boolean increase) {
                int width = chartP.getMaximumDrawWidth() - chartP.getMinimumDrawWidth();
                int height = chartP.getMaximumDrawHeight() - chartP.getMinimumDrawWidth();
                if (increase) {
                    chartP.zoomInBoth(width / 2, height / 2);
                } else {
                    chartP.zoomOutBoth(width / 2, height / 2);
                }

            }
        });

        return chartPanel;
    }
}

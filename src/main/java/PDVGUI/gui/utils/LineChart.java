package PDVGUI.gui.utils;

import java.awt.*;

import PDVGUI.gui.MSDataDisplay;
import PDVGUI.gui.utils.Export.ExportTICDialog;
import org.jfree.chart.*;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;

/**
 * Line chart
 * Created by Ken on 10/15/2017.
 */
public class LineChart extends ChartPanel{

    /**
     * X cross hair
     */
    private Crosshair xCrosshair;
    /**
     * Y cross hair
     */
    private Crosshair yCrosshair;
    /**
     * Parent class
     */
    private MSDataDisplay msDataDisplay;

    /**
     * Constructor
     */
    public LineChart(MSDataDisplay msDataDisplay, JFreeChart chart) {
        super(chart);

        this.msDataDisplay = msDataDisplay;

        createChart();
    }

    /**
     * Create chart
     * @return Chart JPanel
     */
    private void createChart() {

        CrosshairOverlay crosshairOverlay = new CrosshairOverlay();
        this.xCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        this.xCrosshair.setLabelVisible(true);
        this.yCrosshair = new Crosshair(Double.NaN, Color.GRAY, new BasicStroke(0f));
        this.yCrosshair.setLabelVisible(true);
        crosshairOverlay.addDomainCrosshair(xCrosshair);
        crosshairOverlay.addRangeCrosshair(yCrosshair);

        this.addOverlay(crosshairOverlay);

        this.setMouseWheelEnabled(true);
    }

    public void doSaveAs() {
        new ExportTICDialog(msDataDisplay, this.getChart(), this);
    }

}

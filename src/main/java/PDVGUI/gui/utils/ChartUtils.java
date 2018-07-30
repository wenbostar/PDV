package PDVGUI.gui.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.HashMap;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.PieLabelLinkStyle;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.*;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

/**
 * Chart utils to optimize pic
 * Created by Ken on 9/15/2017.
 */
public class ChartUtils {

    /**
     * Default color
     */
    private static Color[] CHART_COLORS = {
            new Color(31,129,188), new Color(92,92,97), new Color(144,237,125), new Color(255,188,117),
            new Color(153,158,255), new Color(255,117,153), new Color(253,236,109), new Color(128,133,232),
            new Color(158,90,102),new Color(255, 204, 102) };

    static {
        setChartTheme();
    }

    /**
     * Set theme
     */
    private static void setChartTheme() {
        StandardChartTheme chartTheme = new StandardChartTheme("US");
        chartTheme.setExtraLargeFont(new Font("Arial", Font.BOLD, 14));
        chartTheme.setRegularFont(new Font("Arial", Font.PLAIN, 13));
        chartTheme.setLargeFont(new Font("Arial", Font.PLAIN, 14));
        chartTheme.setSmallFont(new Font("Arial", Font.PLAIN, 12));
        chartTheme.setTitlePaint(new Color(51, 51, 51));
        chartTheme.setSubtitlePaint(new Color(85, 85, 85));

        chartTheme.setLegendBackgroundPaint(Color.WHITE);
        chartTheme.setLegendItemPaint(Color.BLACK);
        chartTheme.setChartBackgroundPaint(Color.WHITE);

        Paint[] OUTLINE_PAINT_SEQUENCE = new Paint[] { Color.WHITE };
        DefaultDrawingSupplier drawingSupplier = new DefaultDrawingSupplier(CHART_COLORS, CHART_COLORS, OUTLINE_PAINT_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_STROKE_SEQUENCE, DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
                DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
        chartTheme.setDrawingSupplier(drawingSupplier);

        chartTheme.setPlotBackgroundPaint(Color.WHITE);
        chartTheme.setPlotOutlinePaint(Color.WHITE);
        chartTheme.setLabelLinkPaint(new Color(8, 55, 114));
        chartTheme.setLabelLinkStyle(PieLabelLinkStyle.CUBIC_CURVE);

        chartTheme.setAxisOffset(new RectangleInsets(5, 12, 5, 12));
        chartTheme.setDomainGridlinePaint(new Color(192, 208, 224));
        chartTheme.setRangeGridlinePaint(new Color(192, 192, 192));

        chartTheme.setBaselinePaint(Color.WHITE);
        chartTheme.setCrosshairPaint(Color.BLUE);
        chartTheme.setAxisLabelPaint(new Color(51, 51, 51));
        chartTheme.setTickLabelPaint(new Color(67, 67, 72));
        chartTheme.setBarPainter(new StandardBarPainter());
        chartTheme.setXYBarPainter(new StandardXYBarPainter());

        chartTheme.setItemLabelPaint(Color.black);
        chartTheme.setThermometerPaint(Color.white);

        ChartFactory.setChartTheme(chartTheme);
    }

    /**
     * set AntiAlias
     */
    public static void setAntiAlias(JFreeChart chart) {
        chart.setTextAntiAlias(true);
    }

    /**
     * Create data set
     * @param keyToRtAndInt Key to RT and intensity
     * @param mode Mode
     * @return XY dataset
     */
    public static XYDataset creatXYDataset(HashMap<String, ArrayList<float[]>> keyToRtAndInt, Integer mode, Integer topNum){
        XYSeriesCollection dataset = new XYSeriesCollection();

        for (String name : keyToRtAndInt.keySet()){
            if (mode == 0){
                XYSeries xySeries = new XYSeries(name);

                for (float[] each : keyToRtAndInt.get(name)){
                    xySeries.add(each[0], each[1]/topNum);
                }

                dataset.addSeries(xySeries);
            } else if (mode == 1 && name.equals("TIC")){
                XYSeries xySeries = new XYSeries(name);

                for (float[] each : keyToRtAndInt.get(name)){
                    xySeries.add(each[0], each[1]/topNum);
                }

                dataset.addSeries(xySeries);
            } else if (mode == 2 && name.equals("BP")){
                XYSeries xySeries = new XYSeries(name);

                for (float[] each : keyToRtAndInt.get(name)){
                    xySeries.add(each[0], each[1]);
                }

                dataset.addSeries(xySeries);
            }
        }

        return dataset;
    }

    /**
     * Create multi line data set
     * @param nameToKeyToRtAndInt File name to key to RT and intensity
     * @param mode Mode
     * @return XY data set
     */
    public static XYDataset creatMultiXYDataset(HashMap<String, HashMap<String, ArrayList<float[]>>> nameToKeyToRtAndInt, Integer mode, Integer topNum){
        XYSeriesCollection dataset = new XYSeriesCollection();

        HashMap<String, ArrayList<float[]>> keyToRtAndInt;

        float revert;

        for (String fileName : nameToKeyToRtAndInt.keySet()){

            keyToRtAndInt = nameToKeyToRtAndInt.get(fileName);

            for (String name : keyToRtAndInt.keySet()){

                if (mode == 0){
                    XYSeries xySeries = new XYSeries(fileName + name);

                    if (name.equals("TIC")){
                        for (float[] each : keyToRtAndInt.get(name)){
                            xySeries.add(each[0], each[1]/topNum);
                        }

                        dataset.addSeries(xySeries);
                    } else if (name.equals("BP")){
                        for (float[] each : keyToRtAndInt.get(name)){

                            revert = -(each[1]);

                            xySeries.add(each[0], revert);
                        }

                        dataset.addSeries(xySeries);
                    }
                } else if (mode == 1 && name.equals("TIC")){
                    XYSeries xySeries = new XYSeries(fileName + name);

                    for (float[] each : keyToRtAndInt.get(name)){
                        xySeries.add(each[0], each[1]/topNum);
                    }

                    dataset.addSeries(xySeries);
                } else if (mode == 2 && name.equals("BP")){

                    XYSeries xySeries = new XYSeries(fileName + name);

                    for (float[] each : keyToRtAndInt.get(name)){
                        xySeries.add(each[0], each[1]);
                    }

                    dataset.addSeries(xySeries);
                }
            }
        }
        return dataset;
    }

    /**
     * Set line style
     * @param plot XYPlot
     */
    public static void setLineRender(XYPlot plot) {
        plot.setInsets(new RectangleInsets(10, 10, 0, 10), false);
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

        renderer.setLegendItemToolTipGenerator(
                new StandardXYSeriesLabelGenerator("Legend {0}"));

        setXAixs(plot);
        setYAixs(plot);
    }

    /**
     * Set XY color
     * @param plot XYPlot
     */
    private static void setXAixs(XYPlot plot) {
        Color lineColor = new Color(31, 121, 170);
        plot.getDomainAxis().setAxisLinePaint(lineColor);
        plot.getDomainAxis().setTickMarkPaint(lineColor);
    }

    /**
     * Set XY color
     * @param plot XY plot
     */
    private static void setYAixs(XYPlot plot) {
        Color lineColor = new Color(192, 208, 224);
        ValueAxis axis = plot.getRangeAxis();
        axis.setAxisLinePaint(lineColor);
        axis.setTickMarkPaint(lineColor);

        axis.setAutoRange(true);
        axis.setAxisLineVisible(false);
        axis.setTickMarksVisible(false);
        plot.setRangeGridlinePaint(new Color(192, 192, 192));
        plot.setRangeGridlineStroke(new BasicStroke(1));

        plot.getRangeAxis().setUpperMargin(0.1);
        plot.getRangeAxis().setLowerMargin(0.1);
    }
}
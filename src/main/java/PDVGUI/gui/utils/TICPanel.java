package PDVGUI.gui.utils;

import PDVGUI.gui.MSDataDisplay;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import static PDVGUI.gui.utils.ChartUtils.creatMultiXYDataset;
import static PDVGUI.gui.utils.ChartUtils.creatXYDataset;

/**
 * TIC panel
 * Created by Ken on 10/26/2017.
 */
public class TICPanel extends JPanel{

    private JPanel viewJPanel;

    /**
     * Spectrum file name
     */
    private String spectrumFileName;
    /**
     * Spectrum key to RT and intensity
     */
    private HashMap<String, ArrayList<float[]>> keyToRtAndInt;
    /**
     * Parent class
     */
    private MSDataDisplay msDataDisplay;

    /**
     * Constructor
     */
    public TICPanel(MSDataDisplay msDataDisplay){
        this.msDataDisplay = msDataDisplay;

        initComponents();
    }

    /**
     * Update panel
     * @param spectrumFileName Spectrum file name
     * @param keyToRtAndInt Key to RT and intensity
     * @param mode Mode
     */
    public void updatePanel(String spectrumFileName, HashMap<String, ArrayList<float[]>> keyToRtAndInt, Integer mode, Integer topNum){

        this.spectrumFileName = spectrumFileName;
        this.keyToRtAndInt = keyToRtAndInt;

        processData(mode, topNum);
    }

    /**
     * Update panel
     * @param nameToKeyToRtAndInt File name to spectrum key to RT and intensity
     * @param mode Mode
     */
    public void updatePanel(HashMap<String, HashMap<String, ArrayList<float[]>>> nameToKeyToRtAndInt, Integer mode, Integer topNum){

        XYDataset xyDataset = creatMultiXYDataset(nameToKeyToRtAndInt, mode, topNum);

        JFreeChart chart = ChartFactory.createXYLineChart(spectrumFileName,"Elution Time","Intensity (10^" + (topNum.toString().length() - 1) +")", xyDataset, PlotOrientation.VERTICAL,true,false,false);
        ChartUtils.setAntiAlias(chart);
        ChartUtils.setLineRender(chart.getXYPlot());
        chart.getLegend().setFrame(new BlockBorder(Color.WHITE));

        viewJPanel.removeAll();

        ChartPanel chartPanel = new LineChart(msDataDisplay, chart);

        GroupLayout viewJPanelLayout = new GroupLayout(viewJPanel);
        viewJPanel.setLayout(viewJPanelLayout);

        viewJPanelLayout.setHorizontalGroup(
                viewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(viewJPanelLayout.createSequentialGroup()
                                .addComponent(chartPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        )
        );

        viewJPanelLayout.setVerticalGroup(
                viewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(viewJPanelLayout.createSequentialGroup()
                                .addComponent(chartPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                        )
        );

        viewJPanel.revalidate();
        viewJPanel.repaint();
    }

    /**
     * Init all GUI components
     */
    private void initComponents(){
        viewJPanel = new JPanel();

        viewJPanel.setOpaque(false);
        viewJPanel.setBackground(Color.white);

        GroupLayout layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(viewJPanel, GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(viewJPanel, GroupLayout.DEFAULT_SIZE, 900, Short.MAX_VALUE))
        );
    }

    /**
     * Process data
     * @param mode Mode
     */
    private void processData(Integer mode, Integer topNum){

        XYDataset xyDataset = creatXYDataset(keyToRtAndInt, mode, topNum);

        JFreeChart chart = ChartFactory.createXYLineChart(spectrumFileName,"Elution Time","Intensity (10^" + (topNum.toString().length() - 1) + ")", xyDataset, PlotOrientation.VERTICAL,true,false,false);
        ChartUtils.setAntiAlias(chart);
        ChartUtils.setLineRender(chart.getXYPlot());
        chart.getLegend().setFrame(new BlockBorder(Color.WHITE));

        viewJPanel.removeAll();

        ChartPanel chartPanel = new LineChart(msDataDisplay, chart);

        GroupLayout viewJPanelLayout = new GroupLayout(viewJPanel);
        viewJPanel.setLayout(viewJPanelLayout);

        viewJPanelLayout.setHorizontalGroup(
                viewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(viewJPanelLayout.createSequentialGroup()
                                .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                )
        );

        viewJPanelLayout.setVerticalGroup(
                viewJPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(viewJPanelLayout.createSequentialGroup()
                                .addComponent(chartPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                                )
        );

        viewJPanel.revalidate();
        viewJPanel.repaint();
    }
}

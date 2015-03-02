package com.seagate.kinetic.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class KineticStatView extends ApplicationFrame implements ActionListener {
    public static final String SYSTEM_TOTAL_IOPS_AND_THROUGHPUT_STATISTICS = "system total iops and throughput statistics";
    private static final long serialVersionUID = 1L;
    private static final int MAX_TIMESERIES_ITEM_COUNT = 240;
    private TimeSeries putOpsTs = new TimeSeries("Put kvop/s");
    private TimeSeries putTrgTs = new TimeSeries("Put MB/s");
    private TimeSeries getOpsTs = new TimeSeries("Get kvop/s");
    private TimeSeries getTrgTs = new TimeSeries("Get MB/s");
    private TimeSeries deleteOpsTs = new TimeSeries("Delete kvop/s");
    private TimeSeries deleteTrgTs = new TimeSeries("Delete MB/s");
    private JFreeChart statChart = null;
    private ChartPanel chartPanel = null;
    private JComboBox<String> orientationComboBox = null;
    private String choosenNode = SYSTEM_TOTAL_IOPS_AND_THROUGHPUT_STATISTICS;

    public void render() {
        this.pack();
        RefineryUtilities.centerFrameOnScreen(this);
        this.setVisible(true);
    }

    public KineticStatView(String s) {
        super(s);
        putOpsTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        putTrgTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        getOpsTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        getTrgTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        deleteOpsTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        deleteTrgTs.setMaximumItemCount(MAX_TIMESERIES_ITEM_COUNT);
        
        createStatChart();
    }

    public synchronized void clearTimeSeriesItems() {
        putOpsTs.clear();
        putTrgTs.clear();
        getOpsTs.clear();
        getTrgTs.clear();
        deleteOpsTs.clear();
        deleteTrgTs.clear();
    }

    public synchronized void addTimeSeriesItem(double putOps, double putTrg,
            double getOps, double getTrg, double deleteOps, double deleteTrg) {
        Millisecond now = new Millisecond();
        putOpsTs.add(now, putOps);
        putTrgTs.add(now, putTrg);
        getOpsTs.add(now, getOps);
        getTrgTs.add(now, getTrg);
        deleteOpsTs.add(now, deleteOps);
        deleteTrgTs.add(now, deleteTrg);
    }

    public synchronized void addNewNodeOption(String node) {
        orientationComboBox.addItem(node);
    }

    public String getChoosenNode() {
        return choosenNode;
    }

    public synchronized void updateChartAxisRange(double maxRangeForOps,
            double maxRangeForTrg) {
        XYPlot xyplot = (XYPlot) statChart.getPlot();
        xyplot.getRangeAxis(0).setRange(0, maxRangeForOps);
        xyplot.getRangeAxis(1).setRange(0, maxRangeForOps);
        xyplot.getRangeAxis(2).setRange(0, maxRangeForOps);
        xyplot.getRangeAxis(3).setRange(0, maxRangeForTrg);
        xyplot.getRangeAxis(4).setRange(0, maxRangeForTrg);
        xyplot.getRangeAxis(5).setRange(0, maxRangeForTrg);
    }

    private void createStatChart() {
        TimeSeriesCollection putOpsTsc = new TimeSeriesCollection(putOpsTs);
        TimeSeriesCollection putTrgTsc = new TimeSeriesCollection(putTrgTs);
        TimeSeriesCollection getOpsTsc = new TimeSeriesCollection(getOpsTs);
        TimeSeriesCollection getTrgTsc = new TimeSeriesCollection(getTrgTs);
        TimeSeriesCollection deleteOpsTsc = new TimeSeriesCollection(
                deleteOpsTs);
        TimeSeriesCollection deleteTrgTsc = new TimeSeriesCollection(
                deleteTrgTs);

        statChart = ChartFactory.createTimeSeriesChart("", "Time",
                "put kvop/s", putOpsTsc, true, true, false);
        XYPlot xyplot = (XYPlot) statChart.getPlot();
        xyplot.setOrientation(PlotOrientation.VERTICAL);
        xyplot.setDomainPannable(true);
        xyplot.setRangePannable(true);

        ValueAxis yAxis = xyplot.getDomainAxis();
        yAxis.setAutoRange(true);
        yAxis.setFixedAutoRange(60000.0);

        NumberAxis numberaxis1 = new NumberAxis("get kvop/s");
        xyplot.setRangeAxis(1, numberaxis1);
        xyplot.setDataset(1, getOpsTsc);
        xyplot.mapDatasetToRangeAxis(1, 1);
        StandardXYItemRenderer standardxyitemrenderer1 = new StandardXYItemRenderer();
        xyplot.setRenderer(1, standardxyitemrenderer1);

        NumberAxis numberaxis2 = new NumberAxis("delete kvop/s");
        numberaxis2.setAutoRangeIncludesZero(false);
        xyplot.setRangeAxis(2, numberaxis2);
        xyplot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_LEFT);
        xyplot.setDataset(2, deleteOpsTsc);
        xyplot.mapDatasetToRangeAxis(2, 2);
        StandardXYItemRenderer standardxyitemrenderer2 = new StandardXYItemRenderer();
        xyplot.setRenderer(2, standardxyitemrenderer2);

        NumberAxis numberaxis3 = new NumberAxis("put MB/s");
        xyplot.setRangeAxis(3, numberaxis3);
        xyplot.setDataset(3, putTrgTsc);
        xyplot.mapDatasetToRangeAxis(3, 3);
        StandardXYItemRenderer standardxyitemrenderer3 = new StandardXYItemRenderer();
        xyplot.setRenderer(3, standardxyitemrenderer3);

        NumberAxis numberaxis4 = new NumberAxis("get MB/s");
        numberaxis4.setAutoRangeIncludesZero(false);
        xyplot.setRangeAxis(4, numberaxis4);
        xyplot.setRangeAxisLocation(2, AxisLocation.BOTTOM_OR_LEFT);
        xyplot.setDataset(4, getTrgTsc);
        xyplot.mapDatasetToRangeAxis(4, 4);
        StandardXYItemRenderer standardxyitemrenderer4 = new StandardXYItemRenderer();
        xyplot.setRenderer(4, standardxyitemrenderer4);

        NumberAxis numberaxis5 = new NumberAxis("delete MB/s");
        xyplot.setRangeAxis(5, numberaxis5);
        xyplot.setDataset(5, deleteTrgTsc);
        xyplot.mapDatasetToRangeAxis(5, 5);
        StandardXYItemRenderer standardxyitemrenderer5 = new StandardXYItemRenderer();
        xyplot.setRenderer(5, standardxyitemrenderer5);

        ChartUtilities.applyCurrentTheme(statChart);
        xyplot.getRenderer().setSeriesPaint(0, Color.black);
        standardxyitemrenderer1.setSeriesPaint(0, Color.red);
        numberaxis1.setLabelPaint(Color.red);
        numberaxis1.setTickLabelPaint(Color.red);
        standardxyitemrenderer2.setSeriesPaint(0, Color.green);
        numberaxis2.setLabelPaint(Color.green);
        numberaxis2.setTickLabelPaint(Color.green);
        standardxyitemrenderer3.setSeriesPaint(0, Color.orange);
        numberaxis3.setLabelPaint(Color.orange);
        numberaxis3.setTickLabelPaint(Color.orange);
        standardxyitemrenderer4.setSeriesPaint(0, Color.blue);
        numberaxis4.setLabelPaint(Color.blue);
        numberaxis4.setTickLabelPaint(Color.blue);
        standardxyitemrenderer5.setSeriesPaint(0, Color.cyan);
        numberaxis5.setLabelPaint(Color.cyan);
        numberaxis5.setTickLabelPaint(Color.cyan);

        final JPanel main = new JPanel(new BorderLayout());
        final JPanel optionsPanel = new JPanel();

        String[] options = { SYSTEM_TOTAL_IOPS_AND_THROUGHPUT_STATISTICS };
        this.orientationComboBox = new JComboBox<String>(options);
        this.orientationComboBox.setSize(600, 50);
        this.orientationComboBox.addActionListener(this);
        optionsPanel.add(this.orientationComboBox);

        chartPanel = new ChartPanel(statChart);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setPreferredSize(new Dimension(900, 450));
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);

        main.add(optionsPanel, BorderLayout.NORTH);
        main.add(this.chartPanel);
        setContentPane(main);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        final Object source = evt.getSource();
        if (source == this.orientationComboBox) {
            choosenNode = this.orientationComboBox.getSelectedItem().toString();
        }
    }
}

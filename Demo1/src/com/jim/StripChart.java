package com.jim;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.RefineryUtilities;

public class StripChart extends JFrame implements WindowListener {

	static Logger log = MyLogger.get(Level.FINEST);

	private static final long serialVersionUID = 1L;

	static int windowsOpen = 0;
	StripChartPanel demoPanel;

	@Override
	public void windowOpened(WindowEvent e) {
		log.info("windowOpened");

		windowsOpen++;
	}

	@Override
	public void windowClosing(WindowEvent e) {
		log.info("windowClosing");
		demoPanel.timer.stop();
		windowsOpen--;
		if (windowsOpen <= 0) {
			// TODO fix
			System.exit(0);
		}

	}

	@Override
	public void windowClosed(WindowEvent e) {
		log.info("windowClosed");
	}

	@Override
	public void windowIconified(WindowEvent e) {
		log.info("windowIconified");
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		log.info("windowDeiconified");
	}

	@Override
	public void windowActivated(WindowEvent e) {
		log.info("windowActivated");
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		log.info("windowDeactivated");
	}


	KineticDevice device;
	
	
	public StripChart(KineticDevice device) {
		super(device.wwn+": "+device.model);
		this.stat = device.stat;
		this.device = device;
		demoPanel = new StripChartPanel();
		setContentPane(demoPanel);
		this.addWindowListener(this);
		pack();
		RefineryUtilities.positionFrameRandomly(this);
		setVisible(true);
		
	}


	public Stat stat;

	class StripChartPanel extends JPanel implements ActionListener,
			ListSelectionListener {

		private static final long serialVersionUID = 1L;

		/** The time series data. */
		private TimeSeries bytes = new TimeSeries("MB/s");
		private TimeSeries ops = new TimeSeries("kvop/s");

		// Statistics about this device

		Timer timer;

		JList<String> benchmarkChoice;
		JList<String> benchmarkChoice1;
		JList<String> valueSizeChoice;

		/**
		 * Creates a new instance.
		 */
		public StripChartPanel() {
			super(new BorderLayout());
			
			// TODO fix constant
			bytes.setMaximumItemCount(240);
			ops.setMaximumItemCount(240);

			TimeSeriesCollection mbset = new TimeSeriesCollection(this.bytes);
			TimeSeriesCollection kvset = new TimeSeriesCollection(this.ops);

			JFreeChart chart = ChartFactory.createTimeSeriesChart("", "Time",
					"MB/s", mbset);

			XYPlot plot = (XYPlot) chart.getPlot();

			ValueAxis xAxis = plot.getRangeAxis();
//			xAxis.setUpperBound(100.0);
			xAxis.setLowerBound(0.0);
			xAxis.setAutoRange(true);

			ValueAxis yAxis = plot.getDomainAxis();
			yAxis.setAutoRange(true);
			yAxis.setFixedAutoRange(60000.0);

			plot.setDataset(1, kvset);

			NumberAxis xAxis1 = new NumberAxis("KV op/s");
			xAxis1.setLowerBound(0.0);
			xAxis1.setAutoRange(true);

			plot.setRangeAxis(1, xAxis1);
			plot.mapDatasetToRangeAxis(1, 1);

			XYItemRenderer plotRenderer0 = new StandardXYItemRenderer();
			XYItemRenderer plotRenderer1 = new StandardXYItemRenderer();

			plot.setRenderer(0, plotRenderer0);
			plot.setRenderer(1, plotRenderer1);

			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

			add(chartPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			DefaultListModel<String> benchmarkList = new DefaultListModel<String>();
			benchmarkList.addElement("Sequential");
			benchmarkList.addElement("Random");

			benchmarkChoice = new JList<String>(benchmarkList);
			benchmarkChoice
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			benchmarkChoice.setSelectedIndex(1);
			benchmarkChoice.addListSelectionListener(this);
			buttonPanel.add(benchmarkChoice);

			DefaultListModel<String> benchmarkList1 = new DefaultListModel<String>();
			benchmarkList1.addElement("Read");
			benchmarkList1.addElement("Write");

			benchmarkChoice1 = new JList<String>(benchmarkList1);
			benchmarkChoice1
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			benchmarkChoice1.setSelectedIndex(1);
			buttonPanel.add(benchmarkChoice1);

			DefaultListModel<String> valueSizeList = new DefaultListModel<String>();
			valueSizeList.addElement("10 Bytes");
			valueSizeList.addElement("1M Bytes");

			valueSizeChoice = new JList<String>(valueSizeList);
			valueSizeChoice
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			valueSizeChoice.setSelectedIndex(1);
			buttonPanel.add(valueSizeChoice);

			JToggleButton a = new JToggleButton("Run");
			a.setActionCommand("RUN");
			a.addActionListener(this);
			buttonPanel.add(a);

			add(buttonPanel, BorderLayout.SOUTH);

			timer = new Timer(250, this);
			timer.setActionCommand("TIMER");
			timer.setRepeats(true);
			timer.start();

		}

		Stat last = new Stat();
		KineticBench runningBench = null;

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "TIMER":
				// TODO fix constant
				Stat delta = Stat.sub(stat, last).times(4);
				Millisecond now = new Millisecond();
				bytes.add(now, delta.bytes/1000000.0);
				ops.add(now, delta.ops);
				last = new Stat(stat);
				break;
			case "RUN":
				JToggleButton a = (JToggleButton) e.getSource();
				if (a.isSelected()) {
					if (runningBench != null) {
						if (!runningBench.isDone()) {
							log.warning("Starting bench, but not done. Ignoring");
							break;
						}
					}
					String syncAsync = benchmarkChoice.getSelectedValue();
					String readWrite = benchmarkChoice1.getSelectedValue();
					String smallLarge = valueSizeChoice.getSelectedValue();
					log.info(syncAsync + ":"
							+ readWrite + ":"
							+ smallLarge);
					
					runningBench = new KineticBench(device,syncAsync, readWrite, smallLarge);
					runningBench.execute();
					a.setText("Stop");
				} else {
					runningBench.cancel(false);
					log.info("Stop");
					a.setText("Run");
				}
				break;
			default:
				throw new Error("unknown action");
			}
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			log.info(e.toString());
		}
	}
}
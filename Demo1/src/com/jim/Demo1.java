package com.jim;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RefineryUtilities;

/**
 * A demonstration application showing a time series chart where you can
 * dynamically add (random) data by clicking on a button.
 */
public class Demo1 extends JFrame implements WindowListener {

	static Logger log = MyLogger.get(Level.FINEST, Level.INFO);

	private static final long serialVersionUID = 1L;

	static int windowsOpen = 0;
	StripChart demoPanel;

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
			DiscoveryWorker.close();
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

	/**
	 * Constructs a new demonstration application.
	 * 
	 * @param title
	 *            the frame title.
	 */
	public Demo1(String title) {
		super(title);
		demoPanel = new StripChart();
		setContentPane(demoPanel);
		this.addWindowListener(this);
	}

	static class Point {
		RegularTimePeriod x;
		Double y;

		public Point(Millisecond x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	static class KineticDevice {
		ArrayList<String> inet4 = new ArrayList<String>();
		int port = 8123;
		int tlsPort = 8443;
		String wwn = "";

		public String toString() {
			String s = "\"" + wwn + "\": ";
			s += port + ": ";
			s += tlsPort + ": {";
			String comma = "";
			for (String ip : inet4) {
				s += comma + ip;
				comma = ", ";
			}
			s += "}";
			return s;
		}
	}

	static class DiscoveryWorker extends SwingWorker<Void, KineticDevice> {

		static HashMap<String, KineticDevice> devices = new HashMap<String, KineticDevice>();
		static HashMap<String, DiscoveryWorker> workers = new HashMap<String, DiscoveryWorker>();

		static void findAll() {
			try {
				Enumeration<NetworkInterface> nets;
				nets = NetworkInterface.getNetworkInterfaces();
				for (NetworkInterface netIf : Collections.list(nets)) {
					System.out.println(netIf);
					new DiscoveryWorker("239.1.2.3", 8123, netIf).execute();
				}
			} catch (SocketException e) {
				log.warning("getNetworkInterdaces: " + e);
			}
		}

		static void close() {
			log.info("Closing multicast listeners");
			for (Entry<String, DiscoveryWorker> e : workers.entrySet()) {
				e.getValue().cancel(true);
			}

		}

		MulticastSocket s;
		String netIfName;

		DiscoveryWorker(String address, int port, NetworkInterface netIf) {
			netIfName = netIf.getDisplayName();
			try {
				workers.put(netIfName, this);
				InetAddress iadd;
				iadd = InetAddress.getByName(address);

				s = new MulticastSocket(port);
				s.setNetworkInterface(netIf);
				s.joinGroup(iadd);
			} catch (Exception e) {
				log.fine("Exception opening " + address);
			}
		}

		@Override
		protected Void doInBackground() throws Exception {

			while (true) {
				byte[] b = new byte[64 * 1024];
				DatagramPacket p = new DatagramPacket(b, b.length);
				s.receive(p);

				// String device = p.getAddress().getHostAddress();

				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readValue(p.getData(), JsonNode.class);

				String wwn = root.get("world_wide_name").asText();
				if (devices.containsKey(wwn))
					continue;

				String protocolVersion = root.get("protocol_version").asText();
				if (!protocolVersion.startsWith("3"))
					continue;

				KineticDevice dev = new KineticDevice();
				dev.port = root.get("port").asInt();
				dev.tlsPort = root.get("tlsPort").asInt();
				dev.wwn = wwn;

				JsonNode ifs = root.get("network_interfaces");

				if (!ifs.isArray())
					continue;

				for (int i = 0; i < ifs.size(); i++) {
					dev.inet4.add(ifs.get(i).get("ipv4_addr").asText());
				}

				devices.put(wwn, dev);

				publish(dev);
			}

		}

		protected void process(List<KineticDevice> devs) {
			for (KineticDevice dev : devs) {
				System.out.println(dev.toString());

				Demo1 demo = new Demo1(dev.wwn);
				demo.pack();
				// RefineryUtilities.centerFrameOnScreen(demo);
				RefineryUtilities.positionFrameRandomly(demo);
				demo.setVisible(true);
			}
		}

		protected void done() {
			// workers.remove(netIfName);
			s.close();
			log.info("closing interface: " + netIfName);
		}
	}

	static class Worker extends SwingWorker<Object, Point> {

		TimeSeries series;

		@Override
		protected Object doInBackground() throws Exception {
			Random r = new Random();
			for (int i = 0; i < 5; i++) {
				Thread.sleep(1000);
				publish(new Point(new Millisecond(), r.nextDouble()));
			}
			return null;
		}

		protected void process(List<Point> pairs) {
			for (Point p : pairs) {
				series.add(p.x, p.y);
			}
			System.out.println("-");
		}

		@Override
		public void done() {
			System.out.println("Done");
		}

		Worker(TimeSeries series) {
			this.series = series;
		}

	}

	static class StripChart extends JPanel implements ActionListener {

		private static final long serialVersionUID = 1L;

		/** The time series data. */
		private TimeSeries bytes = new TimeSeries("MB/s");
		private TimeSeries ops = new TimeSeries("kvop/s");

		/** The most recent value added. */
		private double lastValue = 100.0;

		Timer timer;

		/**
		 * Creates a new instance.
		 */
		public StripChart() {
			super(new BorderLayout());

			TimeSeriesCollection mbset = new TimeSeriesCollection(this.bytes);
			TimeSeriesCollection kvset = new TimeSeriesCollection(this.ops);

			JFreeChart chart = ChartFactory.createTimeSeriesChart("", "Time",
					"MB/s", mbset);

			XYPlot plot = (XYPlot) chart.getPlot();
			ValueAxis yAxis = plot.getDomainAxis();
			yAxis.setAutoRange(true);
			yAxis.setFixedAutoRange(60000.0);

			plot.setDataset(1, kvset);
			
			NumberAxis xAxis1 = new NumberAxis("KV op/s");
			plot.setRangeAxis(1, xAxis1);
			plot.mapDatasetToRangeAxis(1, 1);
			
			XYItemRenderer plotRenderer0 = new StandardXYItemRenderer();	
			XYItemRenderer plotRenderer1 = new StandardXYItemRenderer();
			
			plot.setRenderer(0,plotRenderer0);
			plot.setRenderer(1,plotRenderer1);


			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));

			add(chartPanel);

			JPanel buttonPanel = new JPanel();
			buttonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

			DefaultListModel<String> benchmarkList = new DefaultListModel<String>();
			benchmarkList.addElement("Sequential Write");
			benchmarkList.addElement("Sequential Read");
			benchmarkList.addElement("Random Write");
			benchmarkList.addElement("random Read");

			JList<String> benchmarkChoice = new JList<String>(benchmarkList);
			benchmarkChoice
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			buttonPanel.add(benchmarkChoice);

			DefaultListModel<String> valueSizeList = new DefaultListModel<String>();
			valueSizeList.addElement("10 Bytes");
			valueSizeList.addElement("1M Bytes");

			JList<String> valueSizeChoice = new JList<String>(valueSizeList);
			valueSizeChoice
					.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			buttonPanel.add(valueSizeChoice);

			timer = new Timer(250, this);
			timer.setActionCommand("TIMER");
			timer.setRepeats(true);
			timer.start();

			JButton button = new JButton("Start");
			button.setActionCommand("START");
			button.addActionListener(this);
			buttonPanel.add(button);

			button = new JButton("Stop");
			button.setActionCommand("STOP");
			button.addActionListener(this);
			buttonPanel.add(button);

			add(buttonPanel, BorderLayout.SOUTH);
		}

		/**
		 * Creates a sample chart.
		 * 
		 * @param dataset
		 *            the dataset.
		 * 
		 * @return A sample chart.
		 */
		private JFreeChart createChart(XYDataset dataset) {
			JFreeChart result = ChartFactory.createTimeSeriesChart("", "Time",
					"Value", dataset);
			XYPlot plot = (XYPlot) result.getPlot();

			ValueAxis xAxis = plot.getDomainAxis();
			xAxis.setAutoRange(true);
			xAxis.setFixedAutoRange(60000.0); // 60 seconds

			ValueAxis yAxis = plot.getRangeAxis();
			yAxis.setAutoRange(true);
			// axis.setRange(0.0, 200.0);

			return result;
		}

		/**
		 * Handles a click on the button by adding new (random) data.
		 * 
		 * @param e
		 *            the action event.
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			switch (e.getActionCommand()) {
			case "TIMER":
				double opps = Math.random();
				double mbps = Math.random();
				Millisecond now = new Millisecond();
				this.bytes.add(now, opps);
				this.ops.add(now, mbps);
				break;
			case "START":
				(new Worker(this.bytes)).execute();
				break;
			default:
				throw new Error("unknown action");
			}
		}
	}

	
	/**
	 * Starting point for the demonstration application.
	 * 
	 * @param args
	 *            ignored.
	 */
	public static void main(String[] args) {

		Map<String,Stat> stats = new HashMap<String,Stat>();
		
		Dial sd = new Dial("asdf", stats);

		DiscoveryWorker.findAll();
	}
}

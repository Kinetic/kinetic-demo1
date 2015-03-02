package com.seagate.kinetic.monitor;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import kinetic.admin.AdminClientConfiguration;
import kinetic.admin.KineticAdminClient;
import kinetic.admin.KineticAdminClientFactory;
import kinetic.admin.KineticLog;
import kinetic.admin.KineticLogType;
import kinetic.admin.MessageType;
import kinetic.admin.Statistics;
import kinetic.client.KineticException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import com.seagate.kinetic.heartbeat.HeartbeatMessage;
import com.seagate.kinetic.heartbeat.KineticNetworkInterface;
import com.seagate.kinetic.monitor.HeartbeatListener;
import com.seagate.kinetic.monitor.KineticStatModel.NodeStatItem;

public class KineticStatController {
	private static final String DEFAULT_MC_DESTNATION = "239.1.2.3";
	private static final double BYTES_PER_MB = 1048576; // 1024*1024
	private StatDataCollectThread dcThread;
	private boolean stop = false;
	private boolean cleanChartWhenChooseNode = true;
	private boolean useHeartbeatListener = false;

	public KineticStatController(KineticStatModel kineticStatModel,
			KineticStatView kineticStatView,
			KineticOverviewView kineticOverviewView) throws KineticException,
			IOException {
		dcThread = new StatDataCollectThread(kineticStatModel, kineticStatView,
				kineticOverviewView);
	}

	private void broadcastToDiscoverNodes(StatDataCollectThread dcThread)
			throws IOException {
		Enumeration<NetworkInterface> nets = NetworkInterface
				.getNetworkInterfaces();
		String mcastDestination = DEFAULT_MC_DESTNATION;
		int mcastPort = 8123;
		MulticastSocket multicastSocket;
		for (NetworkInterface netIf : Collections.list(nets)) {
			InetAddress iadd;
			iadd = InetAddress.getByName(mcastDestination);

			multicastSocket = new MulticastSocket(mcastPort);
			multicastSocket.setNetworkInterface(netIf);
			multicastSocket.joinGroup(iadd);
			new NodeDiscoveryThread(dcThread, multicastSocket).start();
		}
	}

	public void startCollectDataAndUpdateView() throws IOException {
		this.dcThread.start();
		if (!useHeartbeatListener) {
			broadcastToDiscoverNodes(this.dcThread);
		}else
		{
			new MyHeartbeatListner(this.dcThread);
		}
	}

	public void stopCollectDataAndUpdateView() {
		this.stop = true;
	}

	public class MyHeartbeatListner extends HeartbeatListener {
		private StatDataCollectThread dcThread;

		public MyHeartbeatListner(StatDataCollectThread dcThread)
				throws IOException {
			super();
			this.dcThread = dcThread;
			// TODO Auto-generated constructor stub
		}

		public void onMessage(byte[] data) {
			HeartbeatMessage msg = HeartbeatMessage.fromJson(new String(data)
					.trim());
			List<KineticNetworkInterface> networkItfs = msg
					.getNetworkInterfaces();
			int port = msg.getPort();
			int tlsPort = msg.getTlsPort();
			String wwn = msg.getWorldWideName();
			String model = msg.getModel();

			List<String> inet4 = new ArrayList<String>();

			for (int i = 0; i < networkItfs.size(); i++) {
				inet4.add(networkItfs.get(i).getIpV4Address());
			}

			KineticNode node;
			try {
				node = new KineticNode(inet4, port, tlsPort, wwn, model);
				dcThread.registerNode(node);
				dcThread.registerWwn(wwn);
			} catch (KineticException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public class NodeDiscoveryThread extends Thread {
		private StatDataCollectThread dcThread;
		private MulticastSocket multicastSocket;

		public NodeDiscoveryThread(StatDataCollectThread dcThread,
				MulticastSocket multicastSocket) {
			this.dcThread = dcThread;
			this.multicastSocket = multicastSocket;
		}

		public List<KineticNode> registerNewDiscoveredNodes()
				throws IOException, KineticException {
			List<KineticNode> newDiscoveredNodes = new ArrayList<KineticNode>();

			byte[] b = new byte[64 * 1024];
			DatagramPacket p = new DatagramPacket(b, b.length);
			multicastSocket.receive(p);

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readValue(p.getData(), JsonNode.class);

			String model = root.get("model").asText();

			String wwn = root.get("world_wide_name").asText();
			if (dcThread.isWwnRegistered(wwn))
				return newDiscoveredNodes;

			JsonNode ifs = root.get("network_interfaces");
			List<String> inet4 = new ArrayList<String>();
			if (!ifs.isArray()) {
				return newDiscoveredNodes;
			} else {
				for (int i = 0; i < ifs.size(); i++) {
					inet4.add(ifs.get(i).get("ipv4_addr").asText());
				}
			}

			KineticNode node = new KineticNode(inet4, root.get("port").asInt(),
					root.get("tlsPort").asInt(), wwn, model);
			dcThread.registerNode(node);
			dcThread.registerWwn(wwn);

			newDiscoveredNodes.add(node);

			return newDiscoveredNodes;
		}

		@Override
		public void run() {
			while (true) {
				try {
					registerNewDiscoveredNodes();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (KineticException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class StatDataCollectThread extends Thread {
		private static final int DATA_COLLECT_INTERVAL = 1;
		private static final double AXIS_RANGE_PEAK_HIGH_WATER_MARK_RATIO = 1.5;
		private static final double AXIS_RANGE_PEAK_LOW_WATER_MARK_RATIO = 1.2;
		private KineticStatModel kineticStatModel;
		private KineticStatView kineticStatView;
		private KineticOverviewView kineticOverviewView;
		private Map<String, KineticNode> nodes;
		private Set<String> registeredWwn;

		public StatDataCollectThread(KineticStatModel kineticStatModel,
				KineticStatView kineticStatView,
				KineticOverviewView kineticOverviewView)
				throws KineticException, IOException {
			this.kineticStatModel = kineticStatModel;
			this.kineticStatView = kineticStatView;
			this.kineticOverviewView = kineticOverviewView;
			nodes = new HashMap<String, KineticNode>();
			registeredWwn = new HashSet<String>();

		}

		public NodeStatItem getNodeStat(String node) {
			return null;
		}

		public synchronized void registerWwn(String wwn) {
			registeredWwn.add(wwn);
		}

		public synchronized boolean isWwnRegistered(String wwn) {
			return registeredWwn.contains(wwn);
		}

		public synchronized void registerNode(KineticNode node) {
			if (!nodes.containsKey(node.toString())) {
				nodes.put(node.toString(), node);
				kineticStatView.addNewNodeOption(node.toString());
			}
		}

		@Override
		public void run() {
			String choosenNode = kineticStatView.getChoosenNode();
			double maxOps = 0;
			double maxOpsSetForYAxis = 0;
			double maxTrg = 0;
			double maxTrgSetForYAxis = 0;
			while (!stop) {
				synchronized (this) {
					NodeStatItem nodeStatItem = null;
					for (String node : nodes.keySet()) {
						try {
							kineticStatModel.updateNodeStat(node,
									nodes.get(node).getLatestNodeStat());
						} catch (Exception e) {
							e.printStackTrace();
							nodes.get(node).clinetReconnect();
						}

						nodeStatItem = kineticStatModel.getAvgNodeStat(node);
						kineticOverviewView.updateDataSet(node,
								nodeStatItem.getTotalPutTimes(),
								nodeStatItem.getTotalGetTimes(),
								nodeStatItem.getTotalDeleteTimes());
					}

					if (!choosenNode.equals(kineticStatView.getChoosenNode())) {
						if (cleanChartWhenChooseNode) {
							kineticStatView.clearTimeSeriesItems();
						}
						choosenNode = kineticStatView.getChoosenNode();
					}

					if (choosenNode
							.equals(KineticStatView.SYSTEM_TOTAL_IOPS_AND_THROUGHPUT_STATISTICS)) {
						nodeStatItem = kineticStatModel.getAvgSystemStat();
					} else {
						nodeStatItem = kineticStatModel
								.getAvgNodeStat(choosenNode);
					}

					double putOps = nodeStatItem.getTotalPutTimes();
					double putTrg = nodeStatItem.getTotalPutBytes()
							/ BYTES_PER_MB;
					double getOps = nodeStatItem.getTotalGetTimes();
					double getTrg = nodeStatItem.getTotalGetBytes()
							/ BYTES_PER_MB;
					double deleteOps = nodeStatItem.getTotalDeleteTimes();
					double deleteTrg = nodeStatItem.getTotalDeleteBytes()
							/ BYTES_PER_MB;

					maxOps = maxOps > putOps ? maxOps : putOps;
					maxOps = maxOps > getOps ? maxOps : getOps;
					maxOps = maxOps > deleteOps ? maxOps : deleteOps;
					maxTrg = maxTrg > putTrg ? maxTrg : putTrg;
					maxTrg = maxTrg > getTrg ? maxTrg : getTrg;
					maxTrg = maxTrg > deleteTrg ? maxTrg : deleteTrg;

					if (maxOps != 0 && maxTrg != 0) {
						if (maxOps >= AXIS_RANGE_PEAK_LOW_WATER_MARK_RATIO
								* maxOpsSetForYAxis
								|| maxTrg >= AXIS_RANGE_PEAK_LOW_WATER_MARK_RATIO
										* maxTrgSetForYAxis) {
							maxOpsSetForYAxis = maxOps;
							maxTrgSetForYAxis = maxTrg;
							kineticStatView.updateChartAxisRange(
									AXIS_RANGE_PEAK_HIGH_WATER_MARK_RATIO
											* maxOpsSetForYAxis,
									AXIS_RANGE_PEAK_HIGH_WATER_MARK_RATIO
											* maxTrgSetForYAxis);
						}
					}

					kineticStatView.addTimeSeriesItem(putOps, putTrg, getOps,
							getTrg, deleteOps, deleteTrg);
				}
				try {
					TimeUnit.SECONDS.sleep(DATA_COLLECT_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
	}
}

class KineticNode {
	private List<String> inet4;
	private int port = 8123;
	private int tlsPort = 8443;
	private String wwn = "";
	private String model = "";
	private KineticAdminClient adminClient;
	private AdminClientConfiguration adminClientConfig;

	public KineticNode(List<String> inet4, int port, int tlsPort, String wwn,
			String model) throws KineticException {
		super();
		assert (inet4.size() > 0);
		this.inet4 = inet4;
		this.port = port;
		this.tlsPort = tlsPort;
		this.wwn = wwn;
		this.model = model;
		adminClientConfig = new AdminClientConfiguration();
		adminClientConfig.setHost(inet4.get(0));
		adminClientConfig.setUseSsl(false);
		adminClientConfig.setPort(port);
		adminClientConfig.setRequestTimeoutMillis(1000);
		adminClient = KineticAdminClientFactory
				.createInstance(adminClientConfig);
	}

	public void clinetReconnect() {
		try {
			adminClient.close();
			adminClient = KineticAdminClientFactory
					.createInstance(adminClientConfig);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append(inet4.get(0));
		sb.append(":");
		sb.append(port);
		sb.append("(tls:" + tlsPort + ",wwn" + wwn + ",model" + model + ")");
		return sb.toString();
	}

	public KineticStatModel.NodeStatItem getLatestNodeStat()
			throws KineticException {
		List<KineticLogType> listOfLogType = new ArrayList<KineticLogType>();
		listOfLogType.add(KineticLogType.STATISTICS);
		KineticLog kineticLog = adminClient.getLog(listOfLogType);
		List<Statistics> stats = kineticLog.getStatistics();
		double totalPutTimes = 0;
		double totalPutBytes = 0;
		double totalGetTimes = 0;
		double totalGetBytes = 0;
		double totalDeleteTimes = 0;
		double totalDeleteBytes = 0;
		for (Statistics stat : stats) {
			MessageType messageType = stat.getMessageType();
			if (null != messageType) {
				switch (messageType) {
				case PUT:
					totalPutTimes = stat.getCount();
					totalPutBytes = stat.getBytes();
				case GET:
					totalGetTimes = stat.getCount();
					totalGetBytes = stat.getBytes();
				case DELETE:
					totalDeleteTimes = stat.getCount();
					totalDeleteBytes = stat.getBytes();
				default:
					continue;
				}
			}
		}

		NodeStatItem noteStatItem = new NodeStatItem(totalPutTimes,
				totalPutBytes, totalGetTimes, totalGetBytes, totalDeleteTimes,
				totalDeleteBytes);
		return noteStatItem;
	}
}

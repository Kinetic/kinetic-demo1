package com.jim;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import kinetic.client.ClientConfiguration;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class KineticDiscovery extends SwingWorker<Void, KineticDevice> {

	static Logger log = MyLogger.get(Level.INFO);

	static HashMap<String, KineticDevice> devices = new HashMap<String, KineticDevice>();
	static HashMap<String, KineticDiscovery> workers = new HashMap<String, KineticDiscovery>();

	static Map<String, Stat> stats;
	
	static void findAll(Map<String, Stat> s) {
		stats = s;
		try {
			Enumeration<NetworkInterface> nets;
			nets = NetworkInterface.getNetworkInterfaces();
			for (NetworkInterface netIf : Collections.list(nets)) {
				System.out.println(netIf);
				new KineticDiscovery("239.1.2.3", 8123, netIf).execute();
			}
		} catch (SocketException e) {
			log.warning("getNetworkInterdaces: " + e);
		}
	}

	static void close() {
		log.info("Closing multicast listeners");
		for (Entry<String, KineticDiscovery> e : workers.entrySet()) {
			e.getValue().cancel(true);
		}

	}

	MulticastSocket s;
	String netIfName;

	KineticDiscovery(String address, int port, NetworkInterface netIf) {
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

			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readValue(p.getData(), JsonNode.class);

			String wwn = root.get("world_wide_name").asText();
			
			String model = root.get("model").asText();
			
			if (devices.containsKey(wwn))
				continue;

			log.fine(new String(p.getData()));
			
			String protocolVersion = root.get("protocol_version").asText();
			
			// get the high level version information.
			// TODO don't like this code.
			String mypv = ClientConfiguration.getProtocolVersion().split("\\.")[0];
			String pv = protocolVersion.split("\\.")[0];
			if (!pv.equals(mypv)) {
				log.warning("Drive protocol version "+pv+" is not compatible with the library version "+mypv+", ignoring.");
				continue;
			}

			KineticDevice dev = new KineticDevice();
			dev.port = root.get("port").asInt();
			dev.tlsPort = root.get("tlsPort").asInt();
			dev.wwn = wwn;
			dev.model = model;

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
			log.info(dev.toString());
			new StripChart(dev);
			stats.put(dev.wwn, dev.stat);
		}
	}

	protected void done() {
		// workers.remove(netIfName);
		s.close();
		log.info("closing interface: " + netIfName);
	}
}

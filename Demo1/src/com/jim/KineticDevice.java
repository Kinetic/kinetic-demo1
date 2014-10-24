package com.jim;

import java.util.ArrayList;

class KineticDevice {
	ArrayList<String> inet4 = new ArrayList<String>();
	int port = 8123;
	int tlsPort = 8443;
	String wwn = "";
	String model = "";
	Stat stat = new Stat();
	
	public String toString() {
		String s = "\"" + wwn + "\": ";
		s += port + ": ";
		s += tlsPort + ": {";
		String comma = "";
		for (String ip : inet4) {
			s += comma + ip;
			comma = ", ";
		}
		s += "}: "+stat.toString();
		return s;
	}
}
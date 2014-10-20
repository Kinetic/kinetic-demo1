package com.jim;

public class Stat {
	long bytes = 0;
	long ops = 0;
	String deviceName;
	
	void add(Stat s) {
		bytes += s.bytes;
		ops += s.ops;
	}
	
	void sub(Stat s) {
		bytes -= s.bytes;
		ops -= s.ops;
	}
	
	static Stat add(Stat s1, Stat s2) {
		Stat s = new Stat();
		s.add(s1);
		s.add(s2);
		return s;
	}
	
	static Stat sub(Stat s1, Stat s2) {
		Stat s = new Stat();
		s.add(s1);
		s.sub(s2);
		return s;
	}
	
	Stat (String s) {
		deviceName = s;
	}

	Stat () {
		deviceName = "";
	}
}

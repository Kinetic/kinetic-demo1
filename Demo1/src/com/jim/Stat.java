package com.jim;

public class Stat {
	long bytes = 0;
	long ops = 0;
	String deviceName;
	
	Stat add(Stat s) {
		bytes += s.bytes;
		ops += s.ops;
		return this;
	}
	
	Stat sub(Stat s) {
		bytes -= s.bytes;
		ops -= s.ops;
		return this;
	}
	
	Stat times(int i) {
		bytes *= i;
		ops *= i;
		return this;
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
	
	// deep copy
	Stat (Stat s) {
		this.bytes = s.bytes;
		this.ops = s.ops;
		this.deviceName = s.deviceName;
	}

	Stat () {
		deviceName = "";
	}
	
	Stat(int i, int j) {
		bytes = i;
		ops = j;
	}
	
	public String toString() {
		return "{"+deviceName+","+ops+","+bytes+"}";
	}
	
	@Override
	public boolean equals(Object x) {
		if (x instanceof Stat) {
			Stat s = (Stat)x;
			return (s.bytes == this.bytes)&&(s.ops == this.ops);
		}
		return false;
	}
}

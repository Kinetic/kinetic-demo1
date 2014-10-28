package com.jim;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A demonstration application showing a time series chart where you can
 * dynamically add (random) data by clicking on a button.
 */
class KineticDemo1 {

	static Logger log = MyLogger.get(Level.INFO, Level.INFO);
	
	public static void main(String[] args) throws Exception {

		Map<String, Stat> stats = new HashMap<String, Stat>();
		new Dial("asdf", stats);
		KineticDiscovery.findAll(stats);
		
		
	}
}

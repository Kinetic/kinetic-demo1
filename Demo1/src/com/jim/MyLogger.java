package com.jim;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;



public class MyLogger { 

	private static String thisClass(int n) {
		StackTraceElement[] ste = new RuntimeException().getStackTrace();
		if (n>ste.length)
			throw new Error("depth incorrect");
		return ste[n].getClassName();
	}

	public static Logger get(Level thisClassLevel, Level rootLevel) {
		Logger log = Logger.getLogger(thisClass(1));
		log.setLevel(thisClassLevel);
		log.warning("SetLogLevel: "+thisClassLevel.getName());
		root.setLevel(rootLevel);
		log.warning("SetRootLogLevel: "+rootLevel.getName());
		return log;
	}

	public static Logger get(Level thisClassLevel) {
		Logger log = Logger.getLogger(thisClass(1));
		log.setLevel(thisClassLevel);
		log.warning("SetLogLevel: "+thisClassLevel.getName());
		return log;
	}

	public static Logger get() {
		Logger log = Logger.getLogger(thisClass(1));
		return log;
	}

	
	static class myFormatter extends Formatter {

		static final DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");

		public String format(LogRecord record) {
			
			Date date = new Date(record.getMillis());
			
			return formatter.format(date) + ":  " + record.getLevel() + ":  "
					+ record.getSourceClassName() + ": "
					+ record.getSourceMethodName() + ": " + record.getMessage()
					+ "\n";
		}
	}
	
	private static Logger root = Logger.getLogger("");
	
	static {		
		Handler[] handlers = root.getHandlers();
		for (Handler h:handlers)
			root.removeHandler(h);
		// log.setUseParentHandlers(false);
		Handler conHdlr = new ConsoleHandler();
		conHdlr.setFormatter(new myFormatter());
		conHdlr.setLevel(Level.FINEST);
		root.addHandler(conHdlr);
		root.setLevel(Level.FINEST);
	}

	private static long baseNano  = System.nanoTime();
	private static long baseMilli = System.currentTimeMillis();
	static {
		long n1,n3,n5;
		long m2, m4;
		long dt = Integer.MAX_VALUE;
		for (int i=0;i<20;i++) {
			do {
				n1=System.nanoTime();
				m2=System.currentTimeMillis();
				n3=System.nanoTime();
				m4=System.currentTimeMillis();
				n5=System.nanoTime();				
			} while (m2==m4);
			long dt1 = n5-n1;
			if (dt1 < dt) {
				baseMilli = m4;
				baseNano = n3;
				dt = dt1;
			}
		}
	}
	
	public static double nanoTime() {
		double t = (System.nanoTime()-baseNano)/1e9;
		return (baseMilli/1e3)+t;
	}
}
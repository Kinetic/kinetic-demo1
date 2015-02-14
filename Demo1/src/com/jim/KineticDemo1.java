package com.jim;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * A demonstration application showing a time series chart where you can
 * dynamically add (random) data by clicking on a button.
 */
class KineticDemo1 {

	static Logger log = MyLogger.get(Level.FINEST, Level.INFO);

	// added to allow increasing the worker threads. 
	@SuppressWarnings("restriction")
	public static void main(String[] args) throws Exception {

		{
			// The following code was suggested from
			// http://stackoverflow.com/questions/8356784/whats-the-maximum-number-of-swing-worker-threads-that-can-be-run
			final int corePoolSize = 100;
			final int maximumPoolSize = 100;
			final long keepAliveTime = 100000;
			final TimeUnit unit = TimeUnit.SECONDS;
			final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(maximumPoolSize);
			sun.awt.AppContext.getAppContext().put(SwingWorker.class, new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue));
		}
		
		// Choose a L&F
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            // log.finest(()->{return "Installed L&F: " + info.getName();});
			if ("Mac OS X".equals(info.getName())) {
				UIManager.setLookAndFeel(info.getClassName());
			}
		}

		Map<String, Stat> stats = new HashMap<String, Stat>();
		new Dial("System Throughput", stats);
		KineticDiscovery.findAll(stats);

	}
}

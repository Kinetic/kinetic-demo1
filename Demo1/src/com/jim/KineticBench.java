package com.jim;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import kinetic.client.AsyncKineticException;
import kinetic.client.CallbackHandler;
import kinetic.client.CallbackResult;
import kinetic.client.ClientConfiguration;
import kinetic.client.Entry;
import kinetic.client.EntryMetadata;
import kinetic.client.advanced.AdvancedKineticClient;
import kinetic.client.advanced.AdvancedKineticClientFactory;
import kinetic.client.advanced.PersistOption;

public class KineticBench extends SwingWorker<Object, Object> {

	static Logger log = MyLogger.get(Level.FINEST);

	KineticDevice dev;
	Stat stat;

	boolean sequential;
	boolean write;
	int length;

	AdvancedKineticClient c;

	class Outstanding {

		int count = 0;

		synchronized void initiate(int n) {
			drain(n);
			count++;
		}

		synchronized void complete() {
			count--;
			this.notifyAll();
			if (count < 0)
				throw new Error("negative count");
		}

		synchronized void drain() {
			drain(0);
		}

		synchronized void drain(int n) {
			try {
				while (count > n) {
					this.wait(30000);
				}
			} catch (InterruptedException e) {
				throw new Error(e);
			}
		}
	}

	Outstanding outstanding = new Outstanding();

	class Callback implements CallbackHandler<Entry> {

		@Override
		public void onSuccess(CallbackResult<Entry> result) {
			outstanding.complete();
			long l = 0;
			Entry e = result.getResult();
			if (e == null) {
				log.fine("not found. ");
			} else {
				l = e.getValue().length;
				if (l != length) {
					log.severe("incorrect length " + l + " " + length);
				}
			}
			dev.stat.ops++;
			dev.stat.bytes += l;
		}

		@Override
		public void onError(AsyncKineticException exception) {
			outstanding.complete();
			log.warning(exception.toString());
		}
	}

	Callback callback = new Callback();

	long loop;
	long prev;

	private byte[] nextKey() {
		long x = prev;
		if (sequential) {
			x++;
		} else {
			long p = 1299709;
			long a = 546677;
			x = (a * x) % p;
		}
		prev = x;
		return String.format("%s%s%010d", sequential ? "s" : "r",
				length > 10 ? "l" : "s", x).getBytes();
	}

	@Override
	protected Object doInBackground() {
		try {
			String host = dev.inet4.get(0);
			ClientConfiguration cc = new ClientConfiguration();
			cc.setHost(host);
			log.finest("Connecting to " + host);
			c = AdvancedKineticClientFactory.createAdvancedClientInstance(cc);
			log.finest("Connected  to " + host);
			if (c == null)
				throw new Error("really?");

			loop = 0;
			prev = 2; // this can not be 0 or 1.

			while (!this.isCancelled()) {
				loop++;
				byte[] key = nextKey();
				log.fine(new String(key));
				outstanding.initiate(4);
				if (write) {
					EntryMetadata em = new EntryMetadata();
					Entry e = new Entry(key, new byte[length], em);
					c.putForcedAsync(e, PersistOption.ASYNC, callback);
				} else {
					c.getAsync(key, callback);
				}
			}
			outstanding.drain();
			c.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void done() {
		System.out.println("Done!");
	}

	KineticBench(KineticDevice dev, String order, String rw, String size) {
		this.dev = dev;
		this.stat = dev.stat;

		switch (order) {
		case "Sequential":
			sequential = true;
			break;
		case "Random":
			sequential = false;
			break;
		default:
			throw new Error("oops");
		}

		switch (rw) {
		case "Read":
			write = false;
			break;
		case "Write":
			write = true;
			break;
		default:
			throw new Error("oops");
		}

		switch (size) {
		case "10 Bytes":
			length = 10;
			break;
		case "1M Bytes":
			length = 1024 * 1024;
			break;
		default:
			throw new Error("oops");
		}
	}
}

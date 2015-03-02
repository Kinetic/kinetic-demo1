package com.seagate.kinetic.monitor;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import kinetic.client.KineticException;

public class KineticStatDemo {
	public static void main(String[] args) throws InterruptedException,
			KineticException, IOException {
		KineticStatModel kineticStatModel = new KineticStatModel();
		KineticStatView kineticStatView = new KineticStatView(
				"Kinetic Stat Demo");
		KineticOverviewView kineticOverviewView = new KineticOverviewView(
				"Kinetic Drives Overview");
		KineticStatController kineticStatController = new KineticStatController(
				kineticStatModel, kineticStatView, kineticOverviewView);

		kineticStatController.startCollectDataAndUpdateView();

		TimeUnit.SECONDS.sleep(2);
		kineticStatView.render();
		kineticOverviewView.render();
	}
}

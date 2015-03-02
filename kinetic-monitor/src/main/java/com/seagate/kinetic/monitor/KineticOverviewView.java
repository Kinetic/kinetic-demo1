package com.seagate.kinetic.monitor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.GradientPaintTransformType;
import org.jfree.ui.RefineryUtilities;
import org.jfree.ui.StandardGradientPaintTransformer;

public class KineticOverviewView extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	private DefaultCategoryDataset defaultcategorydataset = new DefaultCategoryDataset();
	private GroupedStackedBarRenderer groupedstackedbarrenderer = new GroupedStackedBarRenderer();
	private KeyToGroupMap keytogroupmap = new KeyToGroupMap("G1");

	public KineticOverviewView(String s) {
		super(s);
		JPanel jpanel = new ChartPanel(createChart(defaultcategorydataset));
		jpanel.setPreferredSize(new Dimension(590, 350));
		setContentPane(jpanel);
	}

	public void render() {
		this.pack();
		RefineryUtilities.centerFrameOnScreen(this);
		this.setVisible(true);
	}

	public synchronized void updateDataSet(String node, double putOps,
			double getOps, double deleteOps) {
		String ipPlusPort = node.substring(0, node.indexOf("("));
		defaultcategorydataset.addValue(getOps, "Get", ipPlusPort);
		defaultcategorydataset.addValue(putOps, "Put", ipPlusPort);
		defaultcategorydataset.addValue(deleteOps, "Delete", ipPlusPort);
	}

	private JFreeChart createChart(CategoryDataset categorydataset) {
		JFreeChart jfreechart = ChartFactory.createStackedBarChart("",
				"", "Total Ops/s", categorydataset,
				PlotOrientation.HORIZONTAL, true, true, false);
		keytogroupmap = new KeyToGroupMap("G1");
		keytogroupmap.mapKeyToGroup("Get", "G1");
		keytogroupmap.mapKeyToGroup("Put", "G1");
		keytogroupmap.mapKeyToGroup("Delete", "G1");
		groupedstackedbarrenderer.setSeriesToGroupMap(keytogroupmap);
		groupedstackedbarrenderer.setItemMargin(0.10000000000000001D);
		groupedstackedbarrenderer.setDrawBarOutline(false);

		GradientPaint gradientpaint = new GradientPaint(0.0F, 0.0F, new Color(
				34, 34, 255), 0.0F, 0.0F, new Color(136, 136, 255));
		groupedstackedbarrenderer.setSeriesPaint(0, gradientpaint);
		groupedstackedbarrenderer.setSeriesPaint(4, gradientpaint);
		groupedstackedbarrenderer.setSeriesPaint(8, gradientpaint);
		GradientPaint gradientpaint1 = new GradientPaint(0.0F, 0.0F, new Color(
				34, 255, 34), 0.0F, 0.0F, new Color(136, 255, 136));
		groupedstackedbarrenderer.setSeriesPaint(1, gradientpaint1);
		groupedstackedbarrenderer.setSeriesPaint(5, gradientpaint1);
		groupedstackedbarrenderer.setSeriesPaint(9, gradientpaint1);
		GradientPaint gradientpaint2 = new GradientPaint(0.0F, 0.0F, new Color(
				255, 34, 34), 0.0F, 0.0F, new Color(255, 136, 136));
		groupedstackedbarrenderer.setSeriesPaint(2, gradientpaint2);
		groupedstackedbarrenderer.setSeriesPaint(6, gradientpaint2);
		groupedstackedbarrenderer.setSeriesPaint(10, gradientpaint2);
		GradientPaint gradientpaint3 = new GradientPaint(0.0F, 0.0F, new Color(
				255, 255, 34), 0.0F, 0.0F, new Color(255, 255, 136));
		groupedstackedbarrenderer.setSeriesPaint(3, gradientpaint3);
		groupedstackedbarrenderer.setSeriesPaint(7, gradientpaint3);
		groupedstackedbarrenderer.setSeriesPaint(11, gradientpaint3);
		groupedstackedbarrenderer
				.setGradientPaintTransformer(new StandardGradientPaintTransformer(
						GradientPaintTransformType.HORIZONTAL));

		return jfreechart;
	}
}

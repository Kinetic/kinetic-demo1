package com.jim;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.dial.DialPlot;
import org.jfree.chart.plot.dial.DialPointer;
import org.jfree.chart.plot.dial.DialValueIndicator;
import org.jfree.chart.plot.dial.StandardDialFrame;
import org.jfree.chart.plot.dial.StandardDialScale;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.RefineryUtilities;

/**
 * Two dials on one panel.
 */
public class Dial extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	DemoPanelA mbsPanel;
	DemoPanelA opsPanel;
	
	Map<String,Stat> stats;
	
	Stat last = new Stat();

	Timer timer;

	class DemoPanelA extends JPanel {

		private static final long serialVersionUID = 1L;

		DefaultValueDataset dataset;


		public DemoPanelA(int max, String name) {
			super(new BorderLayout());
		
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

			this.dataset = new DefaultValueDataset(10.0);
		
			DialPlot plot = new DialPlot();
			plot.setDataset(dataset);

			plot.setDialFrame(new StandardDialFrame());

			DialValueIndicator dvi = new DialValueIndicator(0);
			plot.addLayer(dvi);

			StandardDialScale scale = new StandardDialScale(0.0, max,
					240.0, -300.0, max/15, 4);
			plot.addScale(0, scale);

			plot.addPointer(new DialPointer.Pin());

			ChartPanel cp = new ChartPanel(new JFreeChart(plot));
			cp.setPreferredSize(new Dimension(400, 400));
			add(cp);
			
			JButton textArea = new JButton(name);
			textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
			Font f = textArea.getFont().deriveFont((float) 40.0);
			textArea.setFont(f);
			add(textArea);
			
		}
	}
	


	@Override
	public void actionPerformed(ActionEvent e) {
		switch (e.getActionCommand()) {
		case "TIMER":
			Stat next = new Stat();
			for (Stat s: stats.values()) {
				next.add(s);
			}
			Stat diff = Stat.sub(next,last);
			last = next;
			// TODO change the time constants to value
			mbsPanel.dataset.setValue(diff.bytes*(1.0/1000000.0));
			opsPanel.dataset.setValue(diff.ops*1.0);
			break;
		default:
			throw new Error("Unknown action");
		}
	}
	
	Dial(String title, Map<String,Stat> stats) {
		super(title);

		this.stats = stats;
		
		JPanel panel = new JPanel(new GridLayout(1, 2));
		panel.add(mbsPanel = new DemoPanelA(150, "MB/s"));
		panel.add(opsPanel = new DemoPanelA(1500, "KVop/s"));

		setContentPane(panel);

		// TODO change the time constants to value
		timer = new Timer(1000, this);
		timer.setActionCommand("TIMER");
		timer.setRepeats(true);
		timer.start();


		pack();
		RefineryUtilities.positionFrameRandomly(this);
		setVisible(true);
	}
}

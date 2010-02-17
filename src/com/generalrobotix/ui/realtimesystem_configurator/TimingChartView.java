package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleEdge;

import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.TreeModelItem;

public class TimingChartView extends ViewPart {
	private final static String XAXIS_LABEL = "Time[msec]";
	List<JFreeChart> chartList = new ArrayList<JFreeChart>();
	private Composite parent;
	public TimingChartView() {
	}

	@Override
	public void createPartControl(final Composite parent) {
		this.parent = parent;
		parent.setLayout( new GridLayout(1, false));
		updateChart(null, 0);

		getSite().getPage().addSelectionListener(new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part != TimingChartView.this &&	 selection instanceof IStructuredSelection) {
					List sel = ((IStructuredSelection) selection).toList();
					if ( sel.size()>0 && sel.get(0) instanceof TreeModelItem ) {
						TreeModelItem root = ((TreeModelItem)sel.get(0)).getRoot();
						TreeModelItem[] checkedItems = root.getCheckedItems();
						int index = 0;
						for (int i=0; i<checkedItems.length; i++) {
							TreeModelItem item = checkedItems[i];
							if ( item instanceof ExecutionContextItem ) {	
								updateChart((ExecutionContextItem)item, index++);
							}
						}
					}
				}
			}
		});
	}
	
	private void updateChart(ExecutionContextItem ecItem, int index) {
		if ( chartList.size() < index + 1 ) {
			chartList.add(createChart(parent));
		}
		
		if ( ecItem == null ) {
			Iterator<JFreeChart> charts = chartList.iterator();
			while ( charts.hasNext() ) {
				JFreeChart chart = charts.next();
			}
			return;
		}
		
		JFreeChart chart = chartList.get(index);
		XYPlot plot = chart.getXYPlot();
		chart.setTitle(ecItem.getName());
		Iterator<TreeModelItem> rtcs = ecItem.getChildren().iterator();
		int t1 = 0;
		
		TimeSeriesCollection dataset = new TimeSeriesCollection();
		while ( rtcs.hasNext() ) {
			RTComponentItem rtc = (RTComponentItem)rtcs.next();
			int t2 = (int)(rtc.getResult().max*1000.0) + 1;
			
			TimeSeries tseries = new TimeSeries(rtc.getName(), FixedMillisecond.class);
			tseries.add(new FixedMillisecond(t1), 1);
			tseries.add(new FixedMillisecond(t2), 1);
			t1 += t2;

			dataset.addSeries(tseries);
		}
		plot.setDataset(dataset);
		
		double cycle = 1.0/ecItem.getComponent().getExecutionContexts().get(0).getRate()*1000.0;
		plot.getDomainAxis().setRange(0.0, cycle);
		//TimeSeries cycle = new TimeSeries("cycle", FixedMillisecond.class);
		//cycle.add(new FixedMillisecond(5), 1);
		//tscollection.addSeries(cycle);
	}
	
	private JFreeChart createChart(Composite parent) {
		JFreeChart chart = ChartFactory.createXYStepAreaChart(
				"NO DATA", XAXIS_LABEL, null, null, PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);
		
		LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		
		XYPlot xyplot = chart.getXYPlot();
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainMinorGridlinePaint(Color.white);
		xyplot.setRangeGridlinePaint(Color.white);
		
		ValueAxis yAxis = xyplot.getRangeAxis();
		yAxis.setUpperBound( 1.5);
		yAxis.setLowerBound( 0.0);
		yAxis.setAutoRange(false);
		yAxis.setLowerMargin(1.0);
		yAxis.setRange( 0.0, 1.5);
		yAxis.setMinorTickCount(1);
		yAxis.setVisible(false);
		
		ValueAxis xAxis = xyplot.getDomainAxis();
		TickUnits tickUnits = new TickUnits(); 
		tickUnits.add(new NumberTickUnit(1));
		xAxis.setStandardTickUnits(tickUnits);
		xAxis.setAutoRange(false);
		xAxis.setAutoTickUnitSelection(false);

		ChartComposite chartcomp = new ChartComposite(parent, SWT.NONE, chart, true);
		chartcomp.setLayoutData(new GridData(GridData.FILL_BOTH));
		chartcomp.getChart().getPlot().zoom(1.0);
		chartcomp.setDomainZoomable(false);
		chartcomp.setRangeZoomable(false);
		
		return chart;
	}

	@Override
	public void setFocus() {
	}
}

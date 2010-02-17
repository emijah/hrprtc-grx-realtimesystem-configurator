package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.Color;
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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleEdge;

import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.TreeModelItem;

public class TimingChartView extends ViewPart {
	private final static String XAXIS_LABEL = "Time[msec]";
	ChartComposite chartComp1;	
	
	public TimingChartView() {
	}

	@Override
	public void createPartControl(final Composite parent) {

		getSite().getPage().addSelectionListener(new ISelectionListener() {
			
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part != TimingChartView.this &&	 selection instanceof IStructuredSelection) {
					List ret = ((IStructuredSelection) selection).toList();
					if ( ret.size()>0 && ret.get(0) instanceof TreeModelItem ) {
						TreeModelItem root = ((TreeModelItem)ret.get(0)).getRoot();
						TreeModelItem[] checkedItems = root.getCheckedItems();
						for (int i=0; i<checkedItems.length; i++) {
							TreeModelItem item = checkedItems[i];
							if ( item instanceof RTComponentItem ) {
								RTComponentItem rtc = (RTComponentItem) item;
								String compositeType = rtc.getComponent().getCompositeType();
								//if ( !compositeType.equals("PeriodicECShared") && !compositeType.equals("PeriodicStateShared") ) {
									chartComp1.getChart().setTitle(rtc.getName());
									XYPlot plot = chartComp1.getChart().getXYPlot();
									
									TimeSeriesCollection tscollection = new TimeSeriesCollection();
									TimeSeries tseries = new TimeSeries(rtc.getName(), FixedMillisecond.class);
									tseries.add(new FixedMillisecond(0), 1);
									tseries.add(new FixedMillisecond((int)(rtc.getResult().max*1000000.0)), 1);
									tscollection.addSeries(tseries);
									plot.setDataset(tscollection);
									
									//TimeSeries cycle = new TimeSeries("cycle", FixedMillisecond.class);
									//cycle.add(new FixedMillisecond(5), 1);
									//tscollection.addSeries(cycle);
									
									chartComp1.redraw();
								//}
							}
						}
					}
				}
			}
			
		});
		
		parent.setLayout( new GridLayout(1, false));
		chartComp1 = new ChartComposite(parent, SWT.NONE, createCombinedChart("NOT TESTED YET"), true);
		chartComp1.setLayoutData(new GridData(GridData.FILL_BOTH));
	}
	
	public static JFreeChart createCombinedChart(String title) {

		JFreeChart chart = ChartFactory.createXYStepAreaChart(
				title, XAXIS_LABEL, null, null, PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);
		XYPlot xyplot = chart.getXYPlot();
		xyplot.getRangeAxis().setUpperBound( 1.0);
		xyplot.getRangeAxis().setLowerBound( 0.0);
		xyplot.getRangeAxis().setAutoRange(false);
		TickUnits tickUnits = new TickUnits(); 
		tickUnits.add(new NumberTickUnit(1));
		xyplot.getRangeAxis().setStandardTickUnits(tickUnits);
		xyplot.getDomainAxis().setStandardTickUnits(tickUnits);
		xyplot.getRangeAxis().setLowerMargin(1.0);
		xyplot.getRangeAxis().setRange( 0.0, 1.5);
		xyplot.getRangeAxis().setMinorTickCount(1);
		xyplot.getRangeAxis().setVisible(false);
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainMinorGridlinePaint(Color.white);
		xyplot.setRangeGridlinePaint(Color.white);

		LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setVisible(true);

		return chart;
	}

	@Override
	public void setFocus() {
	}
}

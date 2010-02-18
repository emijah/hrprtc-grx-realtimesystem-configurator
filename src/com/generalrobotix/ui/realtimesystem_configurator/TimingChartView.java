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
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.TreeModelItem;

public class TimingChartView extends ViewPart {
	private List<ChartComposite> chartList = new ArrayList<ChartComposite>();
	private Composite parent;
	private static final int INITIAL_CHART_NUM = 3;
	private final static String XAXIS_LABEL = "Time[msec]";
	
	public TimingChartView() {
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		this.parent = parent;//new Composite(parent, SWT.V_SCROLL);
		this.parent.setLayout( new GridLayout(1, false));
		
		updateChart(null, INITIAL_CHART_NUM-1);

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
						updateChart(null, index);
					}
				}
			}
		});
	}
	
	private void updateChart(RTComponentItem item, int index) {
		while ( chartList.size() < index + 1 ) {
			chartList.add(createChart(parent));
		}
		
		if ( item == null ) {
			for (int i=index; i<chartList.size(); i++) {
				ChartComposite comp = chartList.get(i);
				JFreeChart chart = comp.getChart();
				chart.getXYPlot().setDataset(null);
				if ( index+1 > INITIAL_CHART_NUM ) {
					comp.setVisible(false);
				}
				comp.redraw();
			}
			return;
		}

		JFreeChart chart = chartList.get(index).getChart();
		chart.setTitle("ExecutionContext : "+item.getName());
		XYPlot plot = chart.getXYPlot();
		Iterator<TreeModelItem> rtcs = item.getChildren().iterator();
		double t1 = 0;
		XYSeriesCollection dataset = new XYSeriesCollection();
		//plot.getDomainAxis().setRange(0.0, cycle*1.2);
		double cycle = 1.0/item.getComponent().getExecutionContexts().get(0).getRate()*1000.0;
		XYSeries xyseries = new XYSeries("cycle");
		xyseries.add(cycle-0.005,2);
		xyseries.add(cycle+0.005,2);
		dataset.addSeries(xyseries);
		while ( rtcs.hasNext() ) {
			RTComponentItem rtc = (RTComponentItem)rtcs.next();
			double t2 = rtc.getResult().max*1000.0;
			xyseries = new XYSeries(rtc.getName());
			xyseries.add(t1, 1);
			xyseries.add(t1+t2, 1);
			t1 += t2;
			dataset.addSeries(xyseries);
		}		

		plot.setDataset(dataset);
	}
	
	private ChartComposite createChart(Composite parent) {
		JFreeChart chart = ChartFactory.createXYStepAreaChart(
				"NO DATA", XAXIS_LABEL, null, null, PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);

		XYPlot xyplot = chart.getXYPlot();
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainGridlinePaint(Color.black);
		xyplot.setDomainGridlinesVisible(true);
		
		ValueAxis yAxis = xyplot.getRangeAxis();
		yAxis.setRange(0.0, 1.5);
		yAxis.setVisible(false);

		ChartComposite chartComp = new ChartComposite(parent, SWT.NONE, chart, true);
		chartComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		chartComp.setRangeZoomable(false);
		
		return chartComp;
	}

	@Override
	public void setFocus() {
	}
}

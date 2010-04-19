package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.action.Action;
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

import RTC.TimedState;

import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.TreeModelItem;

public class TimingChartView extends ViewPart {
	private List<ChartComposite> chartList = new ArrayList<ChartComposite>();
	private Composite parent;
	private static final int INITIAL_CHART_NUM = 3;
	private static final String XAXIS_LABEL = "Time[msec]";
	private static final int SHOW_WHOLE   = 0;
	private static final int SHOW_LASTONE = 1;
	private static final int SHOW_WORST   = 2;
	private static final int SHOW_AVERAGE = 3;
	private static final String[] SHOW_MODE_LABELS = new String[]{"whole data", "last one", "worst one", "average"};
	private int showMode = SHOW_WHOLE;
	private Action action1;
	private static TimingChartView this_;
	private TreeModelItem selectedItem_;
	
	public TimingChartView() {
		this_ = this;
	}
	
	@Override
	public void createPartControl(final Composite parent) {
		this.parent = parent;//new Composite(parent, SWT.V_SCROLL);
		this.parent.setLayout( new GridLayout(1, false));
		
		updateChart(null, 0, false);

		getSite().getPage().addSelectionListener(new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				if (part != TimingChartView.this &&	 selection instanceof IStructuredSelection) {
					List sel = ((IStructuredSelection) selection).toList();
					if ( sel.size()>0 && sel.get(0) instanceof TreeModelItem ) {
						selectedItem_ = (TreeModelItem)sel.get(0);
						updateCharts();
					}
				}
			}
		});
		
		action1 = new Action(SHOW_MODE_LABELS[2], Action.AS_PUSH_BUTTON) {
			public void run() {
				changeShowMode();
				action1.setText(SHOW_MODE_LABELS[showMode]);
			}
		};
		action1.setText(SHOW_MODE_LABELS[showMode]);
		getViewSite().getActionBars().getToolBarManager().add(action1);
	}
	
	private void changeShowMode() 
	{
		showMode ++;
		if ( showMode >= SHOW_MODE_LABELS.length) {
			showMode = 0;
		}
	}
	
	synchronized public void updateCharts() {
		TreeModelItem root = selectedItem_.getRoot();
		Iterator<TreeModelItem> checkedItems = root.getCheckedItems().iterator();
		int index = 0;
		while ( checkedItems.hasNext() ) {
			TreeModelItem item = checkedItems.next();
			if ( item instanceof ExecutionContextItem ) {
				boolean isSelected = (item == selectedItem_) || (item.getChildren().contains(selectedItem_));
				updateChart((ExecutionContextItem)item, index++, isSelected);
			}
		}
		updateChart(null, index, false);
	}
	
	private void updateChart(ExecutionContextItem item, int index, boolean isSelected) {
		while ( chartList.size() < index + 1 || chartList.size() < INITIAL_CHART_NUM ) {
			chartList.add(createChart(parent));
		}

		if ( item == null ) {
			for (int i=chartList.size()-1; i >= index; i--) {
				ChartComposite comp = chartList.get(i);
				if ( i + 1 > INITIAL_CHART_NUM ) {
					comp.dispose();
					chartList.remove(i);
				} else {
					JFreeChart chart = comp.getChart();
					chart.setBackgroundPaint(Color.white);
					chart.setTitle("NO DATA");
					chart.getXYPlot().getDomainAxis().setUpperBound(5.2);
					createDataSet(chart, 0.005);
					//chart.getXYPlot().getRenderer(0).setSeriesVisibleInLegend(0,false);
				}
			}
			return;
		}

		JFreeChart chart = chartList.get(index).getChart();
		chart.setBackgroundPaint(isSelected ? Color.yellow : Color.white);
		chart.setTitle("EC : "+item.getName());
		XYSeriesCollection dataset = createDataSet(chart, 1.0/item.getRate()*1000.0);
		Iterator<TreeModelItem> rtcs = item.getChildren().iterator();
		if ( showMode == SHOW_WHOLE ) {
			double bias = -1;
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				List<Double> log = rtc.getResult().lastLog;
				if ( bias <= 0 ) {
					bias =  log.get(0);
				} else {
					bias = Math.min(bias, log.get(0));
				}
			}
			rtcs =  item.getChildren().iterator();
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				XYSeries xyseries = new XYSeries(rtc.getName());
				List<Double> log = rtc.getResult().lastLog;
				for (int i=0; i<log.size(); i+=2) {
					double t1 = log.get(i) - bias;
					double t2 = t1 + log.get(i+1);
					xyseries.add(t1*1000.0, 1);
					xyseries.add(t2*1000.0, 1);
					xyseries.add((t2+0.0000001)*1000.0, 0);
				}
				dataset.addSeries(xyseries);
			}
		} else if ( showMode == SHOW_LASTONE ) {
			double bias = -1;
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				TimedState ts1 = rtc.getResult().lastT1;
				if ( ts1 != null ) {
					if ( bias <= 0 ) {
						bias =  ts1.tm.sec*1000.0 + ts1.tm.nsec*1.0e-6;
					} else {
						bias = Math.min(bias, ts1.tm.sec*1000.0 + ts1.tm.nsec*1.0e-6);
					}
				}
			}

			double t1 = 0;
			double t2 = 0;
			rtcs =  item.getChildren().iterator();
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				XYSeries xyseries = new XYSeries(rtc.getName());
				TimedState ts1 = rtc.getResult().lastT1;
				if ( ts1 != null ) {
					t1 = ts1.tm.sec*1000.0 + ts1.tm.nsec*1.0e-6 - bias;
				}
				TimedState ts2 = rtc.getResult().lastT2;
				if ( ts2 != null ) {
					t2 = ts2.tm.sec*1000.0 + ts2.tm.nsec*1.0e-6 - bias;
				}
				xyseries.add(t1, 1);
				xyseries.add(t2, 1);
				dataset.addSeries(xyseries);
			}
		} else {
			double t1 = 0;
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				double t2 = 0;
				if ( showMode == SHOW_WORST ) {
					t2 = rtc.getResult().max*1000.0;
				} else {
					t2 = rtc.getResult().mean*1000.0;
				}
				
				XYSeries xyseries = new XYSeries(rtc.getName());
				xyseries.add(t1, 1);
				xyseries.add(t1+t2, 1);
				t1 += t2;
				dataset.addSeries(xyseries);
			}
		}
		parent.update();
	}
	
	public XYSeriesCollection createDataSet(JFreeChart chart, double cycle) {
		XYSeriesCollection dataset = new XYSeriesCollection();
		XYSeries xyseries = new XYSeries("cycle");
		xyseries.add(cycle-0.005, 2);
		xyseries.add(cycle+0.005, 2);
		dataset.addSeries(xyseries);
		chart.getXYPlot().setDataset(dataset);
		return dataset;
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
	
	public static TimingChartView getInstance() {
		return this_; // TODO update chart without this method
	}
}

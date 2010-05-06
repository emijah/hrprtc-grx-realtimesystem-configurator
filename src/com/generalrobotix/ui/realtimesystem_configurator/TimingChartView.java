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
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.experimental.chart.swt.ChartComposite;

import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.TreeModelItem;

public class TimingChartView extends ViewPart
{
	private List<ChartComposite> chartList = new ArrayList<ChartComposite>();
	private Composite parent;
	private static TimingChartView this_;
	private TreeModelItem selectedItem_;
	
	private static final int INITIAL_CHART_NUM = 3;
	private static final String XAXIS_LABEL = "Time[msec]";
	private static final int LAST_PERIOD  = 0;
	private static final int SHOW_WORST   = 1;
	private static final int SHOW_AVERAGE = 2;
	private static final String[] SHOW_MODE_LABELS = new String[]{"last period", "worst one", "average"};
	
	private int showMode = LAST_PERIOD;
	private boolean isOffsetUpdated = false;
	private double offset = 0;
	
	private Action actZoomIn;
	private Action actZoomOut;
	private Action actChangeMode;
	private Action actMoveRangeRight;
	private Action actMoveRangeLeft;
	private Action actMoveRangeRight2;
	private Action actMoveRangeLeft2;
	
	private static final double SEC2MSEC = 1000.0;
	private static final double TRANSITION = 0.0;
	private static final double DEFAULT_DOWNSTATE = 1.0;
	private static final double DEFAULT_UPSTATE   = 0.5;
	private static final double DEFAULT_RANGE_MAX = 4.0;
	private static final double DEFAULT_RANGE_MIN = 0.5;
	
	private Text txtPlatFromInfo;
	
	public TimingChartView()
	{
		this_ = this;
	}
	
	@Override
	public void createPartControl(final Composite parent)
	{
		this.parent = parent;//new Composite(parent, SWT.V_SCROLL);
		this.parent.setLayout( new GridLayout(1, false));
		
		updateChart(null, 0, false);

		getSite().getPage().addSelectionListener(new ISelectionListener() 
		{
			public void selectionChanged(IWorkbenchPart part, ISelection selection)
			{
				if (part != TimingChartView.this &&	 selection instanceof IStructuredSelection) {
					List sel = ((IStructuredSelection) selection).toList();
					if ( sel.size()>0 && sel.get(0) instanceof TreeModelItem ) {
						selectedItem_ = (TreeModelItem)sel.get(0);
						updateCharts();
					}
				}
			}
		});
		
		actZoomIn = new Action("Zoom In")
		{
			public void run()
			{
				zoomRange(0.9);
			}
		};
		actZoomIn.setToolTipText("Zoom In && sync. graphs");
		actZoomIn.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/zoomin.png"));
		getViewSite().getActionBars().getToolBarManager().add(actZoomIn);
		
		actZoomOut = new Action("Zoom Out")
		{
			public void run()
			{
				zoomRange(1.1);
			}
		};
		actZoomOut.setToolTipText("Zoom Out && sync. graphs");
		actZoomOut.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/zoomout.png"));
		getViewSite().getActionBars().getToolBarManager().add(actZoomOut);
		
		
		actMoveRangeLeft2 = new Action("<<", Action.AS_PUSH_BUTTON)
		{
			public void run()
			{
				moveRange(-0.1);
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actMoveRangeLeft2);
		
		actMoveRangeLeft = new Action("<", Action.AS_PUSH_BUTTON)
		{
			public void run()
			{
				moveRange(-0.01);
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actMoveRangeLeft);
		
		actMoveRangeRight = new Action(">", Action.AS_PUSH_BUTTON) 
		{
			public void run() 
			{
				moveRange(0.01);
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actMoveRangeRight);
		
		actMoveRangeRight2 = new Action(">>", Action.AS_PUSH_BUTTON)
		{
			public void run()
			{
				moveRange(0.1);
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actMoveRangeRight2);
		
		actChangeMode = new Action(SHOW_MODE_LABELS[showMode], Action.AS_PUSH_BUTTON)
		{
			public void run()
			{
				changeShowMode();
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actChangeMode);
	}
	
	private void changeShowMode() 
	{
		showMode ++;
		if ( showMode >= SHOW_MODE_LABELS.length) {
			showMode = 0;
		}
		actChangeMode.setText(SHOW_MODE_LABELS[showMode]);
		updateCharts();
		showAllValue();
	}
	
	private void zoomRange(double zoomRate) 
	{
		ValueAxis xaxis = chartList.get(0).getChart().getXYPlot().getDomainAxis();
		double l = xaxis.getLowerBound();
		double u = xaxis.getUpperBound();
		for (int i=0; i<chartList.size(); i++) {
			chartList.get(i).getChart().getXYPlot().getDomainAxis().setRange(l, l+(u-l)*zoomRate);
		}
	}
	
	private void moveRange(double moveRate) 
	{
		for (int i=0; i<chartList.size(); i++) {
			ValueAxis xaxis = chartList.get(i).getChart().getXYPlot().getDomainAxis();
			double l = xaxis.getLowerBound();
			double u = xaxis.getUpperBound();
			double r = (u-l)*moveRate;
			xaxis.setRange(l+r, u+r);
		}
	}
	
	private void showAllValue()
	{
		double l = 0, u = 0;
		for (int i=0; i<chartList.size(); i++) {
			XYSeriesCollection dataset = (XYSeriesCollection) chartList.get(i).getChart().getXYPlot().getDataset();
			if ( dataset != null) {
				double tmpL = dataset.getDomainLowerBound(false);
				double tmpU = dataset.getDomainUpperBound(false);
				if ( i == 0 ) {
					l = tmpL;
					u = tmpU;
				} else {
					l = Math.min(l, tmpL);
					u = Math.max(u, tmpU);
				}
			}
		}
		double m = (u-l)*0.05;
		for (int i=0; i<chartList.size(); i++) {
			chartList.get(i).getChart().getXYPlot().getDomainAxis().setRange(l-m, u+m);
		}
	}
	
	synchronized public void updateCharts()
	{
		TreeModelItem root = selectedItem_.getRoot();
		Iterator<TreeModelItem> checkedItems = root.getCheckedItems().iterator();
		int index = 0;
		isOffsetUpdated = false;
		offset = 0;
		while ( checkedItems.hasNext() ) {
			TreeModelItem item = checkedItems.next();
			if ( item instanceof ExecutionContextItem ) {
				boolean isSelected = (item == selectedItem_) || (item.getChildren().contains(selectedItem_));
				updateChart((ExecutionContextItem)item, index++, isSelected);
			}
		}
		updateChart(null, index, false);
	}
	
	private void updateChart(ExecutionContextItem item, int index, boolean isSelected)
	{
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
					chart.getXYPlot().setDataset(null);
					chart.getXYPlot().getDomainAxis().setRange(-0.25, 5.25);
				}
			}
			return;
		}
		double downStateValue = DEFAULT_DOWNSTATE + item.getChildren().size() - 1;
		double upStateValue   = downStateValue + DEFAULT_UPSTATE;
		double cycle = 1.0/item.getRate()*SEC2MSEC;
		JFreeChart chart = chartList.get(index).getChart();
		chart.setBackgroundPaint(isSelected ? Color.yellow : Color.white);
		chart.setTitle("EC : "+item.getName());
		
		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.addSeries(new XYSeries("cycle"));
		chart.getXYPlot().setDataset(dataset);
		
		Iterator<TreeModelItem> rtcs = item.getChildren().iterator();
		if ( showMode == LAST_PERIOD ) {
			if ( !isOffsetUpdated ) {
				while ( rtcs.hasNext() ) {
					RTComponentItem rtc = (RTComponentItem)rtcs.next();
					List<Double> log = rtc.getResult().lastLog_;
					if ( log.size() > 0 && log.get(0) > 0 ) {
						if ( offset <= 0 ) {
							offset = log.get(0);
						} else {
							offset = Math.min(offset, log.get(0));
						}
						isOffsetUpdated = true;
					}
				}
			}
			rtcs =  item.getChildren().iterator();
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				XYSeries xyseries = new XYSeries(rtc.getName());
				List<Double> log = rtc.getResult().lastLog_;
				if ( log.size() > 0 ) {
					xyseries.add(-TRANSITION, downStateValue);
					for (int i=0; i<log.size(); i+=2) {
						double t1 = (log.get(i) - offset)*SEC2MSEC;
						double t2 = t1 + log.get(i+1)*SEC2MSEC;
						xyseries.add(t1-TRANSITION, downStateValue);
						xyseries.add(t1, upStateValue);
						xyseries.add(t2, upStateValue);
						xyseries.add(t2+TRANSITION, downStateValue);
					}
				}
				dataset.addSeries(xyseries);
				downStateValue -= 1;
				upStateValue -= 1;
			}
		} else {
			double t1 = 0;
			while ( rtcs.hasNext() ) {
				RTComponentItem rtc = (RTComponentItem)rtcs.next();
				XYSeries xyseries = new XYSeries(rtc.getName());
				xyseries.add(-TRANSITION, downStateValue);
				xyseries.add(t1-TRANSITION, downStateValue);
				xyseries.add(t1, upStateValue);
				t1 += ((showMode == SHOW_WORST) ? rtc.getResult().max*SEC2MSEC : rtc.getResult().mean*SEC2MSEC);
				xyseries.add(t1, upStateValue);
				xyseries.add(t1+TRANSITION, downStateValue);
				dataset.addSeries(xyseries);
				downStateValue -= 1;
				upStateValue -= 1;
			}
		}
		
		// add cycle
		double tmax = dataset.getDomainUpperBound(false);
		List<XYSeries> list = dataset.getSeries();
		XYSeries cycleSeries = list.get(0);
		if ( cycle > 0 ) {
			for (double v=0; v<tmax+cycle ; v += cycle) {
				cycleSeries.add(v-TRANSITION, 0);
				cycleSeries.add(v, DEFAULT_DOWNSTATE + item.getChildren().size());
				cycleSeries.add(v+TRANSITION, 0);
			}
			XYPlot xyplot = chart.getXYPlot();
			ValueAxis yAxis = xyplot.getRangeAxis();
			yAxis.setRange(DEFAULT_RANGE_MIN, DEFAULT_DOWNSTATE + item.getChildren().size());
			
			// extends line
			downStateValue = DEFAULT_DOWNSTATE + item.getChildren().size() - 1;
			upStateValue   = downStateValue + DEFAULT_UPSTATE;
			for (int i=1; i<list.size(); i++) {
				list.get(i).add((int)(tmax/cycle)*cycle+cycle, downStateValue);
				downStateValue -= 1;
				upStateValue -= 1;
			}
		}
		
		parent.update();
	}
	
	private ChartComposite createChart(Composite parent)
	{
		JFreeChart chart = ChartFactory.createXYLineChart("NO DATA", XAXIS_LABEL,
				null, null, PlotOrientation.VERTICAL, true, true, false);

		XYPlot xyplot = chart.getXYPlot();
		xyplot.setBackgroundPaint(Color.white);
		xyplot.setDomainGridlinePaint(Color.black);
		xyplot.setDomainGridlinesVisible(true);
		xyplot.setRangeGridlinePaint(Color.black);
		xyplot.setRangeGridlinesVisible(true);
		ValueAxis yAxis = xyplot.getRangeAxis();
		yAxis.setRange(DEFAULT_RANGE_MIN, DEFAULT_RANGE_MAX);
		yAxis.setVisible(false);
		yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

		ChartComposite chartComp = new ChartComposite(parent, SWT.NONE, chart, true);
		chartComp.setLayoutData(new GridData(GridData.FILL_BOTH));
		chartComp.setRangeZoomable(false);
		return chartComp;
	}

	@Override
	public void setFocus()
	{
	}
	
	public static TimingChartView getInstance()
	{
		return this_; // TODO update chart without this method
	}
} 
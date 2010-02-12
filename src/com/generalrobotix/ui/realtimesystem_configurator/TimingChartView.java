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
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;

import com.generalrobotix.model.RTCModel;

public class TimingChartView extends ViewPart {
	private String RtcName1="rtc1", RtcName2;
	private ChartComposite chartComp1, chartComp2;
	

	public TimingChartView() {
	}

	@Override
	public void createPartControl(final Composite parent) {
		getSite().getPage().addSelectionListener(new ISelectionListener() {
			
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				// TODO 自動生成されたメソッド・スタブ
				if (part != TimingChartView.this &&
						selection instanceof IStructuredSelection) {
					List ret = ((IStructuredSelection) selection).toList();
					if ( ret.get(0) instanceof RTCModel ) {
						RTCModel mo = ((RTCModel)ret.get(0)).getTop();
						List<RTCModel> children = mo.getChildren();
						RtcName1 = children.get(0).getName();
						RtcName2 = children.get(1).getName();
						System.out.println("Rtc1: "+RtcName1+ ", RTC2: "+RtcName2);
					}
				}
				parent.getParent().redraw();
				parent.getParent().update();
				chartComp1.redraw();
				chartComp2.redraw();
				chartComp1.getChart().setTitle(RtcName1);
				chartComp2.getChart().setTitle(RtcName2);
				
			}
			
		});
		
		GridLayout gridlayout = new GridLayout(1, false);
		GridData gdata = new GridData(GridData.FILL_BOTH);
		parent.setLayout(gridlayout);
		chartComp1 = new ChartComposite(parent, SWT.NONE, createCombinedChart(RtcName1), true);
		chartComp2 = new ChartComposite(parent, SWT.NONE, createCombinedChart(RtcName2), true);
		
		chartComp1.setLayoutData(gdata);
		chartComp2.setLayoutData(gdata);
		parent.redraw();
	}
	
	public static JFreeChart createCombinedChart(String title) {
		String xAxisLabel = "Time";
		
		NumberAxis domainAxis = new NumberAxis("Time");
		
		CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
		
        plot.setBackgroundPaint(Color.white);
		plot.setDomainMinorGridlinePaint(Color.white);
		plot.setDomainGridlinePaint(Color.black);
		plot.setRangeGridlinePaint(Color.black);
		
		XYDataset data = null, data2 = null;
		JFreeChart	subplot, subplot2, subplotXYStepArea;
		
		data = createStepXYDataset();
			
		subplot = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				(XYDataset) data,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);
		
		XYPlot subplotXY = subplot.getXYPlot();
		subplotXY.getRangeAxis().setUpperBound( 2.0);
		subplotXY.getRangeAxis().setLowerBound(-1.0);
		subplotXY.getRangeAxis().setLowerMargin(0.5);
		subplotXY.getRangeAxis().setRange(-1.0, 2.0);
		
		subplotXY.setBackgroundPaint(Color.white);
		subplotXY.setDomainMinorGridlinePaint(Color.white);
		subplotXY.setDomainGridlinePaint(Color.black);
		subplotXY.setRangeGridlinePaint(Color.black);
		
		subplotXYStepArea = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				(XYDataset) data,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
			);
		
		XYPlot cp = subplotXYStepArea.getXYPlot();
		cp.setBackgroundPaint(Color.white);
		cp.setRangeGridlinePaint(Color.black);
		cp.setDomainGridlinePaint(Color.black);

		subplot2 = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				(XYDataset) data,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
			);
		
		XYPlot subplotXY2 = subplot2.getXYPlot();
		subplotXY2.getRangeAxis().setUpperBound( 2.0);
		subplotXY2.getRangeAxis().setUpperMargin(2.0);
		subplotXY2.getRangeAxis().setLowerBound(-1.0);
		subplotXY2.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		subplotXY2.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		int tmp = subplotXY2.getDomainAxis().getMinorTickCount();
		subplotXY2.setBackgroundPaint(Color.white);
		subplotXY2.setDomainMinorGridlinePaint(Color.white);
		subplotXY2.setDomainGridlinePaint(Color.black);
		subplotXY2.setRangeGridlinePaint(Color.black);
		JFreeChart subplotXYStepArea2 = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				(XYDataset)data2,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);

		plot.add(subplotXYStepArea.getXYPlot());
		plot.add(subplotXYStepArea2.getXYPlot());
		
		return subplot;
	}
	
    public static XYDataset createStepXYDataset() {
    	
        TimeSeries s1 = new TimeSeries("Plan 1", FixedMillisecond.class);
        s1.add(new FixedMillisecond(0), 0);
        s1.add(new FixedMillisecond(1), 1);
        s1.add(new FixedMillisecond(2), 0);
        s1.add(new FixedMillisecond(3), 0);
        s1.add(new FixedMillisecond(4), 1);
        s1.add(new FixedMillisecond(5), 0);
        s1.add(new FixedMillisecond(6), 0);

    	TimeSeries s2 = new TimeSeries("Plan 2", FixedMillisecond.class);
        s2.add(new FixedMillisecond(0), 0);
        s2.add(new FixedMillisecond(1), 0);
        s2.add(new FixedMillisecond(2), 1);
        s2.add(new FixedMillisecond(3), 0);
        s2.add(new FixedMillisecond(4), 0);
        s2.add(new FixedMillisecond(5), 1);
        s2.add(new FixedMillisecond(6), 0);
        
        //final TimeSeriesCollection dataset = new TimeSeriesCollection();
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        
        dataset.addSeries(s1);
        dataset.addSeries(s2);
        return dataset;
    }
    
	@Override
	public void setFocus() {
	}

}

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
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.general.DatasetGroup;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.experimental.chart.swt.ChartComposite;
import org.jfree.ui.RectangleEdge;

import com.generalrobotix.model.RTCModel;

public class TimingChartView extends ViewPart {

	String RtcName1, RtcName2;
	String Title1, Title2;
	List<RTCModel> children;
	ChartComposite chartComp1, chartComp2;
	private static TimeSeries s1;
	private static TimeSeries s2;
	
	public TimingChartView() {
	}

	@Override
	public void createPartControl(final Composite parent) {

		getSite().getPage().addSelectionListener(new ISelectionListener() {
			
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				String RtcName1 = null;
				String RtcName2 = null;
				// TODO 自動生成されたメソッド・スタブ
				if (part != TimingChartView.this &&
						selection instanceof IStructuredSelection) {
					List ret = ((IStructuredSelection) selection).toList();
					if ( ret.get(0) instanceof RTCModel ) {
						RTCModel mo = ((RTCModel)ret.get(0)).getTop();
						children = mo.getChildren();
						RtcName1 = children.get(1).getChildren().get(0).getName();
						s1.setKey(RtcName1);
						RtcName2 = children.get(1).getChildren().get(1).getName();
						s2.setKey(RtcName2);
						List<RTCModel> lists = children.get(1).getChildren();
						for (int k=0; k<lists.size(); k++){
							lists.get(k).getName();
							//System.out.println("."+lists.get(k).getName());
						}
						Title1 = children.get(0).getName();
						Title2 = children.get(1).getName();
					}
				}
				
				
				chartComp1.getChart().setTitle(Title1);
				chartComp2.getChart().setTitle(Title2);
				Plot ttt = chartComp1.getChart().getPlot();
			}
			
		});
		
		GridLayout gridlayout = new GridLayout(1, false);
		GridData gdata = new GridData(GridData.FILL_BOTH);
		parent.setLayout(gridlayout);
		chartComp1 = new ChartComposite(parent, SWT.NONE, createCombinedChart("", children), true);
		chartComp2 = new ChartComposite(parent, SWT.NONE, createCombinedChart("", children), true);
		
		chartComp1.setLayoutData(gdata);
		chartComp2.setLayoutData(gdata);
		parent.redraw();
	}
	
	public static JFreeChart createCombinedChart(String title, List<RTCModel> list) {
		String xAxisLabel = "Time[msec]";
		
		NumberAxis domainAxis = new NumberAxis("Time");
		
		CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domainAxis);
		
        plot.setBackgroundPaint(Color.white);
		plot.setDomainMinorGridlinePaint(Color.white);
		plot.setDomainGridlinePaint(Color.black);
		plot.setRangeGridlinePaint(Color.black);
		
		XYDataset data = null, data2 = null;
		JFreeChart	subplot, subplot2, subplotXYStepArea, subplotXYStepArea2;
		
		//String[] strData =  {"test1", "test2"};
		String[] strData =  {"", ""};
		if (list!=null){
			
			int size = list.size();
			for(int j=0; j<size; j++){
				String[] strData2 = new String[size];
				List<RTCModel> que = list.get(j).getChildren();
				System.out.print("");
			}
		}
		
		data = createStepXYDataset(strData);
			
		subplot = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				data,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);
		
		XYPlot subplotXY = subplot.getXYPlot();
		subplotXY.getRangeAxis().setUpperBound( 1.0);
		subplotXY.getRangeAxis().setLowerBound( 0.0);
		TickUnits tickUnits = new TickUnits(); 
		TickUnit unit = new NumberTickUnit(1);
		tickUnits.add(unit);
		subplotXY.getRangeAxis().setStandardTickUnits(tickUnits);
		
		subplotXY.getDomainAxis().setStandardTickUnits(tickUnits);
		
		subplotXY.getRangeAxis().setLowerMargin(1.0);
		subplotXY.getRangeAxis().setRange( 0.0, 1.5);
		subplotXY.getRangeAxis().setMinorTickCount(1);
		subplotXY.getRangeAxis().setVisible(false);
		subplotXY.setBackgroundPaint(Color.white);
		subplotXY.setDomainMinorGridlinePaint(Color.white);
		subplotXY.setRangeGridlinePaint(Color.white);
		
		subplotXYStepArea = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				data,
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
				data,
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
		subplotXYStepArea2 = ChartFactory.createXYStepAreaChart(
				title,
				xAxisLabel, null,
				data2,
				PlotOrientation.VERTICAL,
				true,   // legend
				true,   // tooltips
				false   // urls
		);

		plot.add(subplotXYStepArea.getXYPlot());
		plot.add(subplotXYStepArea2.getXYPlot());
		
		LegendTitle legend = subplot.getLegend();
		legend.setPosition(RectangleEdge.BOTTOM);
		legend.setVisible(true);

		return subplot;
	}
	
	public static XYDataset createStepXYDataset(String[] components) {
		
	    //final TimeSeriesCollection dataset = new TimeSeriesCollection();
	    TimeSeriesCollection dataset = new TimeSeriesCollection();
	    
		//if(components[0]!=""){
	        s1 = new TimeSeries(components[0], FixedMillisecond.class);
	        s1.add(new FixedMillisecond(0), 0);
	        s1.add(new FixedMillisecond(1), 1);
	        s1.add(new FixedMillisecond(2), 0);
	        s1.add(new FixedMillisecond(3), 0);
	        s1.add(new FixedMillisecond(4), 1);
	        s1.add(new FixedMillisecond(5), 0);
	        s1.add(new FixedMillisecond(6), 0);
	        dataset.addSeries(s1);
		//}
	    
		//if(components[1]!=""){
	    	s2 = new TimeSeries(components[1], FixedMillisecond.class);
	        s2.add(new FixedMillisecond(0), 0);
	        s2.add(new FixedMillisecond(1), 0);
	        s2.add(new FixedMillisecond(2), 1);
	        s2.add(new FixedMillisecond(3), 0);
	        s2.add(new FixedMillisecond(4), 0);
	        s2.add(new FixedMillisecond(5), 1);
	        s2.add(new FixedMillisecond(6), 0);
	        dataset.addSeries(s2);
		//}
		
	    return dataset;
	}
    
	@Override
	public void setFocus() {
	}

}

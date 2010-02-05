package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.category.DefaultIntervalCategoryDataset;
import org.jfree.data.gantt.Task;
import org.jfree.data.gantt.TaskSeries;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.date.DateUtilities;
import org.jfree.experimental.chart.swt.ChartComposite;

public class TimingChartView extends ViewPart {

	public TimingChartView() {
	}

	private static Date date(int day, int month, int year) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, day);
		Date result = calendar.getTime();
		return result;
	}
	
	@Override
	public void createPartControl(Composite parent) {
        final JFreeChart chart = createChart();
		final ChartComposite frame = new ChartComposite(parent, SWT.NONE, chart, true);
	}

	private JFreeChart createChart() {
        // create a default chart based on some sample data...
        final String title = "LCACs in use at given time";
        final String xAxisLabel = "Time";
        final String yAxisLabel = "Number of Transports";

        final XYDataset data = createStepXYDataset();

        final JFreeChart chart = ChartFactory.createXYStepAreaChart(
            title,
            xAxisLabel, yAxisLabel,
            data,
            PlotOrientation.VERTICAL,
            true,   // legend
            true,   // tooltips
            false   // urls
        );

		// then customise it a little...
		chart.setBackgroundPaint(Color.white);						// 図の背景色設定
		
		XYPlot plot = (XYPlot)chart.getPlot();						// 描画領域のオブジェクト取得
        plot.setBackgroundPaint(Color.white);						// 描画領域の色設定
		plot.setDomainGridlinePaint(Color.black);					// x軸方向の目盛線の色設定
		plot.setRangeGridlinePaint(Color.black);					// y軸方向の目盛線の色設定
        
        return chart;
	}
	
    public static XYDataset createStepXYDataset() {
    	
        //final CategoryPlot plotArea = chart.getCategoryPlot();
        //plotArea.setBackgroundPaint(null);

    	TimeSeries s1 = new TimeSeries("Plan 1", FixedMillisecond.class);
 
        s1.add(new FixedMillisecond(0), 0);
        s1.add(new FixedMillisecond(1), 1);
        s1.add(new FixedMillisecond(2), 0);
        s1.add(new FixedMillisecond(3), 0);
        s1.add(new FixedMillisecond(4), 1);
        s1.add(new FixedMillisecond(5), 0);
        s1.add(new FixedMillisecond(6), 0);

    	TimeSeries s2 = new TimeSeries("Plan 1", FixedMillisecond.class);
    	 
        s2.add(new FixedMillisecond(0), 0);
        s2.add(new FixedMillisecond(1), 0);
        s2.add(new FixedMillisecond(2), 1);
        s2.add(new FixedMillisecond(3), 0);
        s2.add(new FixedMillisecond(4), 0);
        s2.add(new FixedMillisecond(5), 1);
        s2.add(new FixedMillisecond(6), 0);
        
        final TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.addSeries(s1);
        //dataset.addSeries(s2);
        return dataset;
    }
    
	@Override
	public void setFocus() {
	}

}

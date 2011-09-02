package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BenchmarkResultItem extends TreeModelItem 
{ 
	public static final String PROPERTY_DATE = "Date";
	public static final String PROPERTY_DATACOUNT = "dataCount";
	public static final String PROPERTY_MAX_DURATION  = "Max. duration";
	public static final String PROPERTY_MIN_DURATION  = "Min. duration";
	public static final String PROPERTY_MEAN_DURATION = "Ave. duration";
	public static final String PROPERTY_STDDEV_DURATION = "stddev duration";
	
	public static final String PROPERTY_CPU_TYPE = "CPU Type";
	public static final String PROPERTY_CPU_NUM = "CPU Num.";
	public static final String PROPERTY_CPU_FREQUENCY = "CPU Frequency";
	public static final String PROPERTY_CPU_AFFINITY = "CPU Affinity";
	public static final String PROPERTY_OS_NAME = "OS Name";
	public static final String PROPERTY_OS_RELEASE = "OS Release";
	public static final String PROPERTY_KERNEL_NAME = "Kernel Name";
	public static final String PROPERTY_KERNEL_RELEASE = "Kernel Release";
	public static final String PROPERTY_EXTRA_DATA = "Extra Data";
	
	public Date date;
	public int count = 0;    
	public double max = 0;
	public double min = 0;
	public double mean = 0;
	public double stddev = 0;
	public double cycle = 0;
	public List<Double> lastLog_ = new ArrayList<Double>();
	public Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
	
	public BenchmarkResultItem() 
	{
		reset();
		updateProperties();
	}
	
	public BenchmarkResultItem(Map<Object, Object> properties) 
	{
		count = Integer.parseInt(properties.get(PROPERTY_DATACOUNT).toString());
		max = Double.parseDouble(properties.get(PROPERTY_MAX_DURATION).toString());
		min = Double.parseDouble(properties.get(PROPERTY_MIN_DURATION).toString());
		mean = Double.parseDouble(properties.get(PROPERTY_MEAN_DURATION).toString());
		Object dateObj = (Object)properties.get(PROPERTY_DATE);
		if ( dateObj != null ) {
			try {
				date = new Date(((Double)dateObj).longValue());
			} catch (Exception e) {
				date = null;
				e.printStackTrace();
			}
		}
		updateProperties();
	}
	
	public void setCycle(double cycle) 
	{
		this.cycle = cycle;
	}
	
	public void updateMax(double val)
	{
		max = Math.max(max, val);
	}
	
	public void updateLastPeriod(OpenHRP.ExecutionProfileServicePackage.TimePeriod[] log)
	{
		if ( log.length == 0 ) {
			return;
		}
		
		lastLog_.clear();
		double sum = 0;
		double t_prev = 0;
		for (int i=0; i<log.length; i++) {
			double t1   = log[i].begin.sec + log[i].begin.nsec*1.0e-9;
			if (t1 <= t_prev)
				break;
			t_prev = t1;
			double diff = log[i].end.sec   + log[i].end.nsec*1.0e-9 - t1;
			sum += diff;
			lastLog_.add(t1);
			lastLog_.add(diff);
		}
		mean = (mean*count + sum)/(count + lastLog_.size()/2);
		count += lastLog_.size()/2;
		date   = new Date();
		updateProperties();
	}
	
	public void updatePlatformInfo(OpenHRP.ExecutionProfileServicePackage.PlatformInfo pInfo)
	{
		setPropertyValue(PROPERTY_CPU_NUM, pInfo.cpuNum);
		setPropertyValue(PROPERTY_CPU_FREQUENCY, pInfo.cpuFrequency);
		setPropertyValue(PROPERTY_CPU_AFFINITY, pInfo.cpuAffinity);
		setPropertyValue(PROPERTY_CPU_TYPE, pInfo.cpuType);
		setPropertyValue(PROPERTY_OS_NAME, pInfo.osName);
		setPropertyValue(PROPERTY_OS_RELEASE, pInfo.osRelease);
		setPropertyValue(PROPERTY_KERNEL_NAME, pInfo.kernelName);
		setPropertyValue(PROPERTY_KERNEL_RELEASE, pInfo.kernelRelease);
		setPropertyValue(PROPERTY_EXTRA_DATA, pInfo.extraData);
	}
	
	private void updateProperties()
	{
		setPropertyValue(PROPERTY_DATACOUNT, String.valueOf(count));
		setPropertyValue(PROPERTY_MAX_DURATION, String.valueOf(max));
		setPropertyValue(PROPERTY_MIN_DURATION, String.valueOf(min));
		setPropertyValue(PROPERTY_MEAN_DURATION, String.valueOf(mean));
		setPropertyValue(PROPERTY_STDDEV_DURATION, String.valueOf(stddev));
		if ( date != null ) {
			setPropertyValue(PROPERTY_DATE, String.valueOf(date.getTime()));
		}
	}

	public void reset()
	{
		count = 0;
		date = null;
		max = 0;
		min = 0;
		mean = 0;
		stddev = 0;
		lastLog_.clear();
		updateProperties();
	}
	
	public void resetLastLog()
	{
		lastLog_.clear();
	}
	
	public void plus(BenchmarkResultItem result)
	{
		if ( count < result.count ) {
			count = result.count;
		}

		if ( result.date != null && ( date == null || result.date.after(date)) ) {
			date = result.date;
		}
		//max  += result.max;
		//min  += result.min;
		mean += result.mean;
		updateProperties();
	}
}

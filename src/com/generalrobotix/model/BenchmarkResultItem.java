package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import RTC.TimedState;

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
	
	private Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
	private static int LOG_CAPACITY = 500;
	private int logCapacity = LOG_CAPACITY;

	public BenchmarkResultItem() 
	{
		reset();
		updateProperties();
	}
	
	public BenchmarkResultItem(NamedStateLog namedLog, PlatformInfo pinfo, double cycle) 
	{
		setCycle(cycle);
		updateLog(namedLog);
		updatePlatformInfo(pinfo);
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
	
	public void updateLog(NamedStateLog namedLog)
	{
		TimedState[] log = namedLog.log;
		if ( log.length == 0 ) {
			return;
		}
		double lastT = 0;
		if ( lastLog_ != null && lastLog_.size() > 0 ) {
			lastT = lastLog_.get(lastLog_.size() - 2);
			logCapacity = log.length - 2;
		}
		int lastCount = count;
		//stddev = Math.pow(stddev, 2);
		for (int i=0; i<log.length; i++) {
			int pos1 = namedLog.headPos + i;
			if ( pos1 > log.length-1 ) {
				pos1 -= log.length;
			}
			int pos2 = ( pos1 == log.length-1 ) ? 0 : pos1+1;
			double t1 = log[pos1].tm.sec + log[pos1].tm.nsec*1.0e-9;
			if ( lastT < t1 && log[pos1].data == 1 && log[pos2].data == 2 ) {
				double diff = log[pos2].tm.sec + log[pos2].tm.nsec*1.0e-9 - t1;
				if ( diff > 0 ) {
					max = Math.max(max, diff);
					//min = Math.min(min, diff);
					mean = (mean*count + diff)/(count + 1);
					//stddev = (stddev*count + Math.pow(diff-mean, 2))/(count + 1);
					count ++;
					i++;
					lastLog_.add(t1);
					lastLog_.add(diff);
					while ( lastLog_.size() > logCapacity ) {
						lastLog_.remove(0);
						lastLog_.remove(0);
					}
				}
			}
		}
		//stddev = Math.sqrt(stddev);
		if ( lastCount < count ) {
			date   = new Date();
			updateProperties();
		}
	}
	
	public void updatePlatformInfo(PlatformInfo pInfo)
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
		max  += result.max;
		min  += result.min;
		mean += result.mean;
		updateProperties();
	}
	
	public Object getPropertyValue(Object id)
	{
		return properties.get(id);
	}

	public void setPropertyValue(Object id, Object value)
	{
		properties.put(id, value);		
	}
	
	public Map<Object, Object> getPropertyMap()
	{
		return properties;
	}
}

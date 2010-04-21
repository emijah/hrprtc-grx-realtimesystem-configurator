package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import RTC.TimedState;

public class BenchmarkResultItem extends TreeModelItem 
{ 
	private static final String PROPERTY_DATE = "Date";
	private static final String PROPERTY_DATACOUNT = "dataCount";
	private static final String PROPERTY_MAX_DURATION  = "Max. duration";
	private static final String PROPERTY_MIN_DURATION  = "Min. duration";
	private static final String PROPERTY_MEAN_DURATION = "Ave. duration";
	private static final String PROPERTY_STDDEV_DURATION = "stddev duration";
	
	private static final String PROPERTY_CPU_TYPE = "CPU Type";
	private static final String PROPERTY_CPU_NUM = "CPU Num.";
	private static final String PROPERTY_CPU_FREQUENCY = "CPU Frequency";
	private static final String PROPERTY_CPU_AFFINITY = "CPU Affinity";
	private static final String PROPERTY_OS_NAME = "OS Name";
	private static final String PROPERTY_OS_RELEASE = "OS Release";
	private static final String PROPERTY_KERNEL_NAME = "Kernel Name";
	private static final String PROPERTY_KERNEL_RELEASE = "Kernel Release";
	private static final String PROPERTY_EXTRA_DATA = "Extra Data";
	
	public Date date;
	public int count = 0;    
	public double max = 0;
	public double min = 0;
	public double mean = 0;
	public double stddev = 0;
	public double cycle = 0;
	private TimedState lastT1;
	public List<Double> lastLog = new ArrayList<Double>();

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
		lastLog.clear();
		int lastPos = 0;
		stddev = Math.pow(stddev, 2);
		for (int i=0; i<log.length; i++) {
			int pos1 = i + namedLog.endPoint;
			if ( pos1 > log.length-1 ) {
				pos1 -= log.length;
			}
			int pos2 = ( pos1 == log.length-1 ) ? 0:pos1+1;
			
			if ( log[pos1].data == 1 && log[pos2].data == 2) {
				double diff = (log[pos2].tm.sec - log[pos1].tm.sec) + (log[pos2].tm.nsec - log[pos1].tm.nsec)*1.0e-9;
				if ( diff < 0 || (lastT1 != null && log[pos1].tm.sec <= lastT1.tm.sec && log[pos1].tm.nsec <= lastT1.tm.nsec) ) {
					continue;
				}
				max = Math.max(max, diff);
				min = Math.min(min, diff);
				mean = (mean*count + diff)/(count + 1);
				stddev = (stddev*count + Math.pow(diff-mean, 2))/(count + 1);
				count ++;
				i++;
				lastPos = pos1;
				lastLog.add(log[pos1].tm.sec + log[pos1].tm.nsec*1.0e-9);
				lastLog.add(diff);
				if ( max < 0 || min < 0) {
					System.out.println("dataerror: "+ cycle + ":" +max +":" + min + ":" +pos1+":"+pos2+":"+namedLog.endPoint+":"+i );
				}
			}
		}
		stddev = Math.sqrt(stddev);
		date   = new Date();
		lastT1 = log[lastPos];
		updateProperties();
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
		lastT1 = null;
		updateProperties();
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
}
package com.generalrobotix.model;

import java.util.Date;
import java.util.Map;

import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import RTC.TimedState;

public class BenchmarkResultItem extends TreeModelItem { 
	private static final String PROPERTY_DATE = "Date";
	private static final String PROPERTY_DATACOUNT = "dataCount";
	private static final String PROPERTY_MAX_DURATION  = "Max. duration";
	private static final String PROPERTY_MIN_DURATION  = "Min. duration";
	private static final String PROPERTY_MEAN_DURATION = "Ave. duration";
	
	public Date date;
	public int count = 0;    
	public double max = 0;
	public double min = 0;
	public double mean = 0;
	public double stddev = 0;
	public double cycle = 0;
	public TimedState lastT1 = null;
	public TimedState lastT2 = null;

	public String cpuType;
	public int cpuNum;
	public int cpuFrequency;
	public int cpuAffinity;
	public String osName;
	public String osRelease;
	public String kernelName; 
	public String kernelRelease; 
	public String extraData;

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
		int lastPos = 0;
		stddev = Math.pow(stddev, 2);
		for (int i=0; i<log.length; i++) {
			int pos1 = i + namedLog.endPoint + 1;
			if ( pos1 > log.length-1 ) {
				pos1 -= log.length;
			}
			int pos2 = ( pos1 == log.length-1 ) ? 0:pos1+1;
			
			if ( log[pos1].data == 1 && log[pos2].data == 2) {
				double diff = (log[pos2].tm.sec - log[pos1].tm.sec) + (log[pos2].tm.nsec - log[pos1].tm.nsec)*10e-9;
				if ( diff < 0 || (lastT1 != null && log[pos1].tm.sec <= lastT1.tm.sec && log[pos1].tm.sec <= lastT1.tm.sec) ) {
					continue;
				}
				max = Math.max(max, diff);
				min = Math.min(min, diff);
				mean = (mean*count + diff)/(count + 1);
				stddev = (stddev*count + Math.pow(diff-mean, 2))/(count + 1);
				count ++;
				i++;
				lastPos = pos1;
			}
			if ( max < 0 || cycle < max || min < 0) {
				System.out.println(cycle + ":" +max +":" + min + ":" +pos1+":"+pos2+":"+namedLog.endPoint+":"+i );
			}
		}
		stddev = Math.sqrt(stddev);
		date   = new Date();
		lastT1 = log[lastPos];
		lastT2 = log[lastPos+1];
	}
	
	public void updatePlatformInfo(PlatformInfo pInfo)
	{
		cpuType = pInfo.cpuType;
		cpuNum = pInfo.cpuNum;
		cpuFrequency = pInfo.cpuFrequency;
		cpuAffinity = pInfo.cpuAffinity;
		osName = pInfo.osName;
		osRelease = pInfo.osRelease;
		kernelName = pInfo.kernelName;
		kernelRelease = pInfo.kernelRelease;
		extraData = pInfo.extraData;
	}
	
	private void updateProperties()
	{
		setPropertyValue(PROPERTY_DATACOUNT, String.valueOf(count));
		setPropertyValue(PROPERTY_MAX_DURATION, String.valueOf(max));
		setPropertyValue(PROPERTY_MIN_DURATION, String.valueOf(min));
		setPropertyValue(PROPERTY_MEAN_DURATION, String.valueOf(mean));
		if ( date != null ) {
			setPropertyValue(PROPERTY_DATE, String.valueOf(date.getTime()));
		}
		/*stddev = value.stddev;
		date   = new Date();
		cpuType = value2.cpuType;
		cpuNum = value2.cpuNum;
		cpuFrequency = value2.cpuFrequency;
		osName = value2.osName;
		osRelease = value2.osRelease;
		kernelName = value2.kernelName;
		kernelRelease = value2.kernelRelease;
		extraData = value2.extraData;*/
	}

	public void reset()
	{
		count = 0;
		date = null;
		max = 0;
		min = 0;
		mean = 0;
		stddev = 0;
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
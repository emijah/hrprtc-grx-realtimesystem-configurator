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
	public int count;    
	public double max;
	public double min;
	public double mean;
	public double stddev;

	public String cpuType;
	public int cpuNum;
	public int cpuFrequency;
	public int cpuAffinity;
	public String osName;
	public String osRelease;
	public String kernelName; 
	public String kernelRelease; 
	public String extraData;
	

	public BenchmarkResultItem(NamedStateLog namedLog, PlatformInfo value2) {
		TimedState[] log = namedLog.log;
		System.out.println("id:"+namedLog.id);
		count = 0;
		mean = 0;
		max = 0;
		min = 10000;
		for (int i=0; i<log.length; i++) {
			int pos = i + namedLog.endPoint + 1;
			pos = ( pos >= log.length ) ? i : pos;
			if ( pos+1 >= log.length ) {
				break;
			}
				
			if ( log[pos].data == 1 && log[pos+1].data == 2) {
				count ++;
				i++;
				double diff = (log[pos+1].tm.sec - log[pos].tm.sec) + (log[pos+1].tm.nsec - log[pos].tm.nsec)*10e-9;
				max = Math.max(max, diff);
				min = Math.min(min, diff);
				mean += diff;
			}
		}
		mean /= (double)count;
		
		stddev = 0;
		for ( int i = 1; i < count; i++ ) {
			int pos = i + namedLog.endPoint + 1;
			pos = ( pos >= log.length ) ? i : pos;
			if ( pos+1 >= log.length ) {
				break;
			}
			if ( log[pos].data == 1 && log[pos+1].data == 2) {
				count ++;
				i++;
				stddev += Math.pow(((log[pos+1].tm.sec - log[pos].tm.sec) + (log[pos+1].tm.nsec - log[pos].tm.nsec)*10e-9)-mean, 2);
			}
	    }
		// Change to ( n - 1 ) to n if you have complete data instead of a sample.
		stddev = Math.sqrt( stddev / count );

		date   = new Date();
		cpuType = value2.cpuType;
		cpuNum = value2.cpuNum;
		cpuFrequency = value2.cpuFrequency;
		cpuAffinity = value2.cpuAffinity;
		osName = value2.osName;
		osRelease = value2.osRelease;
		kernelName = value2.kernelName;
		kernelRelease = value2.kernelRelease;
		extraData = value2.extraData;
		updateProperties();
	}

	public BenchmarkResultItem() {
	}
	
	public BenchmarkResultItem(Map<Object, Object> properties) {
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

	public void reset() {
		count = 0;
		date = null;
		max = 0;
		min = 0;
		mean = 0;
		stddev = 0;
		updateProperties();
	}
	
	public void plus(BenchmarkResultItem result) {
		date = result.date;
		max  += result.max;
		min  += result.min;
		mean += result.mean;
		updateProperties();
	}
	
	private void updateProperties() {
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
}

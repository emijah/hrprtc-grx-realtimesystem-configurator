package com.generalrobotix.model;

import java.util.Date;
import java.util.Map;

import OpenHRP.BenchmarkResult;
import OpenHRP.PlatformInfo;

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
	public String osName;
	public String osRelease;
	public String kernelName; 
	public String kernelRelease; 
	public String extraData;
	

	public BenchmarkResultItem(BenchmarkResult value, PlatformInfo value2) {
		count  = value.count;
		max    = value.max;
		mean   = value.mean;
		min    = value.min;
		stddev = value.stddev;
		date   = new Date();
		cpuType = value2.cpuType;
		cpuNum = value2.cpuNum;
		cpuFrequency = value2.cpuFrequency;
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

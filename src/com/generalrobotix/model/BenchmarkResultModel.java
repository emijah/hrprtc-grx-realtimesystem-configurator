package com.generalrobotix.model;

import java.util.Date;
import java.util.Map;

import OpenHRP.BenchmarkResult;
import OpenHRP.PlatformInfo;

public class BenchmarkResultModel extends TreeModelItem { 
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
	

	public BenchmarkResultModel(BenchmarkResult value, PlatformInfo value2) {
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

	public BenchmarkResultModel() {
	}
	
	public BenchmarkResultModel(Map<Object, Object> properties) {
		count = Integer.parseInt(properties.get("dataCount").toString());
		max = Double.parseDouble(properties.get("Max. duration").toString());
		min = Double.parseDouble(properties.get("Min. duration").toString());
		mean = Double.parseDouble(properties.get("Ave. duration").toString());

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
	
	public void plus(BenchmarkResultModel result) {
		max  += result.max;
		min  += result.min;
		mean += result.mean;
		updateProperties();
	}
	
	private void updateProperties() {
		setPropertyValue("dataCount", String.valueOf(count));
		setPropertyValue("Max. duration", String.valueOf(max));
		setPropertyValue("Min. duration", String.valueOf(min));
		setPropertyValue("Ave. duration", String.valueOf(mean));
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

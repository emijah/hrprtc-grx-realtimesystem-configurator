package com.generalrobotix.model;

import java.util.Date;

public class BenchmarkResultModel { 
	public Date date;
	public int count;    
	public double max;
	public double min;
	public double mean;
	public double stddev;

	public class PlatformInfomation {
	    String cpuType;
	    int cpuNum;
	    int cpuFrequency;
	    String osName;
	    String osRelease;
	    String kernelName; 
	    String kernelRelease; 
	    String extraData;
	}
	
	public void reset() {
		count = 0;
		date = null;
		max = 0;
		min = 0;
		mean = 0;
		stddev = 0;
	}
}

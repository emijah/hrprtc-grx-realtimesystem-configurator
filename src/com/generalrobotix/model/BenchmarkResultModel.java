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
}

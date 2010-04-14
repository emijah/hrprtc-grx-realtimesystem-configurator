package com.generalrobotix.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.openrtp.namespaces.rts.version02.Component;

public class RTComponentItem extends TreeModelItem
{
	private Component component;
	private RTSystemItem rtsystem;
	private BenchmarkResultItem result;
	private Map<String, BenchmarkResultItem> resultMap = new HashMap<String, BenchmarkResultItem>();
	private static final String ICON_PATH = "icons/Component.png";
	
    public RTComponentItem(RTSystemItem rtsystem, Component comp)
    {
    	setRoot(rtsystem.getRoot());
    	setName(comp.getInstanceName());
    	this.rtsystem = rtsystem;
    	this.component = comp;
    	this.rtsystem.add(this);
    	setIconPath(ICON_PATH);
    }

	public BenchmarkResultItem getResult()
	{
		if ( result == null ) {
			result = new BenchmarkResultItem();
		}
		return result;
	}
	
	public void setResult(BenchmarkResultItem result) 
	{
		this.result = result;
	}
	
	public void setResult(Map<Object, Object> properties) 
	{
		this.result = new BenchmarkResultItem(properties);
	}
	
	public void calcSummation() 
	{
		getResult().reset();
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem model = it.next();
			if ( model instanceof TreeModelItem ) {
				getResult().plus(((RTComponentItem)model).getResult());
			}
		}
		TreeModelItem item = getParent();
		if ( item instanceof RTComponentItem ) {
			((RTComponentItem)item).calcSummation();
		}
	}
	
	public Component getComponent() 
	{
		return component;
	}
	
	static public class RTCConnection 
	{
		public RTCConnection(RTComponentItem source, RTComponentItem target) {
			this.source = source;
			this.target = target;
		}
		public RTComponentItem source;
		public RTComponentItem target;
		
		public boolean equals(RTCConnection con) {
			return (source.equals(con.source) && target.equals(con.target));
		}
	}
	
	public RTComponentItem findRTC(String id, String instanceName)
	{
		if ( component != null && component.getId().equals(id) && component.getInstanceName().equals(instanceName) ) {
			return this;
		}
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext()) {
			TreeModelItem model = it.next();
			if ( model instanceof RTComponentItem ) {
				RTComponentItem ret = ((RTComponentItem)model).findRTC(id, instanceName);
				if ( ret != null) {
					return ret;
				}
			}
		}
		return null;
	}
	
	public String getHostName()
	{
		return component.getPathUri().split("/")[0];
	}
	
	public RTSystemItem getRTSystem()
	{
		return rtsystem;
	}

	public String getId()
	{
		return component.getId();
	}
}
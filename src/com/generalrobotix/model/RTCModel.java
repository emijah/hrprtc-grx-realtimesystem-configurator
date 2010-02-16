package com.generalrobotix.model;

import java.util.Iterator;

import org.openrtp.namespaces.rts.version02.Component;

public class RTCModel extends TreeModelItem {
	private Component component;
	private BenchmarkResultModel result;
	private RTSystemItem rtsystem;
	
    public RTCModel(RTSystemItem rtsystem, Component comp) {
    	this.setRoot(rtsystem.getRoot());
    	this.setName(comp.getInstanceName());
    	this.rtsystem = rtsystem;
    	this.component = comp;
    }
	
	public BenchmarkResultModel getResult() {
		if ( result == null ) {
			result = new BenchmarkResultModel();
		}
		return result;
	}
	
	public void setResult(BenchmarkResultModel result) {
		this.result = result;
		TreeModelItem item = getParent();
		if ( item instanceof RTCModel ) {
			((RTCModel)item).updateResult();
		}
	}
	
	private void updateResult() {
		getResult().reset();
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem model = it.next();
			if ( model instanceof TreeModelItem ) {
				RTCModel rtcmodel = (RTCModel)model;
				getResult().max += rtcmodel.getResult().max;
				getResult().min += rtcmodel.getResult().min;
				getResult().mean += rtcmodel.getResult().mean;
			}
		}
		TreeModelItem item = getParent();
		if ( item instanceof RTCModel ) {
			((RTCModel)item).updateResult();
		}
	}
	
	public Component getComponent() {
		return component;
	}
	
	static public class RTCConnection {
		public RTCConnection(RTCModel source, RTCModel target) {
			this.source = source;
			this.target = target;
		}
		public RTCModel source;
		public RTCModel target;
		
		public boolean equals(RTCConnection con) {
			return (source.equals(con.source) && target.equals(con.target));
		}
	}
	
	public RTCModel findRTC(String id, String instanceName) {
		if ( component != null && component.getId().equals(id) && component.getInstanceName().equals(instanceName) ) {
			return this;
		}
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext()) {
			TreeModelItem model = it.next();
			if ( model instanceof RTCModel ) {
				RTCModel ret = ((RTCModel)model).findRTC(id, instanceName);
				if ( ret != null) {
					return ret;
				}
			}
		}
		return null;
	}
	
	public String getHostName() {
		return component.getPathUri().split("/")[0];
	}
	
	public RTSystemItem getRTSystem() {
		return rtsystem;
	}
}

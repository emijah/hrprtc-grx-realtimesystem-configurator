package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.openrtp.repository.ProfileValidateException;
import org.openrtp.repository.RTSystemProfileOperator;
import org.openrtp.repository.xsd.rtsystem.Component;
import org.openrtp.repository.xsd.rtsystem.RtsProfile;
import org.openrtp.repository.xsd.rtsystem.TargetComponent;

public class RTCModel {
	private String nodeName;
	private RTCModel parent;
	private RTCModel top;
	private List<RTCModel> children = new ArrayList<RTCModel>();
	
	private RtsProfile profile;
	private Component component;
	private BenchmarkResultModel result;
	
    public static RTSystemProfileOperator rtsProfileOperator = new RTSystemProfileOperator();
	
	public RTCModel(String rtsProfilePath) {
	   	this.top = this;
		load(rtsProfilePath);
	}
	
    private RTCModel(RTCModel top, Component comp) {
    	this.top = top;
    	this.component = comp;
    	this.nodeName = comp.getInstanceName();
    }
	
	private void load(String fname) {
	   	try {
	   		rtsProfileOperator.loadProfile(fname);
	   		profile = rtsProfileOperator.getRtsProfile();
	   	} catch (ProfileValidateException e) {
	   		e.printStackTrace();
	   	}
	   	
    	nodeName = fname;
 
		Iterator<Component> it = profile.getComponent().iterator();
		while ( it.hasNext() ) {
			this.add(new RTCModel(this, it.next()));
		}
		this.updateStructure();
	}
	
	public String getName() {
		return nodeName;
	}
	
	public BenchmarkResultModel getResult() {
		return result;
	}
	
	public void setResult(BenchmarkResultModel result) {
		this.result = result;
	}
	
	public void add(RTCModel model) {
		children.add(model);
		if ( model.parent != null ) {
			model.parent.children.remove(model);
		}
		model.parent = this;
	}
	
	public RTCModel getTop() {
		return top;
	}
	
	public RTCModel getParent() {
		return parent;
	}
	
	public List<RTCModel> getChildren () {
		 return children;
	}
	
	public Component getComponent() {
		return component;
	}
	
	public RTCModel find(String id, String instanceName) {
		if ( component != null && component.getId().equals(id) && component.getInstanceName().equals(instanceName) ) {
			return this;
		}
		Iterator<RTCModel> it = children.iterator();
		while ( it.hasNext()) {
			RTCModel model = it.next();
			RTCModel ret = model.find(id, instanceName);
			if ( ret != null) {
				return ret;
			}
		}
		return null;
	}
	
	/*
	 * update model structure
	 */
	private void updateStructure() {
		for (int i=0;i<children.size(); i++) {
			RTCModel model = children.get(i);
			Component comp = model.component;
	 		if ( comp.getCompositeType().equals("PeriodicECShared") || comp.getCompositeType().equals("PeriodicStateShared")) {
	 			Iterator<TargetComponent> it2 = comp.getParticipants().getParticipant().iterator();
	 			while ( it2.hasNext() ) {
	 				TargetComponent tcomp = it2.next();
	 				RTCModel result = top.find(tcomp.getComponentId(), tcomp.getInstanceName());
	 				if ( result != null && !model.children.contains(result)) {
	 					model.add(result);
	 				}
	 			}
	 		}
		}
	}
}

package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jp.go.aist.rtm.toolscommon.profiles.util.XmlHandler;

import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.DataportConnector;
import org.openrtp.namespaces.rts.version02.Participants;
import org.openrtp.namespaces.rts.version02.RtsProfileExt;
import org.openrtp.namespaces.rts.version02.TargetComponent;
import org.openrtp.namespaces.rts.version02.TargetPort;

public class RTCModel {
	private String nodeName;
	private RTCModel parent;
	private RTCModel top;
	private List<RTCModel> children = new ArrayList<RTCModel>();
	private Component component;
	private BenchmarkResultModel result;
	
	private List<RTCModel> members;
	private List<RTCConnection> rtcConnections;
	private RtsProfileExt profile;
	
    public static XmlHandler rtsProfileOperator = new XmlHandler();
	
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
			getTop().profile = rtsProfileOperator.loadXmlRts(fname);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	nodeName = fname;
 
    	// update the whole list of the member of this system
    	members = new ArrayList<RTCModel>();
		Iterator<Component> it = getTop().profile.getComponents().iterator();
		while ( it.hasNext() ) {
			RTCModel model = new RTCModel(this, it.next());
			this.add(model);
			members.add(model);
		}
		this.updateStructure();
		
		// update the list of connection between RTCs
		rtcConnections = new ArrayList<RTCConnection>();
		Iterator<DataportConnector> connectors = getTop().profile.getDataPortConnectors().iterator();
		while ( connectors.hasNext() ) {
			DataportConnector con = connectors.next();
			TargetPort sourcePort = con.getSourceDataPort();
			TargetPort targetPort = con.getTargetDataPort();
			RTCModel smodel = getTop().find(sourcePort.getComponentId(), sourcePort.getInstanceName());
			RTCModel tmodel = getTop().find(targetPort.getComponentId(), targetPort.getInstanceName());
			rtcConnections.add(new RTCConnection(smodel, tmodel));
		}
	}
	
	public String getName() {
		return nodeName;
	}
	
	public String toString() {
		return getName();
	}
	
	public List<DataportConnector> getDataPortConnectors() {
		return getTop().profile.getDataPortConnectors();
	}
	
	public BenchmarkResultModel getResult() {
		if ( result == null ) {
			result = new BenchmarkResultModel();
		}
		return result;
	}
	
	public void setResult(BenchmarkResultModel result) {
		this.result = result;
		getParent().updateResult();
	}
	
	private void updateResult() {
		if ( this != top ) {
			getResult().reset();
			Iterator<RTCModel> it = getChildren().iterator();
			while ( it.hasNext() ) {
				RTCModel model = it.next();
				getResult().max += model.getResult().max;
				getResult().min += model.getResult().min;
				getResult().mean += model.getResult().mean;
			}
			getParent().updateResult();
		}
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
	
	public List<RTCModel> getRTCMembers() {
		return getTop().members;
	}
	
	public class RTCConnection {
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
	
	public List<RTCConnection> getRTCConnections() {
		return getTop().rtcConnections;
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
	 			Iterator<Participants> it2 = comp.getParticipants().iterator();
	 			while ( it2.hasNext() ) {
	 				TargetComponent tcomp = it2.next().getParticipant();
		 			RTCModel result = top.find(tcomp.getComponentId(), tcomp.getInstanceName());
		 			if ( result != null && !model.children.contains(result)) {
		 				model.add(result);
		 			}
	 			}
	 		}
		}
	}
	
	public String getHostName() {
		return component.getPathUri().split("/")[0];
	}
}

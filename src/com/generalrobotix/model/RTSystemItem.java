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

import com.generalrobotix.model.RTComponentItem.RTCConnection;

public class RTSystemItem extends TreeModelItem {
    private XmlHandler rtsProfileOperator = new XmlHandler();
	private RtsProfileExt profile;
	private List<RTComponentItem> members;
	private List<RTCConnection> rtcConnections;
	private String version;
	
	public RTSystemItem(String rtsProfilePath) {
		setRoot(this);
		load(rtsProfilePath);
	}
	
	public String getId() {
		return profile.getId();
	}
	
	private void load(String fname) {
	   	try {
			profile = rtsProfileOperator.loadXmlRts(fname);
	    	String[] ids = profile.getId().split(":");
	    	setName(ids[1].substring(ids[1].lastIndexOf(".")+1));
	    	version = ids[2];
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.updateStructure();
	}
	
	public List<RTComponentItem> getRTCMembers() {
		return members;
	}
	
	public List<DataportConnector> getDataPortConnectors() {
		return profile.getDataPortConnectors();
	}
	
	public List<RTCConnection> getRTCConnections() {
		return rtcConnections;
	}
	
	public RTComponentItem findRTC(String componentId, String instanceId) {
		Iterator<RTComponentItem> it = members.iterator();
		while ( it.hasNext() ) {
			RTComponentItem rtc = it.next();
			Component comp = rtc.getComponent();
			if ( comp.getId().equals(componentId) && comp.getInstanceName().equals(instanceId) ) {
				return rtc;
			}
		}
		return null;
	}
	
	/*
	 * update model structure
	 */
	private void updateStructure() {
    	// update the whole list of the member of this system
    	members = new ArrayList<RTComponentItem>();
		Iterator<Component> comps = profile.getComponents().iterator();
		while ( comps.hasNext() ) {
			Component comp = comps.next();
			String type = comp.getCompositeType();
			RTComponentItem item = null;
			if ( type.equals("PeriodicECShared") || type.equals("PeriodicStateShared")) {
				item = new ExecutionContextItem(this, comp);
			} else {
				item = new RTComponentItem(this, comp);
			}
			this.add(item);
			members.add(item);
		}

		Iterator<RTComponentItem> rtcs = members.iterator();
		while ( rtcs.hasNext() ) {
			RTComponentItem rtc = rtcs.next();
	 		if ( rtc instanceof ExecutionContextItem ) {
	 			Iterator<Participants> participants = rtc.getComponent().getParticipants().iterator();
	 			while ( participants.hasNext() ) {
	 				TargetComponent tcomp = participants.next().getParticipant();
		 			RTComponentItem result = findRTC(tcomp.getComponentId(), tcomp.getInstanceName());
		 			if ( result != null && !rtc.getChildren().contains(result)) {
		 				rtc.add(result);
		 			}
	 			}
	 		}
		}
		
		
		// update the list of connection between RTCs
		rtcConnections = new ArrayList<RTCConnection>();
		Iterator<DataportConnector> connectors = profile.getDataPortConnectors().iterator();
		while ( connectors.hasNext() ) {
			DataportConnector con = connectors.next();
			TargetPort sourcePort = con.getSourceDataPort();
			TargetPort targetPort = con.getTargetDataPort();
			RTComponentItem smodel = findRTC(sourcePort.getComponentId(), sourcePort.getInstanceName());
			RTComponentItem tmodel = findRTC(targetPort.getComponentId(), targetPort.getInstanceName());
			rtcConnections.add(new RTCConnection(smodel, tmodel));
		}
	}
	
	public String getVersion() {
		return version;
	}
}

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

import com.generalrobotix.model.RTCModel.RTCConnection;

public class RTSystemItem extends TreeModelItem {
    private XmlHandler rtsProfileOperator = new XmlHandler();
	private RtsProfileExt profile;
	private List<RTCModel> members;
	private List<RTCConnection> rtcConnections;
	private String version;
	
	public RTSystemItem(String rtsProfilePath) {
		setRoot(this);
		load(rtsProfilePath);
	}
	
	private void load(String fname) {
	   	try {
			profile = rtsProfileOperator.loadXmlRts(fname);
		} catch (Exception e) {
			e.printStackTrace();
		}
    	String[] ids = profile.getId().split(":");
    	setName(ids[1].substring(ids[1].lastIndexOf(".")+1));
    	version = ids[2];System.out.println("version:"+version);
 
    	// update the whole list of the member of this system
    	members = new ArrayList<RTCModel>();
		Iterator<Component> it = profile.getComponents().iterator();
		while ( it.hasNext() ) {
			RTCModel model = new RTCModel(this, it.next());
			this.add(model);
			members.add(model);
		}
		this.updateStructure();
		
		// update the list of connection between RTCs
		rtcConnections = new ArrayList<RTCConnection>();
		Iterator<DataportConnector> connectors = profile.getDataPortConnectors().iterator();
		while ( connectors.hasNext() ) {
			DataportConnector con = connectors.next();
			TargetPort sourcePort = con.getSourceDataPort();
			TargetPort targetPort = con.getTargetDataPort();
			RTCModel smodel = findRTC(sourcePort.getComponentId(), sourcePort.getInstanceName());
			RTCModel tmodel = findRTC(targetPort.getComponentId(), targetPort.getInstanceName());
			rtcConnections.add(new RTCConnection(smodel, tmodel));
		}
	}
	
	public List<RTCModel> getRTCMembers() {
		return members;
	}
	
	public List<DataportConnector> getDataPortConnectors() {
		return profile.getDataPortConnectors();
	}
	
	public List<RTCConnection> getRTCConnections() {
		return rtcConnections;
	}
	
	public RTCModel findRTC(String componentId, String instanceId) {
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem item = it.next();
			if ( item instanceof RTCModel) {
				RTCModel ret = ((RTCModel)item).findRTC(componentId, instanceId);
				if ( ret != null) {
					return ret;
				}
			}
		}
		return null;
	}
	
	
	/*
	 * update model structure
	 */
	private void updateStructure() {
		for (int i=0;i<getChildren().size(); i++) {
			TreeModelItem model = getChildren().get(i);
			if ( !(model instanceof RTCModel) ) {
				continue;
			}
			Component comp = ((RTCModel)model).getComponent();
	 		if ( comp.getCompositeType().equals("PeriodicECShared") || comp.getCompositeType().equals("PeriodicStateShared")) {
	 			Iterator<Participants> it2 = comp.getParticipants().iterator();
	 			while ( it2.hasNext() ) {
	 				TargetComponent tcomp = it2.next().getParticipant();
		 			RTCModel result = findRTC(tcomp.getComponentId(), tcomp.getInstanceName());
		 			if ( result != null && !model.getChildren().contains(result)) {
		 				model.add(result);
		 			}
	 			}
	 		}
		}
	}
	
	public String getVersion() {
		return version;
	}
}

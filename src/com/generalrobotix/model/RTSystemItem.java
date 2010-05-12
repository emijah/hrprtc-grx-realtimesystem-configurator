package com.generalrobotix.model;

import java.io.File;
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

public class RTSystemItem extends TreeModelItem
{
    private XmlHandler rtsProfileOperator = new XmlHandler();
	private RtsProfileExt profile;
	private File file;
	private List<RTComponentItem> members;
	private List<RTCConnection> rtcConnections;
	private List<ExecutionContextItem> eclist;
	private String version;
	private static final String ICON_PATH = "icons/grxRTS.png";
	
	public RTSystemItem(String rtsProfilePath)
	{
		setRoot(this);
		load(rtsProfilePath);
		setIconPath(ICON_PATH);
	}
	
	public String getId()
	{
		return profile.getId();
	}
	
	public File getFilePath()
	{
		return file;
	}
	
	private void load(String fname)
	{
	   	try {
	   		file = null;
			profile = rtsProfileOperator.loadXmlRts(fname);
	    	String[] ids = profile.getId().split(":");
	    	setName(ids[1].substring(ids[1].lastIndexOf(".")+1));
	    	file = new File(fname).getParentFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.updateStructure();
	}
	
	public List<RTComponentItem> getRTCMembers()
	{
		return members;
	}
	
	public List<ExecutionContextItem> getExecutionContexts()
	{
		return eclist;
	}
	
	public List<DataportConnector> getDataPortConnectors()
	{
		return profile.getDataPortConnectors();
	}
	
	public List<RTCConnection> getRTCConnections()
	{
		return rtcConnections;
	}
	
	public RTComponentItem findRTC(String componentId, String instanceId)
	{
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
	private void updateStructure()
	{
    	// step1.update the list of rtc item and ec item
    	members = new ArrayList<RTComponentItem>();
    	eclist = new ArrayList<ExecutionContextItem>();
		Iterator<Component> comps = profile.getComponents().iterator();
		while ( comps.hasNext() ) {
			Component comp = comps.next();
			String type = comp.getCompositeType();
			RTComponentItem item = null;
			if ( type.equals("PeriodicECShared") || type.equals("PeriodicStateShared")) {
				item = new ExecutionContextItem(this, comp);
				eclist.add((ExecutionContextItem) item);
			} else {
				item = new RTComponentItem(this, comp);
			}
			members.add(item);
		}

		// step2.update the tree structure
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
		
		// step3.insert ec item for not owned rtc
		List<TreeModelItem> children = this.getChildren();
		for (int i=0; i<children.size(); i++) {
			TreeModelItem item = children.get(i);
			if ( !(item instanceof ExecutionContextItem) && (item instanceof RTComponentItem) ) { // TODO prepare common base class
				RTComponentItem rtc = (RTComponentItem)item;
				ExecutionContextItem ecItem = new ExecutionContextItem(this, rtc.getComponent());
				ecItem.add(rtc);
				members.add(ecItem);
				eclist.add(ecItem);
			}
		}
		
		// step4.update the list of connection between RTCs
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
	
	public String getVersion()
	{
		return version;
	}
} 
package com.generalrobotix.ui.util;

import java.util.List;

import org.omg.CORBA.*;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import _SDOPackage.NameValue;

import RTC.ComponentProfile;
import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
import RTC.ExecutionContext;
import RTC.PortInterfaceProfile;
import RTC.PortProfile;
import RTC.PortService;
import RTC.RTObject;
import RTC.RTObjectHelper;
import RTM.Manager;
import RTM.ManagerHelper;

public class GrxRTMUtil 
{
	static ORB orb;
	
	private static void init() 
	{
		if ( orb == null ) {
			java.util.Properties props = System.getProperties();
			props.put("org.omg.CORBA.ORBSingletonClass","com.ooc.CORBA.ORBSingleton");
			orb = ORB.init(new String[]{}, props);
		}
	}
	
	public static NamingContext getRootNamingContext(String host, int port) 
	{
		init();
		org.omg.CORBA.Object obj = orb.string_to_object("corbaloc:iiop:"+host+":"+port+"/NameService");
		return NamingContextHelper.narrow(obj);
	}
	
	public static Manager findRTCmanager(String hostname, int port) 
	{
		init();
		org.omg.CORBA.Object obj = orb.string_to_object("corbaloc:iiop:"+hostname+":"+port+"/manager");
		Manager manager = ManagerHelper.narrow(obj);
		return manager;
	}
	
	public static Manager findRTCmanager(String hostname, NamingContext rnc) 
	{
		init();
		org.omg.CORBA.Object cxt = findObject(hostname, "host_cxt", rnc);
		org.omg.CORBA.Object obj = findObject("manager", "mgr", NamingContextHelper.narrow(cxt));
		Manager manager = ManagerHelper.narrow(obj);
		return manager;
	}
	
	public static org.omg.CORBA.Object findObject(String name, String kind, NamingContext rnc) 
	{
		init();
		if ( rnc == null ) {
			return null;
		}
		NameComponent nc = new NameComponent();
		nc.id = name;
		nc.kind = kind;
		try {
			return rnc.resolve(new NameComponent[]{nc});
		} catch (NotFound e) {
			//e.printStackTrace();
		} catch (CannotProceed e) {
			//e.printStackTrace();
		} catch (InvalidName e) {
			//e.printStackTrace();
		}
		return null;
	}
	
	public static RTObject findRTC(String name, NamingContext rnc) 
	{
		init();
		try {
			org.omg.CORBA.Object obj = findObject(name, "rtc", rnc);
			return RTObjectHelper.narrow(obj);
		} catch (Exception e) {
			return null;
		}
	}
	
	public static PortService findPort(RTObject rtc, String portName)
	{
		PortService[] ports    = rtc.get_ports();
		String instanceName = rtc.get_component_profile().instance_name;
		if ( portName.indexOf(instanceName + ".") < 0 )  {
			portName = instanceName + "." + portName;
		}
		for (int i=0; i<ports.length; i++) {
			PortProfile prof = ports[i].get_port_profile();
			if ( prof.name.equals(portName) ) {
				return ports[i];
			}
		}
		return null;
	}
	
	public static org.omg.CORBA.Object findService(RTObject rtc, String svcname) 
	{
		init();
		ComponentProfile prof= rtc.get_component_profile();
		PortProfile[] port_prof = prof.port_profiles;
		PortService port = null;
		for (int i=0; i<port_prof.length; i++) {
			System.out.println("name:"+port_prof[i].name);
			PortInterfaceProfile[] ifs = port_prof[i].interfaces;
			for (int j=0; j<ifs.length; j++) {
				if ( ifs[j].instance_name.equals(svcname) ) {
					port = port_prof[i].port_ref;
				}
			}
		}
		if ( port == null ) {
			return null;
		}
		ConnectorProfile con_prof = new ConnectorProfile();
		con_prof.name = "noname";
		con_prof.connector_id = "";
		con_prof.ports = new PortService[] {port};
		con_prof.properties = new NameValue[]{};
		ConnectorProfileHolder con_prof_holder = new ConnectorProfileHolder();
		con_prof_holder.value = con_prof;
		port.connect(con_prof_holder);
		port.disconnect_all();
		String ior = con_prof_holder.value.properties[0].value.extract_string();
		return orb.string_to_object(ior);
	}
	
	public static boolean releaseObject(org.omg.CORBA.Object obj) 
	{
		try {
			obj._release();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	public static void serializeComponents(List<RTObject> rtcs)
	{
		if ( rtcs.size() == 0 ) {
			return;
		}
		ExecutionContext ec = rtcs.get(0).get_owned_contexts()[0];
		String owner = rtcs.get(0).get_component_profile().instance_name;
		for (int i=1; i<rtcs.size(); i++) {
			ExecutionContext[] pecs = rtcs.get(i).get_participating_contexts();
			boolean alreadyAdded = false;
			for (int j=0; j<pecs.length; j++) {
				if ( ec._is_equivalent(pecs[j]) ) {
					System.out.println(rtcs.get(i).get_component_profile().instance_name + " is already added to " + owner+ " "+ pecs.length);
					alreadyAdded = true;
					break;
				}
			}
			if ( !alreadyAdded ) {
				ec.add_component(rtcs.get(i));
				System.out.println(rtcs.get(i).get_component_profile().instance_name + " is added to " + owner);
			}
		}
	}
	
	public static void activateComponents(List<RTObject> rtcs)
	{
		for (int i=0; i<rtcs.size(); i++) {
			RTObject rtc = rtcs.get(i);
			ExecutionContext[] pecs = rtc.get_participating_contexts();
			if ( pecs.length > 0 ) {
				pecs[0].activate_component(rtc);
				System.out.println(rtc.get_component_profile().instance_name + " is activated by part. ec."+ rtc.get_participating_contexts().length + ":"+ rtc.get_owned_contexts().length);
			} else {
				rtc.get_owned_contexts()[0].activate_component(rtc);
				System.out.println(rtc.get_component_profile().instance_name + " is activated by own ec."+ rtc.get_participating_contexts().length + ":"+ rtc.get_owned_contexts().length);
			}
		}
	}
	
	public static NameValue createNameValue(String name, String value)
	{
		_SDOPackage.NameValue nv = new _SDOPackage.NameValue();
		nv.name = name;
		nv.value = orb.create_any();
		nv.value.insert_string(value);
		return nv;
	}
}

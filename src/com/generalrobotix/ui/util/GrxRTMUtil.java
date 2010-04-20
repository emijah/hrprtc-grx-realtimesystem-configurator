package com.generalrobotix.ui.util;

import org.omg.CORBA.*;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import com.generalrobotix.model.RTComponentItem;

import _SDOPackage.NameValue;

import RTC.ComponentProfile;
import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
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
}

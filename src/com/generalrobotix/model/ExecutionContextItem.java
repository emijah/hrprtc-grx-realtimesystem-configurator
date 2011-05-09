package com.generalrobotix.model;

import java.util.List;

import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.ExecutionContext;
import org.openrtp.namespaces.rts.version02.Participants;

public class ExecutionContextItem extends RTComponentItem{
	private static final String ICON_PATH = "icons/ExecutionContext.png";
	public ExecutionContext ec;
	
	public ExecutionContextItem(RTSystemItem rtsystem) 
	{
		super(rtsystem);
		setIconPath(ICON_PATH);
		this.ec = new ExecutionContext();
	}
	
	public ExecutionContextItem(RTSystemItem rtsystem, Component comp) 
	{
		super(rtsystem, comp);
		setIconPath(ICON_PATH);
		ec = comp.getExecutionContexts().get(0);
	}
	
	public String getId() 
	{
		return ec.getId();
	}
	
	public double getRate() 
	{
		return ec.getRate();
	}
	
	public String getOwnerName() 
	{
		// for kanehiro script
		// assume the first member of Execution context is owner
		// if there is no participants, use own name
		List<Participants> p = getComponent().getParticipants();
		if ( p.size()>0 ) {
			return p.get(0).getParticipant().getInstanceName();
		}
		return getName();
	}
	
    public String getIconPath()
    {
    	switch(state_) {
    	case RTC_NOT_EXIST:
    		return "icons/Zombie.gif";
    	case RTC_SLEEP:
    		return "icons/ExecutionContext.png";
    	case RTC_ACTIVE:
    		return "icons/ExecutionContextActive.png";
    	case RTC_BENCHMARK_AVAILABLE:
    		return "icons/ExecutionContextMeasure.png";
    	default:
    		return "icons/Zombie.gif";
    	}
    }
}

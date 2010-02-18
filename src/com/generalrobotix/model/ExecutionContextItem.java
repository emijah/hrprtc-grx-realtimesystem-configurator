package com.generalrobotix.model;

import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.ExecutionContext;

public class ExecutionContextItem extends RTComponentItem{
	private static final String ICON_PATH = "icons/view_pan_on.png";
	private ExecutionContext ec;
	
	public ExecutionContextItem(RTSystemItem rtsystem, Component comp) {
		super(rtsystem, comp);
		setIconPath(ICON_PATH);
		ec = comp.getExecutionContexts().get(0);
	}
	
	public String getId() {
		return ec.getId();
	}
	
	public double getRate() {
		return ec.getRate();
	}
}

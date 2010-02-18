package com.generalrobotix.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class TreeModelItem implements IPropertySource {
	private String nodeName = "root";
	private TreeModelItem root = this;
	private TreeModelItem parent;
	private List<TreeModelItem> children = new ArrayList<TreeModelItem>();
	private List<TreeModelItem> checkedItems = new ArrayList<TreeModelItem>();
	private Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
	protected String iconpath = null;
	
	public String getName() {
		return nodeName;
	}
	
	protected void setName(String name) {
		nodeName = name;
		properties.put("name", name);
	}
	
	public String toString() {
		return getName();
	}
	
	public String getIconPath() {
		return iconpath;
	}
	
	public void setIconPath(String path) {
		iconpath = path;
	}
	
	public TreeModelItem getRoot() {
		return root;
	}
	
	protected void setRoot(TreeModelItem item) {
		root = item;
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext() ) {
			it.next().setRoot(item);
		}
		properties.put("root", root.toString());
	}
	
	public boolean isRoot() {
		return ( root == null );
	}
	
	public boolean hasParent() {
		return ( parent != null );
	}
	
	public TreeModelItem getParent(){
		return parent;
	}
	
	public List<TreeModelItem> getChildren(){
		return children;
	}
		
	public boolean hasChildren() {
		return (this.children.size() > 0);
	}

	public void add(TreeModelItem model) {
		children.add(model);
		if ( model.parent != null ) {
			model.parent.children.remove(model);
		}
		model.parent = this;
		model.setRoot(getRoot());
	}
	
	public void setCheckedItems(TreeModelItem[] list) {
		checkedItems.clear();
		checkedItems.addAll(Arrays.asList(list));
	}
	
	public List<TreeModelItem> getCheckedItems() {
		return checkedItems;
	}

	public Object getEditableValue() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private IPropertyDescriptor[] propertyDescriptors;
	
	public IPropertyDescriptor[] getPropertyDescriptors() {
		Object[] keys = properties.keySet().toArray();
		if ( propertyDescriptors == null ) {
			propertyDescriptors = new IPropertyDescriptor[keys.length];
			for (int i=0; i<keys.length; i++) {
				propertyDescriptors[i] = new PropertyDescriptor(keys[i], keys[i].toString());
			}
		}
		return propertyDescriptors;
	}

	public Object getPropertyValue(Object id) {
		return properties.get(id);
	}

	public boolean isPropertySet(Object id) {
		// TODO Auto-generated method stub
		return false;
	}

	public void resetPropertyValue(Object id) {
		// TODO Auto-generated method stub
	}

	public void setPropertyValue(Object id, Object value) {
		properties.put(id, value);		
	}
	
	public Map<Object, Object> getPropertyMap() {
		return properties;
	}
}

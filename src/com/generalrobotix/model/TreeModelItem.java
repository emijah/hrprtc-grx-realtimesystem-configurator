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

public class TreeModelItem implements IPropertySource
{
	private String nodeName = "root";
	private TreeModelItem root = this;
	private TreeModelItem parent;
	private List<TreeModelItem> children = new ArrayList<TreeModelItem>();
	private List<TreeModelItem> checkedItems = new ArrayList<TreeModelItem>();
	public Map<Object, Object> properties = new LinkedHashMap<Object, Object>();
	protected String iconpath = null;
	protected IPropertyDescriptor[] propertyDescriptors;
	
	public TreeModelItem()
	{
	}
	
	public TreeModelItem(String name)
	{
		setName(name);
	}
	
	public String getName()
	{
		return nodeName;
	}
	
	public void setName(String name)
	{
		nodeName = name;
		properties.put("name", name);
	}
	
	public String toString()
	{
		return getName();
	}
	
	public String getIconPath()
	{
		return iconpath;
	}
	
	public void setIconPath(String path)
	{
		iconpath = path;
	}
	
	public TreeModelItem getRoot()
	{
		return root;
	}
	
	protected void setRoot(TreeModelItem item)
	{
		root = item;
		Iterator<TreeModelItem> it = getChildren().iterator();
		while ( it.hasNext() ) {
			it.next().setRoot(item);
		}
		properties.put("root", root.toString());
	}
	
	public boolean isRoot()
	{
		return ( root == null );
	}
	
	public boolean hasParent()
	{
		return ( parent != null );
	}
	
	public TreeModelItem getParent()
	{
		return parent;
	}
	
	public List<TreeModelItem> getChildren()
	{
		return children;
	}
		
	public boolean hasChildren()
	{
		return (this.children.size() > 0);
	}

	public void add(TreeModelItem model)
	{
		children.add(model);
		if ( model.parent != null ) {
			model.parent.children.remove(model);
		}
		model.parent = this;
		model.setRoot(getRoot());
	}
	
	public void removeChild(TreeModelItem child)
	{
		children.remove(child);
	}
	
	public void removeChildren()
	{
		children.clear();
	}
	
	public void setCheckedItems(TreeModelItem[] list)
	{
		checkedItems.clear();
		checkedItems.addAll(Arrays.asList(list));
	}
	
	public List<TreeModelItem> getCheckedItems()
	{
		return checkedItems;
	}

	public Object getEditableValue() 
	{
		return null;
	}
	
	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		Object[] keys = properties.keySet().toArray();
		propertyDescriptors = new IPropertyDescriptor[keys.length];
		for (int i=0; i<keys.length; i++) {
			propertyDescriptors[i] = new PropertyDescriptor(keys[i], keys[i].toString());
		}
		return propertyDescriptors;
	}

	public Object getPropertyValue(Object id)
	{
		return properties.get(id);
	}

	public boolean isPropertySet(Object id)
	{
		return false;
	}

	public void resetPropertyValue(Object id)
	{
	}

	public void setPropertyValue(Object id, Object value)
	{
		properties.put(id, value);		
	}
	
	public Map<Object, Object> getPropertyMap()
	{
		return properties;
	}
	
	public TreeModelItem find(String name)
	{
		Iterator<TreeModelItem> it = children.iterator();
		while ( it.hasNext() ) {
			TreeModelItem item = it.next();
			if ( item.getName().equals(name) ) {
				return item;
			}
		}
		return null;
	}
}

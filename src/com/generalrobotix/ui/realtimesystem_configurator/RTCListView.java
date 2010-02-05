package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;
import org.openrtp.repository.ProfileValidateException;
import org.openrtp.repository.RTSystemProfileOperator;
import org.openrtp.repository.xsd.rtsystem.Component;
import org.openrtp.repository.xsd.rtsystem.RtsProfile;

public class RTCListView extends ViewPart {
	private TableViewer viewer;

    public RTSystemProfileOperator rtsProfileOperator = new RTSystemProfileOperator();
    private RtsProfile profile; 
    
	private NullProgressMonitor progress;
    
	/**
	 * The constructor.
	 */
	public RTCListView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		Button btnLoad = new Button(parent, SWT.NONE);
		btnLoad.setText("Load RTSystem Profile");
		btnLoad.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				FileDialog fdlg = new FileDialog(((Button)e.getSource()).getShell(), SWT.OPEN);
				fdlg.setFilterExtensions( new String[] {"*.xml"} );
				
				
				fdlg.open();
				String fname = fdlg.getFileName();
				if ( fname != null && fname != "" ) {
					profile = getRTSProfile(fdlg.getFilterPath()+java.io.File.separator+fname);
					viewer.refresh();
					try {
						createProjectDir(fname.replace(".xml", ""));
					} catch (CoreException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		});		
		
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(new Object[]{});
		
		Table table = viewer.getTable();
		GridData tableLayoutData = new GridData(GridData.FILL_BOTH);
		table.setLayoutData(tableLayoutData);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn column = new TableColumn(table, SWT.LEFT, 0);
		column.setText("RTC Instance");
		column.setWidth(200);
		
		column = new TableColumn(table, SWT.LEFT, 1);
		column.setText("EC ID");
		column.setWidth(150);
		
		column = new TableColumn(table, SWT.LEFT, 2);
		column.setText("RealTime");
		column.setWidth(100);
		
		column = new TableColumn(table, SWT.LEFT, 3);
		column.setText("test1");
		column.setWidth(100);
		
		column = new TableColumn(table, SWT.LEFT, 4);
		column.setText("test2");
		column.setWidth(100);
	}
	
	public void createProjectDir(String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if ( !project.exists() ) {
			project.create(progress);
			project.open(progress);
		}
		
		IFolder folderScripts = project.getFolder("scripts");
		if ( !folderScripts.exists() ) {
			folderScripts.create(true, true, progress);
		}
		
		IFolder folderIdls = project.getFolder("idls");
		if ( !folderIdls.exists() ) {
			folderIdls.create(true, true, progress);
		}

		IFile file = project.getFile("scripts/rtc.conf");
		if ( !file.exists() ) {
			file.create(new ByteArrayInputStream(new byte[0]), true, progress);
		}
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
    public RtsProfile getRTSProfile(String fname) {
    	if ( fname != null)  {
            try {
    			rtsProfileOperator.loadProfile(fname);
    	        return rtsProfileOperator.getRtsProfile();
    		} catch (ProfileValidateException e) {
    			e.printStackTrace();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	}
		return null;
    }
     
	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		public void dispose() {
		}
		public Object[] getElements(Object parent) {
			if ( profile != null) {
				return profile.getComponent().toArray();
			}
			return new Object[]{};
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			Component comp = (Component)obj;
			switch(index) {
			case 0:
				return comp.getInstanceName();
			case 1:
				return comp.getCompositeType();
			case 2:
				return comp.getPathUri();
			case 3:
				return comp.getId();
			default:
				break;
			}
			return getText(obj);
		}
		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return null;//PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}
	class NameSorter extends ViewerSorter {
	}
}

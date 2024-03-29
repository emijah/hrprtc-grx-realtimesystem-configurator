package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.openrtp.namespaces.rts.version02.Component;

import com.generalrobotix.model.RTComponentItem;

public class RTCListView extends ViewPart {
	private TreeViewer viewer;
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
		getSite().getPage().addSelectionListener(new ISelectionListener() {
	        public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
	        	if (sourcepart != RTCListView.this &&
	        			selection instanceof IStructuredSelection) {
	        		List ret = ((IStructuredSelection) selection).toList();
	        		if ( ret.size() > 0 && ret.get(0) instanceof RTComponentItem ) {
	        			viewer.setInput(((RTComponentItem)ret.get(0)).getRTSystem());
	        			viewer.refresh();
	        		}
	            }
	        }
	    });
		
		GridLayout layout = new GridLayout(1, false);
		parent.setLayout(layout);
		
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());

		Tree tree = viewer.getTree();
		TreeItem root = new TreeItem(tree, SWT.NONE);
		GridData tableLayoutData = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(tableLayoutData);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		
		TreeColumn column = new TreeColumn(tree, SWT.LEFT, 0);
		column.setText("Execution Context");
		column.setWidth(200);
		
		column = new TreeColumn(tree, SWT.LEFT, 1);
		column.setText("Asigned Freq.[Hz]");
		column.setWidth(150);
		
		column = new TreeColumn(tree, SWT.LEFT, 2);
		column.setText("Max.");
		column.setWidth(100);
		
		column = new TreeColumn(tree, SWT.LEFT, 3);
		column.setText("Min.");
		column.setWidth(100);
		
		column = new TreeColumn(tree, SWT.LEFT, 4);
		column.setText("Average");
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
     
	class ViewContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			return getChildren(parent);
		}
		
		public Object[] getChildren(Object parentElement) {
			if ( parentElement instanceof RTComponentItem ) {
				return ((RTComponentItem)parentElement).getChildren().toArray();
			}
			return null;
		}
		
		public Object getParent(Object element) {
			return ((RTComponentItem)element).getParent();
		}
		
		public boolean hasChildren(Object element) {
			return (((RTComponentItem)element).getChildren().size() > 0);
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			try {
				RTComponentItem model = (RTComponentItem)obj;
				Component comp = model.getComponent();
				if (comp == null) 
					return "";
				switch(index) {
				case 0:
					return model.getName();
				case 1:
					return String.valueOf(comp.getExecutionContexts().get(0).getRate());
				case 2:
					if (model.getResult() != null) {
						return ""+model.getResult().max;
					}
					return "-";
				case 3:
					if (model.getResult() != null) {
						return ""+model.getResult().mean;
					}
					return "-";
				case 4:
					if (model.getResult() != null) {
						return ""+model.getResult().min;
					}
					return "-";
				default:
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
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

package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;

import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;

public class BenchmarkResultExplorer extends ViewPart {
	private IProject project;
	private TreeModelItem rootItem = new TreeModelItem();
	private CheckboxTreeViewer resultViewer;
	private NullProgressMonitor progress;
	
	public BenchmarkResultExplorer() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		// TODO show dialog to confirm create project
		try {
			project = createProjectDir("RealtimeSystemConfiguratorRepository");
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		
		resultViewer = new CheckboxTreeViewer(parent, SWT.NONE);
		resultViewer.setContentProvider(new ViewContentProvider());
		resultViewer.setLabelProvider(new ViewLabelProvider());
		resultViewer.setInput(rootItem);
		resultViewer.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				TreeModelItem item = (TreeModelItem)event.getElement();
				resultViewer.setSubtreeChecked(item, event.getChecked());
				Object[] objList = resultViewer.getCheckedElements();
				TreeModelItem[] itemList = Arrays.asList(objList).toArray(new TreeModelItem[0]);
				item.getRoot().setCheckedItems(itemList);
			}
		});
		
		getSite().setSelectionProvider(resultViewer);
		
		Tree tree = resultViewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		setHeader(tree, "RTSystem Name", 300, SWT.LEFT, 0);
		setHeader(tree, "Version",		 100, SWT.LEFT, 1);
		setHeader(tree, "Date", 		 100, SWT.LEFT, 2);
		
		Button btnLoad = new Button(parent, SWT.NONE);
		btnLoad.setText("Import RTSystem Profile");
		btnLoad.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				FileDialog fdlg = new FileDialog(((Button)e.getSource()).getShell(), SWT.OPEN);
				fdlg.setFilterExtensions( new String[] {"*.xml"} );
				fdlg.open();
				String fname = fdlg.getFilterPath() + java.io.File.separator + fdlg.getFileName();
				importProfile(fname);
			}
		});	
		
		updateList();
		resultViewer.refresh();
	}

	private void importProfile(String fname) {
		if ( fname != null && fname != "" ) {
			try {
				File srcFile = new File(fname);
				RTSystemItem system = new RTSystemItem(fname);
				rootItem.add(system);
				String systemId = system.getName();
				IFolder destFolder  = project.getFolder(systemId);
				if ( !destFolder.exists() ) {
					destFolder.create(false, true, progress);
				}
				
				IPath destPath = destFolder.getFile(srcFile.getName()).getLocation();

				FileChannel srcChannel  = new FileInputStream(srcFile.getAbsolutePath()).getChannel();
				FileChannel destChannel = new FileOutputStream(destPath.toString()).getChannel();
				try {
					srcChannel.transferTo(0, srcChannel.size(), destChannel);
				} finally {
					srcChannel.close();
					destChannel.close();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			resultViewer.refresh();
		}
	}
	
	@Override
	public void setFocus() {
	}
	
	public IProject createProjectDir(String projectName) throws CoreException {
		IProject ret = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if ( !ret.exists() ) {
			ret.create(null);
			ret.open(progress);
		}
		return ret;
	}

	private void updateList() {
		File[] files = project.getLocation().toFile().listFiles();
		for (int i=0; i<files.length; i++) {
			if ( files[i].isDirectory() ) {
				File[] systemprofiles = files[i].listFiles(new FilenameFilter(){
					public boolean accept(File file, String name) {
						return ( name.endsWith(".xml") );
					}
				});
				
				RTSystemItem rts = new RTSystemItem(systemprofiles[0].getAbsolutePath());
				rootItem.add(rts);
			}
		}
	}
	
	class ViewContentProvider implements ITreeContentProvider {

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			return ((TreeModelItem)parent).getChildren().toArray();
		}
		
		public Object[] getChildren(Object parentElement) {
		    return ((TreeModelItem)parentElement).getChildren().toArray();
		}
		
		public Object getParent(Object element) {
			return ((TreeModelItem)element).getParent();
		}
		
		public boolean hasChildren(Object element) {
			return (getChildren(element).length > 0);
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			try {				
				TreeModelItem item = (TreeModelItem)obj;
				switch(index) {
				case 0:
					return item.getName();
				case 1:
					if ( item instanceof RTSystemItem ) {
						return ((RTSystemItem)item).getVersion();
					} else {
						return "-";
					}
				case 2:
					return "-";
				case 3:
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
	
	private void setHeader(Tree tree, String text, int width, int alignment, int index) {
		TreeColumn column = new TreeColumn(tree, alignment, index);
		column.setText(text);
		column.setWidth(width);
	}
}

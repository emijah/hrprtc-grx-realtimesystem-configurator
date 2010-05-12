package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.ho.yaml.Yaml;

import com.generalrobotix.model.BenchmarkResultItem;
import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;

public class BenchmarkResultExplorer extends ViewPart 
{
	private IProject project;
	private TreeModelItem rootItem = new TreeModelItem();
	private TreeViewer resultViewer;
	private NullProgressMonitor progress;
	private FileDialog fdlg;
	private List<Action> actionList = new ArrayList<Action>();
	private static BenchmarkResultExplorer this_;

	private static final SimpleDateFormat FORMAT_DATE1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static final String REALTIME_SYSTEM_PROJECT_NAME = "RealtimeSystemProjects";
	private static final String LOG_FILE_NAME = "result.yaml";

	public BenchmarkResultExplorer() 
	{
		this_ = this;
	}
	
	public static BenchmarkResultExplorer getInstance()
	{
		return this_;
	}

	@Override
	public void createPartControl(Composite parent) 
	{
		parent.setLayout(new GridLayout(1, false));
		
		fdlg = new FileDialog(parent.getShell(), SWT.OPEN);
		
		resultViewer = setupTreeViewer(parent);
		getSite().setSelectionProvider(resultViewer);
		Transfer[] transfers = new Transfer[] { org.eclipse.swt.dnd.FileTransfer.getInstance() };
		resultViewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, new MyViewerDropListener(resultViewer));
		
		actionList.add(new ImportAction());
		actionList.add(new SaveAction());

		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) 
			{
				Iterator<Action> it = actionList.iterator();
				while (it.hasNext()) {
					manager.add(it.next());
				}
			}
		});
		Menu menu = menuManager.createContextMenu(resultViewer.getControl());
		resultViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuManager, resultViewer);
		
		IMenuManager manager1 = getViewSite().getActionBars().getMenuManager();
		IToolBarManager manager2 = getViewSite().getActionBars().getToolBarManager();
		Iterator<Action> it = actionList.iterator();
		while (it.hasNext()) {
			Action action = it.next();
			manager1.add(action);
			manager2.add(action);
		}

		// TODO show dialog to confirm create project
		project = getProject(REALTIME_SYSTEM_PROJECT_NAME);
		updateList();
	}

	@Override
	public void setFocus()
	{
	}
	
	private IProject getProject(String projectName)
	{
		IProject ret = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		try {
			if ( !ret.exists() ) {
				ret.create(progress);
				System.out.println(projectName +" is created.");
			}
			if ( !ret.isOpen() ) {
				ret.open(progress);
				System.out.println(projectName +" is opened.");
			}
			ret.refreshLocal(IResource.DEPTH_INFINITE, progress);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return ret;
	}

	private void updateList() 
	{
		File[] files = project.getLocation().toFile().listFiles();
		for (int i=0; files != null && i<files.length; i++) {
			if ( files[i].isDirectory() ) {
				File[] systemprofiles = files[i].listFiles(new FilenameFilter(){
					public boolean accept(File file, String name)
					{
						return ( name.endsWith(".xml") );
					}
				});
				
				if ( systemprofiles.length > 0 ) {
					RTSystemItem system = new RTSystemItem(systemprofiles[0].getAbsolutePath());
					TreeModelItem systemRoot = new TreeModelItem(system.getId());
					systemRoot.add(system);
					TreeModelItem resultRoot = new TreeModelItem("ResultRoot");
					systemRoot.add(resultRoot);
					rootItem.add(systemRoot);
					IFolder folder = project.getFolder(system.getId()+"/results");
					try {
						IResource[] members = folder.members();
						for (int j=0; j<members.length; j++ ) {
							if ( members[j].getType() == IResource.FILE && members[j].getName().endsWith(".yaml")) {
								TreeModelItem item = new TreeModelItem(members[j].getName());
								resultRoot.add(item);
							}
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}
					
				}
			}
		}
		resultViewer.refresh();
	}
	
	private void loadResult(String filename, RTSystemItem rts)
	{
		// load result
		try {
			IFolder folder = project.getFolder(rts.getId());
			if ( folder.findMember(filename, false) != null ) {
				IFile file = folder.getFile(filename);
				Map<String, BenchmarkResultItem> ret = fromYaml(file);
				if ( ret != null ) {
					Iterator<RTComponentItem> rtcs = rts.getRTCMembers().iterator();
					while(rtcs.hasNext()) {
						RTComponentItem rtc = rtcs.next();
						rtc.setResult(ret.get(rtc.getId()));
						double cycle = 1.0/rtc.getComponent().getExecutionContexts().get(0).getRate();
						rtc.getResult().setCycle(cycle);
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Exception occured during loading log file.");
		}
		// calculate summation
		Iterator<RTComponentItem> it = rts.getRTCMembers().iterator();
		while ( it.hasNext() ) {
			RTComponentItem model = it.next();
			if ( model instanceof ExecutionContextItem ) {
				((ExecutionContextItem)model).calcSummation();
			}
		}
	}
	
	private class ImportAction extends Action
	{
		public ImportAction()
		{
			setText("Import RTSystem Profile");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/folder_open.png"));
		}
		
		public void run()
		{
			fdlg.setFilterExtensions( new String[] {"*.xml"} );
			fdlg.open();
			String fname = fdlg.getFilterPath() + java.io.File.separator + fdlg.getFileName();
			if ( fname != null ) {
				importProfile(fname);
			}
		}
	};
	
	private class SaveAction extends Action
	{
		public SaveAction() 
		{
			setText("Save All Benchmark Results");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/save.png"));
		}
	
		public void run()
		{
			save();
		}
	};

	private class ViewContentProvider implements ITreeContentProvider
	{

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}
		
		public Object[] getElements(Object parent)
		{
			return ((TreeModelItem)parent).getChildren().toArray();
		}
		
		public Object[] getChildren(Object parentElement)
		{
			TreeModelItem item = (TreeModelItem)parentElement;
			if ( item.getParent() == rootItem ) {
				return item.find("ResultRoot").getChildren().toArray();
			}
		    return item.getChildren().toArray();
		}
		
		public Object getParent(Object element)
		{
			return ((TreeModelItem)element).getParent();
		}
		
		public boolean hasChildren(Object element)
		{
			Object[] list = getChildren(element);
			return ( list != null && list.length > 0);
		}
	}
	
	private class ViewLabelProvider extends LabelProvider 
	{
	    private HashMap<ImageDescriptor, Image> imageMap;

		public String getText(Object obj)
		{
			return ((TreeModelItem)obj).getName();
		}
		
		public Image getColumnImage(Object obj, int index)
		{
			if ( index == 0 ) {
				return getImage(obj);
			}
			return null;
		}
		
	    public Image getImage(Object element)
	    {
	        if ( element instanceof TreeModelItem ) {
	            String path = ((TreeModelItem)element).getIconPath();
	            if ( path != null ) {
	            	ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), path);
	            	return cacheImage(desc);
	            }
	        }
	        return null;
	    }

		Image cacheImage(ImageDescriptor desc)
		{
			if ( imageMap == null ) {
				imageMap = new HashMap<ImageDescriptor, Image>();
			}
	        Image image = (Image) imageMap.get(desc);
	        if ( image == null ) {
	            image = desc.createImage();
	            imageMap.put(desc, image);
	        }
	        return image;
	    }

	    public void dispose()
	    {
	        if ( imageMap != null ) {
	        	Iterator<Image> images = imageMap.values().iterator();
	            while ( images.hasNext() ) {
	                images.next().dispose();
	            }
	            imageMap = null;
	        }
	        super.dispose();
	    }
	}
	
	private TreeViewer setupTreeViewer(Composite parent)
	{
		TreeViewer viewer = new TreeViewer(parent, SWT.SINGLE);
		//ContainerCheckedTreeViewer viewer = new ContainerCheckedTreeViewer(parent, SWT.NONE);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(rootItem);
        viewer.addDoubleClickListener(new IDoubleClickListener()
        {
            @Override
            public void doubleClick(DoubleClickEvent event)
            {
                TreeModelItem item = (TreeModelItem)((TreeSelection)event.getSelection()).getFirstElement();
                if ( item.getParent() == rootItem ) {
                	
                } else if ( item.getParent().getName().equals("ResultRoot") ) {
                	Iterator<TreeModelItem> it = item.getParent().getParent().getChildren().iterator();
                	while ( it.hasNext() ) {
                		TreeModelItem i = it.next();
                		if ( i instanceof RTSystemItem ) {
                			loadResult(item.getName(), (RTSystemItem)i);
                			resultViewer.setSelection(new StructuredSelection(item));
                			break;
                		}
                	}
                }
            }
        });

		/*viewer.addCheckStateListener(new ICheckStateListener()
		{
			public void checkStateChanged(CheckStateChangedEvent event)
			{
				boolean isSelected = event.getChecked();
				Object selected = event.getElement();
				if ( selected instanceof TreeModelItem ) {
					TreeModelItem item = (TreeModelItem)selected;
					//resultViewer.setSubtreeChecked(item, isSelected);
					List<Object> checkedObjects = Arrays.asList(resultViewer.getCheckedElements());
					while ( item.hasParent() ) {
						item = item.getParent();
						if ( isSelected ) {
							resultViewer.setChecked(item, true);
						} else {
							Iterator<TreeModelItem> children =  item.getChildren().iterator();
							while ( children.hasNext() ) {
								if ( checkedObjects.contains(children.next()) ) {
									isSelected = true;
									break;
								}
							}
							resultViewer.setChecked(item, isSelected);
							break;
						}
					}
					checkedObjects = Arrays.asList(resultViewer.getCheckedElements());
					item.getRoot().setCheckedItems(checkedObjects.toArray(new TreeModelItem[0]));
				}
			}
		});*/
		
		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setLinesVisible(true);
		
		return viewer;
	}
	
	private void importProfile(String fname) 
	{
		if ( fname != null && fname != "" ) {
			try {
				File srcFile = new File(fname);
				RTSystemItem rts = new RTSystemItem(fname);
				rootItem.add(rts);
				
				// create a directory & copy the system profile
				IFolder rtsTopFolder = project.getFolder(rts.getId());
				if ( !rtsTopFolder.exists() ) {
					rtsTopFolder.create(true, true, progress);
				}
				IFile destFile = rtsTopFolder.getFile(srcFile.getName());
				if ( !destFile.isAccessible() ) {
					destFile.create(null, true, progress);
				}
				FileChannel srcChannel  = new FileInputStream(srcFile.getAbsolutePath()).getChannel();
				FileChannel destChannel = new FileOutputStream(destFile.getLocation().toString()).getChannel();
				try {
					srcChannel.transferTo(0, srcChannel.size(), destChannel);
				} finally {
					srcChannel.close();
					destChannel.close();
				}
				
				// create a script directory
				IFolder scriptFolder = rtsTopFolder.getFolder("scripts");
				if ( !scriptFolder.exists() ) {
					scriptFolder.create(true, true, progress);
				}
				
				// create a result directory
				IFolder resultFolder = rtsTopFolder.getFolder("results");
				if ( !resultFolder.exists() ) {
					resultFolder.create(true, true, progress);
				}
				
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			resultViewer.refresh();
		}
	}
	
	private void save()
	{
		Iterator<TreeModelItem> it = rootItem.getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem item = it.next();
			if ( item instanceof RTSystemItem ) {
				RTSystemItem rts = (RTSystemItem)item;
				IFolder folder = project.getFolder(rts.getId());
				IFile   file = folder.getFile(LOG_FILE_NAME);
				try {
					if ( !file.exists() ) {
						file.create(null, false, progress);
					}
					file.setContents(new ByteArrayInputStream(toYaml(rts).getBytes("UTF-8")), true, false, progress);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String toYaml(RTSystemItem rts)
	{
		Map<String, BenchmarkResultItem> resultMap = new HashMap<String, BenchmarkResultItem>();
		Iterator<RTComponentItem> rtcs = rts.getRTCMembers().iterator();
		while (rtcs.hasNext()) {
			RTComponentItem rtc = rtcs.next();
			resultMap.put(rtc.getId(), rtc.getResult());
		}
		String result = Yaml.dump(resultMap);
		return result;
	}
	
    private Map<String, BenchmarkResultItem> fromYaml(IFile file)
    {
    	if ( file == null || !file.exists() || file.isPhantom() ) {
    		return null;
    	}
    	Reader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(file.getContents(), "UTF-8"));
            Object yaml = Yaml.load(reader);
            return (Map<String, BenchmarkResultItem>)yaml;
        } catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} finally {
        	try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        return null;
    }
	
	public class MyViewerDropListener extends org.eclipse.jface.viewers.ViewerDropAdapter
	{
		protected MyViewerDropListener(Viewer viewer)
		{
			super(viewer);
		}

		@Override
		public boolean performDrop(Object data)
		{
			if ( data instanceof String[] ) {
				importProfile(((String[])data)[0]);
			}
			return false;
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType)
		{
			return org.eclipse.swt.dnd.FileTransfer.getInstance().isSupportedType(transferType);
		} 
	} 
}

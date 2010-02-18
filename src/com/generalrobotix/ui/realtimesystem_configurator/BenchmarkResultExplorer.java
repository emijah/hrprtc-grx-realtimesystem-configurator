package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
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
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.ho.yaml.Yaml;

import com.generalrobotix.model.BenchmarkResultItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;

public class BenchmarkResultExplorer extends ViewPart {
	private IProject project;
	private TreeModelItem rootItem = new TreeModelItem();
	private CheckboxTreeViewer resultViewer;
	private NullProgressMonitor progress;
	private FileDialog fdlg;
	private List<Action> actionList = new ArrayList<Action>();

	private static final SimpleDateFormat FORMAT_DATE1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public BenchmarkResultExplorer() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout(1, false));
		
		fdlg = new FileDialog(parent.getShell(), SWT.OPEN);
		
		resultViewer = setupTreeViewer(parent);
		getSite().setSelectionProvider(resultViewer);
		
		actionList.add(new ImportAction());
		actionList.add(new SaveAction());

		MenuManager menuManager = new MenuManager("#PopupMenu");
		menuManager.setRemoveAllWhenShown(true);
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
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
		
		Transfer[] transfers = new Transfer[] { org.eclipse.swt.dnd.FileTransfer.getInstance() };
		resultViewer.addDropSupport(DND.DROP_COPY | DND.DROP_MOVE, transfers, new MyViewerDropListener(resultViewer));
		
		// TODO show dialog to confirm create project
		project = getProject("RealtimeSystemConfiguratorRepository");
		updateList();
	}

	@Override
	public void setFocus() {
	}
	
	private IProject getProject(String projectName) {
		IProject ret = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if ( !ret.exists() ) {
			try {
				ret.create(null);
				ret.open(progress);
			} catch (CoreException e) {
				e.printStackTrace();
			}
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
				if ( systemprofiles.length > 0 ) {
					RTSystemItem rts = new RTSystemItem(systemprofiles[0].getAbsolutePath());
					rootItem.add(rts);
				}
			}
		}
		load();
		resultViewer.refresh();
	}
	
	private class ImportAction extends Action {
		public ImportAction() {
			setText("Import RTSystem Profile");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/folder_open.png"));
		}
		public void run() {
			fdlg.setFilterExtensions( new String[] {"*.xml"} );
			fdlg.open();
			String fname = fdlg.getFilterPath() + java.io.File.separator + fdlg.getFileName();
			if ( fname != null ) {
				importProfile(fname);
			}
		}
	};
	
	private class SaveAction extends Action {
		public SaveAction() {
			setText("Save All Benchmark Results");
			setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/save.png"));
		}
		public void run() {
			save();
		}
	};

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
					}
					return "-";
				case 2:
					if ( item instanceof RTComponentItem ) {
						BenchmarkResultItem result = ((RTComponentItem)item).getResult();
						if ( result.date != null ) {
							return FORMAT_DATE1.format(result.date);
						}
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

	private CheckboxTreeViewer setupTreeViewer(Composite parent) {
		CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.NONE);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(rootItem);
		//viewer.addDragSupport(DND.DROP_COPY , new Transfer[] {GadgetTransfer.getInstance()}, new GadgetDragListener(viewer));
		viewer.addCheckStateListener(new ICheckStateListener(){
			public void checkStateChanged(CheckStateChangedEvent event) {
				TreeModelItem item = (TreeModelItem)event.getElement();
				resultViewer.setSubtreeChecked(item, event.getChecked());
				Object[] objList = resultViewer.getCheckedElements();
				TreeModelItem[] itemList = Arrays.asList(objList).toArray(new TreeModelItem[0]);
				item.getRoot().setCheckedItems(itemList);
			}
		});
		
		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		int idx = 0;
		setHeader(tree, SWT.LEFT, idx++, 250, "RTSystem Name");
		setHeader(tree, SWT.LEFT, idx++,  50, "Ver.");
		setHeader(tree, SWT.LEFT, idx++, 200, "Date");
		
		return viewer;
	}
	
	private void setHeader(Tree tree, int alignment, int index, int width, String text) {
		TreeColumn column = new TreeColumn(tree, alignment, index);
		column.setText(text);
		column.setWidth(width);
	}

	private void importProfile(String fname) {
		if ( fname != null && fname != "" ) {
			try {
				File srcFile = new File(fname);
				RTSystemItem rts = new RTSystemItem(fname);
				rootItem.add(rts);
				IFolder destFolder = project.getFolder(rts.getId());
				if ( !destFolder.exists() ) {
					destFolder.create(true, true, progress);
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
	
	private void save() {
		Iterator<TreeModelItem> it = rootItem.getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem item = it.next();
			if ( item instanceof RTSystemItem ) {
				RTSystemItem rts = (RTSystemItem)item;
				String id = rts.getId();
				IFolder folder = project.getFolder(id);
				IFile   file = folder.getFile("result.yaml");
				try {
					file.setContents(new ByteArrayInputStream(toYaml(rts).getBytes("UTF-8")), true, false, progress);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private String toYaml(RTSystemItem rts) {
		Map<String, Map<Object, Object>> resultMap = new HashMap<String, Map<Object, Object>>();
		Iterator<RTComponentItem> rtcs = rts.getRTCMembers().iterator();
		while (rtcs.hasNext()) {
			RTComponentItem rtc = rtcs.next();
			resultMap.put("'"+rtc.getId()+"'", rtc.getResult().getPropertyMap());
		}
		
        String result = Yaml.dump(resultMap);
        result = result.replace("\r\n", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replace("--- \n", ""); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replace("\"", ""); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replace(" !java.util.LinkedHashMap", ""); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replaceAll("-\n *", "- "); //$NON-NLS-1$ //$NON-NLS-2$
        result = result.replace(": \n", ":\n"); //$NON-NLS-1$ //$NON-NLS-2$
        if (result.length() > 0) {
            result = result.substring(0, result.length() - 1);
        }
        if (!result.endsWith("\n")) {
            result += "\n";
        }

        return result;
    }
	
	private void load() {
		Iterator<TreeModelItem> it = rootItem.getChildren().iterator();
		while ( it.hasNext() ) {
			TreeModelItem item = it.next();
			if ( item instanceof RTSystemItem ) {
				RTSystemItem rts = (RTSystemItem)item;
				String id = rts.getId();
				IFolder folder = project.getFolder(id);
				if ( folder.findMember("result.yaml", false) != null ) {
					IFile file = folder.getFile("result.yaml");
					Map<String, Map<Object, Object>> ret = fromYaml(file);
					if ( ret != null ) {
						Iterator<RTComponentItem> rtcs = rts.getRTCMembers().iterator();
						while(rtcs.hasNext()) {
							RTComponentItem rtc = rtcs.next();
							Map m = ret.get(rtc.getId());
							if ( m != null) {
								rtc.setResult(m);
							}
						}
					}
				}
			}
		}
	}
    
    private Map<String, Map<Object, Object>> fromYaml(IFile file) {
    	if ( !file.exists() || file.isPhantom() ) {
    		return null;
    	}
        BufferedInputStream bufferedIn = null;
        ByteArrayOutputStream byteOut = null;
        try {
            bufferedIn = new BufferedInputStream(file.getContents(true));
            byteOut = new ByteArrayOutputStream();
            int read = 0;
            while ((read = bufferedIn.read()) != -1) {
                byteOut.write(read);
            }
            Object yaml = Yaml.load(byteOut.toString("UTF-8"));
            return (Map<String, Map<Object, Object>>)yaml;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CoreException e) {
            e.printStackTrace();
        } catch (Exception ex) {
        	ex.printStackTrace();
        } finally {
            try {
                if (bufferedIn != null) {
                    bufferedIn.close();
                }
            } catch (IOException ioe) {
            }
            try {
                if (byteOut != null) {
                    byteOut.close();
                }
            } catch (IOException ioe) {
            }
        }
        return null;
    }
    
	public class MyViewerDropListener extends org.eclipse.jface.viewers.ViewerDropAdapter { 
		protected MyViewerDropListener(Viewer viewer) {
			super(viewer);
		}

		@Override
		public boolean performDrop(Object data) {
			if ( data instanceof String[] ) {
				importProfile(((String[])data)[0]);
			}
			return false;
		}

		@Override
		public boolean validateDrop(Object target, int operation, TransferData transferType) {
			return org.eclipse.swt.dnd.FileTransfer.getInstance().isSupportedType(transferType);
		} 
	} 
}

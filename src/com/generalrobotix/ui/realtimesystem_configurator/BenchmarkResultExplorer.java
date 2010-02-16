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
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.ho.yaml.Yaml;

import com.generalrobotix.model.RTCModel;
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
		
		resultViewer = setupTreeViewer(parent);
		getSite().setSelectionProvider(resultViewer);

		Button btnImportSystem = new Button(parent, SWT.NONE);
		btnImportSystem.setText("Import RTSystem Profile");
		btnImportSystem.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				FileDialog fdlg = new FileDialog(((Button)e.getSource()).getShell(), SWT.OPEN);
				fdlg.setFilterExtensions( new String[] {"*.xml"} );
				fdlg.open();
				String fname = fdlg.getFilterPath() + java.io.File.separator + fdlg.getFileName();
				importProfile(fname);
			}
		});
		
		Button btnSaveResult = new Button(parent, SWT.NONE);
		btnSaveResult.setText("Save All Benchmark Results");
		btnSaveResult.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				save();
			}
		});
		
		Button btnLoadResult = new Button(parent, SWT.NONE);
		btnLoadResult.setText("Load Benchmark Results");
		btnLoadResult.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			public void widgetSelected(SelectionEvent e) {
				load();
			}
		});
		
		// TODO show dialog to confirm create project
		project = getProject("RealtimeSystemConfiguratorRepository");
		updateList();
	}

	@Override
	public void setFocus() {
	}
	
	public IProject getProject(String projectName) {
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
				
				RTSystemItem rts = new RTSystemItem(systemprofiles[0].getAbsolutePath());
				rootItem.add(rts);
			}
		}
		resultViewer.refresh();
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

	private CheckboxTreeViewer setupTreeViewer(Composite parent) {
		CheckboxTreeViewer viewer = new CheckboxTreeViewer(parent, SWT.NONE);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setInput(rootItem);
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
		setHeader(tree, SWT.LEFT, 0, 300, "RTSystem Name");
		setHeader(tree, SWT.LEFT, 1, 100, "Version");
		setHeader(tree, SWT.LEFT, 2, 100, "Date");
		
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
		Iterator<RTCModel> rtcs = rts.getRTCMembers().iterator();
		while (rtcs.hasNext()) {
			RTCModel rtc = rtcs.next();
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
				IFile   file = folder.getFile("result.yaml");
				Map<String, Map<Object, Object>> ret = fromYaml(file);
				Iterator<RTCModel> rtcs = rts.getRTCMembers().iterator();
				while(rtcs.hasNext()) {
					RTCModel rtc = rtcs.next();
					Map m = ret.get(rtc.getId());
					if ( m != null) {
						rtc.setResult(m);
					}
				}
			}
		}
	}
    
    private Map<String, Map<Object, Object>> fromYaml(IFile file) {
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
}

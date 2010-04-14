package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.omg.CosNaming.NamingContext;
import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.ExecutionContext;

import OpenRTM.BenchmarkService;
import OpenRTM.BenchmarkServiceHelper;
import OpenRTM.NamedStateLog;
import RTC.RTObject;
import _SDOPackage.InternalError;
import _SDOPackage.InvalidParameter;
import _SDOPackage.NotAvailable;

import com.generalrobotix.model.BenchmarkResultItem;
import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;
import com.generalrobotix.ui.util.GrxRTMUtil;

public class BenchmarkOperatorView extends ViewPart {
	private int robotPort_ = 2809;
	private RTSystemItem currentSystem;
	private TreeViewer rtsViewer;
	private Button chkAutoUpdate;
	private static final DecimalFormat FORMAT_MSEC = new DecimalFormat(" 0.000;-0.000");
	private static final SimpleDateFormat FORMAT_DATE1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public BenchmarkOperatorView()
	{
	}

	@Override
	public void createPartControl(Composite parent)
	{		
		parent.setLayout(new GridLayout(1, false));
		final Text text = new Text(parent, SWT.NONE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rtsViewer = setupTreeViewer(parent);
		
		Composite btnPanel = new Composite(parent, SWT.NONE);
		btnPanel.setLayout(new RowLayout());
		Button btnStartup = new Button(btnPanel, SWT.NONE);
		btnStartup.setText("Startup System");
		btnStartup.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				execPython("startup.py");
			}	
		});
		
		final Button btnReset = new Button(btnPanel, SWT.NONE);
		btnReset.setText("Reset Log");
		btnReset.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				resetLogs();
				rtsViewer.refresh();
			}	
		});
		
		final Button btnUpdate = new Button(btnPanel, SWT.NONE);
		btnUpdate.setText("Update Log");
		btnUpdate.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				updateLog();
				rtsViewer.refresh();
			}	
		});
		
	    chkAutoUpdate = new Button(btnPanel, SWT.CHECK);
	    chkAutoUpdate.setText("auto");
		chkAutoUpdate.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				if ( ((Button)e.getSource()).getSelection() ) {
					btnUpdate.setEnabled(false);
					Display display = Display.getCurrent();
					if ( !display.isDisposed() ) {
						display.timerExec(1000, new UpdateLogThread());
					}
				} else {
					btnUpdate.setEnabled(true);
				}
			}	
		});
		
		// Catch selection event from BenchmarkResultExplorer
		getSite().getPage().addSelectionListener(new ISelectionListener() {
	        public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
	        	if (sourcepart != BenchmarkOperatorView.this && selection instanceof IStructuredSelection) {
	        		List sel = ((IStructuredSelection) selection).toList();
	        		if ( sel.size() > 0 && sel.get(0) instanceof RTSystemItem ) {
	        			currentSystem = ((RTSystemItem)sel.get(0));
	        			rtsViewer.setInput(currentSystem);
	        			text.setText(currentSystem.getName()+":"+currentSystem.getVersion());
	        		}
	            }
	        }
	    });
	}

	@Override
	public void setFocus()
	{
	}
	
	class ViewContentProvider implements ITreeContentProvider
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}
		
		public void dispose() {
		}
		
		public Object[] getElements(Object parent) {
			if ( parent instanceof RTSystemItem ) {
				return ((RTSystemItem)parent).getChildren().toArray();
			}
			return null;
		}
		
		public Object[] getChildren(Object parentElement) {
			return ((TreeModelItem)parentElement).getChildren().toArray();
		}
		
		public Object getParent(Object element) {
			return ((TreeModelItem)element).getParent();
		}
		
		public boolean hasChildren(Object element) {
			return (((TreeModelItem)element).getChildren().size() > 0);
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public String getColumnText(Object obj, int index) {
			try {				
				RTComponentItem model = (RTComponentItem)obj;
				BenchmarkResultItem result = model.getResult();
				Component comp = model.getComponent();
				List<ExecutionContext> eclist = comp.getExecutionContexts();
				switch(index) {
				case 0: return eclist.size() > 0 ? (model.getName() + ":" + eclist.get(0).getId()) : (model.getName());
				case 1: return eclist.size() > 0 ? (String.valueOf((int)(1.0/eclist.get(0).getRate()*1000))) : ("");
				case 2: return FORMAT_MSEC.format(result.max*1000.0);
				case 3: return FORMAT_MSEC.format(result.mean*1000.0);
				case 4: return String.valueOf(result.count);
				case 5: return result.date == null ? "-":FORMAT_DATE1.format(result.date);
				default: break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			return "";
		}
		/*public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}
		public Image getImage(Object obj) {
			return null;//PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}*/
		public Image getColumnImage(Object obj, int index) {
			if ( index == 0 ) {
				return getImage(obj);
			}
			return null;
		}
		
	    public Image getImage(Object element) {
	        if (element instanceof TreeModelItem) {
	            ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), ((TreeModelItem)element).getIconPath());
	            return cacheImage(desc);
	        }
	        return null;
	    }
	    
	    private HashMap<ImageDescriptor, Image> imageMap;
		Image cacheImage(ImageDescriptor desc) {
			if ( imageMap == null ) {
				imageMap = new HashMap<ImageDescriptor, Image>();
			}
	        Image image = (Image) imageMap.get(desc);
	        if (image == null) {
	            image = desc.createImage();
	            imageMap.put(desc, image);
	        }
	        return image;
	    }

	    public void dispose() {
	        if (imageMap != null) {
	        	Iterator<Image> images = imageMap.values().iterator();
	            while (images.hasNext()) {
	                images.next().dispose();
	            }
	            imageMap = null;
	        }
	        super.dispose();
	    }
	}
	
	private TreeViewer setupTreeViewer(Composite parent)
	{
		TreeViewer viewer = new TreeViewer(parent, SWT.NONE);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		int idx = 0;
		setHeader(tree, SWT.LEFT, idx++, 180, "Name");
		setHeader(tree, SWT.LEFT, idx++,  50, "T[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Max.[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Ave.[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Num.");
		setHeader(tree, SWT.LEFT, idx++,  80, "LastUpdate");
		return viewer;
	}

	private void setHeader(Tree tree, int alignment, int index, int width, String text)
	{
		TreeColumn column = new TreeColumn(tree, alignment, index);
		column.setWidth(width);
		column.setText(text);
	}
	
	private void updateLog()
	{
		if ( currentSystem != null ) {
			Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
			while ( it.hasNext() ) {
				RTComponentItem model = it.next();
				if ( model instanceof ExecutionContextItem ) {
					benchmarkTest((ExecutionContextItem)model);
				}
			}
		}
	}
	
	private void benchmarkTest(ExecutionContextItem ecModel)
	{
		try {
			double cycle = 1.0/ecModel.getRate();
			NamingContext rnc = GrxRTMUtil.getRootNamingContext(ecModel.getHostName(), robotPort_);
			
			RTObject rtc = GrxRTMUtil.findRTC(ecModel.getOwnerName(), rnc);
			BenchmarkService bmSVC = BenchmarkServiceHelper.narrow(rtc.get_sdo_service("BenchmarkService_EC0"));
			NamedStateLog[] logs = bmSVC.get_logs();

			Iterator<TreeModelItem> it = ecModel.getChildren().iterator();
			while ( it.hasNext() ) {
				RTComponentItem model = (RTComponentItem)it.next();
				model.getResult().setCycle(cycle);
				model.getResult().updatePlatformInfo(bmSVC.get_platform_info());
				for (int i=0; i<logs.length; i++) {
					if ( logs[i].id.equals(model.getName()) ) {
						model.getResult().updateLog(logs[i]);
						break;
					}
				}

				TreeModelItem item = model.getParent();
				if ( item instanceof RTComponentItem ) {
					((RTComponentItem)item).calcSummation();
				}
			}
		} catch (InvalidParameter e) {
			e.printStackTrace();
		} catch (NotAvailable e) {
			e.printStackTrace();
		} catch (InternalError e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("EC:" + ecModel.getName() + " is not available.");
		}
	}
	
	private void resetLogs() 
	{
		if ( currentSystem != null ) {
			Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
			while ( it.hasNext() ) {
				RTComponentItem model = it.next();
				BenchmarkResultItem result = model.getResult();
				if ( result != null) {
					result.reset();
				}
			}
		}
	}
	
	private void execPython(String fname)
	{
		//IProject proj = getProject("RealtimeSystemConfigurator");
		try {
			Process p = Runtime.getRuntime().exec("python " + fname);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			//BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			PrintStream ps = new PrintStream(p.getOutputStream());
			do {
				System.out.println(stdout.readLine());
			} while ( stdout.ready() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public IProject getProject(String projectName)
	{
		IProject ret = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if ( !ret.exists() ) {
			try {
				ret.create(null);
				ret.open(null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	

	public class UpdateLogThread implements Runnable
	{
		public void run() {
			try {
				updateLog();
				rtsViewer.refresh();
				Display display = Display.getCurrent();
				if ( !display.isDisposed() && chkAutoUpdate.getSelection() ) {
					display.timerExec(1000, this);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
}

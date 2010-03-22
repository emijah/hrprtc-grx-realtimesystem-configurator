package com.generalrobotix.ui.realtimesystem_configurator;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.omg.CosNaming.NamingContext;
import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.ExecutionContext;

import OpenRTM.BenchmarkService;
import OpenRTM.BenchmarkServiceHelper;
import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import OpenRTM.PlatformInfoHolder;
import RTC.RTObject;

import _SDOPackage.InternalError;
import _SDOPackage.InvalidParameter;
import _SDOPackage.NotAvailable;

import com.generalrobotix.model.BenchmarkResultItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;
import com.generalrobotix.ui.util.GrxRTMUtil;

public class BenchmarkOperatorView extends ViewPart {
	private String robotHost_ = "localhost";
	private int robotPort_ = 2809;
	private RTSystemItem currentSystem;
	private TreeViewer rtsViewer;
	private static final DecimalFormat FORMAT_MSEC = new DecimalFormat(" 0.000;-0.000");

	public BenchmarkOperatorView() {
	}

	@Override
	public void createPartControl(Composite parent) {		
		parent.setLayout(new GridLayout(1, false));
		final Text text = new Text(parent, SWT.NONE);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		rtsViewer = setupTreeViewer(parent);
		
		Button btn = new Button(parent, SWT.NONE);
		btn.setText("BenchmarkTest");
		btn.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				try{
					if ( currentSystem != null ) {
						Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
						while ( it.hasNext() ) {
							RTComponentItem model = it.next();
							String compositeType = model.getComponent().getCompositeType();
							if ( !compositeType.equals("PeriodicECShared") && !compositeType.equals("PeriodicStateShared") ) {
								benchmarkTest(model);
							}
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
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
	public void setFocus() {
	}
	
	class ViewContentProvider implements ITreeContentProvider {

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
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			try {				
				RTComponentItem model = (RTComponentItem)obj;
				BenchmarkResultItem result = model.getResult();
				Component comp = model.getComponent();
				//if ( comp.getCompositeType().equals("PeriodicECShared") || comp.getCompositeType().equals("PeriodicStateShared") )
				List<ExecutionContext> eclist = comp.getExecutionContexts();
				switch(index) {
				case 0: return "EC1";
				case 1: return eclist.size() > 0 ? (model.getName() + ":" + eclist.get(0).getId()) : (model.getName());
				case 2: return eclist.size() > 0 ? (String.valueOf((int)(1.0/eclist.get(0).getRate()*1000))) : ("");
				case 3: return robotHost_;
				case 4: return String.valueOf(robotPort_);
				case 5: return FORMAT_MSEC.format(result.max*1000.0);
				case 6: return FORMAT_MSEC.format(result.min*1000.0);
				case 7: return FORMAT_MSEC.format(result.mean*1000.0);
				default: break;
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
	
	private TreeViewer setupTreeViewer(Composite parent) {
		TreeViewer viewer = new TreeViewer(parent, SWT.NONE);
		viewer.setContentProvider(new ViewContentProvider());
		viewer.setLabelProvider(new ViewLabelProvider());
		Tree tree = viewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		int idx = 0;
		setHeader(tree, SWT.LEFT, idx++, 100, "ExecutionContext");
		setHeader(tree, SWT.LEFT, idx++, 100, "Component Name");
		setHeader(tree, SWT.LEFT, idx++,  80, "Cycle[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Hostname");
		setHeader(tree, SWT.LEFT, idx++,  80, "Port");
		setHeader(tree, SWT.LEFT, idx++,  80, "Max.[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Min.[ms]");
		setHeader(tree, SWT.LEFT, idx++,  80, "Ave.[ms]");
		return viewer;
	}

	private void setHeader(Tree tree, int alignment, int index, int width, String text) {
		TreeColumn column = new TreeColumn(tree, alignment, index);
		column.setWidth(width);
		column.setText(text);
	}
	
	private void benchmarkTest(RTComponentItem model) {
		String hostName = model.getHostName();
		String rtcName = model.getName();
		
		NamingContext rnc = GrxRTMUtil.getRootNamingContext(hostName, robotPort_);
		RTObject rtc = GrxRTMUtil.findRTC(rtcName, rnc);
		if ( rtc == null ) {
			return;
		}
		BenchmarkService benchmark_svc = null;
		try {
			benchmark_svc = BenchmarkServiceHelper.narrow(rtc.get_sdo_service("BenchmarkService_EC0"));
		} catch (InvalidParameter e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotAvailable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InternalError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		PlatformInfo pinfo = benchmark_svc.get_platform_info();
		
		rtc.get_owned_contexts()[0].start();
		
		NamedStateLog[] namedLogs = benchmark_svc.get_logs();
		
		//rtc.get_owned_contexts()[0].stop();
		
		model.setResult(new BenchmarkResultItem(namedLogs[0], pinfo));
		
		rtsViewer.refresh();
	}
	
	private void execPython(String fname) {
		IProject proj = getProject("RealtimeSystemConfigurator");
	}
	
	public IProject getProject(String projectName) {
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
}

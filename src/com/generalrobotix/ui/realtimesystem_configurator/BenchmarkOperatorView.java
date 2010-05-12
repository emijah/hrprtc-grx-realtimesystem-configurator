package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.omg.CosNaming.NamingContext;
import org.openrtp.namespaces.rts.version02.DataportConnector;
import org.openrtp.namespaces.rts.version02.TargetPort;

import OpenRTM.BenchmarkService;
import OpenRTM.BenchmarkServiceHelper;
import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import RTC.ConnectorProfile;
import RTC.ConnectorProfileHolder;
import RTC.PortService;
import RTC.RTObject;
import RTM.Manager;

import com.generalrobotix.model.BenchmarkResultItem;
import com.generalrobotix.model.ExecutionContextItem;
import com.generalrobotix.model.RTComponentItem;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;
import com.generalrobotix.ui.util.GrxRTMUtil;

public class BenchmarkOperatorView extends ViewPart {
	private RTSystemItem currentSystem;
private Action actTest;	
	private TreeViewer rtsViewer;
	private Button btnUpdate;
	private Button btnSave;
	private Combo cmbInterval_;
	private Combo cmbRobotHost_;
	
    private static Color white_;
    private static Color black_;
    private static Color red_;
    private static Color yellow_;
    
    private static final int DEFAULT_LOGGING_INTERVAL = 1000;
	private static final DecimalFormat FORMAT_MSEC = new DecimalFormat(" 0.000;-0.000");
	private static final SimpleDateFormat FORMAT_DATE1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private static final SimpleDateFormat FORMAT_DATE2 = new SimpleDateFormat("yyyyMMddHHmmss");
	
	private int robotPort_ = 2809;
	private int managerPort_ = 2810;
	private int loggingInterval_ = DEFAULT_LOGGING_INTERVAL;
	
	boolean isTest = false;	
    
	public BenchmarkOperatorView()
	{
		white_ = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);
		black_ = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
		red_ = Display.getDefault().getSystemColor(SWT.COLOR_RED);
		yellow_ = Display.getDefault().getSystemColor(SWT.COLOR_DARK_MAGENTA);//.COLOR_YELLOW);
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

			public void widgetSelected(SelectionEvent e)
			{
				setupRTSystem();
				/*String pythonPath = "/home/kawasumi/project/hrpsysRTM/src/hrpsys3/grx/REFHW/scripts";
				String moduleName = "test";
				ArrayList<String> args = new ArrayList<String>();
				args.add(cmbRobotHost_.getText());
				execPython(pythonPath, moduleName, args);*/
				checkState();
				rtsViewer.refresh();
			}	
		});
		
		final Button btnReset = new Button(btnPanel, SWT.NONE);
		btnReset.setText("Reset Log");
		btnReset.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e)
			{
				resetLogAction();
				rtsViewer.refresh();
			}	
		});
		
		btnUpdate = new Button(btnPanel, SWT.TOGGLE);
		btnUpdate.setText("Update Log");
		btnUpdate.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}
			
			public void widgetSelected(SelectionEvent e)
			{
				if ( ((Button)e.getSource()).getSelection() ) {
					//resetLastLogs();
					resetLogAction();
					Display display = Display.getCurrent();
					if ( !display.isDisposed() ) {
						display.timerExec(100, new UpdateLogThread());
					}
				}
			}	
		});
		
		Label lblInterval = new Label(btnPanel, SWT.NONE);
		lblInterval.setText("Interval:");
		
		cmbInterval_ = new Combo(btnPanel, SWT.NONE | SWT.READ_ONLY);
		for (int i=0; i<20; i++) {
			cmbInterval_.add(Integer.toString(100 + i*100)+" [ms]");
		}
		cmbInterval_.select((DEFAULT_LOGGING_INTERVAL-100)/100);
		cmbInterval_.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				loggingInterval_ = Integer.parseInt(cmbInterval_.getText().split(" ")[0]);
			}
		});
		
		Label lblRobotHost = new Label(btnPanel, SWT.NONE);
		lblRobotHost.setText("Host:");
		cmbRobotHost_ = new Combo(btnPanel, SWT.NONE);
		cmbRobotHost_.add("localhost");
		cmbRobotHost_.select(0);
		
		btnSave = new Button(btnPanel, SWT.NONE);
		btnSave.setText("Save");
		btnSave.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				save();
			}
			
		});

		// Catch selection event from BenchmarkResultExplorer
		getSite().getPage().addSelectionListener(new ISelectionListener() {
	        public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) 
	        {
	        	if (sourcepart != BenchmarkOperatorView.this && selection instanceof IStructuredSelection) {
	        		List sel = ((IStructuredSelection) selection).toList();
	        		if ( sel.size() > 0 ) {
	        			TreeModelItem item = (TreeModelItem) sel.get(0);
	        			List<TreeModelItem> children = item.getChildren();
	        			if ( children.size() > 0 && children.get(0) instanceof RTSystemItem ) {
	        				currentSystem = (RTSystemItem)children.get(0);
	        				rtsViewer.setInput(currentSystem);
	        				text.setText(currentSystem.getId());
	        				rtsViewer.expandAll();
	        			} else {
	        				Iterator<TreeModelItem> it = item.getParent().getParent().getChildren().iterator();
	        				while( it.hasNext() ) {
	        					TreeModelItem m = it.next();
	        					if ( m instanceof RTSystemItem ) {
	        						currentSystem = (RTSystemItem)m;
	        						rtsViewer.setInput(currentSystem);
	        						text.setText(currentSystem.getId());
	        						rtsViewer.expandAll();
	        						break;
	        					}
	        				}
	        			}
	        		}
	            }
	        }
	    });
		
		actTest = new Action("test", Action.AS_PUSH_BUTTON)
		{
			public void run() 
			{
				//isTest = !isTest;
				//System.out.println(isTest);
				checkState();
				rtsViewer.refresh();
			}
		};
		getViewSite().getActionBars().getToolBarManager().add(actTest);
	}

	@Override
	public void setFocus()
	{
	}
	
	class ViewContentProvider implements ITreeContentProvider
	{
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}
		public void dispose() {}
		
		public Object[] getElements(Object parent)
		{
			if ( parent instanceof RTSystemItem ) {
				return ((RTSystemItem)parent).getChildren().toArray();
			}
			return null;
		}
		
		public Object[] getChildren(Object parentElement)
		{
			return ((TreeModelItem)parentElement).getChildren().toArray();
		}
		
		public Object getParent(Object element)
		{
			return ((TreeModelItem)element).getParent();
		}
		
		public boolean hasChildren(Object element)
		{
			return (((TreeModelItem)element).getChildren().size() > 0);
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider, ITableColorProvider
	{
		public String getColumnText(Object obj, int index)
		{
			try {				
				if ( !(obj instanceof RTComponentItem) ) {
					return null;
				}
				RTComponentItem model = (RTComponentItem)obj;
				BenchmarkResultItem result = model.getResult();
				switch(index) {
				case 0: return model.getName();
				case 1: return FORMAT_MSEC.format(result.cycle*1000.0);
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

		@Override
		public Color getForeground(Object element, int columnIndex)
		{
			if ( columnIndex == 2 ) {
				RTComponentItem model = (RTComponentItem)element;
				BenchmarkResultItem result = model.getResult();
				double val = result.max;
				if ( result.cycle < val ) {
					return red_;
				} else if ( result.cycle < val * 2 ){
					return yellow_;
				}
			} else  if ( columnIndex == 3 ) {
				RTComponentItem model = (RTComponentItem)element;
				BenchmarkResultItem result = model.getResult();
				double val = result.mean;
				if ( result.cycle < val ) {
					return red_;
				} else if ( result.cycle < val * 2 ){
					return yellow_;
				}
			}
			return black_;
		}
		
		@Override
		public Color getBackground(Object element, int columnIndex)
		{
			return null;
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
	        if (element instanceof TreeModelItem ) {
	        	String path = ((TreeModelItem)element).getIconPath();
	        	if ( path != null ) {
	        		ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), path);
	            	return cacheImage(desc);
	        	}
	        }
	        return null;
	    }
	    
	    private HashMap<ImageDescriptor, Image> imageMap;
		Image cacheImage(ImageDescriptor desc) 
		{
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

	    public void dispose()
	    {
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
	
	private void setupRTSystem()
	{
		if ( currentSystem != null ) {
			try {
				System.out.println("creating components");
				String hostName = cmbRobotHost_.getText();
				NamingContext rnc = GrxRTMUtil.getRootNamingContext(hostName, robotPort_);
				Manager mgr = GrxRTMUtil.findRTCmanager(hostName, managerPort_);
				if ( mgr == null ) {
					System.out.println("cant find rtc manager on "+hostName + "("+ robotPort_ + ")");
				}
				
				// Create Components
				Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
				while ( it.hasNext() ) {
					RTComponentItem model = it.next();
					if ( GrxRTMUtil.findRTC(model.getName(), rnc) == null ) {
						if ( createComp(model, mgr) != null ) {
							System.out.print(" created:");
							model.setState(RTComponentItem.RTC_SLEEP);
						} else {
							System.out.print(" failed:");
							model.setState(RTComponentItem.RTC_NOT_EXIST);
						}
					} else {
						model.setState(RTComponentItem.RTC_SLEEP);
						System.out.print("exist:");
					}
					System.out.println(model.getName() + " on " + hostName + " ...");
				}
				System.out.println("\nconnecting components");
				
				// Connect Data ports
				Iterator<DataportConnector> connectors = currentSystem.getDataPortConnectors().iterator();
				while ( connectors.hasNext() ) {
					DataportConnector con = connectors.next();
					TargetPort src = con.getSourceDataPort();
					TargetPort dst = con.getTargetDataPort();
					RTObject srcObj = GrxRTMUtil.findRTC(src.getInstanceName(), rnc);
					RTObject dstObj = GrxRTMUtil.findRTC(dst.getInstanceName(), rnc);
					PortService srcPort = GrxRTMUtil.findPort(srcObj, src.getPortName());
					PortService dstPort = GrxRTMUtil.findPort(dstObj, dst.getPortName());
					ConnectorProfileHolder con_prof = new ConnectorProfileHolder();
					con_prof.value = new ConnectorProfile();
					con_prof.value.name = "connector0";
					con_prof.value.connector_id = "";
					con_prof.value.ports = new PortService[]{srcPort, dstPort};
					con_prof.value.properties = new _SDOPackage.NameValue[]{
							GrxRTMUtil.createNameValue("dataport.interface_type", "corba_cdr"),
							GrxRTMUtil.createNameValue("dataport.dataflow_type","Push"),
							GrxRTMUtil.createNameValue("dataport.subscription_type","flush")};
					srcPort.connect(con_prof);
					System.out.println("connect: " + src.getPortName() + " to " + dst.getPortName());
				}
				
				System.out.println("\nactivating components");
				// Serialize and Activate Components
				Iterator<ExecutionContextItem> ecs = currentSystem.getExecutionContexts().iterator();
				while ( ecs.hasNext() ) {
					ExecutionContextItem ec = ecs.next();
					List<RTObject> rtcs = new ArrayList<RTObject>();
					Iterator<TreeModelItem> children = ec.getChildren().iterator();
					while ( children.hasNext() ) {
						TreeModelItem item = children.next();
						if ( item instanceof RTComponentItem ) {
							RTObject r = GrxRTMUtil.findRTC(((RTComponentItem)item).getComponent().getInstanceName(), rnc);
							if ( r != null ) {
								rtcs.add(r);
							} 
						}
					}
					GrxRTMUtil.serializeComponents(rtcs);
					ec.setState(RTComponentItem.RTC_SLEEP);
					GrxRTMUtil.activateComponents(rtcs);
					ec.setState(RTComponentItem.RTC_ACTIVE);
				}
				
				System.out.println("\nInitialized successfully");
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private RTObject createComp(RTComponentItem model, Manager mgr)
	{
		RTObject ret = null;
		try {
			String category = model.getId().split(":")[0];
			if ( category.equals("RTC") ) {
				model.getName();
				String[] s = model.getId().split(":")[1].split("[.]");
				String loadable = s[s.length-1];
				ret = mgr.create_component(loadable);
			}
		} catch (Exception e) {
			e.printStackTrace();
			GrxRTMUtil.releaseObject(mgr);
		}
		
		return ret;
	}
	
	private void resetLogAction() 
	{
		if ( currentSystem != null ) {
			Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
			while ( it.hasNext() ) {
				it.next().getResult().reset();
			}
		}
	}
	
	private void resetLastLogs() 
	{
		if ( currentSystem != null ) {
			Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
			while ( it.hasNext() ) {
				it.next().getResult().resetLastLog();
			}
		}
	}

	private void checkState()
	{
		if ( currentSystem != null ) {
			NamingContext rnc = GrxRTMUtil.getRootNamingContext(cmbRobotHost_.getText(), robotPort_);
			Iterator<ExecutionContextItem> ecs = currentSystem.getExecutionContexts().iterator();
			//RTM.Manager mgr = GrxRTMUtil.findRTCmanager(cmbRobotHost_.getText(), managerPort_);
			// TODO check manager and monitor
			while ( ecs.hasNext() ) {
				ExecutionContextItem ec = ecs.next();
				RTObject ecOwner = null;
				BenchmarkService bmSVC =  null;
				try {
					ec.setState(RTComponentItem.RTC_NOT_EXIST);
					ecOwner = GrxRTMUtil.findRTC(ec.getOwnerName(), rnc);
					if ( ecOwner != null ) {
						ec.setState(RTComponentItem.RTC_SLEEP);
						bmSVC = BenchmarkServiceHelper.narrow(ecOwner.get_sdo_service("BenchmarkService_EC0"));
						if ( bmSVC != null ) {
							ec.setState(RTComponentItem.RTC_BENCHMARK_AVAILABLE);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if ( ecOwner != null) {
						ecOwner._release();
					}
					if ( bmSVC != null ) {
						bmSVC._release();
					}
				}
				Iterator<TreeModelItem> items = ec.getChildren().iterator();
				while ( items.hasNext() ) {
					RTComponentItem rtcItem = (RTComponentItem)items.next();
					rtcItem.setState(RTComponentItem.RTC_NOT_EXIST);
					RTObject rtc = GrxRTMUtil.findRTC(rtcItem.getName(), rnc);
					if ( rtc != null ) {
						rtcItem.setState(RTComponentItem.RTC_SLEEP);
						if ( rtc.is_alive(ecOwner.get_owned_contexts()[0]) ) {
							if ( bmSVC == null ) {
								rtcItem.setState(RTComponentItem.RTC_ACTIVE);
							} else {
								rtcItem.setState(RTComponentItem.RTC_BENCHMARK_AVAILABLE);
							}
						}
					}
				}
			}
		}
	}
	
	public class UpdateLogThread implements Runnable
	{
		public void run() {
			try {
				updateLogAction();
				Display display = Display.getCurrent();
				if ( !display.isDisposed() && btnUpdate.getSelection() ) {
					display.timerExec(loggingInterval_, this);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}
	
	private boolean updateLogAction()
	{
		boolean ret = false;
		if ( currentSystem != null ) {
			NamingContext rnc = GrxRTMUtil.getRootNamingContext(cmbRobotHost_.getText(), robotPort_);
			List<NamedStateLog[]> l = new ArrayList<NamedStateLog[]>();
			List<ExecutionContextItem> eclist = currentSystem.getExecutionContexts();
			for (int i=0; i<eclist.size(); i++) {
				l.add(getLogs(eclist.get(i), rnc));
			}
			for (int i=0; i<eclist.size(); i++) {
				setLogs(eclist.get(i), rnc, l.get(i));
			}
			GrxRTMUtil.releaseObject(rnc);
			rtsViewer.refresh();
			TimingChartView.getInstance().updateCharts();
			checkState();
		}
		return ret;
	}
	
	private NamedStateLog[] getLogs(ExecutionContextItem ecModel, NamingContext rnc)
	{
		RTObject rtc = null;
		BenchmarkService bmSVC = null;
		try {
			rtc = GrxRTMUtil.findRTC(ecModel.getOwnerName(), rnc);
			if ( rtc != null ) {
				bmSVC =  BenchmarkServiceHelper.narrow(rtc.get_sdo_service("BenchmarkService_EC0"));
				if ( bmSVC != null ) {
					return bmSVC.get_logs();
				}	
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GrxRTMUtil.releaseObject(rtc);
			GrxRTMUtil.releaseObject(bmSVC);
		}
		return null;
	}
	
	private boolean setLogs(ExecutionContextItem ecModel, NamingContext rnc, NamedStateLog[] logs)
	{
		RTObject rtc = null;
		BenchmarkService bmSVC = null;
		try {
			// get information from target system
			double cycle = 1.0/ecModel.getRate();
			rtc = GrxRTMUtil.findRTC(ecModel.getOwnerName(), rnc);
			PlatformInfo pInfo = null;
			if ( logs != null ) {
				bmSVC = BenchmarkServiceHelper.narrow(rtc.get_sdo_service("BenchmarkService_EC0"));
				pInfo = bmSVC.get_platform_info();
			}
			// update logs
			Iterator<TreeModelItem> it = ecModel.getChildren().iterator();
			while ( it.hasNext() ) {
				RTComponentItem model = (RTComponentItem)it.next();
				BenchmarkResultItem result = model.getResult();
				if ( logs == null ) {
					result.resetLastLog();
				} else {
					result.setCycle(cycle);
					result.updatePlatformInfo(pInfo);
					for (int i=0; i<logs.length; i++) {
						if ( logs[i].id.equals(model.getName()) ) {
							result.updateLog(logs[i]);
							break;
						}
					}
				}
			}
			ecModel.calcSummation();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GrxRTMUtil.releaseObject(rtc);
			GrxRTMUtil.releaseObject(bmSVC);
		}
		return false;
	}
	
	private void execPython(String pythonPath, String moduleName, List<String> args)
	{
		try {
			args.add(0, moduleName);
			args.add(0, "-m");
			args.add(0, "python");
			String[] props = new String[]{"PYTHONPATH=" + pythonPath};
			Process p = Runtime.getRuntime().exec(args.toArray(new String[0]), props);
			BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			//PrintStream ps = new PrintStream(p.getOutputStream());
			while ( stdout.ready() ) {
				System.out.println(stdout.readLine());
			}
			while ( stderr.ready() ) {
				System.out.println(stdout.readLine());
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private void save()
	{
		IFolder folder = BenchmarkResultExplorer.getInstance().getProject().getFolder(currentSystem.getId()+"/results");
		IFile   file = folder.getFile(FORMAT_DATE2.format(new Date())+".yaml");
		try {
			if ( !file.exists() ) {
				file.create(null, false, null);
			}
			file.setContents(new ByteArrayInputStream(currentSystem.toYaml().getBytes("UTF-8")), true, false, null);
			BenchmarkResultExplorer.getInstance().updateList();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

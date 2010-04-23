package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
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
import org.openrtp.namespaces.rts.version02.Component;
import org.openrtp.namespaces.rts.version02.DataportConnector;
import org.openrtp.namespaces.rts.version02.ExecutionContext;
import org.openrtp.namespaces.rts.version02.TargetPort;

import OpenRTM.BenchmarkService;
import OpenRTM.BenchmarkServiceHelper;
import OpenRTM.NamedStateLog;
import OpenRTM.PlatformInfo;
import RTC.RTObject;
import RTM.Manager;
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
	private RTSystemItem currentSystem;
	
	private TreeViewer rtsViewer;
	private Button btnUpdate;
	private Combo cmbInterval_;
	private Combo cmbRobotHost_;
	
	private int robotPort_ = 2809;
	private int loggingInterval_ = DEFAULT_LOGGING_INTERVAL;
	
    private static Color white_;
    private static Color black_;
    private static Color red_;
    private static Color yellow_;
    
    private static final int DEFAULT_LOGGING_INTERVAL = 1000;
	private static final DecimalFormat FORMAT_MSEC = new DecimalFormat(" 0.000;-0.000");
	private static final SimpleDateFormat FORMAT_DATE1 = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    
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
				//setupRTSystem();
				execPython(null);
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
					resetLastLogs();
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
		for (int i=0; i<10; i++) {
			cmbInterval_.add(Integer.toString(500 + i*100)+" [ms]");
			cmbInterval_.select((DEFAULT_LOGGING_INTERVAL-500)/100);
		}
		cmbInterval_.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}

			@Override
			public void widgetSelected(SelectionEvent e) {
				loggingInterval_ = Integer.parseInt(cmbInterval_.getText().split(" ")[0]);
			}
		});
		
		Label lblRobotHost = new Label(btnPanel, SWT.NONE);
		lblRobotHost.setText("Host:");
		cmbRobotHost_ = new Combo(btnPanel, SWT.NONE);
		cmbRobotHost_.add("localhost");
		cmbRobotHost_.select(0);

		// Catch selection event from BenchmarkResultExplorer
		getSite().getPage().addSelectionListener(new ISelectionListener() {
	        public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) 
	        {
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
				RTComponentItem model = (RTComponentItem)obj;
				BenchmarkResultItem result = model.getResult();
				Component comp = model.getComponent();
				List<ExecutionContext> eclist = comp.getExecutionContexts();
				switch(index) {
				case 0: return eclist.size() > 0 ? (model.getName() + ":" + eclist.get(0).getId()) : (model.getName());
				case 1: return FORMAT_MSEC.format(result.cycle*1000.0);//eclist.size() > 0 ? (String.valueOf((int)(1.0/eclist.get(0).getRate()*1000))) : ("");
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
	        if (element instanceof TreeModelItem) {
	            ImageDescriptor desc = AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), ((TreeModelItem)element).getIconPath());
	            return cacheImage(desc);
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
			String hostname = null;
			try {
				hostname = InetAddress.getLocalHost().getHostName(); // = txtRobotHost_.getText();
				NamingContext rnc = GrxRTMUtil.getRootNamingContext(hostname, robotPort_);
				Manager mgr = GrxRTMUtil.findRTCmanager(hostname, rnc);
				// Create Components
				Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
				while ( it.hasNext() ) {
					RTComponentItem model = it.next();
					if ( GrxRTMUtil.findRTC(model.getName(), rnc) == null ) {
						createComp(model, mgr);
					}
				}
				
				// Connect Dataports
				Iterator<DataportConnector> connectors = currentSystem.getDataPortConnectors().iterator();
				while ( connectors.hasNext() ) {
					DataportConnector con = connectors.next();
					TargetPort src = con.getSourceDataPort();
					TargetPort dst = con.getTargetDataPort();
					RTObject srcObj = GrxRTMUtil.findRTC(src.getInstanceName(), rnc);
					RTObject dstObj = GrxRTMUtil.findRTC(dst.getInstanceName(), rnc);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void createComp(RTComponentItem model, Manager mgr)
	{
		try {
			String category = model.getId().split(":")[0];
			if ( category.equals("RTC") ) {
				model.getName();
				String[] s = model.getId().split(":")[1].split("[.]");
				String loadable = s[s.length-1];
				mgr.create_component(loadable);
			}
		} catch (Exception e) {
			e.printStackTrace();
			GrxRTMUtil.releaseObject(mgr);
		}
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
			Iterator<RTComponentItem> it = currentSystem.getRTCMembers().iterator();
			String hostName = cmbRobotHost_.getText();
			while ( it.hasNext() ) {
				RTComponentItem model = it.next();
				if ( model instanceof ExecutionContextItem ) {
					ret |= updateLog((ExecutionContextItem)model, hostName);
				}
			}
			rtsViewer.refresh();
			TimingChartView.getInstance().updateCharts();
		}
		return ret;
	}
	
	private boolean updateLog(ExecutionContextItem ecModel, String hostName)
	{
		NamingContext rnc = null;
		RTObject rtc = null;
		BenchmarkService bmSVC = null;
		try {
			// get information from target system
			double cycle = 1.0/ecModel.getRate();
			rnc = GrxRTMUtil.getRootNamingContext(hostName, robotPort_);
			rtc = GrxRTMUtil.findRTC(ecModel.getOwnerName(), rnc);
			bmSVC = BenchmarkServiceHelper.narrow(rtc.get_sdo_service("BenchmarkService_EC0"));
			NamedStateLog[] logs = bmSVC.get_logs();
			PlatformInfo pInfo = bmSVC.get_platform_info();
			
			// update logs
			Iterator<TreeModelItem> it = ecModel.getChildren().iterator();
			while ( it.hasNext() ) {
				RTComponentItem model = (RTComponentItem)it.next();
				model.getResult().setCycle(cycle);
				model.getResult().updatePlatformInfo(pInfo);
				for (int i=0; i<logs.length; i++) {
					if ( logs[i].id.equals(model.getName()) ) {
						model.getResult().updateLog(logs[i]);
						break;
					}
				}
			}
			ecModel.calcSummation();
			return true;
		} catch (InvalidParameter e) {
			e.printStackTrace();
		} catch (NotAvailable e) {
			e.printStackTrace();
		} catch (InternalError e) {
			e.printStackTrace();
		} catch (Exception e) {
			System.out.println("EC:" + ecModel.getName() + " is not available.");
			e.printStackTrace();
		} finally {
			GrxRTMUtil.releaseObject(rnc);
			GrxRTMUtil.releaseObject(rtc);
			GrxRTMUtil.releaseObject(bmSVC);
		}
		return false;
	}
	
	private void execPython(String fname)
	{
		IProject proj = BenchmarkResultExplorer.getInstance().getProject();
		try {
			Process p = Runtime.getRuntime().exec(new String[]{"python", "-m", "test"}, new String[]{"PYTHONPATH=/home/kawasumi/project/hrpsysRTM/src/hrpsys3/grx/REFHW/scripts"});
			BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			PrintStream ps = new PrintStream(p.getOutputStream());
			do {
				System.out.println(stdout.readLine());
			} while ( stdout.ready() );
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}

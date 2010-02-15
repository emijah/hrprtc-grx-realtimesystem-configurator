package com.generalrobotix.ui.realtimesystem_configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.part.ViewPart;
import org.omg.CosNaming.NamingContext;

import OpenHRP.BenchmarkResultHolder;
import OpenHRP.BenchmarkService;
import OpenHRP.BenchmarkServiceHelper;
import OpenHRP.PlatformInfoHolder;
import RTC.RTObject;

import com.generalrobotix.model.BenchmarkResultModel;
import com.generalrobotix.model.RTCModel;
import com.generalrobotix.ui.util.GrxRTMUtil;

public class BenchmarkOperatorView extends ViewPart {
	private ArrayList<SetPropertyPanel> propList_ = new ArrayList<SetPropertyPanel>();
	private String robotHost_ = "localhost";
	private int robotPort_ = 2809;
	private List<RTCModel> currentModels;
	private RTCModel selectedModel;
	private TreeViewer resultViewer;
	private Text text;

	public BenchmarkOperatorView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		
		currentModels = new ArrayList<RTCModel>();
		resultViewer = new TreeViewer(parent, SWT.NONE);
		resultViewer.setContentProvider(new ViewContentProvider());
		resultViewer.setLabelProvider(new ViewLabelProvider());
		resultViewer.setInput(currentModels);
		resultViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				Object[] l = ((IStructuredSelection)event.getSelection()).toArray();
				if ( l.length > 0 && l[0] instanceof RTCModel ) {
					selectedModel = (RTCModel)l[0];
				}
			}
		});
		getSite().setSelectionProvider(resultViewer);
		
		Tree tree = resultViewer.getTree();
		GridData tableLayoutData = new GridData(GridData.FILL_BOTH);
		tree.setLayoutData(tableLayoutData);
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		TreeColumn column = new TreeColumn(tree, SWT.LEFT, 0);
		column.setText("date");
		column.setWidth(100);
		column = new TreeColumn(tree, SWT.LEFT, 1);
		column.setText("time");
		column.setWidth(200);

		GridLayout gridLayout = new GridLayout(1, false);
		GridData gdata = new GridData(GridData.FILL_BOTH);
		
		Group conditionPanel = new Group(parent, SWT.VERTICAL);
		conditionPanel.setText("Benchmark Settings");
		conditionPanel.setLayout(gridLayout);
		
		Group fileLoadGroup = new Group(conditionPanel, SWT.NONE);
		fileLoadGroup.setText("Current RTSystem");
		fileLoadGroup.setLayout(new FillLayout());
		fileLoadGroup.setLayoutData(gdata);
		Button btnLoad = new Button(fileLoadGroup, SWT.NONE);
		btnLoad.setText("Load");
		btnLoad.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {}

			public void widgetSelected(SelectionEvent e) {
				FileDialog fdlg = new FileDialog(((Button)e.getSource()).getShell(), SWT.OPEN);
				fdlg.setFilterExtensions( new String[] {"*.xml"} );
				fdlg.open();
				String fname = fdlg.getFileName();
				if ( fname != null && fname != "" ) {
					fname = fdlg.getFilterPath() + java.io.File.separator + fname;
					currentModels.add(new RTCModel(fname));
					resultViewer.setInput(currentModels);
					text.setText(fname);
					resultViewer.refresh();
					//createProjectDir(fname.replace(".xml", ""));
				}
			}
		});		
		text = new Text(fileLoadGroup, SWT.NONE);

		propList_.add(new SetPropertyPanel(conditionPanel, SWT.NONE, "Robot Host",  "robotHost", true, robotHost_));
		propList_.add(new SetPropertyPanel(conditionPanel, SWT.NONE, "Robot Port",  "robotPort", true, new Integer(robotPort_).toString()));
		
		Button btn = new Button(conditionPanel, SWT.NONE);
		btn.setText("Execute BenchmarkTest");
		btn.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {

			}

			public void widgetSelected(SelectionEvent e) {
				try{
					if ( selectedModel != null) {
						Iterator<RTCModel> it = selectedModel.getRTCMembers().iterator();
						while ( it.hasNext() ) {
							RTCModel model = it.next();
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
	}
	
	private void benchmarkTest(RTCModel model) {
		String hostName = model.getHostName();
		String rtcName = model.getName();
		
		NamingContext rnc = GrxRTMUtil.getRootNamingContext(hostName, robotPort_);
		RTObject rtc = GrxRTMUtil.findRTC(rtcName, rnc);
		if ( rtc == null ) {
			return;
		}
		BenchmarkService benchmark_svc = BenchmarkServiceHelper.narrow(GrxRTMUtil.findService(rtc, "benchmarkService"));
		PlatformInfoHolder platformInfoH = new PlatformInfoHolder();
		benchmark_svc.getPlatformInfo(platformInfoH);
		rtc.get_owned_contexts()[0].start();
		BenchmarkResultHolder resultH = new BenchmarkResultHolder();
		benchmark_svc.measure(resultH);
		rtc.get_owned_contexts()[0].stop();
		System.out.println(resultH.value.max);
		BenchmarkResultModel result = new BenchmarkResultModel();
		result.count = resultH.value.count;
		result.max = resultH.value.max;
		result.mean = resultH.value.mean;
		result.min  = resultH.value.min;
		result.stddev = resultH.value.stddev;
		result.date = new Date();
		model.setResult(result);
		resultViewer.setSelection(resultViewer.getSelection());
	}
	
	private void testAction() {
		try {
			Process p = Runtime.getRuntime().exec("python measure.py");
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
	private class SetPropertyPanel extends Composite {
		private String propName;	
		private boolean isLocal = true;
		private String defaultVal;

		private Label    label;
		private Text  fld;
		private Button     set;
		private Button  resume;


		public SetPropertyPanel(Composite parent, int style, String _title, String _propName, boolean _isLocal, String _defaultVal) {
			super(parent, style);
			GridLayout gridLayout = new GridLayout(4, false);
			gridLayout.marginWidth = 5;
			gridLayout.horizontalSpacing = 5;
			this.setLayout(gridLayout);
			GridData textGridData = new GridData();
			textGridData.widthHint = 80;
			label = new Label(this, SWT.NONE);
			label.setLayoutData(textGridData);
			label.setText(_title);
			fld = new Text(this, SWT.NONE);
			textGridData.widthHint = 100;
			fld.setLayoutData(textGridData);
			set = new Button(this, SWT.NONE);
			set.setText("Set");
			resume = new Button(this, SWT.NONE);
			resume.setText("Resume");
			propName = _propName;
			isLocal = _isLocal;
			defaultVal = _defaultVal;

			fld.addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.keyCode == SWT.CR) {
						//set(); 
					} else {
						/*boolean hasChanged = !fld.getText().equals(getValue());
						set.setEnabled(hasChanged);
						resume.setEnabled(hasChanged);*/
					}
				}
			});

			set.setEnabled(false);
			set.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					//set();
				}
			});

			resume.setEnabled(false);
			resume.addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
				}
				public void widgetSelected(SelectionEvent e) {
					//resume();
				}
			});
		}
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
			if ( parent instanceof List ) {
				return ((List)parent).toArray();
			}
			return null;
		}
		
		public Object[] getChildren(Object parentElement) {
			if ( parentElement instanceof RTCModel ) {
				return ((RTCModel)parentElement).getChildren().toArray();
			}
			return null;
		}
		
		public Object getParent(Object element) {
			return ((RTCModel)element).getParent();
		}
		
		public boolean hasChildren(Object element) {
			return (((RTCModel)element).getChildren().size() > 0);
		}
	}
	
	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			try {				
				RTCModel model = (RTCModel)obj;
				BenchmarkResultModel result = model.getResult();
				switch(index) {
				case 0:
					if ( result.date == null) {
						return "empty";
					}
					return result.date.toString();
				case 1:
					return model.getName();
				case 2:
					return "0.0";
				case 3:
					return "0.0";
				case 4:
					return "0.0";
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
}

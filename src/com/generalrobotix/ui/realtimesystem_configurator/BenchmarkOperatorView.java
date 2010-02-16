package com.generalrobotix.ui.realtimesystem_configurator;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.omg.CosNaming.NamingContext;

import OpenHRP.BenchmarkResultHolder;
import OpenHRP.BenchmarkService;
import OpenHRP.BenchmarkServiceHelper;
import OpenHRP.PlatformInfoHolder;
import RTC.RTObject;

import com.generalrobotix.model.BenchmarkResultModel;
import com.generalrobotix.model.RTCModel;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.TreeModelItem;
import com.generalrobotix.ui.util.GrxRTMUtil;

public class BenchmarkOperatorView extends ViewPart {
	private String robotHost_ = "localhost";
	private int robotPort_ = 2809;
	private RTSystemItem currentSystem;
	private TreeViewer resultViewer;

	public BenchmarkOperatorView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		
		getSite().getPage().addSelectionListener(new ISelectionListener() {
	        public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
	        	if (sourcepart != BenchmarkOperatorView.this && selection instanceof IStructuredSelection) {
	        		List sel = ((IStructuredSelection) selection).toList();
	        		if ( sel.size() > 0 && sel.get(0) instanceof RTSystemItem ) {
	        			currentSystem = ((RTSystemItem)sel.get(0));
	        			resultViewer.setInput(currentSystem);
	        		}
	            }
	        }
	    });
		
		parent.setLayout(new GridLayout(1, false));
		
		resultViewer = new TreeViewer(parent, SWT.NONE);
		resultViewer.setContentProvider(new ViewContentProvider());
		resultViewer.setLabelProvider(new ViewLabelProvider());
		resultViewer.setInput(currentSystem);
		
		Tree tree = resultViewer.getTree();
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);
		setHeader(tree, "date", 100, SWT.LEFT, 0);
		setHeader(tree, "time", 200, SWT.LEFT, 1);
		
		Button btn = new Button(parent, SWT.NONE);
		btn.setText("Execute BenchmarkTest");
		btn.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				try{
					if ( currentSystem != null) {
						Iterator<RTCModel> it = currentSystem.getRTCMembers().iterator();
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
	
	private void setHeader(Tree tree, String text, int width, int alignment, int index) {
		TreeColumn column = new TreeColumn(tree, alignment, index);
		column.setText(text);
		column.setWidth(width);
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

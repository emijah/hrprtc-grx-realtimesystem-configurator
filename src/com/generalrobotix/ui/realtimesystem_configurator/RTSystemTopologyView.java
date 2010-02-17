package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.generalrobotix.model.RTCModel;
import com.generalrobotix.model.RTSystemItem;
import com.generalrobotix.model.RTCModel.RTCConnection;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.swt.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.swt.VisualizationComposite;

public class RTSystemTopologyView extends ViewPart {
	private Graph<RTCModel, RTCConnection> graph;
	private VisualizationComposite<RTCModel, RTCConnection> vcomp;
	private Action actionZoomIn;
	private Action actionZoomOut;
	private Action actionMouseMode;
	
	public RTSystemTopologyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		getSite().getPage().addSelectionListener(new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart sourcepart, ISelection selection) {
				if (sourcepart != RTSystemTopologyView.this	 && selection instanceof IStructuredSelection) {
					List ret = ((IStructuredSelection) selection).toList();
					if (ret.size() > 0 ) {
						if ( ret.get(0) instanceof RTCModel ) {
							updateGraphStructure(((RTCModel)ret.get(0)).getRTSystem());
						} else if ( ret.get(0) instanceof RTSystemItem ) {
							updateGraphStructure((RTSystemItem)ret.get(0));
						}
					}
				}
			}
		});
		parent.setLayout(new GridLayout());
		
		graph = new DirectedOrderedSparseMultigraph<RTCModel, RTCConnection>();
		FRLayout layout = new FRLayout<RTCModel, RTCConnection>(graph);
		layout.setSize(new Dimension(300, 300));
		GraphZoomScrollPane<RTCModel, RTCConnection> graphPanel = new GraphZoomScrollPane<RTCModel, RTCConnection>(parent, SWT.NONE, layout, new Dimension(300, 300));
		graphPanel.setLayoutData(new GridData(GridData.FILL_BOTH|GridData.HORIZONTAL_ALIGN_FILL| GridData.VERTICAL_ALIGN_FILL));

		vcomp = graphPanel.vv;
		vcomp.setBackground(Color.white);
		vcomp.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<RTCModel>());
		vcomp.setVertexToolTipTransformer(new ToStringLabeller<RTCModel>());
		vcomp.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));
		vcomp.getComposite().setLayoutData(new GridData(GridData.FILL_BOTH|GridData.HORIZONTAL_ALIGN_FILL| GridData.VERTICAL_ALIGN_FILL));
		
		final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();
		vcomp.setGraphMouse(graphMouse);
		vcomp.addKeyListener(graphMouse.getModeKeyListener());
		
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		actionMouseMode = new Action("Switch Mouse Mode", Action.AS_CHECK_BOX) {
			public void run() {
				if ( this.isChecked() ) {
					actionMouseMode.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/view_pan_on.png"));
					graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
				} else {
					actionMouseMode.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/view_pan.png"));
					graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
				}
			}
		};
		actionMouseMode.setToolTipText("Switch Mouse Mode");
		actionMouseMode.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/view_pan.png"));
		graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
		toolbarManager.add(actionMouseMode);
		
		final ScalingControl scaler = new CrossoverScalingControl();
		vcomp.scaleToLayout(scaler);
		actionZoomIn = new Action("Zoom In") {
			public void run() {
				scaler.scale(vcomp.getServer(), 1.1f, vcomp.getCenter());
			}
		};
		actionZoomIn.setToolTipText("Zoom In");
		actionZoomIn.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/zoomin.png"));
		toolbarManager.add(actionZoomIn);
		
		actionZoomOut = new Action("Zoom Out") {
			public void run() {
				scaler.scale(vcomp.getServer(), 1/1.1f, vcomp.getCenter());
			}
		};
		actionZoomOut.setToolTipText("Zoom Out");
		actionZoomOut.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(getSite().getPluginId(), "icons/zoomout.png"));
		toolbarManager.add(actionZoomOut);
	}

	private void updateGraphStructure(RTSystemItem model) {
		RTCConnection[] edges = graph.getEdges().toArray(new RTCConnection[0]);
		for (int i=edges.length-1; i>0; i--) {
			graph.removeEdge(edges[i]);
		}
		
		RTCModel[] vertices = graph.getVertices().toArray(new RTCModel[0]);
		for (int i=vertices.length-1; i>0; i--) {
			graph.removeVertex(vertices[i]);
		}
		
		Iterator<RTCModel> members = model.getRTCMembers().iterator();
		while (members.hasNext()) {
			RTCModel m = members.next();
			if ( m.getChildren().size()==0 ){
				graph.addVertex(m);
			}	
		}
		
		Iterator<RTCConnection> rtccons = model.getRTCConnections().iterator();
		while( rtccons.hasNext() ) {
			RTCConnection con = rtccons.next();
			
			boolean isContains = false;
			Iterator<RTCConnection> it = graph.getEdges().iterator();
			while (it.hasNext() ) {
				RTCConnection c = it.next();
				if ( c.equals(con) ) {
					isContains = true;
					break;
				}
			}
			if ( !isContains ) {
				graph.addEdge(con, con.source, con.target);
			}
		}
	}

	@Override
	public void setFocus() {
	}
}

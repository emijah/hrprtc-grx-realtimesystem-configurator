package com.generalrobotix.ui.realtimesystem_configurator;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections15.functors.ConstantTransformer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

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
	private VisualizationComposite<RTCModel, RTCConnection> vv;
	public RTSystemTopologyView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		getSite().getPage().addSelectionListener(new ISelectionListener() {
			public void selectionChanged(IWorkbenchPart sourcepart,
					ISelection selection) {
				if (sourcepart != RTSystemTopologyView.this
						&& selection instanceof IStructuredSelection) {
					List ret = ((IStructuredSelection) selection).toList();
					if (ret.size() > 0 && ret.get(0) instanceof RTCModel) {
						updateStructure(((RTCModel) ret.get(0)).getRTSystem());
					}
				}
			}
		});
		parent.setLayout(new GridLayout());
		
		graph = new DirectedOrderedSparseMultigraph<RTCModel, RTCConnection>();
		//graph = Graphs.<RTCModel, Integer>synchronizedDirectedGraph(new DirectedSparseMultigraph<RTCModel, Integer>());
		
		FRLayout layout = new FRLayout<RTCModel, RTCConnection>(graph);
		layout.setSize(new Dimension(900, 900));

		GraphZoomScrollPane<RTCModel, RTCConnection> graphPanel = new GraphZoomScrollPane<RTCModel, RTCConnection>(parent, SWT.NONE, layout, new Dimension(600, 600));
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		graphPanel.setLayoutData(gridData);

		vv = graphPanel.vv;
		vv.setBackground(Color.white);
		//vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<RTCModel, Integer>());
		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<RTCModel>());

		// add a listener for ToolTips
		vv.setVertexToolTipTransformer(new ToStringLabeller<RTCModel>());
		vv.getRenderContext().setArrowFillPaintTransformer(new ConstantTransformer(Color.lightGray));

		GridData gd = new GridData();
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		vv.getComposite().setLayoutData(gd);

		final DefaultModalGraphMouse graphMouse = new DefaultModalGraphMouse();

		vv.setGraphMouse(graphMouse);
		vv.addKeyListener(graphMouse.getModeKeyListener());

		final ScalingControl scaler = new CrossoverScalingControl();
		vv.scaleToLayout(scaler);

		Group controls = new Group(parent, SWT.NONE);
		GridData gdc = new GridData();
		gdc.horizontalAlignment = GridData.CENTER;
		controls.setLayoutData(gdc);

		GridLayout cl = new GridLayout();
		cl.numColumns = 4;
		controls.setLayout(cl);
		controls.setText("controls");

		Group zoom = new Group(controls, SWT.NONE);
		GridLayout zcl = new GridLayout();
		zcl.numColumns = 2;
		zoom.setLayout(zcl);
		zoom.setText("zoom");

		Button plus = new Button(zoom, SWT.PUSH);
		plus.setText("+");
		plus.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				scaler.scale(vv.getServer(), 1.1f, vv.getCenter());
			}
		});
		Button minus = new Button(zoom, SWT.PUSH);
		minus.setText("-");
		minus.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				scaler.scale(vv.getServer(), 1 / 1.1f, vv.getCenter());
			}
		});

		final Combo combo = new Combo(controls, SWT.READ_ONLY);
		combo.setItems(new String[] { "Transforming", "Picking" });
		combo.select(0);
		combo.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				int i = combo.getSelectionIndex();
				switch (i) {
				case 0:
					graphMouse.setMode(ModalGraphMouse.Mode.TRANSFORMING);
					break;
				case 1:
					graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
					break;
				}
			}
		});
	}

	private void updateStructure(RTSystemItem model) {
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

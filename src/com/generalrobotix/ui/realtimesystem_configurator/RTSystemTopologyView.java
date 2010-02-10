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
import org.openrtp.repository.xsd.rtsystem.Component;
import org.openrtp.repository.xsd.rtsystem.Dataport;
import org.openrtp.repository.xsd.rtsystem.DataportConnector;
import org.openrtp.repository.xsd.rtsystem.TargetPort;

import com.generalrobotix.model.RTCModel;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.swt.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.swt.VisualizationComposite;

public class RTSystemTopologyView extends ViewPart {
	Forest<String, Integer> graph;

	VisualizationComposite<String, Integer> vv;

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
					if (ret.get(0) instanceof RTCModel) {
						count = 0;
						updateStructure(((RTCModel) ret.get(0)).getTop());
					}
				}
			}
		});

		parent.setLayout(new GridLayout());

		graph = new DelegateForest<String, Integer>();

		TreeLayout layout = new TreeLayout<String, Integer>(graph);
		layout.setSize(new Dimension(900, 900));

		final GraphZoomScrollPane<String, Integer> panel = new GraphZoomScrollPane<String, Integer>(
				parent, SWT.NONE, layout, new Dimension(600, 600));
		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		panel.setLayoutData(gridData);

		vv = panel.vv;
		vv.setBackground(Color.white);
		vv.getRenderContext().setEdgeShapeTransformer(
				new EdgeShape.Line<String, Integer>());
		vv.getRenderContext().setVertexLabelTransformer(
				new ToStringLabeller<String>());

		// add a listener for ToolTips
		vv.setVertexToolTipTransformer(new ToStringLabeller<String>());
		vv.getRenderContext().setArrowFillPaintTransformer(
				new ConstantTransformer(Color.lightGray));

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

	int count = 0;

	private void updateStructure(RTCModel model) {
		Iterator<RTCModel> it = model.getChildren().iterator();
		while (it.hasNext()) {
			RTCModel m = it.next();
			graph.addVertex(m.getName());
			Component c = m.getComponent();
			Iterator<Dataport> ports = c.getDataPorts().iterator();
			while (ports.hasNext()) {
				Dataport port = ports.next();
				Iterator<DataportConnector> cons = port.getDataPortConnectors().iterator();
				while(cons.hasNext()) {
					DataportConnector con = cons.next();
					TargetPort tport = con.getTargetDataPort();
					String id = tport.getComponentId();
					String name = tport.getInstanceName();
					RTCModel target = model.getTop().find(id, name);
					if ( target != null ) {
						graph.addVertex(target.getName());
						graph.addEdge(count++, m.getName(), target.getName());
					}
				}
			}
		}
		/*
		 * Iterator<RTCModel> it2 = model.getTop().getChildren().iterator();
		 * while (it2.hasNext()) { RTCModel m = it2.next();
		 * graph.addVertex(m.getName()); graph.getVertices(); }
		 * graph.addVertex("A0"); graph.addEdge(0, "A0", "B0"); graph.addEdge(1,
		 * "A0", "B1"); graph.addEdge(2, "A0", "B2");
		 */
	}

	@Override
	public void setFocus() {
	}

}

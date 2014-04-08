package com.coralcea.jasper.tools.dta.diagrams;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.draw2d.graph.DirectedGraph;
import org.eclipse.draw2d.graph.DirectedGraphLayout;
import org.eclipse.draw2d.graph.Edge;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.GraphicalEditPart;

public class DTAGraphLayoutManager {

	@SuppressWarnings("unchecked")
	private static Map<GraphicalEditPart, Node> applyLayout(GraphicalEditPart container) {
		Map<GraphicalEditPart, Node> nodeMap = new HashMap<GraphicalEditPart, Node>();
		Map<ConnectionEditPart, Edge> edgeMap = new HashMap<ConnectionEditPart, Edge>();
		
		for(Object e : container.getChildren()) {
			GraphicalEditPart editpart = (GraphicalEditPart)e;
			Rectangle bounds = editpart.getFigure().getBounds();
			
			Node node = new Node(editpart.getModel());
			node.x = bounds.x;
			node.y = bounds.y;
			node.width = bounds.width;
			node.height = bounds.height;
			node.setPadding(new Insets(30, 30, 30, 30));
			
			nodeMap.put(editpart, node);
		}

		for(Object e : container.getChildren()) {
			GraphicalEditPart editpart = (GraphicalEditPart)e;
			
			for(Object c : editpart.getSourceConnections()) {
				ConnectionEditPart connection = (ConnectionEditPart) c;
				Node source = nodeMap.get(connection.getSource());
				Node target = nodeMap.get(connection.getTarget());
				
				if (target != null && source != target) {
					Edge edge = new Edge(connection.getModel(), source, target);
					edge.setDelta(2);
					edgeMap.put(connection, edge);
				}
			}
		}
		
		DirectedGraph graph = new DirectedGraph();
		graph.edges.addAll(edgeMap.values());
		graph.nodes.addAll(nodeMap.values());

		DirectedGraphLayout layout = new DirectedGraphLayout();
		layout.visit(graph);
		
		return nodeMap;
	}
	
	public static void layout(GraphicalEditPart container) {
		for(Map.Entry<GraphicalEditPart, Node> e : applyLayout(container).entrySet()) {
			Rectangle rect = new Rectangle(e.getValue().x, e.getValue().y, -1, -1);
			container.setLayoutConstraint(e.getKey(), e.getKey().getFigure(), rect);
		}
	}
}

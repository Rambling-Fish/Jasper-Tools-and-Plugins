package com.coralcea.jasper.tools.dta.diagrams;

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.NodeEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.requests.DropRequest;
import org.eclipse.jface.viewers.ILabelProvider;

public abstract class DTANodeEditPart extends AbstractGraphicalEditPart implements NodeEditPart {

	protected ILabelProvider getLabelProvider() {
		return (ILabelProvider) getViewer().getProperty("LabelProvider");
	}

	@Override
	protected List<Object> getModelSourceConnections() {
		return Collections.emptyList();
	}

	@Override
	protected List<Object> getModelTargetConnections() {
		return Collections.emptyList();
	}

	protected DTANode getNode() {
		return (DTANode) getFigure();
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(ConnectionEditPart connEditPart) {
		return getNode().getConnectionAnchor(getSourceConnectionAnchorKey(connEditPart));
	}

	@Override
	public ConnectionAnchor getSourceConnectionAnchor(Request request) {
		Point pt = new Point(((DropRequest) request).getLocation());
		return getNode().getSourceConnectionAnchorAt(pt);
	}

	protected String getSourceConnectionAnchorKey(ConnectionEditPart connEditPart) {
		return "Center";
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(ConnectionEditPart connEditPart) {
		return getNode().getConnectionAnchor(getTargetConnectionAnchorKey(connEditPart));
	}

	@Override
	public ConnectionAnchor getTargetConnectionAnchor(Request request) {
		Point pt = new Point(((DropRequest) request).getLocation());
		return getNode().getTargetConnectionAnchorAt(pt);
	}

	protected String getTargetConnectionAnchorKey(ConnectionEditPart connEditPart) {
		return "Center";
	}
	
	final protected String mapConnectionAnchorToTerminal(ConnectionAnchor c) {
		return getNode().getConnectionAnchorName(c);
	}

	@Override
	protected void createEditPolicies() {
	}

}

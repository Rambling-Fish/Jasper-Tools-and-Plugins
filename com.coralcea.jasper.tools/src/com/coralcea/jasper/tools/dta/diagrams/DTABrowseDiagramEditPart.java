package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FanRouter;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.DeselectAllTracker;
import org.eclipse.gef.tools.MarqueeDragTracker;
import org.eclipse.swt.SWT;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;

public class DTABrowseDiagramEditPart extends DTANodeEditPart implements LayerConstants {

	public DTABrowseDiagramEditPart(OntModel model) {
		setModel(model);
	}
	
	protected OntModel getOntModel() {
		return (OntModel) getModel();
	}

	@Override
	protected IFigure createFigure() {
		Figure f = new FreeformLayer();
		f.setLayoutManager(new FreeformLayout());
		f.setBorder(new MarginBorder(5));
		return f;
	}

	@Override
	public DragTracker getDragTracker(Request req) {
		if (req instanceof SelectionRequest
				&& ((SelectionRequest) req).getLastButtonPressed() == 3)
			return new DeselectAllTracker(this);
		return new MarqueeDragTracker();
	}

	@Override
	protected List<OntResource> getModelChildren() {
		List<OntResource> children = new ArrayList<OntResource>();
		children.addAll(DTAUtilities.listClasses(getOntModel()));
		children.addAll(DTAUtilities.listProperties(getOntModel()));
		for (Individual dta : getOntModel().listIndividuals(DTA.DTA).toList()) {
			children.add(dta);
			children.addAll(DTAUtilities.listOntResourceObjects(dta, DTA.operation));
			children.addAll(DTAUtilities.listOntResourceObjects(dta, DTA.request));
		}
		return children;
	}

	@Override
	protected void refreshVisuals() {
		ConnectionLayer cLayer = (ConnectionLayer) getLayer(CONNECTION_LAYER);
		if ((getViewer().getControl().getStyle() & SWT.MIRRORED) == 0)
			cLayer.setAntialias(SWT.ON);

		FanRouter router = new FanRouter();
		router.setNextRouter(new DTABendpointConnectionRouter());
		cLayer.setConnectionRouter(router);
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		getFigure().validate();
		DTAGraphLayoutManager.layout(this);
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new DTAXYLayoutEditPolicy());
	}

	private class DTAXYLayoutEditPolicy extends XYLayoutEditPolicy {
		protected Command getCreateCommand(CreateRequest request) {
			return null;
		}
		protected EditPolicy createChildEditPolicy(EditPart child) {
			NonResizableEditPolicy editpolicy = new NonResizableEditPolicy();
			editpolicy.setDragAllowed(false);
			return editpolicy;
		}
	}
}

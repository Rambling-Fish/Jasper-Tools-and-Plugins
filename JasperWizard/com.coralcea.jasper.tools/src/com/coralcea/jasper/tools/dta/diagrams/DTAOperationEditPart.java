package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;

import com.hp.hpl.jena.ontology.OntResource;

public class DTAOperationEditPart extends DTAResourceNodeEditPart {
	
	public DTAOperationEditPart(OntResource operation) {
		super(operation);
	}

	protected OntResource getOperation() {
		return getOntResource();
	}

	@Override
	protected IFigure createFigure() {
		Label label = new Label();
		label.setLabelAlignment(PositionConstants.LEFT);
		label.setBorder(new MarginBorder(2));
		label.setOpaque(true);
		return label;
	}

	@Override
	protected void refreshVisuals() {
		super.refreshVisuals();

		Label label = (Label)getFigure();
		label.setText(getLabelProvider().getText(getOperation()));
		label.setToolTip(new Label(getOperation().getURI()));
		label.setIcon(getLabelProvider().getImage(getOperation()));
	}
	
	@Override
	protected void createEditPolicies() {
		NonResizableEditPolicy editpolicy = new NonResizableEditPolicy() {
			protected void showSelection() {
				super.showSelection();
				getHostFigure().setBackgroundColor(ColorConstants.menuBackgroundSelected);
			}			
			protected void hideSelection() {
				super.hideSelection();
				getHostFigure().setBackgroundColor(null);
			}
		};
		editpolicy.setDragAllowed(false);
		installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, editpolicy);
	}
	
	@Override
	public DragTracker getDragTracker(Request request) {
		if (getViewer().getSelectedEditParts().isEmpty())
			return getParent().getDragTracker(request);
		return super.getDragTracker(request);
	}
}

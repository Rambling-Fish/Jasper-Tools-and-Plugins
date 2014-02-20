package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;

public class DTAPropertyEditPart extends DTAResourceNodeEditPart {
	
	public DTAPropertyEditPart(OntProperty property) {
		super(property);
	}

	protected OntProperty getOntProperty() {
		return (OntProperty) getModel();
	}

	@Override
	protected IFigure createFigure() {
		Label label = new Label();
		label.setLabelAlignment(PositionConstants.LEFT);
		label.setBorder(new MarginBorder(2));
		return label;
	}

	@Override
	protected void refreshVisuals() {
		super.refreshVisuals();

		OntClass aClass = ((DTAClassEditPart)getParent()).getOntClass();
		Restriction initial = DTAUtilities.getRestriction(aClass, DTA.restriction, getOntProperty());
	    String initialValue = " ["+DTAUtilities.getRestrictionValue(initial)+"]";

		Label label = (Label)getFigure();
		label.setText(getLabelProvider().getText(getOntProperty())+initialValue);
		label.setToolTip(new Label(getOntProperty().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntProperty()));
	}
	
	@Override
	protected void createEditPolicies() {
		NonResizableEditPolicy editpolicy = new NonResizableEditPolicy();
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

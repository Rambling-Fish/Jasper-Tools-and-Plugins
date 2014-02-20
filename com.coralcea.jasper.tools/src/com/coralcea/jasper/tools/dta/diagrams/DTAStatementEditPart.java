package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MidpointLocator;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.handles.HandleBounds;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAStatementEditPart extends DTAConnectionEditPart {

	public DTAStatementEditPart(Statement statement) {
		setModel(statement);
	}
	
	protected Statement getStatement() {
		return (Statement) getModel();
	}

	@Override
	protected IFigure createFigure() {
		StatementPolyline polyline =  new StatementPolyline();
		
		PolylineDecoration decoration = new PolylineDecoration();
		decoration.setScale(10, 5);
		polyline.setTargetDecoration(decoration);
		
		Label label = new Label();
		label.setOpaque(true);
		polyline.add(label);
		
		return polyline;
	}
	
	protected Label getLabel() {
		return (Label)getFigure().getChildren().get(1);
	}
	
	protected String getLabelText() {
	    Property p = getStatement().getPredicate();
		if (p.equals(RDFS.subClassOf))
			return "subclass of";
		if (p.equals(RDFS.range))
			return "of type";
		if (p.equals(RDFS.domain)) {
			if (getSource()!=null && getTarget()!=null) {
				OntClass aClass = ((DTAClassEditPart2)getSource()).getOntClass();
				OntProperty aProperty = ((DTAPropertyEditPart2)getTarget()).getOntProperty();
				Restriction initial = DTAUtilities.getRestriction(aClass, DTA.restriction, aProperty);
			    String initialValue = " ["+DTAUtilities.getRestrictionValue(initial)+"]";
				return "has";
			}
		}
		if (p.equals(DTA.input)) {
			if (getSource()!=null && getTarget()!=null) {
				OntResource operation = ((DTAOperationEditPart)getSource()).getOperation();
				OntProperty aProperty = ((DTAPropertyEditPart2)getTarget()).getOntProperty();
				Restriction initial = DTAUtilities.getRestriction(operation, DTA.inputRestriction, aProperty);
			    String initialValue = " ["+DTAUtilities.getRestrictionValue(initial)+"]";
				return "input";
			}
		}
		if (p.equals(DTA.output)) {
			if (getSource()!=null && getTarget()!=null) {
				OntResource operation = ((DTAOperationEditPart)getSource()).getOperation();
				OntProperty aProperty = ((DTAPropertyEditPart2)getTarget()).getOntProperty();
				Restriction initial = DTAUtilities.getRestriction(operation, DTA.outputRestriction, aProperty);
			    String initialValue = " ["+DTAUtilities.getRestrictionValue(initial)+"]";
				return "output";
			}
		}
		return p.getLocalName();
	}

	@Override
	protected void refreshVisuals() {
	    PolylineConnection polyline = (PolylineConnection) getFigure();
		getLabel().setText(getLabelText());
		polyline.setConstraint(getLabel(), new MidpointLocator(polyline, 0));
	}

	private class StatementPolyline extends PolylineConnection implements HandleBounds {
		
		public Rectangle getHandleBounds() {
			return getLabel().getBounds();
		}
	}
}

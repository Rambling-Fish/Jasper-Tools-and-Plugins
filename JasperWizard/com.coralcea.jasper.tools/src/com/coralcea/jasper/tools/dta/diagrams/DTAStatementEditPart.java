package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.ArrowLocator;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.RotatableDecoration;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;

import com.coralcea.jasper.tools.dta.DTA;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;
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
		PolylineConnection polyline =  new PolylineConnection();
		polyline.setForegroundColor(ColorConstants.lightGray);
		polyline.setToolTip(new Label(getLabelText()));
		
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setScale(6,3);
		polyline.add(decoration, new ArrowLocator(polyline, ConnectionLocator.MIDDLE) {
			public void relocate(IFigure target) {
				PointList points = getConnection().getPoints();
				RotatableDecoration arrow = (RotatableDecoration) target;
				arrow.setLocation(getLocation(points));
				arrow.setReferencePoint(points.getPoint(0));
			}
		});
		
		return polyline;
	}
	
	protected Label getLabel() {
		return (Label)getFigure().getChildren().get(1);
	}
	
	protected String getLabelText() {
	    Property p = getStatement().getPredicate();
		if (p.equals(RDFS.subClassOf))
			return "subTypeOf";
		if (p.equals(RDFS.subPropertyOf))
			return "subPropertyOf";
		if (p.equals(OWL.equivalentProperty))
			return "equivalentTo";
		if (p.equals(RDFS.range))
			return "ofType";
		if (p.equals(RDFS.domain))
			return "hasProperty";
		if (p.equals(DTA.output))
			return "hasOutput";
		if (p.equals(DTA.input))
			return "hasInput";
		if (p.equals(DTA.operation))
			return "hasOperation";
		if (p.equals(DTA.request))
			return "hasRequest";
		return p.getLocalName();
	}

	@Override
	protected void refreshVisuals() {
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new org.eclipse.gef.tools.DragEditPartsTracker(this) {
			protected boolean isMove() {
				return true;
			}
		};
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new StatementEndpointEditPolicy());
	}

	private class StatementEndpointEditPolicy extends ConnectionEndpointEditPolicy {
			
		protected void addSelectionHandles() {
			getConnectionFigure().setLineWidth(2);
			getConnectionFigure().setForegroundColor(ColorConstants.red);
		}
		
		protected PolylineConnection getConnectionFigure() {
			return (PolylineConnection) ((GraphicalEditPart) getHost()).getFigure();
		}
		
		protected void removeSelectionHandles() {
			getConnectionFigure().setLineWidth(0);
			getConnectionFigure().setForegroundColor(ColorConstants.lightGray);
		}
	}
	
}

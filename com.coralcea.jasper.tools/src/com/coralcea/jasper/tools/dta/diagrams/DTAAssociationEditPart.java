package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.ConnectionLocator;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.PolylineDecoration;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.handles.HandleBounds;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.RefreshEditPartCommand;
import com.coralcea.jasper.tools.dta.commands.SetPropertyCommand;
import com.coralcea.jasper.tools.dta.editors.DTAEditor;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;

public class DTAAssociationEditPart extends DTAConnectionEditPart {

	public DTAAssociationEditPart(OntProperty property) {
		setModel(property);
	}

	protected OntProperty getOntProperty() {
		return (OntProperty) getModel();
	}

	@Override
	protected IFigure createFigure() {
		AssociationPolyline polyline =  new AssociationPolyline();
		
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

	@Override
	protected void refreshVisuals() {
		OntClass aClass = ((DTAClassEditPart)getSource()).getOntClass();
		Restriction initial = DTAUtilities.getRestriction(aClass, DTA.restriction, getOntProperty());
	    String initialValue = " ["+DTAUtilities.getRestrictionValue(initial)+"]";

		getLabel().setText(DTAUtilities.getLabel(getOntProperty())+initialValue);
		getLabel().setToolTip(new Label(getOntProperty().getURI()));
		getLabel().setIcon(getLabelProvider().getImage(getOntProperty()));

		int x = DTAUtilities.getIntegerValue(getOntProperty(), DTA.x);
		int y = DTAUtilities.getIntegerValue(getOntProperty(), DTA.y);
	    PolylineConnection polyline = (PolylineConnection) getFigure();
		polyline.setConstraint(getLabel(), new AssociationLabelLocator(polyline, x, y));
	}

	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.PRIMARY_DRAG_ROLE, new AssociationNonResizableEditPolcy());
		installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE, new AssociationEndpointEditPolicy());
	}
	
	@Override
	public void performRequest(Request req) {
		DTAEditor editor = (DTAEditor) ((DefaultEditDomain)getViewer().getEditDomain()).getEditorPart();
		editor.setSelectedElement(getOntProperty(), DTAEditor.PAGE_MODEL);
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new org.eclipse.gef.tools.DragEditPartsTracker(this) {
			protected boolean isMove() {
				return true;
			}
		};
	}
	
	private class AssociationPolyline extends PolylineConnection implements HandleBounds {
		
		public Rectangle getHandleBounds() {
			return getLabel().getBounds();
		}
	}
	
	private class AssociationLabelLocator extends ConnectionLocator {
		
		private int xOffset, yOffset;
		
		public AssociationLabelLocator(Connection connection, int x, int y) {
			super(connection);
			xOffset = x;
			yOffset = y;
		}
		
		public Point getOffset() {
			return new Point(xOffset, yOffset);
		}
		
		protected Point getReferencePoint() {
			Connection conn = getConnection();
			Point p = Point.SINGLETON;
			int size = conn.getPoints().size()-2;
			Point p1 = conn.getPoints().getPoint(size);
			Point p2 = conn.getPoints().getPoint(size+1);
			conn.translateToAbsolute(p1);
			conn.translateToAbsolute(p2);
			p.x = (p2.x - p1.x) / 2 + p1.x + xOffset;
			p.y = (p2.y - p1.y) / 2 + p1.y + yOffset;
			return p;
		}
	}

	private class AssociationEndpointEditPolicy extends ConnectionEndpointEditPolicy {
		
		protected void addSelectionHandles() {
			getConnectionFigure().setLineWidth(2);
		}
		
		protected PolylineConnection getConnectionFigure() {
			return (PolylineConnection) ((GraphicalEditPart) getHost()).getFigure();
		}
		
		protected void removeSelectionHandles() {
			getConnectionFigure().setLineWidth(0);
		}
	}
	
	private class AssociationNonResizableEditPolcy extends NonResizableEditPolicy {
		
		protected Command getMoveCommand(ChangeBoundsRequest request) {
			OntProperty p = getOntProperty();
			AssociationLabelLocator locator = (AssociationLabelLocator) getFigure().getLayoutManager().getConstraint(getLabel());
			Point loc = locator.getOffset();
			Point delta = request.getMoveDelta();
			CompoundCommand cc = new CompoundCommand("Change Position");
			cc.add(new SetPropertyCommand(p, DTA.x, p.getModel().createTypedLiteral(loc.x+delta.x)));
			cc.add(new SetPropertyCommand(p, DTA.y, p.getModel().createTypedLiteral(loc.y+delta.y)));
			cc.add(new RefreshEditPartCommand(getParent()));
			return cc;
		}
	}
		
}

package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolygonDecoration;
import org.eclipse.draw2d.PolylineConnection;

import com.hp.hpl.jena.rdf.model.Statement;

public class DTAGeneralizationEditPart extends DTAConnectionEditPart {

	public DTAGeneralizationEditPart(Statement statement) {
		setModel(statement);
	}
	
	protected Statement getStatement() {
		return (Statement) getModel();
	}

	protected IFigure createFigure() {
		PolylineConnection polyline = (PolylineConnection) super.createFigure();
		
		PolygonDecoration decoration = new PolygonDecoration();
		decoration.setBackgroundColor(ColorConstants.white);
		decoration.setScale(10, 5);
		polyline.setTargetDecoration(decoration);
		
		return polyline;
	}
}

package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RoundedRectangle;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class DTADTAEditPart extends DTAResourceNodeEditPart {
	
	public DTADTAEditPart(OntResource resource) {
		super(resource);
	}
	
	public OntResource getDTA() {
		return getOntResource();
	}

	@Override
	protected IFigure createFigure() {
		DTANodeFigure figure = new DTANodeFigure();
		figure.setLayoutManager(new StackLayout());
		
		RoundedRectangle diamond = new RoundedRectangle();
		diamond.setCornerDimensions(new Dimension(15, 15));
		diamond.setBorder(new MarginBorder(10));
		diamond.setLayoutManager(new ToolbarLayout());
		figure.add(diamond);

		Label label = new Label();
		label.setLabelAlignment(PositionConstants.CENTER);
		diamond.add(label);

		return figure;
	}

	@Override
	protected void refreshVisuals() {
		Rectangle r = new Rectangle(0, 0, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		IFigure ellipse = (IFigure)getFigure().getChildren().get(0);

		Label label = (Label)ellipse.getChildren().get(0);
		label.setText(DTAUtilities.getLabel(getDTA()));
		label.setToolTip(new Label(getDTA().getURI()));
		label.setIcon(getLabelProvider().getImage(getDTA()));
	}
	
	@Override
	protected List<Object> getModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getDTA().getOntModel().listStatements(getDTA(), DTA.operation, (RDFNode)null).toList());
		connections.addAll(getDTA().getOntModel().listStatements(getDTA(), DTA.request, (RDFNode)null).toList());
		return connections;
	}

}

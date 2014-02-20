package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Ellipse;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.ConnectionEditPart;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAPropertyEditPart2 extends DTAResourceNodeEditPart {
	
	public DTAPropertyEditPart2(OntProperty property) {
		super(property);
	}

	protected OntProperty getOntProperty() {
		return (OntProperty) getModel();
	}

	@Override
	protected IFigure createFigure() {
		DTANodeFigure figure = new DTANodeFigure();
		figure.setLayoutManager(new StackLayout());
		
		Ellipse ellipse = new Ellipse();
		ellipse.setBorder(new MarginBorder(10));
		ellipse.setLayoutManager(new ToolbarLayout());
		figure.add(ellipse);

		Label label = new Label();
		label.setLabelAlignment(PositionConstants.CENTER);
		ellipse.add(label);

		return figure;
	}

	@Override
	protected void refreshVisuals() {
		Rectangle r = new Rectangle(0, 0, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		Ellipse ellipse = (Ellipse)getFigure().getChildren().get(0);

		Label label = (Label)ellipse.getChildren().get(0);
		if (getOntProperty().isDatatypeProperty())
			label.setText(getLabelProvider().getText(getOntProperty()));
		else
			label.setText(DTAUtilities.getLabel(getOntProperty()));
		label.setToolTip(new Label(getOntProperty().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntProperty()));
	}
	
	protected String getTargetConnectionAnchorKey(ConnectionEditPart connEditPart) {
		return "CenterEllipse";
	}

	protected String getSourceConnectionAnchorKey(ConnectionEditPart connEditPart) {
		return "CenterEllipse";
	}

	@Override
	protected List<Object> getModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		if (getOntProperty().isObjectProperty())
			connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), RDFS.range, (RDFNode)null).toList());
		return connections;
	}

	@Override
	protected List<Object> getModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), RDFS.domain, (RDFNode)null).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(null, DTA.input, getOntProperty()).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(null, DTA.output, getOntProperty()).toList());
		return connections;
	}
}

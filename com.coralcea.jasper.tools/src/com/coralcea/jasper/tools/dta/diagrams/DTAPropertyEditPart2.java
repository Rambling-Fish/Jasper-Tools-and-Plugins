package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAPropertyEditPart2 extends DTABrowseNodeEditPart {
	
	public DTAPropertyEditPart2(OntProperty property) {
		super(property);
	}

	protected OntProperty getOntProperty() {
		return (OntProperty) getModel();
	}

	@Override
	protected IFigure createFigure() {
		return new DTANodeLabel();
	}

	@Override
	protected void refreshVisuals() {
		Rectangle r = new Rectangle(0, 0, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		Label label = (Label)getFigure();
		label.setText(DTAUtilities.getLabel(getOntProperty()));
		label.setToolTip(new Label(getOntProperty().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntProperty()));
	}
	
	@Override
	protected List<Object> findModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), RDFS.subPropertyOf, (RDFNode)null).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), OWL.equivalentProperty, (RDFNode)null).toList());
		if (getOntProperty().isObjectProperty())
			connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), RDFS.range, (RDFNode)null).toList());
		return connections;
	}

	@Override
	protected List<Object> findModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntProperty().getOntModel().listStatements(null, RDFS.subPropertyOf, getOntProperty()).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(null, OWL.equivalentProperty, getOntProperty()).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(getOntProperty(), RDFS.domain, (RDFNode)null).toList());
		connections.addAll(getOntProperty().getOntModel().listStatements(null, DTA.output, getOntProperty()).toList());
		return connections;
	}

}

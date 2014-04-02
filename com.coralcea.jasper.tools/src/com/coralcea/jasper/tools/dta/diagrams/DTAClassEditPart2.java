package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAClassEditPart2 extends DTABrowseNodeEditPart {
	
	public DTAClassEditPart2(OntClass aClass) {
		super(aClass);
	}

	protected OntClass getOntClass() {
		return (OntClass) getModel();
	}

	@Override
	protected IFigure createFigure() {
		return new DTANodeLabel();
	}

	@Override
	protected void refreshVisuals() {
		Rectangle r = new Rectangle(0, 0, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		DTANodeLabel label = (DTANodeLabel)getFigure();
		label.setText(getLabelProvider().getText(getOntClass()));
		label.setToolTip(new Label(getOntClass().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntClass()));
	}
	
	@Override
	protected List<Object> findModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntClass().getOntModel().listStatements(getOntClass(), RDFS.subClassOf, (RDFNode)null).toList());
		connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.domain, getOntClass()).toList());
		return connections;
	}

	@Override
	protected List<Object> findModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.subClassOf, getOntClass()).toList());
		if (!RDFS.Literal.equals(getOntClass()))
			connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.range, getOntClass()).toList());
		connections.addAll(getOntClass().getOntModel().listStatements(null, DTA.input, getOntClass()).toList());
		return connections;
	}
	
}

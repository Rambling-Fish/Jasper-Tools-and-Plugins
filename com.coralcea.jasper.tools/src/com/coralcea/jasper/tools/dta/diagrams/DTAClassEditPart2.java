package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.StackLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAClassEditPart2 extends DTAResourceNodeEditPart {
	
	public DTAClassEditPart2(OntClass aClass) {
		super(aClass);
	}

	protected OntClass getOntClass() {
		return (OntClass) getModel();
	}

	@Override
	protected IFigure createFigure() {
		DTANodeFigure figure = new DTANodeFigure();
		figure.setBorder(new LineBorder(1));
		figure.setLayoutManager(new StackLayout());

		Label label = new Label();
		label.setLabelAlignment(PositionConstants.CENTER);
		label.setBorder(new MarginBorder(5));
		figure.add(label);
		
		return figure;
	}

	@Override
	protected void refreshVisuals() {
		Rectangle r = new Rectangle(0, 0, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		Label label = (Label)getFigure().getChildren().get(0);
		label.setText(getLabelProvider().getText(getOntClass()));
		label.setToolTip(new Label(getOntClass().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntClass()));
	}
	
	@Override
	protected List<Object> getModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntClass().getOntModel().listStatements(getOntClass(), RDFS.subClassOf, (RDFNode)null).toList());
		connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.domain, getOntClass()).toList());
		return connections;
	}

	@Override
	protected List<Object> getModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.subClassOf, getOntClass()).toList());
		connections.addAll(getOntClass().getOntModel().listStatements(null, RDFS.range, getOntClass()).toList());
		return connections;
	}

}

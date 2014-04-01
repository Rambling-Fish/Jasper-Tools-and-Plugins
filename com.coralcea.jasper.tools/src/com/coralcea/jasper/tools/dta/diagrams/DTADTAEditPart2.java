package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class DTADTAEditPart2 extends DTABrowseNodeEditPart {
	
	public DTADTAEditPart2(OntResource resource) {
		super(resource);
	}
	
	public OntResource getDTA() {
		return getOntResource();
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
		label.setText(DTAUtilities.getLabel(getDTA()));
		label.setToolTip(new Label(getDTA().getURI()));
		label.setIcon(getLabelProvider().getImage(getDTA()));
	}
	
	@Override
	protected List<Object> findModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getDTA().getOntModel().listStatements(getDTA(), DTA.operation, (RDFNode)null).toList());
		connections.addAll(getDTA().getOntModel().listStatements(getDTA(), DTA.request, (RDFNode)null).toList());
		return connections;
	}

}

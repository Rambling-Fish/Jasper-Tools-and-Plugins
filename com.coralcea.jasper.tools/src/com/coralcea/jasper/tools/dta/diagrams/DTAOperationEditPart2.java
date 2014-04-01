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

public class DTAOperationEditPart2 extends DTABrowseNodeEditPart {
	
	public DTAOperationEditPart2(OntResource resource) {
		super(resource);
	}
	
	public OntResource getOperation() {
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
		label.setText(DTAUtilities.getLabel(getOperation()));
		label.setToolTip(new Label(getOperation().getURI()));
		label.setIcon(getLabelProvider().getImage(getOperation()));
	}
	
	@Override
	protected List<Object> findModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOperation().getOntModel().listStatements(getOperation(), DTA.input, (RDFNode)null).toList());
		connections.addAll(getOperation().getOntModel().listStatements(getOperation(), DTA.output, (RDFNode)null).toList());
		return connections;
	}

	@Override
	protected List<Object> findModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		connections.addAll(getOperation().getOntModel().listStatements(null, DTA.operation, getOperation()).toList());
		connections.addAll(getOperation().getOntModel().listStatements(null, DTA.request, getOperation()).toList());
		return connections;
	}

}

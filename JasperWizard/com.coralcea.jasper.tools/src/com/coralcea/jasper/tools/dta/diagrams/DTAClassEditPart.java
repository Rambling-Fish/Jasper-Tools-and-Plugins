package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.ToolbarLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAClassEditPart extends DTAResourceNodeEditPart {
	
	public DTAClassEditPart(OntClass aClass) {
		super(aClass);
	}

	protected OntClass getOntClass() {
		return (OntClass) getModel();
	}

	@Override
	protected IFigure createFigure() {
		DTANodeFigure figure = new DTANodeFigure();
		figure.setBorder(new LineBorder(1));
		
		ToolbarLayout layout = new ToolbarLayout(false);
		layout.setStretchMinorAxis(true);
		figure.setLayoutManager(layout);

		Label label = new Label();
		label.setLabelAlignment(PositionConstants.CENTER);
		label.setBorder(new MarginBorder(5));
		figure.add(label);
		
		IFigure separator = new RectangleFigure();
		separator.setPreferredSize(new Dimension(0, 1));
		figure.add(separator);

		IFigure compartment = new Figure();
		
		layout = new ToolbarLayout(false);
		layout.setStretchMinorAxis(true);
		compartment.setLayoutManager(layout);
		
		figure.add(compartment);
		
		return figure;
	}

	@Override
	public IFigure getContentPane() {
		return (IFigure) getFigure().getChildren().get(2);
	}
	
	@Override
	protected void refreshVisuals() {
		int x = DTAUtilities.getIntegerValue(getOntClass(), DTA.x);
		int y = DTAUtilities.getIntegerValue(getOntClass(), DTA.y);
		Rectangle r = new Rectangle(x, y, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		Label label = (Label)getFigure().getChildren().get(0);
		label.setText(getLabelProvider().getText(getOntClass()));
		label.setToolTip(new Label(getOntClass().getURI()));
		label.setIcon(getLabelProvider().getImage(getOntClass()));

		IFigure separator = (IFigure) getFigure().getChildren().get(1);
		separator.setVisible(!getModelChildren().isEmpty()); 
		IFigure compartment = (IFigure) getFigure().getChildren().get(2);
		compartment.setVisible(!getModelChildren().isEmpty()); 
	}

	@Override
	protected List<Resource> getModelChildren() {
		List<Resource> children = new ArrayList<Resource>();
		OntModel model = getOntClass().getOntModel();
		DTAUMLDiagramEditPart parent = (DTAUMLDiagramEditPart) getParent();
		
		for (StmtIterator i = model.getBaseModel().listStatements(null, RDFS.domain, getOntClass()); i.hasNext();) {
			Statement s = i.next();
        	if (parent.getModelConnections().contains(s))
        		children.add(model.getOntResource(s.getSubject()).as(OntProperty.class));
		}
		Collections.sort(children, new Comparator<Resource>() {
			public int compare(Resource o1, Resource o2) {
				String s1 = getLabelProvider().getText(o1);
				String s2 = getLabelProvider().getText(o2);
				return s1.compareTo(s2);
			}
		 });
		 return children;
	}
	
	@Override
	protected List<Object> getModelSourceConnections() {
		List<Object> connections = new ArrayList<Object>();
		OntModel model = getOntClass().getOntModel();
		DTAUMLDiagramEditPart parent = (DTAUMLDiagramEditPart) getParent();

		for (StmtIterator i = model.getBaseModel().listStatements(getOntClass(), RDFS.subClassOf, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
        	if (parent.getModelConnections().contains(s))
        		connections.add(s);
		}		
		return connections;
	}

	@Override
	protected List<Object> getModelTargetConnections() {
		List<Object> connections = new ArrayList<Object>();
		OntModel model = getOntClass().getOntModel();
		DTAUMLDiagramEditPart parent = (DTAUMLDiagramEditPart) getParent();

		for (StmtIterator i = model.getBaseModel().listStatements(null, RDFS.subClassOf, getOntClass()); i.hasNext();) {
			Statement s = i.next();
        	if (parent.getModelConnections().contains(s))
        		connections.add(s);
		}		
		return connections;
	}
}

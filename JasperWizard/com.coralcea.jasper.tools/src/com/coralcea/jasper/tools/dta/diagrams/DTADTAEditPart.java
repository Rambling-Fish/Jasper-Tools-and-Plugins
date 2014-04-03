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
import com.hp.hpl.jena.ontology.OntResource;

public class DTADTAEditPart extends DTAResourceNodeEditPart {
	
	public DTADTAEditPart(OntResource resource) {
		super(resource);
	}

	protected OntResource getDTA() {
		return getOntResource();
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
		int x = DTAUtilities.getIntegerValue(getDTA(), DTA.x);
		int y = DTAUtilities.getIntegerValue(getDTA(), DTA.y);
		Rectangle r = new Rectangle(x, y, -1, -1);
		((GraphicalEditPart) getParent()).setLayoutConstraint(this, getFigure(), r);

		Label label = (Label)getFigure().getChildren().get(0);
		label.setText(getLabelProvider().getText(getDTA()));
		label.setToolTip(new Label(getDTA().getURI()));
		label.setIcon(getLabelProvider().getImage(getDTA()));

		IFigure separator = (IFigure) getFigure().getChildren().get(1);
		separator.setVisible(!getModelChildren().isEmpty()); 
		IFigure compartment = (IFigure) getFigure().getChildren().get(2);
		compartment.setVisible(!getModelChildren().isEmpty()); 
	}

	@Override
	protected List<OntResource> getModelChildren() {
		List<OntResource> children = new ArrayList<OntResource>();
		
		List<OntResource> operations = new ArrayList<OntResource>();
		operations.addAll(DTAUtilities.listObjects(getDTA(), DTA.operation, OntResource.class));
		Collections.sort(operations, new Comparator<OntResource>() {
			public int compare(OntResource o1, OntResource o2) {
				String s1 = getLabelProvider().getText(o1);
				String s2 = getLabelProvider().getText(o2);
				return s1.compareTo(s2);
			}
		});
		children.addAll(operations);
		 
		List<OntResource> requests = new ArrayList<OntResource>();
		requests.addAll(DTAUtilities.listObjects(getDTA(), DTA.request, OntResource.class));
		Collections.sort(requests, new Comparator<OntResource>() {
			public int compare(OntResource o1, OntResource o2) {
				String s1 = getLabelProvider().getText(o1);
				String s2 = getLabelProvider().getText(o2);
				return s1.compareTo(s2);
			}
		});
		children.addAll(requests);
		
		return children;
	}
	
}

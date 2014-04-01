package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FanRouter;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.DeselectAllTracker;
import org.eclipse.gef.tools.MarqueeDragTracker;
import org.eclipse.swt.SWT;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.TransientSetLayoutConstraintCommand;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTABrowseDiagramEditPart extends DTAResourceNodeEditPart  {

	private List<OntResource> modelChildren;
	private List<Statement> modelConnections;
	
	public DTABrowseDiagramEditPart(Ontology ontology) {
		super(ontology);
	}
	
	protected Ontology getOntology() {
		return (Ontology) getModel();
	}

	@Override
	protected IFigure createFigure() {
		Figure f = new FreeformLayer();
		f.setLayoutManager(new FreeformLayout());
		f.setBorder(new MarginBorder(5));
		return f;
	}

	@Override
	public DragTracker getDragTracker(Request req) {
		if (req instanceof SelectionRequest
				&& ((SelectionRequest) req).getLastButtonPressed() == 3)
			return new DeselectAllTracker(this);
		return new MarqueeDragTracker();
	}

	public List<Statement> getModelConnections() {
		if (modelConnections == null)
			collectRelevantElements();
		return modelConnections;
	}
	
	@Override
	protected List<OntResource> getModelChildren() {
		if (modelChildren == null)
			collectRelevantElements();
		return modelChildren;
	}
	
	protected void collectRelevantElements() {
		Set<OntResource> relevantChildren = new HashSet<OntResource>();
		modelConnections = new ArrayList<Statement>();
		String filter = (String) getViewer().getProperty("filter");
		OntModel model = getOntology().getOntModel();
		
		StmtIterator i = DTAUtilities.listStatementsOfPredicates(model.getBaseModel(), new Property[]{
			RDFS.subClassOf, RDFS.subPropertyOf, RDFS.domain, RDFS.range, OWL.equivalentProperty, 
			DTA.operation, DTA.request,	DTA.input, DTA.output
		});
		while (i.hasNext()) {
			Statement s = i.next();
			OntResource subject = model.getOntResource(s.getSubject());
			OntResource object = model.getOntResource(s.getObject().asResource());
			if (!model.contains(subject, RDF.type) || !model.contains(object, RDF.type))
				continue;
			if (RDFS.range.equals(s.getPredicate())  && DTAUtilities.isDatatype(object))
				continue;
			if (DTAUtilities.isTypedBy(subject, new Resource[] {RDFS.Datatype, OWL.AnnotationProperty, OWL.OntologyProperty}))
				continue;
			if (DTAUtilities.isTypedBy(object, new Resource[] {RDFS.Datatype, OWL.AnnotationProperty, OWL.OntologyProperty}))
				continue;
			if ("Types".equals(filter) && !RDFS.subClassOf.equals(s.getPredicate()))
				continue;
			if ("Properties".equals(filter) &&!RDFS.subPropertyOf.equals(s.getPredicate()) && !OWL.equivalentProperty.equals(s.getPredicate()))
				continue;
			if ("Associations".equals(filter) && !RDFS.domain.equals(s.getPredicate()) && !RDFS.range.equals(s.getPredicate()))
				continue;
			if ("Operations".equals(filter) && !DTA.operation.equals(s.getPredicate()) && !DTA.request.equals(s.getPredicate()) && !DTA.input.equals(s.getPredicate()) && !DTA.output.equals(s.getPredicate()))
				continue;
			if ("Element".equals(filter)) {
				Object element = getViewer().getProperty("element");
				if (!subject.equals(element) && !object.equals(element))
					continue;
			}
			modelConnections.add(s);
			relevantChildren.add(subject);
			relevantChildren.add(object);
		}
			
		if (filter == null) {
			i = DTAUtilities.listStatementsOfPredicates(model.getBaseModel(), new Property[]{RDF.type});
			while (i.hasNext()) {
				Statement s = i.next();
				OntResource subject = model.getOntResource(s.getSubject());
				if (DTAUtilities.isTypedBy(subject, new Resource[] {OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty, DTA.DTA, DTA.Operation, DTA.Request}))
					relevantChildren.add(subject);
			}
		} else if ("Element".equals(filter))
			relevantChildren.add((OntResource)getViewer().getProperty("element"));
		
		modelChildren = new ArrayList<OntResource>(relevantChildren);
	}

	@Override
	protected void refreshVisuals() {
		ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		if ((getViewer().getControl().getStyle() & SWT.MIRRORED) == 0)
			cLayer.setAntialias(SWT.ON);

		FanRouter router = new FanRouter();
		cLayer.setConnectionRouter(router);
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
		getFigure().validate();
		DTAGraphLayoutManager.layout(this);
	}

	@Override
	public void removeNotify() {
		super.removeNotify();
		modelChildren = null;
		modelConnections = null;
	}
	
	@Override
	protected void createEditPolicies() {
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new DTAXYLayoutEditPolicy());
	}

	private class DTAXYLayoutEditPolicy extends XYLayoutEditPolicy {
		protected Command getCreateCommand(CreateRequest request) {
			return null;
		}
		protected EditPolicy createChildEditPolicy(EditPart child) {
			return new NonResizableEditPolicy();
		}
		protected Command createChangeConstraintCommand(ChangeBoundsRequest request, EditPart child, Object constraint) {
			return new TransientSetLayoutConstraintCommand((GraphicalEditPart)getHost(), (GraphicalEditPart)child, constraint);
		}
	}
}

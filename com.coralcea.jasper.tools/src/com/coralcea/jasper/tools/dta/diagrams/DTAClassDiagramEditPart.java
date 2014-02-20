package com.coralcea.jasper.tools.dta.diagrams;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.draw2d.Animation;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.draw2d.FanRouter;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.draw2d.FreeformLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
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
import com.coralcea.jasper.tools.dta.commands.SetPropertyCommand;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTAClassDiagramEditPart extends DTAResourceNodeEditPart {

	public DTAClassDiagramEditPart(Ontology ontology) {
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

	@Override
	protected List<OntClass> getModelChildren() {
		return getRelevantClasses();
	}

	@Override
	protected void refreshVisuals() {
		Animation.markBegin();
		
		ConnectionLayer cLayer = (ConnectionLayer) getLayer(LayerConstants.CONNECTION_LAYER);
		if ((getViewer().getControl().getStyle() & SWT.MIRRORED) == 0)
			cLayer.setAntialias(SWT.ON);

		FanRouter router = new FanRouter();
		router.setNextRouter(new DTABendpointConnectionRouter());
		cLayer.setConnectionRouter(router);

		Animation.run(400);
	}

	private List<OntClass> getRelevantClasses() {
		List<OntClass> relevantTypes = new ArrayList<OntClass>();
		
		for(Iterator<Resource> i = DTAUtilities.listDefinedResources(getOntology(), DTA.DTA); i.hasNext();) {
			Resource dta = i.next();
			
			Set<RDFNode> operations = DTAUtilities.listObjects(dta, DTA.operation);
			operations.addAll(DTAUtilities.listObjects(dta, DTA.request));
			
			for(RDFNode n : operations) {
				Resource op = (Resource) n;
				
				Resource input = op.getPropertyResourceValue(DTA.input);
				if (input != null) {
					Resource ptype = input.getPropertyResourceValue(RDFS.range);
					if (ptype != null && !ptype.getNameSpace().equals(XSD.getURI()))
						getRelevantClasses(ptype.as(OntClass.class), relevantTypes);
				}
				Resource output = op.getPropertyResourceValue(DTA.output);
				if (output != null) {
					Resource ptype = output.getPropertyResourceValue(RDFS.range);
					if (ptype != null && !ptype.getNameSpace().equals(XSD.getURI()))
						getRelevantClasses(ptype.as(OntClass.class), relevantTypes);
				}
			}
		}
		
		for(Iterator<Resource> i = DTAUtilities.listDefinedClasses(getOntology()); i.hasNext();) {
			Resource type = i.next();
			getRelevantClasses(type.as(OntClass.class), relevantTypes);
		}
		
		return relevantTypes;
	}

	private void getRelevantClasses(OntClass type, List<OntClass> relevantTypes) {
        if (relevantTypes.contains(type))
        	return;
        relevantTypes.add(type);
		for (Resource p : DTAUtilities.getDeclaredProperties(type)) {
			Resource ptype = p.getPropertyResourceValue(RDFS.range);
			if (ptype != null && !ptype.getNameSpace().equals(XSD.getURI()))
				getRelevantClasses(ptype.as(OntClass.class), relevantTypes);
        }
		for (RDFNode supertype : DTAUtilities.listObjects(type, RDFS.subClassOf)) {
			getRelevantClasses(supertype.as(OntClass.class), relevantTypes);
        }
		for (Resource subtype : DTAUtilities.listSubjects(RDFS.subClassOf, type)) {
			getRelevantClasses(subtype.as(OntClass.class), relevantTypes);
        }
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
			OntResource resource = ((DTAResourceNodeEditPart) child).getOntResource();
			Rectangle rect = (Rectangle) constraint;
			CompoundCommand cc = new CompoundCommand("Change Position");
			cc.add(new SetPropertyCommand(resource, DTA.x, resource.getModel().createTypedLiteral(rect.x)));
			cc.add(new SetPropertyCommand(resource, DTA.y, resource.getModel().createTypedLiteral(rect.y)));
			//cc.add(new RefreshEditPartCommand(child.getRoot()));
			return cc;
		}
	}
}

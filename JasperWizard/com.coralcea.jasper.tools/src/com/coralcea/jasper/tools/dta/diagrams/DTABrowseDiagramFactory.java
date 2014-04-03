package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DTABrowseDiagramFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof OntModel)
			return new DTABrowseDiagramEditPart(((OntModel)model).listOntologies().next());
		else if (model instanceof Statement)
			return new DTAStatementEditPart((Statement)model);
		else if (model instanceof OntResource) {
			OntResource resource = (OntResource) model;
			if (resource.isClass())
				return new DTAClassEditPart2(resource.asClass());
			else if (resource.isProperty())
				return new DTAPropertyEditPart2(resource.asProperty());
			else if (DTAUtilities.isOperation(resource) || DTAUtilities.isRequest(resource))
				return new DTAOperationEditPart2(resource);
			else if (DTAUtilities.isDTA(resource))
				return new DTADTAEditPart2(resource);
		}
		return null;
	}
}


package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAUMLDiagramFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof OntModel)
			return new DTAUMLDiagramEditPart(((OntModel)model).listOntologies().next());
		else if (model instanceof Statement) {
			if (((Statement)model).getPredicate().equals(RDFS.subClassOf))
				return new DTAGeneralizationEditPart((Statement)model);
		} else if (model instanceof OntResource) {
			OntResource resource = (OntResource) model;
			if (DTAUtilities.isClass(resource))
				return new DTAClassEditPart(resource.as(OntClass.class));
			if (DTAUtilities.isProperty(resource))
				return new DTAPropertyEditPart(resource.as(OntProperty.class));
			if (DTAUtilities.isDTA(resource))
				return new DTADTAEditPart(resource);
			if (DTAUtilities.isOperation(resource))
				return new DTAOperationEditPart(resource);
			if (DTAUtilities.isRequest(resource))
				return new DTAOperationEditPart(resource);
		}
		return null;
	}
}


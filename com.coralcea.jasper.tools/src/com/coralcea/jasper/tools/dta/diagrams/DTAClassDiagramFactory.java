package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAClassDiagramFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof OntModel)
			return new DTAClassDiagramEditPart(((OntModel)model).listOntologies().next());
		else if (model instanceof OntClass)
			return new DTAClassEditPart((OntClass)model);
		else if (model instanceof Statement && ((Statement)model).getPredicate().equals(RDFS.subClassOf))
			return new DTAGeneralizationEditPart((Statement)model);
		else if (model instanceof OntProperty && ((OntProperty)model).isDatatypeProperty())
			return new DTAPropertyEditPart((OntProperty)model);
		else if (model instanceof OntProperty && ((OntProperty)model).isObjectProperty())
			return new DTAAssociationEditPart((OntProperty)model);
		return null;
	}
}


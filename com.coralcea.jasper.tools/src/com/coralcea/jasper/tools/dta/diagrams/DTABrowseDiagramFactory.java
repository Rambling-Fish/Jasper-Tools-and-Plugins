package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DTABrowseDiagramFactory implements EditPartFactory {

	@Override
	public EditPart createEditPart(EditPart context, Object model) {
		if (model instanceof OntModel)
			return new DTABrowseDiagramEditPart((OntModel)model);
		else if (model instanceof OntClass)
			return new DTAClassEditPart2((OntClass)model);
		else if (model instanceof OntProperty)
			return new DTAPropertyEditPart2((OntProperty)model);
		else if (model instanceof Resource) {
			if (DTAUtilities.isOperation((Resource)model) || DTAUtilities.isRequest((Resource)model))
				return new DTAOperationEditPart((OntResource)model);
			else if (DTAUtilities.isDTA((Resource)model))
				return new DTADTAEditPart((OntResource)model);
		} else if (model instanceof Statement)
			return new DTAStatementEditPart((Statement)model);
		return null;
	}
}


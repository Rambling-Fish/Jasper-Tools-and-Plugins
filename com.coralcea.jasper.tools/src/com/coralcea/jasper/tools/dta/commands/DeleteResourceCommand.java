package com.coralcea.jasper.tools.dta.commands;

import java.util.List;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;

public class DeleteResourceCommand extends DTACommand {

	private Resource element;
	private Model model;
	private List<Statement> statements;
	
	public DeleteResourceCommand(Resource element) {
		super("Deleting Element");
		this.element = element;
		this.model = element.getModel();
		if (this.model instanceof OntModel)
			this.model = ((OntModel)model).getBaseModel();
	}

	@Override
	public void prepare() {
    	statements = DTAUtilities.getStatementsOn(model, element);
    	
		for (RDFNode object : DTAUtilities.listObjects(element, null))
			if (object.isAnon())
				statements.addAll(DTAUtilities.getStatementsOn(model, (Resource)object));
		
    	if (element instanceof Property)
    		for (Resource restriction : DTAUtilities.listSubjects(OWL.onProperty, element))
    			statements.addAll(DTAUtilities.getStatementsOn(model, restriction));
	}

	@Override
	public void undo() {
		if (!statements.isEmpty())
			model.add(statements);
	}

	@Override
	public void redo() {
		if (!statements.isEmpty())
			model.remove(statements);
	}

}

package com.coralcea.jasper.tools.dta.commands;

import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DeleteCommand extends DTACommand {

	private OntResource element;
	private List<Statement> statements;
	
	public DeleteCommand(OntResource element) {
		super("Deleting Element");
		this.element = element;
	}

	@Override
	public void store() {
		statements = new ArrayList<Statement>();
		statements.addAll(element.getOntModel().getBaseModel().listStatements(element, null, (RDFNode)null).toList());
		statements.addAll(element.getOntModel().getBaseModel().listStatements((Resource)null, null, element).toList());
	}

	@Override
	public void undo() {
		if (!statements.isEmpty())
			element.getOntModel().getBaseModel().add(statements);
	}

	@Override
	public void redo() {
		if (!statements.isEmpty())
			element.getOntModel().getBaseModel().remove(statements);
	}

}

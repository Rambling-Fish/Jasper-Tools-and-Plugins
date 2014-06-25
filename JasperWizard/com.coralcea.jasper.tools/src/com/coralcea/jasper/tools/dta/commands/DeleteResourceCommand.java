package com.coralcea.jasper.tools.dta.commands;

import java.util.List;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

public class DeleteResourceCommand extends DTACommand {

	private Resource element;
	private Model model;
	private List<Statement> statements;
	
	public DeleteResourceCommand(Resource element) {
		super("Deleting Element");
		this.element = element;
		this.model = element.getModel();
	}

	@Override
	public void prepare() {
    	statements = DTAUtilities.listAllStatementsOn(model, element);
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

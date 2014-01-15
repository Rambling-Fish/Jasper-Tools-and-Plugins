package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class RemovePropertyCommand extends DTACommand {

	private OntResource element;
	private Property property;
	private RDFNode value;
	
	public RemovePropertyCommand(OntResource element, Property property, RDFNode value) {
		super("Removing Property");
		this.element = element;
		this.property = property;
		this.value = value;
	}

	@Override
	public void store() {
	}

	@Override
	public void undo() {
		element.addProperty(property, value);	
	}

	@Override
	public void redo() {
		element.removeProperty(property, value);
	}

}

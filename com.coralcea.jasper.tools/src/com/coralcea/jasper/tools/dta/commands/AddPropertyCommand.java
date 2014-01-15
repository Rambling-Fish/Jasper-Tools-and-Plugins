package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class AddPropertyCommand extends DTACommand {

	private OntResource element;
	private Property property;
	private RDFNode value;
	
	public AddPropertyCommand(OntResource element, Property property, RDFNode value) {
		super("Adding Property");
		this.element = element;
		this.property = property;
		this.value = value;
	}

	@Override
	public void store() {
	}

	@Override
	public void undo() {
		element.removeProperty(property, value);	
	}

	@Override
	public void redo() {
		element.addProperty(property, value);	
	}

}

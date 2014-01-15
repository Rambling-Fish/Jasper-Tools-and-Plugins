package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;

public class SetPropertyCommand extends DTACommand {

	private OntResource element;
	private Property property;
	private RDFNode value;
	private RDFNode oldValue;
	
	public SetPropertyCommand(OntResource element, Property property, RDFNode value) {
		super("Setting Property");
		this.element = element;
		this.property = property;
		this.value = value;
	}

	@Override
	public void store() {
		oldValue = element.getPropertyValue(property);
	}

	@Override
	public void undo() {
		element.setPropertyValue(property, oldValue);	
	}

	@Override
	public void redo() {
		element.setPropertyValue(property, value);
	}

}

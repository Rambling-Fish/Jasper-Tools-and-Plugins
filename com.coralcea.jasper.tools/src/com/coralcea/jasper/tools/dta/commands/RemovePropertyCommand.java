package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class RemovePropertyCommand extends DTACommand {

	private Resource element;
	private Property property;
	private RDFNode value;
	
	public RemovePropertyCommand(Resource element, Property property, RDFNode value) {
		super("Removing Property");
		this.element = element;
		this.property = property;
		this.value = value;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void undo() {
		element.addProperty(property, value);	
	}

	@Override
	public void redo() {
		element.getModel().remove(element, property, value);
	}

}

package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

public class RenameResourceCommand extends DTACommand {

	private Model model;
	private String oldURI;
	private String newURI;
	
	public RenameResourceCommand(Resource element, Resource newElement) {
		super("Renaming Resource");
		this.model = element.getModel();
		this.oldURI = element.getURI();
		this.newURI = newElement.getURI();
	}

	@Override
	public void undo() {
		Resource old = model.getResource(newURI);
		ResourceUtils.renameResource(old, oldURI);
	}

	@Override
	public void redo() {
		Resource old = model.getResource(oldURI);
		ResourceUtils.renameResource(old, newURI);
	}

}

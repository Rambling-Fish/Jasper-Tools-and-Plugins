package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.ResourceUtils;

public class RenameResourceCommand extends DTACommand {

	private OntModel model;
	private String oldURI;
	private String newURI;
	
	public RenameResourceCommand(OntResource element, String newURI) {
		super("Renaming Resource");
		this.model = element.getOntModel();
		this.oldURI = element.getURI();
		this.newURI = newURI;
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

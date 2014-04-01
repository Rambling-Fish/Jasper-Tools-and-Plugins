package com.coralcea.jasper.tools.dta.commands;

import com.coralcea.jasper.tools.dta.DTACore;
import com.hp.hpl.jena.ontology.OntModel;

public class ChangeImportCommand extends DTACommand {

	private OntModel model;
	private String importedURI;
	private boolean load;
	
	public ChangeImportCommand(OntModel model, String importedURI, boolean load) {
		super(load ? "Loading" : "Unloading" + " Import");
		this.model = model;
		this.importedURI = importedURI;
		this.load = load;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void undo() {
		if (load) {
			DTACore.unloadImport(model, importedURI);
		} else
			DTACore.loadImport(model, importedURI);
	}

	@Override
	public void redo() {
		if (load)
			DTACore.loadImport(model, importedURI);
		else {
			DTACore.unloadImport(model, importedURI);
		}
	}

}

package com.coralcea.jasper.tools.dta.commands;

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
	public void store() {
	}

	@Override
	public void undo() {
		if (load) {
			model.getDocumentManager().unloadImport(model, importedURI);
			//Hack: to get the model to not forget the cached import
			model.getDocumentManager().getFileManager().removeCacheModel(importedURI);
			model.getSpecification().getImportModelMaker().removeModel(importedURI);
		} else
			model.getDocumentManager().loadImport(model, importedURI);
	}

	@Override
	public void redo() {
		if (load)
			model.getDocumentManager().loadImport(model, importedURI);
		else {
			model.getDocumentManager().unloadImport(model, importedURI);
			//Hack: to get the model to not forget the cached import
			model.getDocumentManager().getFileManager().removeCacheModel(importedURI);
			model.getSpecification().getImportModelMaker().removeModel(importedURI);
		}
	}

}

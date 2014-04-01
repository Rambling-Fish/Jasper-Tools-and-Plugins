package com.coralcea.jasper.tools.dta.commands;

import org.eclipse.core.resources.IFile;

import com.coralcea.jasper.tools.dta.DTACore;
import com.hp.hpl.jena.ontology.OntModel;

public class ChangeImportLoadCommand extends DTACommand {

	private IFile file;
	private OntModel model;
	private String importedURI;
	private boolean load;
	
	public ChangeImportLoadCommand(IFile file, OntModel model, String importedURI, boolean load) {
		super(load ? "Loading" : "Unloading" + " Import");
		this.file = file;
		this.model = model;
		this.importedURI = importedURI;
		this.load = load;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void undo() {
		if (load)
			model.getDocumentManager().unloadImport(model, importedURI);
		else
			model.getDocumentManager().loadImport(model, importedURI);
		DTACore.notifyListeners(file);
	}

	@Override
	public void redo() {
		if (load)
			model.getDocumentManager().loadImport(model, importedURI);
		else
			model.getDocumentManager().unloadImport(model, importedURI);
		DTACore.notifyListeners(file);
	}

}

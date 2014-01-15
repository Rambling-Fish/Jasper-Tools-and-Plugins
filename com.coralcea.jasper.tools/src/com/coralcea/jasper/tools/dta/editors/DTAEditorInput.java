package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.part.FileEditorInput;

import com.hp.hpl.jena.ontology.OntModel;

public class DTAEditorInput extends FileEditorInput {

	private OntModel model;
	
	public DTAEditorInput(IFile file, OntModel model) {
		super(file);
		this.model = model;
	}

	public OntModel getModel() {
		return model;
	}
	
}

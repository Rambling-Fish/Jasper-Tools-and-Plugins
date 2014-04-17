package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.hp.hpl.jena.ontology.OntModel;

public class DTAOwlImportWizardPage extends DTAImportWizardPage {
	
	public DTAOwlImportWizardPage(IStructuredSelection selection) {
		super(selection);
		setTitle("Import DTA library from OWL ontology");
		setDescription("Create a new DTA library based on an OWL ontology");
	}

	protected Label createFileLabel(Composite parent) {
		Label label = new Label(parent, SWT.NULL);
		label.setText("Ontology file:");
		label.setToolTipText("The path to the imported OWL ontology file");
		return label;
	}
	
	protected OntModel importFile(String path) {
		return new DTAOwlImporter().importFile(path);
	}
	
}
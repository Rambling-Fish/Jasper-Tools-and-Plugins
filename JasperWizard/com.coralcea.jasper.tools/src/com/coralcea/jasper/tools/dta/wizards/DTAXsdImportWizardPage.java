package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.hp.hpl.jena.ontology.OntModel;

public class DTAXsdImportWizardPage extends DTAImportWizardPage {
	
	public DTAXsdImportWizardPage(IStructuredSelection selection) {
		super(selection);
		setTitle("Import DTA library from XDE schema");
		setDescription("Create a new DTA library based on an XDE schema");
	}

	protected Label createFileLabel(Composite parent) {
		Label label = new Label(parent, SWT.NULL);
		label.setText("Schema file:");
		label.setToolTipText("The path to the imported XSD schema file");
		return label;
	}
	
	protected OntModel importFile(String path) {
		return new DTAXsdImporter().importFile(path);
	}
	
}
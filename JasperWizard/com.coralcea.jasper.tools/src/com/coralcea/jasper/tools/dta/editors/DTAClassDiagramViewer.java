package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;

import com.coralcea.jasper.tools.dta.diagrams.DTAUMLDiagramFactory;

public class DTAClassDiagramViewer extends DTADiagramViewer {

	public DTAClassDiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}

	protected Form createControl(Composite parent) {
		Form f = super.createControl(parent);
		f.setText("UML Diagram");
		return f;
	}

	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		viewer.setEditPartFactory(new DTAUMLDiagramFactory());
	}
	
}

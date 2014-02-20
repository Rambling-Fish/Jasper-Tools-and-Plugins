package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.swt.widgets.Composite;

import com.coralcea.jasper.tools.dta.diagrams.DTABrowseDiagramFactory;

public class DTABrowseDiagramViewer extends DTADiagramViewer {

	public DTABrowseDiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		getControl().setText("Browse Diagram");
	}

	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		viewer.setEditPartFactory(new DTABrowseDiagramFactory());
	}
	
}

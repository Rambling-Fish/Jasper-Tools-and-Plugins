package com.coralcea.jasper.tools.dta.commands;

import org.eclipse.jface.viewers.StructuredSelection;

import com.coralcea.jasper.tools.dta.editors.DTAViewer;
import com.hp.hpl.jena.rdf.model.Resource;

public class RefreshViewerCommand extends DTACommand {

	private DTAViewer viewer;
	private String newURI;
	private String oldURI;
	
	public RefreshViewerCommand(DTAViewer viewer, Resource newElement) {
		this.viewer = viewer;
		this.newURI = newElement.getURI();
		this.oldURI = viewer.getSelectedElement().getURI();
	}
	
	@Override
	public void undo() {
		if (newURI.equals(viewer.getSelectedElement().getURI())) {
			Resource r = viewer.getInput().getResource(oldURI);
			viewer.setSelection(new StructuredSelection(r));
		}
	}

	@Override
	public void redo() {
		if (oldURI.equals(viewer.getSelectedElement().getURI())) {
			Resource r = viewer.getInput().getResource(newURI);
			viewer.setSelection(new StructuredSelection(r));
		}
	}
}

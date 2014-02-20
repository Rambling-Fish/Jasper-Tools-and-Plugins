package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.AccessibleEditPart;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.Request;
import org.eclipse.swt.accessibility.AccessibleEvent;

import com.coralcea.jasper.tools.dta.editors.DTAEditor;
import com.hp.hpl.jena.ontology.OntResource;

public abstract class DTAResourceNodeEditPart extends DTANodeEditPart {

	private AccessibleEditPart acc;

	public DTAResourceNodeEditPart(OntResource resource) {
		setModel(resource);
	}
	
	protected OntResource getOntResource() {
		return (OntResource) getModel();
	}
	
	protected AccessibleEditPart createAccessible() {
		return new AccessibleGraphicalEditPart() {
			public void getName(AccessibleEvent e) {
				e.result = getOntResource().getURI();
			}
		};
	}

	@Override
	protected AccessibleEditPart getAccessibleEditPart() {
		if (acc == null)
			acc = createAccessible();
		return acc;
	}

	@Override
	public void performRequest(Request req) {
		DTAEditor editor = (DTAEditor) ((DefaultEditDomain)getViewer().getEditDomain()).getEditorPart();
		editor.setSelectedElement(getOntResource(), DTAEditor.PAGE_MODEL);
	}
	
}

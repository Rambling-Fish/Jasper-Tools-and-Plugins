package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.tools.DragEditPartsTracker;

import com.hp.hpl.jena.ontology.OntResource;

public abstract class DTAUMLNodeEditPart extends DTAResourceNodeEditPart {

	public DTAUMLNodeEditPart(OntResource resource) {
		super(resource);
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new DragEditPartsTracker(this) {
			protected void performDrag() {
				Command command = getCurrentCommand();
				command.execute();
			}
		};
	}
}

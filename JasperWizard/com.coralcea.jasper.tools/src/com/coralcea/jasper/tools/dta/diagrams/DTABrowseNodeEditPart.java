package com.coralcea.jasper.tools.dta.diagrams;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.DragTracker;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.tools.DragEditPartsTracker;

import com.coralcea.jasper.tools.dta.editors.DTABrowseDiagramViewer;
import com.coralcea.jasper.tools.dta.editors.DTADiagramViewer.DTAEditingDomain;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Statement;

public abstract class DTABrowseNodeEditPart extends DTAResourceNodeEditPart {

	public DTABrowseNodeEditPart(OntResource resource) {
		super(resource);
	}

	@Override
	protected List<Object> getModelSourceConnections() {
		return filterModelConnections(findModelSourceConnections());
	}

	@Override
	protected List<Object> getModelTargetConnections() {
		return filterModelConnections(findModelTargetConnections());
	}

	protected List<Object> findModelSourceConnections() {
		return Collections.emptyList();
	}

	protected List<Object> findModelTargetConnections() {
		return Collections.emptyList();
	}

	protected List<Object> filterModelConnections(List<Object> connections) {
		DTABrowseDiagramEditPart parent = (DTABrowseDiagramEditPart) getParent();
		for (Iterator<Object> i = connections.iterator(); i.hasNext();) {
			Statement s = (Statement) i.next();
			if (!parent.getModelConnections().contains(s))
				i.remove();
		}
		return connections;
	}

	@Override
	public DragTracker getDragTracker(Request request) {
		return new DragEditPartsTracker(this) {
			protected void performDrag() {
				Command command = getCurrentCommand();
				command.execute();
			}
			protected boolean handleDoubleClick(int button) {
				if (getCurrentInput().isControlKeyDown()) {
					DTAEditingDomain domain = (DTAEditingDomain) getSourceEditPart().getViewer().getEditDomain();
					DTABrowseDiagramViewer browseViewer = (DTABrowseDiagramViewer) domain.getDiagramViewer();
					browseViewer.focusOn(((DTABrowseNodeEditPart) getSourceEditPart()).getOntResource());
					return true;
				}
				return super.handleDoubleClick(button);
			}
		};
	}
}

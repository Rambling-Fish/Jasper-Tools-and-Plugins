package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.NavigationLocation;

import com.hp.hpl.jena.rdf.model.Resource;

public class DTANavigationLocation extends NavigationLocation {

	private int page = -1;
	private Resource element;
	
	protected DTANavigationLocation(IEditorPart editorPart, boolean initialize) {
		super(editorPart);
		if (initialize)
			update();
	}
	
	@Override
	public void saveState(IMemento memento) {
		memento.putInteger("Page", page);
		if (element != null)
			memento.putString("Element", element.getURI());
	}

	@Override
	public void restoreState(IMemento memento) {
		if (getEditorPart() instanceof DTAEditor) {
			DTAEditor editor = (DTAEditor) getEditorPart();
			page = memento.getInteger("Page");
			String uri = memento.getString("Element");
			if (uri != null)
				element = editor.getModel().getOntResource(uri);
		}
	}

	@Override
	public void restoreLocation() {
		if (getEditorPart() instanceof DTAEditor) {
			DTAEditor editor = (DTAEditor) getEditorPart();
			if (page != -1 && page != editor.getActivePage())
				editor.setActivePage(page);
			if (element != null)
				editor.setSelectedElement(element);
		}
	}

	@Override
	public boolean mergeInto(INavigationLocation currentLocation) {
		if (currentLocation instanceof DTANavigationLocation) {
			DTANavigationLocation current = (DTANavigationLocation) currentLocation;
			if (current.page == -1) {
				current.page = page;
				current.element = element;
				return true;
			} else if (page == current.page && ((element == null) || element.equals(current.element)))
				return true;
		}
		return false;
	}

	@Override
	public void update() {
		if (getEditorPart() instanceof DTAEditor) {
			DTAEditor editor = (DTAEditor) getEditorPart();
			page = editor.getActivePage();
			element = editor.getSelectedElement();
		}
	}
}

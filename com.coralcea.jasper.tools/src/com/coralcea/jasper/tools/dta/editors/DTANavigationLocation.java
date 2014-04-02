package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.NavigationLocation;

public class DTANavigationLocation extends NavigationLocation {

	private int page = -1;
	private String element;
	private String state;
	
	protected DTANavigationLocation(IEditorPart editorPart, boolean initialize) {
		super(editorPart);
		if (initialize)
			update();
	}
	
	@Override
	public void saveState(IMemento memento) {
		memento.putInteger("Page", page);
		if (element != null)
			memento.putString("Element", element);
		if (state != null)
			memento.putString("State", state);
	}

	@Override
	public void restoreState(IMemento memento) {
		if (getEditorPart() instanceof DTAEditor) {
			page = memento.getInteger("Page");
			element = memento.getString("Element");
			state = memento.getString("State");
		}
	}

	@Override
	public void restoreLocation() {
		if (getEditorPart() instanceof DTAEditor) {
			DTAEditor editor = (DTAEditor) getEditorPart();
			if (page != -1 && page != editor.getActivePage())
				editor.setActivePage(page);
			if (element != null)
				editor.setSelectedElement(editor.getModel().getOntResource(element));
			editor.setInternalState(state);
		}
	}

	@Override
	public boolean mergeInto(INavigationLocation currentLocation) {
		if (currentLocation instanceof DTANavigationLocation) {
			DTANavigationLocation current = (DTANavigationLocation) currentLocation;
			if (current.page == -1) {
				current.page = page;
				current.element = element;
				current.state = state;
				return true;
			} else if (page == current.page && 
					   ((element == null && current.element==null) || (element != null && element.equals(current.element))) &&
					   ((state == null) || state.equals(current.state)) || (state != null && state.equals(current.state)))
				return true;
		}
		return false;
	}

	@Override
	public void update() {
		if (getEditorPart() instanceof DTAEditor) {
			DTAEditor editor = (DTAEditor) getEditorPart();
			page = editor.getActivePage();
			if (editor.getSelectedElement() != null)
				element = editor.getSelectedElement().getURI();
			state = editor.getInternalState();
		}
	}
}

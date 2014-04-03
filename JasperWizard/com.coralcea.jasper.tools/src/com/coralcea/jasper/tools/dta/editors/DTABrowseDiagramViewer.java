package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.coralcea.jasper.tools.dta.diagrams.DTABrowseDiagramFactory;
import com.hp.hpl.jena.ontology.OntResource;

public class DTABrowseDiagramViewer extends DTADiagramViewer {

	public DTABrowseDiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}

	protected Form createControl(Composite parent) {
		Form f = super.createControl(parent);
		f.setText("Browse Diagram");
		return f;
	}
	
	public void activate() {
		if (viewer!= null)
			viewer.setProperty("filter", null);
		super.activate();
	}
	
	public void focusOn(OntResource resource) {
		viewer.setProperty("filter", resource.toString());
		getEditor().markLocation();
		refresh();
	}
	
	protected void setupActions(IToolBarManager manager) {
		super.setupActions(manager);

		DTADropDownAction action = new DTADropDownAction("Filter diagram");
		action.setToolTipText("Filter diagram");
		action.setImageDescriptor(Activator.getImageDescriptor(Images.FILTER));
		action.addSubAction(new Action("All") { 
			public void run() {
				viewer.setProperty("filter", null);
				getEditor().markLocation();
				refresh();
			}
		});
		action.addSubAction(new Action("Types") { 
			public void run() {
				viewer.setProperty("filter", "Types");
				getEditor().markLocation();
				refresh();
			}
		});
		action.addSubAction(new Action("Properties") { 
			public void run() {
				viewer.setProperty("filter", "Properties");
				getEditor().markLocation();
				refresh();
			}
		});
		action.addSubAction(new Action("Associations") { 
			public void run() {
				viewer.setProperty("filter", "Associations");
				getEditor().markLocation();
				refresh();
			}
		});
		action.addSubAction(new Action("Operations") { 
			public void run() {
				viewer.setProperty("filter", "Operations");
				getEditor().markLocation();
				refresh();
			}
		});
		
		manager.appendToGroup("Viewer", action);
	}

	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		viewer.setEditPartFactory(new DTABrowseDiagramFactory());
	}
	
	public String getInternalState() {
		return (String) viewer.getProperty("filter");
	}
	
	public void setInternalState(String state) {
		viewer.setProperty("filter", state);
		refresh();
	}
}

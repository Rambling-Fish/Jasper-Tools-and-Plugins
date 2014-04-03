package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.coralcea.jasper.tools.dta.diagrams.DTAUMLDiagramFactory;
import com.coralcea.jasper.tools.dta.diagrams.DTAGraphLayoutManager;

public class DTAClassDiagramViewer extends DTADiagramViewer {

	public DTAClassDiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}

	protected Form createControl(Composite parent) {
		Form f = super.createControl(parent);
		f.setText("UML Diagram");
		return f;
	}

	protected void setupActions(IToolBarManager manager) {
		super.setupActions(manager);

		IAction action;
		
		action = new Action("Arrange diagram") { //$NON-NLS-1$
			public void run() {
				Command c = DTAGraphLayoutManager.getLayoutCommand((GraphicalEditPart)viewer.getContents());
				getEditor().executeCommand(c, true);
			}
		};
		action.setToolTipText("Arrange diagram");
		action.setImageDescriptor(Activator.getImageDescriptor(Images.LAYOUT));
		manager.appendToGroup("Viewer", action);
	}

	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		viewer.setEditPartFactory(new DTAUMLDiagramFactory());
	}
	
}

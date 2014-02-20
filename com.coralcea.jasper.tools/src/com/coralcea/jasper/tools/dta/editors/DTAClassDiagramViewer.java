package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.coralcea.jasper.tools.dta.diagrams.DTAClassDiagramFactory;
import com.coralcea.jasper.tools.dta.diagrams.DTAGraphLayoutManager;

public class DTAClassDiagramViewer extends DTADiagramViewer {

	public DTAClassDiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		getControl().setText("UML Diagram");
	}

	protected void addActionsToToolBar(IToolBarManager manager) {
		super.addActionsToToolBar(manager);

		IAction action;
		
		action = new Action("Arrange diagram") { //$NON-NLS-1$
			public void run() {
				Command c = DTAGraphLayoutManager.getLayoutCommand((GraphicalEditPart)viewer.getContents());
				getEditor().executeCommand(c, true);
			}
		};
		action.setToolTipText("Arrange diagram");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.LAYOUT));
		manager.appendToGroup("Viewer", action);
	}

	protected void createGraphicalViewer(Composite parent) {
		super.createGraphicalViewer(parent);
		viewer.setEditPartFactory(new DTAClassDiagramFactory());
	}
	
}

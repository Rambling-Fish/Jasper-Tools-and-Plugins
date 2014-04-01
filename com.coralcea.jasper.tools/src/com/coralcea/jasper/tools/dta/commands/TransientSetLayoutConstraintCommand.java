package com.coralcea.jasper.tools.dta.commands;

import org.eclipse.gef.GraphicalEditPart;

public class TransientSetLayoutConstraintCommand extends DTACommand {

	private GraphicalEditPart container;
	private GraphicalEditPart child;
	private Object newConstraint;

	public TransientSetLayoutConstraintCommand(GraphicalEditPart container, GraphicalEditPart child, Object newConstraint) {
		super("Set Layout Constraint");
		this.container = container;
		this.child = child;
		this.newConstraint = newConstraint;
	}

	@Override
	public void redo() {
		container.setLayoutConstraint(child, child.getFigure(), newConstraint);
	}
}

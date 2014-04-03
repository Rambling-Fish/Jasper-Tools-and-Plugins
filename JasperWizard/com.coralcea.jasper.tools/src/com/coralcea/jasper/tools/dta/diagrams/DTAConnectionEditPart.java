package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.gef.AccessibleEditPart;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.accessibility.AccessibleEvent;

public abstract class DTAConnectionEditPart extends AbstractConnectionEditPart {

	private AccessibleEditPart acc;

	@Override
	protected void createEditPolicies() {
	}

	@Override
	protected IFigure createFigure() {
		PolylineConnection connx = new PolylineConnection();
		return connx;
	}

	protected ILabelProvider getLabelProvider() {
		return (ILabelProvider) getViewer().getProperty("LabelProvider");
	}
	
	protected AccessibleEditPart createAccessible() {
		return new AccessibleGraphicalEditPart() {
			public void getName(AccessibleEvent e) {
				e.result = getModel().toString();
			}
		};
	}

	@Override
	protected AccessibleEditPart getAccessibleEditPart() {
		if (acc == null)
			acc = createAccessible();
		return acc;
	}

}

/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.coralcea.jasper.tools.dta.diagrams;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.graphics.Image;

public class DTANodeLabel extends Figure implements DTANode {

	protected ChopboxAnchor anchor;
	protected Label icon;
	protected Label text;

	public DTANodeLabel() {
		setLayoutManager(new FlowLayout(true));
		add(icon = new Label());
		add(text = new Label());
		text.setBackgroundColor(ColorConstants.menuBackgroundSelected);
		text.setOpaque(true);
		anchor = new ChopboxAnchor(this);
	}

	public void setIcon(Image image) {
		icon.setIcon(image);
	}
	
	public void setText(String s) {
		text.setText(s);
	}

	public ConnectionAnchor getConnectionAnchor(String terminal) {
		return anchor;
	}

	public String getConnectionAnchorName(ConnectionAnchor c) {
		return "icon";
	}

	public ConnectionAnchor getSourceConnectionAnchorAt(Point p) {
		return anchor;
	}

	public ConnectionAnchor getTargetConnectionAnchorAt(Point p) {
		return anchor;
	}

}

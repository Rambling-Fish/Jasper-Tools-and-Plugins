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
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

public class DTANodeLabel extends Label implements DTANode {

	protected ChopboxAnchor anchor;

	public DTANodeLabel() {
		setLabelAlignment(PositionConstants.CENTER);
		anchor = new ChopboxAnchor(this) {
			protected Rectangle getBox() {
				return getIconBounds();
			}
		};
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

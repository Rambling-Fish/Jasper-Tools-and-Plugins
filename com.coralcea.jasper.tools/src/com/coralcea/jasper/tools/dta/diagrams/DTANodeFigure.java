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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.ChopboxAnchor;
import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.EllipseAnchor;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Point;

public class DTANodeFigure extends Figure {

	protected Map<String, ConnectionAnchor> connectionAnchors;
	protected List<ConnectionAnchor> inputConnectionAnchors;
	protected List<ConnectionAnchor> outputConnectionAnchors;

	public DTANodeFigure() {
		connectionAnchors = new HashMap<String, ConnectionAnchor>();
		connectionAnchors.put("Center", new ChopboxAnchor(this));
		connectionAnchors.put("CenterEllipse", new EllipseAnchor(this));
		inputConnectionAnchors = new ArrayList<ConnectionAnchor>();
		outputConnectionAnchors = new ArrayList<ConnectionAnchor>();
	}
	
	public ConnectionAnchor getConnectionAnchor(String terminal) {
		return (ConnectionAnchor) connectionAnchors.get(terminal);
	}

	public List<ConnectionAnchor> getSourceConnectionAnchors() {
		return outputConnectionAnchors;
	}

	public List<ConnectionAnchor> getTargetConnectionAnchors() {
		return inputConnectionAnchors;
	}

	public ConnectionAnchor connectionAnchorAt(Point p) {
		ConnectionAnchor closest = null;
		double min = Double.MAX_VALUE;

		for (ConnectionAnchor c : getSourceConnectionAnchors()) {
			Point p2 = c.getLocation(null);
			double d = p.getDistance(p2);
			if (d < min) {
				min = d;
				closest = c;
			}
		}
		for (ConnectionAnchor c : getTargetConnectionAnchors()) {
			Point p2 = c.getLocation(null);
			double d = p.getDistance(p2);
			if (d < min) {
				min = d;
				closest = c;
			}
		}
		return closest;
	}

	public String getConnectionAnchorName(ConnectionAnchor c) {
		for(String key : connectionAnchors.keySet()) {
			if (connectionAnchors.get(key).equals(c))
				return key;
		}
		return null;
	}

	public ConnectionAnchor getSourceConnectionAnchorAt(Point p) {
		ConnectionAnchor closest = null;
		double min = Double.MAX_VALUE;

		for (ConnectionAnchor c : getSourceConnectionAnchors()) {
			Point p2 = c.getLocation(null);
			double d = p.getDistance(p2);
			if (d < min) {
				min = d;
				closest = c;
			}
		}
		return closest;
	}

	public ConnectionAnchor getTargetConnectionAnchorAt(Point p) {
		ConnectionAnchor closest = null;
		double min = Double.MAX_VALUE;

		for (ConnectionAnchor c : getTargetConnectionAnchors()) {
			Point p2 = c.getLocation(null);
			double d = p.getDistance(p2);
			if (d < min) {
				min = d;
				closest = c;
			}
		}
		return closest;
	}

}

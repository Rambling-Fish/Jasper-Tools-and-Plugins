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

import org.eclipse.draw2d.ConnectionAnchor;
import org.eclipse.draw2d.geometry.Point;

public interface DTANode {

	String getConnectionAnchorName(ConnectionAnchor c);

	ConnectionAnchor getConnectionAnchor(String terminal);

	ConnectionAnchor getSourceConnectionAnchorAt(Point p);

	ConnectionAnchor getTargetConnectionAnchorAt(Point p);
}

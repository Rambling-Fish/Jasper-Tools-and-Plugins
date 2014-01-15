package com.coralcea.jasper.tools.dta.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;

public class DTATreeContentSorter extends ViewerSorter {

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		DTATreeData d1 = (DTATreeData) e1;
		DTATreeData d2 = (DTATreeData) e2;
		if (d1.getElement() == DTA.DTAs || d1.getElement() == DTA.Classes || d1.getElement() == DTA.Properties)
			return 1;
		return DTAUtilities.getLabel(d1.getElement()).compareTo(DTAUtilities.getLabel(d2.getElement()));
	}
}

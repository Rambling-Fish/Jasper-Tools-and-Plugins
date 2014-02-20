package com.coralcea.jasper.tools.dta.views;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.navigator.IDescriptionProvider;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTALabelProvider;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTATreeLabelProvider extends DTALabelProvider implements IStyledLabelProvider, IDescriptionProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) element;
			return super.getImage(data.getElement());
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) element;
			return super.getText(data.getElement());
		}  
		return null;
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString s = new StyledString(getText(element));
		int offset = s.getString().indexOf(" ");
		if (offset > -1) {
			s.setStyle(offset, s.length() - offset, StyledString.DECORATIONS_STYLER);
		}
		return s;
	}

	@Override
	public String getDescription(Object element) {
		if (element instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) element;
			Resource modelElement = (Resource) data.getElement();
			if (modelElement == DTA.DTAs || modelElement == DTA.Types || modelElement == DTA.Properties)
				return getDescription(data.getParent());
			else if (DTAUtilities.isOntology(modelElement))
				return "Model "+modelElement.getLocalName();
			else if (DTAUtilities.isProperty(modelElement))
				return "Property "+modelElement.getLocalName();
			else
				return DTAUtilities.getRDFType(modelElement).getLocalName()+" "+modelElement.getLocalName();
		}
		return null;
	}

}

package com.coralcea.jasper.tools.dta;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTALabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element == null)
			return null;
		Resource modelElement = (Resource) element;
		if (modelElement == DTA.DTAs || modelElement == DTA.Classes | modelElement == DTA.Properties)
			return Activator.getImage(JasperImages.PACKAGE);
		if (DTAUtilities.isOntology(modelElement))
			return Activator.getImage(JasperImages.MODEL);
		if (DTAUtilities.isClass(modelElement))
			return Activator.getImage(JasperImages.CLASS);
		if (DTAUtilities.isProperty(modelElement))
			return Activator.getImage(JasperImages.PROPERTY);
		if (DTAUtilities.isDTA(modelElement))
			return Activator.getImage(JasperImages.DTA);
		if (DTAUtilities.isOperation(modelElement))
			return Activator.getImage(JasperImages.OPERATION);
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element == null)
			return null;
		Resource modelElement = (Resource) element;
		String text = DTAUtilities.getLabel(modelElement);
		if (DTAUtilities.isProperty(modelElement)) {
			OntResource range = modelElement.as(OntProperty.class).getRange();
			text += (range != null) ? " : "+range.getLocalName() : "";
		} else if (DTAUtilities.isOperation(modelElement)) {
			Resource input = modelElement.getPropertyResourceValue(DTA.input);
			text += (input != null) ? " ("+input.getLocalName()+")" : "";
			Resource output = modelElement.getPropertyResourceValue(DTA.output);
			text += (output != null) ? " : "+output.getLocalName() : "";
		}
		return text;
	}

}
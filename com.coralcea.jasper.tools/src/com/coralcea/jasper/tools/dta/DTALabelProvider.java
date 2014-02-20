package com.coralcea.jasper.tools.dta;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTALabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element == null)
			return null;
		Resource modelElement = (Resource) element;
		if (modelElement == DTA.DTAs || modelElement == DTA.Types | modelElement == DTA.Properties)
			return Activator.getImage(JasperImages.PACKAGE);
		if (DTAUtilities.isOntology(modelElement))
			return (DTAUtilities.isDefinedByBase((Ontology)modelElement))
				? Activator.getImage(JasperImages.MODEL)
				: Activator.getImage(JasperImages.IMPORT);
		if (DTAUtilities.isClass(modelElement))
			return Activator.getImage(JasperImages.CLASS);
		if (DTAUtilities.isProperty(modelElement))
			return Activator.getImage(JasperImages.PROPERTY);
		if (DTAUtilities.isDTA(modelElement))
			return Activator.getImage(JasperImages.DTA);
		if (DTAUtilities.isOperation(modelElement))
			return Activator.getImage(JasperImages.OPERATION);
		if (DTAUtilities.isRequest(modelElement))
			return Activator.getImage(JasperImages.REQUEST);
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
			text += (range != null) ? " : "+ DTAUtilities.getLabel(range) : "";
		} else if (DTAUtilities.isOperation(modelElement) || DTAUtilities.isRequest(modelElement)) {
			String inputs = "";
			String outputs = "";
			for (RDFNode input : DTAUtilities.listObjects(modelElement, DTA.input)) {
				if (inputs.length()!=0)
					inputs += ", ";
				inputs += DTAUtilities.getLabel(input);
			}
			Resource output = modelElement.getPropertyResourceValue(DTA.output);
			if (output!=null)
				outputs = " "+DTAUtilities.getLabel(output);
			text += " ("+inputs+")"+outputs;
		}
		return text;
	}

}

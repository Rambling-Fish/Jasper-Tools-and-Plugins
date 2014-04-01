package com.coralcea.jasper.tools.dta;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTALabelProvider extends LabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element == null)
			return null;
		Resource modelElement = (Resource) element;
		if (modelElement == DTA.DTAs || modelElement == DTA.Types | modelElement == DTA.Properties)
			return Activator.getImage(Images.PACKAGE);
		if (DTAUtilities.isOntology(modelElement))
			return (DTAUtilities.isDefinedByBase(modelElement.as(Ontology.class)))
				? Activator.getImage(Images.MODEL)
				: Activator.getImage(Images.IMPORT);
		if (DTAUtilities.isClass(modelElement))
			return Activator.getImage(Images.CLASS);
		if (DTAUtilities.isProperty(modelElement))
			return Activator.getImage(Images.PROPERTY);
		if (DTAUtilities.isDTA(modelElement))
			return Activator.getImage(Images.DTA);
		if (DTAUtilities.isOperation(modelElement))
			return Activator.getImage(Images.OPERATION);
		if (DTAUtilities.isRequest(modelElement))
			return Activator.getImage(Images.REQUEST);
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
			Property input = DTAUtilities.getPropertyResourceValue(modelElement, DTA.input, Property.class);
			Property output = DTAUtilities.getPropertyResourceValue(modelElement, DTA.output, Property.class);
			OntClass outputType = (output != null) ? DTAUtilities.getPropertyResourceValue(output, RDFS.range, OntClass.class) : null;
			Restriction restriction = DTAUtilities.getDirectRestriction(modelElement, DTA.outputRestriction, output);

			String inputs = (input!=null) ? " ("+DTAUtilities.getLabel(input)+")" : " ()"; 
			String outputs = (outputType!=null) ? " : "+DTAUtilities.getLabel(outputType) : "";
			String cardinality = (output!=null) ? " ["+DTAUtilities.getCardinality(restriction)+"]" : "";
			
			text += inputs+outputs+cardinality;
		}
		return text;
	}

}

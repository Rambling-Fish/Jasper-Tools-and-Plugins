package com.coralcea.jasper.tools.dta;

import java.util.Iterator;

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
			Iterator<? extends OntResource> ranges = modelElement.as(OntProperty.class).listRange();
			if (ranges.hasNext()) {
				text += " : ";
				while (ranges.hasNext())
					text += DTAUtilities.getLabel(ranges.next()) + ", ";
				text = text.substring(0, text.length()-2);
			}
		} else if (DTAUtilities.isOperation(modelElement) || DTAUtilities.isRequest(modelElement)) {
			Property parameter = DTAUtilities.getPropertyResourceValue(modelElement, DTA.parameter, Property.class);
			Property data = DTAUtilities.getPropertyResourceValue(modelElement, DTA.data, Property.class);
			OntClass dataType = (data != null) ? DTAUtilities.getPropertyResourceValue(data, RDFS.range, OntClass.class) : null;
			Restriction restriction = DTAUtilities.getDirectRestriction(modelElement, DTA.dataRestriction, data);

			String parameterStr = (parameter!=null) ? " ("+DTAUtilities.getLabel(parameter)+")" : " ()"; 
			String dataStr = (dataType!=null) ? " : "+DTAUtilities.getLabel(dataType) : "";
			String cardinality = (data!=null) ? " "+DTAUtilities.getCardinality(restriction) : "";
			
			text += parameterStr+dataStr+cardinality;
		}
		return text;
	}

}

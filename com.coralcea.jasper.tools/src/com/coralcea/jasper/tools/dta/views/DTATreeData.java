package com.coralcea.jasper.tools.dta.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTATreeData {
	
	private Resource element;
	private Object parent;

	public static DTATreeData createRoot(OntModel model, Object parent) {
		return new DTATreeData(model.listOntologies().next(), parent);
	}

	private DTATreeData(Resource element, Object parent) {
		this.element = element;
		this.parent = parent;
	}

	public Resource getElement() {
		return element;
	}
	
	public Object getParent() {
		return parent;
	}

	public DTATreeData[] getChildren() {
		List<?> elements = getChildElements();
		if (elements != null && !elements.isEmpty()) {
			DTATreeData[] children = new DTATreeData[elements.size()];
			for(int i=0; i<elements.size(); i++)
				children[i] = new DTATreeData((Resource)elements.get(i), this);
			return children;
		}
		return null;
	}

	protected List<?> getChildElements() {
		if (element == DTA.DTAs) {
			Ontology ont = ((DTATreeData)getParent()).getElement().as(Ontology.class);
			return filter(ont.getOntModel().listIndividuals(DTA.DTA));
		} else if (element == DTA.Classes) {
			Ontology ont = ((DTATreeData)getParent()).getElement().as(Ontology.class);
			return filter(ont.getOntModel().listHierarchyRootClasses());
		} else if (element == DTA.Properties) {
			Ontology ont = ((DTATreeData)getParent()).getElement().as(Ontology.class);
			return filter(ont.getOntModel().listAllOntProperties());
		} 
		OntResource modelElement = element.as(OntResource.class);
		if (DTAUtilities.isDTA(modelElement)) {
			return filter(modelElement.listPropertyValues(DTA.operation));
		} else if (DTAUtilities.isClass(modelElement)) {
			return filter(modelElement.asClass().listSubClasses());
		} else if (DTAUtilities.isOntology(modelElement)) {
			ArrayList<Resource> groups = new ArrayList<Resource>();
			groups.add(DTA.DTAs);
			groups.add(DTA.Classes);
			groups.add(DTA.Properties);
			return groups;
		}
		return null;
	}

	private List<OntResource> filter(Iterator<?> ch) {
		ArrayList<OntResource> children = new ArrayList<OntResource>();
		while (ch.hasNext()) {
			OntResource r = (OntResource) ch.next();
			if (DTAUtilities.isDefinedByBase(r))
				children.add(r);
		}
		return children;
	}
	
	@Override
	public String toString() {
		return element.getURI();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != this && obj instanceof DTATreeData) {
			DTATreeData other = (DTATreeData) obj;
			if (element.getModel() == null && other.element.getModel() == null)
				return getParent().equals(other.getParent()) && other.element == element;
			return other.element == element;
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}
	

}
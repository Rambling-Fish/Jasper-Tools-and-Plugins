package com.coralcea.jasper.tools.dta.views;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTATreeData {
	
	private Resource element;
	private Object parent;

	public static DTATreeData[] createRoots(OntModel model, Object parent) {
		List<DTATreeData> roots = new ArrayList<DTATreeData>();
		for (Iterator<Ontology> i = model.listOntologies(); i.hasNext();)
			roots.add(new DTATreeData(i.next(), parent));
		return roots.toArray(new DTATreeData[0]);
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
		OntResource modelElement = (element.getModel()!=null) 
			? element.as(OntResource.class)
			: ((DTATreeData)getParent()).getElement().as(OntResource.class);

		if (element == DTA.DTAs) {
			Ontology ont = modelElement.as(Ontology.class);
			return DTAUtilities.listDefinedResources(ont, DTA.DTA).toList();
		} else if (element == DTA.Types) {
			Ontology ont = modelElement.as(Ontology.class);
			return DTAUtilities.listDefinedClasses(ont).toList();
		} else if (element == DTA.Properties) {
			Ontology ont = modelElement.as(Ontology.class);
			return DTAUtilities.listDefinedProperties(ont).toList();
		} else if (DTAUtilities.isOntology(modelElement)) {
			ArrayList<Resource> children = new ArrayList<Resource>();
			children.add(DTA.DTAs);
			children.add(DTA.Types);
			children.add(DTA.Properties);
			return children;
		} else if (DTAUtilities.isDTA(modelElement)) {
			ArrayList<RDFNode> children = new ArrayList<RDFNode>();
			children.addAll(DTAUtilities.listObjects(modelElement, DTA.operation));
			children.addAll(DTAUtilities.listObjects(modelElement, DTA.request));
			return children;
		}
		return null;
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
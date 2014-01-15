package com.coralcea.jasper.tools.dta;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

import org.apache.xerces.util.XMLChar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTAUtilities {

	public static String getLabel(RDFNode node) {
		if (node == null)
			return "";
		if (node.isLiteral())
			return node.asLiteral().getString();
		Resource resource = (Resource) node;
		String name = resource.getLocalName();
		String namespace = resource.getNameSpace();
		String prefix = resource.getModel() != null ? resource.getModel().getNsURIPrefix(namespace) : null;
		if (prefix != null) 
			return prefix.equals("") ? name : prefix+":"+name;
		if (XSD.getURI().equals(namespace))
			return "xsd:"+name;
		if (DTA.URI.equals(namespace))
			return name;
		return "<"+namespace+name+">";
	}

	public static Iterator<Individual> listDefinedIndividuals(Ontology ontology, Resource type) {
		return filterIfDefinedBy(getDefiningModel(ontology), ontology.getOntModel().listIndividuals(type));
	}

	public static Iterator<OntClass> listDefinedClasses(Ontology ontology) {
		return filterIfDefinedBy(getDefiningModel(ontology), ontology.getOntModel().listClasses());
	}

	public static Iterator<OntProperty> listDefinedProperties(Ontology ontology) {
		return filterIfDefinedBy(getDefiningModel(ontology), ontology.getOntModel().listAllOntProperties());
	}

	public static Set<OntProperty> getDeclaredProperties(Resource element) {
		HashSet<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (Resource p : getSubjects(RDFS.domain, element))
        	properties.add(p.as(OntProperty.class));
		return properties;
	}
	
	public static Set<OntProperty> getAllProperties(Resource element) {
		Set<OntProperty> properties = getDeclaredProperties(element);
        for (RDFNode supertype : getObjects(element, RDFS.subClassOf))
        	properties.addAll(getDeclaredProperties(supertype.as(OntClass.class)));
		return properties;
	}

	public static Set<OntClass> getAllSubClasses(Resource element) {
		HashSet<OntClass> subclasses = new LinkedHashSet<OntClass>();
	    for (ExtendedIterator<OntClass> i = element.as(OntClass.class).listSubClasses(); i.hasNext(); )
	    	subclasses.add(i.next().as(OntClass.class));
	    return subclasses;
	}

	public static Set<RDFNode> getObjects(Resource r, Property p) {
		Set<RDFNode> values = new LinkedHashSet<RDFNode>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject());
        return values;
    }

    public static Set<Resource> getSubjects(Property p, Resource r) {
    	LinkedHashSet<Resource> values = new LinkedHashSet<Resource>();
        for (ResIterator i = r.getModel().listSubjectsWithProperty(p, r); i.hasNext();)
        	values.add(i.next());
        return values;
    }
	
	public static <T extends Resource> Iterator<T> filterIfDefinedBy(Model model, Iterator<T> i) {
		ArrayList<T> children = new ArrayList<T>();
		while (i.hasNext()) {
			T r = (T) i.next();
			if (DTAUtilities.isDefinedBy(model, r))
				children.add(r);
		}
		return children.iterator();
	}

	public static <T extends RDFNode> Collection<T> sortOnLabel(Iterator<T> i) {
		TreeMap<String, T> map = new TreeMap<String, T>();
		while (i.hasNext()) {
			T element = i.next();
			String label = getLabel(element);
			map.put(label, element);
		}
		return map.values();
	}

	public static Model getDefiningModel(OntResource element) {
		Model model = element.getOntModel().getImportedModel(element.getURI());
		if (model == null)
			model = element.getOntModel().getBaseModel();
		return model;
	}
	
	public static boolean isDefinedBy(Model model, Resource r) {
		return model.contains(r, RDF.type);
	}

	public static boolean isDefinedByBase(OntResource r) {
		return isDefinedBy(r.getOntModel().getBaseModel(), r);
	}
	
	public static boolean isDefinedByBase(OntResource r, Property p, RDFNode v) {
		return r.getOntModel().getBaseModel().contains(r, p, v);
	}


	public static Collection<Resource> getAvailableTypes(OntModel model) {
		Collection<Resource> types = new ArrayList<Resource>(model.listClasses().toList());
		types.add(XSD.integer);
		types.add(XSD.decimal);
		types.add(XSD.xstring);
		types.add(XSD.xboolean);
		types.add(XSD.date);
		return types;
	}

	public static Resource getRDFType(Resource r) {
		return r.getModel() != null ? r.getPropertyResourceValue(RDF.type) : null;
	}
	
	public static boolean isValidURI(String uri) {
		try {
			URL url = new URL(uri);
			url.toURI();
			return XMLChar.isNCName(uri.charAt(uri.length()-1));
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isValidPrefix(String prefix) {
    	return prefix.length() == 0 || XMLChar.isValidNCName( prefix );
    }

	public static boolean isValidNsURI(String uri) {
    	return uri.length() == 0 || !XMLChar.isNCName(uri.charAt(uri.length()-1)); 
    }

	public static boolean hasNature(IProject project, String nature) {
		try {
			IProjectDescription desc = project.getDescription();
		    for (String natureId : desc.getNatureIds()) {
		    	if (natureId.equals(nature))
		    		return true;
		    }
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to get project description", e);
		}
	    return false;
	}

	public static boolean isOntology(Resource res) {
		Resource type = getRDFType(res);
		return OWL.Ontology.equals(type);
	}

	public static boolean isClass(Resource res) {
		Resource type = getRDFType(res);
		return OWL.Class.equals(type) || RDFS.Class.equals(type);
	}

	public static boolean isProperty(Resource res) {
		Resource type = getRDFType(res);
		return OWL.ObjectProperty.equals(type) || OWL.DatatypeProperty.equals(type);
	}

    public static boolean isOperation(Resource r) {
    	Resource type = DTAUtilities.getRDFType(r);
    	return DTA.Post.equals(type) || DTA.Receive.equals(type) || DTA.Request.equals(type) || DTA.Provide.equals(type);
    }

    public static boolean isDTA(Resource res) {
		Resource type = getRDFType(res);
		return DTA.DTA.equals(type);
	}
}

package com.coralcea.jasper.tools.dta;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.xerces.util.XMLChar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ResIteratorImpl;
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
		if (XSD.getURI().equals(resource.getNameSpace()))
			return "xsd:"+resource.getLocalName();
		if (resource.getModel()==null)
			return resource.getLocalName();
		if (resource.getModel().getNsURIPrefix(resource.getNameSpace()) != null)
			return resource.getModel().shortForm(resource.getURI());
		return "<"+resource.getURI()+">";
	}

	public static ResIterator listDefinedResources(Ontology ontology, Resource type) {
		return filterIfDefinedBy(getDefiningModel(ontology), ontology.getOntModel().listIndividuals(type));
	}

	public static ResIterator listDefinedClasses(Ontology ontology) {
		return filterIfDefinedBy(getDefiningModel(ontology), listClasses(ontology.getOntModel()).iterator());
	}

	public static ResIterator listDefinedProperties(Ontology ontology) {
		return filterIfDefinedBy(getDefiningModel(ontology), listProperties(ontology.getOntModel()).iterator());
	}

	public static List<OntClass> listClasses(OntModel model) {
		List<OntClass> classes = new ArrayList<OntClass>();
		for (Iterator<OntClass> i = model.listClasses(); i.hasNext();) {
			OntClass c = i.next();
			if (!c.isRestriction())
				classes.add(c);
		}
		return classes;
	}

	public static List<OntProperty> listProperties(OntModel model) {
		return model.listAllOntProperties().toList();
	}

	public static Set<OntProperty> getDeclaredProperties(Resource element) {
		HashSet<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (Resource p : listSubjects(RDFS.domain, element))
        	properties.add(p.as(OntProperty.class));
		return properties;
	}
	
	public static Set<OntProperty> getAllProperties(Resource element) {
		Set<OntProperty> properties = getDeclaredProperties(element);
        for (RDFNode supertype : listObjects(element, RDFS.subClassOf))
        	properties.addAll(getDeclaredProperties(supertype.as(OntClass.class)));
		return properties;
	}

	public static Set<OntProperty> getPropertiesTypedBy(Resource element) {
		HashSet<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (Resource p : listSubjects(RDFS.range, element))
        	properties.add(p.as(OntProperty.class));
		return properties;
	}

	public static Set<OntProperty> listInputs(Resource operation) {
		HashSet<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (RDFNode p : listObjects(operation, DTA.input))
        	properties.add(p.as(OntProperty.class));
		return properties;
	}

	public static OntProperty getOutput(Resource operation) {
		Resource output = operation.getPropertyResourceValue(DTA.output);
		return (output!=null) ? output.as(OntProperty.class) : null;
	}

	public static Set<OntClass> getSelfAndAllSubClasses(Resource element) {
		Set<OntClass> subclasses = element.as(OntClass.class).listSubClasses().toSet();
		subclasses.add(element.as(OntClass.class));
	    return subclasses;
	}

	public static Set<RDFNode> listObjects(Resource r, Property p) {
		Set<RDFNode> values = new LinkedHashSet<RDFNode>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject());
        return values;
    }

	public static Set<Resource> listResourceObjects(Resource r, Property p) {
		Set<Resource> values = new LinkedHashSet<Resource>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject().asResource());
        return values;
    }

	public static Set<OntResource> listOntResourceObjects(Resource r, Property p) {
		Set<OntResource> values = new LinkedHashSet<OntResource>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject().as(OntResource.class));
        return values;
    }

	public static Set<Resource> listSubjects(Property p, Resource r) {
    	LinkedHashSet<Resource> values = new LinkedHashSet<Resource>();
        for (ResIterator i = r.getModel().listResourcesWithProperty(p, r); i.hasNext();)
        	values.add(i.next());
        return values;
    }
	
	public static Set<OntResource> listOntResourceSubjects(Property p, Resource r) {
    	LinkedHashSet<OntResource> values = new LinkedHashSet<OntResource>();
        for (ResIterator i = r.getModel().listResourcesWithProperty(p, r); i.hasNext();)
        	values.add(i.next().as(OntResource.class));
        return values;
    }

	public static Resource getDTA(Resource operation) {
        for (Resource subject : DTAUtilities.listSubjects(null, operation)) {
        	if (isDTA(subject))
        		return subject;
        }
        return null;
    }

    public static <T extends Resource> ResIterator filterIfDefinedBy(OntModel model, Iterator<T> i) {
		ArrayList<Resource> children = new ArrayList<Resource>();
		while (i.hasNext()) {
			T r = (T) i.next();
			if (DTAUtilities.isDefinedBy(model, r))
				children.add(r);
		}
		return new ResIteratorImpl(children.iterator());
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

	public static OntModel getDefiningModel(Ontology element) {
		OntModel model = element.getOntModel().getImportedModel(element.getURI());
		if (model == null)
			model = element.getOntModel();
		return model;
	}
	
	public static Ontology getDefiningOntology(OntResource element) {
		OntModel model = element.getOntModel();
		if (isDefinedBy(model, element))
			return model.listOntologies().next();
		
		Iterator<OntModel> imports = element.getOntModel().listSubModels();
		while (imports.hasNext()) {
			OntModel importedModel = imports.next();
			if (isDefinedBy(importedModel, element)) {
				Ontology o = importedModel.listOntologies().next();
				return model.getOntology(o.getURI());
			}
		}
		
		return null;
	}

	public static boolean isDefinedBy(OntModel model, Resource r) {
		return model.getBaseModel().contains(r, RDF.type);
	}

	public static boolean isDefinedByBase(OntResource r) {
		return isDefinedBy(r.getOntModel(), r);
	}
	
	public static boolean isDefinedByBase(OntResource r, Property p, RDFNode v) {
		return r.getOntModel().getBaseModel().contains(r, p, v);
	}


	public static Collection<Resource> getAvailableTypes(OntModel model) {
		Collection<Resource> types = new ArrayList<Resource>(DTAUtilities.listClasses(model));
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
	
	public static boolean isValidURL(String url) {
		try {
			new URL(url);
			return true;
		} catch (Exception e) {
			return false;
		}
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
    	return prefix.length() == 0 || (XMLChar.isValidNCName(prefix) && !prefix.equalsIgnoreCase("base"));
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

	public static boolean isLibrary(Ontology res) {
		Literal lib = (Literal) res.getPropertyValue(DTA.isLibrary);
		return lib!= null && lib.getBoolean();
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
    	return DTA.Operation.equals(DTAUtilities.getRDFType(r));
    }

    public static boolean isRequest(Resource r) {
    	return DTA.Request.equals(DTAUtilities.getRDFType(r));
    }

    public static boolean isDTA(Resource res) {
		Resource type = getRDFType(res);
		return DTA.DTA.equals(type);
	}
    
    public static String getUniqueDestination(Resource dta, Resource op) {
		String s = dta.getLocalName()+"/"+op.getLocalName()+"/"+UUID.randomUUID().toString();
		return s.toLowerCase();
    }
    
    public static String getStringValue(Resource r, Property p) {
    	Statement s = r.getProperty(p);
    	return s!=null ? s.getObject().asLiteral().getString() : "";
    }

    public static int getIntegerValue(Resource r, Property p) {
    	Statement s = r.getProperty(p);
    	return s!=null ? s.getObject().asLiteral().getInt() : 0;
    }
    
    public static List<Statement> getStatementsOn(Model model, Resource resource) {
    	List<Statement> statements = new ArrayList<Statement>();
		statements.addAll(model.listStatements(resource, null, (RDFNode)null).toList());
		statements.addAll(model.listStatements((Resource)null, null, resource).toList());
		return statements;
    }
    
    public static Restriction getRestriction(Resource resource, Property kind, Property property) {
		for (RDFNode n : DTAUtilities.listObjects(resource, kind)) {
			if (n.canAs(Restriction.class)) {
				Restriction r = n.as(Restriction.class);
				if (r.getOnProperty().equals(property))
					return r;
			}
		}
    	return null;
    }

    public static String getRestrictionValue(Restriction restriction) {
	    String value = "1..1";
	    if (restriction == null)
	    	value = "0..*";
	    else if (restriction.isMaxCardinalityRestriction())
	    	value = "0..1";
	    else if (restriction.isMinCardinalityRestriction())
	    	value = "1..*";
    	return value;
    }
    
    public static boolean isMultiValued(Resource aClass, Property aProperty, Property kind) {
    	Restriction restriction = getRestriction(aClass, kind, aProperty);
    	return restriction==null || restriction.isMinCardinalityRestriction();
    }
}

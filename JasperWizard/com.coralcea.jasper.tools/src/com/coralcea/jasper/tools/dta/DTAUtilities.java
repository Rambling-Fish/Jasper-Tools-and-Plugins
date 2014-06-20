package com.coralcea.jasper.tools.dta;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.xerces.util.XMLChar;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
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
import com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTAUtilities {
	
	private static final Resource[] primitives = new Resource[] {XSD.integer, XSD.decimal, XSD.xstring, XSD.xboolean, XSD.duration, XSD.time, XSD.date, XSD.dateTime, XSD.hexBinary, RDFS.Literal};
	private static final Set<Resource> primitiveSet = new HashSet<Resource>(Arrays.asList(primitives));


	public static String getLabel(RDFNode node) {
		if (node == null)
			return "";
		if (node.isLiteral())
			return node.asLiteral().getString();
		Resource resource = (Resource) node;
		if (XSD.getURI().equals(resource.getNameSpace()))
			return "xsd:"+resource.getLocalName();
		if (RDFS.Literal.equals(resource))
			return "rdfs:Literal";
		if (resource.getModel()==null)
			return resource.getLocalName();
		if (resource.getModel().getNsURIPrefix(resource.getNameSpace()) != null)
			return resource.getModel().shortForm(resource.getURI());
		return "<"+resource.getURI()+">";
	}

	public static ResIterator listDefinedResources(Ontology ontology, Resource type) {
		return filterIfDefinedByBase(getDefiningModel(ontology), ontology.getOntModel().listIndividuals(type));
	}

	public static ResIterator listDefinedClasses(Ontology ontology) {
		return filterIfDefinedByBase(getDefiningModel(ontology), listClasses(ontology.getOntModel()).iterator());
	}

	public static ResIterator listDefinedProperties(Ontology ontology) {
		return filterIfDefinedByBase(getDefiningModel(ontology), listProperties(ontology.getOntModel()).iterator());
	}

	public static List<OntClass> listClasses(OntModel model) {
		List<OntClass> classes = new ArrayList<OntClass>();
		for (Iterator<OntClass> i = model.listClasses(); i.hasNext();) {
			OntClass c = i.next();
			if (!c.isAnon())
				classes.add(c);
		}
		return classes;
	}

	public static List<OntProperty> listProperties(OntModel model) {
		List<OntProperty> properties = new ArrayList<OntProperty>();
		for (Iterator<OntProperty> i = model.listAllOntProperties(); i.hasNext();) {
			OntProperty p = i.next();
			if (isProperty(p))
				properties.add(p);
		}
		return properties;
	}

	public static List<Resource> listResourcesOfType(OntModel model, Resource type) {
		return model.listResourcesWithProperty(RDF.type, type).toList();
	}

	public static Set<OntProperty> listDeclaredProperties(Resource type) {
		Set<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (Resource p : listSubjects(RDFS.domain, type))
        	if (isProperty(p))
        		properties.add(p.as(OntProperty.class));
		return properties;
	}
	
	public static Set<OntProperty> listAllProperties(Resource type) {
		Set<OntProperty> properties = listDeclaredProperties(type);
        for (RDFNode supertype : listObjects(type, RDFS.subClassOf))
        	properties.addAll(listDeclaredProperties(supertype.as(OntClass.class)));
		return properties;
	}

	public static Set<OntProperty> listPropertiesTypedBy(Resource type) {
		Set<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (Resource p : listSubjects(RDFS.range, type))
        	if (isProperty(p))
        		properties.add(p.as(OntProperty.class));
		return properties;
	}

	public static OntClass getParameter(Resource operation) {
		Resource parameter = operation.getPropertyResourceValue(DTA.parameter);
		return (parameter!=null) ? parameter.as(OntClass.class) : null;
	}

	public static OntProperty getData(Resource operation) {
		Resource data = operation.getPropertyResourceValue(DTA.data);
		return (data!=null) ? data.as(OntProperty.class) : null;
	}

	public static List<OntClass> listSelfAndAllSubClasses(Resource type) {
		List<OntClass> subclasses = new ArrayList<OntClass>();
		subclasses.add(type.as(OntClass.class));
		for (Iterator<OntClass> i = type.as(OntClass.class).listSubClasses(true); i.hasNext();)
			subclasses.addAll(listSelfAndAllSubClasses(i.next()));
	    return subclasses;
	}

	public static List<OntClass> listSelfAndAllSuperClasses(Resource type) {
		List<OntClass> superclasses = new ArrayList<OntClass>();
		superclasses.add(type.as(OntClass.class));
		for (Iterator<OntClass> i = type.as(OntClass.class).listSuperClasses(true); i.hasNext();)
			superclasses.addAll(listSelfAndAllSuperClasses(i.next()));
	    return superclasses;
	}

	public static <T extends RDFNode> Set<T> listObjects(Resource r, Property p, Class<T> aClass) {
		Set<T> values = new LinkedHashSet<T>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject().as(aClass));
        return values;
    }

	public static Set<RDFNode> listObjects(Resource r, Property p) {
		Set<RDFNode> values = new LinkedHashSet<RDFNode>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject());
        return values;
    }

	public static <T extends Resource> Set<T> listSubjects(Property p, Resource r, Class<T> aClass) {
    	LinkedHashSet<T> values = new LinkedHashSet<T>();
        for (ResIterator i = r.getModel().listResourcesWithProperty(p, r); i.hasNext();)
        	values.add(i.next().as(aClass));
        return values;
    }
	
	public static Set<Resource> listSubjects(Property p, Resource r) {
    	LinkedHashSet<Resource> values = new LinkedHashSet<Resource>();
        for (ResIterator i = r.getModel().listResourcesWithProperty(p, r); i.hasNext();)
        	values.add(i.next());
        return values;
    }
	
	public static <T extends Resource> T getPropertyResourceValue(Resource r, Property p, Class<T> aClass) {
		Resource v = r.getPropertyResourceValue(p);
		return (v!=null) ? v.as(aClass) : null;
	}

	public static StmtIterator listStatementsOfPredicates(Model model, Property[] predicates) {
		List<Statement> statements = new ArrayList<Statement>();
		for (Property p : predicates)
			statements.addAll(model.listStatements(null, p, (RDFNode)null).toList());
		return new StmtIteratorImpl(statements.iterator());
	}
	
	public static StmtIterator listStatementsOfTypes(Model model, Resource[] types) {
		List<Statement> statements = new ArrayList<Statement>();
		for (Resource t : types)
			statements.addAll(model.listStatements(null, RDF.type, t).toList());
		return new StmtIteratorImpl(statements.iterator());
	}

	public static Resource getDTA(Resource operation) {
        for (Resource subject : DTAUtilities.listSubjects(null, operation, Resource.class)) {
        	if (isDTA(subject))
        		return subject;
        }
        return null;
    }

    public static <T extends Resource> ResIterator filterIfDefinedByBase(OntModel model, Iterator<T> i) {
		ArrayList<Resource> children = new ArrayList<Resource>();
		while (i.hasNext()) {
			T r = (T) i.next();
			if (DTAUtilities.isDefinedByBase(model, r))
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
	
	public static Ontology getDefiningOntology(OntResource resource) {
		OntModel model = resource.getOntModel();
		if (isDefinedByBase(model, resource))
			return model.listOntologies().next();
		
		Iterator<OntModel> imports = resource.getOntModel().listSubModels();
		while (imports.hasNext()) {
			OntModel importedModel = imports.next();
			if (isDefinedByBase(importedModel, resource)) {
				Ontology o = importedModel.listOntologies().next();
				return model.getOntology(o.getURI());
			}
		}
		
		return null;
	}

	public static boolean isDefinedByBase(OntModel model, Resource r) {
		return model.getBaseModel().contains(r, RDF.type);
	}

	public static boolean isDefinedByBase(OntResource r) {
		return isDefinedByBase(r.getOntModel(), r);
	}
	
	public static boolean isDefinedByBase(OntResource r, Property p, RDFNode v) {
		return r.getOntModel().getBaseModel().contains(r, p, v);
	}

	public static boolean isDefinedByBase(OntModel m, Resource r, Property p, RDFNode v) {
		return m.getBaseModel().contains(r, p, v);
	}

	public static Collection<Resource> listAvailableTypes(OntModel model) {
		Collection<Resource> types = new ArrayList<Resource>(DTAUtilities.listClasses(model));
		types.add(RDFS.Resource);
		types.addAll(primitiveSet);
		return types;
	}

	public static Set<Resource> listRDFTypes(Resource r) {
		if (r.getModel() == null)
			return Collections.emptySet();
		return listObjects(r, RDF.type, Resource.class);
	}
	
	public static boolean isTypedBy(Resource r, Resource[] types) {
		Set<Resource> actualTypes = listRDFTypes(r);
		for (Resource type : types) 
			if (actualTypes.contains(type))
				return true;
		return false;
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
			new URI(uri);
			return XMLChar.isNCName(uri.charAt(uri.length()-1));
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isValidFile(String file) {
		try {
			return new File(file).exists();
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isValidPrefix(String prefix) {
    	return XMLChar.isValidNCName(prefix) && !prefix.equalsIgnoreCase("base");
    }

	public static boolean isValidNsURI(String uri) {
		try {
			new URI(uri);
			return !XMLChar.isNCName(uri.charAt(uri.length()-1));
		} catch (Exception e) {
			return false;
		}
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

	public static boolean isOntology(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.Ontology);
	}

	public static boolean isLibrary(Ontology resource) {
		Literal lib = (Literal) resource.getPropertyValue(DTA.isLibrary);
		return lib!= null && lib.getBoolean();
	}
	
	public static boolean isClass(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.Class) || 
			   types.contains(RDFS.Class);
	}

    public static boolean isDatatype(Resource resource) {
    	return XSD.getURI().equals(resource.getNameSpace()) || 
     		   RDFS.Literal.equals(resource) ||
     		   resource.getModel().contains(resource, RDF.type, RDFS.Datatype);
	}

    public static boolean isSupportedDatatype(Resource resource) {
    	return primitiveSet.contains(resource);
	}

    public static boolean isCardinality(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.minCardinality) || 
			   types.contains(OWL.maxCardinality) || 
			   types.contains(OWL.cardinality);
	}

	public static boolean isProperty(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.ObjectProperty) || 
			   types.contains(OWL.DatatypeProperty) || 
			   types.contains(RDF.Property);
	}

	public static boolean isDatatypeProperty(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.DatatypeProperty);
	}

	public static boolean isObjectProperty(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
		return types.contains(OWL.ObjectProperty);
	}

	public static boolean isOperation(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
    	return types.contains(DTA.Operation);
    }

    public static boolean isRequest(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
    	return types.contains(DTA.Request);
    }

    public static boolean isDTA(Resource resource) {
		Set<Resource> types = listRDFTypes(resource);
    	return types.contains(DTA.DTA);
	}
    
    public static String getKind(Resource resource) {
    	if (listRDFTypes(resource).isEmpty())
    		return resource.getLocalName();
		if (isOntology(resource))
    		return isLibrary((Ontology)resource) ? "Library" : "Model";
    	if (isProperty(resource))
    		return "Property";
    	if (isClass(resource))
    		return "Type";
    	return DTAUtilities.listRDFTypes(resource).iterator().next().getLocalName();
    }
    
    public static String getUniqueDestination(Resource dta, Resource op) {
		return dta.getLocalName()+"/"+op.getLocalName()+"/"+UUID.randomUUID().toString();
    }
    
    public static String getStringValue(Resource r, Property p) {
    	Statement s = r.getProperty(p);
    	return s!=null ? s.getObject().asLiteral().getString() : "";
    }

    public static int getIntegerValue(Resource r, Property p) {
    	Statement s = r.getProperty(p);
    	return s!=null ? s.getObject().asLiteral().getInt() : 0;
    }
    
    public static List<Statement> listDirectStatementsOn(Model model, Resource resource) {
    	List<Statement> statements = new ArrayList<Statement>();
		statements.addAll(model.listStatements(resource, null, (RDFNode)null).toList());
		statements.addAll(model.listStatements((Resource)null, null, resource).toList());
		return statements;
    }
    
    public static List<Statement> listAllStatementsOn(Model model, Resource resource) {
    	List<Statement> statements = new ArrayList<Statement>();

		for(StmtIterator i = model.listStatements(resource, null, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			statements.add(s);
			if (s.getObject().isAnon())
				statements.addAll(listAllStatementsOn(model, s.getObject().asResource()));
		}
		
		for(StmtIterator i = model.listStatements(null, null, resource); i.hasNext();) {
			Statement s = i.next();
			statements.add(s);
			if (OWL.onProperty.equals(s.getPredicate()))
				statements.addAll(listAllStatementsOn(model, s.getSubject()));
		}

		if (resource.canAs(Property.class))
			statements.addAll(model.listStatements(null, resource.as(Property.class), (RDFNode)null).toList());

		return statements;
    }

    public static Restriction getDirectRestriction(Resource resource, Property kind, Property property) {
		for (RDFNode n : DTAUtilities.listObjects(resource, kind)) {
			if (n.canAs(Restriction.class)) {
				Restriction r = n.as(Restriction.class);
				if (r.getOnProperty().equals(property))
					return r;
			}
		}
		return null;
    }
 
    public static Restriction getIndirectRestriction(Resource resource, Property kind, Property property) {
    	Restriction r = null;
    	for (OntClass superClass : DTAUtilities.listObjects(resource, RDFS.subClassOf, OntClass.class)) {
			r = getRestriction(superClass, kind, property);
			if (r!=null)
				break;
		}
    	return r;
    }

    public static Restriction getRestriction(Resource resource, Property kind, Property property) {
		Restriction r = getDirectRestriction(resource, kind, property);
		if (r==null)
			r = getIndirectRestriction(resource, kind, property);
    	return r;
    }

    public static String getCardinality(Restriction restriction) {
    	if (restriction == null)
    		return DTACardinality.ZERO_STAR;
	    if (restriction.isCardinalityRestriction())
	    	return DTACardinality.ONE_ONE;
	    else if (restriction.isMaxCardinalityRestriction())
	    	return DTACardinality.ZERO_ONE;
	    else if (restriction.isMinCardinalityRestriction())
	    	return DTACardinality.ONE_STAR;
	    return null;
    }
    
    public static Resource getRestrictedType(Restriction restriction) {
    	if (restriction!=null && restriction.isAllValuesFromRestriction())
    		return restriction.asAllValuesFromRestriction().getAllValuesFrom();
    	return null;
    }

    public static boolean isGet(Resource operation) {
    	return DTA.Get.equals(operation.getPropertyResourceValue(DTA.kind)); 
    }

    public static boolean isPost(Resource operation) {
    	return DTA.Post.equals(operation.getPropertyResourceValue(DTA.kind)); 
    }
    
    public static boolean isPublish(Resource operation) {
    	return DTA.Publish.equals(operation.getPropertyResourceValue(DTA.kind)); 
    }

    public static boolean isSubscribe(Resource request) {
    	return DTA.Subscribe.equals(request.getPropertyResourceValue(DTA.kind)); 
    }

    public static boolean hasDestination(Resource r) {
    	return (isOperation(r) && !isPublish(r)) || isSubscribe(r);
    }

    public static Set<Property> listAllEquivalentProperties(Property p) {
    	Set<Property> equivalents = new LinkedHashSet<Property>();
    	equivalents.addAll(DTAUtilities.listObjects(p, OWL.equivalentProperty, Property.class));
    	equivalents.addAll(DTAUtilities.listSubjects(OWL.equivalentProperty, p, Property.class));
    	return equivalents;
    }
    
    public static IMarker[] getMarkers(IFile modelFile, Resource resource) throws CoreException {
		List<IMarker> markers = new ArrayList<IMarker>();
    	for (IMarker marker : modelFile.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO)) {
			if (resource.getURI().equals(marker.getAttribute(IMarker.LOCATION))) {
				markers.add(marker);
			}
		}
    	return markers.toArray(new IMarker[0]);
    }
    
}

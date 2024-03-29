package com.coralcea.jasper.connector;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.datatype.Duration;

import org.apache.log4j.Logger;
import org.mule.api.lifecycle.Callable;
import org.mule.util.ClassUtils;

import com.hp.hpl.jena.ontology.MinCardinalityRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class JasperMetadata extends JasperConstants {

	protected Logger logger = Logger.getLogger(getClass());

	private OntModel model;
	
	public JasperMetadata(String metadataPath) throws FileNotFoundException {
		File file = new File(metadataPath);
		if (!file.exists())
			throw new FileNotFoundException(metadataPath);

		FileManager fileManager = new FileManager(LocationMapper.get()) {
			public Model readModel(Model model, String filenameOrURI) {
		        String mappedURI = mapURI(filenameOrURI) ;
		        if (mappedURI!=null && mappedURI.endsWith(DTA_EXTENSION))
		        	return readModel(model, filenameOrURI, DTA_FORMAT);
		        return super.readModel(model, filenameOrURI);
			}
		};

		fileManager.addLocatorFile(file.getParent());
		Path path = Paths.get(file.getParent());

		OntDocumentManager dm = new OntDocumentManager(fileManager, path.resolve(DTA_IMPORT_POLICY).toString());
		dm.setReadFailureHandler(new OntDocumentManager.ReadFailureHandler() {
			public void handleFailedRead(String uri, Model model, Exception e) {
				logger.error("Could not load the imported model <"+uri+"> : " + e);
			}
		});
		
		OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
		spec.setDocumentManager(dm);
		
		FileInputStream fis = new FileInputStream(file);
		model = ModelFactory.createOntologyModel(spec);
		model.read(fis, null, DTA_FORMAT);
	}
	
	public String serializeRelevantModelSubset() {
		Model outModel = getRelevantModelSubset(model);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		RDFWriter writer = outModel.getWriter(DTA_FORMAT);
		writer.write(outModel, out, "");
		return out.toString();
	}
	
	private Model getRelevantModelSubset(OntModel model) {
		Model subsetModel = ModelFactory.createDefaultModel();
		subsetModel.setNsPrefixes(model.getNsPrefixMap());
		List<Statement> statementsToRemove = new ArrayList<Statement>();

		// Add all statements in the base model
		subsetModel.add(model.getBaseModel().listStatements());
		
		// Remove ontology statements
		for(ResIterator i = subsetModel.listSubjectsWithProperty(RDF.type, OWL.Ontology); i.hasNext();)
			statementsToRemove.addAll(i.next().listProperties().toSet());

		// Remove request statements
		for(StmtIterator i = subsetModel.listStatements(null, DTA_request, (RDFNode)null); i.hasNext();) {
			Statement s1 = i.next();
			Resource request = s1.getObject().asResource();
			statementsToRemove.add(s1);
			for(StmtIterator j = request.listProperties(); j.hasNext();) {
				Statement s2 = j.next();
				statementsToRemove.add(s2);
				if (s2.getObject().isAnon())
					statementsToRemove.addAll(s2.getObject().asResource().listProperties().toSet());
			}
		}
		
		// Add relevant dependent resources statements
		for (Resource resource : collectRelevantDependecies(model))
			subsetModel.add(model.listStatements(resource, null, (RDFNode)null));
		
		// Remove notation statements
		statementsToRemove.addAll(subsetModel.listStatements(null, DTA_x, (RDFNode)null).toSet());
		statementsToRemove.addAll(subsetModel.listStatements(null, DTA_y, (RDFNode)null).toSet());
		
		subsetModel.remove(statementsToRemove);
		
		return subsetModel;
	}
	
	private static Set<Resource> collectRelevantDependecies(OntModel model) {
		Set<Resource> resources = new HashSet<Resource>();
		Set<Resource> localResources = new HashSet<Resource>();
		
		for(ResIterator i = model.getBaseModel().listSubjectsWithProperty(RDF.type, DTA_DTA); i.hasNext();) {
			Resource dta = model.getResource(i.next().getURI());
			for(Resource operation : listObjects(dta, DTA_operation, Resource.class)) {
		        if (operation.hasProperty(DTA_parameter))
					collectType(operation.getPropertyResourceValue(DTA_parameter), resources);
				if (operation.hasProperty(DTA_data))
		        	collectProperty(operation.getPropertyResourceValue(DTA_data), resources);
			}
		}
		
		for(ResIterator i = model.getBaseModel().listSubjectsWithProperty(RDF.type, OWL.Class); i.hasNext();) {
        	Resource type = model.getResource(i.next().getURI());
        	localResources.add(type);
			collectType(type, resources);
		}
		
		for(ResIterator i = model.getBaseModel().listSubjectsWithProperty(RDF.type, OWL.ObjectProperty); i.hasNext();) {
        	Resource property = model.getResource(i.next().getURI());
        	localResources.add(property);
        	collectProperty(property, resources);
		}

		for(ResIterator i = model.getBaseModel().listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty); i.hasNext();) {
        	Resource property = model.getResource(i.next().getURI());
        	localResources.add(property);
        	collectProperty(property, resources);
		}

		resources.removeAll(localResources);
		return resources;
	}

	private static void collectType(Resource type, Set<Resource> resources) {
        if (resources.contains(type))
        	return;
        resources.add(type);

		for (OntProperty p : listDeclaredProperties(type))
			collectProperty(p, resources);
		
		for (OntClass supertype : listObjects(type, RDFS.subClassOf, OntClass.class))
			collectType(supertype, resources);
	}

	private static void collectProperty(Resource property, Set<Resource> resources) {
        if (resources.contains(property))
        	return;
        resources.add(property);

		Resource type = property.getPropertyResourceValue(RDFS.range);
		if (type!=null && !isDatatype(type))
			collectType(type, resources);

		for(Resource equivalentProperty : listObjects(property, OWL.equivalentProperty, Resource.class))
			collectProperty(equivalentProperty, resources);

		for(Resource equivalentProperty : listSubjects(OWL.equivalentProperty, property, Resource.class))
			collectProperty(equivalentProperty, resources);

		for(Resource superProperty : listObjects(property, RDFS.subPropertyOf, Resource.class))
			collectProperty(superProperty, resources);
	}

	private static Set<OntProperty> listDeclaredProperties(Resource element) {
		HashSet<OntProperty> properties = new LinkedHashSet<OntProperty>();
        for (OntProperty p : listSubjects(RDFS.domain, element, OntProperty.class))
        	properties.add(p);
		return properties;
	}

	private static <T extends Resource> Set<T> listSubjects(Property p, Resource r, Class<T> aClass) {
    	LinkedHashSet<T> values = new LinkedHashSet<T>();
        for (ResIterator i = r.getModel().listResourcesWithProperty(p, r); i.hasNext();)
        	values.add(i.next().as(aClass));
        return values;
    }

	private static <T extends RDFNode> Set<T> listObjects(Resource r, Property p, Class<T> aClass) {
		Set<T> values = new LinkedHashSet<T>();
        for (StmtIterator i = r.listProperties(p); i.hasNext(); )
        	values.add(i.next().getObject().as(aClass));
        return values;
    }

    private static boolean isDatatype(Resource resource) {
    	return XSD.getURI().equals(resource.getNameSpace()) || RDFS.Literal.equals(resource);
	}

	public Set<Resource> listRDFTypes(Resource r) {
		return listObjects(r, RDF.type, Resource.class);
	}

	public boolean isOfType(Resource resource, Resource type) {
		Set<Resource> actualTypes = listRDFTypes(resource);
		return actualTypes.contains(type);
    }
    
	public boolean isOfKind(Resource operation, Resource kind) {
		return kind.equals(operation.getPropertyResourceValue(DTA_kind));
    }

	public String getDestination(OntResource operation) {
    	RDFNode value = operation.getPropertyValue(JasperConstants.DTA_destination);
		return value!=null ? value.asLiteral().getString() : null;
    }

    public Resource getParameter(OntResource operation) {
    	return operation.getPropertyResourceValue(JasperConstants.DTA_parameter);
    }

	public Resource getData(OntResource operation) {
    	return operation.getPropertyResourceValue(JasperConstants.DTA_data);
   }

	public Resource getDataType(OntResource operation) {
    	Resource value = operation.getPropertyResourceValue(JasperConstants.DTA_data);
		return value!=null ? value.as(OntProperty.class).getRange() : null;
    }

	public boolean hasMultivaluedData(OntResource operation) {
		Resource value = operation.getPropertyResourceValue(JasperConstants.DTA_dataRestriction);
		return value!=null ? value.canAs(MinCardinalityRestriction.class) : true;
    }

	public Resource getKind(OntResource operation) {
    	return operation.getPropertyResourceValue(JasperConstants.DTA_kind);
    }

    public String getRule(OntResource request) {
    	RDFNode value = request.getPropertyValue(JasperConstants.DTA_rule);
		return value!=null ? value.asLiteral().getString() : null;
    }

    private String getBasePackage() {
    	RDFNode value = model.listOntologies().next().getPropertyValue(JasperConstants.DTA_basepackage);
		return value!=null ? value.asLiteral().getString() : "";
    }
    
	public Class<?> getOperationParameterClass(OntResource operation) throws ClassNotFoundException {
		Resource type = getParameter(operation);
		return type != null ? getType(type, false) : null;
	}
	
	public Class<?> getOperationDataClass(OntResource operation) throws ClassNotFoundException {
		Resource type = getDataType(operation);
		return type != null ? getType(type, hasMultivaluedData(operation)) : null;
	}

	public Class<?> getRequestParameterClass(OntResource request) throws ClassNotFoundException {
		Class<?> type = getType(request, false);
		return type != null ? loadClass(type.getName()+"$Parameter", false) : null;
	}
	
	public Class<?> getRequestDataClass(OntResource request) throws ClassNotFoundException {
		Resource type = getDataType(request);
		return type != null ? getType(type, hasMultivaluedData(request)) : null;
	}

	public Callable getCallable(OntResource operation) throws Exception {
		Class<?> type = getType(operation, false);
		Object processor = type.newInstance();
		if (!(processor instanceof Callable))
			throw new Exception("The class "+type.getName()+" does not implement the interface org.mule.api.lifecycle.Callable");
		return (Callable) processor;
	}

	private Class<?> getType(Resource resource, boolean isArray) throws ClassNotFoundException {
		String packageName = getPackageName(resource);
		String className = getTypeName(resource);
		if (!isDatatype(resource))
			className = packageName+"."+className;
		return loadClass(className, isArray);
	}

	private Class<?> loadClass(String className, boolean isArray) throws ClassNotFoundException {
		Class<?> type = ClassUtils.loadClass(className, getClass());
		if (isArray)
			type = Array.newInstance(type, 0).getClass(); 
		return type;
	}
	
	private String getPackageName(Resource res) {
		if (isDatatype(res))
			return "xsd";
		String prefix = res.getModel().getNsURIPrefix(res.getNameSpace());
		String basePackage = getBasePackage();
		String name = basePackage.length()>0 ? basePackage : DTA_BASE_PACKAGE;
		name += prefix.length()>0 ? "."+prefix : "";
		return name.toLowerCase();
	}

	private String getTypeName(Resource res) {
		if (res == null || RDFS.Literal.equals(res))
			return Object.class.getName();
		if (XSD.getURI().equalsIgnoreCase(res.getNameSpace())) {
			if (XSD.xstring.equals(res))
				return String.class.getName();
			if (XSD.integer.equals(res))
				return Integer.class.getName();
			if (XSD.decimal.equals(res))
				return Double.class.getName();
			if (XSD.xboolean.equals(res))
				return Boolean.class.getName();
			if (XSD.dateTime.equals(res) || XSD.time.equals(res) || XSD.date.equals(res))
				return Calendar.class.getName();
			if (XSD.duration.equals(res))
				return Duration.class.getName();
			if (XSD.hexBinary.equals(res))
				return byte[].class.getName();
		}
		return toTitleCase(res.getLocalName());
	}

	private String toTitleCase(String s) {
		return s.substring(0, 1).toUpperCase()+s.substring(1);
	}
	
	public OntResource get(String uri) {
		return model.getOntResource(uri);
	}

}

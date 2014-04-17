package com.coralcea.jasper.tools.dta.wizards;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.xerces.util.XMLChar;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.StmtIteratorImpl;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DC_10;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public abstract class DTAImporter {

	private static final Resource[] integerTypes = new Resource[] {XSD.xbyte, XSD.xint, XSD.xlong, XSD.xshort, XSD.unsignedByte, XSD.unsignedInt, XSD.unsignedLong, XSD.unsignedShort, XSD.nonNegativeInteger, XSD.nonPositiveInteger, XSD.positiveInteger, XSD.negativeInteger};
	private static final Set<Resource> integerTypeSet = new HashSet<Resource>(Arrays.asList(integerTypes));
	private static final Resource[] decimalTypes = new Resource[] {XSD.xfloat, XSD.xdouble};
	private static final Set<Resource> decimalTypeSet = new HashSet<Resource>(Arrays.asList(decimalTypes));
	private static final Resource[] binaryTypes = new Resource[] {XSD.base64Binary};
	private static final Set<Resource> binaryTypeSet = new HashSet<Resource>(Arrays.asList(binaryTypes));
	private static final Resource[] stringTypes = new Resource[] {XSD.normalizedString, XSD.token, XSD.Name, XSD.QName, XSD.language, XSD.NMTOKEN, XSD.ENTITY, XSD.ID, XSD.NCName, XSD.IDREF, XSD.anyURI};
	private static final Set<Resource> stringTypeSet = new HashSet<Resource>(Arrays.asList(stringTypes));
	private static final Resource[] dateTypes = new Resource[] {XSD.gDay, XSD.gMonth, XSD.gMonthDay, XSD.gYear, XSD.gYearMonth};
	private static final Set<Resource> dateTypeSet = new HashSet<Resource>(Arrays.asList(dateTypes));
	
	public abstract String getName();
	
	public final OntModel importFile(String path) {
		OntModel model = null;
		
		try {
			model = readFile(path);
		} catch (Exception e) {
			Activator.getDefault().log(e);
			MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, Status.ERROR, "Problems reading the file with this import kind", null);
			status.add(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return null;
		}
		
		try {
			transformModel(path, model);
		} catch (Exception e) {
			Activator.getDefault().log(e);
			MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, Status.ERROR, "Problems transforming the imported model", null);
			status.add(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return null;
		}
		
		return model;
	}
	
	public abstract OntModel readFile(String path) throws Exception;
	
	public OntModel transformModel(String path, OntModel model) throws Exception {
		Model baseModel = model.getBaseModel();
		Set<Statement> toRemove = new LinkedHashSet<Statement>();
		
		// Make sure the ontology URI is valid
		Resource ontology = model.listOntologies().hasNext() ? model.listOntologies().next() : null;
		if (ontology == null)
			model.createOntology("file://"+path);
		else {
			String uri = ontology.getURI();
			if (uri.length()>0 && !XMLChar.isNCName(uri.charAt(uri.length()-1)))
				ResourceUtils.renameResource(ontology, uri.substring(0, uri.length()-1));
		}

		// convert owl:AllValuesFrom to rdfs:range
		for(StmtIterator i = baseModel.listStatements(null, RDFS.subClassOf, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			RDFNode object = s.getObject();
			if (object.isAnon()) {
				object = model.getOntResource(object.asResource());
				if (object.canAs(AllValuesFromRestriction.class)) {
					AllValuesFromRestriction r = object.as(AllValuesFromRestriction.class);
					baseModel.add(r.getPropertyResourceValue(OWL.onProperty), RDFS.range, r.getAllValuesFrom());
					toRemove.add(s);
					toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, r));
				}
			}
		}
		remove(baseModel, toRemove);

		// convert rdfs:subclassOf to dta:restriction/rdfs:domain for restrictions
		for(StmtIterator i = baseModel.listStatements(null, RDFS.subClassOf, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			RDFNode object = s.getObject();
			if (object.isAnon()) {
				object = model.getOntResource(object.asResource());
				if (object.canAs(Restriction.class)) {
					Restriction r = object.as(Restriction.class);
					baseModel.add(s.getSubject(), DTA.restriction, s.getObject());
					baseModel.add(r.getPropertyResourceValue(OWL.onProperty), RDFS.domain, s.getSubject());
					toRemove.add(s);
				}
			}
		}
		remove(baseModel, toRemove);
		
		// convert owl:unionOf to individual statements
		for(Statement s : baseModel.listStatements().toList()) {
			RDFNode object = s.getObject();
			if (object.isAnon()) {
				object = model.getOntResource(object.asResource());
				if (object.canAs(UnionClass.class)) {
					UnionClass union = object.as(UnionClass.class);
					for (ExtendedIterator<?> k = union.listOperands(); k.hasNext();) {
						OntClass c = (OntClass) k.next();
						baseModel.add(s.getSubject(), s.getPredicate(), c);
					}
					toRemove.add(s);
					toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, union));
				}
			}
		}
		remove(baseModel, toRemove);

		// replace rdfs:Datatype extension by its primitive base
		for(ResIterator i = baseModel.listSubjectsWithProperty(RDF.type, RDFS.Datatype); i.hasNext();) {
			Resource datatype = i.next();
			if (datatype.isAnon())
				continue;
			Resource primitiveType = getExtendedPrimitiveType(datatype);
			if (primitiveType != null) {
				List<Statement> statements = baseModel.listStatements(null, null, datatype).toList();
				for (Statement statement : statements) {
					baseModel.add(statement.getSubject(), statement.getPredicate(), primitiveType);
					toRemove.add(statement);
				}
				toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, datatype));
			}
		}
		remove(baseModel, toRemove);

		// convert rdf:Class to owl:Class
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, RDFS.Class); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), s.getPredicate(), OWL.Class);
			toRemove.add(s);
		}
		remove(baseModel, toRemove);

		// convert owl:DeprecatedClass to owl:Class
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, OWL.DeprecatedClass); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), s.getPredicate(), OWL.Class);
		}

		// convert rdf:Property to either owl:DatatypeProperty or owl:ObjectProperty
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, RDF.Property); i.hasNext();) {
			Statement s = i.next();
			Resource range = s.getSubject().getPropertyResourceValue(RDFS.range);
			if (range != null) {
				if (DTAUtilities.isDatatype(range))
					baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
				else
					baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
			} else
				baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
			toRemove.add(s);
		}
		remove(baseModel, toRemove);

		// convert other types of properties to either owl:DatatypeProperty or owl:ObjectProperty
		Resource[] types = new Resource[]{OWL.DeprecatedProperty, OWL.SymmetricProperty, OWL.TransitiveProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty};
		for(StmtIterator i = DTAUtilities.listStatementsOfTypes(baseModel, types); i.hasNext();) {
			Statement s = i.next();
			Resource range = s.getSubject().getPropertyResourceValue(RDFS.range);
			if (range != null) {
				if (DTAUtilities.isDatatype(range))
					baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
				else
					baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
			} else
				baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
		}

		// substitute unknown ranges of OWL.DatatypeProperty
		for(Statement s : baseModel.listStatements(null, RDFS.range, (RDFNode)null).toList()) {
			Resource range = s.getObject().asResource();
			if (integerTypeSet.contains(range)) {
				baseModel.add(s.getSubject(), RDFS.range, XSD.integer);
				toRemove.add(s);
			}
			else if (decimalTypeSet.contains(range)) {
				baseModel.add(s.getSubject(), RDFS.range, XSD.decimal);
				toRemove.add(s);
			}
			else if (stringTypeSet.contains(range)) {
				baseModel.add(s.getSubject(), RDFS.range, XSD.xstring);
				toRemove.add(s);
			}
			else if (binaryTypeSet.contains(range)) {
				baseModel.add(s.getSubject(), RDFS.range, XSD.hexBinary);
				toRemove.add(s);
			}
			else if (dateTypeSet.contains(range)) {
				baseModel.add(s.getSubject(), RDFS.range, XSD.date);
				toRemove.add(s);
			}
		}
		remove(baseModel, toRemove);

		// convert dc:description to rdfs:comment
		for(StmtIterator i = DTAUtilities.listStatementsOfPredicates(baseModel, new Property[]{DCTerms.description, DC.description, DC_10.description}); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), RDFS.comment, s.getObject());
			toRemove.add(s);
		}
		remove(baseModel, toRemove);
		
		// convert dc:title to rdfs:label
		for(StmtIterator i = baseModel.listStatements(null, DC.title, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), RDFS.label, s.getObject());
			toRemove.add(s);
		}
		remove(baseModel, toRemove);
		
		// convert all cardinalities to 1
		for(StmtIterator i = DTAUtilities.listStatementsOfPredicates(baseModel, new Property[]{OWL.cardinality, OWL.minCardinality, OWL.maxCardinality}); i.hasNext();) {
			Statement s = i.next();
			if (s.getObject().asLiteral().getInt() != 1) {
				baseModel.addLiteral(s.getSubject(), s.getPredicate(), 1);
				toRemove.add(s);
			}
		}
		remove(baseModel, toRemove);

		return model;
	}

	protected Resource getExtendedPrimitiveType(Resource type) {
		for(Resource superType : DTAUtilities.listObjects(type, RDFS.subClassOf, Resource.class)) {
			if (DTAUtilities.isDatatype(superType))
				return superType;
			Resource primitiveType = getExtendedPrimitiveType(superType);
			if (primitiveType != null)
				return primitiveType;
		}
		return null;
	}
	
	protected void remove(Model model, Set<Statement> statements) {
		model.remove(new StmtIteratorImpl(statements.iterator()));
		statements.clear();
	}

}

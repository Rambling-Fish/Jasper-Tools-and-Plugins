package com.coralcea.jasper.tools.dta.wizards;

import com.coralcea.jasper.tools.MultiValueMap;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTARdfImporter extends DTAImporter {
	
	public static final String NAME = "RDF Model";
	
	public String getName() {
		return NAME;
	}

	public OntModel readFile(String path) throws Exception {
		OntModel ontModel = DTACore.createNewModel();
		Model model = ModelFactory.createDefaultModel();
		model.read(path);

		// add namespaces
		ontModel.setNsPrefixes(model.getNsPrefixMap());
		
		// add an ontology
		ResIterator r = model.listSubjectsWithProperty(RDF.type, OWL.Ontology);
		if (r.hasNext())
			ontModel.createOntology(r.next().getURI());
		else
			ontModel.createOntology("file:"+path);
		
		MultiValueMap<Resource, Resource> resourceToType = new MultiValueMap<Resource, Resource>();
		
		// add classes
		for(StmtIterator i = model.listStatements(null, RDF.type, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			Resource resource = s.getSubject();
			Resource type = s.getObject().asResource();
			if (!DTAUtilities.isDatatype(type) && 
				!OWL.getURI().equals(type.getNameSpace()) && 
				!RDFS.getURI().equals(type.getNameSpace()) &&
				!RDF.getURI().equals(type.getNameSpace())) {
				ontModel.createClass(type.getURI());
				resourceToType.put(resource, type);
			}
		}
		
		// add properteis
		for(StmtIterator i = model.listStatements(); i.hasNext();) {
			Statement s = i.next();
			Property property = s.getPredicate();
			if (property.equals(RDF.type) || !resourceToType.containsKey(s.getSubject()))
				continue;
			if (OWL.getURI().equals(property.getNameSpace()) || 
				RDFS.getURI().equals(property.getNameSpace()) ||
				RDF.getURI().equals(property.getNameSpace()) ||
				DC.getURI().equals(property.getNameSpace()))
				continue;
			if (s.getObject().isLiteral()) {
				ontModel.add(s.getPredicate(), RDF.type, OWL.DatatypeProperty);
				String datatype = s.getObject().asLiteral().getDatatypeURI();
				if (datatype != null)
					ontModel.add(s.getPredicate(), RDFS.range, ontModel.createResource(datatype));
				else
					ontModel.add(s.getPredicate(), RDFS.range, RDFS.Literal);
			} else {
				ontModel.add(s.getPredicate(), RDF.type, OWL.ObjectProperty);
				if (resourceToType.containsKey(s.getObject().asResource())) {
					for(Resource type : resourceToType.get(s.getObject().asResource()))
						ontModel.add(s.getPredicate(), RDFS.range, type);
				} else
					ontModel.add(s.getPredicate(), RDFS.range, RDFS.Resource);
			}
			for(Resource type : resourceToType.get(s.getSubject()))
				ontModel.add(s.getPredicate(), RDFS.domain, type);
		}
		
		// replace multiple ranges of datatype properties by rdfs:Label
		for(StmtIterator i = ontModel.listStatements(null, null, OWL.DatatypeProperty); i.hasNext();) {
			Statement s = i.next();
			OntProperty property = s.getSubject().as(OntProperty.class);
			if (property.listRange().toSet().size()>1) {
				property.setRange(RDFS.Literal);
			}
		}
	
		model.close();
		return ontModel;
	}

}

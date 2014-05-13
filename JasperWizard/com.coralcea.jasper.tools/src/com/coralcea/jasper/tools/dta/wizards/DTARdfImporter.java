package com.coralcea.jasper.tools.dta.wizards;

import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
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
		
		// add all classes
		for(NodeIterator i = model.listObjectsOfProperty(RDF.type); i.hasNext();) {
			Resource type = i.next().asResource();
			if (!DTAUtilities.isDatatype(type) && 
				!OWL.getURI().equals(type.getNameSpace()) && 
				!RDFS.getURI().equals(type.getNameSpace()) &&
				!RDF.getURI().equals(type.getNameSpace()) )
				ontModel.createClass(type.getURI());
		}
		
		model.close();
		return ontModel;
	}

}

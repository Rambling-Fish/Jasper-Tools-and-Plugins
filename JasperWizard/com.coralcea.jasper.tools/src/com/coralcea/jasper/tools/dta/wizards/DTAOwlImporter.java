package com.coralcea.jasper.tools.dta.wizards;

import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DC_10;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class DTAOwlImporter extends DTAImporter {
	
	public static final String NAME = "OWL Ontology";
	
	public String getName() {
		return NAME;
	}

	public OntModel readFile(String path) throws Exception {
		OntModel model = DTACore.createNewModel();
		model.read(path);

		// Make sure there is an ontology
		Model baseModel = model.getBaseModel();
		StmtIterator j = baseModel.listStatements(null, RDF.type, OWL.Ontology);
		if (!j.hasNext()) {
			j = DTAUtilities.listStatementsOfPredicates(baseModel, new Property[] {DCTerms.title, DC.title, DC_10.title});
			while (j.hasNext()) {
				Resource subject = j.next().getSubject();
				if (!subject.hasProperty(RDF.type)) {
					baseModel.add(subject, RDF.type, OWL.Ontology);
					break;
				}
			}
		}
		
		return model;
	}

}

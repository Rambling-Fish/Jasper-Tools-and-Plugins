package com.coralcea.jasper.tools.dta.wizards;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import tr.com.srdc.ontmalizer.XSD2OWLMapper;

import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;

public class DTAXsdImporter extends DTAImporter {
	
	public static final String NAME = "XSD Schema";

	private static final Resource Enumeration = ResourceFactory.createResource("http://www.srdc.com.tr/ontmalizer#Enumeration");
	private static final Resource EnumeratedValue = ResourceFactory.createResource("http://www.srdc.com.tr/ontmalizer#EnumeratedValue");
	private static final Property hasValue = ResourceFactory.createProperty("http://www.srdc.com.tr/ontmalizer#hasValue");
	
	public String getName() {
		return NAME;
	}
	
	public OntModel readFile(String path) throws Exception {
	    XSD2OWLMapper mapping = new XSD2OWLMapper(new File(path));
	    mapping.setObjectPropPrefix("");
	    mapping.setDataTypePropPrefix("");
	    mapping.convertXSD2OWL();		
		return mapping.getOntology();
	}

	@Override
	public OntModel transformModel(String path, OntModel model) throws Exception {
		super.transformModel(path, model);
		Model baseModel = model.getBaseModel();
		Set<Statement> toRemove = new LinkedHashSet<Statement>();
		
		toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, Enumeration));
		toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, EnumeratedValue));
		toRemove.addAll(DTAUtilities.listAllStatementsOn(baseModel, hasValue));
		remove(baseModel, toRemove);

		return model;
	}

}

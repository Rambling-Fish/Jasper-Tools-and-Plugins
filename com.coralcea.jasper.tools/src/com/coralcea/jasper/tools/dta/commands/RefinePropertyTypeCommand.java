package com.coralcea.jasper.tools.dta.commands;

import java.util.List;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.OWL;

public class RefinePropertyTypeCommand extends DTACommand {

	private OntResource element;
	private OntProperty property;
	private Resource newType;
	private List<Statement> oldStatements;
	private List<Statement> newStatements;
	
	public RefinePropertyTypeCommand(OntResource element, OntProperty property, Resource newType) {
		super("Changing Restriction");
		this.element = element;
		this.property = property;
		this.newType = newType;
	}

	@Override
	public void prepare() {
		Restriction r = DTAUtilities.getDirectRestriction(element, DTA.inputRestriction, property);
		
		if (DTA.None.equals(newType) || newType.equals(property.getRange())) {
			if (r!=null)
				oldStatements = DTAUtilities.listStatementsOn(element.getOntModel().getBaseModel(), r);
		} else if (r==null) {
			r = element.getOntModel().createAllValuesFromRestriction(null, property, newType);
			element.getOntModel().add(element, DTA.inputRestriction, r);
			newStatements = DTAUtilities.listStatementsOn(element.getOntModel().getBaseModel(), r);
		} else {
			oldStatements = element.getOntModel().listStatements(r, OWL.allValuesFrom, (RDFNode)null).toList();
			r.asAllValuesFromRestriction().setAllValuesFrom(newType);
			newStatements = element.getOntModel().listStatements(r, OWL.allValuesFrom, (RDFNode)null).toList();
		}
	}

	@Override
	public void undo() {
		if (newStatements!=null)
			element.getOntModel().remove(newStatements);
		if (oldStatements!=null)
			element.getOntModel().add(oldStatements);
	}

	@Override
	public void redo() {
		if (oldStatements!=null)
			element.getOntModel().remove(oldStatements);
		if (newStatements!=null)
			element.getOntModel().add(newStatements);
	}

}

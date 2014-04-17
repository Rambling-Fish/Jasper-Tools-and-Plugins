package com.coralcea.jasper.tools.dta.commands;

import java.util.List;

import com.coralcea.jasper.tools.dta.Cardinality;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Statement;

public class ChangeCardinalityCommand extends DTACommand {

	private OntResource element;
	private Property kind;
	private Property property;
	private String value;
	private List<Statement> oldRestriction;
	private List<Statement> newRestriction;
	
	public ChangeCardinalityCommand(OntResource element, Property kind, Property property, String value) {
		super("Changing Restriction");
		this.element = element;
		this.kind = kind;
		this.property = property;
		this.value = value;
	}

	@Override
	public void prepare() {
		Restriction r = DTAUtilities.getDirectRestriction(element, kind, property);
		if (r!=null)
			oldRestriction = DTAUtilities.listDirectStatementsOn(element.getOntModel().getBaseModel(), r);
		
		r = null;
		if (value.equals(Cardinality.ZERO_ONE))
			r = element.getOntModel().createMaxCardinalityRestriction(null, property, 1);
		else if (value.equals(Cardinality.ONE_STAR))
			r = element.getOntModel().createMinCardinalityRestriction(null, property, 1);
		else if (value.equals(Cardinality.ONE_ONE))
			r = element.getOntModel().createCardinalityRestriction(null, property, 1);
		if (r!=null) {
			element.addProperty(kind, r);
			newRestriction = DTAUtilities.listDirectStatementsOn(element.getOntModel().getBaseModel(), r);
		}
	}

	@Override
	public void undo() {
		if (newRestriction!=null)
			element.getOntModel().remove(newRestriction);
		if (oldRestriction!=null)
			element.getOntModel().add(oldRestriction);
	}

	@Override
	public void redo() {
		if (oldRestriction!=null)
			element.getOntModel().remove(oldRestriction);
		if (newRestriction!=null)
			element.getOntModel().add(newRestriction);
	}

}

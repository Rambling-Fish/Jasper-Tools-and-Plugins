package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntModel;

public class RemoveNsPrefixCommand extends DTACommand {

	private OntModel model;
	private String prefix;
	private String oldURI;
	
	public RemoveNsPrefixCommand(OntModel model, String prefix) {
		super("Removing NsPrefix");
		this.model = model;
		this.prefix = prefix;
	}
	
	@Override
	public void prepare() {
		oldURI = model.getNsPrefixURI(prefix);
	}

	@Override
	public void undo() {
		if (oldURI != null) {
			model.setNsPrefix(prefix, oldURI);
			model.getGraph().getEventManager().notifyEvent(model.getGraph(), null);
		}
	}

	@Override
	public void redo() {
		if (oldURI != null) {
			model.removeNsPrefix(prefix);
			model.getGraph().getEventManager().notifyEvent(model.getGraph(), null);
		}
	}

}

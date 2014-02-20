package com.coralcea.jasper.tools.dta.commands;

import com.hp.hpl.jena.ontology.OntModel;

public class SetNsPrefixCommand extends DTACommand {

	private OntModel model;
	private String prefix;
	private String uri;
	private String oldURI;
	
	public SetNsPrefixCommand(OntModel model, String prefix, String uri) {
		super("Setting NsNrefix");
		this.model = model;
		this.prefix = prefix;
		this.uri = uri;
	}

	@Override
	public void prepare() {
		oldURI = model.getNsPrefixURI(prefix);
	}

	@Override
	public void undo() {
		if (oldURI != null)
			model.setNsPrefix(prefix, oldURI);
		else
			model.removeNsPrefix(prefix);
		model.getGraph().getEventManager().notifyEvent(model.getGraph(), null);
	}

	@Override
	public void redo() {
		model.setNsPrefix(prefix, uri);
		model.getGraph().getEventManager().notifyEvent(model.getGraph(), null);
	}

}

package com.coralcea.jasper.tools.dta.wizards;

import com.coralcea.jasper.tools.dta.DTACore;
import com.hp.hpl.jena.ontology.OntModel;

public class DTAJsonImporter extends DTAImporter {
	
	public static final String NAME = "Json File";
	
	public String getName() {
		return NAME;
	}
	
	public OntModel readFile(String path) throws Exception {
		OntModel model = DTACore.createNewModel();
		model.setNsPrefix("", path+'#');
		return model;
	}

}

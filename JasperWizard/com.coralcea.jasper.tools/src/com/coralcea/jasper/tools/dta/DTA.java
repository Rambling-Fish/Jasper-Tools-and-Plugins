package com.coralcea.jasper.tools.dta;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class DTA {

	public static final String URI = "http://coralcea.ca/2014/01/dta#";
	public static final String PREFIX = "dta";
	public static final String EXTENSION = "dta";
	public static final String FORMAT = "TURTLE";
	public static final String IMPORT_POLICY = "import-policy.rdf";
	public static final String MARKER = "com.coralcea.jasper.markers.DTA";

	public static final Resource DTAs = resource("DTAs");
	public static final Resource Types = resource("Types");
	public static final Resource Properties = resource("Properties");
	public static final Resource None = resource("None");
	public static final Resource New = resource("New");
	
	public static final Resource DTA = resource("DTA");
	public static final Resource Operation = resource("Operation");
	public static final Resource Request = resource("Request");
	public static final Resource Get = resource("Get");
	public static final Resource Post = resource("Post");
	public static final Resource Publish = resource("Publish");
	
	public static final Property isLibrary = property("isLibrary");
	public static final Property operation = property("operation");
	public static final Property request = property("request");
	public static final Property kind = property("kind");
	public static final Property rule = property("rule");
	public static final Property expires = property("expires");
	public static final Property input = property("input");
	public static final Property output = property("output");
	public static final Property destination = property("destination");
	public static final Property basepackage = property("basepackage");
	public static final Property restriction = property("restriction");
	public static final Property inputRestriction = property("inputRestriction");
	public static final Property outputRestriction = property("outputRestriction");

	public static final Property x = property("x");
	public static final Property y = property("y");

    private static final Resource resource( String local ){ 
    	return ResourceFactory.createResource( URI + local ); 
    }

    private static final Property property( String local ){ 
    	return ResourceFactory.createProperty( URI, local ); 
    }
    
    public static final String getURI(){ 
    	return URI; 
    }
}

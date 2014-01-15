package com.coralcea.jasper.tools.dta;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class DTA {

	public static final String URI = "http://coralcea.ca/2014/01/dta#";
	public static final String PREFIX = "dta";
	public static final String EXTENSION = "dta";
	public static final String GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	public static final Resource DTAs = resource("DTAs");
	public static final Resource Classes = resource("Classes");
	public static final Resource Properties = resource("Properties");
	public static final Resource None = resource("None");
	
	public static final Resource DTA = resource("DTA");
	public static final Resource Post = resource("Post");
	public static final Resource Receive = resource("Receive");
	public static final Resource Request = resource("Request");
	public static final Resource Provide = resource("Provide");
	
	public static final Property operation = property("operation");
	public static final Property input = property("input");
	public static final Property output = property("output");
	public static final Property destination = property("destination");
	
    private static final Resource resource( String local ){ 
    	return ResourceFactory.createResource( URI + local ); 
    }

    private static final Property property( String local ){ 
    	return ResourceFactory.createProperty( URI, local ); 
    }
    
}

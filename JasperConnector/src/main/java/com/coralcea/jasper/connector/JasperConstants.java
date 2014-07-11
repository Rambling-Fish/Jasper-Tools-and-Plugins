package com.coralcea.jasper.connector;

import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;

public class JasperConstants {

	public static final String GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";

	// Status properties
	public static final String STATUS_CODE = "code";
	public static final String STATUS_DESCRIPTION = "description";
	
	// Request/response keys
	public static final String CONTENT_TYPE = "content-type";
	public static final String RESPONSE_TYPE = "response-type";
	public static final String PROCESSING_SCHEME = "processing-scheme";
	public static final String EXPIRES = "expires";

	// Request/response values
	public static final String VERSION = "1.0";
	public static final String JSON = "application/json";
	public static final String AGGREGATE = "aggregate";
	public static final String COALESCE = "coalesce";
	public static final String GET = "GET";
	public static final String POST = "POST";
	public static final String PUBLISH = "PUBLISH";
	public static final String SUBSCRIBE = "SUBSCRIBE";

	// DTA metadata
	public static final Resource DTA_DTA = resource("DTA");
	public static final Resource DTA_Operation = resource("Operation");
	public static final Resource DTA_Request = resource("Request");
	public static final Resource DTA_Get = resource("Get");
	public static final Resource DTA_Post = resource("Post");
	public static final Resource DTA_Publish = resource("Publish");
	public static final Resource DTA_Subscribe = resource("Subscribe");
	
	public static final Property DTA_operation = property("operation");
	public static final Property DTA_request = property("request");
	public static final Property DTA_parameter = property("parameter");
	public static final Property DTA_data = property("data");
	public static final Property DTA_dataRestriction = property("dataRestriction");
	public static final Property DTA_kind = property("kind");
	public static final Property DTA_rule = property("rule");
	public static final Property DTA_destination = property("destination");
	public static final Property DTA_basepackage = property("basepackage");
	public static final Property DTA_x = property("x");
	public static final Property DTA_y = property("y");
	
	// DTA info
	public static final String DTA_URI = "http://coralcea.ca/2014/01/dta#";
	public static final String DTA_EXTENSION = "dta";
	public static final String DTA_FORMAT = "TURTLE";
	public static final String DTA_IMPORT_POLICY = "import-policy.rdf";
	public static final String DTA_BASE_PACKAGE = "base";
	
    public static final Resource resource( String local ){ 
    	return ResourceFactory.createResource( DTA_URI + local ); 
    }

    public static final Property property( String local ){ 
    	return ResourceFactory.createProperty( DTA_URI, local ); 
    }

}

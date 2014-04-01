package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#MSData")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value=MSDataImpl.class, name="http://coralcea.ca/heartratedta#MSData"),
	@JsonSubTypes.Type(value=HRDataImpl.class, name="http://coralcea.ca/heartratedta#HRData")
})
public interface MSData {
}


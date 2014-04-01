package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRDataReq")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value=HRDataReqImpl.class, name="http://coralcea.ca/heartratedta#HRDataReq")
})
public interface HRDataReq {

	/**
	 * @return sid 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public String getSid();

	/**
	 * @param sid 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public void setSid(String sid);
}


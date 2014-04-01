package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRUpdateReq")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value=HRUpdateReqImpl.class, name="http://coralcea.ca/heartratedta#HRUpdateReq")
})
public interface HRUpdateReq {

	/**
	 * @return hrData 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#hrData")
	public HRData getHrData();

	/**
	 * @return sid 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public String getSid();

	/**
	 * @param hrData 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#hrData")
	public void setHrData(HRData hrData);

	/**
	 * @param sid 
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public void setSid(String sid);
}


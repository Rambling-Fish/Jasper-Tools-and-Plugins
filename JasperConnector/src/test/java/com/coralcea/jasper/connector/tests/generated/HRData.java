package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

/**
 * The heart rate
 */
@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRData")
@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property="@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value=HRDataImpl.class, name="http://coralcea.ca/heartratedta#HRData")
})
public interface HRData extends MSData {

	/**
	 * @return bpm The bit per minute
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#bpm")
	public int getBpm();

	/**
	 * @return timestamp The timestamp
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#timestamp")
	public String getTimestamp();

	/**
	 * @param bpm The bit per minute
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#bpm")
	public void setBpm(int bpm);

	/**
	 * @param timestamp The timestamp
	 */
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#timestamp")
	public void setTimestamp(String timestamp);
}


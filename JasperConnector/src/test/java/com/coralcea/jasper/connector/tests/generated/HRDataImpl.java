package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

/**
 * The heart rate
 */
@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRData")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class HRDataImpl implements HRData {

	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#bpm")
	private int bpm;

	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#timestamp")
	private String timestamp;

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#bpm")
	public int getBpm() {
		return bpm;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#timestamp")
	public String getTimestamp() {
		return timestamp;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#bpm")
	public void setBpm(int bpm) {
		this.bpm = bpm;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#timestamp")
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	@Generated("true")
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bpm;
		result = prime * result
				+ ((timestamp == null) ? 0 : timestamp.hashCode());
		return result;
	}

	@Override
	@Generated("true")
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HRDataImpl other = (HRDataImpl) obj;
		if (bpm != other.bpm)
			return false;
		if (timestamp == null) {
			if (other.timestamp != null)
				return false;
		} else if (!timestamp.equals(other.timestamp))
			return false;
		return true;
	}

	@Override
	@Generated("true")
	public String toString() {
		return "HRDataImpl [ " + "bpm=" + bpm + ", " + "timestamp=" + timestamp
				+ " ]";
	}
}

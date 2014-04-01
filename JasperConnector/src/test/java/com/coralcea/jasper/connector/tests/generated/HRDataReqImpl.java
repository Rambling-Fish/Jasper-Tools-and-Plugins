package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRDataReq")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class HRDataReqImpl implements HRDataReq {

	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	private String sid;

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public String getSid() {
		return sid;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public void setSid(String sid) {
		this.sid = sid;
	}

	@Override
	@Generated("true")
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sid == null) ? 0 : sid.hashCode());
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
		HRDataReqImpl other = (HRDataReqImpl) obj;
		if (sid == null) {
			if (other.sid != null)
				return false;
		} else if (!sid.equals(other.sid))
			return false;
		return true;
	}

	@Override
	@Generated("true")
	public String toString() {
		return "HRDataReqImpl [ " + "sid=" + sid + " ]";
	}
}

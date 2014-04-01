package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;
import org.codehaus.jackson.annotate.*;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#HRUpdateReq")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
public class HRUpdateReqImpl implements HRUpdateReq {

	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#hrData")
	private HRData hrData;

	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	private String sid;

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#hrData")
	public HRData getHrData() {
		return hrData;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#sid")
	public String getSid() {
		return sid;
	}

	@Override
	@Generated("true")
	@JsonProperty("http://coralcea.ca/heartratedta#hrData")
	public void setHrData(HRData hrData) {
		this.hrData = hrData;
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
		result = prime * result + ((hrData == null) ? 0 : hrData.hashCode());
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
		HRUpdateReqImpl other = (HRUpdateReqImpl) obj;
		if (hrData == null) {
			if (other.hrData != null)
				return false;
		} else if (!hrData.equals(other.hrData))
			return false;
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
		return "HRUpdateReqImpl [ " + "hrData=" + hrData + ", " + "sid=" + sid
				+ " ]";
	}
}

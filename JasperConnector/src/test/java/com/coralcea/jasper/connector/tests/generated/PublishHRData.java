package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;

import org.codehaus.jackson.annotate.*;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#publishHRData")
public class PublishHRData implements Callable {

	/**
	 * @param muleEventContext
	 * @return Parameters
	 */
	@Generated("false")
	public Parameters onCall(MuleEventContext muleEventContext) throws Exception {
		Parameters parameters = new Parameters();
		parameters.setSid((String) muleEventContext.getMessage().getInboundProperty("sid"));
		HRData hrData = new HRDataImpl();
		hrData.setBpm(Integer.valueOf((String) muleEventContext.getMessage().getInboundProperty("bpm")));
		hrData.setTimestamp((String) muleEventContext.getMessage().getInboundProperty("timestamp"));
		parameters.setHrData(hrData);
		return parameters;
	}

	/**
	 * The parameters of {@link PublishHRData}
	 */
	@Generated("true")
	public static class Parameters {

		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#hrData")
		private HRData hrData;

		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#sid")
		private String sid;

		/**
		 * @return hrData 
		 */
		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#hrData")
		public HRData getHrData() {
			return hrData;
		}

		/**
		 * @return sid 
		 */
		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#sid")
		public String getSid() {
			return sid;
		}

		/**
		 * @param hrData 
		 */
		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#hrData")
		public void setHrData(HRData hrData) {
			this.hrData = hrData;
		}

		/**
		 * @param sid 
		 */
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
			result = prime * result
					+ ((hrData == null) ? 0 : hrData.hashCode());
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
			Parameters other = (Parameters) obj;
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
			return "Parameters [ " + "hrData=" + hrData + ", " + "sid=" + sid
					+ " ]";
		}
	}
}

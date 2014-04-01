package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;

import org.codehaus.jackson.annotate.*;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#requestHRData")
public class RequestHRData implements Callable {

	/**
	 * @param muleEventContext
	 * @return Parameters
	 */
	@Generated("true")
	public Parameters onCall(MuleEventContext muleEventContext) throws Exception {
		Parameters parameters = new Parameters();
		parameters.setSid((String) muleEventContext.getMessage().getInboundProperty("sid"));
		return parameters;
	}

	/**
	 * The parameters of {@link RequestHRData}
	 */
	@Generated("true")
	public static class Parameters {

		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#sid")
		private String sid;

		/**
		 * @return sid 
		 */
		@Generated("true")
		@JsonProperty("http://coralcea.ca/heartratedta#sid")
		public String getSid() {
			return sid;
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
			return "Parameters [ " + "sid=" + sid + " ]";
		}
	}
}

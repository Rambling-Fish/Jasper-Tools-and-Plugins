package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;

import org.codehaus.jackson.annotate.*;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;

import com.coralcea.jasper.connector.tests.HRDataCache;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#getHRData")
public class GetHRData implements Callable {

	/**
	 * @param muleEventContext
	 * @return MuleMessage
	 */
	@Generated("true")
	public MuleMessage onCall(MuleEventContext muleEventContext) throws Exception {
		MuleMessage message = muleEventContext.getMessage();
		HRDataReq input = (HRDataReq) message.getPayload();
		Object output = process(input, message);
		message.setPayload(output);
		return message;
	}

	/**
	 * @param hRDataReq
	 * @param muleMessage (on which you may set the property 'statusCode' to report errors)
	 * @return HRData[] (or some other Object if this processor is not terminal)
	 */
	@Generated("false")
	private Object process(HRDataReq hRDataReq, MuleMessage muleMessage) throws Exception{
		HRDataCache cache = HRDataCache.getInstance();
		String sid = hRDataReq.getSid();
		HRData hrData = cache.get(sid);
		if (hrData != null)
			return new HRData[] { hrData };
		return new HRData[0];
	}
}


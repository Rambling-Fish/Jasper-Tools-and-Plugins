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
		HRDataReq parameter = (HRDataReq) message.getPayload();
		Object data = execute(parameter, message);
		message.setPayload(data);
		return message;
	}

	/**
	 * Execute the operation (put your implementation here)
	 * To report error code, call muleMessage.setOutboundProperty("code", <integer>)
	 * To report error description, call muleMessage.setOutboundProperty("description", <string>)
	 * 
	 * @param hRDataReq
	 * @param muleMessage
	 * @return HRData[] (or another Object if this processor is not terminal)
	 */
	@Generated("false")
	private Object execute(HRDataReq hRDataReq, MuleMessage muleMessage) throws Exception {
		HRDataCache cache = HRDataCache.getInstance();
		String sid = hRDataReq.getSid();
		HRData hrData = cache.get(sid);
		if (hrData != null)
			return new HRData[] { hrData };
		return new HRData[0];
	}
}


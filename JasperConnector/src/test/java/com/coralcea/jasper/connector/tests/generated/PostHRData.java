package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;

import org.codehaus.jackson.annotate.*;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;

import com.coralcea.jasper.connector.tests.HRDataCache;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#postHRData")
public class PostHRData implements Callable {

	/**
	 * @param muleEventContext
	 * @return MuleMessage
	 */
	@Generated("false")
	public MuleMessage onCall(MuleEventContext muleEventContext) throws Exception {
		MuleMessage message = muleEventContext.getMessage();
		HRUpdateReq parameter = (HRUpdateReq) message.getPayload();
		Object data = execute(parameter, message);
		message.setPayload(data);
		return message;
	}

	/**
	 * Execute the operation (put your implementation here)
	 * To report error code, call muleMessage.setOutboundProperty("code", <integer>)
	 * To report error description, call muleMessage.setOutboundProperty("description", <string>)
	 * 
	 * @param hRUpdateReq
	 * @param muleMessage
	 * @return null (or another Object if this processor is not terminal)
	 */
	@Generated("false")
	private Object execute(HRUpdateReq hRUpdateReq, MuleMessage muleMessage) throws Exception {
		HRDataCache cache = HRDataCache.getInstance();
		cache.put(hRUpdateReq.getSid(), hRUpdateReq.getHrData());
		return null;
	}
}


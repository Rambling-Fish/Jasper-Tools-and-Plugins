package com.coralcea.jasper.connector.tests.generated;

import javax.annotation.Generated;

import org.codehaus.jackson.annotate.*;
import org.mule.api.lifecycle.Callable;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;

import com.coralcea.jasper.connector.tests.HRDataCache;

@Generated("true")
@JsonTypeName("http://coralcea.ca/heartratedta#getBpm")
public class GetBpm implements Callable {

	/**
	 * @param muleEventContext
	 * @return MuleMessage
	 */
	@Generated("true")
	public MuleMessage onCall(MuleEventContext muleEventContext) throws Exception {
		MuleMessage message = muleEventContext.getMessage();
		HRDataReq input = (HRDataReq) message.getPayload();
		Object output = execute(input, message);
		message.setPayload(output);
		return message;
	}

	/**
	 * Execute the operation (this is what you need to override to implement it)
	 * To report error code, call muleMessage.setOutboundProperty("code", <integer>)
	 * To report error description, call muleMessage.setOutboundProperty("description", <string>)
	 * 
	 * @param hRDataReq
	 * @param muleMessage
	 * @return int ':bpm' (or some other Object if this processor is not terminal)
	 */
	@Generated("false")
	private Object execute(HRDataReq hRDataReq, MuleMessage muleMessage) throws Exception{
		HRDataCache cache = HRDataCache.getInstance();
		String sid = hRDataReq.getSid();
		HRData hrData = cache.get(sid);
		if (hrData != null)
			return hrData.getBpm();
		muleMessage.setOutboundProperty("code", 300);
		return -1;
	}
}


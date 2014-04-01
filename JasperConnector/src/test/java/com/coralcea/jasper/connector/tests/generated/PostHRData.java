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
	@Generated("true")
	public MuleMessage onCall(MuleEventContext muleEventContext) throws Exception {
		MuleMessage message = muleEventContext.getMessage();
		HRUpdateReq input = (HRUpdateReq) message.getPayload();
		Object output = process(input, message);
		message.setPayload(output);
		return message;
	}

	/**
	 * @param hRUpdateReq
	 * @param muleMessage (on which you may set the property 'statusCode' to report errors)
	 * @return null (or some other Object if this processor is not terminal)
	 */
	@Generated("false")
	private Object process(HRUpdateReq hRUpdateReq, MuleMessage muleMessage) throws Exception {
		HRDataCache cache = HRDataCache.getInstance();
		cache.put(hRUpdateReq.getSid(), hRUpdateReq.getHrData());
		return null;
	}
}


package com.coralcea.jasper.connector.tests;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

public class StatusCodeReporter implements Callable {

	public MuleMessage onCall(MuleEventContext muleEventContext) throws Exception {
		ExceptionPayload payload = muleEventContext.getMessage().getExceptionPayload();
		muleEventContext.getMessage().setPayload(payload.getCode());
		return muleEventContext.getMessage();
	}
}

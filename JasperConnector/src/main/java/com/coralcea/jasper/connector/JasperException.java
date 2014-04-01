package com.coralcea.jasper.connector;

import org.mule.api.MuleException;
import org.mule.config.i18n.MessageFactory;

public class JasperException extends MuleException {
	
	private static final long serialVersionUID = 7681600202310726916L;

	public JasperException(String message, int code) {
		super(MessageFactory.createStaticMessage(message));
		setExceptionCode(code);
	}

}

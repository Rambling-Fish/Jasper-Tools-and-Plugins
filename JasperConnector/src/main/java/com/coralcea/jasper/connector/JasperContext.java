package com.coralcea.jasper.connector;

import java.util.UUID;

import javax.jms.Session;
import javax.jms.TextMessage;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.transport.jms.JmsMuleMessageFactory;
import org.mule.transport.jms.transformers.ObjectToJMSMessage;

public class JasperContext {

	private MuleContext context;
	private JmsMuleMessageFactory toMuleMessage;
	private ObjectToJMSMessage toJMSMessage;
	
	public JasperContext(MuleContext context) {
		this.context = context;
		this.toMuleMessage = new JmsMuleMessageFactory(context);
		this.toJMSMessage = new ObjectToJMSMessage();
	}

	public String getValue(String key) {
		return context.getRegistry().lookupObject(key);
	}
	
	public MuleMessage createMuleMessage(Object payload) {
		return new DefaultMuleMessage(payload, context);
	}

	public MuleMessage toMuleMessage(TextMessage jmsMsg) throws Exception {
		String encoding = context.getConfiguration().getDefaultEncoding();
        MuleMessage muleMsg = toMuleMessage.create(jmsMsg, encoding);
        muleMsg.setCorrelationId(jmsMsg.getJMSCorrelationID());
        muleMsg.setReplyTo(null);
        return muleMsg;
	}
	
	public TextMessage toJMSMessage(MuleMessage muleMsg, Session session) throws Exception {
		TextMessage jmsMessage = session.createTextMessage();
		jmsMessage.setJMSCorrelationID(UUID.randomUUID().toString());
        toJMSMessage.setJmsProperties(muleMsg, jmsMessage);
        return jmsMessage;
	}
}

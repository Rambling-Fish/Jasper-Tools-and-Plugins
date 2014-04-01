package com.coralcea.jasper.connector;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.codehaus.jackson.map.ObjectMapper;
import org.mule.api.MuleMessage;

import com.hp.hpl.jena.ontology.OntResource;


public class JasperReceiver {

	private OntResource operation;
	private String destination;
	private boolean isOperation;
	private Queue queue;
	private Session session;
	private MessageConsumer consumer;
	private MessageListener listener;
	private ObjectMapper jsonMapper;
	private Class<?> inputType, outputType;
	
	public JasperReceiver() {
		this(null, false);
	}
	
	public JasperReceiver(String destination, boolean isOperation) {
		this.destination = destination;
		this.isOperation = isOperation; 
		this.jsonMapper = new ObjectMapper();
	}

	public void start(JasperConnection connection) throws Exception {
		session = connection.createSession();
		if (destination == null) {
			queue = session.createTemporaryQueue();
		} else if (isOperation == false) {
			queue = session.createQueue(destination);
		} else {
			operation = connection.getMetadata().get(destination);
    		if (operation == null)
    			throw new IllegalArgumentException("operation "+operation+" is not defined in the DTA");
			queue = session.createQueue(connection.getMetadata().getDestination(operation));
   			inputType = connection.getMetadata().getInputTypeOfOperation(operation);
			outputType = connection.getMetadata().getOutputTypeOfOperation(operation);
		}
		consumer = session.createConsumer(queue);
		if (listener != null)
			consumer.setMessageListener(listener);
	}
	
	public void stop() throws JMSException {
		if (session != null)
			session.close();
	}
	
	public Queue getQueue() {
		return queue;
	}
	
	public void setMessageListener(MessageListener listener) throws JMSException {
		this.listener = listener;
	}
	
	public MuleMessage convertIncomingMessage(JasperContext context, TextMessage jmsMsg) throws Exception {
		Object payload;
		try {
			payload = jsonMapper.readValue(jmsMsg.getText(), inputType);
		} catch (Exception e) {
			throw new Exception("Invalid input passed to operation "+operation, e);
		}

		MuleMessage muleMsg = context.toMuleMessage(jmsMsg);
		muleMsg.setPayload(payload);
		return muleMsg;
	}

	public TextMessage convertOutgoingMessage(JasperContext context, MuleMessage muleMsg) throws Exception {
		Object content = muleMsg.getPayload();

		TextMessage jmsMsg = context.toJMSMessage(muleMsg, session);

		if (outputType != null) {
			if (!outputType.isInstance(content)) {
				throw new Exception("The reply of operation "+operation+" does not comply to the operation's output type");
			} else {
				String payload = jsonMapper.writeValueAsString(content);
		        jmsMsg.setText(payload);
			}
		}
		
        int code = muleMsg.getOutboundProperty(JasperConstants.STATUS_CODE, 200);
        jmsMsg.setIntProperty(JasperConstants.STATUS_CODE, code);
        
        return jmsMsg;
	}

}

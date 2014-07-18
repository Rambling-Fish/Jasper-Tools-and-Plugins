package com.coralcea.jasper.connector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.codehaus.jackson.map.ObjectMapper;
import org.mule.api.MuleMessage;
import org.mule.transport.NullPayload;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;


public class JasperReceiver {

	private Queue queue;
	private Session session;
	private MessageConsumer consumer;
	private MessageListener listener;
	private ObjectMapper jsonMapper;
	private Resource data;
	private String rule;
	private boolean isMultiValued;
	private Class<?> parameterType, dataType;
	private boolean isMetaDefined;
	private boolean subscribe;
	private String operationURI;
	private String destination;
	
	public JasperReceiver() {
		this(null, false);
	}
	
	public JasperReceiver(String destination, boolean isMetaDefined) {
		this.destination = destination;
		this.isMetaDefined = isMetaDefined; 
		this.jsonMapper = new ObjectMapper();
	}

	public void start(JasperConnection connection) throws Exception {
		session = connection.createSession();
		if (destination == null) {
			queue = session.createTemporaryQueue();
		} else if (isMetaDefined == false) {
			queue = session.createQueue(destination);
		} else {
			JasperMetadata meta = connection.getMetadata();
			OntResource operation = meta.get(destination);
    		if (operation == null)
    			throw new IllegalArgumentException("resource "+operation+" is not defined in the DTA model");
    		else if (meta.isOfType(operation, JasperConstants.DTA_Request) && !meta.isOfKind(operation, JasperConstants.DTA_Subscribe))
    			throw new IllegalArgumentException("request "+operation+" cannot be associated with an inbound endpoint");
    		else if (meta.isOfKind(operation, JasperConstants.DTA_Publish))
    			throw new IllegalArgumentException("operation "+operation+" cannot be associated with an inbound endpoint");
    		subscribe = !meta.isOfType(operation, JasperConstants.DTA_Operation); 
    		if (subscribe) {
	   			parameterType = meta.getOperationDataClass(operation);
				dataType = null;
				data = meta.getData(operation);
				rule = meta.getRule(operation);
				isMultiValued = meta.hasMultivaluedData(operation);
    		} else { // get/post
	   			parameterType = meta.getOperationParameterClass(operation);
				dataType = meta.getOperationDataClass(operation);
    		}
    		operationURI = operation.getURI();
			queue = session.createQueue(meta.getDestination(operation));
		}
		consumer = session.createConsumer(queue);
		if (listener != null)
			consumer.setMessageListener(listener);
		if (subscribe)
			subscribe(-1/* no expiry */);
	}
	
	public void stop() throws Exception {
		if (subscribe)
			subscribe(0/* unsubscribe */);
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
			payload = jsonMapper.readValue(jmsMsg.getText(), parameterType);
		} catch (Exception e) {
			throw new Exception("Invalid input passed to operation "+operationURI, e);
		}

		MuleMessage muleMsg = context.toMuleMessage(jmsMsg);
		muleMsg.setPayload(payload);
		return muleMsg;
	}

	public TextMessage convertOutgoingMessage(JasperContext context, MuleMessage muleMsg) throws Exception {
		Object content = muleMsg.getPayload();

		TextMessage jmsMsg = context.toJMSMessage(muleMsg, session);

		if (dataType != null && content != null && !(content instanceof NullPayload)) {
			if (dataType.isInstance(content)) {
				String payload = jsonMapper.writeValueAsString(content);
		        jmsMsg.setText(payload);
			} else
				throw new Exception("The reply of operation "+operationURI+" does not comply to the operation's data type: "+content);
		}
		
        int code = muleMsg.getOutboundProperty(JasperConstants.STATUS_CODE, 200);
        jmsMsg.setIntProperty(JasperConstants.STATUS_CODE, code);
        
        return jmsMsg;
	}

	public void subscribe(int expiry) throws Exception {
		JasperRequest req = new JasperRequest();
		req.setVersion(JasperConstants.VERSION);
		req.setMethod(JasperConstants.DTA_Subscribe.getLocalName().toUpperCase());
		req.setRuri(data.getURI());
		req.setRule(rule);
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(JasperConstants.CONTENT_TYPE, JasperConstants.JSON);
		headers.put(JasperConstants.RESPONSE_TYPE, JasperConstants.JSON);
		headers.put(JasperConstants.PROCESSING_SCHEME, isMultiValued? JasperConstants.AGGREGATE : JasperConstants.COALESCE);
		headers.put(JasperConstants.EXPIRES, String.valueOf(expiry));//no expiry yet
		req.setHeaders(headers);
		req.setParameters(null);//no parameters yet
				
		String payload = jsonMapper.writeValueAsString(req);
		
        TextMessage msg = session.createTextMessage();
		msg.setJMSCorrelationID(UUID.randomUUID().toString());
		msg.setJMSReplyTo(queue);// send the data on the receiver's queue
		msg.setText(payload);
        
		Queue q = session.createQueue(JasperConstants.GLOBAL_QUEUE);
		MessageProducer producer = session.createProducer(q);
		producer.send(msg);
	}
}

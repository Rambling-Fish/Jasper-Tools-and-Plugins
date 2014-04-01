package com.coralcea.jasper.connector;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.mule.DefaultMuleEventContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class JasperSender {

	private OntResource request;
	private Queue queue;
	private Session session;
	private MessageProducer producer;
	private ObjectMapper jsonMapper;
	private boolean isMultiValued;
	private Resource kind;
	private String rule;
	private Resource input, output;
	private Class<?> inputType, outputType;
	private Callable processor;
	
	public JasperSender(JasperConnection connection) throws Exception {
		this(connection, null);
	}
	
	public JasperSender(JasperConnection connection, String req) throws Exception {
		session = connection.createSession();
		if (req != null) {
			request = connection.getMetadata().get(req);
			if (request == null)
				throw new IllegalArgumentException("request "+req+" is not defined in the DTA");
			kind = connection.getMetadata().getKind(request);
			rule = connection.getMetadata().getRule(request);
			isMultiValued = connection.getMetadata().hasMultivaluedOutput(request);
			input = connection.getMetadata().getInput(request);
			output = connection.getMetadata().getOutput(request);
			inputType = connection.getMetadata().getInputTypeOfRequest(request);
			outputType = connection.getMetadata().getOutputTypeOfRequest(request);
			processor = connection.getMetadata().getCallable(request);
			queue = session.createQueue(JasperConstants.GLOBAL_QUEUE);
		}
		producer = session.createProducer(queue);
		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
		producer.setTimeToLive(30000);
		jsonMapper = new ObjectMapper();
		jsonMapper.setSerializationInclusion(Inclusion.NON_NULL);
	}

	public void send(Message msg) throws JMSException {
		producer.send(msg);
	}

	public void send(Message msg, Queue destination) throws JMSException {
		producer.send(destination, msg);
	}
	
	public ObjectMessage createObjectMessage(Serializable obj) throws JMSException {
		return session.createObjectMessage(obj);
	}

	public TextMessage createTextMessage(String s) throws JMSException {
		return session.createTextMessage(s);
	}

	public TextMessage convertOutgoingMessage(JasperContext context, MuleMessage muleMsg, int expires) throws Exception {
		Object content = muleMsg.getPayload();
		if (!inputType.isInstance(content))
			throw new Exception("Invalid input passed to request "+request+": "+content);
		
		JasperRequest req = new JasperRequest();
		req.setVersion(JasperConstants.VERSION);
		req.setMethod(kind.getLocalName().toUpperCase());
		req.setRuri(req.getMethod().equals(JasperConstants.GET) ? output.getURI() : input.getURI());
		req.setRule(rule);
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(JasperConstants.CONTENT_TYPE, JasperConstants.JSON);
		headers.put(JasperConstants.RESPONSE_TYPE, JasperConstants.JSON);
		headers.put(JasperConstants.PROCESSING_SCHEME, isMultiValued? JasperConstants.AGGREGATE : JasperConstants.MERGE);
		headers.put(JasperConstants.EXPIRES, String.valueOf(expires));
		req.setHeaders(headers);
		req.setParameters(content);
				
		String payload = jsonMapper.writeValueAsString(req);
		
        TextMessage jmsMsg = context.toJMSMessage(muleMsg, session);
        jmsMsg.setText(payload);
        return jmsMsg;
	}

	public MuleMessage convertIncomingMessage(JasperContext context, TextMessage jmsMsg) throws Exception {
		JasperResponse response;
		try {
			response = jsonMapper.readValue(jmsMsg.getText(), JasperResponse.class);
		} catch (Exception e) {
			throw new Exception("Invalid resoinse received for request "+request, e);
		}

		int code = response.getCode();
		if (code!=200) {
			String description = response.getDescription();
			throw new JasperException("The message with correlation id '" + jmsMsg.getJMSCorrelationID() + "' received a response with code <"+code+"> and description <"+description+">", code);
		}
		
		MuleMessage muleMsg = null;
		if (outputType != null && response.getHeaders().get(JasperConstants.CONTENT_TYPE).equals(JasperConstants.JSON)) {
			muleMsg = context.toMuleMessage(jmsMsg);
			Object payload = jsonMapper.readValue(new String(response.getPayload()), outputType);
			muleMsg.setPayload(payload);
		}
		return muleMsg;
	}

	public MuleMessage preprocess(MuleEvent event) throws Exception {
		MuleEventContext context = new DefaultMuleEventContext(event);
		Object payload = processor.onCall(context);
		event.getMessage().setPayload(payload);
		return event.getMessage();
	}
}

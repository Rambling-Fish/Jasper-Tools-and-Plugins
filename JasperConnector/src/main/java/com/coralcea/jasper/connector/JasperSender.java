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

	private Queue queue;
	private Session session;
	private MessageProducer producer;
	private ObjectMapper jsonMapper;
	private String requestURI;
	private Resource kind;
	private String rule;
	private boolean isMultiValued;
	private Resource parameter, data;
	private Class<?> parameterType, dataType;
	private Callable processor;
	
	public JasperSender(JasperConnection connection) throws Exception {
		this(connection, null);
	}
	
	public JasperSender(JasperConnection connection, String req) throws Exception {
		session = connection.createSession();
		if (req != null) {
			JasperMetadata meta = connection.getMetadata();
			OntResource request = meta.get(req);
			if (request == null)
				throw new IllegalArgumentException("resource "+req+" is not defined in the DTA");
    		else if (meta.isOfType(request, JasperConstants.DTA_Operation) && !meta.isOfKind(request, JasperConstants.DTA_Publish))
    			throw new IllegalArgumentException("operation "+request+" cannot be associated with an outbound endpoint");
    		else if (meta.isOfKind(request, JasperConstants.DTA_Subscribe))
    			throw new IllegalArgumentException("request "+request+" cannot be associated with an outbound endpoint");
			requestURI = request.getURI();
			kind = meta.getKind(request);
			rule = meta.getRule(request);
			isMultiValued = meta.hasMultivaluedData(request);
			parameter = meta.getParameter(request);
			data = meta.getData(request);
			parameterType = meta.getRequestParameterClass(request);
			dataType = meta.getRequestDataClass(request);
			processor = meta.getCallable(request);
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
		if (!parameterType.isInstance(content))
			throw new Exception("Invalid input passed to request "+requestURI+": "+content);
		
		JasperRequest req = new JasperRequest();
		req.setVersion(JasperConstants.VERSION);
		req.setMethod(kind.getLocalName().toUpperCase());
		req.setRuri(req.getMethod().equals(JasperConstants.GET) ? data.getURI() : parameter.getURI());
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
			throw new Exception("Invalid response received for request "+requestURI, e);
		}

		int code = response.getCode();
		if (code!=200) {
			String description = response.getDescription();
			throw new JasperException("The message with correlation id '" + jmsMsg.getJMSCorrelationID() + "' received a response with code <"+code+"> and description <"+description+">", code);
		}
		
		MuleMessage muleMsg = null;
		if (dataType != null && response.getHeaders().get(JasperConstants.CONTENT_TYPE).equals(JasperConstants.JSON)) {
			muleMsg = context.toMuleMessage(jmsMsg);
			if (response.getPayload() != null) {
				Object payload = jsonMapper.readValue(new String(response.getPayload()), dataType);
				muleMsg.setPayload(payload);
			}
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

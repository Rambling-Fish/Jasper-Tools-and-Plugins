package com.coralcea.jasper.connector.tests;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.Assert;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.module.json.JsonData;
import org.mule.tck.junit4.FunctionalTestCase;

import com.coralcea.jasper.connector.JasperConstants;
import com.coralcea.jasper.connector.JasperRequest;
import com.coralcea.jasper.connector.JasperResponse;
import com.coralcea.jasper.connector.tests.generated.HRData;
import com.coralcea.jasper.connector.tests.generated.HRDataImpl;
import com.coralcea.jasper.connector.tests.generated.HRDataReq;
import com.coralcea.jasper.connector.tests.generated.HRDataReqImpl;
import com.coralcea.jasper.connector.tests.generated.HRUpdateReq;
import com.coralcea.jasper.connector.tests.generated.HRUpdateReqImpl;
import com.coralcea.jasper.connector.tests.generated.MSData;
import com.coralcea.jasper.connector.tests.generated.PublishHRData;
import com.coralcea.jasper.connector.tests.generated.RequestMSData;
import com.coralcea.jasper.connector.tests.generated.UpdateHRData;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class JasperConnectorTest extends FunctionalTestCase {

	private static final Logger log = Logger.getLogger(JasperConnectorTest.class.getName());
	private static final String ADDRESS = "tcp://0.0.0.0:61616";
	
	private static BrokerService broker;

	@Override
	protected String getConfigResources() {
        return "mule-config.xml";
	}

	@Override
    protected MuleContext createMuleContext() throws Exception {
		broker = new BrokerService();
		broker.setUseJmx(true);
		broker.setPersistent(false);
		broker.setBrokerName("Broker_for_stubbing_activemq");
		try {
			broker.addConnector("tcp://0.0.0.0:61616");
			broker.start();
			log.info("=========Broker Started==========");
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		
		MuleContext context = super.createMuleContext();
    	context.getRegistry().registerObject(
        		MuleProperties.APP_HOME_DIRECTORY_PROPERTY, 
        		getClass().getProtectionDomain().getCodeSource().getLocation().getPath());
    	return context;
	}

    @Test
    public void testAdminMessage() throws Exception {
    	TestConnection connection = new TestConnection();
    	
		final String[] result = new String[] {""};
    	
    	Consumer consumer = new Consumer(connection, "ResponseQueue");
		consumer.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				try {
					result[0] = ((TextMessage) message).getText();
				} catch (JMSException e) {
					Assert.assertFalse(e.getMessage(), false);
				}
			}
		});
    	
    	connection.start();
		Producer producer = new Producer(connection, "jms.jasper.jasperConnectorTestDTA.0.1.jasperLab.admin.queue");
    	JasperAdminMessage message = new JasperAdminMessage(Type.ontologyManagement, Command.get_ontology);
		producer.send(producer.createObjectMessage(message, consumer.getQueue()));
    	Thread.sleep(1000);
    	connection.stop();
    	
		Model submodel = ModelFactory.createDefaultModel();
		submodel.read(new ByteArrayInputStream(result[0].getBytes()), null, JasperConstants.DTA_FORMAT);
    	Assert.assertEquals(78, submodel.listStatements().toList().size());
    }

    @Test
    public void testUpdateHRData() throws Exception
    {
    	final TestConnection connection = new TestConnection();
   	
		Consumer JasperCore = new Consumer(connection, JasperConstants.GLOBAL_QUEUE);
		JasperCore.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				try {
					final ObjectMapper mapper = new ObjectMapper();
					mapper.setSerializationInclusion(Inclusion.NON_NULL);

					String text = ((TextMessage)message).getText();
					JsonNode parameters = new JsonData(text).get("parameters");
					UpdateHRData.Parameters receivedRequest = mapper.readValue(parameters.toString(), UpdateHRData.Parameters.class);

					HRUpdateReq requestToForward = new HRUpdateReqImpl();
					requestToForward.setSid(receivedRequest.getSid());
					requestToForward.setHrData(receivedRequest.getHrData());
					
					final Queue replyTo = (Queue) message.getJMSReplyTo();
					Consumer consumer = new Consumer(connection, "Queue1Response");
					consumer.setMessageListener(new MessageListener() {
						public void onMessage(Message message) {
							try {
								Producer producer = new Producer(connection, replyTo);
						    	String text = mapper.writeValueAsString(createResonse(message));
								producer.send(producer.createTextMessage(text, message.getJMSCorrelationID(), null));
							} catch (Exception e) {
								Assert.assertFalse(e.getMessage(), false);
							}
						}
					});

					Producer producer = new Producer(connection, "Queue1");
					String payload = mapper.writeValueAsString(requestToForward);
					String correlationID = message.getJMSCorrelationID();
					producer.send(producer.createTextMessage(payload, correlationID, consumer.getQueue()));
				} catch (Exception e) {
					Assert.assertFalse(e.getMessage(), false);
				}
			}
		});
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("sid", "400");
		properties.put("bpm", "400");
		properties.put("timestamp", "400:400:400");

		UpdateHRData.Parameters request = new UpdateHRData.Parameters();
		request.setSid((String)properties.get("sid"));
		HRData hrData = new HRDataImpl();
		hrData.setBpm(Integer.valueOf((String)properties.get("bpm")));
		hrData.setTimestamp((String)properties.get("timestamp"));
		request.setHrData(hrData);

    	HRDataCache.getInstance().reset();

		connection.start();
    	runFlowAndExpect("testUpdateHRData", null, properties, request);
		Thread.sleep(1000);
    	connection.stop();

		Assert.assertEquals(HRDataCache.getInstance().get("400"), hrData);
   }

    @Test
    public void testRequestMSData() throws Exception
    {
    	final TestConnection connection = new TestConnection();
   	
		Consumer jasperCore = new Consumer(connection, JasperConstants.GLOBAL_QUEUE);
		jasperCore.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				try {
					final ObjectMapper mapper = new ObjectMapper();
					mapper.setSerializationInclusion(Inclusion.NON_NULL);
					
					String text = ((TextMessage)message).getText();
					JsonNode parameters = new JsonData(text).get("parameters");
					RequestMSData.Parameters receivedRequest = mapper.readValue(parameters.toString(), RequestMSData.Parameters.class);

					HRDataReq requestToForward = new HRDataReqImpl();
					requestToForward.setSid(receivedRequest.getSid());
					
					final Queue replyTo = (Queue) message.getJMSReplyTo();
					Consumer consumer = new Consumer(connection, "Queue2Response");
					consumer.setMessageListener(new MessageListener() {
						public void onMessage(Message message) {
							try {
								Producer producer = new Producer(connection, replyTo);
						    	String text = mapper.writeValueAsString(createResonse(message));
								producer.send(producer.createTextMessage(text, message.getJMSCorrelationID(), null));
							} catch (Exception e) {
								Assert.assertFalse(e.getMessage(), false);
							}
						}
					});
					
					Producer producer = new Producer(connection, "Queue2");
					String payload = mapper.writeValueAsString(requestToForward);
					String correlationID = message.getJMSCorrelationID();
					producer.send(producer.createTextMessage(payload, correlationID, consumer.getQueue()));
				} catch (Exception e) {
					Assert.assertFalse(e.getMessage(), false);
				}
			}
		});
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("sid", "200");

    	HRDataCache.getInstance().reset();
    	HRData hrData = HRDataCache.getInstance().get((String)properties.get("sid"));

    	connection.start();
    	runFlowAndExpect("testRequestMSData", null, properties, new MSData[]{hrData});
    	connection.stop();
    }

    @Test
    public void testPublishHRData() throws Exception
    {
    	final TestConnection connection = new TestConnection();
    	
		Consumer JasperCore = new Consumer(connection, JasperConstants.GLOBAL_QUEUE);
		JasperCore.setMessageListener(new MessageListener() {
			private final ExecutorService pool = Executors.newFixedThreadPool(2);   
			private Object lock = new Object();
	    	private String sid;
	    	private HRData hrData;
			public void onMessage(final Message message) {
				pool.execute(new Runnable() {
					public void run() {
						try {
							final ObjectMapper mapper = new ObjectMapper();
							mapper.setSerializationInclusion(Inclusion.NON_NULL);

							String text = ((TextMessage)message).getText();
							JasperRequest request = mapper.readValue(text,JasperRequest.class);
							
							if (request.getRuri().equals("http://coralcea.ca/heartratedta#hrData")) {
								JsonNode parameters = new JsonData(text).get("parameters");
								RequestMSData.Parameters receivedRequest = mapper.readValue(parameters.toString(), RequestMSData.Parameters.class);
								
								synchronized (lock) {
									String expires = request.getHeaders().get(JasperConstants.EXPIRES);
									int timeout = Integer.valueOf(expires);
									lock.wait(timeout*1000);
								}
								
								if (receivedRequest.getSid().equals(sid)) {
									final Queue replyTo = (Queue) message.getJMSReplyTo();
									Producer producer = new Producer(connection, replyTo);
							    	String response = mapper.writeValueAsString(createResonse(mapper.writeValueAsString(hrData)));
									producer.send(producer.createTextMessage(response, message.getJMSCorrelationID(), null));
								}
							} else {
								JsonNode parameters = new JsonData(text).get("parameters");
								PublishHRData.Parameters receivedRequest = mapper.readValue(parameters.toString(), PublishHRData.Parameters.class);
								sid = receivedRequest.getSid();
								hrData = receivedRequest.getHrData();
								synchronized (lock) {
									lock.notifyAll();
								}
							}
						} catch (Exception e) {
							Assert.assertFalse(e.getMessage(), false);
						}
					}
				});
			}
		});
		
		final List<Throwable> errors = new ArrayList<Throwable>();
		
		Thread publisher = new Thread(new Runnable() {
			public void run() {
		    	Map<String, Object> properties = new HashMap<String, Object>();
				properties.put("sid", "400");
				properties.put("bpm", "400");
				properties.put("timestamp", "400:400:400");

				PublishHRData.Parameters request = new PublishHRData.Parameters();
				request.setSid((String)properties.get("sid"));
				HRData hrData = new HRDataImpl();
				hrData.setBpm(Integer.valueOf((String)properties.get("bpm")));
				hrData.setTimestamp((String)properties.get("timestamp"));
				request.setHrData(hrData);

				try {
					runFlowAndExpect("testPublishHRData", null, properties, request);
				} catch (Exception e) {
					errors.add(e);
				}
			}
		});

		Thread client = new Thread(new Runnable() {
			public void run() {
				Map<String, Object> properties = new HashMap<String, Object>();
				properties.put("sid", "400");

				HRData hrData = new HRDataImpl();
				hrData.setBpm(400);
				hrData.setTimestamp("400:400:400");

				try {
			    	runFlowAndExpect("testRequestHRData", null, properties, hrData);
				} catch (Exception e) {
					errors.add(e);
				}
			}
		});
		
    	connection.start();
		client.start();
		Thread.sleep(1000);
		publisher.start();
		client.join();
    	connection.stop();
    	
    	if (!errors.isEmpty())
    		Assert.assertTrue(errors.get(0).getMessage(), false);
    }

    @Test
    public void testRequestBpm() throws Exception
    {
    	final TestConnection connection = new TestConnection();
   	
		Consumer jasperCore = new Consumer(connection, JasperConstants.GLOBAL_QUEUE);
		jasperCore.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				try {
					final ObjectMapper mapper = new ObjectMapper();
					mapper.setSerializationInclusion(Inclusion.NON_NULL);

					String text = ((TextMessage)message).getText();
					JsonNode parameters = new JsonData(text).get("parameters");
					RequestMSData.Parameters receivedRequest = mapper.readValue(parameters.toString(), RequestMSData.Parameters.class);

					HRDataReq requestToForward = new HRDataReqImpl();
					requestToForward.setSid(receivedRequest.getSid());
					
					final Queue replyTo = (Queue) message.getJMSReplyTo();
					Consumer consumer = new Consumer(connection, "Queue3Response");
					consumer.setMessageListener(new MessageListener() {
						public void onMessage(Message message) {
							try {
								Producer producer = new Producer(connection, replyTo);
						    	String text = mapper.writeValueAsString(createResonse(message));
								producer.send(producer.createTextMessage(text, message.getJMSCorrelationID(), null));
							} catch (Exception e) {
								Assert.assertFalse(e.getMessage(), false);
							}
						}
					});
					
					Producer producer = new Producer(connection, "Queue3");
					String payload = mapper.writeValueAsString(requestToForward);
					String correlationID = message.getJMSCorrelationID();
					producer.send(producer.createTextMessage(payload, correlationID, consumer.getQueue()));
				} catch (Exception e) {
					Assert.assertFalse(e.getMessage(), false);
				}
			}
		});
		
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("sid", "600");

    	HRDataCache.getInstance().reset();

    	connection.start();
    	runFlowAndExpect("testRequestBpm", null, properties, 300);
    	connection.stop();
    }

    /*****************************************************************************/
  
    private JasperResponse createResonse(Message message) throws Exception {
		JasperResponse response = new JasperResponse();
		response.setVersion(JasperConstants.VERSION);
    	response.setCode(message.getIntProperty(JasperConstants.STATUS_CODE));
    	response.setDescription(message.getStringProperty(JasperConstants.STATUS_DESCRIPTION));
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(JasperConstants.CONTENT_TYPE, JasperConstants.JSON);
		response.setHeaders(headers);	
		if (((TextMessage)message).getText()!=null)
			response.setPayload(((TextMessage)message).getText().getBytes());
		return response;
    }
    
    private JasperResponse createResonse(String payload) throws Exception {
		JasperResponse response = new JasperResponse();
		response.setVersion(JasperConstants.VERSION);
    	response.setCode(200);
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(JasperConstants.CONTENT_TYPE, JasperConstants.JSON);
		response.setHeaders(headers);	
		response.setPayload(payload.getBytes());
		return response;
    }

    protected <T, U> void runFlowAndExpect(String flowName, U payload, Map<String, Object> properties, T expect) throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        MuleEvent event = getTestEvent(payload);
        event.getMessage().addProperties(properties, PropertyScope.INBOUND);
        MuleEvent responseEvent = flow.process(event);
        Object response = responseEvent.getMessage().getPayload();

        if (expect == null)
        	Assert.assertNull(response);
        else if (expect.getClass().isArray())
        	Assert.assertTrue(Arrays.equals((Object[])expect, (Object[])response));
        else
        	Assert.assertTrue(expect.equals(response));
    }

     /********************************************************/
    private class TestConnection {
    	
    	protected Connection connection;
    	
    	public TestConnection() throws Exception {
       		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ADDRESS);
    		connection = connectionFactory.createConnection("", "");
    	}
    	
    	public Session createSession() throws Exception {
    		return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    	}
    	
    	public void start() throws Exception {
    		connection.start();
    	}
    	
    	public void stop() throws Exception {
			connection.stop();
			connection.close();
    	}
    }
    
    private class Producer {
    	
    	private Session session;
    	private MessageProducer producer;
    	
    	public Producer(TestConnection c, String queuename) throws Exception {
    		session = c.createSession();
    		producer  = session.createProducer(session.createQueue(queuename));
    		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    		producer.setTimeToLive(30000);
    	}

    	public Producer(TestConnection c, Queue queue) throws Exception {
    		session = c.createSession();
    		producer  = session.createProducer(queue);
    		producer.setDeliveryMode(DeliveryMode.PERSISTENT);
    		producer.setTimeToLive(30000);
    	}

    	public void send(Message m) throws Exception {
    		producer.send(m);
    	}

    	public TextMessage createTextMessage(String text, String correlationID, Destination replyTo) throws Exception {
    		TextMessage msg = session.createTextMessage(text);
    		msg.setJMSCorrelationID(correlationID);
    		msg.setJMSReplyTo(replyTo);
    		return msg;
    	}
    	
    	public ObjectMessage createObjectMessage(Serializable o, Destination replyTo) throws Exception {
    		ObjectMessage msg = session.createObjectMessage();
	        msg.setObject(o);
	        msg.setJMSCorrelationID(UUID.randomUUID().toString());
	        msg.setJMSReplyTo(replyTo);
    		return msg;
    	}
 }

    private class Consumer {
    	
    	private MessageConsumer consumer;
    	private Queue queue;
    	
    	public Consumer(TestConnection c, String queuename) throws Exception {
    		Session s = c.createSession();
    		queue = s.createQueue(queuename);
    		consumer  = s.createConsumer(queue);
    	}
    	
    	public Queue getQueue() {
    		return queue;
    	}
    	
    	public void setMessageListener(MessageListener listener) throws Exception {
    		consumer.setMessageListener(listener);
    	}
    }
}

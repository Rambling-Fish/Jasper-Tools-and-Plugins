package com.coralcea.jasper.connector;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.ConnectionException;
import org.mule.api.ConnectionExceptionCode;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.ConnectionIdentifier;
import org.mule.api.annotations.Connector;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Source;
import org.mule.api.annotations.SourceThreadingModel;
import org.mule.api.annotations.ValidateConnection;
import org.mule.api.annotations.display.FriendlyName;
import org.mule.api.annotations.display.Placement;
import org.mule.api.annotations.display.Summary;
import org.mule.api.annotations.expressions.Lookup;
import org.mule.api.annotations.lifecycle.Start;
import org.mule.api.annotations.lifecycle.Stop;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.api.callback.SourceCallback;
import org.mule.api.callback.StopSourceCallback;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.ReplyToHandler;
import org.mule.session.DefaultMuleSession;

/**
 * Jasper Connector
 *
 * @author Coral CEA
 */
@Connector(name="jasper", schemaVersion="1.0", friendlyName="Jasper", minMuleVersion="3.4")
public class JasperConnector implements MuleContextAware
{
    /**
     * The URL of Jasper
     */
    @Configurable
    @Optional @Default(value="tcp://0.0.0.0:61616")
    @Placement(tab="General", group="Connection", order=0)
    @FriendlyName(value="Jasper URL")
    @Summary(value="The URL of Jasper")
    private String url;

    /**
     * The vendor's name
     */
    @Configurable
    @Placement(tab="General", group="Connection", order=1)
    @FriendlyName(value="Vendor")
    @Summary(value="The vendor's name")
    private String vendor;

    /**
     * The application's name
     */
    @Configurable
	@Placement(tab="General", group="Connection", order=2)
    @FriendlyName(value="Application")
    @Summary(value="The application's name")
    private String application;

    /**
     * The application's version
     */
    @Configurable
    @Placement(tab="General", group="Connection", order=3)
    @FriendlyName(value="Version")
    @Summary(value="The application's version")
    private String version;

    /**
     * The application's version
     */
    @Configurable
    @Placement(tab="General", group="DTA", order=3)
    @FriendlyName(value="Model file")
    @Summary(value="The DTA's model file (e.g., xyz.dta)")
    private String model;

    /**
     * The error logger
     */
	protected Logger logger = Logger.getLogger(getClass());

	/**
	 * The context
	 */
	private JasperContext context;
	
    /**
     * The JMS connection to Jasper
     */
    private JasperConnection connection;

    /**
     * The request senders
     */
	private Map<String, JasperSender> requestSenders= new HashMap<String, JasperSender>();

    /**
     * The provide receivers
     */
	private Map<String, JasperReceiver> provideReceivers = new HashMap<String, JasperReceiver>();
	
    /**
     * The reply sender
     */
	private JasperSender replySender;

    /**
     * The reply receiver
     */
	private JasperReceiver replyReceiver;

    /**
     * The responses (of the sent requests)
     */
	private Map<String,Message> responses = new HashMap<String, Message>();

    /**
     * The locks (for the request senders to wait on)
     */
	private Map<String,Object> locks = new HashMap<String, Object>();

	/**
	 * The exchange pattern between connector and Jasper 
	 */
	public enum ExchangePattern {
		@FriendlyName("One Way")one_way,
		@FriendlyName("Request Response")request_response
	}
	
	/**
     * Set URL property
     *
     * @param url URL
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * Get URL property
     * 
     * @return url
     */
    public String getUrl() {
		return url;
	}

    /**
     * Set Vendor property
     *
     * @param vendor Vendor
     */
    public void setVendor(String vendor)
    {
        this.vendor = vendor;
    }

    /**
     * Get Vendor property
     * 
     * @return vendor
     */
    public String getVendor() {
		return vendor;
	}

    /**
     * Set Application property
     *
     * @param application Application
     */
    public void setApplication(String application)
    {
        this.application = application;
    }

    /**
     * Get Application property
     * 
     * @return application
     */
    public String getApplication() {
		return application;
	}

    /**
     * Set Version property
     *
     * @param version Version
     */
    public void setVersion(String version)
    {
        this.version = version;
    }

    /**
     * Get Version property
     * 
     * @return version
     */
    public String getVersion() {
		return version;
	}
    
    /**
     * Get Model property
     * 
     * @return model
     */
	public String getModel() {
		return model;
	}

    /**
     * Set Model property
     *
     * @param model Model
     */
	public void setModel(String model) {
		this.model = model;
	}

	@Override
	public void setMuleContext(MuleContext muleContext) {
		this.context = new JasperContext(muleContext);
	}    	

    @Start
    public void connect() throws ConnectionException {
		connection = new JasperConnection(url, vendor, application, version);
		
    	try {
    		connection.validate();
    	} catch (Throwable e) {
    		connection = null;
     		logger.error("Error validating DTA license", e);
    		throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, null, "Error validating DTA license", e);
    	}
		
    	try {
    		String appHomeName = context.getValue(MuleProperties.APP_HOME_DIRECTORY_PROPERTY);
    		connection.configure(appHomeName+"/"+model);
    	} catch (Throwable e) {
    		connection = null;
     		logger.error("Error configuring DTA connection", e);
    		throw new ConnectionException(ConnectionExceptionCode.INCORRECT_CREDENTIALS, null, "Error configuring DTA connection", e);
    	}

    	try {
    		connection.open();
    	} catch (Throwable e) {
     		disconnect();
     		logger.error("Error opening a connection to Jasper", e);
    		throw new ConnectionException(ConnectionExceptionCode.CANNOT_REACH, null, "Error opening a connection to Jasper", e);
    	}
    	
    	try {
			for(JasperReceiver provideReceiver : provideReceivers.values())
				provideReceiver.start(connection);
    	} catch (Throwable e) {
     		disconnect();
     		logger.error("Error starting the connection receivers", e);
    		throw new ConnectionException(ConnectionExceptionCode.UNKNOWN, null, "Error starting the connection receivers", e);
    	}
    }

    @Stop
    public void disconnect() {
    	try {
    		connection.close();
    	} catch (Exception e) {
    		logger.error("Error closing the connection to Jasper", e);
    	}
		connection = null;
    }

    @ValidateConnection
    public boolean isConnected() {
    	return (connection == null) ? connection.isOpen() : false;
    }

    @ConnectionIdentifier
    public String connectionId() {
    	return (connection == null) ? connection.getId() : null;
    }

    /**
     * Send
     *
     * {@sample.xml ../../../doc/jasper-connector.xml.sample jasper:send}
     *
     * @param request A given get/post request 
     * @param exchangePattern The exchange pattern between the connector and Jasper
     * @param timeout The timeout in seconds to wait for a response to the sent request
     * @param muleEvent The mule event
     * @return Response The MuleMessage to forward to the next processor
     * @throws Exception if any error occurs
     */
    @Processor(friendlyName="Send")@Inject
    public Object send(@Placement(group="Parameters")@FriendlyName("Request URI") final String request,
    				   @Placement(group="Parameters")@FriendlyName("Exchange Pattern")@Optional@Default("request_response") ExchangePattern exchangePattern,
    				   @Placement(group="Parameters")@FriendlyName("Timout")@Optional@Default("10") int timeout,
    				   @Lookup MuleEvent muleEvent) throws Exception
    {
    	JasperSender requestSender = requestSenders.get(request);
    	if (requestSender == null) {
    		requestSender = new JasperSender(connection, request);
    		requestSenders.put(request, requestSender);
    	}

    	if (replyReceiver == null) {
    		replyReceiver = new JasperReceiver();
    		replyReceiver.setMessageListener(new MessageListener() {
				public void onMessage(Message msg) {
					try {
						logger.info("A reply message is received by operation: "+request);
						if(msg.getJMSCorrelationID() == null)
							logger.error("Jasper response message recieved with null JMSCorrelationID, ignoring message.");
						else if (!locks.containsKey(msg.getJMSCorrelationID()))
							logger.error("response with correlationID = " + msg.getJMSCorrelationID() + " recieved however no record of sending message with this ID, ignoring");
						else {
							responses.put(msg.getJMSCorrelationID(), msg);
							Object lock = locks.remove(msg.getJMSCorrelationID());
							synchronized (lock) {
								lock.notifyAll();
							}
						}
					} catch (JMSException e) {
						logger.error("Exception when storing response recieved in onMessage",e);
					}
				}
			});
    		replyReceiver.start(connection);
    	}
	    
        MuleMessage muleMessage = requestSender.preprocess(muleEvent);
		Message jmsMessage = requestSender.convertOutgoingMessage(context, muleMessage, timeout);
		jmsMessage.setJMSReplyTo(replyReceiver.getQueue());
		
		logger.info("A message is sent from operation: "+request);
		if (exchangePattern == ExchangePattern.request_response) {
	        String correlationID = jmsMessage.getJMSCorrelationID();
			Message responseMsg = null;
	
			Object lock = new Object();
			synchronized (lock) {
				locks.put(correlationID, lock);
				requestSender.send(jmsMessage);
				lock.wait(timeout*1000);
			    responseMsg = responses.remove(correlationID);
			}
			
			if(responseMsg == null)
				throw new JMSException("Timeout waiting for response to JMSCorrelationID : " + correlationID);			
			else if (!(responseMsg instanceof TextMessage))
				throw new JMSException("Response was not a TextMessage for JMSCorrelationID : " + correlationID);
			else {
				MuleMessage forwardMessage = requestSender.convertIncomingMessage(context, (TextMessage)responseMsg);
				if (forwardMessage != null)
					muleMessage = forwardMessage;
			}
        } else
			requestSender.send(jmsMessage);
		
		return muleMessage;
    }

    /**
     * Publish
     *
     * {@sample.xml ../../../doc/jasper-connector.xml.sample jasper:publish}
     *
     * @param operation A given publish operation 
     * @param muleEvent The mule event
     * @return Response The MuleMessage to forward to the next processor
     * @throws Exception if any error occurs
     */
    @Processor(friendlyName="Publish")@Inject
    public Object publish(@Placement(group="Parameters")@FriendlyName("Operation URI") final String operation,
    				   @Lookup MuleEvent muleEvent) throws Exception
    {
    	return send(operation, ExchangePattern.one_way, 0, muleEvent);
    }
    
    /**
     * Execute
     *
     * {@sample.xml ../../../doc/jasper-connector.xml.sample jasper:execute}
     *
     * @param operation A given get/post operation
     * @param callback The message processor to forward messages received to
     * @throws Exception if any error occurs
     */
    @Source(friendlyName="Execute", exchangePattern=MessageExchangePattern.REQUEST_RESPONSE, threadingModel=SourceThreadingModel.NONE)
    public StopSourceCallback execute(@Placement(group="Parameters")@FriendlyName("Operation URI") final String operation,
    		                          final SourceCallback callback) throws Exception
    {
    	final JasperReceiver provideReceiver = new JasperReceiver(operation, true);
    	provideReceivers.put(operation, provideReceiver);
    	
    	final ReplyToHandler replyToHandler = new ReplyToHandler() {
			public void processReplyTo(MuleEvent event, MuleMessage replyMessage, Object replyTo) throws MuleException {
				try {
					if (replySender == null)
						replySender = new JasperSender(connection);
					Message jmsMessage = provideReceiver.convertOutgoingMessage(context, replyMessage);
					logger.info("A reply message is sent from operation: "+operation);
		            replySender.send(jmsMessage, (Queue)replyTo);
				} catch (Exception e) {
					logger.error("Error forwarding the reply message from the flow", e);
				}
			}
		};
    	
    	provideReceiver.setMessageListener(new MessageListener() {
			public void onMessage(Message msg) {
				try {
					logger.info("A message is received by operation: "+operation);
					if(msg.getJMSCorrelationID() == null)
						throw new JMSException("Recieved a message from Jasper with null JMSCorrelationID");			
					else if (!(msg instanceof TextMessage))
						throw new JMSException("Received message from Jasper was not a TextMessage for JMSCorrelationID : " + msg.getJMSCorrelationID());			
					else {
				        MuleMessage muleMessage = provideReceiver.convertIncomingMessage(context, (TextMessage)msg);
						FlowConstruct flowConstruct = getFlowConstruct(callback);
						Object replyTo = msg.getJMSReplyTo();
						if (replyTo!=null)
							callback.processEvent(createMuleEvent(muleMessage, flowConstruct, replyTo, replyToHandler));
						else
							callback.processEvent(createMuleEvent(muleMessage, flowConstruct, null, null));
					}
				} catch (Exception e) {
					logger.error("Error forwarding received message to the flow", e);
				}
			}
		});
 
		return new StopSourceCallback() {
			public void stop() throws Exception {
				provideReceiver.stop();
			}
		};
    }

    /**
     * Subscribe
     *
     * {@sample.xml ../../../doc/jasper-connector.xml.sample jasper:subscribe}
     *
     * @param request A given subscribe request 
     * @param callback The message processor to forward messages received to
     * @throws Exception if any error occurs
     */
    @Source(friendlyName="Subscribe", exchangePattern=MessageExchangePattern.REQUEST_RESPONSE, threadingModel=SourceThreadingModel.NONE)
    public StopSourceCallback subscribe(@Placement(group="Parameters")@FriendlyName("Request URI") final String request,
    		                          final SourceCallback callback) throws Exception
    {
    	return execute(request, callback);
    }
    
	private MuleEvent createMuleEvent(MuleMessage msg, FlowConstruct flowConstruct, Object replyTo, ReplyToHandler replyToHandler) {
		return new DefaultMuleEvent(msg, 
			URI.create(""), "", MessageExchangePattern.ONE_WAY,
			flowConstruct, new DefaultMuleSession(), 10000, null, 
			null, msg.getEncoding(), false, true, replyTo, replyToHandler);
    }

	/**
	 *  This is a hack for getting the flow construct since there is no other supported way to get it in devkit
	 */
	private FlowConstruct getFlowConstruct(SourceCallback callback) {
		FlowConstruct flowConstruct = null;
		try {
			Method m = callback.getClass().getMethod("getFlowConstruct");
			flowConstruct = (m != null) ? (FlowConstruct) m.invoke(callback) : null;
		} catch (Exception e) {
			logger.error("Error getting flow construct from callback", e);
		}
		return flowConstruct;
	}
}

package com.coralcea.jasper.connector;

import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.AuthenticationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

public class JasperConnection {

	private String url;

    private String vendor;
	
	private String application;
	
	private String version;
	
	private String id;
	
	private ClientLicense license;

	private Connection connection;
	
	private JasperMetadata metadata;
	
	private JasperSender adminSender;

	private JasperReceiver adminReceiver;

	protected Logger logger = Logger.getLogger(getClass());

	public JasperConnection(String url, String vendor, String application, String version) {
    	this.url = url;
		this.vendor = vendor;
		this.application = application;
		this.version = version;
	}

	public String getVendor() {
		return vendor;
	}

	public String getApplication() {
		return application;
	}

	public String getVersion() {
		return version;
	}

    public String getId() {
       	return id;
    }
    
    public JasperMetadata getMetadata() {
    	return metadata;
    }
    
	public void validate() throws AuthenticationException, IOException {
		String keyFile = System.getProperty("jta-keystore");
		keyFile += "/" + vendor + "_" + application + "_" + version;
		keyFile += JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX;
		
		license = JAuthHelper.loadClientLicenseFromFile(keyFile);
			
		if (!license.getVendor().equals(vendor) || 
	    	!license.getAppName().equals(application) ||
	    	!license.getVersion().equals(version))
			throw new AuthenticationException("Invalid license key for the DTA"); 
			
	    if (licenseExpiresInDays(license, 0))
			throw new AuthenticationException("Expired license key for the DTA"); 
			
		id = license.getDeploymentId();
   	}

	public void configure(String dtaName) throws Exception {
		metadata = new JasperMetadata(dtaName);
	}
	
	public void open() throws Exception {
		logger.info("Trying to connect to Jasper at: "+url);
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("failover://("+url+")");
		connection = connectionFactory.createConnection(getUsername(), getPassword());
	    connection.start();
		logger.info("Connected to Jasper at: "+url);

	    adminSender = new JasperSender(this);
		adminReceiver = new JasperReceiver(getAdminEndpoint(), false);
		adminReceiver.setMessageListener(new MessageListener() {
			public void onMessage(Message message) {
				try {
			        if (!(message instanceof ObjectMessage)) {
	                	logger.warn("Received JMSMessage that wasn't an ObjectMessage, ignoring : " + message);
	                	return;
			        }
			        
		        	Object obj = ((ObjectMessage) message).getObject();
		        	if(!(obj instanceof JasperAdminMessage)) {
	                	logger.warn("Received ObjectMessage that wasn't a JasperAdminMessage, ignoring : " + obj);
	                	return;
		        	}
		        		
	        		JasperAdminMessage adminMessage = (JasperAdminMessage) obj;
					if (adminMessage.getType() != Type.ontologyManagement || adminMessage.getCommand() != Command.get_ontology) {
						logger.warn("Received JasperAdminMessage that isn't supported, ignoring : " + obj);
						return;
					}
						
        			String statements = metadata.serializeRelevantModelSubset();
        			Message response = adminSender.createTextMessage(statements);
        			response.setJMSCorrelationID(message.getJMSCorrelationID());
        			adminSender.send(response, (Queue)message.getJMSReplyTo());
				} catch (Exception e) {
					logger.error("Error when sending response to admin message",e);
				}		
			}
		});
		adminReceiver.start(this);
	}
    
    public void close() throws JMSException {
    	if (connection != null)
    		connection.close();
    	connection = null;
    }
    
    public boolean isOpen() {
    	return connection != null;
    }
    
    public Session createSession() throws JMSException {
    	return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }
   
    private boolean licenseExpiresInDays(ClientLicense license, int days) {
		if(license.getExpiry() == null)
			return false;
		Calendar currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		currentTime.add(Calendar.DAY_OF_YEAR, days);
		return currentTime.after(license.getExpiry());
	}

    private String getUsername() {
		return vendor+":"+application+":"+version+":"+id;
	}
	
    private String getPassword() {
		return JAuthHelper.bytesToHex(license.getLicenseKey());
	}
	
    private String getAdminEndpoint() {
		return "jms."+vendor+"."+application+"."+version+"."+id+".admin.queue";
	}
    
}

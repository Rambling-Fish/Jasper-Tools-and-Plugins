package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.AuthenticationException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.jasper.jLib.jAuth.ClientLicense;
import org.jasper.jLib.jAuth.util.JAuthHelper;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Command;
import org.jasper.jLib.jCommons.admin.JasperAdminMessage.Type;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.AddPropertyCommand;
import com.coralcea.jasper.tools.dta.commands.ChangeImportLoadCommand;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class DTADownloadJasperModelDialog extends Dialog {

	private static final String JASPER_SEVER ="tcp://0.0.0.0:61616";
	private static final String JASPER_FILE = "Jasper.dta";
	private static final String JASPER_NS = "http://coralcea.ca/jasper";
	private static final String JASPER_GLOBAL_QUEUE = "jms.jasper.delegate.global.queue";
	
	protected DTAEditor editor;
	protected String title;
	protected String prompt;
	
	protected Text url, vendor, application, version;
	
	private static class Credentials {
		protected String url, vendor, application, version;
	}

	public static void run(Shell shell, DTAEditor editor) {
		String title = "Download Jasper Model";
		String prompt = "Enter the Jasper DTA credentials:";
		new DTADownloadJasperModelDialog(shell, title, prompt, editor).open();
	}

	protected DTADownloadJasperModelDialog(Shell shell, String title, String prompt, DTAEditor editor) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		this.editor = editor;
		this.title = title;
		this.prompt = prompt;
	}

	@Override
	protected void okPressed() {
		final Credentials credentials = saveCredentials();
		super.okPressed();
		try {
			new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						monitor.beginTask("Downloading Japser model", 10);
						if (updateJasperModel(credentials, new SubProgressMonitor(monitor, 7))) {
							updateImportPolicy(new SubProgressMonitor(monitor, 2));
							updateDTAModel(new SubProgressMonitor(monitor, 1));
						}
					} catch (Exception e) { 
						Activator.getDefault().log("Error ading Jasper model", e);
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error downloading Jasper model", e);
						StatusManager.getManager().handle(status, StatusManager.BLOCK);
					} finally {
						monitor.done();
					}
				}
			});
		} catch (Exception e) {
			Activator.getDefault().log("Failed to download Jasper model", e);
		}
	}

	protected Credentials loadCredentials() {
		Credentials credentials = new Credentials();
		IFile file = editor.getFile();
		
		try {
			QualifiedName name = new QualifiedName(DTA.URI, "jasperURL");
			String url = (String) file.getPersistentProperty(name);
			credentials.url = (url != null) ? url : JASPER_SEVER;
	
			name = new QualifiedName(DTA.URI, "jasperVendor");
			String vendor = (String) file.getPersistentProperty(name);
			credentials.vendor = (vendor != null)? vendor : "";
	
			name = new QualifiedName(DTA.URI, "jasperApplication");
			String application = (String) file.getPersistentProperty(name);
			credentials.application = (application != null)? application : "";
	
			name = new QualifiedName(DTA.URI, "jasperVersion");
			String version = (String) file.getPersistentProperty(name);
			credentials.version = (version != null)? version : "";
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to load jasper properties from DTA file", e);
		}
		
		return credentials;
	}
	
	protected Credentials saveCredentials() {
		Credentials credentials = new Credentials();
		IFile file = editor.getFile();
		
		try {
			credentials.url = url.getText();
			QualifiedName name = new QualifiedName(DTA.URI, "jasperURL");
			file.setPersistentProperty(name, credentials.url);
	
			credentials.vendor = vendor.getText();
			name = new QualifiedName(DTA.URI, "jasperVendor");
			file.setPersistentProperty(name, credentials.vendor);
	
			credentials.application = application.getText();
			name = new QualifiedName(DTA.URI, "jasperApplication");
			file.setPersistentProperty(name, credentials.application);
	
			credentials.version = version.getText();
			name = new QualifiedName(DTA.URI, "jasperVersion");
			file.setPersistentProperty(name, credentials.version);
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to load jasper properties from DTA file", e);
		}

		return credentials;
	}

	protected boolean updateJasperModel(Credentials credentials, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Downloading Japser model", 8);
		String[] submodels = getJasperSubModels(credentials, new SubProgressMonitor(monitor, 6));
		if (submodels == null)
			return false;
		
		OntModel model = DTACore.createNewModel();
		Ontology ont = model.createOntology(JASPER_NS);
		ont.setPropertyValue(DTA.isLibrary, model.createTypedLiteral(true));
		
		Map<String, String> nsPrefixMap = new HashMap<String, String>();
		
		for (String submodel : submodels) {
			Model m = ModelFactory.createDefaultModel();
			m.read(new ByteArrayInputStream(submodel.getBytes()), null, DTA.FORMAT);
			model.add(m);
			
			Map<String, String> nsPrefixes = m.getNsPrefixMap();
			String uri = nsPrefixes.remove("");
			Set<Resource> ontology = DTAUtilities.listSubjects(RDF.type, OWL.Ontology);
			if (ontology != null && !ontology.isEmpty())
				nsPrefixes.put(ontology.iterator().next().getLocalName(), uri);
			nsPrefixMap.putAll(nsPrefixes);
		}
		
		model.setNsPrefixes(nsPrefixMap);
		
		IFile file = editor.getFile().getParent().getFile(Path.fromOSString(JASPER_FILE));
		DTACore.saveModel(model, file, true, monitor);
		return true;
	}

	protected String[] getJasperSubModels(Credentials credentials, IProgressMonitor monitor) {
		monitor.beginTask("Getting Jasper model", 10);
		ClientLicense license = null;
		Connection connection = null;
		
		try {
			license = getJasperLicense(credentials);
			monitor.worked(1);
			connection = connectToJasper(license, credentials);
			monitor.worked(4);
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error connecting to the Jasper server", e);
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			monitor.done();
			return null;
		}
		
		String[] submodels = downloadJasperSubModels(license, connection);
		monitor.worked(4);
		
		try {
			connection.close();
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error disconnecting from the Jasper server", e);
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
		}
		
		monitor.done();
		return submodels;
	}
	
	protected String[] downloadJasperSubModels(ClientLicense license, Connection connection) {
		Queue adminQueue;
		final String correlationID = UUID.randomUUID().toString();
		final Object[] submodels = new Object[1];
		
		try {
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			String adminEndpoint = license.getAdminQueue();
			adminQueue = session.createQueue(adminEndpoint);
			MessageConsumer consumer = session.createConsumer(adminQueue);
			consumer.setMessageListener(new MessageListener() {
				public void onMessage(Message msg) {
					try {
						if (correlationID.equals(msg.getJMSCorrelationID())) {
							if (msg instanceof ObjectMessage) {
								Object payload = ((ObjectMessage) msg).getObject();
								if (payload instanceof String[]) {
									submodels[0] = (String[])payload;
									synchronized (correlationID) {
										correlationID.notifyAll();
									}
								}
							}
						}
					} catch (Exception e) {
						Activator.getDefault().log("Error receiving from the Jasper server", e);
					}
				}
			});
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error receiving from the Jasper server", e);
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return new String[0];
		}

		try {
			Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			Queue globalQueue = session.createQueue(JASPER_GLOBAL_QUEUE);
			MessageProducer producer = session.createProducer(globalQueue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			producer.setTimeToLive(30000);
			ObjectMessage msg = session.createObjectMessage(new JasperAdminMessage(Type.ontologyManagement,Command.get_ontology));
	        msg.setJMSCorrelationID(correlationID);
	        msg.setJMSReplyTo(adminQueue);
	        producer.send(msg);
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error sending to the Jasper server", e);
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return new String[0];
		}
		
		if (submodels[0] == null) {
			synchronized (correlationID) {
		    	try {
		    		correlationID.wait(10000);
				} catch (InterruptedException e) {
					Activator.getDefault().log("Error waiting for the Jasper server", e);
				}
			}
		}
		
		return (String[])submodels[0];
	}
	
    protected void updateImportPolicy(IProgressMonitor monitor) {
		IFile policy = (IFile) editor.getFile().getParent().getFile(Path.fromOSString(DTA.IMPORT_POLICY));
		Model imports = DTACore.loadImportPolicyModel(policy);
		DTACore.addImportPolicyEntry(imports, JASPER_NS, "file:"+JASPER_FILE);
		DTACore.saveImportPolicyModel(imports, policy);
		monitor.done();
	}

	protected void updateDTAModel(IProgressMonitor monitor) {
		final OntModel model = editor.getModel();
		Ontology ontology = model.listOntologies().next();
		Resource r = model.getResource(JASPER_NS);
		if (!ontology.hasProperty(OWL.imports, r)) {
			CompoundCommand cc = new CompoundCommand();
			cc.add(new AddPropertyCommand(ontology, OWL.imports, r));
			cc.add(new ChangeImportLoadCommand(editor.getFile(), model, JASPER_NS, true));
			editor.executeCommand(cc, true);
		} else {
			new UIJob("Reload DATA Model") {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					editor.reload();
					return Status.OK_STATUS;						
				}
			}.schedule();
		}
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.numColumns = 2;
		
		Label label = new Label(container, SWT.NONE);
		label.setText(prompt);
		GridData layoutData = new GridData();
		layoutData.horizontalSpan = 2;
		label.setLayoutData(layoutData);
		
		Credentials credentials = loadCredentials();
		
		label = new Label(container, SWT.NONE);
		label.setText("Jasper URL:");

		url = new Text(container, SWT.BORDER);
		url.setText(credentials.url);
		url.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
		
		label = new Label(container, SWT.NONE);
		label.setText("Vendor:");

		vendor = new Text(container, SWT.BORDER);
		vendor.setText(credentials.vendor);
		vendor.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		label = new Label(container, SWT.NONE);
		label.setText("Application:");

		application = new Text(container, SWT.BORDER);
		application.setText(credentials.application);
		application.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		label = new Label(container, SWT.NONE);
		label.setText("Version:");

		version = new Text(container, SWT.BORDER);
		version.setText(credentials.version);
		version.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));

		return container;
	}

	public ClientLicense getJasperLicense(Credentials credentials) throws Exception {
		String keyFile = System.getProperty("jta-keystore");
		if (keyFile == null)
			throw new IllegalArgumentException("cannot find environment variable jta-keystore");
		
		keyFile += "/" + credentials.vendor + "_" + credentials.application + "_" + credentials.version;
		keyFile += JAuthHelper.CLIENT_LICENSE_FILE_SUFFIX;
		
		ClientLicense license = JAuthHelper.loadClientLicenseFromFile(keyFile);
			
		if (!license.getVendor().equals(credentials.vendor) || 
	    	!license.getAppName().equals(credentials.application) ||
	    	!license.getVersion().equals(credentials.version))
			throw new AuthenticationException("Invalid license key for the DTA"); 
			
	    if (licenseExpiresInDays(license, 0))
			throw new AuthenticationException("Expired license key for the DTA"); 

	    return license;
	}
	
	public Connection connectToJasper(ClientLicense license, Credentials credentials) throws Exception {
		String username = credentials.vendor+":"+credentials.application+":"+credentials.version+":"+license.getDeploymentId();
		String password = JAuthHelper.bytesToHex(license.getLicenseKey());
		
		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(credentials.url);
		Connection connection = connectionFactory.createConnection(username, password);
	    connection.start();
	    
		return connection;
   	}

	protected boolean licenseExpiresInDays(ClientLicense license, int days) {
		if(license.getExpiry() == null)
			return false;
		Calendar currentTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
		currentTime.add(Calendar.DAY_OF_YEAR, days);
		return currentTime.after(license.getExpiry());
	}

}

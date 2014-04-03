package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

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


public class DTADownloadJasperModel {

	private static final String JASPER_URL ="tcp://0.0.0.0:61616";
	private static final String JASPER_FILE = "Jasper.dta";
	private static final String JASPER_URI = "http://coralcea.ca/jasper";
	
	private static class Credentials {
		public String url;
	}
	
	public static void run(final DTAEditor editor) {
		final IFile file = editor.getFile();
		final OntModel model = editor.getModel();
		final Shell shell = editor.getSite().getShell();

		final Credentials credentials;
		try {
			credentials = promptForJasperCredentials(shell, file);
			if (credentials == null)
				return;
		} catch (Exception e) {
			Activator.getDefault().log("Error getting Jasper credentials", e);
			return;
		}

		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					monitor.beginTask("Downloading Japser model", 10);
					IFile jasperFile = file.getParent().getFile(Path.fromOSString(JASPER_FILE));
					String[] subgraphs = downLoadGraphs(credentials, new SubProgressMonitor(monitor, 4));
					updateJasperModel(subgraphs, jasperFile, new SubProgressMonitor(monitor, 2));
					
					IFile policy = (IFile) jasperFile.getParent().findMember(DTA.IMPORT_POLICY);
					Model imports = DTACore.loadImportPolicyModel(policy);
					DTACore.addImportPolicyEntry(imports, JASPER_URI, "file:"+JASPER_FILE);
					DTACore.saveImportPolicyModel(imports, policy);
					monitor.worked(1);
				} catch (Exception e) { 
					Activator.getDefault().log("Error ading Jasper model", e);
					Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error downloading Jasper model", e);
					StatusManager.getManager().handle(status, StatusManager.BLOCK);
				} finally {
					monitor.done();
				}
			}
		};
		
		try {
			new ProgressMonitorDialog(shell).run(true, false, runnable);
		} catch (Exception e) {
			Activator.getDefault().log("Failed to download Jasper model", e);
		}

		Ontology ontology = model.listOntologies().next();
		Resource r = model.getResource(JASPER_URI);
		if (!ontology.hasProperty(OWL.imports, r)) {
			CompoundCommand cc = new CompoundCommand();
			cc.add(new AddPropertyCommand(ontology, OWL.imports, r));
			cc.add(new ChangeImportLoadCommand(editor.getFile(), model, JASPER_URI, true));
			editor.executeCommand(cc, true);
		} else
			editor.reload();
	}

	private static Credentials promptForJasperCredentials(Shell shell, IFile file) throws Exception {
		Credentials credentials = loadCredentials(file);
				
		InputDialog dialog = new InputDialog(shell, "Download Jasper Model", "Enter the URL of the Jasper server", credentials.url, new IInputValidator() {
			public String isValid(String url) {
				return DTAUtilities.isValidURI(url) ? null : "Invalid URI";
			}
		});
		
		if (dialog.open() != Dialog.OK)
			return null;
		
		credentials.url = dialog.getValue();
		
		saveCredentials(file, credentials);
		return credentials;
	}
	
	private static Credentials loadCredentials(IFile file) throws Exception {
		Credentials credentials = new Credentials();
		
		QualifiedName name = new QualifiedName(DTA.URI, "jasperURL");
		String value = (String) file.getPersistentProperty(name);
		if (value != null)
			credentials.url = value;
		else
			credentials.url = JASPER_URL;
		
		return credentials;
	}

	private static void saveCredentials(IFile file, Credentials credentials) throws Exception {
		QualifiedName name = new QualifiedName(DTA.URI, "jasperURL");
		file.setPersistentProperty(name, credentials.url);
	}
	
	private static String[] downLoadGraphs(Credentials credentials, IProgressMonitor monitor) {
		String[] graphs = new String[1];
		
		Model model = ModelFactory.createDefaultModel();
		model.add(
			model.createResource("http://mycompany.com/dta1#Type3"), 
			model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
			model.createResource("http://www.w3.org/2002/07/owl#Class")
		);
		model.add(
				model.createResource("http://mycompany.com/dta1#property1"), 
				model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), 
				model.createResource("http://www.w3.org/2002/07/owl#DatatypeProperty")
		);
		model.add(
				model.createResource("http://mycompany.com/dta1#property1"), 
				model.createProperty("http://www.w3.org/2000/01/rdf-schema#domain"), 
				model.createResource("http://mycompany.com/dta1#Type3")
		);
		model.add(
				model.createResource("http://mycompany.com/dta1#property1"), 
				model.createProperty("http://www.w3.org/2000/01/rdf-schema#range"), 
				model.createResource("http://www.w3.org/2001/XMLSchema#integer")
		);

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		model.write(os, DTA.FORMAT);
		graphs[0] = os.toString();
				
		return graphs;
	}
	
	private static void updateJasperModel(String[] subgraphs, IFile file, IProgressMonitor monitor) throws Exception {
		OntModel model = DTACore.createNewModel();
		Ontology ont = model.createOntology(JASPER_URI);
		ont.setPropertyValue(DTA.isLibrary, model.createTypedLiteral(true));
		for (String subgraph : subgraphs) {
			Model submodel = ModelFactory.createDefaultModel();
			submodel.read(new ByteArrayInputStream(subgraph.getBytes()), null, DTA.FORMAT);
			model.add(submodel);
		}
		DTACore.saveModel(model, file, true, monitor);
	}

}

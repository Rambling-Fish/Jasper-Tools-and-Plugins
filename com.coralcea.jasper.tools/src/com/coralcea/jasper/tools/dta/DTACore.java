package com.coralcea.jasper.tools.dta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelChangedListener;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public class DTACore  {

	public static String FORMAT = "RDF/XML-ABBREV";
	
	public static String IMPORT_POLICY = "import-policy.rdf";

	private static ResourceListener resourceListener = new ResourceListener();
	
	private static HashMap<IFile, OntModel> fileToModelMap = new HashMap<IFile, OntModel>();
	
	private static HashMap<IContainer, OntModelSpec> containerToSpecMap = new HashMap<IContainer, OntModelSpec>();
	
	private static List<DTAChangeListener> listeners = new ArrayList<DTAChangeListener>();
	
	public static void initialize() {
		startListening();
	}
	
	public static void dispose() {
		stopListening();
		listeners.clear();
		for(IFile file : fileToModelMap.keySet())
			unloadModel(file);
		fileToModelMap.clear();
		containerToSpecMap.clear();
	}
	
	private static void startListening() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
	}
	
	private static void stopListening() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
	}

	private static void configureDocumentManager(OntDocumentManager dm, IFile policy) {
		dm.setMetadataSearchPath(policy.getLocation().toOSString(), true);
		dm.getFileManager().addLocatorFile(policy.getParent().getLocation().toOSString());
		dm.setReadFailureHandler(new OntDocumentManager.ReadFailureHandler() {
			public void handleFailedRead(String uri, Model model, Exception e) {
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not find the model for imported URI <"+uri+">", e);
				StatusManager.getManager().handle(status, StatusManager.SHOW);
			}
		});
	}
	
	private static OntModelSpec getModelSpec(IFile file) {
		IContainer container = file.getParent();
		OntModelSpec spec = containerToSpecMap.get(container);
		if (spec == null) {
			spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
			OntDocumentManager dm = new OntDocumentManager();
			IFile policy = container.getFile(Path.fromOSString(IMPORT_POLICY));
			configureDocumentManager(dm, policy);
			spec.setDocumentManager(dm);
			containerToSpecMap.put(container, spec);
		}
		return spec;
	}
	
	private static void updateModelSpec(IFile policy) {
		IContainer container = policy.getParent();
		OntModelSpec spec = containerToSpecMap.get(container);
		if (spec != null)
			configureDocumentManager(spec.getDocumentManager(), policy);
	}

	public static OntModel getModel(IFile file) throws CoreException {
		OntModel model = fileToModelMap.get(file);
		if (model == null) {
			loadModel(file);
			model = fileToModelMap.get(file);
		}
		return model;
	}

	public static OntModel createModel(IFile file) throws CoreException {
		OntModel model = ModelFactory.createOntologyModel(getModelSpec(file));
		model.setNsPrefix(DTA.PREFIX, DTA.URI);
		model.register(new ChangeListener(file));
		fileToModelMap.put(file, model);
		return model;
	}

	private static void loadModel(IFile file) throws CoreException {
		OntModel model = ModelFactory.createOntologyModel(getModelSpec(file));
		model.setNsPrefix(DTA.PREFIX, DTA.URI);
		model.read(file.getContents(), null, FORMAT);
		model.register(new ChangeListener(file));
		fileToModelMap.put(file, model);
	}
	
	public static void unloadModel(IFile file) {
		OntModel model = fileToModelMap.get(file);
		if (model != null) {
			model.close();
			fileToModelMap.remove(file);
		}
	}
	
	public static Collection<IFile> getLoadedModelFiles() {
		return new ArrayList<IFile>(fileToModelMap.keySet());
	}
	
	public static Collection<OntModel> getLoadedModels() {
		return new ArrayList<OntModel>(fileToModelMap.values());
	}
	
	public static void writeModel(OntModel model, OutputStream out) {
		writeModel(model.getBaseModel(), out);
	}
	
	public static void writeModel(Model model, OutputStream out) {
		RDFWriter writer = model.getWriter(FORMAT);
		writer.setProperty("prettyTypes", new Resource[]{OWL.Ontology, OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty});
		writer.setProperty("width", 500);
		writer.setProperty("tab", 5);
		writer.setProperty("showXmlDeclaration", true);
		writer.setProperty("longid", false);
		writer.write(model, out, "");
	}
	
	public static void saveModel(OntModel model, IFile file, IProgressMonitor monitor) throws FileNotFoundException, CoreException {
		saveModel(model.getBaseModel(), file, false, monitor);
	}
	
	public static void saveModel(Model model, IFile file, boolean notify, IProgressMonitor monitor) throws FileNotFoundException, CoreException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DTACore.writeModel(model, out);
		if (!notify)
			stopListening();
		if (file.exists())
			file.setContents(new ByteArrayInputStream(out.toByteArray()), false, true, monitor);
		else
			file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
		if (!notify)
			startListening();
	}

	public static void addChangeListener(DTAChangeListener listener) {
		listeners.add(listener);
	}
	
	public static void removeChangeListener(DTAChangeListener listener) {
		listeners.remove(listener);
	}
	
	public static void notifyListeners(IFile file) {
		for (DTAChangeListener listener : listeners)
			listener.dtaChanged(file);
	}

	private static class ResourceListener implements IResourceChangeListener, IResourceDeltaVisitor {
		
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta delta = event.getDelta();
			try {
				delta.accept(this);
			} catch (CoreException e) { 
				Activator.getDefault().log("Error handling DTA resource changes", e);
			} 
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			if (!(delta.getResource() instanceof IFile))
				return true;
			IFile file = (IFile) delta.getResource();
			if (fileToModelMap.containsKey(file)) {
				if ((delta.getKind() & (IResourceDelta.REMOVED|IResourceDelta.CHANGED))!=0) 
					unloadModel(file);
			} else if (file.getName().equals(IMPORT_POLICY)) {
				if ((delta.getKind() & (IResourceDelta.ADDED|IResourceDelta.REMOVED|IResourceDelta.CHANGED))!=0) 
					updateModelSpec(file);
			}
			return false;
		}
	}

	private static class ChangeListener implements ModelChangedListener {

		private IFile file;
		
		public ChangeListener(IFile file) {
			this.file = file;
		}
		
		@Override
		public void addedStatement(Statement s) {
			changeOccurred();
		}

		@Override
		public void addedStatements(Statement[] statements) {
			changeOccurred();
		}

		@Override
		public void addedStatements(List<Statement> statements) {
			changeOccurred();
		}

		@Override
		public void addedStatements(StmtIterator statements) {
			changeOccurred();
		}

		@Override
		public void addedStatements(Model m) {
			changeOccurred();
		}

		@Override
		public void removedStatement(Statement s) {
			changeOccurred();
		}

		@Override
		public void removedStatements(Statement[] statements) {
			changeOccurred();
		}

		@Override
		public void removedStatements(List<Statement> statements) {
			changeOccurred();
		}

		@Override
		public void removedStatements(StmtIterator statements) {
			changeOccurred();
		}

		@Override
		public void removedStatements(Model m) {
			changeOccurred();
		}

		@Override
		public void notifyEvent(Model m, Object event) {
			changeOccurred();
		}

		protected void changeOccurred() {
			notifyListeners(file);
		}

	}
}

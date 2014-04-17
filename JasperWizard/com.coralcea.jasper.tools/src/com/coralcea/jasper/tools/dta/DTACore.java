package com.coralcea.jasper.tools.dta;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;

public class DTACore  {

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

	private static OntModelSpec getModelSpec(IFile file) {
		IContainer container = file.getParent();
		OntModelSpec spec = containerToSpecMap.get(container);
		if (spec == null) {
			FileManager fileManager = new FileManager(LocationMapper.get()) {
				public Model readModel(Model model, String filenameOrURI) {
			        String mappedURI = mapURI(filenameOrURI) ;
			        if (mappedURI!=null && mappedURI.endsWith(DTA.EXTENSION))
			        	return readModel(model, filenameOrURI, DTA.FORMAT);
			        return super.readModel(model, filenameOrURI);
				}
			};

			IFile policy = container.getFile(Path.fromOSString(DTA.IMPORT_POLICY));
			fileManager.addLocatorFile(policy.getParent().getLocation().toOSString());
			
			OntDocumentManager dm = new OntDocumentManager(fileManager, policy.getLocation().toOSString());
			dm.setReadFailureHandler(new OntDocumentManager.ReadFailureHandler() {
				public void handleFailedRead(String uri, Model model, Exception e) {
					Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not load the imported model <"+uri+">", e);
					StatusManager.getManager().handle(status, StatusManager.SHOW);
				}
			});
			
			spec = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
			spec.setDocumentManager(dm);
			containerToSpecMap.put(container, spec);
		}
		return spec;
	}
	
	private static void updateModelSpec(IFile policy) {
		OntModelSpec spec = containerToSpecMap.get( policy.getParent());
		if (spec != null)
			spec.getDocumentManager().setMetadataSearchPath(policy.getLocation().toOSString(), false);
	}

	public static OntModel getPossiblyLoadedModel(IFile file) throws CoreException {
		OntModel model = fileToModelMap.get(file);
		if (model == null)
			model = loadModel(file);
		return model;
	}

	public static OntModel createNewModel(IFile file) {
		OntModel model = ModelFactory.createOntologyModel(getModelSpec(file));
		model.setNsPrefix(DTA.PREFIX, DTA.URI);
		return model;
	}

	public static OntModel createNewModel() {
		OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
		model.setNsPrefix(DTA.PREFIX, DTA.URI);
		return model;
	}

	public static OntModel loadModel(IFile file) throws CoreException {
		if (file.exists()) {
			OntModel model = createNewModel(file);
			model.register(new ChangeListener(file));
			fileToModelMap.put(file, model);
			model.read(file.getContents(), null, DTA.FORMAT);
			return model;
		}
		return null;
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
		writeModel(model, out, DTA.FORMAT);
	}
	
	public static void writeModel(OntModel model, OutputStream out, String format) {
		writeModel(model.getBaseModel(), out, format);
	}

	public static void writeModel(Model model, OutputStream out) {
		writeModel(model, out, DTA.FORMAT);
	}

	public static void writeModel(Model model, OutputStream out, String format) {
		RDFWriter writer = model.getWriter(format);
		writer.setProperty("prettyTypes", new Resource[]{OWL.Ontology, OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty});
		writer.setProperty("width", 500);
		writer.setProperty("tab", 5);
		writer.setProperty("showXmlDeclaration", true);
		writer.setProperty("longid", false);
		writer.write(model, out, "");
	}
	
	public static void saveModel(OntModel model, IFile file, boolean notify, IProgressMonitor monitor) throws CoreException {
		if (!notify)
			stopListening();
		saveModel(model.getBaseModel(), file, DTA.FORMAT, monitor);
		if (!notify)
			startListening();
	}
	
	public static void saveModel(Model model, IFile file, String format, IProgressMonitor monitor) throws CoreException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DTACore.writeModel(model, out, format);
		if (file.exists())
			file.setContents(new ByteArrayInputStream(out.toByteArray()), false, true, monitor);
		else
			file.create(new ByteArrayInputStream(out.toByteArray()), true, monitor);
	}

	public static Model loadImportPolicyModel(IFile policy) {
        Model model = ModelFactory.createDefaultModel() ;
        model.setNsPrefix("", OntDocumentManager.NS);
		if (policy.exists()) {
	        try {
				model.read(policy.getContents(), null, "RDF/XML-ABBREV" );
			} catch (CoreException e) {
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed to read the DTA policy file");
				StatusManager.getManager().handle(status, StatusManager.SHOW);
			}
		}
		return model;
	}

	public static void saveImportPolicyModel(Model model, IFile policy) {
		try {
			DTACore.saveModel(model, policy, "RDF/XML-ABBREV", null);
		} catch (Exception e)  {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Error in Import dialog");
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		} finally {
			model.close();
		}
	}
	
	public static Resource addImportPolicyEntry(Model model, String publicURI, String altURL) {
		Resource spec = null;
		for (Resource statement : model.listResourcesWithProperty(RDF.type, OntDocumentManager.ONTOLOGY_SPEC).toList()) {
			if (publicURI.equals(statement.getPropertyResourceValue(OntDocumentManager.PUBLIC_URI).getURI()))
				spec = statement;
		};
		if (spec == null) {
			spec = model.createResource(OntDocumentManager.ONTOLOGY_SPEC);
			spec.addProperty(OntDocumentManager.PUBLIC_URI, model.getResource(publicURI));
		}
		spec.removeAll(OntDocumentManager.ALT_URL);
		spec.addProperty(OntDocumentManager.ALT_URL, model.getResource(altURL));
		return spec;
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
					if ((delta.getFlags() & IResourceDelta.MARKERS)==0)
						unloadModel(file);
			} else if (file.getName().equals(DTA.IMPORT_POLICY)) {
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

package com.coralcea.jasper.tools.dta;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAModelValidator {

	private Map<Resource, List<String>> errors = new HashMap<Resource, List<String>>();

	public static void run(Shell shell, final IFile file, final OntModel model) {
		try {
			new ProgressMonitorDialog(shell).run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException,	InterruptedException {
					try {
						ResourcesPlugin.getWorkspace().run(getRunnable(file, model), monitor);
						if (file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO).length!=0) {
							Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Validation problems can be inspected in Problems view");
							StatusManager.getManager().handle(status, StatusManager.BLOCK);
						}
					} catch (CoreException e) {
						Activator.getDefault().log(e);
					}
				}
			});
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			Activator.getDefault().log(e);
		}
	}
	
	public static IWorkspaceRunnable getRunnable(final IFile file, final OntModel model) {
		return new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				new DTAModelValidator().validate(file, model, monitor);
			}
		};
	}
	
	private boolean validate(IFile file, OntModel model, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Validating model", 17);

		try {
			for (IMarker problem : file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO))
				problem.delete();
		} catch (CoreException e) {
			Activator.getDefault().log(e);
		}
		monitor.worked(1);

		for(Resource operation : DTAUtilities.listResourcesOfType(model, DTA.Operation))
			validateOperation(file, operation);
		monitor.worked(2);

		for(Resource request : DTAUtilities.listResourcesOfType(model, DTA.Request))
			validateRequest(file, request);
		monitor.worked(2);
		
		for (OntClass aClass : DTAUtilities.listClasses(model))
			validateType(file, aClass);
		monitor.worked(4);

		for (OntProperty property : DTAUtilities.listProperties(model))
			validateProperty(file, property);
		monitor.worked(8);

		return errors.isEmpty();
	}

	private void validateOperation(IFile file, Resource operation) throws CoreException {
		if (!operation.hasProperty(DTA.input))
			log(file, operation, "does not have an input type", IMarker.SEVERITY_ERROR);
		
		if (DTAUtilities.isGet(operation) && !operation.hasProperty(DTA.output))
			log(file, operation, "does not have an output property", IMarker.SEVERITY_ERROR);
	}	
	
	private void validateRequest(IFile file, Resource request) throws CoreException {
		if (!request.hasProperty(DTA.input))
			log(file, request, "does not have an input type", IMarker.SEVERITY_ERROR);
		
		if (DTAUtilities.isGet(request) && !request.hasProperty(DTA.output))
			log(file, request, "does not have an output property", IMarker.SEVERITY_ERROR);
	}	

	private void validateType(IFile file, Resource type) {
        // no type validation rules yet
	}

	private void validateProperty(IFile file, Resource property) throws CoreException {
		Resource type = property.getPropertyResourceValue(RDFS.range);
		if (type == null)
			log(file, property, "does not have a type", IMarker.SEVERITY_ERROR);

		for(Resource equivalentProperty : DTAUtilities.listObjects(property, OWL.equivalentProperty, Resource.class)) {
			Resource equivalentType = equivalentProperty.getPropertyResourceValue(RDFS.range);
			if (equivalentType!=null && !equivalentType.equals(type))
				log(file, property, "does not have the same type as its equivalent property <"+equivalentProperty+">", IMarker.SEVERITY_ERROR);
		}

		for(Resource superProperty : DTAUtilities.listObjects(property, RDFS.subPropertyOf, Resource.class)) {
			Resource superType = superProperty.getPropertyResourceValue(RDFS.range);
			if (superType!=null && !DTAUtilities.listSelfAndAllSubClasses(superType).contains(type))
				log(file, property, "does not have a compatible type with its super property <"+superProperty+">", IMarker.SEVERITY_ERROR);
		}
	}
	
	private void log(IFile file, Resource resource, String message, int kind) throws CoreException {
		IMarker marker = file.createMarker(DTA.MARKER);
	    marker.setAttribute(IMarker.MESSAGE, DTAUtilities.getKind(resource)+" <"+resource+"> "+message);
	    marker.setAttribute(IMarker.LOCATION, resource.getURI());
	    marker.setAttribute(IMarker.SEVERITY, kind);
	}
	
}

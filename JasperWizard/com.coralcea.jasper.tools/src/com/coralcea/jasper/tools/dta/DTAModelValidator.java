package com.coralcea.jasper.tools.dta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.CardinalityRestriction;
import com.hp.hpl.jena.ontology.MaxCardinalityRestriction;
import com.hp.hpl.jena.ontology.MinCardinalityRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAModelValidator {

	private Map<Resource, List<String>> errors = new HashMap<Resource, List<String>>();

	public static boolean run(final IFile file, IProgressMonitor monitor, boolean block) throws CoreException {
		ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				new DTAModelValidator().validate(file, monitor);
			}
		}, monitor);
		if (file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO).length!=0) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Validation problems can be inspected in Problems view");
			StatusManager.getManager().handle(status, block ? StatusManager.BLOCK : StatusManager.SHOW);
			return false;
		}
		return true;
	}
	
	private boolean validate(IFile file, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Validating model", 17);

		OntModel model = DTACore.getPossiblyLoadedModel(file);

		Resource ontology = model.listOntologies().next();
		String basePackage = DTAUtilities.getStringValue(ontology, DTA.basepackage);
		final Pattern pattern = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");
		if (basePackage.length()>0 && !pattern.matcher(basePackage).matches())
			log(file, ontology, "has an invalid base package", IMarker.SEVERITY_ERROR);

		for (IMarker problem : file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO))
			problem.delete();
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
		if (!operation.hasProperty(DTA.parameter))
			log(file, operation, "does not have a parameter specified", IMarker.SEVERITY_ERROR);
		
		if (DTAUtilities.isGet(operation) && !operation.hasProperty(DTA.data))
			log(file, operation, "does not have data specified", IMarker.SEVERITY_ERROR);
	}	
	
	private void validateRequest(IFile file, Resource request) throws CoreException {
		if (!DTAUtilities.isSubscribe(request) && !request.hasProperty(DTA.parameter))
			log(file, request, "does not have a parameter specified", IMarker.SEVERITY_ERROR);
		
		if (!DTAUtilities.isPost(request) && !request.hasProperty(DTA.data))
			log(file, request, "does not have data specified", IMarker.SEVERITY_ERROR);
	}	

	private void validateType(IFile file, Resource type) throws CoreException {
		for(Property property : DTAUtilities.listDeclaredProperties(type)) {
			Restriction direct = DTAUtilities.getDirectRestriction(type, DTA.restriction, property);
			if (direct != null) {
				Restriction indirect = DTAUtilities.getIndirectRestriction(type, DTA.restriction, property);
				if (indirect != null) {
					if ((indirect.canAs(MaxCardinalityRestriction.class) && direct.canAs(MinCardinalityRestriction.class)) ||
						(indirect.canAs(MinCardinalityRestriction.class) && direct.canAs(MaxCardinalityRestriction.class)) ||
						(indirect.canAs(CardinalityRestriction.class) && !direct.canAs(CardinalityRestriction.class)))
						log(file, type, "has a more relaxed cardinality on property <"+property+"> than in super types", IMarker.SEVERITY_ERROR);
				}
			}
		}
	}

	private void validateProperty(IFile file, Resource property) throws CoreException {
		Resource type = property.getPropertyResourceValue(RDFS.range);
		if (DTAUtilities.isDatatypeProperty(property) && !DTAUtilities.isSupportedDatatype(type))
			log(file, property, "does not have a supported type", IMarker.SEVERITY_ERROR);

		for(Resource equivalentProperty : DTAUtilities.listObjects(property, OWL.equivalentProperty, Resource.class)) {
			if (property.getModel().contains(equivalentProperty, RDF.type)) {
				Resource equivalentType = equivalentProperty.getPropertyResourceValue(RDFS.range);
				if ((type==null && equivalentType!=null) || (type!=null && !type.equals(equivalentType)))
					log(file, property, "does not have the same type as its equivalent property <"+equivalentProperty+">", IMarker.SEVERITY_ERROR);
			}
		}

		for(Resource superProperty : DTAUtilities.listObjects(property, RDFS.subPropertyOf, Resource.class)) {
			if (property.getModel().contains(superProperty, RDF.type)) {
				Resource superType = superProperty.getPropertyResourceValue(RDFS.range);
				if (type==null) {
					if (superType!=null)
						log(file, property, "does not have a compatible type with its super property <"+superProperty+">", IMarker.SEVERITY_ERROR);
				} else if (DTAUtilities.isDatatypeProperty(property)) {
					if (!type.equals(superType) && superType!=null && !RDFS.Literal.equals(superType))
						log(file, property, "does not have a compatible type with its super property <"+superProperty+">", IMarker.SEVERITY_ERROR);
				} else {
					if (superType!=null && !DTAUtilities.listSelfAndAllSuperClasses(type).contains(superType))
						log(file, property, "does not have a compatible type with its super property <"+superProperty+">", IMarker.SEVERITY_ERROR);
				}
			}
		}
	}
	
	private void log(IFile file, Resource resource, String message, int kind) throws CoreException {
		IMarker marker = file.createMarker(DTA.MARKER);
	    marker.setAttribute(IMarker.MESSAGE, DTAUtilities.getKind(resource)+" <"+resource+"> "+message);
	    marker.setAttribute(IMarker.LOCATION, resource.getURI());
	    marker.setAttribute(IMarker.SEVERITY, kind);
	}
	
}

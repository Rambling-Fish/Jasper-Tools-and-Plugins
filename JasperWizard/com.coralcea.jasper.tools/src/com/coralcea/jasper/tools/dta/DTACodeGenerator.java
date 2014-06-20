package com.coralcea.jasper.tools.dta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTACodeGenerator {

	private static final String BASE_PACKAGE = "base";

	public static boolean run(final IFile file, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Generating code", 10);
		
		if (!DTAModelValidator.run(file, new SubProgressMonitor(monitor, 3), true)) {
			monitor.done();
			return false;
		}

		if (new DTACodeGenerator().generate(file, new SubProgressMonitor(monitor, 7))) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Errors were detected code generation. Please refer to the log for details.");
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return false;
		}
		
		return true;
	}

	private boolean generate(IFile file, IProgressMonitor monitor) {
		monitor.beginTask("Generating code", 10);

		boolean error=false;

		IResource container = file.getProject().findMember("src/main/java");
		if (container==null || !container.exists()) {
			Activator.getDefault().log("Could not find a folder named src/main/java in project '"+file.getProject().getName()+"'");
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Errors were detected code generation. Please refer to the log for details.");
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return false;
		}

		OntModel model;
		try {
			model = DTACore.getPossiblyLoadedModel(file);
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to open DTA file", e);
			return true;
		}

		Set<String> packages = new HashSet<String>();
		Set<Resource> types = new HashSet<Resource>();
		Set<Resource> operations = new HashSet<Resource>();
		Set<Resource> requests = new HashSet<Resource>();
		Set<String> namespaces = new HashSet<String>();
		collect(model, packages, types, operations, requests, namespaces);
		
		for (Iterator<String> i = namespaces.iterator(); i.hasNext();) {
			if (model.getNsPrefixMap().containsValue(i.next()))
				i.remove();
		}
		if (!namespaces.isEmpty()) {
			MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, Status.ERROR, "The following namespaces do not have prefixes", null);
			for(String namespace : namespaces)
				status.add(new Status(Status.ERROR, Activator.PLUGIN_ID, namespace));
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return false;
		}
		
		monitor.worked(1);
		
		IPackageFragmentRoot root = (IPackageFragmentRoot) JavaCore.create(container);
		error |= generatePackages(root, packages, new SubProgressMonitor(monitor, 1));
		error |= generateTypes(root, types, new SubProgressMonitor(monitor, 4));
		error |= generateOperations(root, operations, new SubProgressMonitor(monitor, 2));
		error |= generateRequests(root, requests, new SubProgressMonitor(monitor, 2));
		
		try {
			container.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to generate/update folder", e);
			error=true;
		}

		return error;
	}
	
	private void collect(OntModel model, Set<String> packages, Set<Resource> types, Set<Resource> operations, Set<Resource> requests, Set<String> namespaces) {
		Ontology ontology = model.listOntologies().next();
		
		for (ResIterator i = DTAUtilities.listDefinedResources(ontology, DTA.DTA); i.hasNext();) {
			Resource dta = i.next();
			
			for(Resource operation : DTAUtilities.listObjects(dta, DTA.operation, Resource.class)) {
				operations.add(operation);
		        packages.add(getPackageName(operation));
		        namespaces.add(operation.getNameSpace());
		        
				if (operation.hasProperty(DTA.parameter))
					collectType(operation.getPropertyResourceValue(DTA.parameter), packages, types, namespaces);
				
				if (operation.hasProperty(DTA.data))
					collectProperty(operation.getPropertyResourceValue(DTA.data), packages, types, namespaces);
			}

			for(Resource request : DTAUtilities.listObjects(dta, DTA.request, Resource.class)) {
				requests.add(request);
		        packages.add(getPackageName(request));
		        namespaces.add(request.getNameSpace());
		        
				if (request.hasProperty(DTA.parameter)) {
					Resource parameter = request.getPropertyResourceValue(DTA.parameter);
					for (OntProperty p : DTAUtilities.listAllProperties(parameter))
						collectProperty(p, packages, types, namespaces);
					for (AllValuesFromRestriction restriction : DTAUtilities.listObjects(request, DTA.parameterRestriction, AllValuesFromRestriction.class))
						collectType(restriction.getPropertyResourceValue(OWL.allValuesFrom), packages, types, namespaces);
				}
				
				if (request.hasProperty(DTA.data)) 
					collectProperty(request.getPropertyResourceValue(DTA.data), packages, types, namespaces);
			}
		}
	}

	private void collectType(Resource type, Set<String> packages, Set<Resource> types, Set<String> namespaces) {
        if (types.contains(type))
        	return;

        types.add(type);
        packages.add(getPackageName(type));
		namespaces.add(type.getNameSpace());

		for (OntProperty p : DTAUtilities.listDeclaredProperties(type))
			collectProperty(p, packages, types, namespaces);
		
		for (OntClass supertype : DTAUtilities.listObjects(type, RDFS.subClassOf, OntClass.class))
			collectType(supertype, packages, types, namespaces);
		
		for (OntClass subtype : DTAUtilities.listSubjects(RDFS.subClassOf, type, OntClass.class))
			collectType(subtype, packages, types, namespaces);
	}

	private void collectProperty(Resource property, Set<String> packages, Set<Resource> types, Set<String> namespaces) {
		namespaces.add(property.getNameSpace());
		
		Resource ptype = property.getPropertyResourceValue(RDFS.range);
		if (ptype != null && !DTAUtilities.isDatatype(ptype))
			collectType(ptype, packages, types, namespaces);
	}

	private boolean generatePackages(IPackageFragmentRoot root, Set<String> packageNames, IProgressMonitor monitor) {
		boolean error=false;

		monitor.beginTask("Generating packages", packageNames.size());
		for(String name : packageNames) {
			try {
				root.createPackageFragment(name, true, new SubProgressMonitor(monitor, 1));
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java package", e);
				error=true;
			}
		}
		monitor.done();
		
		return error;
	}

	private boolean generateTypes(IPackageFragmentRoot root, Set<Resource> types, IProgressMonitor monitor) {
		boolean error=false;
		
		monitor.beginTask("Generating types", 2*types.size());
		for(Resource type : types) {
			String packageName = getPackageName(type);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			error |= generateTypeInterface(aPackage, type, new SubProgressMonitor(monitor, 1));
			error |= generateTypeClass(aPackage, type, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
		
		return error;
	}
	
	private boolean generateOperations(IPackageFragmentRoot root, Set<Resource> operations, IProgressMonitor monitor) {
		boolean error=false;
		
		monitor.beginTask("Generating operations", operations.size());
		for(Resource operation : operations) {
			String packageName = getPackageName(operation);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			if (DTAUtilities.isPublish(operation))
				error |= generateProcessorClass(aPackage, operation, new SubProgressMonitor(monitor, 1));
			else
				error |= generateSourceClass(aPackage, operation, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
		
		return error;
	}

	private boolean generateRequests(IPackageFragmentRoot root, Set<Resource> requests, IProgressMonitor monitor) {
		boolean error=false;
		
		monitor.beginTask("Generating requests", requests.size());
		for(Resource request : requests) {
			String packageName = getPackageName(request);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			if (DTAUtilities.isSubscribe(request))
				error |= generateSourceClass(aPackage, request, new SubProgressMonitor(monitor, 1));
			else
				error |= generateProcessorClass(aPackage, request, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
		
		return error;
	}

	private boolean generateTypeClass(IPackageFragment aPackage, Resource type, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(type);
		String className = typeName+"Impl";
		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(className+".java");
        IType aType = aJavaFile.getType(className);

        if (aType.exists() && isGenerated(aType)) {
			try {
				aType.delete(true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error deleting Java type", e);
				error=true;
			}
		}
        
        if (!aType.exists() || isGenerated(aType)) {
        	String extensions = " implements "+typeName;
        	
	        String genAnnot = "@Generated(\"true\")\n";
	        String uriAnnot = "@JsonTypeName(\""+type.getURI()+"\")\n";
	        String infoAnnot = "@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property=\"@type\")\n";

    		String comment = DTAUtilities.getStringValue(type, RDFS.comment);
			comment = comment.length()>0 ? createComment(new String[]{comment}) : comment;

			try {
				aType = aJavaFile.createType(comment+genAnnot+uriAnnot+infoAnnot+"public class "+className+extensions+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}

		error |= generateFields(aType, type, null);
		error |= generateGetters(aType, type, null, false, true, true);
		error |= generateSetters(aType, type, null, false, true, true);
		error |= generateHashCode(aType);
		error |= generateEquals(aType);
		error |= generateToString(aType);
		
		error |= formatCompilationUnit(aJavaFile);
		
		monitor.done();
		return error;
	}			

	private boolean generateTypeInterface(IPackageFragment aPackage, Resource type, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating interface", 1);

		String typeName = getTypeName(type);
		String typePackage = getPackageName(type);
		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType aType = aJavaFile.getType(typeName);

        if (aType.exists() && isGenerated(aType)) {
			try {
				aType.delete(true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error deleting Java type", e);
				error=true;
			}
		}
        
        if (!aType.exists() || isGenerated(aType)) {
			String extensions = "";
	        for (RDFNode n : DTAUtilities.listObjects(type, RDFS.subClassOf)) {
				Resource superType = (Resource)n;
	        	String superTypeName = getTypeName(superType);
				String superTypePackName = getPackageName(superType);
				if (!superTypePackName.equals("xsd") && !superTypePackName.equals(typePackage))
					superTypeName = superTypePackName+"."+superTypeName;
				extensions += extensions.length()==0 ? " extends "+superTypeName : ", "+superTypeName;
			}
	        
	        String subTypeNames = "";
	        for (OntClass subType : DTAUtilities.listSelfAndAllSubClasses(type)) {
	        	String subTypeName = getTypeName(subType);
				String subTypePackName = getPackageName(subType);
				if (!subTypePackName.equals("xsd") && !subTypePackName.equals(typePackage))
					subTypeName = subTypePackName+"."+subTypeName;
	        	if (subTypeNames.length()>0)
	        		subTypeNames += ",\n";
	        	subTypeNames += "\t@JsonSubTypes.Type(value="+subTypeName+"Impl.class, name=\""+subType.getURI()+"\")";
	        }
	        
    		String comment = DTAUtilities.getStringValue(type, RDFS.comment);
			comment = comment.length()>0 ? createComment(new String[]{comment}) : comment;
    		
    		String genAnnot = "@Generated(\"true\")\n";
	        String uriAnnot = "@JsonTypeName(\""+type.getURI()+"\")\n";
	        String infoAnnot = "@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property=\"@type\")\n";
	        String subtypeAnnot = subTypeNames.length()>0 ? "@JsonSubTypes({\n"+subTypeNames+"\n})\n" : "";

			try {
				aType = aJavaFile.createType(comment+genAnnot+uriAnnot+infoAnnot+subtypeAnnot+"public interface "+typeName+extensions+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}

		error |= generateGetters(aType, type, null, true, false, false);
		error |= generateSetters(aType, type, null, true, false, false);

		monitor.done();
		return error;
	}			

	private boolean generateParameter(IPackageFragment aPackage, Resource request, IProgressMonitor monitor) {
		boolean error=false;
 		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(request);
		String typePackage = getPackageName(request);

		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType opType = aJavaFile.getType(typeName);

		String className = "Parameter";
		IType aType = opType.getType(className);
		
        if (aType.exists() && isGenerated(aType)) {
			try {
				aType.delete(true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error deleting Java type", e);
				error=true;
			}
		}
        
        if (!aType.exists() || isGenerated(aType)) {
	        String genAnnot = "@Generated(\"true\")\n";

			if (!typePackage.equals("xsd") && !typePackage.equals(typePackage))
				typeName = typePackage+"."+typeName;
			String comment = createComment(new String[]{"The parameter of {@link "+typeName+"}"});

			try {
				aType = opType.createType(comment+genAnnot+"public static class "+className+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}

		OntClass parameter = DTAUtilities.getParameter(request);
		Map<OntProperty, Resource> typeOverrides = new HashMap<OntProperty, Resource>();
		for(AllValuesFromRestriction r : DTAUtilities.listObjects(request, DTA.parameterRestriction, AllValuesFromRestriction.class))
			typeOverrides.put(r.getOnProperty(), r.getAllValuesFrom());
			
		error |= generateFields(aType, parameter, typeOverrides);
		error |= generateGetters(aType, parameter, typeOverrides, true, true, true);
		error |= generateSetters(aType, parameter, typeOverrides, true, true, true);
		error |= generateHashCode(aType);
		error |= generateEquals(aType);
		error |= generateToString(aType);
		
		error |= formatCompilationUnit(aJavaFile);
		
		monitor.done();
		return error;
	}			

	private boolean generateFields(IType aType, Resource type, Map<OntProperty, Resource> typeOverrides) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties = DTAUtilities.listAllProperties(type);

		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		for (OntProperty property : properties) {
			String fieldName = getFieldName(property, false);
			if (nameCount.containsKey(fieldName))
				nameCount.put(fieldName, nameCount.get(fieldName)+1);
			else
				nameCount.put(fieldName, 1);
		}
		
		for (OntProperty property : properties) {
        	String fieldName = getFieldName(property, false);
        	if (nameCount.get(fieldName) > 1)
				fieldName = getFieldName(property, true);

            IField aField = aType.getField(fieldName);
            
            if (aField.exists() && isGenerated(aField)) {
    			try {
    				aField.delete(true, null);
    			} catch (Exception e) {
    				Activator.getDefault().log("Error deleting Java field", e);
    				error=true;
    			}
    		}

            if (!aField.exists() || isGenerated(aField)) {
    			Resource fieldType = property.getPropertyResourceValue(RDFS.range);
    			if (typeOverrides!=null) {
    				Resource override = typeOverrides.get(property);
    				if (override!=null)
    					fieldType = override;
    			}
    			
            	String fieldTypeName = getTypeName(fieldType);
    			String fieldTypePackName = getPackageName(fieldType);
    			if (!fieldTypePackName.equals("xsd") && !fieldTypePackName.equals(typePackage))
    				fieldTypeName = fieldTypePackName+"."+fieldTypeName;
    			if (isArrayProperty(type, property))
    				fieldTypeName += "[]";
    			
				String genAnnot = "@Generated(\"true\")\n";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
			        aField = aType.createField(genAnnot+propAnnot+"private "+fieldTypeName+" "+fieldName+";", null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private boolean generateGetters(IType aType, Resource type, Map<OntProperty, Resource> typeOverrides, boolean genComment, boolean genBody, boolean genInherited) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties;
		if (genInherited)
			properties = DTAUtilities.listAllProperties(type);
		else
			properties = DTAUtilities.listDeclaredProperties(type);

		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		for (OntProperty property : properties) {
			String getterName = getGetterName(property, false);
			if (nameCount.containsKey(getterName))
				nameCount.put(getterName, nameCount.get(getterName)+1);
			else
				nameCount.put(getterName, 1);
		}
		
		for (OntProperty property : properties) {
        	String getterName = getGetterName(property, false);
        	if (nameCount.get(getterName) > 1)
				getterName = getGetterName(property, true);

            IMethod aMethod = aType.getMethod(getterName, new String[]{});
            
            if (aMethod.exists() && isGenerated(aMethod)) {
    			try {
    				aMethod.delete(true, null);
    			} catch (Exception e) {
    				Activator.getDefault().log("Error deleting Java method", e);
    				error=true;
    			}
    		}

            if (!aMethod.exists() || isGenerated(aMethod)) {
    			Resource methodType = property.getPropertyResourceValue(RDFS.range);
    			if (typeOverrides!=null) {
    				Resource override = typeOverrides.get(property);
    				if (override!=null)
    					methodType = override;
    			}

    			String methodTypeName = getTypeName(methodType);
    			String methodTypePackName = getPackageName(methodType);
    			if (!methodTypePackName.equals("xsd") && !methodTypePackName.equals(typePackage))
    				methodTypeName = methodTypePackName+"."+methodTypeName;

    			if (isArrayProperty(type, property))
    				methodTypeName += "[]";

    			String fieldName = getFieldName(property, false);
            	if (nameCount.get(getterName) > 1)
    				fieldName = getFieldName(property, true);
            	
        		String comment;
        		if (genComment) {
    				comment = DTAUtilities.getStringValue(property, RDFS.comment);
    				comment = createComment(new String[]{"@return "+fieldName+" "+comment});
        		} else
        			comment = "@Override\n";

            	String body = genBody ? "{\n\treturn "+fieldName+";\n}" : ";";
            	
            	String genAnnot = "@Generated(\"true\")\n";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
		        	aMethod = aType.createMethod(comment+genAnnot+propAnnot+"public "+methodTypeName+" "+getterName+"()"+body, null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private boolean generateSetters(IType aType, Resource type, Map<OntProperty, Resource> typeOverrides, boolean genComment, boolean genBody, boolean genInherited) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties;
		if (genInherited)
			properties = DTAUtilities.listAllProperties(type);
		else
			properties = DTAUtilities.listDeclaredProperties(type);

		Map<String, Integer> nameCount = new HashMap<String, Integer>();
		for (OntProperty property : properties) {
			String setterName = getSetterName(property, false);
			if (nameCount.containsKey(setterName))
				nameCount.put(setterName, nameCount.get(setterName)+1);
			else
				nameCount.put(setterName, 1);
		}
		
		for (OntProperty property : properties) {
        	String setterName = getSetterName(property, false);
        	if (nameCount.get(setterName) > 1)
        		setterName = getGetterName(property, true);

			Resource methodType = property.getPropertyResourceValue(RDFS.range);
			if (typeOverrides!=null) {
				Resource override = typeOverrides.get(property);
				if (override!=null)
					methodType = override;
			}

			String methodTypeName = getTypeName(methodType);
			String methodTypePackName = getPackageName(methodType);
			if (!methodTypePackName.equals("xsd") && !methodTypePackName.equals(typePackage))
				methodTypeName = methodTypePackName+"."+methodTypeName;

			if (isArrayProperty(type, property))
				methodTypeName += "[]";

			IMethod aMethod = aType.getMethod(setterName, new String[]{Signature.createTypeSignature(methodTypeName, false)});
            
            if (aMethod.exists() && isGenerated(aMethod)) {
    			try {
    				aMethod.delete(true, null);
    			} catch (Exception e) {
    				Activator.getDefault().log("Error deleting Java method", e);
    				error=true;
    			}
    		}

            if (!aMethod.exists() || isGenerated(aMethod)) {
            	String fieldName = getFieldName(property, false);
            	if (nameCount.get(setterName) > 1)
    				fieldName = getFieldName(property, true);

        		String comment;
        		if (genComment) {
    				comment = DTAUtilities.getStringValue(property, RDFS.comment);
    				comment = createComment(new String[]{"@param "+fieldName+" "+comment});
        		} else
        			comment = "@Override\n";

       			String body = genBody ? "{\n\tthis."+fieldName+" = "+fieldName+";\n}" : ";";
            	
            	String genAnnot = "@Generated(\"true\")\n";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
		        	aMethod = aType.createMethod(comment+genAnnot+propAnnot+"public void "+setterName+"("+methodTypeName+" "+fieldName+")"+body, null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private boolean generateHashCode(IType aType) {
		boolean error=false;
		
    	String genAnnot = "@Generated(\"true\")\n";
    	String genOverride = "@Override\n";
    	String body = "";

		try {
			IField[] fields= aType.getFields();
			if (fields.length == 0)
				return error;
			
	    	body += "\tfinal int prime = 31;\n";
	    	body += "\tint result = 1;\n";
	    	for(IField field : fields) {
    			String typeSig = field.getTypeSignature();
    			String fieldName = field.getElementName();;
    			if (typeSig.startsWith("[")) {
    				body += "\tresult = prime * result + java.util.Arrays.hashCode("+fieldName+");\n";
    			} else if (typeSig.equals("I")) {
    				body += "\tresult = prime * result + "+fieldName+";\n";
    			} else if (typeSig.equals("Z")) {
    				body += "\tresult = prime * result + ("+fieldName+" ? 1231 : 1237);\n";
    			} else if (typeSig.equals("D")) {
    				body += "\tlong temp = Double.doubleToLongBits("+fieldName+");\n";
    				body += "\tresult = prime * result + (int) (temp ^ (temp >>> 32));\n";
    			} else {
    				body += "\tresult = prime * result + (("+fieldName+" == null) ? 0 : "+fieldName+".hashCode());\n";
    			}
	    	}
	    	body += "\treturn result;";
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
        try {
        	aType.createMethod(genOverride+genAnnot+"public int hashCode() {\n"+body+"\n}", null, true, null);
		} catch (Exception e) {
			Activator.getDefault().log("Error creating hashCode() method", e);
			error=true;
		}
		return error;
	}

	private boolean generateEquals(IType aType) {
		boolean error=false;
		
    	String genAnnot = "@Generated(\"true\")\n";
    	String genOverride = "@Override\n";
    	String body = "";

		try {
			IField[] fields= aType.getFields();
			if (fields.length == 0)
				return error;
			
	    	body += "\tif (this == obj)\n";
	    	body += "\t\treturn true;\n";
	    	body += "\tif (obj == null)\n";
	    	body += "\t\treturn false;\n";
	    	body += "\tif (getClass() != obj.getClass())\n";
	    	body += "\t\treturn false;\n";
	    	body += "\t"+aType.getElementName()+" other = ("+aType.getElementName()+") obj;\n";
	    	for(IField field : fields) {
				String typeSig = field.getTypeSignature();
				String fieldName = field.getElementName();;
    			if (typeSig.startsWith("[")) {
					body += "\tif (!java.util.Arrays.equals("+fieldName+", other."+fieldName+"))\n";
					body += "\t\treturn false;\n";
    			} else if (typeSig.equals("I")) {
					body += "\tif ("+fieldName+" != other."+fieldName+")\n";
					body += "\t\treturn false;\n";
				} else if (typeSig.equals("Z")) {
					body += "\tif ("+fieldName+" != other."+fieldName+")\n";
					body += "\t\treturn false;\n";
				} else if (typeSig.equals("D")) {
					body += "\tif (Double.doubleToLongBits("+fieldName+") != Double.doubleToLongBits(other."+fieldName+"))\n";
					body += "\t\treturn false;\n";
				} else {
					body += "\tif ("+fieldName+" == null) {\n";
					body += "\t\tif (other."+fieldName+" != null)\n";
					body += "\t\t\treturn false;\n";
					body += "\t} else if (!"+fieldName+".equals(other."+fieldName+"))\n";
					body += "\t\treturn false;\n";
				}
	    	}
	    	body += "\treturn true;";
		} catch (Exception e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
   	
        try {
        	aType.createMethod(genOverride+genAnnot+"public boolean equals(Object obj) {\n"+body+"\n}", null, true, null);
		} catch (Exception e) {
			Activator.getDefault().log("Error creating equals() method", e);
			error=true;
		}
		return error;
	}

	private boolean generateToString(IType aType) {
		boolean error=false;
		
    	String genAnnot = "@Generated(\"true\")\n";
    	String genOverride = "@Override\n";
    	String body = "";

		try {
			IField[] fields= aType.getFields();
			if (fields.length == 0)
				return error;
			
	    	body += "\treturn \""+aType.getElementName()+" [ \"+";
	    	for(IField field : fields) {
				String fieldName = field.getElementName();
				String valueName = fieldName;
				String typeSig = field.getTypeSignature();
    			if (typeSig.startsWith("["))
    				valueName = "java.util.Arrays.toString("+valueName+")";
				body += "\t\t\t\""+fieldName+"=\"+"+valueName+"+";
				body += (field==fields[fields.length-1]) ? "\t\t\" ]\";" : "\", \"+";
	    	}
		} catch (Exception e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
   	
        try {
        	aType.createMethod(genOverride+genAnnot+"public String toString() {\n"+body+"\n}", null, true, null);
		} catch (Exception e) {
			Activator.getDefault().log("Error creating toString() method", e);
			error=true;
		}
		return error;
	}

	private boolean generateSourceClass(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType aType = aJavaFile.getType(typeName);

        if (!aType.exists()) {
    		String comment = DTAUtilities.getStringValue(operation, RDFS.comment);
			comment = comment.length()>0 ? createComment(new String[]{comment}) : comment;

			String genAnnot = "@Generated(\"true\")\n";
	        String uriAnnot = "@JsonTypeName(\""+operation.getURI()+"\")\n";

			try {
				aType = aJavaFile.createType(comment+genAnnot+uriAnnot+"public class "+typeName+" implements Callable {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        } else
        	error |= updateOperationClass(aJavaFile, operation);

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
			aJavaFile.createImport("org.mule.api.lifecycle.Callable", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
		error |= generateOnCall(aType, operation);
		error |= generateExecute(aType, operation);
		
		monitor.done();
		return error;
	}			

	private boolean generateProcessorClass(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType aType = aJavaFile.getType(typeName);

        if (!aType.exists()) {
    		String comment = DTAUtilities.getStringValue(operation, RDFS.comment);
			comment = comment.length()>0 ? createComment(new String[]{comment}) : comment;

			String genAnnot = "@Generated(\"true\")\n";
	        String uriAnnot = "@JsonTypeName(\""+operation.getURI()+"\")\n";

			try {
				aType = aJavaFile.createType(comment+genAnnot+uriAnnot+"public class "+typeName+" implements Callable {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        } else
        	error |= updateOperationClass(aJavaFile, operation);

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
			aJavaFile.createImport("org.mule.api.lifecycle.Callable", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
		error |= generateOnCall(aType, operation);
		error |= generateParameter(aPackage, operation, new SubProgressMonitor(monitor, 1));
		
		monitor.done();
		return error;
	}			

	@SuppressWarnings("unchecked")
	private boolean updateOperationClass(ICompilationUnit aJavaFile, Resource operation) {
		boolean error = false;
		try {
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setSource(aJavaFile);

			CompilationUnit astRoot = (CompilationUnit) parser.createAST(null);
			astRoot.recordModifications();
			AST ast = astRoot.getAST();
		 
			TypeDeclaration typeDecl = (TypeDeclaration) astRoot.types().get(0);

			typeDecl.setJavadoc(null);
			String comment = DTAUtilities.getStringValue(operation, RDFS.comment);
			if (comment.length()>0) {
				Javadoc javadoc = ast.newJavadoc();
				typeDecl.setJavadoc(javadoc);
				TagElement newTag= ast.newTagElement();
				javadoc.tags().add(newTag);
				TextElement text = ast.newTextElement();
				text.setText(comment);
				newTag.fragments().add(text);
			}
			
			typeDecl.superInterfaceTypes().clear();
			SimpleType type = ast.newSimpleType(ast.newName("Callable"));
			typeDecl.superInterfaceTypes().add(type);
			
			try {
				aJavaFile.createImport("org.mule.api.lifecycle.Callable", null, null);
			} catch (JavaModelException e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
			
			Document document = new Document(aJavaFile.getSource());
			TextEdit edits = astRoot.rewrite(document, aJavaFile.getJavaProject().getOptions(true));
		    aJavaFile.applyTextEdit(edits, null);
		} catch (Exception e) {
			Activator.getDefault().log("Error updating Java type", e);
			error=true;
		}
		return error;
	}

	private boolean generateOnCall(IType aType, Resource operation) {
		boolean error=false;

		String typePackage = getPackageName(operation);
		String methodName = "onCall";
		
		String parameterTypeName = "MuleEventContext";
    	String parameterName = toCamelCase(parameterTypeName);
 
		Resource parameterType;
		if (DTAUtilities.isSubscribe(operation)) {
			Resource dataProperty = operation.getPropertyResourceValue(DTA.data);
			parameterType = dataProperty.getPropertyResourceValue(RDFS.range);
		} else
			parameterType = operation.getPropertyResourceValue(DTA.parameter);

		String realParameterTypeName = getTypeName(parameterType);
		String parameterTypePackName = getPackageName(parameterType);
		if (!parameterTypePackName.equals("xsd") && !parameterTypePackName.equals(typePackage))
			realParameterTypeName = parameterTypePackName+"."+realParameterTypeName;
		if (DTAUtilities.isSubscribe(operation)) {
			Resource dataProperty = operation.getPropertyResourceValue(DTA.data);
			if (isArrayData(operation, dataProperty.as(Property.class)))
				realParameterTypeName += "[]";
		}

    	String dataTypeName;
    	if (isProcessor(operation))
    		dataTypeName = "Parameter";
    	else
    		dataTypeName = "MuleMessage";

    	String body = "{\n";
    	if (isProcessor(operation)) {
   	    	body += "\tParameter parameter = new Parameter();\n";
   	    	body += "\treturn parameter;\n";
    	} else {
    		body += "\tMuleMessage message = "+parameterName+".getMessage();\n";
   			body += "\t"+realParameterTypeName+" parameter = ("+realParameterTypeName+") message.getPayload();\n";
   			body += "\tObject data = execute(parameter, message);\n";
    		body += "\tmessage.setPayload(data);\n";
    		body += "\treturn message;\n";
    	}
    	body += "}";

		try {
	    	for (IMethod aMethod : aType.getMethods()) {
	    		if (aMethod.getElementName().equals(methodName) && isGenerated(aMethod))
					aMethod.delete(true, null);
	    	}
		} catch (Exception e) {
			Activator.getDefault().log("Error deleting Java methods", e);
			error=true;
		}
   	
    	IMethod aMethod = aType.getMethod(methodName, new String[]{Signature.createTypeSignature(parameterTypeName, false)});
        
       if (!aMethod.exists()) {
        	String genAnnot = "@Generated(\"true\")\n";

			String comment = createComment(new String[]{"@param "+parameterName, "@return "+dataTypeName});

	        try {
	        	aMethod = aType.createMethod(comment+genAnnot+"public "+dataTypeName+" "+methodName+"("+parameterTypeName+" "+parameterName+") throws Exception "+body, null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			ICompilationUnit aJavaFile = aType.getCompilationUnit();
			aJavaFile.createImport("org.mule.api.MuleEventContext", null, null);
	    	if (isSource(operation))
	    		aJavaFile.createImport("org.mule.api.MuleMessage", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}

		return error;
	}
	
	private boolean generateExecute(IType aType, Resource operation) {
		boolean error=false;

		String typePackage = getPackageName(operation);
		String methodName = "execute";

		boolean isArrayParameter;
		Resource parameterType;
		Resource dataProperty;
		if (DTAUtilities.isSubscribe(operation)) {
			dataProperty = operation.getPropertyResourceValue(DTA.data);
			isArrayParameter = isArrayData(operation, dataProperty.as(Property.class));
			parameterType = dataProperty.getPropertyResourceValue(RDFS.range);
			dataProperty = null;
		} else {
			isArrayParameter = false;
			parameterType = operation.getPropertyResourceValue(DTA.parameter);
			dataProperty = operation.getPropertyResourceValue(DTA.data);
		}

		String parameterTypeName = getTypeName(parameterType);
		String parameterTypePackName = getPackageName(parameterType);
		if (!parameterTypePackName.equals("xsd") && !parameterTypePackName.equals(typePackage))
			parameterTypeName = parameterTypePackName+"."+parameterTypeName;

    	String dataTypeName = "null";
    	if (dataProperty != null) {
    		Resource dataType = dataProperty.getPropertyResourceValue(RDFS.range);
    		dataTypeName = getTypeName(dataType);
    		String dataTypePackName = getPackageName(dataType);
			if (!dataTypePackName.equals("xsd") && !dataTypePackName.equals(typePackage))
				dataTypeName = dataTypePackName+"."+dataTypeName;
    		boolean dataIsArray = isArrayData(operation, dataProperty.as(Property.class));
    		if (dataIsArray)
    			dataTypeName += "[]";
    		if (dataProperty.canAs(DatatypeProperty.class))
    			dataTypeName += " '"+DTAUtilities.getLabel(dataProperty)+"'";
    	}

    	String body = "{\n\treturn null;\n}";

		try {
	    	for (IMethod aMethod : aType.getMethods()) {
	    		if (aMethod.getElementName().equals(methodName) && isGenerated(aMethod))
					aMethod.delete(true, null);
	    	}
		} catch (Exception e) {
			Activator.getDefault().log("Error deleting Java methods", e);
			error=true;
		}
   	
		Map<String, Boolean> methodParamTypeNames = new LinkedHashMap<String, Boolean>();
		methodParamTypeNames.put(parameterTypeName, Boolean.valueOf(isArrayParameter));
		methodParamTypeNames.put("MuleMessage", Boolean.FALSE);
		
		List<String> methodParamSignatures = new ArrayList<String>();
		for(String methodParamTypeName : methodParamTypeNames.keySet())
			methodParamSignatures.add(Signature.createTypeSignature(methodParamTypeName, false));
		IMethod aMethod = aType.getMethod(methodName, methodParamSignatures.toArray(new String[0]));
        
        if (!aMethod.exists()) {
        	String genAnnot = "@Generated(\"true\")\n";

    		List<String> lines = new ArrayList<String>();
        	lines.add("Execute the operation (put your implementation here)");
        	lines.add("To report error code, call muleMessage.setOutboundProperty(\"code\", <integer>)");
        	lines.add("To report error description, call muleMessage.setOutboundProperty(\"description\", <string>)");
        	lines.add("");
    		for(String methodParamTypeName : methodParamTypeNames.keySet()) {
    			String[] s = methodParamTypeName.split("\\.");
    			lines.add("@param "+toCamelCase(s[s.length-1]));
    		}
        	lines.add("@return "+dataTypeName+ " (or another Object if this processor is not terminal)");

        	String comment = createComment(lines.toArray(new String[0]));

    		String methodParams = "";
    		for(String methodParamTypeName : methodParamTypeNames.keySet()) {
   				methodParams += (methodParams.length()>0) ? ", " : "";
    			String[] s = methodParamTypeName.split("\\.");
    			methodParams += methodParamTypeName+(methodParamTypeNames.get(methodParamTypeName)? "[] ":" ")+toCamelCase(s[s.length-1]);
    		}
    		
	        try {
	        	aMethod = aType.createMethod(comment+genAnnot+"private Object "+methodName+"("+methodParams+") throws Exception "+body, null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }
        return error;
	}

	private String createComment(String[] lines) {
		String comment = "/**\n";
		for (String line : lines)
			comment += " * "+line+"\n";
		comment += " */\n";
		return comment;
	}
	
	private String getPackageName(Resource res) {
		if (res == null || DTAUtilities.isDatatype(res))
			return "xsd";
		String prefix = res.getModel().getNsURIPrefix(res.getNameSpace());
		Resource ontology = ((OntModel)res.getModel()).listOntologies().next();
		String basePackage = DTAUtilities.getStringValue(ontology, DTA.basepackage);
		String name = basePackage.length()>0 ? basePackage : BASE_PACKAGE;
		name += prefix.length()>0 ? "."+prefix : "";
		return name.toLowerCase();
	}

	private String getTypeName(Resource res) {
		if (res == null || RDFS.Literal.equals(res))
			return "Object";
		if (XSD.getURI().equalsIgnoreCase(res.getNameSpace())) {
			if (XSD.xstring.equals(res))
				return "String";
			if (XSD.integer.equals(res))
				return "int";
			if (XSD.decimal.equals(res))
				return "double";
			if (XSD.xboolean.equals(res))
				return "boolean";
			if (XSD.dateTime.equals(res) || XSD.time.equals(res) || XSD.date.equals(res))
				return "java.util.Calendar";
			if (XSD.duration.equals(res))
				return "javax.xml.datatype.Duration";
			if (XSD.hexBinary.equals(res))
				return "byte[]";
		}
		return toTitleCase(res.getLocalName());
	}

	private String getFieldName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return toCamelCase(name)+postfix;
	}

	private String getGetterName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		Resource type = res.getPropertyResourceValue(RDFS.range);
		String prefix = (type != null && type.equals(XSD.xboolean)) ? "is" : "get";
		prefix = name.startsWith(prefix) ? "" : prefix;
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return prefix+toTitleCase(name)+postfix;
	}

	private String getSetterName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		String prefix = "set";
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return prefix+toTitleCase(name)+postfix;
	}

	private String toCamelCase(String s) {
		return s.substring(0, 1).toLowerCase()+s.substring(1);
	}

	private String toTitleCase(String s) {
		return s.substring(0, 1).toUpperCase()+s.substring(1);
	}

    public static boolean isArrayProperty(Resource type, Property property) {
    	for(OntClass t : DTAUtilities.listSelfAndAllSuperClasses(type)) {
        	Set<OntProperty> properties = DTAUtilities.listDeclaredProperties(t);
    		if (properties.contains(property)) {
		    	Restriction restriction = DTAUtilities.getRestriction(t,  DTA.restriction, property);
		    	if (restriction==null || restriction.isMinCardinalityRestriction())
		    		return true;
    		}
    	}
    	return false;
    }

    public static boolean isArrayData(Resource type, Property property) {
    	Restriction restriction = DTAUtilities.getRestriction(type, DTA.dataRestriction, property);
    	return restriction==null || restriction.isMinCardinalityRestriction();
    }

	private boolean isGenerated(IAnnotatable a) {
		IAnnotation generated = a.getAnnotation("Generated");
		if (generated.exists()) {
			try {
				IMemberValuePair[] pairs = generated.getMemberValuePairs();
				if (pairs.length==1 && pairs[0].getValue().equals("true"))
					return true;
			} catch (JavaModelException e) {
			}
		}
		return false;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean formatCompilationUnit(ICompilationUnit aJavaFile) {
		try {
			Map options = aJavaFile.getJavaProject().getOptions(true);
			options.put(DefaultCodeFormatterConstants.FORMATTER_BLANK_LINES_BEFORE_FIELD, "1");
			CodeFormatter formatter = ToolFactory.createCodeFormatter(options);
			String source = aJavaFile.getSource();
		    TextEdit formatEdit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, source, 0, source.length(), 0, null);
		    aJavaFile.applyTextEdit(formatEdit, null);
		    if (!aJavaFile.isWorkingCopy())
		    	aJavaFile.save(null, false);
		} catch (Exception e) {
			Activator.getDefault().log("Error formatting Java file", e);
			return true;
		}
		return false;
	}
	
	private boolean isSource(Resource resource) {
		return (DTAUtilities.isOperation(resource) && !DTAUtilities.isPublish(resource)) || DTAUtilities.isSubscribe(resource);
	}
	
	private boolean isProcessor(Resource resource) {
		return (DTAUtilities.isRequest(resource) && !DTAUtilities.isSubscribe(resource)) || DTAUtilities.isPublish(resource);
	}

}

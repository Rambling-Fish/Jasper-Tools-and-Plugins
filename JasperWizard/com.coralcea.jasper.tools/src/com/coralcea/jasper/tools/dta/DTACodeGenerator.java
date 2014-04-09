package com.coralcea.jasper.tools.dta;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Shell;
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
	
	public static void run(Shell shell, IFile file, final OntModel model) {
		try {
			MessageDialog question = new MessageDialog(shell, "Code Generation", null, "Are you sure you want to generate code ( in the 'src/main/java' folder)?", MessageDialog.CONFIRM, new String[]{"Yes", "No"}, 0);
			if (question.open() != MessageDialog.OK)
				return;
			new ProgressMonitorDialog(shell).run(true, false, getRunnable(shell, file, model));
		} catch (InterruptedException e) {
			if (e.getMessage().equals("Invalid")) {
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Validation problems can be inspected in Problems view");
				StatusManager.getManager().handle(status, StatusManager.SHOW);
			}
		} catch (InvocationTargetException e) {
			Activator.getDefault().log("Error during code generation", e);
		}
	}
	
	public static IRunnableWithProgress getRunnable(final Shell shell, final IFile file, final OntModel model) {
		return new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				monitor.beginTask("Generating code", 10);
				try {
					ResourcesPlugin.getWorkspace().run(DTAModelValidator.getRunnable(file, model), new SubProgressMonitor(monitor, 3));
					if (file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO).length!=0) {
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Validation problems can be inspected in Problems view");
						StatusManager.getManager().handle(status, StatusManager.BLOCK);
						return;
					}
					
					IResource container = file.getProject().findMember("src/main/java");
					if (container==null || !container.exists()) {
						Activator.getDefault().log("Could not find a folder named src/main/java in project '"+file.getProject().getName()+"'");
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Errors were detected code generation. Please refer to the log for details.");
						StatusManager.getManager().handle(status, StatusManager.BLOCK);
						return;
					}
					
					DTACodeGenerator generator = new DTACodeGenerator();
					if (generator.generate((IContainer)container, model, new SubProgressMonitor(monitor, 7))) {
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Errors were detected code generation. Please refer to the log for details.");
						StatusManager.getManager().handle(status, StatusManager.BLOCK);
					}
				} catch (CoreException e) {
					Activator.getDefault().log(e);
				} finally {
					monitor.done();
				}
			}
		};
	}

	private boolean generate(IContainer container, OntModel model, IProgressMonitor monitor) {
		boolean error=false;
		
		monitor.beginTask("Generating Code", 10);

		Resource ontology = model.listOntologies().next();
		String basePackage = DTAUtilities.getStringValue(ontology, DTA.basepackage);
		final Pattern pattern = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");
		if (basePackage.length()>0 && !pattern.matcher(basePackage).matches()) {
			MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, Status.ERROR, "The base package '"+basePackage+"' is invalid", null);
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
			return false;
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
			monitor.done();
			return false;
		}
		
		monitor.worked(1);
		
		IPackageFragmentRoot root = (IPackageFragmentRoot) JavaCore.create(container);
		error |= generatePackages(root, packages, new SubProgressMonitor(monitor, 1));
		error |= generateTypes(root, types, new SubProgressMonitor(monitor, 4));
		error |= generateOperations(root, operations, new SubProgressMonitor(monitor, 2));
		error |= generateRequests(root, requests, new SubProgressMonitor(monitor, 2));
		
		monitor.done();

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
		        
				if (operation.hasProperty(DTA.input))
					collectType(operation.getPropertyResourceValue(DTA.input), packages, types, namespaces);
				
				if (operation.hasProperty(DTA.output))
					collectProperty(operation.getPropertyResourceValue(DTA.output), packages, types, namespaces);
			}

			for(Resource request : DTAUtilities.listObjects(dta, DTA.request, Resource.class)) {
				requests.add(request);
		        packages.add(getPackageName(request));
		        namespaces.add(request.getNameSpace());
		        
				if (request.hasProperty(DTA.input)) {
					Resource input = request.getPropertyResourceValue(DTA.input);
					for (OntProperty p : DTAUtilities.listAllProperties(input))
						collectProperty(p, packages, types, namespaces);
					for (AllValuesFromRestriction restriction : DTAUtilities.listObjects(request, DTA.inputRestriction, AllValuesFromRestriction.class))
						collectType(restriction.getPropertyResourceValue(OWL.allValuesFrom), packages, types, namespaces);
				}
				
				if (request.hasProperty(DTA.output)) 
					collectProperty(request.getPropertyResourceValue(DTA.output), packages, types, namespaces);
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
		if (!DTAUtilities.isDatatype(ptype))
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
			error |= generateOperationClass(aPackage, operation, new SubProgressMonitor(monitor, 1));
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
			error |= generateOperationClass(aPackage, request, new SubProgressMonitor(monitor, 1));
			error |= generateRequestParams(aPackage, request, new SubProgressMonitor(monitor, 1));
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

	private boolean generateRequestParams(IPackageFragment aPackage, Resource request, IProgressMonitor monitor) {
		boolean error=false;
 		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(request);
		String typePackage = getPackageName(request);

		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType opType = aJavaFile.getType(typeName);

		String className = "Parameters";
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
			String comment = createComment(new String[]{"The parameters of {@link "+typeName+"}"});

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

		OntClass input = DTAUtilities.getInput(request);
		Map<OntProperty, Resource> typeOverrides = new HashMap<OntProperty, Resource>();
		for(AllValuesFromRestriction r : DTAUtilities.listObjects(request, DTA.inputRestriction, AllValuesFromRestriction.class))
			typeOverrides.put(r.getOnProperty(), r.getAllValuesFrom());
			
		error |= generateFields(aType, input, typeOverrides);
		error |= generateGetters(aType, input, typeOverrides, true, true, true);
		error |= generateSetters(aType, input, typeOverrides, true, true, true);
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
    			if (isArray(type, property, DTA.restriction))
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

    			if (isArray(type, property, DTA.restriction))
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

			if (isArray(type, property, DTA.restriction))
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

	private boolean generateOperationClass(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
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
		if (DTAUtilities.isOperation(operation))
			error |= generateProcess(aType, operation);
		
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

		Resource inputType = operation.getPropertyResourceValue(DTA.input);

		String typePackage = getPackageName(operation);
		String methodName = "onCall";
		
		String inputTypeName = "MuleEventContext";
    	String inputName = toCamelCase(inputTypeName);
 
		String realInputTypeName = getTypeName(inputType);
		String inputTypePackName = getPackageName(inputType);
		if (!inputTypePackName.equals("xsd") && !inputTypePackName.equals(typePackage))
			realInputTypeName = inputTypePackName+"."+realInputTypeName;

    	String outputTypeName;
    	if (DTAUtilities.isRequest(operation))
    		outputTypeName = "Parameters";
    	else
    		outputTypeName = "MuleMessage";

    	String body = "{\n";
    	if (DTAUtilities.isRequest(operation)) {
   	    	body += "\tParameters parameters = new Parameters();\n";
   	    	body += "\treturn parameters;\n";
    	} else {
    		body += "\tMuleMessage message = "+inputName+".getMessage();\n";
    		body += "\t"+realInputTypeName+" input = ("+realInputTypeName+") message.getPayload();\n";
    		body += "\tObject output = execute(input, message);\n";
    		body += "\tmessage.setPayload(output);\n";
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
   	
    	IMethod aMethod = aType.getMethod(methodName, new String[]{Signature.createTypeSignature(inputTypeName, false)});
        
       if (!aMethod.exists()) {
        	String genAnnot = "@Generated(\"true\")\n";

			String comment = createComment(new String[]{"@param "+inputName, "@return "+outputTypeName});

	        try {
	        	aMethod = aType.createMethod(comment+genAnnot+"public "+outputTypeName+" "+methodName+"("+inputTypeName+" "+inputName+") throws Exception "+body, null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			ICompilationUnit aJavaFile = aType.getCompilationUnit();
			aJavaFile.createImport("org.mule.api.MuleEventContext", null, null);
	    	if (DTAUtilities.isOperation(operation))
	    		aJavaFile.createImport("org.mule.api.MuleMessage", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}

		return error;
	}
	
	private boolean generateProcess(IType aType, Resource operation) {
		boolean error=false;

		String typePackage = getPackageName(operation);
		String methodName = "execute";

		Resource inputType = operation.getPropertyResourceValue(DTA.input);
		Resource outputProperty = operation.getPropertyResourceValue(DTA.output);

		String inputTypeName = getTypeName(inputType);
		String inputTypePackName = getPackageName(inputType);
		if (!inputTypePackName.equals("xsd") && !inputTypePackName.equals(typePackage))
			inputTypeName = inputTypePackName+"."+inputTypeName;
    	String inputName = toCamelCase(inputTypeName);

    	String outputTypeName;
    	if (outputProperty != null) {
    		Resource outputType = outputProperty.getPropertyResourceValue(RDFS.range);
    		outputTypeName = getTypeName(outputType);
    		String outputTypePackName = getPackageName(outputType);
			if (!outputTypePackName.equals("xsd") && !outputTypePackName.equals(typePackage))
				outputTypeName = outputTypePackName+"."+outputTypeName;
    		boolean outputIsArray = isArray(operation, outputProperty.as(Property.class), DTA.outputRestriction);
    		if (outputIsArray)
    			outputTypeName += "[]";
    		if (outputProperty.canAs(DatatypeProperty.class))
    			outputTypeName += " '"+DTAUtilities.getLabel(outputProperty)+"'";
    	} else
    		outputTypeName = "null";

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
   	
    	IMethod aMethod = aType.getMethod(methodName, new String[]{Signature.createTypeSignature(inputTypeName, false)});
        
       if (!aMethod.exists()) {
        	String genAnnot = "@Generated(\"true\")\n";

        	String line1 = "Execute the operation (put your implementation here)";
        	String line2 = "To report error code, call muleMessage.setOutboundProperty(\"code\", <integer>)";
        	String line3 = "To report error description, call muleMessage.setOutboundProperty(\"description\", <string>)";
			String comment = createComment(new String[]{line1, line2, line3, "", "@param "+inputName, "@param muleMessage", "@return "+outputTypeName+ " (or another Object if this processor is not terminal)"});

	        try {
	        	aMethod = aType.createMethod(comment+genAnnot+"private Object "+methodName+"("+inputTypeName+" "+inputName+", MuleMessage muleMessage) throws Exception "+body, null, true, null);
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
		if (DTAUtilities.isDatatype(res))
			return "xsd";
		String prefix = res.getModel().getNsURIPrefix(res.getNameSpace());
		Resource ontology = ((OntModel)res.getModel()).listOntologies().next();
		String basePackage = DTAUtilities.getStringValue(ontology, DTA.basepackage);
		String name = basePackage.length()>0 ? basePackage : BASE_PACKAGE;
		name += prefix.length()>0 ? "."+prefix : "";
		return name.toLowerCase();
	}

	private String getTypeName(Resource res) {
		String name = res.getLocalName();
		name = toTitleCase(name);
		if (getPackageName(res).equals("xsd")) {
			if (name.equals("Integer"))
				name = "int";
			else if (name.equals("Boolean"))
				name = "boolean";
			else if (name.equals("Date"))
				name = "java.util.Date";
			else if (name.equals("Decimal"))
				name = "double";
			else if (name.equals("Literal"))
				name = "String";
		} 
		return name;
	}

	private String getFieldName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return toCamelCase(name)+postfix;
	}

	private String getGetterName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		Resource type = res.getPropertyResourceValue(RDFS.range);
		String prefix = type.equals(XSD.xboolean) ? "is" : "get";
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

    public static boolean isArray(Resource type, Property property, Property kind) {
    	for(Resource t : DTAUtilities.listSelfAndAllSuperClasses(type)) {
        	Set<OntProperty> properties = DTAUtilities.listDeclaredProperties(t);
    		if (properties.contains(property)) {
		    	Restriction restriction = DTAUtilities.getRestriction(t, kind, property);
		    	if (restriction==null || restriction.isMinCardinalityRestriction())
		    		return true;
    		}
    	}
    	return false;
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
	
}

package com.coralcea.jasper.tools.dta.codegen;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTACodeGenerator {

	private static final String BASE_PACKAGE = "base";
	
	public static void run(Shell shell, final IFile file) {
		final OntModel model;
		
		try {
			model = DTACore.getModel(file);
		} catch (CoreException e1) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed to open the DTA file");
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return;
		}
		
		final Set<Resource> relevantOps = new HashSet<Resource>();
		final Set<Resource> relevantTypes = new HashSet<Resource>();
		final Set<String> missingNamespaces = new HashSet<String>();
		final Set<String> syntaxErrors = new HashSet<String>();
		
		try {
			validate(model, relevantOps, relevantTypes, syntaxErrors, missingNamespaces);
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "An error occurred validating model: "+print(syntaxErrors));
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		}

		if (syntaxErrors.size() > 0) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "The following syntax errors were found in the model:\n"+print(syntaxErrors));
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return;
		}
		if (missingNamespaces.size() > 0) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "The following namespaces are missing their prefixes:\n"+print(missingNamespaces));
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return;
		}
		
		MessageDialog proceed = new MessageDialog(shell, "Generate Code", null, "Are you sure you want to generate code?", MessageDialog.QUESTION_WITH_CANCEL, new String[]{"Yes", "No"}, 0);
		if (proceed.open() != MessageDialog.OK) {
			return;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (generate(file, model, relevantOps, relevantTypes, monitor)) {
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "An error occurred during code generation. Check error log for more details");
						StatusManager.getManager().handle(status, StatusManager.SHOW);
					}
				} finally {
					monitor.done();
				}
			}
		};
		
		try {
			new ProgressMonitorDialog(shell).run(true, false, runnable);
		} catch (Exception e) {
			Activator.getDefault().log("Failed to generate code", e);
		}
	}
	
	private static void validate(OntModel model, Set<Resource> relevantOps, Set<Resource> relevantTypes, Set<String> syntaxErrors, Set<String> missingNamespaces) {
		Model base = model.getBaseModel();
		Map<String, String> nsMap = model.getNsPrefixMap();
		
		for (NodeIterator i = model.listObjectsOfProperty(DTA.operation); i.hasNext(); ) {
			Resource op = (Resource) i.next();
			if (DTAUtilities.isDefinedBy(base, op)) {
				relevantOps.add(op);
				if (nsMap.get(op.getNameSpace()) != null)
					missingNamespaces.add(op.getNameSpace());
			}
			Resource kind = DTAUtilities.getRDFType(op);
			Resource input = op.getPropertyResourceValue(DTA.input);
			if (input == null && !DTA.Post.equals(kind))
				syntaxErrors.add("Operation <"+op.getURI()+"> does not have an input type");
			if (input != null)
				collectRelevantTypes(input, relevantTypes);
			Resource output = op.getPropertyResourceValue(DTA.output);
			if (output == null && !DTA.Receive.equals(kind))
				syntaxErrors.add("Operation <"+op.getURI()+"> does not have an output type");
			if (output != null)
				collectRelevantTypes(output, relevantTypes);
		}
		
		for (Resource type : relevantTypes) {
			if (nsMap.get(type.getNameSpace()) != null)
				missingNamespaces.add(type.getNameSpace());
			for (OntProperty p : DTAUtilities.getDeclaredProperties(type)) {
				if (nsMap.get(p.getNameSpace()) != null)
					missingNamespaces.add(p.getNameSpace());
				if (p.getPropertyResourceValue(RDFS.range) == null)
					syntaxErrors.add("Property <"+p.getURI()+"> does not have a type");
	        }
		}
	}
	
	private static boolean generate(IFile file, OntModel model, Set<Resource> relevantOps, Set<Resource> relevantTypes, IProgressMonitor monitor) {
		boolean error=false;
		
		IProject project = file.getProject();
		IFolder folder = project.getFolder("src/main/java/");
		IPackageFragmentRoot root = (IPackageFragmentRoot) JavaCore.create(folder);

		monitor.beginTask("Generating Code", 10);
		error |= generatePackages(root, model, relevantOps, relevantTypes, new SubProgressMonitor(monitor, 1));
		error |= generateTypes(root, model, relevantOps, relevantTypes, new SubProgressMonitor(monitor, 9));
		monitor.done();

		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			Activator.getDefault().log("Failed to generate/update folder", e);
			error=true;
		}
		return error;
	}
	
	private static boolean generatePackages(IPackageFragmentRoot root, OntModel model, Set<Resource> relevantOps, Set<Resource> relevantTypes, IProgressMonitor monitor) {
		boolean error=false;
		
		Set<String> packageNames = new HashSet<String>();
		for (Resource r : relevantOps)
			packageNames.add(getPackageName(r));
		for (Resource r : relevantTypes)
			packageNames.add(getPackageName(r));
		
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

	private static boolean generateTypes(IPackageFragmentRoot root, OntModel model, Set<Resource> relevantOps, Set<Resource> relevantTypes, IProgressMonitor monitor) {
		boolean error=false;
		
		monitor.beginTask("Generating types", 2*relevantTypes.size()+relevantOps.size());
		for(Resource type : relevantOps) {
			String typeName = getTypeName(type);
			String packageName = getPackageName(type);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
			error |= generateOperationClass(aJavaFile, type, new SubProgressMonitor(monitor, 1));
		}
		for(Resource type : relevantTypes) {
			String typeName = getTypeName(type);
			String packageName = getPackageName(type);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			ICompilationUnit aJavaInterfaceFile = aPackage.getCompilationUnit(typeName+".java");
			ICompilationUnit aJavaClassFile = aPackage.getCompilationUnit(typeName+"Impl.java");
			error |= generateTypeInterface(aJavaInterfaceFile, type, new SubProgressMonitor(monitor, 1));
			error |= generateTypeClass(aJavaClassFile, type, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
		return error;
	}
	
	private static boolean generateTypeClass(ICompilationUnit aJavaFile, Resource type, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(type);
        IType aType = aJavaFile.getType(typeName+"Impl");

        if (aType.exists() && isGenerated(aType)) {
			try {
				aType.delete(true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error deleting Java type", e);
				error=true;
			}
		}
        
        if (!aType.exists() || isGenerated(aType)) {
        	String superTypeNames = " implements "+typeName;
        	
	        String genAnnot = "@Generated(\"true\")";
	        String uriAnnot = "@JsonTypeName(\""+type.getURI()+"\")\n";
	        String infoAnnot = "@JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY, property=\"@type\")\n";

			try {
				aType = aJavaFile.createType(genAnnot+uriAnnot+infoAnnot+"public class "+typeName+"Impl"+superTypeNames+" {\n}", null, true, null);
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

		error |= generateFields(aType, type);
		error |= generateGetters(aType, type, true);
		error |= generateSetters(aType, type, true);
		
		monitor.done();
		return error;
	}			

	private static boolean generateTypeInterface(ICompilationUnit aJavaFile, Resource type, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating interface", 1);

		String typeName = getTypeName(type);
		String typePackage = getPackageName(type);
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
			String superTypeNames = "";
	        for (RDFNode n : DTAUtilities.getObjects(type, RDFS.subClassOf)) {
				Resource superType = (Resource)n;
	        	String superTypeName = getTypeName(superType);
				String superTypePackName = getPackageName(superType);
				if (!superTypePackName.equals("xsd") && !superTypePackName.equals(typePackage))
					superTypeName = superTypePackName+"."+superTypeName;
				superTypeNames += superTypeNames.length()==0 ? " extends "+superTypeName : ", "+superTypeName;
			}
	        
	        String subTypeNames = "";
	        for (OntClass subType : DTAUtilities.getAllSubClasses(type)) {
	        	String subTypeName = getTypeName(subType);
				String subTypePackName = getPackageName(subType);
				if (!subTypePackName.equals("xsd") && !subTypePackName.equals(typePackage))
					subTypeName = subTypePackName+"."+subTypeName;
	        	if (subTypeNames.length()>0)
	        		subTypeNames += ",\n";
	        	subTypeNames += "\t@JsonSubTypes.Type(value="+subTypeName+"Impl.class, name=\""+subType.getURI()+"\")";
	        }
	        
	        String genAnnot = "@Generated(\"true\")";
	        String uriAnnot = "@JsonTypeName(\""+type.getURI()+"\")\n";
	        String subtypeAnnot = subTypeNames.length()>0 ? "@JsonSubTypes({\n"+subTypeNames+"\n})\n" : "";

			try {
				aType = aJavaFile.createType(genAnnot+uriAnnot+subtypeAnnot+"public interface "+typeName+superTypeNames+" {\n}", null, true, null);
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

		error |= generateGetters(aType, type, false);
		error |= generateSetters(aType, type, false);

		monitor.done();
		return error;
	}			

	private static boolean generateFields(IType aType, Resource type) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties = DTAUtilities.getAllProperties(type);
		
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
            	String fieldTypeName = getTypeName(fieldType);
    			String fieldTypePackName = getPackageName(fieldType);
    			if (!fieldTypePackName.equals("xsd") && !fieldTypePackName.equals(typePackage))
    				fieldTypeName = fieldTypePackName+"."+fieldTypeName;

    			String genAnnot = "@Generated(\"true\")";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
			        aField = aType.createField("\n\n\n"+genAnnot+propAnnot+"private "+fieldTypeName+" "+fieldName+";", null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private static boolean generateGetters(IType aType, Resource type, boolean impl) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties = impl ? DTAUtilities.getAllProperties(type) : DTAUtilities.getDeclaredProperties(type);
		
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
            	String methodTypeName = getTypeName(methodType);
    			String methodTypePackName = getPackageName(methodType);
    			if (!methodTypePackName.equals("xsd") && !methodTypePackName.equals(typePackage))
    				methodTypeName = methodTypePackName+"."+methodTypeName;

            	String fieldName = getFieldName(property, false);
            	if (nameCount.get(getterName) > 1)
    				fieldName = getFieldName(property, true);

            	String body = impl ? "{\n\treturn "+fieldName+";\n}" : ";";
            	
            	String genAnnot = "@Generated(\"true\")";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
		        	aMethod = aType.createMethod("\n\n\n"+genAnnot+propAnnot+"public "+methodTypeName+" "+getterName+"()"+body, null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private static boolean generateSetters(IType aType, Resource type, boolean impl) {
		boolean error=false;
		String typePackage = getPackageName(type);

		Set<OntProperty> properties = impl ? DTAUtilities.getAllProperties(type) : DTAUtilities.getDeclaredProperties(type);
		
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
        	String methodTypeName = getTypeName(methodType);
			String methodTypePackName = getPackageName(methodType);
			if (!methodTypePackName.equals("xsd") && !methodTypePackName.equals(typePackage))
				methodTypeName = methodTypePackName+"."+methodTypeName;

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

            	String body = impl ? "{\n\tthis."+fieldName+" = "+fieldName+";\n}" : ";";
            	
            	String genAnnot = "@Generated(\"true\")";
		        String propAnnot = "@JsonProperty(\""+property.getURI()+"\")\n";

		        try {
		        	aMethod = aType.createMethod("\n\n\n"+genAnnot+propAnnot+"public void "+setterName+"("+methodTypeName+" "+fieldName+")"+body, null, true, null);
				} catch (Exception e) {
					Activator.getDefault().log("Error creating Java type", e);
					error=true;
				}
            }
		}
		return error;
	}

	private static boolean generateOperationClass(ICompilationUnit aJavaFile, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
        IType aType = aJavaFile.getType(typeName);

        if (!aType.exists()) {
	        String genAnnot = "@Generated(\"true\")";
	        String uriAnnot = "@JsonTypeName(\""+operation.getURI()+"\")\n";

			try {
				aType = aJavaFile.createType(genAnnot+uriAnnot+"public class "+typeName+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
			Resource kind = DTAUtilities.getRDFType(operation);
			if (DTA.Post.equals(kind) || DTA.Request.equals(kind))
				aJavaFile.createImport("org.mule.api.MuleEventContext", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
		error |= generateMethods(aType, operation);
		
		monitor.done();
		return error;
	}			

	private static boolean generateMethods(IType aType, Resource operation) {
		boolean error=false;
		String typePackage = getPackageName(operation);
		
		String methodName = getMethodName(operation);

		Resource kind = DTAUtilities.getRDFType(operation);
		boolean isSource = DTA.Receive.equals(kind) || DTA.Provide.equals(kind);
		Resource inputType = operation.getPropertyResourceValue(isSource ? DTA.input : DTA.output);
		Resource outputType = operation.getPropertyResourceValue(DTA.output);
		
    	String inputTypeName = "";
    	if (!isSource)
    		inputTypeName = "MuleEventContext";
    	else {
    		inputTypeName = getTypeName(inputType);
    		String inputTypePackName = getPackageName(inputType);
    		if (!inputTypePackName.equals(typePackage))
    			inputTypeName = inputTypePackName+"."+inputTypeName;
    	}
    	
    	String outputTypeName = "void";
    	if (outputType != null) {
    		outputTypeName = getTypeName(outputType);
    		if (DTA.Provide.equals(kind))
    			outputTypeName += "[]";
    		String outputTypePackName = getPackageName(outputType);
    		if (!outputTypePackName.equals(typePackage))
    			outputTypeName = outputTypePackName+"."+outputTypeName;
    	}
    	
    	String inputName = toCamelCase(inputTypeName);
   	
    	String body = !outputTypeName.equals("void") ? " {\n\treturn null;\n}" : " {\n}";

    	IMethod aMethod = aType.getMethod(methodName, new String[]{Signature.createTypeSignature(inputTypeName, false)});
        
    	if (aMethod.exists() && isGenerated(aMethod)) {
			try {
				aMethod.delete(true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error deleting Java method", e);
				error=true;
			}
		}

        if (!aMethod.exists() || isGenerated(aMethod)) {
        	String genAnnot = "@Generated(\"true\")\n";

	        try {
	        	aMethod = aType.createMethod("\n\n\n"+genAnnot+"public "+outputTypeName+" "+methodName+"("+inputTypeName+" "+inputName+")"+body, null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }
        return error;
	}

	private static void collectRelevantTypes(Resource type, Set<Resource> relevantTypes) {
        if (relevantTypes.contains(type))
        	return;
        relevantTypes.add(type);
		for (OntProperty p : DTAUtilities.getDeclaredProperties(type)) {
			Resource ptype = p.getPropertyResourceValue(RDFS.range);
			if (ptype != null && !ptype.getNameSpace().equals(XSD.getURI()))
				collectRelevantTypes(ptype, relevantTypes);
        }
		for (RDFNode supertype : DTAUtilities.getObjects(type, RDFS.subClassOf)) {
			collectRelevantTypes((Resource)supertype, relevantTypes);
        }
		for (Resource subtype : DTAUtilities.getSubjects(RDFS.subClassOf, type)) {
			collectRelevantTypes(subtype, relevantTypes);
        }
	}
	
	private static String print(Set<String> set) {
		StringBuffer buf = new StringBuffer();
		for (String s : set)
			buf.append("\n"+s);
		return buf.toString();
	}
	
	private static String getPackageName(Resource res) {
		String name = res.getModel().getNsURIPrefix(res.getNameSpace());
		return name.length()>0 ? name.toLowerCase() : BASE_PACKAGE;
	}

	private static String getTypeName(Resource res) {
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
		} 
		return name;
	}

	private static String getFieldName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return toCamelCase(name)+postfix;
	}

	private static String getGetterName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		Resource type = res.getPropertyResourceValue(RDFS.range);
		String prefix = type.equals(XSD.xboolean) ? "is" : "get";
		prefix = name.startsWith(prefix) ? "" : prefix;
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return prefix+toTitleCase(name)+postfix;
	}

	private static String getSetterName(Resource res, boolean qualified) {
		String name = res.getLocalName();
		String prefix = "set";
		String postfix = qualified ? "_"+getPackageName(res) : "";
		return prefix+toTitleCase(name)+postfix;
	}

	private static String getMethodName(Resource res) {
		return "OnCall";
	}
	
	private static String toCamelCase(String s) {
		return s.substring(0, 1).toLowerCase()+s.substring(1);
	}

	private static String toTitleCase(String s) {
		return s.substring(0, 1).toUpperCase()+s.substring(1);
	}

	private static boolean isGenerated(IAnnotatable a) {
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
	
}

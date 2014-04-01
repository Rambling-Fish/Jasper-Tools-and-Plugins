package com.coralcea.jasper.tools.dta.editors;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
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
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.Document;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTACodeGenerator {

	private static final String BASE_PACKAGE = "base";
	
	public static void run(DTAEditor editor) {
		final IFile file = editor.getFile();
		final OntModel model = editor.getModel();
		final Shell shell = editor.getSite().getShell();
		
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
		
		final IContainer container;
		try {
			QualifiedName qName = new QualifiedName(DTA.URI, "codegenContainer");
			String value = (String) file.getSessionProperty(qName);
			IResource res = (value != null) ? ResourcesPlugin.getWorkspace().getRoot().findMember(value) : null;
			IContainer initial = (res instanceof IContainer) ? (IContainer) res : file.getParent().getParent().getFolder(Path.fromOSString("java"));
			
			if (initial == null || !initial.exists())
				initial = file.getParent();
		
			ContainerSelectionDialog dialog = new ContainerSelectionDialog(shell, initial, false, "Select the folder to generate code in:");
			if (dialog.open() != ContainerSelectionDialog.OK) {
				return;
			}
			container = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember((Path)dialog.getResult()[0]);
			
			file.setSessionProperty(qName, container.getFullPath().toString());
		} catch (CoreException e) {
			Activator.getDefault().log("Error finding a codegen container", e);
			return;
		}
				
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				try {
					if (generate(container, model, relevantOps, relevantTypes, monitor)) {
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "An error occurred during code generation. Check error log for more details");
						StatusManager.getManager().handle(status, StatusManager.BLOCK);
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
		Map<String, String> nsMap = model.getNsPrefixMap();
		Set<Resource> relevantProperties = new HashSet<Resource>();
		
		Ontology ontology = model.listOntologies().next();
		
		for (ResIterator i = DTAUtilities.listDefinedResources(ontology, DTA.DTA); i.hasNext();) {
			Resource dta = i.next();
			
			Set<RDFNode> operations = DTAUtilities.listObjects(dta, DTA.operation);
			operations.addAll(DTAUtilities.listObjects(dta, DTA.request));
			
			for (RDFNode j : operations) {
				Resource op = (Resource) j;
				Resource kind = op.getPropertyResourceValue(RDF.type);
				boolean isGet = DTA.Get.equals(op.getPropertyResourceValue(DTA.kind));
				
				relevantOps.add(op);
				if (nsMap.get(op.getNameSpace()) != null)
					missingNamespaces.add(op.getNameSpace());
				
				Resource output = op.getPropertyResourceValue(DTA.output);
				if (output == null && isGet)
					syntaxErrors.add(kind+" <"+op.getURI()+"> is of kind 'Get' but does not define an output");
				if (output != null) {
					relevantProperties.add(output);
					Resource type = output.getPropertyResourceValue(RDFS.range);
					if (type != null && !type.getNameSpace().equals(XSD.getURI()))
						collectRelevantTypes(type.as(OntClass.class), relevantTypes, relevantProperties);
				}

				Set<RDFNode> inputs = DTAUtilities.listObjects(op, DTA.input);
				if (inputs.isEmpty())
					syntaxErrors.add(kind+" <"+op.getURI()+"> does not define any inputs");
				for(RDFNode n : inputs) {
					Resource input = (Resource)n;
					relevantProperties.add(input);
					Resource type = input.getPropertyResourceValue(RDFS.range);
					if (type != null && !type.getNameSpace().equals(XSD.getURI()))
						collectRelevantTypes(type.as(OntClass.class), relevantTypes, relevantProperties);
				}
			}
		}
		
		for (ResIterator i = DTAUtilities.listDefinedClasses(ontology); i.hasNext();) {
			collectRelevantTypes(i.next().as(OntClass.class), relevantTypes, relevantProperties);
		}
		
		for (Resource type : relevantTypes) {
			if (nsMap.get(type.getNameSpace()) != null)
				missingNamespaces.add(type.getNameSpace());
		}

		for (Resource p : relevantProperties) {
			if (nsMap.get(p.getNameSpace()) != null)
				missingNamespaces.add(p.getNameSpace());
			if (p.getPropertyResourceValue(RDFS.range) == null)
				syntaxErrors.add("Property <"+p.getURI()+"> does not have a type");
        }
}
	
	private static void collectRelevantTypes(Resource type, Set<Resource> relevantTypes, Set<Resource> relevantProperties) {
        if (relevantTypes.contains(type))
        	return;
        relevantTypes.add(type);
		for (OntProperty p : DTAUtilities.getDeclaredProperties(type)) {
			relevantProperties.add(p);
			Resource ptype = p.getPropertyResourceValue(RDFS.range);
			if (ptype != null && !ptype.getNameSpace().equals(XSD.getURI()))
				collectRelevantTypes(ptype, relevantTypes, relevantProperties);
        }
		for (RDFNode supertype : DTAUtilities.listObjects(type, RDFS.subClassOf)) {
			collectRelevantTypes((Resource)supertype, relevantTypes, relevantProperties);
        }
		for (Resource subtype : DTAUtilities.listSubjects(RDFS.subClassOf, type)) {
			collectRelevantTypes(subtype, relevantTypes, relevantProperties);
        }
	}
	
	private static boolean generate(IContainer container, OntModel model, Set<Resource> relevantOps, Set<Resource> relevantTypes, IProgressMonitor monitor) {
		boolean error=false;
		
		IPackageFragmentRoot root = (IPackageFragmentRoot) JavaCore.create(container);

		monitor.beginTask("Generating Code", 10);
		error |= generatePackages(root, model, relevantOps, relevantTypes, new SubProgressMonitor(monitor, 1));
		error |= generateTypes(root, model, relevantOps, relevantTypes, new SubProgressMonitor(monitor, 9));
		monitor.done();

		try {
			container.refreshLocal(IResource.DEPTH_INFINITE, null);
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
		for(Resource type : relevantTypes) {
			String packageName = getPackageName(type);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			error |= generateTypeInterface(aPackage, type, new SubProgressMonitor(monitor, 1));
			error |= generateTypeClass(aPackage, type, new SubProgressMonitor(monitor, 1));
		}
		for(Resource operation : relevantOps) {
			String packageName = getPackageName(operation);
			IPackageFragment aPackage = root.getPackageFragment(packageName);
			error |= generateOperationParams(aPackage, operation, new SubProgressMonitor(monitor, 1));
			error |= generateOperationClass(aPackage, operation, new SubProgressMonitor(monitor, 1));
			//if (DTAUtilities.isRequest(operation))
				//error |= generateRequestPayload(aPackage, operation, new SubProgressMonitor(monitor, 1));
		}
		monitor.done();
		return error;
	}
	
	private static boolean generateTypeClass(IPackageFragment aPackage, Resource type, IProgressMonitor monitor) {
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

		Set<OntProperty> properties = DTAUtilities.getAllProperties(type);

		error |= generateFields(aType, type, properties, DTA.restriction);
		error |= generateGetters(aType, type, properties, DTA.restriction, false, true);
		error |= generateSetters(aType, type, properties, DTA.restriction, false, true);
		error |= generateHashCode(aType);
		error |= generateEquals(aType);
		error |= generateToString(aType);
		
		error |= formatCompilationUnit(aJavaFile);
		
		monitor.done();
		return error;
	}			

	private static boolean generateTypeInterface(IPackageFragment aPackage, Resource type, IProgressMonitor monitor) {
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
	        for (OntClass subType : DTAUtilities.getSelfAndAllSubClasses(type)) {
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

		Set<OntProperty> properties = DTAUtilities.getDeclaredProperties(type);

		error |= generateGetters(aType, type, properties, DTA.restriction, true, false);
		error |= generateSetters(aType, type, properties, DTA.restriction, true, false);

		monitor.done();
		return error;
	}			

	private static boolean generateOperationParams(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
 		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
		String className = typeName+"Params";
		String typePackage = getPackageName(operation);
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
	        String genAnnot = "@Generated(\"true\")\n";

    		String comment = createComment(new String[]{"The parameters of {@link"+typePackage+"."+typeName+"}"});

			try {
				aType = aJavaFile.createType(comment+genAnnot+"public class "+className+" {\n}", null, true, null);
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

		Set<OntProperty> properties = DTAUtilities.listInputs(operation);

		error |= generateFields(aType, operation, properties, DTA.inputRestriction);
		error |= generateGetters(aType, operation, properties, DTA.inputRestriction, true, true);
		error |= generateSetters(aType, operation, properties, DTA.inputRestriction, true, true);
		error |= generateHashCode(aType);
		error |= generateEquals(aType);
		error |= generateToString(aType);
		
		error |= formatCompilationUnit(aJavaFile);
		
		monitor.done();
		return error;
	}			

	private static boolean generateFields(IType aType, Resource type, Set<OntProperty> properties, Property restriction) {
		boolean error=false;
		String typePackage = getPackageName(type);

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
    			if (DTAUtilities.isMultiValued(type, property, restriction))
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

	private static boolean generateGetters(IType aType, Resource type, Set<OntProperty> properties, Property restriction, boolean genComment, boolean genBody) {
		boolean error=false;
		String typePackage = getPackageName(type);

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

    			if (DTAUtilities.isMultiValued(type, property, restriction))
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

	private static boolean generateSetters(IType aType, Resource type, Set<OntProperty> properties, Property restriction, boolean genComment, boolean genBody) {
		boolean error=false;
		String typePackage = getPackageName(type);

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

			if (DTAUtilities.isMultiValued(type, property, restriction))
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

	private static boolean generateHashCode(IType aType) {
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

	private static boolean generateEquals(IType aType) {
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

	private static boolean generateToString(IType aType) {
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

	private static boolean generateOperationClass(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
		ICompilationUnit aJavaFile = aPackage.getCompilationUnit(typeName+".java");
        IType aType = aJavaFile.getType(typeName);
		boolean isSource = DTAUtilities.isOperation(operation);

        if (!aType.exists()) {
    		String comment = DTAUtilities.getStringValue(operation, RDFS.comment);
			comment = comment.length()>0 ? createComment(new String[]{comment}) : comment;

			String genAnnot = "@Generated(\"true\")\n";
	        String uriAnnot = "@JsonTypeName(\""+operation.getURI()+"\")\n";

	        String interfaces = isSource ? "" : " implements Callable";
	        
			try {
				aType = aJavaFile.createType(comment+genAnnot+uriAnnot+"public class "+typeName+interfaces+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        } else
        	error |= updateOperationClass(aJavaFile, operation);

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.*", null, null);
			if (!isSource) {
				aJavaFile.createImport("org.mule.api.MuleEventContext", null, null);
				aJavaFile.createImport("org.mule.api.lifecycle.Callable", null, null);
			}
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
		error |= generateMethods(aType, operation);
		
		monitor.done();
		return error;
	}			

	@SuppressWarnings("unchecked")
	private static boolean updateOperationClass(ICompilationUnit aJavaFile, Resource operation) {
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
			if (DTAUtilities.isRequest(operation)) {
				SimpleType type = ast.newSimpleType(ast.newName("Callable"));
				typeDecl.superInterfaceTypes().add(type);
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

	private static boolean generateMethods(IType aType, Resource operation) {
		boolean error=false;

		boolean isSource = DTAUtilities.isOperation(operation);
		Resource outputProperty = operation.getPropertyResourceValue(DTA.output);

		String typeName = getTypeName(operation);
		String typePackage = getPackageName(operation);
		String methodName = getMethodName(operation);
		
		String inputTypeName;
    	if (isSource)
    		inputTypeName = typeName+"Params";
    	else
    		inputTypeName = "MuleEventContext";
 
    	String outputTypeName;
    	if (isSource)
    		outputTypeName = "Object";
    	else
    		outputTypeName = typeName+"Params";

    	String realOutputTypeName;
    	if (outputProperty != null) {
    		Resource outputType = outputProperty.getPropertyResourceValue(RDFS.range);
    		realOutputTypeName = getTypeName(outputType);
    		String outputTypePackName = getPackageName(outputType);
			if (!outputTypePackName.equals(typePackage))
				realOutputTypeName = outputTypePackName+"."+realOutputTypeName;
    		boolean outputIsArray = DTAUtilities.isMultiValued(operation, outputProperty.as(Property.class), DTA.outputRestriction);
    		if (outputIsArray)
    			realOutputTypeName += "[]";
    	} else
    		realOutputTypeName = "void";

    	String inputName = toCamelCase(inputTypeName);
   	
    	String body = " {\n\treturn null;\n}";

		try {
	    	for (IMethod aMethod : aType.getMethods()) {
	    		if (isGenerated(aMethod))
					aMethod.delete(true, null);
	    	}
		} catch (Exception e) {
			Activator.getDefault().log("Error deleting Java methods", e);
			error=true;
		}
   	
    	IMethod aMethod = aType.getMethod(methodName, new String[]{Signature.createTypeSignature(inputTypeName, false)});
        
       if (!aMethod.exists()) {
        	String genAnnot = "@Generated(\"true\")\n";

        	String returnComment = outputTypeName;
        	if (isSource) {
        		returnComment = realOutputTypeName.equals("void") ? "null" : realOutputTypeName;
        		returnComment += " (or some Object if this processor is not terminal)";
        	}
			String comment = createComment(new String[]{"@param "+inputName, "@return "+returnComment});

	        try {
	        	aMethod = aType.createMethod(comment+genAnnot+"public "+outputTypeName+" "+methodName+"("+inputTypeName+" "+inputName+") throws Exception"+body, null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }
        return error;
	}
	
	private static boolean generateRequestPayload(IPackageFragment aPackage, Resource operation, IProgressMonitor monitor) {
		boolean error=false;
		monitor.beginTask("Generating class", 1);

		String typeName = getTypeName(operation);
		String className = typeName+"Payload";
		String typePackage = getPackageName(operation);
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
	        String genAnnot = "@Generated(\"true\")\n";

    		String comment = createComment(new String[]{"The payload of request {@link"+typePackage+"."+typeName+"}"});

			try {
				aType = aJavaFile.createType(comment+genAnnot+"public class "+className+" {\n}", null, true, null);
			} catch (Exception e) {
				Activator.getDefault().log("Error creating Java type", e);
				error=true;
			}
        }

		try {
			aJavaFile.createImport("javax.annotation.Generated", null, null);
			aJavaFile.createImport("org.codehaus.jackson.annotate.JsonProperty", null, null);
			aJavaFile.createImport("java.util.HashMap", null, null);
		} catch (JavaModelException e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
    	boolean isGet = DTA.Get.equals(operation.getPropertyResourceValue(DTA.kind));
		String parametersType = typeName+"Params";
        String genAnnot = "@Generated(\"true\")\n";
        String method =  isGet ? "GET" : "POST";
        String ruri = operation.getURI();
        if (isGet) {
    		Resource outputProperty = operation.getPropertyResourceValue(DTA.output);
    		Resource outputType = outputProperty.getPropertyResourceValue(RDFS.range);
    		ruri = outputType.getURI();
        }
        String headers = "\n{\n";
        headers += "\theaders.put(\"response-type\", \"application/json\");\n";
        if (isGet) {
    		Resource outputProperty = operation.getPropertyResourceValue(DTA.output);
    		boolean outputIsArray = DTAUtilities.isMultiValued(operation, outputProperty.as(Property.class), DTA.outputRestriction);
    		String scheme = outputIsArray ? "aggregate" : "merge";
        	headers += "\theaders.put(\"processing-scheme\", \""+scheme+"\");\n";
        }
        headers += "}";
        String methodComment = createComment(new String[]{"@return parameters"});
        String methodBody = " {\n\treturn parameters;\n}";
    		
        try {
	        aType.createField(genAnnot+getJasonProperty("version")+"private String version = \"1.0\";", null, true, null);
	        aType.createField(genAnnot+getJasonProperty("method")+"private String method = \""+method+"\";", null, true, null);
	        aType.createField(genAnnot+getJasonProperty("ruri")+"private String ruri = \""+ruri+"\";", null, true, null);
	        aType.createField(genAnnot+getJasonProperty("parameters")+"private "+parametersType+" parameters = new "+parametersType+"();", null, true, null);
	        aType.createField(genAnnot+getJasonProperty("headers")+"private HashMap<String, String> headers = new HashMap<String, String>();"+headers, null, true, null);
        	aType.createMethod(methodComment+genAnnot+"public "+parametersType+" getParameters()"+methodBody, null, true, null);
		} catch (Exception e) {
			Activator.getDefault().log("Error creating Java type", e);
			error=true;
		}
		
		error |= formatCompilationUnit(aJavaFile);
		
		monitor.done();
		return error;
	}			

	private static String print(Set<String> set) {
		StringBuffer buf = new StringBuffer();
		for (String s : set)
			buf.append("\n"+s);
		return buf.toString();
	}
	
	private static String createComment(String[] lines) {
		String comment = "/**\n";
		for (String line : lines)
			comment += " * "+line+"\n";
		comment += " */\n";
		return comment;
	}
	
	private static String getPackageName(Resource res) {
		Resource ontology = ((OntModel)res.getModel()).listOntologies().next();
		String name = res.getModel().getNsURIPrefix(res.getNameSpace());
		if (!name.equals("xsd")) {
			name = name.length()==0 ? BASE_PACKAGE : name;
			String basePackage = DTAUtilities.getStringValue(ontology, DTA.basepackage);
			name = basePackage.length()>0 ? basePackage+"."+name : name; 
		}
		return name.toLowerCase();
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
		return "onCall";
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static boolean formatCompilationUnit(ICompilationUnit aJavaFile) {
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
	
	private static String getJasonProperty(String property) {
		return "@JsonProperty(\""+property+"\")";
	}
	
}

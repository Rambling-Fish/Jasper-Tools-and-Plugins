package com.coralcea.jasper.tools.dta.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.editors.DTAEditor;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Model;

public class DTALibraryWizard extends Wizard implements INewWizard {
	private DTALibraryWizardPage libraryPage;
	private IStructuredSelection selection;

	/**
	 * Constructor for ConvertToDTA.
	 */
	public DTALibraryWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New DTA Model");
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		addPage(libraryPage = new DTALibraryWizardPage(selection));
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final IProject project = libraryPage.getProject();
		final String modelFile = libraryPage.getModelFile();
		final String modelName = libraryPage.getModelName();
		final String modelNamespace = libraryPage.getModelNamespace();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(project, modelFile, modelName, modelNamespace, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	private void doFinish(IProject project, String modelFile, String modelName, String modelNamespace, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating library", 1);
		final IFile file = (IFile) project.getFile(Path.fromOSString("src/main/app/"+modelFile));
		createLibrary(file, modelName, modelNamespace, monitor);
		updatePolicy(project, modelName, modelName);
		
		new UIJob("Open DATA Library") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				try {
				    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				    page.openEditor(new FileEditorInput(file), DTAEditor.ID);
					return Status.OK_STATUS;						
				} catch (PartInitException e) {
					Activator.getDefault().log("Failed to open DTA editor", e);
					return Status.CANCEL_STATUS;						
				}
			}
		}.schedule();
		
		monitor.done();
	}
	
	private void createLibrary(IFile file, String modelName, String modelNamespace, IProgressMonitor monitor) {
		OntModel model = DTACore.createNewModel();
		if (modelNamespace.length()>0)
			model.setNsPrefix("", modelNamespace);
		Ontology ont = model.createOntology(modelName);
		ont.setPropertyValue(DTA.isLibrary, model.createTypedLiteral(true));
		
		try {
			DTACore.saveModel(model, file, false, monitor);
		} catch (CoreException e) {
		    Activator.getDefault().log("Failed to create new DTA library", e);
		}

		model.close();
	}
	
	private void updatePolicy(IProject project, String modelName, String modelURI) {
		IFile policy = project.getFile(Path.fromOSString("src/main/app/"+DTA.IMPORT_POLICY));
		Model imports = DTACore.loadImportPolicyModel(policy);
		DTACore.addImportPolicyEntry(imports, modelURI, "file:"+modelName+"."+DTA.EXTENSION);
		DTACore.saveImportPolicyModel(imports, policy);
	}
	
	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}

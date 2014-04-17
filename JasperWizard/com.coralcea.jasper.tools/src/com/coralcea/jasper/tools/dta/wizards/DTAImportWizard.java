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
import org.eclipse.ui.IImportWizard;
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

public class DTAImportWizard extends Wizard implements IImportWizard {
	protected DTAImportWizardPage importPage;
	protected IStructuredSelection selection;

	/**
	 * Constructor for ConvertToDTA.
	 */
	public DTAImportWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("Import DTA Library");
	}
	
	@Override
	public void addPages() {
		addPage(importPage = new DTAImportWizardPage(selection));
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final IProject project = importPage.getProject();
		final String modelName = importPage.getModelFile();
		final OntModel loadedModel = importPage.getLoadedModel();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(project, modelName, loadedModel, monitor);
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
	
	private void doFinish(IProject project, String modelName, OntModel loadedModel, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating library", 1);
		
		Ontology ont = loadedModel.listOntologies().next();
		ont.setPropertyValue(DTA.isLibrary, loadedModel.createTypedLiteral(true));

		final IFile file = (IFile) project.getFile(Path.fromOSString("src/main/app/"+modelName));
		try {
			DTACore.saveModel(loadedModel, file, false, monitor);
		} catch (CoreException e) {
		    Activator.getDefault().log("Failed to create new DTA library", e);
		}
		
		String modelURI = loadedModel.listOntologies().next().getURI();
		loadedModel.close();
		
		updatePolicy(project, modelURI, modelName);
		
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
	
	private void updatePolicy(IProject project,  String modelURI, String modelName) {
		IFile policy = project.getFile(Path.fromOSString("src/main/app/"+DTA.IMPORT_POLICY));
		Model imports = DTACore.loadImportPolicyModel(policy);
		DTACore.addImportPolicyEntry(imports, modelURI, "file:"+modelName);
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

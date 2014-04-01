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

public class DTAModelWizard extends Wizard implements INewWizard {
	private DTAModelWizardPage modelPage;
	private IStructuredSelection selection;

	/**
	 * Constructor for ConvertToDTA.
	 */
	public DTAModelWizard() {
		setNeedsProgressMonitor(true);
		setWindowTitle("New DTA Model");
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		addPage(modelPage = new DTAModelWizardPage(selection));
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final IProject project = modelPage.getProject();
		final String modelFile = modelPage.getModelFile();
		final String modelName = modelPage.getModelName();
		final String modelNamespace = modelPage.getModelNamespace();
		final String dtaName = modelPage.getDTAName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(project, modelFile, modelName, modelNamespace, dtaName, monitor);
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
	
	private void doFinish(IProject project, String modelFile, String modelName, String modelNamespace, String dtaName, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating model", 1);
		final IFile file = (IFile) project.getFile(Path.fromOSString("src/main/app/"+modelFile));
		createModel(project, file, modelName, modelNamespace, dtaName, monitor);
		
		new UIJob("Open DATA Model") {
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
		
		monitor.worked(1);
	}
	
	private void createModel(IProject project, IFile file, String modelName, String modelNamespace, String dtaName, IProgressMonitor monitor) {
		OntModel model = DTACore.createNewModel();
		model.setNsPrefix("", modelNamespace);
		model.createOntology(modelName);
		model.createIndividual(dtaName, DTA.DTA);
		
		try {
			DTACore.saveModel(model, file, false, monitor);
		} catch (Exception e) {
		    Activator.getDefault().log("Failed to create new DTA model", e);
		}

		model.close();
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

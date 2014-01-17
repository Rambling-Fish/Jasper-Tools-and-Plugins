package com.coralcea.jasper.tools.dta.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
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
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.editors.DTAEditor;
import com.hp.hpl.jena.ontology.OntModel;

public class DTAModelWizard extends Wizard implements INewWizard {
	private DTAModelWizardPage page;
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
		page = new DTAModelWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String folderName = page.getFolderName();
		final String modelName = page.getModelName();
		final String modelURI = page.getModelURI();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(folderName, modelName, modelURI, monitor);
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
	
	private void doFinish(String containerName, String modelName, String modelURI, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask("Creating " + modelName, 1);
		IContainer container = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(containerName));
		final IFile file = (IFile) container.getFile(Path.fromOSString(modelName+".dta"));
		
		OntModel model = DTACore.createModel(file);
		model.setNsPrefix("", modelURI+"#");
		model.createOntology(modelURI);
		
		try {
			DTACore.saveModel(model, file, monitor);
		} catch (Exception e) {
		    Activator.getDefault().log("Failed to create new DTA model", e);
		}

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
	
	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}

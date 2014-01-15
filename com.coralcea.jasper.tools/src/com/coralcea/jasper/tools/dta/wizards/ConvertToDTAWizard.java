package com.coralcea.jasper.tools.dta.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;

import com.coralcea.jasper.tools.dta.DTANature;
import com.coralcea.jasper.tools.dta.DTAUtilities;

public class ConvertToDTAWizard extends Wizard implements INewWizard {
	private ConvertToDTAWizardPage page;
	private IStructuredSelection selection;

	/**
	 * Constructor for ConvertToDTA.
	 */
	public ConvertToDTAWizard() {
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new ConvertToDTAWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	public boolean performFinish() {
		final String containerName = page.getContainerName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(containerName, monitor);
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
	
	/**
	 * The worker method. It will find the container, create the
	 * file if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 */

	private void doFinish(
		String containerName,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Converting " + containerName, 1);
		IResource container = ResourcesPlugin.getWorkspace().getRoot()
				.findMember(new Path(containerName));
		
		if (!container.exists() || !(container instanceof IProject)) {
			throwCoreException("Project \"" + containerName + "\" does not exist.");
		}
		IProject project = (IProject) container;
		try {
			if (!DTAUtilities.hasNature(project, DTANature.ID)) {
			    IProjectDescription desc = project.getDescription();
			    String[] prevNatures = desc.getNatureIds();
			    String[] newNatures = new String[prevNatures.length + 1];
			    System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
			    newNatures[prevNatures.length] = DTANature.ID;
			    desc.setNatureIds(newNatures);
			    project.setDescription(desc, new NullProgressMonitor());
			}
		} catch (CoreException e) {
		    e.printStackTrace();
		}
		monitor.worked(1);
	}
	
	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, "test5", IStatus.OK, message, null);
		throw new CoreException(status);
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

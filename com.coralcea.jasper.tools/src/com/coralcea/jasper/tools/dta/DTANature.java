package com.coralcea.jasper.tools.dta;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class DTANature implements IProjectNature {

	public static final String ID = "com.coralcea.jasper.natures.DTA";
	
	private IProject project;

    @Override
	public void configure() throws CoreException {
        // Add nature-specific information
        // for the project, such as adding a builder
        // to a project's build spec.
	}

	@Override
	public void deconfigure() throws CoreException {
		// Remove the nature-specific information here.
	}

	@Override
	public IProject getProject() {
		return project;
	}

	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

}

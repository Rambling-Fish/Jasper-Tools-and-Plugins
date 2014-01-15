package com.coralcea.jasper.tools.dta.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAChangeListener;
import com.coralcea.jasper.tools.dta.DTACore;
import com.hp.hpl.jena.ontology.OntModel;

public class DTATreeContentProvider implements ITreeContentProvider, IResourceChangeListener, IResourceDeltaVisitor, DTAChangeListener {

	private static final Object[] NO_CHILDREN = new Object[0];
	
	private StructuredViewer viewer;

	public DTATreeContentProvider() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		DTACore.addChangeListener(this);
	}
	
	@Override
	public void dispose() {
		DTACore.removeChangeListener(this);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this); 
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (StructuredViewer) viewer;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		DTATreeData[] children = null;
		if (parentElement instanceof IFile) {
			IFile modelFile = (IFile) parentElement;
			if(DTA.EXTENSION.equals(modelFile.getFileExtension())) {				
				DTATreeData root = createRoot(modelFile);
				if (root != null)
					children = new DTATreeData[] {root};
			}
		} else if (parentElement instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) parentElement;
			children = data.getChildren();
		}
		return children != null ? children : NO_CHILDREN;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) element;
			return data.getParent();
		} 
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof DTATreeData) {
			DTATreeData data = (DTATreeData) element;
			return data.getChildren() != null;
		} else if(element instanceof IFile) {
			return DTA.EXTENSION.equals(((IFile) element).getFileExtension());
		}
		return false;
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		IResource resource = delta.getResource();
		if (resource instanceof IFile) {
			final IFile file = (IFile) resource;
			if (DTA.EXTENSION.equals(file.getFileExtension()))
				refresh(file);
			return false;
		}
		return true;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		try {
			delta.accept(this);
		} catch (CoreException e) { 
			Activator.getDefault().log("Error loading DTA file", e);
		} 
	}

	@Override
	public void dtaChanged(IFile file) {
		refresh(file);
	}

	private synchronized DTATreeData createRoot(final IFile modelFile) { 
		try {
			OntModel model = DTACore.getModel(modelFile);
			return DTATreeData.createRoot(model, modelFile);
		} catch (CoreException e) {
			Activator.getDefault().log("Error loading DTA model", e);
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return null;
		}
	}

	private void refresh(final IFile file) {
		new UIJob("Update DATA Model in Viewer") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				viewer.refresh(file);
				return Status.OK_STATUS;						
			}
		}.schedule();
	}

}

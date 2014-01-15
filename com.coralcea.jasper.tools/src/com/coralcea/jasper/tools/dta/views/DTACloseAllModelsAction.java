package com.coralcea.jasper.tools.dta.views;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.editors.DTAEditor;
import com.hp.hpl.jena.ontology.Ontology;

public class DTACloseAllModelsAction extends Action {
	
	private IWorkbenchPage page;
	private ISelectionProvider provider;
	private StructuredViewer viewer;
	
	public DTACloseAllModelsAction(IWorkbenchPage page, ISelectionProvider provider, StructuredViewer viewer) {
		setText("Close All");
		this.page = page;
		this.provider = provider;
		this.viewer = viewer;
	}

	@Override
	public boolean isEnabled() {
		ISelection s = provider.getSelection();
		if (!s.isEmpty()) {
			IStructuredSelection ss = (IStructuredSelection) s;
			if (ss.getFirstElement() instanceof DTATreeData) {
				DTATreeData data = (DTATreeData) ss.getFirstElement();
				if (data.getElement().canAs(Ontology.class))
					return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
		final Collection<IFile> files = DTACore.getLoadedModelFiles();
		
		for (IFile file : files)
			DTACore.unloadModel(file);
		
		new UIJob("Close All DATA Models") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				for (IFile file : files) {
					for (IWorkbenchPage p : page.getWorkbenchWindow().getPages()) {
						IEditorReference[] editors = p.findEditors(new FileEditorInput(file), DTAEditor.ID, IWorkbenchPage.MATCH_ID|IWorkbenchPage.MATCH_INPUT);
						for (IEditorReference editor : editors)
							p.closeEditor(editor.getEditor(false), true);
					}
					
					if (viewer instanceof AbstractTreeViewer)
						((AbstractTreeViewer)viewer).collapseToLevel(file, AbstractTreeViewer.ALL_LEVELS);
				}
				return Status.OK_STATUS;						
			}
		}.schedule();
	}
}

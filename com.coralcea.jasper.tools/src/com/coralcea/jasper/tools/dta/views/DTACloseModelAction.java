package com.coralcea.jasper.tools.dta.views;

import java.util.ArrayList;
import java.util.List;

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

public class DTACloseModelAction extends Action {
	
	private IWorkbenchPage page;
	private ISelectionProvider provider;
	private StructuredViewer viewer;
	private List<DTATreeData> datas;
	
	public DTACloseModelAction(IWorkbenchPage page, ISelectionProvider provider, StructuredViewer viewer) {
		setText("Close");
		this.page = page;
		this.provider = provider;
		this.viewer = viewer;
	}

	@Override
	public boolean isEnabled() {
		datas = new ArrayList<DTATreeData>();
		ISelection s = provider.getSelection();
		if (!s.isEmpty()) {
			IStructuredSelection ss = (IStructuredSelection) s;
			for (Object o : ss.toList()) {
				if (o instanceof DTATreeData) {
					DTATreeData data = (DTATreeData)o;
					if (data.getElement().canAs(Ontology.class))
						datas.add(data);
				}
			}
		}
		return !datas.isEmpty();
	}

	@Override
	public void run() {
		for (DTATreeData data : datas) {
			IFile file = (IFile)data.getParent();
			DTACore.unloadModel(file);
		}
		
		new UIJob("Close DATA Model") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				for (DTATreeData data : datas) {
					IFile file = (IFile)data.getParent();
					
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

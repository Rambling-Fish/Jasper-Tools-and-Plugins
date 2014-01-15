package com.coralcea.jasper.tools.dta.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.progress.UIJob;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.editors.DTAEditor;

public class DTAOpenEditorAction extends Action {
	
	private IWorkbenchPage page;
	private ISelectionProvider provider;
	private DTATreeData data;
	
	public DTAOpenEditorAction(IWorkbenchPage page, ISelectionProvider provider) {
		setText("Open");
		this.page = page;
		this.provider = provider;
	}

	@Override
	public boolean isEnabled() {
		ISelection s = provider.getSelection();
		if (!s.isEmpty()) {
			IStructuredSelection ss = (IStructuredSelection) s;
			if (ss.size() == 1 && ss.getFirstElement() instanceof DTATreeData) {
				data = (DTATreeData) ss.getFirstElement();
				return true;
			}
		}
		return false;
	}

	@Override
	public void run() {
		new UIJob("Close DATA Model") {
			public IStatus runInUIThread(IProgressMonitor monitor) {
				openEditor();
				return Status.OK_STATUS;						
			}
		}.schedule();
	}	
	
	private void openEditor() {
		DTATreeData d = (DTATreeData) data;
		while (!(d.getParent() instanceof IFile))
			d = (DTATreeData) d.getParent();
		IFileEditorInput input = new FileEditorInput((IFile)d.getParent());
		IEditorReference[] editors = page.findEditors(input, DTAEditor.ID, IWorkbenchPage.MATCH_ID|IWorkbenchPage.MATCH_INPUT);
		IEditorPart editor = editors.length>0 ? editors[0].getEditor(false) : null;
		if (editor == null) {
			try {
				editor = page.openEditor(input, DTAEditor.ID, true, IWorkbenchPage.MATCH_ID|IWorkbenchPage.MATCH_INPUT);
			} catch (PartInitException e) {
				Activator.getDefault().log("Failed to open DTA editor", e);
			}
		} else
			page.bringToTop(editor);

		DTAEditor dtaEditor = (DTAEditor)editor;
		if (data instanceof DTATreeData)
			dtaEditor.setSelectedElement(((DTATreeData)data).getElement());
		else
			dtaEditor.setSelectedElement(null);
	}

	
	
}

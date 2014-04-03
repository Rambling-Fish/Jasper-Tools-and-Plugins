package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.ILinkHelper;
import org.eclipse.ui.part.FileEditorInput;

import com.coralcea.jasper.tools.dta.views.DTATreeData;

public class DTALinkHelper implements ILinkHelper {

	@Override
	public IStructuredSelection findSelection(IEditorInput input) {
		IFile file = ResourceUtil.getFile(input);
		if (file != null) {
			return new StructuredSelection(file);
		}
		return StructuredSelection.EMPTY;
	}

	@Override
	public void activateEditor(IWorkbenchPage page, IStructuredSelection selection) {
		if (selection == null || selection.isEmpty())
			return;
		
		Object element= selection.getFirstElement();
		FileEditorInput input;
		if (element instanceof IFile)
			input = new FileEditorInput((IFile)element);
		else {
			DTATreeData e = (DTATreeData) element;
			while (!(e.getParent() instanceof IFile))
				e = (DTATreeData) e.getParent();
			input = new FileEditorInput((IFile)e.getParent());
		}
			
		IEditorPart editor = page.findEditor(input);
		if (editor != null) {
			page.bringToTop(editor);
			if (editor instanceof DTAEditor) {
				DTAEditor dtaEditor = (DTAEditor)editor;
				dtaEditor.setSelectedElement(((DTATreeData)element).getElement());
			}
		}
	}

}

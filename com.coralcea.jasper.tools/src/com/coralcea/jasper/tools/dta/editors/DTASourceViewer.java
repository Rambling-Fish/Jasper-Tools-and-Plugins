package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayOutputStream;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.texteditor.FindReplaceAction;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.coralcea.jasper.tools.dta.DTACore;

public class DTASourceViewer extends DTAViewer {

	private TextViewer source;
	private FindReplaceAction findReplaceAction;
	
	public DTASourceViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}
	
	protected Form createControl(Composite parent) {
		Form f = super.createControl(parent);
		f.setText("Source");
		f.getBody().setLayout(new FillLayout());
		source = new TextViewer(f.getBody(), SWT.V_SCROLL|SWT.H_SCROLL);
		source.getTextWidget().setAlwaysShowScrollBars(false);
		source.setEditable(false);
		return f;
	}

	@Override
	public void refresh() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DTACore.writeModel(getInput(), out);
		IDocument document = new Document();
		document.set( out.toString() );
		source.setDocument( document );
		findReplaceAction.update();
	}
	
	protected IFindReplaceTarget getFindReplaceTarget() {
		return source.getFindReplaceTarget();
	}

	protected void setupActions(IToolBarManager manager) {
		super.setupActions(manager);

		findReplaceAction = new FindReplaceAction(Activator.getResourceBundle(), "Editor.FindReplace.", getEditor().getSite().getShell(), getFindReplaceTarget());
		findReplaceAction.setImageDescriptor(Activator.getImageDescriptor(Images.FIND));
		manager.appendToGroup("Viewer", findReplaceAction);
	}
}

package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.coralcea.jasper.tools.dta.DTACore;

public class DTASourceViewer extends DTAViewer {

	private TextViewer source;
	private IHandlerService handlerService;
	private FindReplaceAction findReplaceAction;
	private List<IHandlerActivation> handlerActivations = new ArrayList<IHandlerActivation>();
	
	public DTASourceViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		handlerService = (IHandlerService) getEditor().getSite().getService(IHandlerService.class);
	}
	
	protected Form createControl(Composite parent) {
		Form f = super.createControl(parent);
		f.setText("Source");
		f.getBody().setLayout(new FillLayout());

		source = new TextViewer(f.getBody(), SWT.V_SCROLL|SWT.H_SCROLL);
		source.getTextWidget().setAlwaysShowScrollBars(false);
		source.setEditable(false);
		source.getTextWidget().addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				deactivateContext();
			}
			public void focusGained(FocusEvent e) {
				activateContext();
			}
		});
		source.getTextWidget().addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				deactivateContext();
			}
		});
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
		findReplaceAction.setHelpContextId(IAbstractTextEditorHelpContextIds.FIND_ACTION);
		findReplaceAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		manager.appendToGroup("Viewer", findReplaceAction);
	}
	
	protected void activateContext() {
		if (handlerActivations.isEmpty()) {
			activateHandler(findReplaceAction);
		}
	}

	protected void deactivateContext() {
		for (IHandlerActivation activation: handlerActivations) {
			handlerService.deactivateHandler(activation);
			activation.getHandler().dispose();
		}
		handlerActivations.clear();	
	}

	protected void activateHandler(IAction action) {
		handlerActivations.add(handlerService.activateHandler(action.getActionDefinitionId(), new ActionHandler(action)));
	}

}

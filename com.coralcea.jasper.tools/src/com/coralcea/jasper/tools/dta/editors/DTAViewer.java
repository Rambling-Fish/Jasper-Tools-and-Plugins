package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public abstract class DTAViewer extends Viewer {

	private DTAEditor editor;
	private Form form;
	private OntModel model;
	private FormToolkit toolkit;
	
	public DTAViewer(Composite parent, DTAEditor editor) {
		this.editor = editor;
		this.toolkit = editor.getFormToolkit();
		form = toolkit.createForm(parent);
		toolkit.decorateFormHeading(form);
		addActions();
	}
	
	protected final void addActions() {
		addActionsToToolBar(form.getToolBarManager());
		form.updateToolBar();
	}

	protected void addActionsToToolBar(IToolBarManager manager) {
		Action action;
		
		manager.add(new Separator("Common"));
		manager.add(new Separator("Viewer"));
		manager.add(new Separator("Basic"));
		
		if (!DTAUtilities.isLibrary(getEditor().getModel().listOntologies().next())) {
			action = new Action("Code Gen") {
				public void run() {
					DTACodeGenerator.run(getEditor());
				}
			};
			action.setToolTipText("Generate code");
			action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.CODEGEN));
			manager.appendToGroup("Common", action);
		}
		
		action = new Action("Download Model") {
			public void run() {
				DTADownloadJasperModel.run(getEditor());
			}
		};
		action.setToolTipText("Download Jasper model");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.SERVER));
		manager.appendToGroup("Common", action);

		action = new Action("Import Policy") {
			public void run() {
				DTAImportPolicyDialog.openToEdit(getEditor().getSite().getShell(), editor);
			}
		};
		action.setToolTipText("Edit import policy");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.POLICY));
		manager.appendToGroup("Common", action);

		action = new Action("Reload model") {
			public void run() {
				getEditor().reload();
			}
		};
		action.setToolTipText("Reload model");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.REFRESH));
		manager.appendToGroup("Basic", action);
				
		action = new SaveAction(getEditor());
		action.setToolTipText("Save model");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.SAVE));
		manager.appendToGroup("Basic", action);

		action = new Action("Help") {
			public void run() {
				PlatformUI.getWorkbench().getHelpSystem().displayHelp("dd");
			}
		};
		action.setToolTipText("Show help");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.HELP));
		manager.appendToGroup("Basic", action);
	}

	protected DTAEditor getEditor() {
		return editor;
	}
	
	@Override
	public Form getControl() {
		return form;
	}
	
	@Override
	public OntModel getInput() {
		return model;
	}

	@Override
	public void setInput(Object input) {
		model = (OntModel) input;
		setSelection(new StructuredSelection(model));
	}

	@Override
	public abstract void refresh();

	@Override
	public ISelection getSelection() {
		Object element = getSelectedElement();
		return element == null 
				? StructuredSelection.EMPTY 
				: new StructuredSelection(element);
	}
	
	@Override
	public void setSelection(ISelection selection, boolean reveal) {
		if (selection.isEmpty())
			return;
		Resource newElement = getTargetElement(((IStructuredSelection)selection).getFirstElement());
		if (newElement != getSelectedElement()) {
			setSelectedElement(newElement);
			if (reveal)
				revealSelectedElement();
		}
	}
	
	protected void revealSelectedElement() {
	}

	public Resource getSelectedElement() {
		return null;
	}

	protected void setSelectedElement(Resource element) {
	}
	
	protected Resource getTargetElement(Object element) {
		return null;
	}
	
	protected Composite createComposite(Composite parent, int columns) {
		Composite composite = toolkit.createComposite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
        composite.setLayout(new GridLayout(columns, false));
		return composite;
	}

	protected ScrolledComposite createScrolledComposite(Composite parent, int columns) {
		ScrolledComposite composite = new ScrolledComposite(parent, SWT.V_SCROLL|SWT.H_SCROLL);
		toolkit.adapt(composite);
		composite.setExpandHorizontal(true);
		composite.setExpandVertical(true);
        composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        composite.setLayout(new GridLayout(columns, false));
		return composite;
	}
	
	protected Section createSection(Composite parent, String title, String description) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR|Section.TWISTIE|Section.EXPANDED|SWT.BORDER);
        section.setText(title);
        section.setToolTipText(description);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace=true;
        section.setLayoutData(data);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        section.setLayout(layout);
        return section;
	}
	
	protected Label createLabel(Composite parent, String text, String tooltip) {
        Label l = toolkit.createLabel(parent, text, SWT.NONE);
        l.setToolTipText(tooltip);
        return l;
	}
	
	protected Text createText(Composite parent, int style, String s) {
        Text text = toolkit.createText(parent, s, SWT.BORDER|style);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace=true;
        text.setLayoutData(data);
        return text;
	}
	
	protected Text createTextArea(Composite parent, int style, String s) {
        Text text = toolkit.createText(parent, s, SWT.BORDER|SWT.MULTI|SWT.FOCUSED|style);
        text.addTraverseListener(new TraverseListener() {
            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    e.doit = true;
                }
            }
        });
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace=true;
        data.heightHint = 3 * text.getLineHeight();
        text.setLayoutData(data);
        return text;
	}

	protected ComboViewer createCombo(Composite parent, int style, ILabelProvider provider, Object[] options, Object selection) {
        ComboViewer viewer = new ComboViewer(parent, style);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        if (provider != null)
        	viewer.setLabelProvider(provider);
        viewer.setInput(options);
        if (selection!=null)
        	viewer.setSelection(new StructuredSelection(selection), true);
        return viewer;
	}

	protected Button createButton(Composite parent, int style, String s) {
        Button button = toolkit.createButton(parent, s, style);
        GridData data = new GridData();
        data.verticalAlignment = GridData.CENTER;
        data.grabExcessHorizontalSpace=false;
        button.setLayoutData(data);
        return button;
	}

	protected ToolBar createToolBar(Section parent, int style) {
		ToolBar toolbar = new ToolBar(parent, SWT.NONE);
		return toolbar;
	}
	
	protected ToolItem createToolItem(ToolBar parent, String text, Image icon) {
		ToolItem item = new ToolItem(parent, SWT.PUSH);
		item.setImage(Activator.getImage(JasperImages.PLUS));
		item.setToolTipText(text);
		return item;
	}

	protected ImageHyperlink createImageHyperlink(Composite parent, String tooltip, Image icon) {
		ImageHyperlink imageHyperLink = toolkit.createImageHyperlink(parent, SWT.CENTER);
		imageHyperLink.setImage(icon);
		imageHyperLink.setToolTipText(tooltip);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.END;
        data.grabExcessHorizontalSpace=true;
        imageHyperLink.setLayoutData(data);
		return imageHyperLink;
	}

	protected Link createLink(Composite parent, String text, String tooltip, final Object element) {
		Link link = new Link(parent, SWT.NONE);
		link.setBackground(toolkit.getColors().getBackground());
		link.setText(text);
		link.setToolTipText(tooltip);
		if (element != null) {
			link.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					setSelection(new StructuredSelection(element), true);
				}
			});
		}
        GridData data = new GridData();
        data.verticalAlignment = GridData.CENTER;
        data.grabExcessHorizontalSpace=false;
        link.setLayoutData(data);
        return link;
	}

}

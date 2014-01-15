package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.coralcea.jasper.tools.dta.codegen.DTACodeGenerator;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public abstract class DTAViewer extends Viewer {

	private DTAEditor editor;
	private ScrolledForm form;
	private OntModel model;
	private FormToolkit toolkit;
	
	public DTAViewer(Composite parent, DTAEditor editor) {
		this.editor = editor;
		this.toolkit = editor.getFormToolkit();
		form = toolkit.createScrolledForm(parent);
		toolkit.decorateFormHeading(form.getForm());
		form.getBody().setLayout(new FillLayout());
		/*form.addControlListener(new ControlAdapter() {
		    public void controlResized(ControlEvent e) {
		    	if (form.getContent() != null)
		    		resizeControl();
		    }
		});*/
		addActions();
	}
	
	protected void addActions() {
		IToolBarManager manager = form.getToolBarManager();
		Action action;
		
		action = new Action("code gen") { //$NON-NLS-1$
			public void run() {
				IFile file = ((IFileEditorInput)getEditor().getFileEditorInput()).getFile();
				DTACodeGenerator.run(getEditor().getSite().getShell(), file);
			}
		};
		action.setToolTipText("Generate Code");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.CODEGEN));
		manager.add(action);

		action = new Action("import policy") { //$NON-NLS-1$
			public void run() {
				DTAImportPolicyDialog.run(getEditor().getSite().getShell(), editor);
			}
		};
		action.setToolTipText("Import Policy");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.POLICY));
		manager.add(action);

		action = new Action("help") { //$NON-NLS-1$
			public void run() {
				PlatformUI.getWorkbench().getHelpSystem().displayHelp("dd");
			}
		};
		action.setToolTipText("Help");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.HELP));
		manager.add(action);
		
		form.updateToolBar();
	}

	protected DTAEditor getEditor() {
		return editor;
	}
	
	@Override
	public ScrolledForm getControl() {
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
				refresh();
		}
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
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace=true;
        composite.setLayoutData(data);
        GridLayout layout = new GridLayout();
        layout.numColumns = columns;
        composite.setLayout(layout);
		return composite;
	}

	protected Composite createGroup(Composite parent, int columns) {
        Composite group = toolkit.createComposite(parent, SWT.BORDER);
        GridData data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace=true;
        group.setLayoutData(data);
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 0;
        layout.numColumns = columns;
        group.setLayout(layout);
		return group;
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
	
	protected Label createLabel(Composite parent, String s) {
        return toolkit.createLabel(parent, s, SWT.NONE);
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
        Combo combo = new Combo(parent, style);
        ComboViewer viewer = new ComboViewer(combo);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(provider);
        viewer.setInput(options);
        viewer.setSelection(new StructuredSelection(selection), true);
        GridData data = new GridData();
        data.verticalAlignment = GridData.CENTER;
        data.grabExcessHorizontalSpace=true;
        combo.setLayoutData(data);
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

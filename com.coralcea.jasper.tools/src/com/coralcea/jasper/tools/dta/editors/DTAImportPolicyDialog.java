package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

public class DTAImportPolicyDialog extends Dialog {

	public static void openToEdit(Shell shell, DTAEditor editor) {
		String title = "Import Policy";
		String prompt = "Configure a map between imported model URIs and their file URLs:";
		DTAImportPolicyDialog dialog = new DTAImportPolicyDialog(shell, title, prompt, editor.getFile());
		if (dialog.open() == Dialog.OK)
			reloadEditor(editor);
	}

	public static List<String> openToSelect(Shell shell, DTAEditor editor) {
		String title = "Add Import";
		String prompt = "Select a model to import:";
		DTAImportPolicyDialog dialog = new DTAImportPolicyDialog(shell, title, prompt, editor.getFile());
		if (dialog.open() == Dialog.OK) {
			    return dialog.selectedImports;
		}
		return Collections.emptyList();
	}
	
	private static void reloadEditor(final DTAEditor editor) {
		MessageDialog reload = new MessageDialog(editor.getSite().getShell(), "Model Imports Reload", null, "Do you want to reload the model imports?", MessageDialog.QUESTION_WITH_CANCEL, new String[]{"Yes", "No"}, 0);
		if (reload.open() == MessageDialog.OK) {
			editor.reload();
		}
	}

	protected IFile modelFile;
	protected IFile policy;
	protected Model model;
	protected String title;
	protected String prompt;
	protected TableViewer viewer;
	protected List<String> selectedImports;
	
	protected DTAImportPolicyDialog(Shell shell, String title, String prompt, IFile modelFile) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		
		this.modelFile = modelFile;
		this.title = title;
		this.prompt = prompt;
		this.selectedImports = new ArrayList<String>();
		
		loadImportPolicyModel();
	}
	
	@Override
	protected void okPressed() {
		super.okPressed();
		DTACore.saveImportPolicyModel(model, policy);
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(title);
	}

	protected void loadImportPolicyModel() {
        IContainer folder = modelFile.getParent();
		policy = folder.getFile(Path.fromOSString(DTA.IMPORT_POLICY));
		model = DTACore.loadImportPolicyModel(policy);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.numColumns = 2;
		
		Label label = new Label(container, SWT.NONE);
		label.setText(prompt);
		GridData layoutData = new GridData();
		layoutData.horizontalSpan = 2;
		label.setLayoutData(layoutData);
		
		final TableViewer viewer = new TableViewer(container, SWT.MULTI | SWT.HORIZONTAL | SWT.VERTICAL | SWT.FULL_SELECTION | SWT.BORDER);
 		viewer.setContentProvider(new IStructuredContentProvider() {
			public Object[] getElements(Object inputElement) {
				List<Resource> statements = model.listResourcesWithProperty(RDF.type, OntDocumentManager.ONTOLOGY_SPEC).toList();
				return statements.toArray();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			public void dispose() {
			}
		});
 		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectedImports.clear();
				StructuredSelection s = (StructuredSelection) event.getSelection();
				if (s != null) {
					for (Object obj : s.toList()) {
						Resource r = (Resource)obj;
						selectedImports.add(r.getPropertyResourceValue( OntDocumentManager.PUBLIC_URI ).getURI());
					}
				}
			}
		});

		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(layoutData);

		TableViewerColumn colURI = new TableViewerColumn(viewer, SWT.NONE);
		colURI.setEditingSupport(new CellEditingSupport(viewer, OntDocumentManager.PUBLIC_URI)); 
		colURI.getColumn().setWidth(200);
		colURI.getColumn().setText("Model Name");
		colURI.setLabelProvider(new ColumnLabelProvider() {
		  public String getText(Object element) {
		    Resource r = (Resource) element;
		    return r.getPropertyResourceValue( OntDocumentManager.PUBLIC_URI ).getURI();
		  }
		});
		
		TableViewerColumn fileURI = new TableViewerColumn(viewer, SWT.NONE);
		fileURI.setEditingSupport(new CellEditingSupport(viewer, OntDocumentManager.ALT_URL)); 
		fileURI.getColumn().setWidth(200);
		fileURI.getColumn().setText("File URL");
		fileURI.setLabelProvider(new ColumnLabelProvider() {
		  public String getText(Object element) {
		    Resource r = (Resource) element;
		    return r.getPropertyResourceValue( OntDocumentManager.ALT_URL ).getURI();
		  }
		});

		viewer.setInput(model);

		Composite buttonColumn = new Composite(container, SWT.NONE);
		layoutData = new GridData();
		layoutData.verticalAlignment = SWT.TOP;
		buttonColumn.setLayoutData(layoutData);
		FillLayout layout2 = new FillLayout(SWT.VERTICAL);
		buttonColumn.setLayout(layout2);
		
		Button addButton = new Button(buttonColumn, SWT.PUSH);
		addButton.setText("Add");
		addButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				Resource entry = DTACore.addImportPolicyEntry(model, "http://www.xyz.org/model1", "file:Model1.dta");
				viewer.refresh();
				viewer.setSelection(new StructuredSelection(entry), true);
			}
		});
		
		Button removeButton = new Button(buttonColumn, SWT.PUSH);
		removeButton.setText("Remove");
		removeButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				List<Statement> statements = new ArrayList<Statement>();
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				for (Object e : selection.toList())
					statements.addAll(model.listStatements((Resource)e, null, (RDFNode)null).toList());
				model.remove(statements);
				viewer.refresh();
			}
		});

		return container;
	}

	protected class CellEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final CellEditor editor;
		private final Property property;

		public CellEditingSupport(TableViewer viewer, Property property) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());
			this.editor.setValidator(new ICellEditorValidator() {
				public String isValid(Object value) {
					return DTAUtilities.isValidURI((String)value) ? null : "Invalid Name";
				}
			});
			this.property = property;
		}

		@Override
		protected CellEditor getCellEditor(Object element) {
			return editor;
		}

		@Override
		protected boolean canEdit(Object element) {
			return true;
		}

		@Override
		protected Object getValue(Object element) {
		    Resource r = (Resource) element;
		    return r.getPropertyResourceValue(property).getURI();
		}

		@Override
		protected void setValue(Object element, Object userInputValue) {
			if (userInputValue != null) {
			    Resource r = (Resource) element;
			    r.removeAll(property);
			    r.addProperty(property, r.getModel().getResource(String.valueOf(userInputValue)));
				viewer.update(element, null);
			}
		}
	}
}

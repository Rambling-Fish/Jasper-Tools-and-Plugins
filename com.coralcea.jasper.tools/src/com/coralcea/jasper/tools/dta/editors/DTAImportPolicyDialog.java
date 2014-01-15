package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
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
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.FileUtils;
import com.hp.hpl.jena.vocabulary.RDF;

public class DTAImportPolicyDialog extends Dialog {

	private DTAEditor editor;
	private IFile policy;
	private Model model;
	
	public static void run(Shell shell, DTAEditor editor) {
		DTAImportPolicyDialog dialog = new DTAImportPolicyDialog(shell, editor);
		try {
			if (dialog.open() == Dialog.OK)
				DTACore.saveModel(dialog.model, dialog.policy, null);
		} catch (Exception e) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed to save the DTA policy file");
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		} finally {
			dialog.model.close();
		}
	}
	
	private DTAImportPolicyDialog(Shell shell, DTAEditor editor) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE); 
		this.editor = editor;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		final Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = (GridLayout) container.getLayout();
		layout.numColumns = 2;
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Configure a map between imported model URIs and their file URLs:");
		GridData layoutData = new GridData();
		layoutData.horizontalSpan = 2;
		label.setLayoutData(layoutData);
		
		final TableViewer viewer = new TableViewer(container, SWT.MULTI | SWT.HORIZONTAL | SWT.VERTICAL | SWT.FULL_SELECTION | SWT.BORDER);
		Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);		
		layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
		table.setLayoutData(layoutData);

		TableViewerColumn colURI = new TableViewerColumn(viewer, SWT.NONE);
		colURI.setEditingSupport(new CellEditingSupport(viewer, OntDocumentManager.PUBLIC_URI)); 
		colURI.getColumn().setWidth(200);
		colURI.getColumn().setText("Model URI");
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
		
        model = ModelFactory.createDefaultModel() ;
        model.setNsPrefix("", OntDocumentManager.NS);
        IProject project = editor.getFileEditorInput().getFile().getProject();
		policy = project.getFile(DTACore.IMPORT_POLICY);
		if (policy.exists()) {
	        try {
				model.read(policy.getContents(), null, "RDF/XML-ABBREV" );
			} catch (CoreException e) {
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed to read the DTA policy file");
				StatusManager.getManager().handle(status, StatusManager.SHOW);
			}
		}
		
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
				Resource entry = model.createResource();
				model.add(entry, RDF.type, OntDocumentManager.ONTOLOGY_SPEC);
				entry.addProperty(OntDocumentManager.PUBLIC_URI, model.getResource("http://www.xyz.org/model1"));
				entry.addProperty(OntDocumentManager.ALT_URL, model.getResource("file:Model1.dta"));
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

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Import Policy");
	}

	public class CellEditingSupport extends EditingSupport {

		private final TableViewer viewer;
		private final CellEditor editor;
		private final Property property;

		public CellEditingSupport(TableViewer viewer, Property property) {
			super(viewer);
			this.viewer = viewer;
			this.editor = new TextCellEditor(viewer.getTable());
			this.editor.setValidator(new ICellEditorValidator() {
				public String isValid(Object value) {
					return DTAUtilities.isValidURI((String)value) ? null : "Invalid URI";
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

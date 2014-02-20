package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;

public class DTALibraryWizardPage extends WizardPage {

	public static final String NATURE = "org.mule.tooling.core.muleNature";
	
	private ComboViewer projectName;
	private Text modelName;
	private Text modelURI;
	private Text modelNamespace;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for DTALibraryWizardPage.
	 * 
	 * @param pageName
	 */
	public DTALibraryWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Create a new DTA Library");
		setDescription("Create and configure a new DTA library.");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		
		Label label = new Label(container, SWT.NULL);
		label.setText("Mule &project:");
		label.setToolTipText("The path to a Mule priject");

		projectName = new ComboViewer(container, SWT.READ_ONLY);
		projectName.setContentProvider(ArrayContentProvider.getInstance());
		projectName.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		projectName.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IProject)element).getName();
			}
        });
		projectName.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return DTAUtilities.hasNature((IProject)element, NATURE);
			}
		});
		projectName.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				dialogChanged();
				IProject project = getProject();
				if (project != null && getModelName().length()==0)
					modelName.setText("Library1");
			}
		});
		projectName.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
        
		label = new Label(container, SWT.NULL);
		label.setText("Library &Name:");
		label.setToolTipText("The unique name of the DTA library");

		modelName = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				String name = getModelName().toLowerCase();
				String uri = getModelURI();
				if (uri.length()==0)
					modelURI.setText("http://mycompany.com/"+name);
				else {
					String path = uri.substring(0, uri.lastIndexOf('/', uri.length()-2)+1);
					modelURI.setText(path+name);
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Library &URI:");
		label.setToolTipText("The unique URI of the DTA library");

		modelURI = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelURI.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelURI.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				String uri = getModelURI();
				String namespace = getModelNamespace();
				if (namespace.length()==0)
					modelNamespace.setText(uri+"#");
				else {
					String separator = namespace.substring(namespace.length()-1);
					modelNamespace.setText(uri+separator);
				}
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Library &Namespace:");
		label.setToolTipText("The default namespace of the DTA library");

		modelNamespace = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelNamespace.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelNamespace.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		initialize();
		dialogChanged();
		setControl(container);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */
	private void initialize() {
		if (selection != null && !selection.isEmpty()) {
			if (selection.size() > 1)
				return;
			Object obj = selection.getFirstElement();
			if (obj instanceof IAdaptable)
				obj = ((IAdaptable)obj).getAdapter(IResource.class);
			if (obj instanceof IResource) {
				IProject project = ((IResource) obj).getProject();
				if (DTAUtilities.hasNature(project, NATURE))
					projectName.setSelection(new StructuredSelection(project));
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IProject project = getProject();
		if (project == null) {
			updateStatus("Mule project must be specified");
			return;
		}
		if (!project.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		
		IResource model = project.findMember("src/main/app/"+getModelName()+"."+DTA.EXTENSION);
		if (model != null && model.exists()) {
			updateStatus("DTA library already exists for this project");
			return;
		}
		
		if (!DTAUtilities.isValidURI(getModelURI())) {
			updateStatus("Invalid library URI");
			return;
		}

		if (!DTAUtilities.isValidNsURI(getModelNamespace())) {
			updateStatus("Invalid library namespace");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public IProject getProject() {
		return (IProject) ((IStructuredSelection)projectName.getSelection()).getFirstElement();
	}

	public String getModelName() {
		return modelName.getText();
	}

	public String getModelURI() {
		return modelURI.getText();
	}

	public String getModelNamespace() {
		return modelNamespace.getText();
	}

}
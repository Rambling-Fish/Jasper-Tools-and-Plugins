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

public class DTAModelWizardPage extends WizardPage {

	public static final String MULE_NATURE = "org.mule.tooling.core.muleNature";
	
	private ComboViewer projectName;
	private Text modelFile;
	private Text modelName;
	private Text modelNamespace;
	private Text dtaName;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for ConvertToDTAWizardPage.
	 * 
	 * @param pageName
	 */
	public DTAModelWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Create a new DTA model");
		setDescription("Create and configure a new DTA model.");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(2, false));
		
		Label label = new Label(container, SWT.NULL);
		label.setText("Mule project:");
		label.setToolTipText("The path to a Mule project");

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
				return DTAUtilities.hasNature((IProject)element, MULE_NATURE);
			}
		});
		projectName.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IProject project = getProject();
				if (project != null) {
					String projectName = project.getName();
					modelFile.setText(projectName+"."+DTA.EXTENSION);
				}
				dialogChanged();
			}
		});
		projectName.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
        
		label = new Label(container, SWT.NULL);
		label.setText("Model file:");
		label.setToolTipText("The unique name of the DTA model file (.dta)");

		modelFile = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String postfix = getModelFile().toLowerCase();
				int index = postfix.lastIndexOf('.');
				if (index >= 0)
					postfix = postfix.substring(0, index);
				String name = getModelName();
				if (name.length()==0)
					name = "http://mycompany.com/";
				else
					name = name.substring(0, name.lastIndexOf('/')+1);
				modelName.setText(name+postfix);
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Model name:");
		label.setToolTipText("The unique name of the DTA model");

		modelName = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String name = getModelName();
				String namespace = getModelNamespace();
				if (namespace.length()==0)
					modelNamespace.setText(name+"#");
				else {
					String separator = namespace.substring(namespace.length()-1);
					modelNamespace.setText(name+separator);
				}
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Default namespace:");
		label.setToolTipText("The default namespace of the DTA model");

		modelNamespace = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelNamespace.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		modelNamespace.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String postfix = getModelFile();
				int index = postfix.lastIndexOf('.');
				if (index >= 0)
					postfix = postfix.substring(0, index);
				String namespace = getModelNamespace();
				dtaName.setText(namespace+postfix);
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("DTA name:");
		label.setToolTipText("The unique name of the DTA");

		dtaName = new Text(container, SWT.BORDER | SWT.SINGLE);
		dtaName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		dtaName.addModifyListener(new ModifyListener() {
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
				if (DTAUtilities.hasNature(project, MULE_NATURE))
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
		
		if (!getModelFile().endsWith("."+DTA.EXTENSION)) {
			updateStatus("File must end with .dta");
			return;
		}

		IResource model = project.findMember("src/main/app/"+getModelFile());
		if (model != null && model.exists()) {
			updateStatus("Model file already exists in this project");
			return;
		}
		
		if (!DTAUtilities.isValidURI(getModelName())) {
			updateStatus("Invalid model name");
			return;
		}

		if (getModelNamespace().length()>0 && !DTAUtilities.isValidNsURI(getModelNamespace())) {
			updateStatus("Invalid model namespace");
			return;
		}

		if (!DTAUtilities.isValidURI(getDTAName())) {
			updateStatus("Invalid DTA name");
			return;
		}

		if (getDTAName().equals(getModelName())) {
			updateStatus("Model and DTA have the same name");
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

	public String getModelFile() {
		return modelFile.getText();
	}

	public String getModelName() {
		return modelName.getText();
	}

	public String getModelNamespace() {
		return modelNamespace.getText();
	}

	public String getDTAName() {
		return dtaName.getText();
	}
}
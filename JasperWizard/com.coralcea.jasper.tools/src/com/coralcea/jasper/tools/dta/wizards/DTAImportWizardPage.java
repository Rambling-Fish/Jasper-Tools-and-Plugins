package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntModel;

public abstract class DTAImportWizardPage extends WizardPage {

	private static final String MULE_NATURE = "org.mule.tooling.core.muleNature";

	private ComboViewer projectName;
	private Text filePath;
	private Text modelName;
	private Button loadButton;
	private OntModel loadedModel;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for DTALibraryWizardPage.
	 */
	public DTAImportWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout(4, false);
		layout.horizontalSpacing = 0;
		container.setLayout(layout);
		
		Label label = new Label(container, SWT.NULL);
		label.setText("Mule project:");
		label.setToolTipText("The path to a Mule project");

		projectName = new ComboViewer(container, SWT.READ_ONLY);
		projectName.setContentProvider(ArrayContentProvider.getInstance());
		projectName.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)projectName.getCombo().getLayoutData()).horizontalSpan = 3;
		projectName.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((IProject)element).getName();
			}
        });
		projectName.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return DTAUtilities.hasNature((IProject)element, DTAImportWizardPage.MULE_NATURE);
			}
		});
		projectName.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				dialogChanged();
			}
		});
		projectName.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
        
		label = new Label(container, SWT.NULL);
		label.setText("Library name:");
		label.setToolTipText("The name of the DTA library");

		modelName = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)modelName.getLayoutData()).horizontalSpan = 3;
		modelName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = createFileLabel(container);

		filePath = new Text(container, SWT.BORDER | SWT.SINGLE);
		filePath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filePath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				String path = new Path(filePath.getText()).removeFileExtension().lastSegment();
				modelName.setText(path!=null ? path : "");
				loadButton.setEnabled(path!=null);
			}
		});

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				String result = dialog.open();
				if (result.length()>0)
					filePath.setText(result);
			}
		});

		loadButton = new Button(container, SWT.PUSH);
		loadButton.setText("Load");
		loadButton.setEnabled(false);
		loadButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				loadModel();
				dialogChanged();
			}
		});
		
		initialize();
		dialogChanged();
		setControl(container);
	}
	
	protected abstract Label createFileLabel(Composite parent);

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
				if (DTAUtilities.hasNature(project, DTAImportWizardPage.MULE_NATURE))
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
		
		if (getOntologyPath().length()>0) {
			if (!DTAUtilities.isValidFile(getOntologyPath())) {
				loadButton.setEnabled(false);
				updateStatus("Invalid file path");
				return;
			}
			
			if (getLoadedModel() == null) {
				updateStatus("File must be (re)loaded");
				return;
			}
		}

		IResource model = project.findMember("src/main/app/"+getModelName()+"."+DTA.EXTENSION);
		if (model != null && model.exists()) {
			updateStatus("DTA library already exists for this project");
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

	protected String getOntologyPath() {
		return filePath.getText();
	}

	public String getModelName() {
		return modelName.getText();
	}

	public OntModel getLoadedModel() {
		return loadedModel;
	}

	protected void loadModel() {
		unloadModel();
		loadedModel = importFile(getOntologyPath());
	}

	protected void unloadModel() {
		if (loadedModel!=null) {
			loadedModel.close();
			loadedModel = null;
		}
	}
	
	protected abstract OntModel importFile(String path);
	
}

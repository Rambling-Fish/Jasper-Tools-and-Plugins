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

public class DTAImportWizardPage extends WizardPage {

	private static final String MULE_NATURE = "org.mule.tooling.core.muleNature";

	private ComboViewer projectName;
	private Text modelFile;
	private ComboViewer fileKind;
	private Text filePath;
	private Button loadButton;
	private OntModel loadedModel;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for DTALibraryWizardPage.
	 */
	public DTAImportWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Import DTA library from a file");
		setDescription("Create a new DTA library based on an imported file");
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
		label.setText("Library file:");
		label.setToolTipText("The unique name of the DTA library file (.dta)");

		modelFile = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelFile.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)modelFile.getLayoutData()).horizontalSpan = 3;
		modelFile.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Imported file:");
		label.setToolTipText("The path to the imported file");

		filePath = new Text(container, SWT.BORDER | SWT.SINGLE);
		filePath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filePath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				String path = new Path(filePath.getText()).removeFileExtension().lastSegment()+"."+DTA.EXTENSION;
				modelFile.setText(path);
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
		loadButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				loadModel();
				dialogChanged();
			}
		});
		
		label = new Label(container, SWT.NULL);
		label.setText("Import kind:");
		label.setToolTipText("The kind of file to import");

		fileKind = new ComboViewer(container, SWT.READ_ONLY);
		fileKind.setContentProvider(ArrayContentProvider.getInstance());
		fileKind.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)fileKind.getCombo().getLayoutData()).horizontalSpan = 3;
		fileKind.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				return ((DTAImporter)element).getName();
			}
        });
		fileKind.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				dialogChanged();
			}
		});
		fileKind.setInput(new DTAImporter[]{new DTAOwlImporter(), new DTARdfImporter(), new DTAXsdImporter(), new DTAXmlImporter(),  new DTAJsonImporter()});

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
				if (DTAUtilities.hasNature(project, DTAImportWizardPage.MULE_NATURE))
					projectName.setSelection(new StructuredSelection(project));
			}
		}
		fileKind.setSelection(new StructuredSelection(((DTAImporter[])fileKind.getInput())[0]));
		loadButton.setEnabled(false);
		filePath.setFocus();
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
		
		String filepath = getFilePath();
		if (filepath.length() == 0) {
			loadButton.setEnabled(false);
			updateStatus("Imported file must be specified");
			return;
		}
		
		if (!DTAUtilities.isValidFile(filepath)) {
			loadButton.setEnabled(false);
			updateStatus("Invalid imported file path");
			return;
		}
			
		loadButton.setEnabled(true);

		String modelName = getModelFile();
		if (modelName.length() == 0) {
			updateStatus("Library file must be specified");
			return;
		}
		
		if (!getModelFile().endsWith("."+DTA.EXTENSION)) {
			updateStatus("Library file name must end with .dta");
			return;
		}

		IResource model = project.findMember("src/main/app/"+modelName);
		if (model != null && model.exists()) {
			updateStatus("Library file already exists in this project");
			return;
		}
		
		if (getLoadedModel() == null) {
			updateStatus("Imported file must be (re)loaded");
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

	public DTAImporter getFileImporter() {
		return (DTAImporter) ((IStructuredSelection)fileKind.getSelection()).getFirstElement();
	}

	protected String getFilePath() {
		return filePath.getText();
	}

	public OntModel getLoadedModel() {
		return loadedModel;
	}

	protected void loadModel() {
		unloadModel();
		loadedModel = importFile(getFilePath());
	}

	protected void unloadModel() {
		if (loadedModel!=null) {
			loadedModel.close();
			loadedModel = null;
		}
	}
	
	protected OntModel importFile(String path) {
		return getFileImporter().importFile(path);
	}
	
}

package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

import com.coralcea.jasper.tools.dta.DTAUtilities;

public class DTAModelWizardPage extends WizardPage {

	public static final String NATURE = "org.mule.tooling.core.muleNature";
	
	private Text folderName;
	private Text modelName;
	private Text modelURI;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for ConvertToDTAWizardPage.
	 * 
	 * @param pageName
	 */
	public DTAModelWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Create a new DTA Model");
		setDescription("Create and configure a new DTA model.");
		this.selection = selection;
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		
		Label label = new Label(container, SWT.NULL);
		label.setText("Parent folder:");
		label.setToolTipText("The path to the parent folder");

		folderName = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		folderName.setLayoutData(gd);
		folderName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});
		
		label = new Label(container, SWT.NULL);
		label.setText("&Model Name:");
		label.setToolTipText("The name of a DTA model (.dta)");

		modelName = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		modelName.setLayoutData(gd);
		modelName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("&Model URI:");
		label.setToolTipText("A unique URI for the model");

		modelURI = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		modelURI.setLayoutData(gd);
		modelURI.addModifyListener(new ModifyListener() {
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
				IContainer container;
				if (obj instanceof IContainer)
					container = (IContainer) obj;
				else
					container = ((IResource) obj).getParent();
			   folderName.setText(container.getFullPath().toString());
			}
		}
		modelName.setText("Model1");
		modelURI.setText("http://www.xyz.org/model1");
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, null);
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				folderName.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getFolderName()));

		if (getFolderName().length() == 0) {
			updateStatus("A folder must be specified");
			return;
		}
		if (container == null || !(container instanceof IContainer)) {
			updateStatus("Folder must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Folder must be writable");
			return;
		}
		
		IResource model = ((IContainer)container).findMember(getModelName()+".dta");
		
		if (model != null && model.exists()) {
			updateStatus("Model already exists");
			return;
		}
		if (!DTAUtilities.isValidURI(getModelURI())) {
			updateStatus("Invalid Model URI");
			return;
		}

		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getFolderName() {
		return folderName.getText();
	}

	public String getModelName() {
		return modelName.getText();
	}

	public String getModelURI() {
		return modelURI.getText();
	}

}
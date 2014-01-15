package com.coralcea.jasper.tools.dta.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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

/**
 * 
 */

public class ConvertToDTAWizardPage extends WizardPage {
	
	private static final String MULE_NATURE = "org.mule.tooling.core.muleNature";
	
	private Text containerText;

	private IStructuredSelection selection;
	
	/**
	 * Constructor for ConvertToDTAWizardPage.
	 * 
	 * @param pageName
	 */
	public ConvertToDTAWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Convert to a DTA Project");
		setDescription("This wizard converts an existing mule project to a DTA project.");
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
		label.setText("&Mule project:");

		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setLayoutData(gd);
		containerText.addModifyListener(new ModifyListener() {
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
			if (obj instanceof IResource) {
				IProject project;
				if (obj instanceof IProject)
					project = (IProject) obj;
				else
					project = ((IResource) obj).getProject();
				if (DTAUtilities.hasNature(project, MULE_NATURE))
				   containerText.setText(project.getFullPath().toString());
			}
		}
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for
	 * the container field.
	 */

	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(
				getShell(), ResourcesPlugin.getWorkspace().getRoot(), false,
				"Select a Mule project");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));

		if (getContainerName().length() == 0) {
			updateStatus("Project must be specified");
			return;
		}
		if (container == null || (container.getType() & IResource.PROJECT) == 0) {
			updateStatus("Project must exist");
			return;
		}
		if (!DTAUtilities.hasNature((IProject)container, MULE_NATURE)) {
			updateStatus("Not a Mule project");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		updateStatus(null);
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getContainerName() {
		return containerText.getText();
	}

}
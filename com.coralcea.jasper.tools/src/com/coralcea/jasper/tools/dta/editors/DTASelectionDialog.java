package com.coralcea.jasper.tools.dta.editors;

import java.util.Collection;
import java.util.Comparator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTALabelProvider;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTASelectionDialog <T extends Resource> extends FilteredItemsSelectionDialog {

	private static final String DIALOG_SETTINGS = "DTASelectionDialogSettings";
	private static final int ID_NONE = -555;
	private static final int ID_NEW = -666;

	private OntModel model;
	private Collection<T> resources;
	private boolean hasNone;
	
	private static final DTALabelProvider labelProvider = new DTALabelProvider2();
	private static class DTALabelProvider2 extends DTALabelProvider implements IStyledLabelProvider {

		@Override
		public String getText(Object element) {
			if (element != null) {
				Resource r = (Resource)element;
				return r.getLocalName()+" - "+super.getText(element);
			}
			return "";
		}

		@Override
		public StyledString getStyledText(Object element) {
			StyledString s = new StyledString(getText(element));
			int offset = s.getString().indexOf("-");
			if (offset > -1) {
				s.setStyle(offset, s.length() - offset, StyledString.QUALIFIER_STYLER);
			}
			return s;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Resource> T run(String title, OntModel model, Collection<T> resources, boolean hasNone) {
		Shell shell = new Shell();
		FilteredItemsSelectionDialog dialog = new DTASelectionDialog<T>(shell, title, model, resources, hasNone);
		int code = dialog.open();
		if (code == Dialog.OK) 
			return (T) dialog.getFirstResult();
		else if (code == ID_NONE)
			return (T) DTA.None;
		else if (code == ID_NEW)
			return (T) DTA.New;
		return null;
	}
	
	public DTASelectionDialog(Shell shell, String title, OntModel model, Collection<T> resources, boolean hasNone) {
		super(shell);
		this.model = model;
		this.resources = resources;
		this.hasNone = hasNone;
		setTitle(title);
		setSelectionHistory(new ResourceSelectionHistory());
		setListLabelProvider(labelProvider);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		if (hasNone)
			createButton(parent, ID_NONE, "None", false);
		createButton(parent, ID_NEW, "New...", false);
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (ID_NONE == buttonId || ID_NEW == buttonId) {
			setReturnCode(buttonId);
			close();
		} else
			super.buttonPressed(buttonId);
	}
	
	@Override
	protected Control createExtendedContentArea(Composite parent) {
		return null;
	}

	@Override
	protected IDialogSettings getDialogSettings() {
		IDialogSettings settings = Activator.getDefault().getDialogSettings().getSection(DIALOG_SETTINGS);
		if (settings == null)
			settings = Activator.getDefault().getDialogSettings().addNewSection(DIALOG_SETTINGS);
		return settings;
	}

	@Override
	protected IStatus validateItem(Object item) {
		return Status.OK_STATUS;
	}

	@Override
	protected ItemsFilter createFilter() {
		return new ItemsFilter() {
			@SuppressWarnings("unchecked")
			public boolean matchItem(Object item) {
				return matches(labelProvider.getText((T)item));
			}
			public boolean isConsistentItem(Object item) {
				return true;
			}
		};
	}

	@Override
	protected Comparator<T> getItemsComparator() {
		return new Comparator<T>() {
			public int compare(T arg0, T arg1) {
				return labelProvider.getText(arg0).compareTo(labelProvider.getText(arg1));
			}
		};
	}

	@Override
	protected void fillContentProvider(AbstractContentProvider contentProvider, ItemsFilter itemsFilter, IProgressMonitor progressMonitor) throws CoreException {
		progressMonitor.beginTask("Searching", resources.size()); //$NON-NLS-1$
		for (T r : resources) {
			contentProvider.add(r, itemsFilter);
			progressMonitor.worked(1);
		}
		progressMonitor.done();
	}

	@SuppressWarnings("unchecked")
	@Override
	public String getElementName(Object item) {
		return labelProvider.getText((T)item);
	}

	private class ResourceSelectionHistory extends SelectionHistory {
		protected Object restoreItemFromMemento(IMemento element) {
			String uri = element.getString("uri");
			Resource r = model.getOntResource(uri);
			return (r != null && resources.contains(r)) ? r : null;
		}
		protected void storeItemToMemento(Object item, IMemento element) {
			element.putString("uri", ((Resource)item).getURI());
		}
	}
	
}

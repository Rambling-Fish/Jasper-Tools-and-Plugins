package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.eclipse.gef.commands.CommandStackEventListener;
import org.eclipse.gef.commands.CommandStackListener;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.actions.UpdateAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.INavigationLocation;
import org.eclipse.ui.INavigationLocationProvider;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.texteditor.DocumentProviderRegistry;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAModelValidator;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTAEditor extends MultiPageEditorPart implements IResourceChangeListener, IResourceDeltaVisitor , INavigationLocationProvider, CommandStackListener, CommandStackEventListener {

	public static final String ID = "com.coralcea.jasper.editors.DTA";
	public static final String PAGE_MODEL = "Model";
	public static final String PAGE_CLASS = "UML Diagram";
	public static final String PAGE_BROWSE = "Browse Diagram";
	public static final String PAGE_NAMESPACES = "Namespaces";
	public static final String PAGE_SOURCE = "Source";
	
	private OntModel model;
	private List<DTAViewer> viewers;
	private CommandStack commandStack;
	private ActionRegistry actionRegistry;
	private List<String> stackActions = new ArrayList<String>();
	private FormToolkit toolkit;
	private boolean ignorePageActivation = false;
	
	public DTAEditor() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		commandStack = new CommandStack();
		commandStack.addCommandStackListener(this);
		commandStack.addCommandStackEventListener(this);
		viewers = new ArrayList<DTAViewer>();
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		getCommandStack().removeCommandStackEventListener(this);
		getCommandStack().removeCommandStackListener(this);
		if (isDirty()) {
			IFile file = getFile();
			DTACore.unloadModel(file);
			DTACore.notifyListeners(file);
		}
		getCommandStack().dispose();
		toolkit.dispose();
		super.dispose();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput))
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		super.init(site, editorInput);
		createActions();
	}
	
	public FormToolkit getFormToolkit() {
		return toolkit;
	}

	@Override
   protected void setInput(IEditorInput input) {
    	super.setInput(input);
		setPartName(input.getName());
		IFile file = ((IFileEditorInput)input).getFile();
		try {
			OntModel newModel = DTACore.getPossiblyLoadedModel(file);
			if (newModel != getModel()) {
				setModel(newModel);
				refresh();
			}
		} catch (Exception e) {
			Activator.getDefault().log("Error opening DTA file", e);
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage());
			StatusManager.getManager().handle(status, StatusManager.SHOW);
		}
    }
 
	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IGotoMarker.class)
			return getGotoAdapter();
		if (adapter == ActionRegistry.class)
			return getActionRegistry();
		else if (adapter == CommandStack.class)
			return getCommandStack();
		return super.getAdapter(adapter);
	}

	protected ActionRegistry getActionRegistry() {
		if (actionRegistry == null)
			actionRegistry = new ActionRegistry();
		return actionRegistry;
	}

	protected void createActions() {
		ActionRegistry registry = getActionRegistry();
		IAction action;
		
		action = new UndoAction(this);
		registry.registerAction(action);
		stackActions.add(action.getId());
		
		action = new RedoAction(this);
		registry.registerAction(action);
		stackActions.add(action.getId());
	}
	
	protected void updateActions(List<String> actionIds) {
		ActionRegistry registry = getActionRegistry();
		for(String id : actionIds) {
			IAction action = registry.getAction(id);
			if (action instanceof UpdateAction)
				((UpdateAction) action).update();
		}
	}

	protected void setModel(OntModel model) {
		this.model = model;
		
		for(DTAViewer viewer : viewers)
			viewer.setInput(model);
	}
	
	CommandStack getCommandStack() {
		return commandStack;
	}
	
	public void executeCommand(Command command, boolean refresh) {
		if (refresh)
			getCommandStack().execute(command);
		else {
			getCommandStack().removeCommandStackEventListener(this);
			getCommandStack().execute(command);
			getCommandStack().addCommandStackEventListener(this);
		}
	}

	protected void createModelPage() {
		DTAViewer viewer = new DTAPropertiesViewer(getContainer(), this);
        int index = addPage(viewer.getControl());
		setPageText(index, PAGE_MODEL);
		viewers.add(viewer);
	}

	protected void createSourcePage() {
		DTAViewer viewer = new DTASourceViewer(getContainer(), this);
		int index = addPage(viewer.getControl());
		setPageText(index, PAGE_SOURCE);
		viewers.add(viewer);
	}

	protected void createNamespacesPage() {
		DTAViewer viewer = new DTANamespacesViewer(getContainer(), this);
		int index = addPage(viewer.getControl());
		setPageText(index, PAGE_NAMESPACES);
		viewers.add(viewer);
	}

	protected void createClassDiagramPage() {
		DTAViewer viewer = new DTAClassDiagramViewer(getContainer(), this);
		int index = addPage(viewer.getControl());
		setPageText(index, PAGE_CLASS);
		viewers.add(viewer);
	}

	protected void createBrowseDiagramPage() {
		DTAViewer viewer = new DTABrowseDiagramViewer(getContainer(), this);
		int index = addPage(viewer.getControl());
		setPageText(index, PAGE_BROWSE);
		viewers.add(viewer);
	}

	@Override
	protected void createPages() {
		toolkit = new FormToolkit(getContainer().getDisplay());
		
		createModelPage();
		createClassDiagramPage();
		createBrowseDiagramPage();
		createNamespacesPage();
		createSourcePage();
		
		setModel(model);
		
		getSite().setSelectionProvider(new DTASelectionProvider(this));
	}
	
	@Override
	protected void pageChange(int newPageIndex) {
		super.pageChange(newPageIndex);
		getViewer(newPageIndex).activate();
		if (!ignorePageActivation)
			markLocation();
	}
	
	private void refresh() {
		if (getActivePage() != -1) {
			DTAViewer viewer = getViewer(getActivePage());
			viewer.refresh();
		}
	}

	@Override
	public void setActivePage(int pageIndex) {
		// needed so DTANavigationLocation can have visibility to call it
		super.setActivePage(pageIndex);
	}

	public OntModel getModel() {
		return model;
	}
	
	DTAViewer getViewer(int page) {
		return (page >= 0) ? viewers.get(page) : null;
	}
	
	public DTAViewer getActiveViewer() {
		return getViewer(getActivePage());
	}
	
	public void setSelectedElement(Resource element, String pageName) {
		int page = getPage(pageName);
		if (page!=getActivePage()) {
			ignorePageActivation = true;
			setActivePage(page);
			ignorePageActivation = false;
		}
		setSelectedElement(element);
	}
	
	private int getPage(String name) {
		for (int i=0; i<viewers.size(); i++) {
			if (getPageText(i).equals(name))
				return i;
		}
		return -1;
	}
	
	public void setSelectedElement(Resource element) {
		if (getActivePage() != -1 && element != null) {
			DTAViewer viewer = getViewer(getActivePage());
			viewer.setSelection(new StructuredSelection(element), true);
		}
	}
	
	public Resource getSelectedElement() {
		if (getActivePage() != -1) {
			IStructuredSelection selection = (IStructuredSelection) getViewer(getActivePage()).getSelection();
			if (selection.getFirstElement() instanceof Resource)
				return (Resource)selection.getFirstElement();
		}
		return null;
	}
	
	public String getInternalState() {
		if (getActivePage() != -1) {
			return getViewer(getActivePage()).getInternalState();
		}
		return null;
	}
	
	public void setInternalState(String state) {
		if (getActivePage() != -1) {
			getViewer(getActivePage()).setInternalState(state);
		}
	}

	void markLocation() {
		getSite().getPage().getNavigationHistory().markLocation(this);
	}
	
	@Override
	public INavigationLocation createEmptyNavigationLocation() {
		return new DTANavigationLocation(this, false);
	}

	@Override
	public INavigationLocation createNavigationLocation() {
		return new DTANavigationLocation(this, true);
	}
	
	public IFileEditorInput getFileEditorInput() {
		return (IFileEditorInput) getEditorInput();
	}

	public IFile getFile() {
		return getFileEditorInput().getFile();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		try {
			monitor.beginTask("Saving", 2);
			validate(new SubProgressMonitor(monitor, 1));
			DTACore.saveModel(model, getFile(), false, new SubProgressMonitor(monitor, 1));
			getCommandStack().markSaveLocation();
		} catch(Throwable t) {
			Activator.getDefault().log("Error saving DTA file", t);
		} finally {
			monitor.done();
		}
	}
	
	protected void validate(IProgressMonitor monitor) throws CoreException {
		IFile file = getFile();
		ResourcesPlugin.getWorkspace().run(DTAModelValidator.getRunnable(file, model), new SubProgressMonitor(monitor, 3));
		if (file.findMarkers(DTA.MARKER, false, IResource.DEPTH_ZERO).length!=0) {
			Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, "Validation problems can be inspected in Problems view");
			StatusManager.getManager().handle(status, StatusManager.SHOW);
			return;
		}
	}
	
	@Override
	public void doSaveAs() {
		IFileEditorInput input = getFileEditorInput();
		IDocumentProvider provider= DocumentProviderRegistry.getDefault().getDocumentProvider(input);

		SaveAsDialog dialog= new SaveAsDialog(getSite().getShell());
		IFile original= ((IFileEditorInput) input).getFile();
		dialog.setOriginalFile(original);
		dialog.create();

		if (provider.isDeleted(input) && original != null) {
			dialog.setErrorMessage(null);
			dialog.setMessage("File "+input.getName()+" is deleted", IMessageProvider.WARNING);
		}

		IProgressMonitor progressMonitor = getEditorSite().getActionBars().getStatusLineManager().getProgressMonitor();
		if (dialog.open() == Window.CANCEL) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}

		IPath filePath= dialog.getResult();
		if (filePath == null) {
			if (progressMonitor != null)
				progressMonitor.setCanceled(true);
			return;
		}

		IFile file= ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
		IFileEditorInput newInput= new FileEditorInput(file);
		
		setInput(newInput);
		setPartName(newInput.getName());
		doSave(progressMonitor);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	protected boolean saveIfNeededBefore(String action) {
		if (isDirty()) {
			MessageDialog dialog = new MessageDialog(getSite().getShell(), action, null, "The model needs to be saved before proceeding with this action. Is that OK?", MessageDialog.CONFIRM, new String[]{"OK", "Cancel"}, 0);
			if (dialog.open() == 0) {
				doSave(null);
				return true;
			}
			return false;
		}
		return true;
	}
	
	public void reload() {
		if (saveIfNeededBefore("Reload Model")) {
			DTACore.unloadModel(getFile());
			setInput(getFileEditorInput());
			DTACore.notifyListeners(getFile());
		}
	}
	
	@Override
	public boolean isDirty() {
		return getCommandStack().isDirty();
	}
	
	public IGotoMarker getGotoAdapter() {
		return new IGotoMarker() {
			public void gotoMarker(IMarker marker) {
				try {
					if (DTA.MARKER.equals(marker.getType())) {
						String uri = marker.getAttribute(IMarker.LOCATION, null);
						if (uri != null) {
							Resource r = model.getResource(uri);
							if (model.containsResource(r))
								setSelectedElement(r, PAGE_MODEL);
						}
					}
				} catch (CoreException e) {
					Activator.getDefault().log("Error reading DTA marker", e);
				}
			}
		};
	}	

	@Override
	public void commandStackChanged(EventObject event) {
		firePropertyChange(IEditorPart.PROP_DIRTY); 
		updateActions(stackActions);
	}

	@Override
	public void stackChanged(CommandStackEvent event) {
		if (event.isPostChangeEvent()) {
			new UIJob("Refesh DATA Model") {
				public IStatus runInUIThread(IProgressMonitor monitor) {
					refresh();
					return Status.OK_STATUS;						
				}
			}.schedule();
		}
	}

	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();
		try {
			delta.accept(this);
		} catch (CoreException e) { 
			Activator.getDefault().log("Error handling resource change event", e);;
		}
	}

	@Override
	public boolean visit(IResourceDelta delta) throws CoreException {
		if (!delta.getResource().equals(getFile()))
			return true;
		if (delta.getKind() == IResourceDelta.REMOVED) {
			Display display = getSite().getShell().getDisplay();
			if ((IResourceDelta.MOVED_TO & delta.getFlags()) == 0) {//An open, saved file gets deleted
				display.asyncExec(new Runnable() {
					public void run() {
						closeEditor(true);
					}
				});
			} else { // else if it was moved or renamed
				final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getMovedToPath());
				display.asyncExec(new Runnable() {
					public void run() {
						setInput(new FileEditorInput(newFile));
					}
				});
			}
		} else if (delta.getKind() == IResourceDelta.CHANGED) {
			if ((delta.getFlags() & IResourceDelta.MARKERS) == 0) {
				final IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(delta.getFullPath());
				Display display = getSite().getShell().getDisplay();
				display.asyncExec(new Runnable() {
					public void run() {
						setInput(new FileEditorInput(newFile));
					}
				});
			} else {
				new UIJob("Refesh DATA Model") {
					public IStatus runInUIThread(IProgressMonitor monitor) {
						refresh();
						return Status.OK_STATUS;						
					}
				}.schedule();
			}
		}
		return false;
	}

	private void closeEditor(boolean save) {
		getSite().getPage().closeEditor(DTAEditor.this, save);
	}
	
}

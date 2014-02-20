package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.MouseWheelHandler;
import org.eclipse.gef.MouseWheelZoomHandler;
import org.eclipse.gef.SnapToGeometry;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.JasperImages;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTALabelProvider;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.rdf.model.Resource;

public class DTADiagramViewer extends DTAViewer {

	protected GraphicalViewer viewer;
	protected ScalableFreeformRootEditPart root;
	protected OntResource element;
	protected boolean newInput;
	
	private ISelectionChangedListener listener = new ISelectionChangedListener() {
		public void selectionChanged(SelectionChangedEvent event) {
			EditPart editpart = (EditPart) ((StructuredSelection)event.getSelection()).getFirstElement();
			Object model = editpart.getModel();
			if (model instanceof Resource)
				setSelection(new StructuredSelection((Resource)model), false);
		}
	};
	
	public DTADiagramViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}

	protected void addActionsToToolBar(IToolBarManager manager) {
		super.addActionsToToolBar(manager);

		root = new ScalableFreeformRootEditPart();
		root.getZoomManager().setZoom(1);

		IAction action;
		
		action = new Action("Copy to clipboard") { //$NON-NLS-1$
			public void run() {
				final LayerManager layerManager = (LayerManager) viewer.getEditPartRegistry().get(LayerManager.ID);
				final IFigure printableLayer = layerManager.getLayer(LayerConstants.PRINTABLE_LAYERS);
				final Rectangle printableLayerBounds = printableLayer.getBounds();

				final Image img = new Image(Display.getDefault(), printableLayerBounds.width, printableLayerBounds.height);
				final GC imageGC = new GC(img);
				final SWTGraphics swtGraphics = new SWTGraphics(imageGC);
				swtGraphics.translate(printableLayerBounds.getLocation().negate());

				printableLayer.paint(swtGraphics);
				
				Clipboard clipboard = new Clipboard(getEditor().getSite().getShell().getDisplay());
				ImageTransfer imageTransfer = ImageTransfer.getInstance();
				clipboard.setContents(new Object[]{img.getImageData()}, new Transfer[]{imageTransfer});
				
				imageGC.dispose(); 
				img.dispose();			}
		};
		action.setToolTipText("Copy to clipboard");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.CAMERA));
		manager.appendToGroup("Viewer", action);

		action = new ZoomInAction(root.getZoomManager());
		manager.appendToGroup("Viewer", action);
		
		action = new ZoomOutAction(root.getZoomManager());
		manager.appendToGroup("Viewer", action);

	}

	@Override
	public Resource getSelectedElement() {
		return element;
	}

	@Override
	protected void setSelectedElement(Resource element) {
		this.element = element.as(OntResource.class);
		getEditor().markLocation();
	}

	@Override
	protected Resource getTargetElement(Object element) {
		if (element == getInput() || element == DTA.DTAs || element == DTA.Types || element == DTA.Properties)
			return getInput().listOntologies().next();
		return (Resource)element;
	}

	@Override
	public void setInput(Object input) {
		newInput = true;
		super.setInput(input);
	}
	
	@Override
	public void refresh() {
		if (viewer==null)
			createGraphicalViewer(getControl().getBody());

		if (newInput) {
			viewer.setContents(getInput());
			newInput = false;
		} else {		
			viewer.removeSelectionChangedListener(listener);
			EditPart contents = viewer.getContents();
			viewer.setContents(null);
			viewer.setContents(contents);
			viewer.addSelectionChangedListener(listener);
		}

		revealSelectedElement();

		getControl().getBody().layout(true, true);
		if (getEditor().equals(getEditor().getSite().getPage().getActivePart()))
			getControl().setFocus();
	}

	@Override
	protected void revealSelectedElement() {
		EditPart editpart = (EditPart) viewer.getEditPartRegistry().get(element);
		if (editpart != null) {
			viewer.setSelection(new StructuredSelection(editpart));
			viewer.reveal(editpart);
		}
	}

	protected void createGraphicalViewer(Composite parent) {
		DefaultEditDomain editDomain = new DefaultEditDomain(getEditor());
		editDomain.setCommandStack(getEditor().getCommandStack());

		parent.setLayout(new FillLayout());
		
		viewer = new ScrollingGraphicalViewer();
		viewer.createControl(parent);
		viewer.getControl().setBackground(ColorConstants.listBackground);
		viewer.setRootEditPart(root);
		viewer.setEditDomain(editDomain);
		viewer.addSelectionChangedListener(listener);
		viewer.setProperty("LabelProvider", new DTALabelProvider());
		viewer.setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, null);
		viewer.setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, null);
		viewer.setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, false);
		viewer.setProperty(SnapToGeometry.PROPERTY_SNAP_ENABLED, false);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, false);
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, false);
		viewer.setProperty(MouseWheelHandler.KeyGenerator.getKey(SWT.MOD1), MouseWheelZoomHandler.SINGLETON);
	}
	
}

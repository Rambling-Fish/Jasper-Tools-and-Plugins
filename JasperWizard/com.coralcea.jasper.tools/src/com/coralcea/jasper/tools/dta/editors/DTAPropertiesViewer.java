package com.coralcea.jasper.tools.dta.editors;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.Section;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.Images;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.AddPropertyCommand;
import com.coralcea.jasper.tools.dta.commands.ChangeCardinalityCommand;
import com.coralcea.jasper.tools.dta.commands.ChangeImportLoadCommand;
import com.coralcea.jasper.tools.dta.commands.DeleteResourceCommand;
import com.coralcea.jasper.tools.dta.commands.RefinePropertyTypeCommand;
import com.coralcea.jasper.tools.dta.commands.RefreshViewerCommand;
import com.coralcea.jasper.tools.dta.commands.RemovePropertyCommand;
import com.coralcea.jasper.tools.dta.commands.RenameResourceCommand;
import com.coralcea.jasper.tools.dta.commands.SetPropertyCommand;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAPropertiesViewer extends DTAViewer {

	private Control content;
	private OntResource element;
	
	public DTAPropertiesViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}
	
	protected void setupActions(IToolBarManager manager) {
		super.setupActions(manager);

		IAction action;
		
		action = new Action("Find item") {
			public void run() {
				OntModel model = element.getOntModel();
				Set<Resource> elements = new HashSet<Resource>();
				for(ResIterator i = model.listSubjects(); i.hasNext();) {
					Resource r = i.next();
					if (DTAUtilities.isOntology(r) || 
						DTAUtilities.isClass(r) || 
						DTAUtilities.isProperty(r) || 
						DTAUtilities.isDTA(r) || 
						DTAUtilities.isOperation(r) || 
						DTAUtilities.isRequest(r))
						elements.add(r);
				}
				Resource r = DTASelectionDialog.run("Find Item", model, elements, false, false);
				if (r != null)
					setSelection(new StructuredSelection(r), true);
			}
		};
		action.setToolTipText("Find item");
		action.setImageDescriptor(Activator.getImageDescriptor(Images.FIND));
		manager.appendToGroup("Viewer", action);

		action = new Action("Up") {
			public void run() {
				Resource r = getEditor().getSelectedElement();
				if (DTAUtilities.isOntology(r))
					r = getEditor().getModel().listOntologies().next();
				else if (DTAUtilities.isOperation(r) || DTAUtilities.isRequest(r))
					r = DTAUtilities.getDTA(r);
				else
					r = DTAUtilities.getDefiningOntology(r.as(OntResource.class));
				setSelection(new StructuredSelection(r), true);
			}
		};
		action.setToolTipText("Navigate up");
		action.setImageDescriptor(Activator.getImageDescriptor(Images.UPARROW));
		manager.appendToGroup("Viewer", action);

		action = new Action("Home") {
			public void run() {
				Resource r = getEditor().getModel().listOntologies().next();
				setSelection(new StructuredSelection(r), true);
			}
		};
		action.setToolTipText("Go home");
		action.setImageDescriptor(Activator.getImageDescriptor(Images.HOME));
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
	protected void revealSelectedElement() {
		refresh();
	}

	@Override
	public void refresh() {
		if (content != null)
			content.dispose();
		content = createContent(getControl().getBody());
		getControl().getBody().layout(true, true);
		if (getEditor().equals(getEditor().getSite().getPage().getActivePart()))
			getControl().setFocus();
	}

	protected Control createContent(Composite parent) {
		parent.setLayout(new GridLayout());
		ScrolledComposite scrollpane = createScrolledComposite(parent, 1);
		Composite body = createComposite(scrollpane, 1);
		scrollpane.setContent(body);

		// this can happen during Undo/Redo of create/delete elements or model unload
		if (!getInput().contains(element, RDF.type))
			element = getInput().listOntologies().next();

		// Show problem marker if any
		try {
			IMarker[] markers = DTAUtilities.getMarkers(getEditor().getFile(), element);
			if (markers.length>0) {
				int kind = IMessageProvider.NONE;
				int markerKind = (int) markers[0].getAttribute(IMarker.SEVERITY);
				if (markerKind == IMarker.SEVERITY_ERROR)
					kind = IMessageProvider.ERROR;
				else if (markerKind == IMarker.SEVERITY_WARNING)
					kind = IMessageProvider.WARNING;
				else if (markerKind == IMarker.SEVERITY_INFO)
					kind = IMessageProvider.INFORMATION;
				getControl().setMessage((String) markers[0].getAttribute(IMarker.MESSAGE), kind);
			} else
				getControl().setMessage(null);
		} catch (CoreException e) {
			Activator.getDefault().log("Error reading markers", e);
		}
		
		String imported = DTAUtilities.isDefinedByBase(element) ? "" : "Imported ";
		if (DTAUtilities.isOntology(element)) {
			Ontology ont = element.asOntology();
			getControl().setText(imported+(DTAUtilities.isLibrary(ont) ? "Library" : "Model"));
			buildOntologyContents(body, ont);
		} else if (DTAUtilities.isClass(element)) {
			getControl().setText(imported+"Type");
			buildClassContents(body, element.asClass());
		} else if (DTAUtilities.isProperty(element)) {
			getControl().setText(imported+"Property");
			buildPropertyContents(body, element.asProperty());
		} else if (DTAUtilities.isDTA(element)) {
			getControl().setText(imported+"DTA");
			buildDTAContents(body, element);
		} else if (DTAUtilities.isOperation(element)) {
			getControl().setText(imported+"Operation");
			buildOperationContents(body, element);
		} else if (DTAUtilities.isRequest(element)) {
			getControl().setText(imported+"Request");
			buildRequestContents(body, element);
		}
		
		scrollpane.setMinSize(body.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scrollpane;
	}

	private void buildOntologyContents(Composite parent, Ontology element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);
		if (!DTAUtilities.isLibrary(element))
			addBasePackage(group, element);

		addImportsSection(parent, element);
		addDTAsSection(parent, element);
		addClassesSection(parent, element);
		addPropertiesSection(parent, element);
	}

	private void buildClassContents(Composite parent, OntClass element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);
		
		addSuperclassesSection(parent, element);
		addSubclassesSection(parent, element);
		addClassPropertiesSection(parent, element);
	}
	
	private void buildPropertyContents(Composite parent, OntProperty element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);
		addType(group, element);

		addEquivalentPropertiesSection(parent, element);
		addSuperPropertiesSection(parent, element);
		addSubPropertiesSection(parent, element);
		addPropertyClassesSection(parent, element);
	}

	private void buildDTAContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);

		addOperationsSection(parent, element);
		addRequestsSection(parent, element);
	}

	private void buildOperationContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);
		addDestination(group, element);
		addKind(group, element);
		addInput(group, element);
		if (DTAUtilities.isGet(element))
			addOutput(group, element);

		addParametersSection(parent, element);
	}

	private void buildRequestContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		addDescription(group, element);
		addRule(group, element);
		addKind(group, element);
		addInput(group, element);
		if (DTAUtilities.isGet(element))
			addOutput(group, element);
		
		addParametersSection(parent, element);
	}

	private void addURI(final Composite group, final OntResource element) {
        createLabel(group, "Name:", "The name of this "+DTAUtilities.getKind(element));
        
        Composite subGroup = createComposite(group, 3, 0);
        
        createText(subGroup, SWT.READ_ONLY, element.getURI());
        
        Button rename = createButton(subGroup, SWT.PUSH, "Rename");
        rename.setEnabled(DTAUtilities.isDefinedByBase(element));
        rename.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String newURI = promptForURI(group.getShell(), "Rename", DTAUtilities.getKind(element), element.getURI());
				if (newURI!=null) {
					Resource newElement = element.getOntModel().getResource(newURI);
					if (!element.getOntModel().contains(newElement, RDF.type)) {
						CompoundCommand cc = new CompoundCommand();
						if (DTAUtilities.isOperation(element)) {
							Resource dta = DTAUtilities.getDTA(element);
							Literal dest = element.getOntModel().createTypedLiteral(DTAUtilities.getUniqueDestination(dta, newElement));
							cc.add(new SetPropertyCommand(element, DTA.destination, dest));
						}
						cc.add(new RenameResourceCommand(element, newElement));
						cc.add(new RefreshViewerCommand(DTAPropertiesViewer.this, newElement));
						getEditor().executeCommand(cc, true);
					}
				}
			}
        });
        
        final Button browse = createButton(subGroup, SWT.PUSH, "Browse");
       	browse.setEnabled(!(element instanceof Ontology));
        browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				getEditor().setSelectedElement(element, DTAEditor.PAGE_BROWSE);					
			}
        });
	}
	
	private void addDescription(Composite group, final OntResource element) {
        createLabel(group, "Description:", "The description of this "+DTAUtilities.getKind(element));
        
        Text text = createTextArea(group, SWT.NONE, DTAUtilities.getLabel(element.getPropertyValue(RDFS.comment)));
		setupEditableText(text, RDFS.comment, "", null, DTAUtilities.isDefinedByBase(element));
	}

	private void addRule(Composite group, final OntResource element) {
        createLabel(group, "Rule:", "The rule of this "+DTAUtilities.getKind(element));
        
        Text text = createTextArea(group, SWT.NONE, DTAUtilities.getLabel(element.getPropertyValue(DTA.rule)));
		setupEditableText(text, DTA.rule, null, XSDDatatype.XSDstring, DTAUtilities.isDefinedByBase(element));
	}

	private void addBasePackage(Composite group, final OntResource element) {
        createLabel(group, "Base Package:", "The base package for code generation");
        
        final Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.basepackage)));
		setupEditableText(text, DTA.basepackage, null, XSDDatatype.XSDstring, true);
    	final ControlDecoration decor = new ControlDecoration(text, SWT.TOP);
		final Pattern pattern = Pattern.compile("^[a-zA-Z_\\$][\\w\\$]*(?:\\.[a-zA-Z_\\$][\\w\\$]*)*$");
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				String basePackage = text.getText();
				String msg = null;
				if (basePackage.length()>0 && !pattern.matcher(basePackage).matches())
					msg = "Invalid package name";
	            if (msg != null) {
	            	decor.setDescriptionText(msg);
	            	decor.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
	            	decor.show();
	            } else
	              	decor.hide();
			}
		});
	}

	private void addDestination(Composite group, OntResource element) {
        createLabel(group, "Destination:", "The destination of this operation");
        
        Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.destination)));
		setupEditableText(text, DTA.destination, null, XSDDatatype.XSDstring, false);
		text.setEnabled(false);
	}

	private void addKind(Composite group, final OntResource element) {
		createLabel(group, "Kind:", "The kind of this operation/request");
		
		List<Resource> kinds = new ArrayList<Resource>(3);
		kinds.add(DTA.Get);
		kinds.add(DTA.Post);
		if (DTAUtilities.isRequest(element))
			kinds.add(DTA.Publish);
		
        ComboViewer combo = createCombo(group, SWT.READ_ONLY, new FragmentProvider(), 
        		kinds.toArray(), element.getPropertyResourceValue(DTA.kind));
        combo.getCombo().setEnabled(DTAUtilities.isDefinedByBase(element));
		combo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.size() > 0) {
					Resource value = (Resource) selection.getFirstElement();
					CompoundCommand cc = new CompoundCommand();
					cc.add(new SetPropertyCommand(element, DTA.kind, value));
					if (!DTA.Get.equals(value) && element.hasProperty(DTA.output))
						cc.add(new SetPropertyCommand(element, DTA.output, null));
					if (!DTA.Get.equals(value) && element.hasProperty(DTA.outputRestriction))
						cc.add(new DeleteResourceCommand(element.getPropertyResourceValue(DTA.outputRestriction)));
					getEditor().executeCommand(cc, true);
				}
			}
		});
	}

	private void addType(final Composite group, final OntProperty element) {
        createLabel(group, "Type:", "The type of this property");
        
        Composite linkGroup = createComposite(group, 2);
        
        createLink(linkGroup, element.getRange());
        if (DTAUtilities.isDefinedByBase(element)) {
	        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
	        button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					CompoundCommand cc = new CompoundCommand();
					Collection<Resource> types = DTAUtilities.listAvailableTypes(element.getOntModel());
					Resource type = DTASelectionDialog.run("Select Type", element.getOntModel(), types, true, true);
					if (DTA.New.equals(type)) {
						String value = promptForNewURI(group.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
						if (value != null) {
							type = element.getOntModel().getResource(value);
							if (!element.getOntModel().contains(type, RDF.type))
								cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
						} else
							return;
					}
					if (type != null && !type.equals(element.getRange())) {
						cc.add(new SetPropertyCommand(element, RDFS.range, DTA.None.equals(type) ? null : type));
						if (element.isDatatypeProperty() && !DTA.None.equals(type) && !DTAUtilities.isDatatype(type)) {
							cc.add(new RemovePropertyCommand(element, RDF.type, OWL.DatatypeProperty));
							cc.add(new AddPropertyCommand(element, RDF.type, OWL.ObjectProperty));
						}
						else if (element.isObjectProperty() && (DTA.None.equals(type) || DTAUtilities.isDatatype(type))) {
							cc.add(new RemovePropertyCommand(element, RDF.type, OWL.ObjectProperty));
							cc.add(new AddPropertyCommand(element, RDF.type, OWL.DatatypeProperty));
						}
						getEditor().executeCommand(cc, true);
					}
				}
			});
        }
	}

	private void addInput(final Composite group, final OntResource element) {
        createLabel(group, "Input:", "The input of this operation/request");
        
        Composite linkGroup = createComposite(group, 2, 0);
        
        final OntClass input = DTAUtilities.getPropertyResourceValue(element, DTA.input, OntClass.class);

        Link link = createLink(linkGroup, input);
        link.setEnabled(input!=null);
        
        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
        button.setEnabled(DTAUtilities.isDefinedByBase(element));
        button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Input Type", element.getOntModel(), types, true, true);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(group.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					} else
						return;
				}
				if (type != null && !type.equals(input)) {
					cc.add(new SetPropertyCommand(element, DTA.input, DTA.None.equals(type) ? null : type));
					for (Resource r : DTAUtilities.listObjects(element, DTA.inputRestriction, Resource.class))
						cc.add(new DeleteResourceCommand(r));
					getEditor().executeCommand(cc, true);
				}
			}
		});
	}

	private void addOutput(final Composite group, final OntResource element) {
        createLabel(group, "Output:", "The output of this opration/request");
        
        Composite linkGroup = createComposite(group, 3, 0);

        final OntProperty output = DTAUtilities.getPropertyResourceValue(element, DTA.output, OntProperty.class);
        
        Link link = createLink(linkGroup, output);
        link.setEnabled(output!=null);
        
		Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
        button.setEnabled(DTAUtilities.isDefinedByBase(element));
        button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Output Property", element.getOntModel(), properties, true, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(group.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntProperty.class), RDF.type, OWL.DatatypeProperty));
					} else
						return;
				}
				if (property != null && !property.equals(output)) {
					cc.add(new SetPropertyCommand(element, DTA.output, DTA.None.equals(property) ? null : property));
					getEditor().executeCommand(cc, true);
				}
			}
        });

        String initialValue = null; 
        if (output!=null) {
        	Restriction initial = DTAUtilities.getDirectRestriction(element, DTA.outputRestriction, output);
        	initialValue = DTAUtilities.getCardinality(initial);
        }
        ComboViewer combo = createCombo(linkGroup, SWT.DROP_DOWN|SWT.READ_ONLY, null, new String[]{"0..*", "0..1", "1..*", "1..1"}, initialValue);
		combo.getControl().setEnabled(DTAUtilities.isDefinedByBase(element) && output!=null);
		combo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				String value = (String) selection.getFirstElement();
				getEditor().executeCommand(new ChangeCardinalityCommand(element, DTA.outputRestriction, output, value), true);
			}
		});
	}

	private void addImportsSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Imports", "The models imported by this model");
        
		if (DTAUtilities.isDefinedByBase(element)) {
			final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			ToolItem button = createAddButton(toolbar, element.getOntModel(), "Import",  new Listener() {
				public void handleEvent(Event event) {
					CompoundCommand cc = new CompoundCommand();
					List<String> values = DTAImportPolicyDialog.openToSelect(toolbar.getShell(), getEditor());
					for (String value : values) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.hasProperty(OWL.imports, r)) {
							cc.add(new AddPropertyCommand(element, OWL.imports, r));
							cc.add(new ChangeImportLoadCommand(getEditor().getFile(), element.getOntModel(), value, true));
						}
					}
					if (!cc.isEmpty())
						getEditor().executeCommand(cc, true);
				}
			});
			button.setEnabled(DTAUtilities.isDefinedByBase(element));
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(element.listImports())) {
			Link link = createLink(group, i);
			
			if (DTAUtilities.isDefinedByBase(element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand();
						cc.add(new ChangeImportLoadCommand(getEditor().getFile(), element.getOntModel(), i.getURI(), false));
						cc.add(new RemovePropertyCommand(element, OWL.imports, i));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addDTAsSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "DTAs", "The DTAs defined in this model");
        
		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			ToolItem button = createNewButton(toolbar, element.getOntModel(), "DTA",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "DTA", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type))
							getEditor().executeCommand(new SetPropertyCommand(r.as(OntResource.class), RDF.type, DTA.DTA), true);
					}
				}
			});
			if (DTAUtilities.isLibrary(element) || DTAUtilities.listDefinedResources(element, DTA.DTA).hasNext())
				button.setEnabled(false);
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final Resource i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedResources(element, DTA.DTA))) {
			Link link = createLink(group, i);
			
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand();
						Collection<RDFNode> operations = DTAUtilities.listObjects(i, DTA.operation);
						operations.addAll(DTAUtilities.listObjects(i, DTA.request));
						for(RDFNode op : operations)
							cc.add(new DeleteResourceCommand(op.asResource()));
						cc.add(new DeleteResourceCommand(i));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addClassesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Types", "The types defined in this model");

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createNewButton(toolbar, element.getOntModel(), "Type",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Type", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type))
							getEditor().executeCommand(new SetPropertyCommand(r.as(OntResource.class), RDF.type, OWL.Class), true);
					}
				}
			});
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final Resource i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedClasses(element))) {
			Link link = createLink(group, i);
			
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteResourceCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addPropertiesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Properties", "The properties defined in this model");

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createNewButton(toolbar, element.getOntModel(), "Property",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Property", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type))
							getEditor().executeCommand(new SetPropertyCommand(r.as(OntResource.class), RDF.type, OWL.DatatypeProperty), true);
					}
				}
			});
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final Resource i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedProperties(element))) {
			Link link = createLink(group, i);
			
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteResourceCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addOperationsSection(Composite parent, final OntResource element) {
        Section section = createSection(parent, "Operations", "The operations defined by this DTA");

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createNewButton(toolbar, element.getOntModel(), "Operation",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Operation", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type)) {
							Literal dest = element.getOntModel().createTypedLiteral(DTAUtilities.getUniqueDestination(element, r));
							CompoundCommand cc = new CompoundCommand();
							cc.add(new SetPropertyCommand(r.as(OntResource.class), RDF.type, DTA.Operation));
							cc.add(new SetPropertyCommand(r.as(OntResource.class), DTA.destination, dest));
							cc.add(new SetPropertyCommand(r.as(OntResource.class), DTA.kind, DTA.Get));
							cc.add(new AddPropertyCommand(element, DTA.operation, r));
							getEditor().executeCommand(cc, true);
						}
					}
				}
			});
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (RDFNode n : DTAUtilities.sortOnLabel(element.listPropertyValues(DTA.operation))) {
			final OntResource i =  n.as(OntResource.class);
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteResourceCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addRequestsSection(Composite parent, final OntResource element) {
        Section section = createSection(parent, "Requests", "The requests defined by this DTA");

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createNewButton(toolbar, element.getOntModel(), "Request",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Request", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type)) {
							CompoundCommand cc = new CompoundCommand();
							cc.add(new SetPropertyCommand(r.as(OntResource.class), RDF.type, DTA.Request));
							cc.add(new SetPropertyCommand(r.as(OntResource.class), DTA.kind, DTA.Get));
							cc.add(new AddPropertyCommand(r.as(OntResource.class), DTA.expires, getInput().createTypedLiteral(10)));
							cc.add(new AddPropertyCommand(element, DTA.request, r));
							getEditor().executeCommand(cc, true);
						}
					}
				}
			});
		}
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (RDFNode n : DTAUtilities.sortOnLabel(element.listPropertyValues(DTA.request))) {
			final OntResource i =  n.as(OntResource.class);
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteResourceCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addSuperclassesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Super Types", "The super types of this type");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Super Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Super Type", element.getOntModel(), types, false, true);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !type.equals(element) && !element.hasProperty(RDFS.subClassOf, type)) {
					cc.add(new AddPropertyCommand(element, RDFS.subClassOf, type));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(element.listSuperClasses(true))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element, RDFS.subClassOf, i))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(element, RDFS.subClassOf, i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addSubclassesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Sub Types", "The subtypes of this class");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Sub Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Sub Type", element.getOntModel(), types, false, true);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !type.equals(element) && !type.hasProperty(RDFS.subClassOf, element)) {
					cc.add(new AddPropertyCommand(type.as(OntResource.class), RDFS.subClassOf, element));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(element.listSubClasses(true))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(i, RDFS.subClassOf, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(i, RDFS.subClassOf, element), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addClassPropertiesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Defined Properties", "The properties defined for this type");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Property",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Property", element.getOntModel(), properties, false, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !property.hasProperty(RDFS.domain, element)) {
					cc.add(new AddPropertyCommand(property.as(OntResource.class), RDFS.domain, element));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 3);
		section.setClient(group);
		for (final OntProperty i : DTAUtilities.sortOnLabel(DTAUtilities.listDeclaredProperties(element).iterator())) {
			createLink(group, i.as(OntResource.class));
			
			Restriction initial = DTAUtilities.getDirectRestriction(element, DTA.restriction, i);
		    String initialValue = DTAUtilities.getCardinality(initial);
			ComboViewer combo = createCombo(group, SWT.DROP_DOWN|SWT.READ_ONLY, null, new String[]{"0..*", "0..1", "1..*", "1..1"}, initialValue);
			combo.getControl().setLayoutData(new GridData());
			combo.getControl().setEnabled(DTAUtilities.isDefinedByBase(element));
			combo.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					String value = (String) selection.getFirstElement();
					getEditor().executeCommand(new ChangeCardinalityCommand(element, DTA.restriction, i, value), true);
				}
			});
			
			if (DTAUtilities.isDefinedByBase(i, RDFS.domain, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand("Removing Property from Type");
						cc.add(new ChangeCardinalityCommand(element, DTA.restriction, i, "0..*"));
						cc.add(new RemovePropertyCommand(i, RDFS.domain, element));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)combo.getControl().getLayoutData()).horizontalSpan = 2;
		}
	}
	
	private void addPropertyClassesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Defining Types", "The defining types of this property");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Defining Type", element.getOntModel(), types, false, true);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !element.hasDomain(type)) {
					cc.add(new AddPropertyCommand(element, RDFS.domain, type));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(element.listDomain())) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element, RDFS.domain, i))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(element, RDFS.domain, i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addEquivalentPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Equivalent Properties", "The equivalent properties of this property");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Equivalent Property",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Equivalent Property", element.getOntModel(), properties, false, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !property.equals(element) && !element.hasProperty(OWL.equivalentProperty, property) && !property.hasProperty(OWL.equivalentProperty, element)) {
					cc.add(new AddPropertyCommand(element, OWL.equivalentProperty, property));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final Property i : DTAUtilities.sortOnLabel(DTAUtilities.listAllEquivalentProperties(element).iterator())) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element, OWL.equivalentProperty, i) || 
				DTAUtilities.isDefinedByBase(element.getOntModel(), i, OWL.equivalentProperty, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						if (DTAUtilities.isDefinedByBase(element, OWL.equivalentProperty, i))
							getEditor().executeCommand(new RemovePropertyCommand(element, OWL.equivalentProperty, i), true);
						else
							getEditor().executeCommand(new RemovePropertyCommand(i, OWL.equivalentProperty, element), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addSuperPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Super Properties", "The super properties of this property");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Super Property",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Super Property", element.getOntModel(), properties, false, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !property.equals(element) && !element.hasProperty(RDFS.subPropertyOf, property)) {
					cc.add(new AddPropertyCommand(element, RDFS.subPropertyOf, property));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(DTAUtilities.listObjects(element, RDFS.subPropertyOf, OntResource.class).iterator())) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element, RDFS.subPropertyOf, i))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(element, RDFS.subPropertyOf, i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addSubPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Sub Properties", "The sub properties of this class");

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Sub Property",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Sub Property", element.getOntModel(), properties, false, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !property.equals(element) && !property.hasProperty(RDFS.subPropertyOf, element)) {
					cc.add(new AddPropertyCommand(property.as(OntResource.class), RDFS.subPropertyOf, element));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		Composite group = createComposite(section, 2);
		section.setClient(group);
		for (final OntResource i : DTAUtilities.sortOnLabel(DTAUtilities.listSubjects(RDFS.subPropertyOf, element, OntResource.class).iterator())) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(i, RDFS.subPropertyOf, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(i, RDFS.subPropertyOf, element), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addParametersSection(final Composite parent, final OntResource element) {
        Section section = createSection(parent, "Parameters", "The input properties of this request");
 
		final Composite group = createComposite(section,4);
		section.setClient(group);
		
		Set<OntProperty> parameters = Collections.emptySet();
		OntClass input = DTAUtilities.getPropertyResourceValue(element, DTA.input, OntClass.class);
		if (input!=null)
			parameters = DTAUtilities.listAllProperties(input);
		
		for (final OntProperty p : DTAUtilities.sortOnLabel(parameters.iterator())) {
			createLink(group, p);

			createLabel(group, ":", null);
			
			final Restriction restriction = DTAUtilities.getDirectRestriction(element, DTA.inputRestriction, p);
		    final Resource oldType = (restriction!=null) ? DTAUtilities.getRestrictedType(restriction) : p.getRange();
			Link link = createLink(group, oldType);

			if (DTAUtilities.isRequest(element)) {
				Button button = createButton(group, SWT.ARROW|SWT.DOWN, "");
		        button.setEnabled(DTAUtilities.isDefinedByBase(element));
		        button.addListener(SWT.Selection, new Listener() {
					public void handleEvent(Event event) {
						CompoundCommand cc = new CompoundCommand();
						List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
						Resource type = DTASelectionDialog.run("Select Parameter Type", element.getOntModel(), types, true, true);
						if (DTA.New.equals(type)) {
							String value = promptForNewURI(group.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
							if (value != null) {
								type = element.getOntModel().getResource(value);
								if (!element.getOntModel().contains(type, RDF.type))
									cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
							} else
								return;
						}
						if (type != null && !type.equals(oldType)) {
							cc.add(new RefinePropertyTypeCommand(element, p, type));
							getEditor().executeCommand(cc, true);
						}
					}
				});
			} else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private ToolItem createNewButton(final ToolBar toolbar, final OntModel model, final String type, Listener listener) {
		ToolItem item = createToolItem(toolbar, "New "+type, Activator.getImage(Images.PLUS));
		item.addListener(SWT.Selection, listener);
		return item;
	}

	private ToolItem createAddButton(final ToolBar toolbar, final OntModel model, final String type, Listener listener) {
		ToolItem item = createToolItem(toolbar, "Add "+type, Activator.getImage(Images.PLUS));
		item.addListener(SWT.Selection, listener);
		return item;
	}
	
	private ImageHyperlink createDeleteButton(Composite parent, RDFNode node, HyperlinkAdapter listener) {
		ImageHyperlink link = createImageHyperlink(parent, "Delete "+DTAUtilities.getLabel(node), Activator.getImage(Images.MINUS));
		link.addHyperlinkListener(listener);
		return link;
	}

	private ImageHyperlink createRemoveButton(Composite parent, RDFNode node, HyperlinkAdapter listener) {
		ImageHyperlink link = createImageHyperlink(parent, "Remove "+DTAUtilities.getLabel(node), Activator.getImage(Images.MINUS));
		link.addHyperlinkListener(listener);
		return link;
	}

	private Link createLink(Composite parent, Resource target) {
		String text = (target!= null) ? DTAUtilities.getLabel(target) : "None";
		text = (target!=null) ? "<a>"+text+"</a>" : text;
		String tooltip = (target != null) ? target.getURI() : null;
		return createLink(parent, text, tooltip, target);
	}
	
	private void setupEditableText(final Text text, final Property property, final String lang, final RDFDatatype type, boolean editable) {
		if (!editable) 
			text.setEditable(false);
		else {
			Listener listener = new Listener() {
				public void handleEvent(Event event) {
			        String s = text.getText();
			        RDFNode newValue;
			        if (s == null || s.length()==0)
			        	newValue = null;
			        else if (lang != null)
						newValue = element.getModel().createLiteral(s, lang);
					else
						newValue = element.getModel().createTypedLiteral(s, type);
			        RDFNode oldValue = element.getPropertyValue(property);
					if ((newValue==null && oldValue!= null) || (newValue!= null && !newValue.equals(oldValue))) {
						SetPropertyCommand command = new SetPropertyCommand(element, property, newValue);
						getEditor().executeCommand(command, false);
					}
				}
			};
			text.addListener(SWT.DefaultSelection, listener);
			text.addListener(SWT.FocusOut, listener);
		}
	}

	private String promptForNewURI(Shell shell, String type, String initial) {
		return promptForURI(shell, "New", type, initial);
	}
	
	private String promptForURI(Shell shell, String action, String type, String initial) {
		InputDialog dialog = new InputDialog(shell, action+" "+type, "Enter the name of the "+type+":", initial, new IInputValidator() {
			public String isValid(String uri) {
				return DTAUtilities.isValidURI(uri) ? null : "Invalid Name";
			}
		});
		return dialog.open() == Dialog.OK ? dialog.getValue() : null;
	}
	
	private static class FragmentProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			Resource r = (Resource) element;
			return URI.create(r.getURI()).getFragment();
		}

	}
}

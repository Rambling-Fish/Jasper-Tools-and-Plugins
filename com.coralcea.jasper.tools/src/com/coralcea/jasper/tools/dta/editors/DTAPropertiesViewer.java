package com.coralcea.jasper.tools.dta.editors;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
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
import com.coralcea.jasper.tools.JasperImages;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.AddPropertyCommand;
import com.coralcea.jasper.tools.dta.commands.ChangeImportCommand;
import com.coralcea.jasper.tools.dta.commands.ChangeRestrictionCommand;
import com.coralcea.jasper.tools.dta.commands.DeleteResourceCommand;
import com.coralcea.jasper.tools.dta.commands.RemovePropertyCommand;
import com.coralcea.jasper.tools.dta.commands.RenameResourceCommand;
import com.coralcea.jasper.tools.dta.commands.SetPropertyCommand;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTAPropertiesViewer extends DTAViewer {

	private Control content;
	private OntResource element;
	
	public DTAPropertiesViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
	}
	
	protected void addActionsToToolBar(IToolBarManager manager) {
		super.addActionsToToolBar(manager);

		IAction action;
		
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
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.UPARROW));
		manager.appendToGroup("Viewer", action);

		action = new Action("Home") {
			public void run() {
				Resource r = getEditor().getModel().listOntologies().next();
				setSelection(new StructuredSelection(r), true);
			}
		};
		action.setToolTipText("Go home");
		action.setImageDescriptor(Activator.getImageDescriptor(JasperImages.HOME));
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
		
		addOperationInputsSection(parent, element);
		addOperationOutputsSection(parent, element);
	}

	private void buildRequestContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		addURI(group, element);
		addDescription(group, element);
		addDestination(group, element);
		addKind(group, element);
		
		addOperationInputsSection(parent, element);
		addOperationOutputsSection(parent, element);
	}

	private void addURI(final Composite group, final OntResource element) {
        createLabel(group, "URI:", "The URI of the resource");
        Composite subGroup = createComposite(group, 3);
        GridLayout layout = (GridLayout) subGroup.getLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        createText(subGroup, SWT.READ_ONLY, element.getURI());
        Button rename = createButton(subGroup, SWT.PUSH, "Rename");
        rename.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String newURI = promptForNewURI(group.getShell(), "Resource", element.getURI());
				getEditor().executeCommand(new RenameResourceCommand(element, newURI), true);
			}
        });
        Button browse = createButton(subGroup, SWT.PUSH, "Browse");
        if (element instanceof Ontology || DTAUtilities.isDTA(element))
        	browse.setEnabled(false);
        browse.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				getEditor().setSelectedElement(element, DTAEditor.PAGE_BROWSE);
			}
        });
	}
	
	private void addDescription(Composite group, final OntResource element) {
        createLabel(group, "Description:", "The description of the resource");
        Text text = createTextArea(group, SWT.WRAP|SWT.V_SCROLL, DTAUtilities.getLabel(element.getPropertyValue(RDFS.comment)));
		setupEditableText(text, RDFS.comment, "", DTAUtilities.isDefinedByBase(element));
	}

	private void addBasePackage(Composite group, final OntResource element) {
        createLabel(group, "Base Package:", "The base package for code generation");
        Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.basepackage)));
		setupEditableText(text, DTA.basepackage, null, true);
	}

	private void addDestination(Composite group, OntResource element) {
        createLabel(group, "Destination:", "The destination of the operation");
        Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.destination)));
		setupEditableText(text, DTA.destination, null, false);
		text.setEnabled(false);
	}

	private void addKind(Composite group, final OntResource element) {
		createLabel(group, "Kind:", "The kind of the operation/request");
        ComboViewer combo = createCombo(group, SWT.READ_ONLY, new FragmentProvider(), 
        	new Resource[]{DTA.Get, DTA.Post}, element.getPropertyResourceValue(DTA.kind));
		if (!DTAUtilities.isDefinedByBase(element))
	        combo.getCombo().setEnabled(false);
		combo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				if (selection.size() > 0) {
					Resource value = (Resource) selection.getFirstElement();
					CompoundCommand cc = new CompoundCommand();
					cc.add(new SetPropertyCommand(element, DTA.kind, value));
					getEditor().executeCommand(cc, true);
				}
			}
		});
	}

	private void addType(final Composite group, final OntProperty element) {
        createLabel(group, "Type:", "The type of the property");
        Composite linkGroup = createComposite(group, 2);
        createLink(linkGroup, element.getRange());
        if (DTAUtilities.isDefinedByBase(element)) {
	        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
	        button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					CompoundCommand cc = new CompoundCommand();
					Collection<Resource> types = DTAUtilities.getAvailableTypes(element.getOntModel());
					Resource type = DTASelectionDialog.run("Select Type", element.getOntModel(), types, true);
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
						if (element.isDatatypeProperty() && !DTA.None.equals(type) && !XSD.getURI().equals(type.getNameSpace()))
							cc.add(new SetPropertyCommand(element, RDF.type, OWL.ObjectProperty));
						else if (element.isObjectProperty() && (DTA.None.equals(type) || XSD.getURI().equals(type.getNameSpace())))
							cc.add(new SetPropertyCommand(element, RDF.type, OWL.DatatypeProperty));
						getEditor().executeCommand(cc, true);
					}
				}
			});
        }
	}

	private void addImportsSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Imports", "The models imported by this model");
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Import",  new Listener() {
				public void handleEvent(Event event) {
					CompoundCommand cc = new CompoundCommand();
					List<String> values = DTAImportPolicyDialog.openToSelect(toolbar.getShell(), getEditor());
					for (String value : values) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.hasProperty(OWL.imports, r)) {
							cc.add(new AddPropertyCommand(element, OWL.imports, r));
							cc.add(new ChangeImportCommand(element.getOntModel(), value, true));
						}
					}
					if (!cc.isEmpty())
						getEditor().executeCommand(cc, true);
				}
			});
		}
		
		for (final OntResource i : DTAUtilities.sortOnLabel(element.listImports())) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand();
						cc.add(new ChangeImportCommand(element.getOntModel(), i.getURI(), false));
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
		Composite group = createComposite(section, 2);
		section.setClient(group);
		
		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			ToolItem button = createAddButton(toolbar, element.getOntModel(), "DTA",  new Listener() {
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
		
		for (final Resource i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedResources(element, DTA.DTA))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createDeleteButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand();
						Set<RDFNode> operations = DTAUtilities.listObjects(i, DTA.operation);
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Type",  new Listener() {
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Property",  new Listener() {
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Operation",  new Listener() {
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Request",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Request", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type)) {
							Literal dest = element.getModel().createTypedLiteral(DTA.GLOBAL_QUEUE);
							CompoundCommand cc = new CompoundCommand();
							cc.add(new SetPropertyCommand(r.as(OntResource.class), RDF.type, DTA.Request));
							cc.add(new SetPropertyCommand(r.as(OntResource.class), DTA.destination, dest));
							cc.add(new SetPropertyCommand(r.as(OntResource.class), DTA.kind, DTA.Get));
							cc.add(new AddPropertyCommand(element, DTA.request, r));
							getEditor().executeCommand(cc, true);
						}
					}
				}
			});
		}
		
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Super Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Super Type", element.getOntModel(), types, false);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !element.hasProperty(RDFS.subClassOf, type)) {
					cc.add(new AddPropertyCommand(element, RDFS.subClassOf, type));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		for (final OntClass i : DTAUtilities.sortOnLabel(element.listSuperClasses(true))) {
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
		Composite group = createComposite(section, 2);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Sub Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntClass> types = DTAUtilities.listClasses(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Sub Type", element.getOntModel(), types, false);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !type.hasProperty(RDFS.subClassOf, element)) {
					cc.add(new AddPropertyCommand(type.as(OntResource.class), RDFS.subClassOf, element));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		for (final OntClass i : DTAUtilities.sortOnLabel(element.listSubClasses(true))) {
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
        Section section = createSection(parent, "Properties", "The properties defined for this type");
		Composite group = createComposite(section, 3);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Property",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Property", element.getOntModel(), properties, false);
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
		
		for (final OntProperty i : DTAUtilities.sortOnLabel(DTAUtilities.getDeclaredProperties(element).iterator())) {
			Link link = createLink(group, i.as(OntResource.class));
			Restriction initial = DTAUtilities.getRestriction(element, DTA.restriction, i);
		    String initialValue = DTAUtilities.getRestrictionValue(initial);
			ComboViewer combo = createCombo(group, SWT.DROP_DOWN, null, new String[]{"0..*", "0..1", "1..*", "1..1"}, initialValue);
			combo.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					String value = (String) selection.getFirstElement();
					getEditor().executeCommand(new ChangeRestrictionCommand(element, DTA.restriction, i, value), true);
				}
			});
			if (initial!=null && !DTAUtilities.isDefinedByBase(initial))
				combo.getControl().setEnabled(false);
			if (DTAUtilities.isDefinedByBase(i, RDFS.domain, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand("Removing Property from Type");
						cc.add(new ChangeRestrictionCommand(element, DTA.restriction, i, "0..*"));
						cc.add(new RemovePropertyCommand(i, RDFS.domain, element));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}
	
	private void addOperationInputsSection(final Composite parent, final OntResource element) {
        Section section = createSection(parent, "Inputs", "The inputs of this operation/request");
		Composite group = createComposite(section, 3);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Input",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Property", element.getOntModel(), properties, false);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !DTAUtilities.listObjects(element, DTA.input).contains(property)) {
					cc.add(new AddPropertyCommand(element, DTA.input, property));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		for (RDFNode i : DTAUtilities.sortOnLabel(DTAUtilities.listObjects(element, DTA.input).iterator())) {
			final OntProperty p = i.as(OntProperty.class);
			Link link = createLink(group, i.as(OntResource.class));
			Restriction initial = DTAUtilities.getRestriction(element, DTA.inputRestriction, p);
		    String initialValue = DTAUtilities.getRestrictionValue(initial);
			ComboViewer combo = createCombo(group, SWT.DROP_DOWN, null, new String[]{"0..*", "0..1", "1..*", "1..1"}, initialValue);
			combo.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					String value = (String) selection.getFirstElement();
					getEditor().executeCommand(new ChangeRestrictionCommand(element, DTA.inputRestriction, p, value), true);
				}
			});
			if (initial!=null && !DTAUtilities.isDefinedByBase(initial))
				combo.getControl().setEnabled(false);
			if (DTAUtilities.isDefinedByBase(element, DTA.input, p))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand("Removing Operation Input");
						cc.add(new ChangeRestrictionCommand(element, DTA.inputRestriction, p, "0..*"));
						cc.add(new RemovePropertyCommand(element, DTA.input, p));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addOperationOutputsSection(final Composite parent, final OntResource element) {
        Section section = createSection(parent, "Outputs", "The outputs of this operation/request");
		Composite group = createComposite(section, 3);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		ToolItem button = createAddButton(toolbar, element.getOntModel(), "Output",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Property", element.getOntModel(), properties, false);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(parent.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntResource.class), RDF.type, OWL.DatatypeProperty));
					}
				}
				if (property != null && !DTAUtilities.listObjects(element, DTA.output).contains(property)) {
					cc.add(new AddPropertyCommand(element, DTA.output, property));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		if (!DTAUtilities.listObjects(element, DTA.output).isEmpty())
			button.setEnabled(false);
		
		for (RDFNode i : DTAUtilities.sortOnLabel(DTAUtilities.listObjects(element, DTA.output).iterator())) {
			final OntProperty p = i.as(OntProperty.class);
			Link link = createLink(group, i.as(OntResource.class));
			Restriction initial = DTAUtilities.getRestriction(element, DTA.outputRestriction, p);
		    String initialValue = DTAUtilities.getRestrictionValue(initial);
			ComboViewer combo = createCombo(group, SWT.DROP_DOWN, null, new String[]{"0..*", "0..1", "1..*", "1..1"}, initialValue);
			combo.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					String value = (String) selection.getFirstElement();
					getEditor().executeCommand(new ChangeRestrictionCommand(element, DTA.outputRestriction, p, value), true);
				}
			});
			if (initial!=null && !DTAUtilities.isDefinedByBase(initial))
				combo.getControl().setEnabled(false);
			if (DTAUtilities.isDefinedByBase(element, DTA.output, p))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand("Removing Operation Output");
						cc.add(new ChangeRestrictionCommand(element, DTA.outputRestriction, p, "0..*"));
						cc.add(new RemovePropertyCommand(element, DTA.output, p));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private ToolItem createAddButton(final ToolBar toolbar, final OntModel model, final String type, Listener listener) {
		ToolItem item = createToolItem(toolbar, "Add "+type, Activator.getImage(JasperImages.PLUS));
		item.addListener(SWT.Selection, listener);
		return item;
	}
	
	private ImageHyperlink createDeleteButton(Composite parent, RDFNode node, HyperlinkAdapter listener) {
		ImageHyperlink link = createImageHyperlink(parent, "Delete "+DTAUtilities.getLabel(node), Activator.getImage(JasperImages.MINUS));
		link.addHyperlinkListener(listener);
		return link;
	}

	private ImageHyperlink createRemoveButton(Composite parent, RDFNode node, HyperlinkAdapter listener) {
		ImageHyperlink link = createImageHyperlink(parent, "Remove "+DTAUtilities.getLabel(node), Activator.getImage(JasperImages.MINUS));
		link.addHyperlinkListener(listener);
		return link;
	}

	private Link createLink(Composite parent, Resource target) {
		String text = (target!= null) ? DTAUtilities.getLabel(target) : "<Not Set>";
		text = (target!=null && !target.getNameSpace().equals(XSD.getURI())) ? "<a>"+text+"</a>" : text;
		String tooltip = (target != null) ? target.getURI() : null;
		return createLink(parent, text, tooltip, target);
	}
	
	private void setupEditableText(final Text text, final Property property, final String lang, boolean editable) {
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
						newValue = element.getModel().createTypedLiteral(s);
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
		InputDialog dialog = new InputDialog(shell, "Add "+type, "Enter the URI of the new "+type+":", initial, new IInputValidator() {
			public String isValid(String uri) {
				return DTAUtilities.isValidURI(uri) ? null : "Invalid URI";
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

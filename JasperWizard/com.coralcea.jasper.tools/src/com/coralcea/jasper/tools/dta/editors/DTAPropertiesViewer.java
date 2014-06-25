package com.coralcea.jasper.tools.dta.editors;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.commands.Command;
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
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
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
import com.coralcea.jasper.tools.dta.DTACardinality;
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
		} else {
			buildUnrecognizedContent(body, element);
		}
		
		scrollpane.setMinSize(body.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scrollpane;
	}

	private void buildUnrecognizedContent(Composite parent, Resource element) {
		Label label = createLabel(parent, "<" + element.getURI()+"> : unrecognized element type", "Could not recognize the element's type");
		label.setForeground(ColorConstants.red);
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

		addPropertyTypesSection(parent, element);
		addPropertyClassesSection(parent, element);
		addEquivalentPropertiesSection(parent, element);
		addSuperPropertiesSection(parent, element);
		addSubPropertiesSection(parent, element);
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
		if (!DTAUtilities.isPublish(element))
			addDestination(group, element);
		addDescription(group, element);
		addKind(group, element);
		if (DTAUtilities.isGet(element))
			addData(group, element);
		addParameter(group, element);

		addParametersSection(parent, element);
	}

	private void buildRequestContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		
		addURI(group, element);
		if (DTAUtilities.isSubscribe(element))
			addDestination(group, element);
		addDescription(group, element);
		addRule(group, element);
		addKind(group, element);
		if (!DTAUtilities.isPost(element))
			addData(group, element);
		if (!DTAUtilities.isSubscribe(element))
			addParameter(group, element);
		
		if (!DTAUtilities.isSubscribe(element))
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
        createLabel(group, "Destination:", "The destination of this "+DTAUtilities.getKind(element));
        
        Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.destination)));
		setupEditableText(text, DTA.destination, null, XSDDatatype.XSDstring, false);
		text.setEnabled(false);
	}

	private void addKind(Composite group, final OntResource element) {
		createLabel(group, "Kind:", "The kind of this "+DTAUtilities.getKind(element));
		
		List<Resource> kinds = new ArrayList<Resource>(3);
		kinds.add(DTA.Get);
		kinds.add(DTA.Post);
		if (DTAUtilities.isRequest(element))
			kinds.add(DTA.Subscribe);
		else
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
					if (!DTA.Get.equals(value) && !DTA.Subscribe.equals(value)) {
						if (element.hasProperty(DTA.data))
							cc.add(new SetPropertyCommand(element, DTA.data, null));
						if (element.hasProperty(DTA.dataRestriction))
							cc.add(new DeleteResourceCommand(element.getPropertyResourceValue(DTA.dataRestriction)));
					}
					if (DTAUtilities.isOperation(element)) {
						if (DTAUtilities.isPublish(element) && !DTA.Publish.equals(value)) {
							OntResource dta = DTAUtilities.listSubjects(DTA.operation, element).iterator().next().as(OntResource.class);
							Literal dest = element.getOntModel().createTypedLiteral(DTAUtilities.getUniqueDestination(dta, element));
							cc.add(new SetPropertyCommand(element, DTA.destination, dest));
						} else if (!DTAUtilities.isPublish(element) && DTA.Publish.equals(value))
							cc.add(new SetPropertyCommand(element, DTA.destination, null));
					} else {
						if (!DTAUtilities.isSubscribe(element) && DTA.Subscribe.equals(value)) {
							OntResource dta = DTAUtilities.listSubjects(DTA.request, element).iterator().next().as(OntResource.class);
							Literal dest = element.getOntModel().createTypedLiteral(DTAUtilities.getUniqueDestination(dta, element));
							cc.add(new SetPropertyCommand(element, DTA.destination, dest));
						} else if (DTAUtilities.isSubscribe(element) && !DTA.Subscribe.equals(value))
							cc.add(new SetPropertyCommand(element, DTA.destination, null));
					}
					getEditor().executeCommand(cc, true);
				}
			}
		});
	}

	private void addParameter(final Composite group, final OntResource element) {
        createLabel(group, "Parameter:", "The parameter of this "+DTAUtilities.getKind(element));
        
        Composite linkGroup = createComposite(group, 2, 0);
        
        final OntClass parameter = DTAUtilities.getPropertyResourceValue(element, DTA.parameter, OntClass.class);

        Link link = createLink(linkGroup, parameter);
        link.setEnabled(parameter!=null);
        
        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
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
				if (type != null && !type.equals(parameter)) {
					cc.add(new SetPropertyCommand(element, DTA.parameter, DTA.None.equals(type) ? null : type));
					for (Resource r : DTAUtilities.listObjects(element, DTA.parameterRestriction, Resource.class))
						cc.add(new DeleteResourceCommand(r));
					getEditor().executeCommand(cc, true);
				}
			}
		});
	}

	private void addData(final Composite group, final OntResource element) {
        createLabel(group, "Data:", "The returned data of this "+DTAUtilities.getKind(element));
        
        Composite linkGroup = createComposite(group, 3, 0);

        final OntProperty data = DTAUtilities.getPropertyResourceValue(element, DTA.data, OntProperty.class);
        
        Link link = createLink(linkGroup, data);
        link.setEnabled(data!=null);
        
		Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
        button.setEnabled(DTAUtilities.isDefinedByBase(element));
        button.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				List<OntProperty> properties = DTAUtilities.listProperties(element.getOntModel());
				Resource property = DTASelectionDialog.run("Select Data Property", element.getOntModel(), properties, true, true);
				if (DTA.New.equals(property)) {
					String value = promptForNewURI(group.getShell(), "Property", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						property = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(property, RDF.type))
							cc.add(new SetPropertyCommand(property.as(OntProperty.class), RDF.type, OWL.DatatypeProperty));
					} else
						return;
				}
				if (property != null && !property.equals(data)) {
					cc.add(new SetPropertyCommand(element, DTA.data, DTA.None.equals(property) ? null : property));
					getEditor().executeCommand(cc, true);
				}
			}
        });

        String initialValue = null; 
        if (data!=null) {
        	Restriction initial = DTAUtilities.getDirectRestriction(element, DTA.dataRestriction, data);
        	initialValue = DTAUtilities.getCardinality(initial);
        }
        ComboViewer combo = createCombo(linkGroup, SWT.READ_ONLY, null, DTACardinality.toArray(), initialValue);
		combo.getControl().setEnabled(DTAUtilities.isDefinedByBase(element) && data!=null);
		combo.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				String value = (String) selection.getFirstElement();
				getEditor().executeCommand(new ChangeCardinalityCommand(element, DTA.dataRestriction, data, value), true);
			}
		});
	}

	private void addImportsSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Imports", "The models imported by this "+DTAUtilities.getKind(element));

        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
        
		if (DTAUtilities.isDefinedByBase(element)) {
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

		addItemsToSection(section, element.listImports(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					CompoundCommand cc = new CompoundCommand();
					cc.add(new ChangeImportLoadCommand(getEditor().getFile(), element.getOntModel(), r.getURI(), false));
					cc.add(new RemovePropertyCommand(element, OWL.imports, r));
					return cc;
				}
				return null;
			}
		});
	}

	private void addDTAsSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "DTAs", "The DTAs defined in this "+DTAUtilities.getKind(element));
        
        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);

		if (DTAUtilities.isDefinedByBase(element)) {
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
		
		addItemsToSection(section, DTAUtilities.listDefinedResources(element, DTA.DTA), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					CompoundCommand cc = new CompoundCommand();
					for(RDFNode op : DTAUtilities.listObjects(r, DTA.operation))
						cc.add(new DeleteResourceCommand(op.asResource()));
					for(RDFNode op : DTAUtilities.listObjects(r, DTA.request))
						cc.add(new DeleteResourceCommand(op.asResource()));
					cc.add(new DeleteResourceCommand(r));
					return cc;
				}
				return null;
			}
		});
	}

	private void addClassesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Types", "The types defined in this "+DTAUtilities.getKind(element));

        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);

		if (DTAUtilities.isDefinedByBase(element)) {
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
		
		addItemsToSection(section, DTAUtilities.listDefinedClasses(element), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					return new DeleteResourceCommand(r);
				}
				return null;
			}
		});
	}

	private void addPropertiesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Properties", "The properties defined in this "+DTAUtilities.getKind(element));

        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);

		if (DTAUtilities.isDefinedByBase(element)) {
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
		
		addItemsToSection(section, DTAUtilities.listDefinedProperties(element), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					return new DeleteResourceCommand(r);
				}
				return null;
			}
		});
	}

	private void addOperationsSection(Composite parent, final OntResource element) {
        Section section = createSection(parent, "Operations", "The operations defined by this "+DTAUtilities.getKind(element));

        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);

		if (DTAUtilities.isDefinedByBase(element)) {
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
		
		addItemsToSection(section, element.listPropertyValues(DTA.operation), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					return new DeleteResourceCommand(r);
				}
				return null;
			}
		});
	}

	private void addRequestsSection(Composite parent, final OntResource element) {
        Section section = createSection(parent, "Requests", "The requests defined by this "+DTAUtilities.getKind(element));

        final ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);

		if (DTAUtilities.isDefinedByBase(element)) {
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
		
		addItemsToSection(section, element.listPropertyValues(DTA.request), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element)) {
					return new DeleteResourceCommand(r);
				}
				return null;
			}
		});
	}

	private void addSuperclassesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Super Types", "The super types of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, element.listSuperClasses(true), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element, RDFS.subClassOf, r)) {
					return new RemovePropertyCommand(element, RDFS.subClassOf, r);
				}
				return null;
			}
		});
	}

	private void addSubclassesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Sub Types", "The subtypes of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, element.listSubClasses(true), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(r, RDFS.subClassOf, element)) {
					return new RemovePropertyCommand(r, RDFS.subClassOf, element);
				}
				return null;
			}
		});
	}

	private void addClassPropertiesSection(final Composite parent, final OntClass element) {
        Section section = createSection(parent, "Defined Properties", "The properties defined for this "+DTAUtilities.getKind(element));

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
			ComboViewer combo = createCombo(group, SWT.READ_ONLY, null, DTACardinality.toArray(), initialValue);
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
						cc.add(new ChangeCardinalityCommand(element, DTA.restriction, i, ""));
						cc.add(new RemovePropertyCommand(i, RDFS.domain, element));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)combo.getControl().getLayoutData()).horizontalSpan = 2;
		}
	}
	
	private void addPropertyTypesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Types", "The types of this "+DTAUtilities.getKind(element));

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		
		createAddButton(toolbar, element.getOntModel(), "Type",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				Collection<Resource> types = DTAUtilities.listAvailableTypes(element.getOntModel());
				Resource type = DTASelectionDialog.run("Select Type", element.getOntModel(), types, false, true);
				if (DTA.New.equals(type)) {
					String value = promptForNewURI(parent.getShell(), "Type", element.getOntModel().getNsPrefixURI(""));
					if (value != null) {
						type = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(type, RDF.type))
							cc.add(new SetPropertyCommand(type.as(OntResource.class), RDF.type, OWL.Class));
					}
				}
				if (type != null && !element.hasRange(type)) {
					cc.add(new AddPropertyCommand(element, RDFS.range, type));
					getEditor().executeCommand(cc, true);
				}
			}
		});
		
		addItemsToSection(section, element.listRange(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element, RDFS.range, r)) {
					return new RemovePropertyCommand(element, RDFS.range, r);
				}
				return null;
			}
		});
	}

	private void addPropertyClassesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Defining Types", "The defining types of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, element.listDomain(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element, RDFS.domain, r)) {
					return new RemovePropertyCommand(element, RDFS.domain, r);
				}
				return null;
			}
		});
	}

	private void addEquivalentPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Equivalent Properties", "The equivalent properties of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, DTAUtilities.listAllEquivalentProperties(element).iterator(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element, OWL.equivalentProperty, r))
					return new RemovePropertyCommand(element, OWL.equivalentProperty, r);
				if (DTAUtilities.isDefinedByBase(element.getOntModel(), r, OWL.equivalentProperty, element))
					return new RemovePropertyCommand(r, OWL.equivalentProperty, element);
				return null;
			}
		});
	}

	private void addSuperPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Super Properties", "The super properties of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, DTAUtilities.listObjects(element, RDFS.subPropertyOf, OntResource.class).iterator(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(element, RDFS.subPropertyOf, r)) {
					return new RemovePropertyCommand(element, RDFS.subPropertyOf, r);
				}
				return null;
			}
		});
	}

	private void addSubPropertiesSection(final Composite parent, final OntProperty element) {
        Section section = createSection(parent, "Sub Properties", "The sub properties of this "+DTAUtilities.getKind(element));

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
		
		addItemsToSection(section, DTAUtilities.listSubjects(RDFS.subPropertyOf, element, OntResource.class).iterator(), new IDeleteCommandProvider() {
			public Command getDeleteCommand(OntResource r) {
				if (DTAUtilities.isDefinedByBase(r, RDFS.subPropertyOf, element)) {
					return new RemovePropertyCommand(r, RDFS.subPropertyOf, element);
				}
				return null;
			}
		});
	}

	private void addParametersSection(final Composite parent, final OntResource element) {
        Section section = createSection(parent, "Parameter Details", "The details of the parameter of this operation/request");
        section.setExpanded(false);
 
		final Composite group = createComposite(section,4);
		section.setClient(group);
		
		Set<OntProperty> parameters = Collections.emptySet();
		OntClass parameter = DTAUtilities.getPropertyResourceValue(element, DTA.parameter, OntClass.class);
		if (parameter!=null)
			parameters = DTAUtilities.listAllProperties(parameter);
		
		for (final OntProperty p : DTAUtilities.sortOnLabel(parameters.iterator())) {
			createLink(group, p);

			createLabel(group, ":", null);
			
			final Restriction restriction = DTAUtilities.getDirectRestriction(element, DTA.parameterRestriction, p);
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

	private ToolItem createRemoveButton(final ToolBar toolbar, final OntModel model, final String type, Listener listener) {
		ToolItem item = createToolItem(toolbar, 0, "Remove "+type, Activator.getImage(Images.MINUS));
		item.addListener(SWT.Selection, listener);
		return item;
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
	
	private void addItemsToSection(Section section, Iterator<? extends RDFNode> items, final IDeleteCommandProvider provider) {
		final TableViewer viewer = new TableViewer(section, SWT.VIRTUAL | SWT.MULTI | SWT.V_SCROLL | SWT.HIDE_SELECTION | SWT.BORDER);
		
		Table table = viewer.getTable();
		TableLayout tableLayout = new TableLayout();
		table.setLayout(tableLayout);		
		section.setClient(table);
		//table.setHeaderVisible(true);
		table.setLinesVisible(true);		
		//GridData layoutData = new GridData(100, 100);
		//table.setLayoutData(layoutData);
		
 		viewer.setContentProvider(new IStructuredContentProvider() {
			@SuppressWarnings("unchecked")
			public Object[] getElements(Object inputElement) {
				return DTAUtilities.sortOnLabel((Iterator<? extends RDFNode>)inputElement).toArray();
			}
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			public void dispose() {
			}
		});

 		viewer.addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
            	Object element = ((StructuredSelection)event.getSelection()).getFirstElement();
				setSelection(new StructuredSelection(element), true);
            }
 		});
 		
		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.NONE);
		tableLayout.addColumnData(new ColumnWeightData(1));
		nameColumn.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				Resource r = (Resource) element;
				return DTAUtilities.getLabel(r);
			}
			public Image getImage(Object element) {
				//Resource r = (Resource) element;
				return null;//DTAUtilities.getImage(r);
			}
			public String getToolTipText(Object element) {
				Resource r = (Resource) element;
				return r.getURI();
			}
			public boolean useNativeToolTip(Object object) {
				return true;
			}
		});
		
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		viewer.setInput(items);
		
		ToolBar toolbar = (ToolBar) section.getChildren()[2];
		
		createRemoveButton(toolbar, element.getOntModel(), "",  new Listener() {
			public void handleEvent(Event event) {
				CompoundCommand cc = new CompoundCommand();
				StructuredSelection selection = (StructuredSelection) viewer.getSelection();
				for (Object e : selection.toList())
					cc.add(provider.getDeleteCommand(((RDFNode)e).as(OntResource.class)));
				getEditor().executeCommand(cc, true);
			}
		});
	}
	
	private interface IDeleteCommandProvider {
		Command getDeleteCommand(OntResource r);
	}
	
	private static class FragmentProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			Resource r = (Resource) element;
			return URI.create(r.getURI()).getFragment();
		}
	}
}

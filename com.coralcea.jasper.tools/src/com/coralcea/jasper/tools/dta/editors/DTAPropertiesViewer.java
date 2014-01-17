package com.coralcea.jasper.tools.dta.editors;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
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
import com.coralcea.jasper.tools.dta.commands.DeleteCommand;
import com.coralcea.jasper.tools.dta.commands.RemovePropertyCommand;
import com.coralcea.jasper.tools.dta.commands.SetPropertyCommand;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Ontology;
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
		if (element == getInput() || element == DTA.DTAs || element == DTA.Classes || element == DTA.Properties)
			return getInput().listOntologies().next();
		return (Resource)element;
	}

	@Override
	public void refresh() {
		if (content != null)
			content.dispose();
		content = createContent();
		getControl().getBody().layout(true, true);
		getControl().reflow(true);
		getEditor().getSite().getPage().getActivePart();
		if (getEditor().equals(getEditor().getSite().getPage().getActivePart()))
			getControl().setFocus();
	}

	protected Control createContent() {
		Composite composite = createComposite(getControl().getBody(), 1);
		composite.setLayoutData(null);

		// this can happen during Undo/Redo of create/delete elements
		if (element.getRDFType() == null)
			element = getInput().listOntologies().next();

		String imported = DTAUtilities.isDefinedByBase(element) ? "" : "Imported ";

		if (DTAUtilities.isOntology(element)) {
			getControl().setText(imported+"Model");
			buildOntologyContents(composite, element.asOntology());
		} else if (DTAUtilities.isClass(element)) {
			getControl().setText(imported+"Class");
			buildClassContents(composite, element.asClass());
		} else if (DTAUtilities.isProperty(element)) {
			getControl().setText(imported+"Property");
			buildPropertyContents(composite, element.asProperty());
		} else if (DTAUtilities.isDTA(element)) {
			getControl().setText(imported+"DTA");
			buildDTAContents(composite, element);
		} else if (DTAUtilities.isOperation(element)) {
			getControl().setText(imported+"Operation");
			buildOperationContents(composite, element);
		}
		
		return composite;
	}

	private void buildOntologyContents(Composite parent, Ontology element) {
		Composite group = createComposite(parent, 2);
		addURI(group, element);
		addDescription(group, element);

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
	}

	private void buildOperationContents(Composite parent, OntResource element) {
		Composite group = createComposite(parent, 2);
		addURI(group, element);
		addDescription(group, element);
		addDestination(group, element);
		addKind(group, element);
		addInputType(group, element);
		addOutoutType(group, element);
	}

	private void addURI(Composite group, OntResource element) {
        createLabel(group, "URI:");
        createText(group, SWT.READ_ONLY, element.getURI());
	}
	
	private void addDescription(Composite group, final OntResource element) {
        createLabel(group, "Description:");
        Text text = createTextArea(group, SWT.WRAP|SWT.V_SCROLL, DTAUtilities.getLabel(element.getPropertyValue(RDFS.comment)));
		setupEditableText(text, RDFS.comment, "", DTAUtilities.isDefinedByBase(element));
	}

	private void addDestination(Composite group, OntResource element) {
        createLabel(group, "Destination:");
        Text text = createText(group, 0, DTAUtilities.getLabel(element.getPropertyValue(DTA.destination)));
		setupEditableText(text, DTA.destination, null, DTAUtilities.isDefinedByBase(element));
		if (DTA.Request.equals(element.getRDFType()))
			text.setEnabled(false);
	}

	private void addKind(Composite group, final OntResource element) {
		createLabel(group, "Kind:");
        ComboViewer combo = createCombo(group, SWT.READ_ONLY, new FragmentProvider(), 
        	new Resource[]{DTA.Provide, DTA.Request}, element.getRDFType());
		if (!DTAUtilities.isDefinedByBase(element))
	        combo.getCombo().setEnabled(false);
		else {
			combo.addSelectionChangedListener(new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					IStructuredSelection selection = (IStructuredSelection) event.getSelection();
					if (selection.size() > 0) {
						Resource value = (Resource) selection.getFirstElement();
						if (!value.equals(element.getRDFType())) {
							CompoundCommand cc = new CompoundCommand();
							cc.add(new SetPropertyCommand(element, RDF.type, value));
							if (DTA.Request.equals(value)) {
								Literal queue = element.getModel().createTypedLiteral(DTA.GLOBAL_QUEUE);
								cc.add(new SetPropertyCommand(element, DTA.destination, queue));
							}
							if (DTA.Request.equals(element.getRDFType()))
								cc.add(new SetPropertyCommand(element, DTA.destination, null));
							getEditor().executeCommand(cc, true);
						}
					}
				}
			});
		}
	}

	private void addType(Composite group, final OntProperty element) {
        createLabel(group, "Type:");
        Composite linkGroup = createComposite(group, 2);
        createLink(linkGroup, element.getRange());
        if (DTAUtilities.isDefinedByBase(element)) {
	        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
	        button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Collection<Resource> types = DTAUtilities.getAvailableTypes(element.getOntModel());
					Resource type = DTASelectionDialog.run("Select Type", element.getOntModel(), types, true);
					if (type != null && !type.equals(element.getRange())) {
						CompoundCommand cc = new CompoundCommand();
						cc.add(new SetPropertyCommand(element, RDFS.range, DTA.None.equals(type) ? null : type));
						if (element.isDatatypeProperty() && !DTA.None.equals(type) && !XSD.getURI().equals(type.getNameSpace()))
							cc.add(new SetPropertyCommand(element, RDF.type, OWL.ObjectProperty));
						else if (element.isObjectProperty() && !DTA.None.equals(type) && XSD.getURI().equals(type.getNameSpace()))
							cc.add(new SetPropertyCommand(element, RDF.type, OWL.DatatypeProperty));
						getEditor().executeCommand(cc, true);
					}
				}
			});
        }
	}

	private void addInputType(Composite group, final OntResource element) {
        createLabel(group, "Input Type:");
        Composite linkGroup = createComposite(group, 2);
        createLink(linkGroup, element.getPropertyResourceValue(DTA.input));
        if (DTAUtilities.isDefinedByBase(element)) {
	        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
	        button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					List<OntClass> types = element.getOntModel().listClasses().toList();
					Resource type = DTASelectionDialog.run("Select Input Type", element.getOntModel(), types, true);
					if (type != null && !type.equals(element.getPropertyResourceValue(DTA.input))) {
						SetPropertyCommand c = new SetPropertyCommand(element, DTA.input, DTA.None.equals(type) ? null : type);
						getEditor().executeCommand(c, true);
					}
				}
			});
        }
	}

	private void addOutoutType(Composite group, final OntResource element) {
        createLabel(group, "Output Type:");
        Composite linkGroup = createComposite(group, 2);
        createLink(linkGroup, element.getPropertyResourceValue(DTA.output));
        if (DTAUtilities.isDefinedByBase(element)) {
	        Button button = createButton(linkGroup, SWT.ARROW|SWT.DOWN, "");
	        button.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					List<OntClass> types = element.getOntModel().listClasses().toList();
					Resource type = DTASelectionDialog.run("Select Output Type", element.getOntModel(), types, true);
					if (type != null && !type.equals(element.getPropertyResourceValue(DTA.output))) {
						SetPropertyCommand c = new SetPropertyCommand(element, DTA.output, DTA.None.equals(type) ? null : type);
						getEditor().executeCommand(c, true);
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
					String value = promptForNewURI(toolbar.getShell(), "Import", "");
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.hasProperty(OWL.imports, r)) {
							CompoundCommand cc = new CompoundCommand();
							cc.add(new AddPropertyCommand(element, OWL.imports, r));
							cc.add(new ChangeImportCommand(element.getOntModel(), value, true));
							getEditor().executeCommand(cc, true);
						}
					}
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
        Section section = createSection(parent, "DTAs", "The DTAs defined by this model");
		Composite group = createComposite(section, 2);
		section.setClient(group);
		
		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "DTA",  new Listener() {
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
		}
		
		for (final Individual i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedIndividuals(element, DTA.DTA))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						CompoundCommand cc = new CompoundCommand();
						for(RDFNode op : DTAUtilities.getObjects((Resource)i, DTA.operation))
							cc.add(new DeleteCommand(op.as(OntResource.class)));
						cc.add(new DeleteCommand(i));
						getEditor().executeCommand(cc, true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addClassesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Classes", "The classes defined by this model");
		Composite group = createComposite(section, 2);
		section.setClient(group);

		if (DTAUtilities.isDefinedByBase(element)) {
	        final ToolBar toolbar = createToolBar(section, 0);
			section.setTextClient(toolbar);
			createAddButton(toolbar, element.getOntModel(), "Class",  new Listener() {
				public void handleEvent(Event event) {
					String baseNs = element.getOntModel().getNsPrefixURI("");
					String value = promptForNewURI(toolbar.getShell(), "Class", baseNs);
					if (value != null) {
						Resource r = element.getOntModel().getResource(value);
						if (!element.getOntModel().contains(r, RDF.type))
							getEditor().executeCommand(new SetPropertyCommand(r.as(OntResource.class), RDF.type, OWL.Class), true);
					}
				}
			});
		}
		
		for (final OntClass i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedClasses(element))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addPropertiesSection(Composite parent, final Ontology element) {
        Section section = createSection(parent, "Properties", "The properties defined by this model");
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
		
		for (final OntProperty i : DTAUtilities.sortOnLabel(DTAUtilities.listDefinedProperties(element))) {
			Link link = createLink(group, i);
			if (DTAUtilities.isDefinedByBase(element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteCommand(i), true);
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
							CompoundCommand cc = new CompoundCommand();
							cc.add(new SetPropertyCommand(r.as(OntResource.class), RDF.type, DTA.Provide));
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
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new DeleteCommand(i), true);
					}
				});
			else
				((GridData)link.getLayoutData()).horizontalSpan = 2;
		}
	}

	private void addSuperclassesSection(Composite parent, final OntClass element) {
        Section section = createSection(parent, "Super Classes", "The super classes of this class");
		Composite group = createComposite(section, 2);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Super Class",  new Listener() {
			public void handleEvent(Event event) {
				List<OntClass> types = element.getOntModel().listClasses().toList();
				OntClass type = DTASelectionDialog.run("Select Super Class", element.getOntModel(), types, false);
				if (type != null && !element.hasProperty(RDFS.subClassOf, type))
					getEditor().executeCommand(new AddPropertyCommand(element, RDFS.subClassOf, type), true);
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

	private void addSubclassesSection(Composite parent, final OntClass element) {
        Section section = createSection(parent, "Sub Classes", "The subclasses of this class");
		Composite group = createComposite(section, 2);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Sub Class",  new Listener() {
			public void handleEvent(Event event) {
				List<OntClass> types = element.getOntModel().listClasses().toList();
				OntClass type = DTASelectionDialog.run("Select Sub Class", element.getOntModel(), types, false);
				if (type != null && !type.hasProperty(RDFS.subClassOf, element))
					getEditor().executeCommand(new AddPropertyCommand(type, RDFS.subClassOf, element), true);
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

	private void addClassPropertiesSection(Composite parent, final OntClass element) {
        Section section = createSection(parent, "Properties", "The properties defined for this class");
		Composite group = createComposite(section, 2);
		section.setClient(group);

        ToolBar toolbar = createToolBar(section, 0);
		section.setTextClient(toolbar);
		createAddButton(toolbar, element.getOntModel(), "Property",  new Listener() {
			public void handleEvent(Event event) {
				List<OntProperty> properties = element.getOntModel().listAllOntProperties().toList();
				OntProperty property = DTASelectionDialog.run("Select Property", element.getOntModel(), properties, false);
				if (property != null && !property.hasProperty(RDFS.domain, element))
					getEditor().executeCommand(new AddPropertyCommand(property, RDFS.domain, element), true);
			}
		});
		
		for (final OntProperty i : DTAUtilities.sortOnLabel(DTAUtilities.getDeclaredProperties(element).iterator())) {
			Link link = createLink(group, i.as(OntResource.class));
			if (DTAUtilities.isDefinedByBase(i, RDFS.domain, element))
				createRemoveButton(group, i, new HyperlinkAdapter() {
					public void linkActivated(HyperlinkEvent e) {
						getEditor().executeCommand(new RemovePropertyCommand(i, RDFS.domain, element), true);
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

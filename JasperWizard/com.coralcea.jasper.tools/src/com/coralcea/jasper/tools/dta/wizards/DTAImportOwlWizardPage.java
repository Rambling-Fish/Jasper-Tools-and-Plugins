package com.coralcea.jasper.tools.dta.wizards;

import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.util.XMLChar;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.ui.statushandlers.StatusManager;

import com.coralcea.jasper.tools.Activator;
import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTACore;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.UnionClass;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.ResourceUtils;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.DCTerms;
import com.hp.hpl.jena.vocabulary.DC_10;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class DTAImportOwlWizardPage extends WizardPage {

	public static final String MULE_NATURE = "org.mule.tooling.core.muleNature";
	
	private ComboViewer projectName;
	private Text ontologyPath;
	private Text modelName;
	private Button loadButton;
	private OntModel loadedModel;
	private IStructuredSelection selection;
	
	/**
	 * Constructor for DTALibraryWizardPage.
	 * 
	 * @param pageName
	 */
	public DTAImportOwlWizardPage(IStructuredSelection selection) {
		super("wizardPage");
		setTitle("Create a new DTA Library");
		setDescription("Create a new DTA library from an OWL/RDFS ontology.");
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
				return DTAUtilities.hasNature((IProject)element, MULE_NATURE);
			}
		});
		projectName.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				dialogChanged();
			}
		});
		projectName.setInput(ResourcesPlugin.getWorkspace().getRoot().getProjects());
        
		label = new Label(container, SWT.NULL);
		label.setText("Library name:");
		label.setToolTipText("The name of the DTA library");

		modelName = new Text(container, SWT.BORDER | SWT.SINGLE);
		modelName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)modelName.getLayoutData()).horizontalSpan = 3;
		modelName.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		label = new Label(container, SWT.NULL);
		label.setText("Ontology file:");
		label.setToolTipText("The path to the imported OWL/RDFS ontology file");

		ontologyPath = new Text(container, SWT.BORDER | SWT.SINGLE);
		ontologyPath.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		ontologyPath.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				String path = new Path(ontologyPath.getText()).removeFileExtension().lastSegment();
				modelName.setText(path!=null ? path : "");
				loadButton.setEnabled(path!=null);
			}
		});

		Button browseButton = new Button(container, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
				String result = dialog.open();
				if (result.length()>0)
					ontologyPath.setText(result);
			}
		});

		loadButton = new Button(container, SWT.PUSH);
		loadButton.setText("Load");
		loadButton.setEnabled(false);
		loadButton.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				loadModel();
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
				IProject project = ((IResource) obj).getProject();
				if (DTAUtilities.hasNature(project, MULE_NATURE))
					projectName.setSelection(new StructuredSelection(project));
			}
		}
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
		
		if (getOntologyPath().length()>0) {
			if (!DTAUtilities.isValidFile(getOntologyPath())) {
				loadButton.setEnabled(false);
				updateStatus("Invalid ontology path");
				return;
			}
			
			if (getLoadedModel() == null) {
				updateStatus("Ontology must be (re)loaded");
				return;
			}
		}

		IResource model = project.findMember("src/main/app/"+getModelName()+"."+DTA.EXTENSION);
		if (model != null && model.exists()) {
			updateStatus("DTA library already exists for this project");
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

	public String getOntologyPath() {
		return ontologyPath.getText();
	}

	public String getModelName() {
		return modelName.getText();
	}

	public OntModel getLoadedModel() {
		return loadedModel;
	}

	protected void loadModel() {
		try {
			unloadModel();
			loadedModel = importFile(getOntologyPath());
		} catch (Exception e) {
			MultiStatus status = new MultiStatus(Activator.PLUGIN_ID, Status.ERROR, "Problems loading the file", null);
			status.add(new Status(Status.ERROR, Activator.PLUGIN_ID, e.getMessage()));
			StatusManager.getManager().handle(status, StatusManager.BLOCK);
		}
	}

	protected void unloadModel() {
		if (loadedModel!=null) {
			loadedModel.close();
			loadedModel = null;
		}
	}
	
	protected OntModel importFile(String file) throws Exception {
		OntModel model = DTACore.createNewModel();
		model.read(file);
		Model baseModel = model.getBaseModel();
		
		List<Statement> toRemove = new ArrayList<Statement>();
		
		// Make sure there is an ontology
		Resource ontology = null;
		StmtIterator j = baseModel.listStatements(null, RDF.type, OWL.Ontology);
		if (j.hasNext()) {
			ontology = j.next().getSubject();
		} else { // try to infer it
			j = DTAUtilities.listStatementsOfPredicates(baseModel, new Property[] {DCTerms.title, DC.title, DC_10.title});
			while (j.hasNext()) {
				Resource subject = j.next().getSubject();
				if (!subject.hasProperty(RDF.type)) {
					baseModel.add(subject, RDF.type, OWL.Ontology);
					ontology = subject;
					break;
				}
			}
		}
		
		// Make sure the ontology URI is valid
		if (ontology == null)
			throw new Exception(file+" does not contain an ontology");
		else {
			String uri = ontology.getURI();
			if (!XMLChar.isNCName(uri.charAt(uri.length()-1)))
				ResourceUtils.renameResource(ontology, uri.substring(0, uri.length()-1));
		}

		// convert rdfs:subclassOf to dta:restriction for restrictions
		for(StmtIterator i = baseModel.listStatements(null, RDFS.subClassOf, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			if (s.getObject().asResource().canAs(Restriction.class)) {
				baseModel.add(s.getSubject(), DTA.restriction, s.getObject());
				toRemove.add(s);
			}
		}

		// convert rdfs:subclassOf to dta:restriction for restrictions
		for(Statement s : baseModel.listStatements().toList()) {
			RDFNode object = s.getObject();
			if (object.isAnon()) {
				OntResource resource = model.getOntResource(object.asResource());
				if (resource.canAs(UnionClass.class)) {
					UnionClass union = resource.as(UnionClass.class);
					for (ExtendedIterator<?> k = union.listOperands(); k.hasNext();) {
						OntClass c = (OntClass) k.next();
						baseModel.add(s.getSubject(), s.getPredicate(), c);
					}
					toRemove.add(s);
					toRemove.addAll(union.listProperties().toList());
				}
			}
		}

		// convert rdf:Class to owl:Class
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, RDFS.Class); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), s.getPredicate(), OWL.Class);
			toRemove.add(s);
		}

		// convert owl:DeprecatedClass to owl:Class
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, OWL.DeprecatedClass); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), s.getPredicate(), OWL.Class);
		}

		// convert rdf:Property to either owl:DatatypeProperty or owl:ObjectProperty
		for(StmtIterator i = baseModel.listStatements(null, RDF.type, RDF.Property); i.hasNext();) {
			Statement s = i.next();
			Resource range = s.getSubject().getPropertyResourceValue(RDFS.range);
			if (range != null) {
				if (DTAUtilities.isDatatype(range))
					baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
				else
					baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
			} else
				baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
			toRemove.add(s);
		}

		// convert other types of properties to either owl:DatatypeProperty or owl:ObjectProperty
		Resource[] types = new Resource[]{OWL.DeprecatedProperty, OWL.SymmetricProperty, OWL.TransitiveProperty, OWL.FunctionalProperty, OWL.InverseFunctionalProperty};
		for(StmtIterator i = DTAUtilities.listStatementsOfTypes(baseModel, types); i.hasNext();) {
			Statement s = i.next();
			Resource range = s.getSubject().getPropertyResourceValue(RDFS.range);
			if (range != null) {
				if (DTAUtilities.isDatatype(range))
					baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
				else
					baseModel.add(s.getSubject(), RDF.type, OWL.ObjectProperty);
			} else
				baseModel.add(s.getSubject(), RDF.type, OWL.DatatypeProperty);
		}

		// convert dc:description to rdfs:comment
		for(StmtIterator i = DTAUtilities.listStatementsOfPredicates(baseModel, new Property[]{DCTerms.description, DC.description, DC_10.description}); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), RDFS.comment, s.getObject());
			toRemove.add(s);
		}
		
		// convert dc:title to rdfs:label
		for(StmtIterator i = baseModel.listStatements(null, DC.title, (RDFNode)null); i.hasNext();) {
			Statement s = i.next();
			baseModel.add(s.getSubject(), RDFS.label, s.getObject());
			toRemove.add(s);
		}

		baseModel.remove(toRemove);

		return model;
	}

	protected List<Statement> removeResource(Resource resource) {
		List<Statement> statementsToRemove = new ArrayList<Statement>();
		for(StmtIterator i = resource.listProperties(); i.hasNext();) {
			Statement s = i.next();
			statementsToRemove.add(s);
			if (s.getObject().isAnon())
				statementsToRemove.addAll(removeResource(s.getObject().asResource()));
		}
		statementsToRemove.addAll(resource.getModel().listStatements(null, null, resource).toList());
		return statementsToRemove;
	}
}
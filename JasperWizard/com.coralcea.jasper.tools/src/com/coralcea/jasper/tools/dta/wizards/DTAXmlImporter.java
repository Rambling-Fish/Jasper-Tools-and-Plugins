package com.coralcea.jasper.tools.dta.wizards;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xerces.util.XMLChar;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.coralcea.jasper.tools.dta.DTACore;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.vocabulary.XSD;

public class DTAXmlImporter extends DTAImporter {
	
	public static final String NAME = "XML File";
	
	public String getName() {
		return NAME;
	}
	
	public OntModel readFile(String path) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.parse(path);
		
		String defaultNs = document.getBaseURI();
		if (XMLChar.isNCName(defaultNs.charAt(defaultNs.length()-2)))
			defaultNs += '#';

		OntModel model = DTACore.createNewModel();
		model.setNsPrefix("", defaultNs);

		Element root = document.getDocumentElement();
		traverseNode(model, root);
		
		return model;
	}

	private void traverseNode(OntModel model, Node node) {
		if (node.getFirstChild().getNodeType() == Node.TEXT_NODE && node.getFirstChild().getTextContent().trim().length() > 0) {
			DatatypeProperty property = model.createDatatypeProperty(getPropertyURI(model, node));
			property.setRange(XSD.xstring);
			OntClass domainClass = model.getOntClass(getClassURI(model, node.getParentNode()));
			property.setDomain(domainClass);
		} else if (node.getParentNode().getNodeType() != Node.DOCUMENT_NODE){
			ObjectProperty property = model.createObjectProperty(getPropertyURI(model, node));
			OntClass rangeClass = model.createClass(getClassURI(model, node));
			property.setRange(rangeClass);
			OntClass domainClass = model.getOntClass(getClassURI(model, node.getParentNode()));
			property.setDomain(domainClass);
		} else
			model.createClass(getClassURI(model, node));
		
		NodeList list = node.getChildNodes();
		for(int i=0, length=list.getLength() ; i<length ; i++ ){
			Node child = list.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE)
				traverseNode(model, child);
		}
	}
	
	private String getNamespace(OntModel model, Node node) {
		String namespace = node.getNamespaceURI();
		if (namespace == null)
			namespace = model.getNsPrefixURI("");
		else
			model.setNsPrefix(node.getPrefix(), namespace);
		return namespace;
	}
	
	private String getPropertyURI(OntModel model, Node node) {
		String namespace = getNamespace(model, node);
		String name = node.getLocalName();
		return namespace + name.substring(0, 1).toLowerCase() + name.substring(1);
	}

	private String getClassURI(OntModel model, Node node) {
		String namespace = getNamespace(model, node);
		String name = node.getLocalName();
		return namespace + name.substring(0, 1).toUpperCase() + name.substring(1);
	}
}

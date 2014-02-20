package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.coralcea.jasper.tools.dta.DTA;
import com.coralcea.jasper.tools.dta.DTAUtilities;
import com.coralcea.jasper.tools.dta.commands.RemoveNsPrefixCommand;
import com.coralcea.jasper.tools.dta.commands.SetNsPrefixCommand;

public class DTANamespacesViewer extends DTAViewer {

	private Control content;
	
	public DTANamespacesViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		getControl().setText("Namespaces");
	}
	
	@Override
	public void refresh() {
		if (content != null)
			content.dispose();
		content = createContent(getControl().getBody());
		getControl().layout(true, true);
		if (getEditor().getSite().getPage().getActivePart().equals(getEditor()))
			getControl().setFocus();
	}

	protected Control createContent(Composite parent) {
		parent.setLayout(new GridLayout());
		ScrolledComposite scrollpane = createScrolledComposite(parent, 1);
		Composite group = createComposite(scrollpane, 4);
		scrollpane.setContent(group);

		createLabel(group, "Prefix", "The prefix of the namespace");
		Label label = createLabel(group, "URI", "The URI of the namespace");
		GridData data = new GridData();
		data.horizontalSpan = 3;
		label.setLayoutData(data);

		final Map<String,String> nsMap = getInput().getNsPrefixMap();
		final List<Button> buttons = new ArrayList<Button>(nsMap.size()*2+2);

		for (String s : nsMap.keySet()) {
			if (s.equals("owl") || s.equals("rdf") || s.equals("rdfs") || s.equals("xsd") || s.equals(DTA.PREFIX))
				continue;
			
			final Text prefix = createText(group, 0, s);
	        final Text uri = createText(group, 0, nsMap.get(s));
	        final Button editSet = createButton(group, 0, "Edit");
			final Button removeCancel = createButton(group, 0, "Remove");
			
			prefix.setEditable(false);
	        prefix.addModifyListener(new NamespaceModifyListener(prefix, uri, editSet));
			data = (GridData)prefix.getLayoutData();
	        data.grabExcessHorizontalSpace=false;
	        data.widthHint = 80;
			
	        uri.setEditable(false);
	        uri.addModifyListener(new NamespaceModifyListener(prefix, uri, editSet));
			
	        buttons.add(editSet);
	        buttons.add(removeCancel);

	        editSet.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					if (editSet.getText().equals("Edit")) {
						prefix.setEditable(true);
						prefix.setFocus();
						uri.setEditable(true);
				        editSet.setData("prefix", prefix.getText());
				        editSet.setData("uri", uri.getText());
						editSet.setText("Set");
						removeCancel.setText("Cancel");
						for (Button button : buttons)
							if (button != editSet && button != removeCancel)
								button.setEnabled(false);
					} else { // Set
						String newPrefix = prefix.getText();
						String newURI = uri.getText();
						CompoundCommand cc = new CompoundCommand();
						cc.add(new RemoveNsPrefixCommand(getInput(), (String)editSet.getData("prefix")));
						cc.add(new SetNsPrefixCommand(getInput(), newPrefix, newURI));
						getEditor().executeCommand(cc, true);
					}
				}
			});
	        
			removeCancel.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					if (removeCancel.getText().equals("Remove")) {
						getEditor().executeCommand(new RemoveNsPrefixCommand(getInput(), prefix.getText()), true);
					} else { // Cancel
						prefix.setText((String)editSet.getData("prefix"));
						prefix.setEditable(false);
						uri.setText((String)editSet.getData("uri"));
						uri.setEditable(false);
						editSet.setText("Edit");
						removeCancel.setText("Remove");
						for (Button button : buttons)
							if (button != editSet && button != removeCancel)
								button.setEnabled(true);
					}
				}
			});
		}
		
		label = createLabel(group, "Create new:", "Add a new namespace");
		data = new GridData();
		data.horizontalSpan = 4;
		label.setLayoutData(data);
		
		final Text prefix = createText(group, 0, "");
        final Text uri = createText(group, 0, "");
        final Button add = createButton(group, 0, "Add" );
        final Button clear = createButton(group, 0, " Clear   ");
		
        prefix.addModifyListener(new NamespaceModifyListener(prefix, uri, add));
        prefix.setFocus();
		data = (GridData)prefix.getLayoutData();
        data.grabExcessHorizontalSpace=false;
        data.widthHint = 80;
		
        uri.addModifyListener(new NamespaceModifyListener(prefix, uri, add));
		
        buttons.add(add);
        buttons.add(clear);
       
        add.setEnabled(false);
        add.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				String newPrefix = prefix.getText();
				String newURI = uri.getText();
				CompoundCommand cc = new CompoundCommand();
				String oldPrefix = getInput().getNsURIPrefix(newURI);
				if (oldPrefix != null)
					cc.add(new RemoveNsPrefixCommand(getInput(), oldPrefix));
				cc.add(new SetNsPrefixCommand(getInput(), newPrefix, newURI));
				getEditor().executeCommand(cc, true);
			}
		});

        clear.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				prefix.setText("");
				uri.setText("");
			}
		});

		scrollpane.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		return scrollpane;
	}

    private Image getErrorImage() {
    	return FieldDecorationRegistry.getDefault()
        .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR)
        .getImage();
    }
    
    private class NamespaceModifyListener implements ModifyListener {
    	
    	private Text prefix;
    	private Text uri;
    	private Button button;
    	private ControlDecoration decor;
    	
    	public NamespaceModifyListener(Text prefix, Text uri, Button button) {
    		this.prefix = prefix;
    		this.uri = uri;
    		this.button = button;
    	}
    	
		@Override
		public void modifyText(ModifyEvent e) {
			if (decor == null)
		        decor = new ControlDecoration((Text)e.widget, SWT.TOP);
			
			boolean validPrefix = DTAUtilities.isValidPrefix(prefix.getText());
			boolean validURI = DTAUtilities.isValidNsURI(uri.getText());
			
			String msg = null;
			if (e.widget == prefix && !validPrefix)
				msg = "Not a valid prefix";
			else if (e.widget == uri && !validURI)
				msg = "Not a valid namespace URI";
			
            if (msg != null) {
            	decor.setDescriptionText(msg);
            	decor.setImage(getErrorImage());
            	decor.show();
            } else
              	decor.hide();
            
            button.setEnabled(uri.getText().length() > 0 && validPrefix && validURI);
		}
    }
}

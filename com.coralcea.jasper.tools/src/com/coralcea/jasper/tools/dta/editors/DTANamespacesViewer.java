package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
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
	}
	
	@Override
	public void refresh() {
		if (content != null)
			content.dispose();
		content = createContent();
		content.setFocus();
		getControl().getBody().layout(true, true);
		getControl().reflow(true);
	}

	protected Control createContent() {
		final Composite composite = createComposite(getControl().getBody(), 1);
		composite.setLayoutData(null);

		getControl().setText("Namespaces");

		final Composite group = createComposite(composite, 1);
		GridLayout layout = (GridLayout) group.getLayout();
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;

		final Map<String,String> nsMap = getInput().getNsPrefixMap();
		final List<Button> buttons = new ArrayList<Button>(nsMap.size()*2+2);

		for (String s : nsMap.keySet()) {
			if (s.equals("owl") || s.equals("rdf") || s.equals("rdfs") || s.equals("xsd") || s.equals(DTA.PREFIX))
				continue;
			
			final Composite namespace = createComposite(group, 4);
	        layout = (GridLayout) namespace.getLayout();
	        layout.horizontalSpacing = 10;
			
			createLabel(namespace, "Prefix");
			Label label = createLabel(namespace, "URI");
			GridData data = new GridData();
			data.horizontalSpan = 3;
			label.setLayoutData(data);

			final Text prefix = createText(namespace, 0, s);
	        final Text uri = createText(namespace, 0, nsMap.get(s));
	        final Button editSet = createButton(namespace, 0, "Edit");
			final Button removeCancel = createButton(namespace, 0, "Remove");
			
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
						namespace.setBackground(namespace.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
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
						namespace.setBackground(null);
						editSet.setText("Edit");
						removeCancel.setText("Remove");
						for (Button button : buttons)
							if (button != editSet && button != removeCancel)
								button.setEnabled(true);
					}
				}
			});
		}
		
		createLabel(group, "Create new:");
		
		final Composite namespace = createComposite(group, 4);
		layout = (GridLayout) namespace.getLayout();
		layout.horizontalSpacing = 10;
		
		final Text prefix = createText(namespace, 0, "");
        final Text uri = createText(namespace, 0, "");
        final Button add = createButton(namespace, 0, " Add " );
        final Button clear = createButton(namespace, 0, " Clear ");
		
        prefix.addModifyListener(new NamespaceModifyListener(prefix, uri, add));
        prefix.setFocus();
		GridData data = (GridData)prefix.getLayoutData();
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

		return composite;
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

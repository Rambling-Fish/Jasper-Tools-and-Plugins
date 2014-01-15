package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;

import com.coralcea.jasper.tools.dta.DTACore;

public class DTASourceViewer extends DTAViewer {

	private StyledText source;
	
	public DTASourceViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		getControl().setText("Source");
		source = new StyledText(getControl().getBody(), SWT.NONE);
		source.setEditable(false);
	}
	
	@Override
	public void refresh() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DTACore.writeModel(getInput(), out);
		source.setText(out.toString());
		getControl().reflow(true);
	}
}

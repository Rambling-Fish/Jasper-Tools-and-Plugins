package com.coralcea.jasper.tools.dta.editors;

import java.io.ByteArrayOutputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

import com.coralcea.jasper.tools.dta.DTACore;

public class DTASourceViewer extends DTAViewer {

	private StyledText source;
	
	public DTASourceViewer(Composite parent, DTAEditor editor) {
		super(parent, editor);
		getControl().setText("Source");
		getControl().getBody().setLayout(new FillLayout());
		source = new StyledText(getControl().getBody(), SWT.V_SCROLL|SWT.H_SCROLL);
		source.setAlwaysShowScrollBars(false);
		source.setEditable(false);
	}
	
	@Override
	public void refresh() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DTACore.writeModel(getInput(), out);
		source.setText(out.toString());
	}
}

package com.coralcea.jasper.tools.dta.commands;

import org.eclipse.gef.EditPart;

public class RefreshEditPartCommand extends DTACommand {

	private EditPart editpart;
	
	public RefreshEditPartCommand(EditPart editpart) {
		this.editpart = editpart;
	}
	
	@Override
	public void prepare() {
		editpart.refresh();
	}
}

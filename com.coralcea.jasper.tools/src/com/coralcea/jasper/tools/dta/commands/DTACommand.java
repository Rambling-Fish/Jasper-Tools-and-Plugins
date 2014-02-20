package com.coralcea.jasper.tools.dta.commands;

import org.eclipse.gef.commands.Command;

public class DTACommand extends Command {

	public DTACommand() {
	}

	public DTACommand(String label) {
		super(label);
	}
	
	@Override
	public final void execute() {
		prepare();
		redo();
	}
	
	@Override
	public void redo() {
	}

	protected void prepare() {}
	
}

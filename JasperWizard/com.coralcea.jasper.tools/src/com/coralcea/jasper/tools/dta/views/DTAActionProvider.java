package com.coralcea.jasper.tools.dta.views;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class DTAActionProvider extends CommonActionProvider {

	private DTAOpenEditorAction openAction;
	private DTACloseModelAction closeAction;
	private DTACloseAllModelsAction closeAllAction;
	
	public DTAActionProvider() {
		super();
	}

	@Override
	public void init(ICommonActionExtensionSite site) {
		ICommonViewerSite viewSite = site.getViewSite();
		if (viewSite instanceof ICommonViewerWorkbenchSite) {
			ICommonViewerWorkbenchSite workbenchSite = (ICommonViewerWorkbenchSite)viewSite;
			openAction = new DTAOpenEditorAction(workbenchSite.getPage(), workbenchSite.getSelectionProvider());
			closeAction = new DTACloseModelAction(workbenchSite.getPage(), workbenchSite.getSelectionProvider(), site.getStructuredViewer());
			closeAllAction = new DTACloseAllModelsAction(workbenchSite.getPage(), workbenchSite.getSelectionProvider(), site.getStructuredViewer());
		}
		super.init(site);
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (openAction.isEnabled())
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAction);
		if (closeAction.isEnabled())
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, closeAction);
		if (closeAction.isEnabled())
			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, closeAllAction);
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		if (openAction.isEnabled())
			actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
	}
	
	
}

package com.coralcea.jasper.tools.dta.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;

public class DTADropDownAction extends Action implements IMenuCreator {
	private Menu menu;
	List<Action> subActions = new ArrayList<Action>();

	public DTADropDownAction(String text) {
		super(text, IAction.AS_DROP_DOWN_MENU);
		setMenuCreator(this);
	}
	
	public void addSubAction(Action action) {
		subActions.add(action);
	}
	
	@Override
	public void dispose() {
		if (menu!=null)
			menu.dispose();
	}

	@Override
	public Menu getMenu(Control parent) {
		if (menu == null) {
			menu = new Menu(parent);
			for(Action subAction : subActions) {
				ActionContributionItem item= new ActionContributionItem(subAction);
			    item.fill(menu, -1);
			}
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

}

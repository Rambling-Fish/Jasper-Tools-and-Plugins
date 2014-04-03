package com.coralcea.jasper.tools.dta.editors;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.part.MultiPageEditorPart;

public class DTASelectionProvider implements IPostSelectionProvider {

    private ListenerList listeners = new ListenerList();
    
    private ListenerList postListeners = new ListenerList();

    private DTAEditor editor;

    public DTASelectionProvider(DTAEditor editor) {
        this.editor = editor;
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        listeners.add(listener);
    }

    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
    	postListeners.add(listener);
	}

    public void fireSelectionChanged(final SelectionChangedEvent event) {
        Object[] listeners = this.listeners.getListeners();
        fireEventChange(event, listeners);
    }

    public void firePostSelectionChanged(final SelectionChangedEvent event) {
		Object[] listeners = postListeners.getListeners();
		fireEventChange(event, listeners);
	}

	private void fireEventChange(final SelectionChangedEvent event, Object[] listeners) {
		for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
	}
    
    public MultiPageEditorPart getMultiPageEditor() {
        return editor;
    }

    public ISelection getSelection() {
       /*DTAViewer activeViewer = editor.getViewer(editor.getActivePage());
        if (activeViewer != null)
			return activeViewer.getSelection();*/
        return StructuredSelection.EMPTY;
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        listeners.remove(listener);
    }
    
    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
    	postListeners.remove(listener);
	}

    public void setSelection(ISelection selection) {
        /*DTAViewer activeViewer = editor.getViewer(editor.getActivePage());
        if (activeViewer != null)
        	activeViewer.setSelection(selection);*/
    }
}

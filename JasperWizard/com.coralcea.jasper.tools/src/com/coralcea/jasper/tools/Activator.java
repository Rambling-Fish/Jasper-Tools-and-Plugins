package com.coralcea.jasper.tools;

import java.util.ResourceBundle;

import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.coralcea.jasper.tools.dta.DTACore;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.coralcea.jasper.tools"; //$NON-NLS-1$

	// The plug-in resource bundle
	public static final String RESOURCE_BUNDLE = "com.coralcea.jasper.tools.Messages"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	private static ResourceBundle resourceBundle;

	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		resourceBundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
		DTACore.initialize();
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		DTACore.dispose();
		plugin = null;
		super.stop(context);
	}

	/*
	 * log a message
	 */
	public void log(String msg) {
		getLog().log(new Status(Status.INFO, PLUGIN_ID, msg));		
	}

	/*
	 * log a message
	 */
	public void log(Throwable e) {
		getLog().log(new Status(Status.INFO, PLUGIN_ID, e.getMessage(), e));		
	}

	/*
	 * log a message
	 */
	public void log(String msg, Throwable e) {
		getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.ERROR, msg, e));		
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(Images.MODEL, ImageDescriptor.createFromURL(getBundle().getEntry("icons/model.png")));
		reg.put(Images.PACKAGE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/package.png")));
		reg.put(Images.DTA, ImageDescriptor.createFromURL(getBundle().getEntry("icons/dta.png")));
		reg.put(Images.CLASS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/class.png")));
		reg.put(Images.PROPERTY, ImageDescriptor.createFromURL(getBundle().getEntry("icons/property.png")));
		reg.put(Images.OPERATION, ImageDescriptor.createFromURL(getBundle().getEntry("icons/operation.png")));
		reg.put(Images.PLUS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/plus.png")));
		reg.put(Images.MINUS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/minus.png")));
		reg.put(Images.POLICY, ImageDescriptor.createFromURL(getBundle().getEntry("icons/policy.png")));
		reg.put(Images.HELP, ImageDescriptor.createFromURL(getBundle().getEntry("icons/help.gif")));
		reg.put(Images.CODEGEN, ImageDescriptor.createFromURL(getBundle().getEntry("icons/codegen.png")));
		reg.put(Images.HOME, ImageDescriptor.createFromURL(getBundle().getEntry("icons/home.png")));
		reg.put(Images.UPARROW, ImageDescriptor.createFromURL(getBundle().getEntry("icons/uparrow.png")));
		reg.put(Images.REFRESH, ImageDescriptor.createFromURL(getBundle().getEntry("icons/refresh.png")));
		reg.put(Images.SERVER, ImageDescriptor.createFromURL(getBundle().getEntry("icons/server.png")));
		reg.put(Images.LAYOUT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/layout.gif")));
		reg.put(Images.ZOOM_IN, ImageDescriptor.createFromURL(getBundle().getEntry("icons/zoom_in.gif")));
		reg.put(Images.ZOOM_OUT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/zoom_out.gif")));
		reg.put(Images.IMPORT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/import.png")));
		reg.put(Images.CAMERA, ImageDescriptor.createFromURL(getBundle().getEntry("icons/camera.gif")));
		reg.put(Images.SAVE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/save.png")));
		reg.put(Images.REQUEST, ImageDescriptor.createFromURL(getBundle().getEntry("icons/request.png")));
		reg.put(Images.VALIDATE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/validate.png")));
		reg.put(Images.FILTER, ImageDescriptor.createFromURL(getBundle().getEntry("icons/filter.png")));
		reg.put(Images.FIND, ImageDescriptor.createFromURL(getBundle().getEntry("icons/find.gif")));
	}

	public static Image getImage(String id) {
		return Activator.getDefault().getImageRegistry().get(id);
	}
	
	public static ImageDescriptor getImageDescriptor(String id) {
		return Activator.getDefault().getImageRegistry().getDescriptor(id);
	}
	
	public static ResourceBundle getResourceBundle() {
		return resourceBundle;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}

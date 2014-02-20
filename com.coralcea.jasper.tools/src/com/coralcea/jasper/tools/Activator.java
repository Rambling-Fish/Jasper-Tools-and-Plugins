package com.coralcea.jasper.tools;

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

	// The shared instance
	private static Activator plugin;
	
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
	public void log(String msg, Throwable e) {
		getLog().log(new Status(Status.INFO, PLUGIN_ID, Status.ERROR, msg, e));		
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put(JasperImages.MODEL, ImageDescriptor.createFromURL(getBundle().getEntry("icons/model.png")));
		reg.put(JasperImages.PACKAGE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/package.png")));
		reg.put(JasperImages.DTA, ImageDescriptor.createFromURL(getBundle().getEntry("icons/dta.png")));
		reg.put(JasperImages.CLASS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/class.png")));
		reg.put(JasperImages.PROPERTY, ImageDescriptor.createFromURL(getBundle().getEntry("icons/property.png")));
		reg.put(JasperImages.OPERATION, ImageDescriptor.createFromURL(getBundle().getEntry("icons/operation.png")));
		reg.put(JasperImages.PLUS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/plus.png")));
		reg.put(JasperImages.MINUS, ImageDescriptor.createFromURL(getBundle().getEntry("icons/minus.png")));
		reg.put(JasperImages.POLICY, ImageDescriptor.createFromURL(getBundle().getEntry("icons/policy.png")));
		reg.put(JasperImages.HELP, ImageDescriptor.createFromURL(getBundle().getEntry("icons/help.gif")));
		reg.put(JasperImages.CODEGEN, ImageDescriptor.createFromURL(getBundle().getEntry("icons/codegen.png")));
		reg.put(JasperImages.HOME, ImageDescriptor.createFromURL(getBundle().getEntry("icons/home.png")));
		reg.put(JasperImages.UPARROW, ImageDescriptor.createFromURL(getBundle().getEntry("icons/uparrow.png")));
		reg.put(JasperImages.REFRESH, ImageDescriptor.createFromURL(getBundle().getEntry("icons/refresh.png")));
		reg.put(JasperImages.SERVER, ImageDescriptor.createFromURL(getBundle().getEntry("icons/server.png")));
		reg.put(JasperImages.LAYOUT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/layout.gif")));
		reg.put(JasperImages.ZOOM_IN, ImageDescriptor.createFromURL(getBundle().getEntry("icons/zoom_in.gif")));
		reg.put(JasperImages.ZOOM_OUT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/zoom_out.gif")));
		reg.put(JasperImages.IMPORT, ImageDescriptor.createFromURL(getBundle().getEntry("icons/import.png")));
		reg.put(JasperImages.CAMERA, ImageDescriptor.createFromURL(getBundle().getEntry("icons/camera.gif")));
		reg.put(JasperImages.SAVE, ImageDescriptor.createFromURL(getBundle().getEntry("icons/save.png")));
		reg.put(JasperImages.REQUEST, ImageDescriptor.createFromURL(getBundle().getEntry("icons/request.png")));
	}

	public static Image getImage(String id) {
		return Activator.getDefault().getImageRegistry().get(id);
	}
	
	public static ImageDescriptor getImageDescriptor(String id) {
		return Activator.getDefault().getImageRegistry().getDescriptor(id);
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

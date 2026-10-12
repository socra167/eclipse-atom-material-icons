package io.github.socra167.atommaterialicons;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator for the Atom Material Icons plug-in.
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "io.github.socra167.atommaterialicons"; //$NON-NLS-1$

	private static Activator plugin;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * @return the shared instance, or {@code null} if the bundle is not active
	 */
	public static Activator getDefault() {
		return plugin;
	}
}

package io.github.socra167.atommaterialicons;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.ILog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import io.github.socra167.atommaterialicons.core.IconResolver;
import io.github.socra167.atommaterialicons.preferences.PreferenceConstants;

/**
 * Bundle activator for the Atom Material Icons plug-in.
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "io.github.socra167.atommaterialicons"; //$NON-NLS-1$

	private static Activator plugin;

	private final Map<String, ImageDescriptor> descriptors = new ConcurrentHashMap<>();
	private volatile IconResolver resolver;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		descriptors.clear();
		resolver = null;
		super.stop(context);
	}

	/**
	 * @return the shared instance, or {@code null} if the bundle is not active
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * @return the icon resolver, or {@code null} if the bundled associations cannot be loaded
	 */
	public IconResolver getIconResolver() {
		IconResolver result = resolver;
		if (result == null) {
			synchronized (this) {
				result = resolver;
				if (result == null) {
					try {
						result = IconResolver.load(Activator.class.getClassLoader());
						result.setPackFilter(PreferenceConstants::isPackEnabled);
						resolver = result;
					} catch (IOException e) {
						ILog.of(getClass()).error("Cannot load the icon associations", e); //$NON-NLS-1$
					}
				}
			}
		}
		return result;
	}

	/**
	 * @param path a bundle relative image path
	 * @return the image descriptor; HiDPI {@code @2x} variants are picked up automatically
	 */
	public ImageDescriptor getImageDescriptor(String path) {
		return descriptors.computeIfAbsent(path, p -> ResourceLocator.imageDescriptorFromBundle(PLUGIN_ID, p)
				.orElseGet(ImageDescriptor::getMissingImageDescriptor));
	}
}

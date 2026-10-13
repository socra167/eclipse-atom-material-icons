package io.github.socra167.atommaterialicons.navigator;

import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.resource.LocalResourceManager;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

import io.github.socra167.atommaterialicons.Activator;
import io.github.socra167.atommaterialicons.core.IconResolver;
import io.github.socra167.atommaterialicons.preferences.PreferenceConstants;

/**
 * Supplies Atom Material icons for resources in the Common Navigator (Project Explorer).
 * <p>
 * The navigator content of this plug-in has the highest priority, so this provider is asked
 * for an image before the providers of the resource and Java content. It only provides images:
 * for texts and for elements without a matching icon it returns {@code null}, so that the next
 * provider is used. Workbench decorators (version control, linked resources, ...) are applied on
 * top of the returned image as usual.
 * </p>
 */
public class MaterialIconLabelProvider extends LabelProvider
		implements ICommonLabelProvider, IPreferenceChangeListener, IResourceChangeListener {

	/** Whether JDT is installed, the Java type icons are only available then. */
	private static final boolean JDT_AVAILABLE = Platform.getBundle("org.eclipse.jdt.core") != null; //$NON-NLS-1$

	private LocalResourceManager resources;
	private volatile Settings settings = Settings.load();

	public MaterialIconLabelProvider() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).addPreferenceChangeListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
	}

	@Override
	public void init(ICommonContentExtensionSite config) {
		// nothing to initialize
	}

	@Override
	public Image getImage(Object element) {
		Settings current = settings;
		IResource resource;
		boolean modelElement;
		if (element instanceof IResource r) {
			resource = r;
			modelElement = false;
		} else {
			resource = Adapters.adapt(element, IResource.class);
			modelElement = true;
		}
		if (resource == null || (modelElement && resource.getType() == IResource.FILE && !representsFile(element, resource))) {
			return null;
		}
		Activator activator = Activator.getDefault();
		IconResolver resolver = activator == null ? null : activator.getIconResolver();
		if (resolver == null) {
			return null;
		}

		String icon;
		switch (resource.getType()) {
		case IResource.FILE:
			icon = null;
			if (current.javaTypeIcons && JDT_AVAILABLE) {
				icon = JavaTypeIcons.resolve(element, (IFile) resource);
			}
			if (icon == null && current.fileIcons) {
				icon = resolver.resolveFile(resource.getName(), pathOf(resource));
			}
			break;
		case IResource.FOLDER:
			icon = current.folderIcons && (!modelElement || current.modelElementFolders)
					? resolver.resolveFolder(resource.getName(), pathOf(resource), current.defaultFolderIcon)
					: null;
			break;
		default:
			// projects keep their nature specific icons
			icon = null;
		}
		if (icon == null) {
			return null;
		}

		ImageDescriptor descriptor = activator.getImageDescriptor(icon);
		if (current.errorOverlays || current.warningOverlays) {
			descriptor = addProblemOverlay(descriptor, resource, current.errorOverlays, current.warningOverlays);
		}
		return getResources().create(descriptor);
	}

	/**
	 * Model elements inside a file, such as the types and methods of a Java file, adapt to the file
	 * too, but must keep their own icons. Only elements that stand for the file itself get its icon.
	 */
	private static boolean representsFile(Object element, IResource file) {
		if (JDT_AVAILABLE) {
			Boolean javaElement = JavaTypeIcons.representsFile(element);
			if (javaElement != null) {
				return javaElement.booleanValue();
			}
		}
		IWorkbenchAdapter adapter = Adapters.adapt(element, IWorkbenchAdapter.class);
		return adapter != null && file.getName().equals(adapter.getLabel(element));
	}

	/**
	 * Path patterns are matched against the workspace path (for example {@code /project/src/Foo.java}),
	 * so that the location of the workspace on disk cannot influence the icons.
	 */
	private static String pathOf(IResource resource) {
		return resource.getFullPath().toString();
	}

	/**
	 * Adds the overlay of the most severe problem whose overlay is enabled. When only warnings are
	 * shown, a resource with errors and warnings still gets the warning overlay.
	 */
	private static ImageDescriptor addProblemOverlay(ImageDescriptor descriptor, IResource resource, boolean errors,
			boolean warnings) {
		if (!resource.isAccessible()) {
			return descriptor;
		}
		String overlay = null;
		try {
			int depth = resource.getType() == IResource.FILE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE;
			int severity = resource.findMaxProblemSeverity(IMarker.PROBLEM, true, depth);
			if (severity == IMarker.SEVERITY_ERROR && errors) {
				overlay = ISharedImages.IMG_DEC_FIELD_ERROR;
			} else if (warnings && (severity == IMarker.SEVERITY_WARNING
					|| (severity == IMarker.SEVERITY_ERROR && hasWarnings(resource, depth)))) {
				overlay = ISharedImages.IMG_DEC_FIELD_WARNING;
			}
		} catch (CoreException e) {
			// markers may change concurrently, show the plain icon
			return descriptor;
		}
		if (overlay == null) {
			return descriptor;
		}
		ImageDescriptor overlayDescriptor = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(overlay);
		return new ProblemOverlayIcon(descriptor, overlayDescriptor);
	}

	private static boolean hasWarnings(IResource resource, int depth) throws CoreException {
		for (IMarker marker : resource.findMarkers(IMarker.PROBLEM, true, depth)) {
			if (marker.getAttribute(IMarker.SEVERITY, -1) == IMarker.SEVERITY_WARNING) {
				return true;
			}
		}
		return false;
	}


	private LocalResourceManager getResources() {
		if (resources == null) {
			resources = new LocalResourceManager(JFaceResources.getResources());
		}
		return resources;
	}

	@Override
	public String getText(Object element) {
		// only images are provided, the text comes from the next label provider
		return null;
	}

	@Override
	public String getDescription(Object element) {
		return null;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		settings = Settings.load();
		if (JDT_AVAILABLE) {
			JavaTypeIcons.clear();
		}
		Activator activator = Activator.getDefault();
		if (activator != null && event.getKey().startsWith(PreferenceConstants.PACK_PREFIX)) {
			IconResolver resolver = activator.getIconResolver();
			if (resolver != null) {
				// clears the cached lookups
				resolver.setPackFilter(PreferenceConstants::isPackEnabled);
			}
		}
		fireAsync(new LabelProviderChangedEvent(this));
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		Settings current = settings;
		Set<IResource> changed = new LinkedHashSet<>();
		if (current.errorOverlays || current.warningOverlays) {
			for (IMarkerDelta delta : event.findMarkerDeltas(IMarker.PROBLEM, true)) {
				// the severity of folders aggregates their content, so update the ancestors too
				for (IResource r = delta.getResource(); r != null && r.getType() != IResource.ROOT; r = r.getParent()) {
					if (!changed.add(r)) {
						break;
					}
				}
			}
		}
		if (current.javaTypeIcons && JDT_AVAILABLE && event.getDelta() != null) {
			// the type declared in a Java file may change when it is saved
			Set<IResource> javaFiles = new LinkedHashSet<>();
			try {
				event.getDelta().accept(delta -> {
					IResource r = delta.getResource();
					if (r.getType() == IResource.FILE) {
						if ("java".equalsIgnoreCase(r.getFileExtension()) //$NON-NLS-1$
								&& (delta.getFlags() & IResourceDelta.CONTENT) != 0) {
							javaFiles.add(r);
						}
						return false;
					}
					return true;
				});
			} catch (CoreException e) {
				// cannot happen, the visitor throws no exceptions
			}
			JavaTypeIcons.forget(javaFiles);
			changed.addAll(javaFiles);
		}
		if (!changed.isEmpty()) {
			Set<Object> elements = new LinkedHashSet<>(changed);
			if (JDT_AVAILABLE) {
				// Java projects show Java elements instead of resources
				elements.addAll(JavaTypeIcons.javaElementsOf(changed));
			}
			fireAsync(new LabelProviderChangedEvent(this, elements.toArray()));
		}
	}


	private void fireAsync(LabelProviderChangedEvent event) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (!display.isDisposed()) {
			display.asyncExec(() -> fireLabelProviderChanged(event));
		}
	}

	@Override
	public void restoreState(IMemento memento) {
		// no state
	}

	@Override
	public void saveState(IMemento memento) {
		// no state
	}

	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		node.removePreferenceChangeListener(this);
		if (resources != null) {
			resources.dispose();
			resources = null;
		}
		super.dispose();
	}

	/**
	 * Snapshot of the preferences, so that they are not looked up for every label.
	 */
	private record Settings(boolean fileIcons, boolean folderIcons, boolean defaultFolderIcon,
			boolean modelElementFolders, boolean errorOverlays, boolean warningOverlays, boolean javaTypeIcons) {

		static Settings load() {
			return new Settings(
					PreferenceConstants.getBoolean(PreferenceConstants.FILE_ICONS),
					PreferenceConstants.getBoolean(PreferenceConstants.FOLDER_ICONS),
					PreferenceConstants.getBoolean(PreferenceConstants.DEFAULT_FOLDER_ICON),
					PreferenceConstants.getBoolean(PreferenceConstants.MODEL_ELEMENT_FOLDERS),
					PreferenceConstants.getBoolean(PreferenceConstants.ERROR_OVERLAYS),
					PreferenceConstants.getBoolean(PreferenceConstants.WARNING_OVERLAYS),
					PreferenceConstants.getBoolean(PreferenceConstants.JAVA_TYPE_ICONS));
		}
	}
}

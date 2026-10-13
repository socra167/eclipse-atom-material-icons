package io.github.socra167.atommaterialicons.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.github.socra167.atommaterialicons.Activator;

/**
 * Sets the default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		for (String key : new String[] { PreferenceConstants.FILE_ICONS, PreferenceConstants.FOLDER_ICONS,
				PreferenceConstants.DEFAULT_FOLDER_ICON, PreferenceConstants.MODEL_ELEMENT_FOLDERS,
				PreferenceConstants.JAVA_TYPE_ICONS, PreferenceConstants.ERROR_OVERLAYS,
				PreferenceConstants.WARNING_OVERLAYS }) {
			defaults.putBoolean(key, getDefault(key));
		}
		for (String pack : PreferenceConstants.ICON_PACKS) {
			String key = PreferenceConstants.PACK_PREFIX + pack;
			defaults.putBoolean(key, getDefault(key));
		}
		migrateProblemOverlays();
	}

	/**
	 * Earlier versions had one switch for error and warning overlays; carry it over to both.
	 */
	private static void migrateProblemOverlays() {
		IEclipsePreferences instance = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		String legacy = instance.get(PreferenceConstants.LEGACY_PROBLEM_OVERLAYS, null);
		if (legacy != null) {
			boolean enabled = Boolean.parseBoolean(legacy);
			instance.putBoolean(PreferenceConstants.ERROR_OVERLAYS, enabled);
			instance.putBoolean(PreferenceConstants.WARNING_OVERLAYS, enabled);
			instance.remove(PreferenceConstants.LEGACY_PROBLEM_OVERLAYS);
		}
	}

	static boolean getDefault(String key) {
		if (PreferenceConstants.MODEL_ELEMENT_FOLDERS.equals(key)) {
			return false;
		}
		if (key.startsWith(PreferenceConstants.PACK_PREFIX)) {
			return !PreferenceConstants.PACKS_DISABLED_BY_DEFAULT
					.contains(key.substring(PreferenceConstants.PACK_PREFIX.length()));
		}
		return true;
	}
}

package io.github.socra167.atommaterialicons.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

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
				PreferenceConstants.PROBLEM_OVERLAYS }) {
			defaults.putBoolean(key, getDefault(key));
		}
		for (String pack : PreferenceConstants.ICON_PACKS) {
			String key = PreferenceConstants.PACK_PREFIX + pack;
			defaults.putBoolean(key, getDefault(key));
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

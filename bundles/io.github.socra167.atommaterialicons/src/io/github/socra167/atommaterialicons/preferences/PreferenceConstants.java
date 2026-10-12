package io.github.socra167.atommaterialicons.preferences;

import java.util.List;

import org.eclipse.core.runtime.Platform;

import io.github.socra167.atommaterialicons.Activator;

/**
 * Preference keys and their access helpers.
 */
public final class PreferenceConstants {

	/** Replace file icons. */
	public static final String FILE_ICONS = "fileIcons"; //$NON-NLS-1$

	/** Replace folder icons. */
	public static final String FOLDER_ICONS = "folderIcons"; //$NON-NLS-1$

	/** Use the generic Material folder icon for folders without a specific icon. */
	public static final String DEFAULT_FOLDER_ICON = "defaultFolderIcon"; //$NON-NLS-1$

	/** Also replace the icons of folders shown as model elements, such as Java packages and source folders. */
	public static final String MODEL_ELEMENT_FOLDERS = "modelElementFolders"; //$NON-NLS-1$

	/** Paint error and warning overlays on the icons. */
	public static final String PROBLEM_OVERLAYS = "problemOverlays"; //$NON-NLS-1$

	/** Prefix of the keys that enable an icon pack, followed by the pack name. */
	public static final String PACK_PREFIX = "iconPack."; //$NON-NLS-1$

	/** The optional icon packs of Atom Material Icons. */
	public static final List<String> ICON_PACKS = List.of("ANGULAR", "ANGULAR2", "CSS", "NEST", "NEXTJS", "NGRX", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"RAILS", "RECOIL", "REDUX", "TESTS"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	/** Icon packs that are disabled by default, like in Atom Material Icons. */
	static final List<String> PACKS_DISABLED_BY_DEFAULT = List.of("ANGULAR"); //$NON-NLS-1$

	private PreferenceConstants() {
	}

	/**
	 * @return the current boolean value of a preference of this plug-in
	 */
	public static boolean getBoolean(String key) {
		return Platform.getPreferencesService().getBoolean(Activator.PLUGIN_ID, key,
				PreferenceInitializer.getDefault(key), null);
	}

	/**
	 * @return whether the icons of the given pack are used
	 */
	public static boolean isPackEnabled(String pack) {
		return getBoolean(PACK_PREFIX + pack);
	}
}

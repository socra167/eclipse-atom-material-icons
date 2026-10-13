package io.github.socra167.atommaterialicons.preferences;

import org.eclipse.osgi.util.NLS;

final class Messages extends NLS {

	private static final String BUNDLE_NAME = "io.github.socra167.atommaterialicons.preferences.messages"; //$NON-NLS-1$

	public static String PreferencePage_description;
	public static String PreferencePage_iconsGroup;
	public static String PreferencePage_fileIcons;
	public static String PreferencePage_folderIcons;
	public static String PreferencePage_defaultFolderIcon;
	public static String PreferencePage_modelElementFolders;
	public static String PreferencePage_javaTypeIcons;
	public static String PreferencePage_problemsGroup;
	public static String PreferencePage_errorOverlays;
	public static String PreferencePage_warningOverlays;
	public static String PreferencePage_iconPacksGroup;
	public static String PreferencePage_pack_ANGULAR;
	public static String PreferencePage_pack_ANGULAR2;
	public static String PreferencePage_pack_CSS;
	public static String PreferencePage_pack_NEST;
	public static String PreferencePage_pack_NEXTJS;
	public static String PreferencePage_pack_NGRX;
	public static String PreferencePage_pack_RAILS;
	public static String PreferencePage_pack_RECOIL;
	public static String PreferencePage_pack_REDUX;
	public static String PreferencePage_pack_TESTS;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}

	/**
	 * @return the display name of an icon pack
	 */
	static String packLabel(String pack) {
		try {
			Object label = Messages.class.getField("PreferencePage_pack_" + pack).get(null); //$NON-NLS-1$
			if (label instanceof String s) {
				return s;
			}
		} catch (ReflectiveOperationException e) {
			// fall through
		}
		return pack;
	}
}

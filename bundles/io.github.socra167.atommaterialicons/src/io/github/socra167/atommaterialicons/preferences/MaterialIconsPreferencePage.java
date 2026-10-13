package io.github.socra167.atommaterialicons.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import io.github.socra167.atommaterialicons.Activator;

/**
 * The preference page under General &gt; Appearance &gt; Atom Material Icons.
 */
public class MaterialIconsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public MaterialIconsPreferencePage() {
		super(GRID);
		setDescription(Messages.PreferencePage_description);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		Composite icons = createGroup(Messages.PreferencePage_iconsGroup);
		addBoolean(PreferenceConstants.FILE_ICONS, Messages.PreferencePage_fileIcons, icons);
		addBoolean(PreferenceConstants.FOLDER_ICONS, Messages.PreferencePage_folderIcons, icons);
		addBoolean(PreferenceConstants.DEFAULT_FOLDER_ICON, Messages.PreferencePage_defaultFolderIcon, icons);
		addBoolean(PreferenceConstants.MODEL_ELEMENT_FOLDERS, Messages.PreferencePage_modelElementFolders, icons);
		addBoolean(PreferenceConstants.JAVA_TYPE_ICONS, Messages.PreferencePage_javaTypeIcons, icons);
		addBoolean(PreferenceConstants.PROBLEM_OVERLAYS, Messages.PreferencePage_problemOverlays, icons);

		Composite packs = createGroup(Messages.PreferencePage_iconPacksGroup);
		for (String pack : PreferenceConstants.ICON_PACKS) {
			addBoolean(PreferenceConstants.PACK_PREFIX + pack, Messages.packLabel(pack), packs);
		}
	}

	private void addBoolean(String key, String label, Composite parent) {
		addField(new BooleanFieldEditor(key, label, parent));
	}

	private Composite createGroup(String text) {
		Group group = new Group(getFieldEditorParent(), 0);
		group.setText(text);
		group.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, true, false));
		group.setLayout(new GridLayout());
		return group;
	}

	@Override
	protected void adjustGridLayout() {
		super.adjustGridLayout();
		// field editors are laid out inside the groups; keep their margins
		for (var child : getFieldEditorParent().getChildren()) {
			if (child instanceof Group group) {
				GridLayout layout = (GridLayout) group.getLayout();
				layout.marginWidth = 5;
				layout.marginHeight = 5;
			}
		}
	}
}

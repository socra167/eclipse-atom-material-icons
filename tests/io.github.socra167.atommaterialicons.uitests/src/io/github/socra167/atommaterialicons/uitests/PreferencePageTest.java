package io.github.socra167.atommaterialicons.uitests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.socra167.atommaterialicons.Activator;
import io.github.socra167.atommaterialicons.preferences.MaterialIconsPreferencePage;
import io.github.socra167.atommaterialicons.preferences.PreferenceConstants;

class PreferencePageTest {

	private Shell shell;
	private MaterialIconsPreferencePage page;

	@BeforeEach
	void setUp() {
		shell = new Shell(PlatformUI.getWorkbench().getDisplay());
		shell.setLayout(new FillLayout());
		page = new MaterialIconsPreferencePage();
		page.init(PlatformUI.getWorkbench());
		page.createControl(shell);
	}

	@AfterEach
	void tearDown() throws Exception {
		page.dispose();
		shell.dispose();
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).clear();
	}

	@Test
	void showsAllOptionsWithTheirDefaults() {
		List<Button> checkboxes = checkboxes(shell);
		assertEquals(5 + PreferenceConstants.ICON_PACKS.size(), checkboxes.size());
		assertTrue(checkbox("Show &file icons").getSelection());
		assertFalse(checkbox("Also replace the icons of &Java packages and source folders").getSelection());
		assertFalse(checkbox("Angular (AngularJS)").getSelection());
		assertTrue(checkbox("Redux").getSelection());
	}

	@Test
	void storesChangedOptions() {
		Button folderIcons = checkbox("Show f&older icons");
		folderIcons.setSelection(false);
		folderIcons.notifyListeners(SWT.Selection, null);
		assertTrue(page.performOk());
		assertFalse(PreferenceConstants.getBoolean(PreferenceConstants.FOLDER_ICONS));

		folderIcons.setSelection(true);
		folderIcons.notifyListeners(SWT.Selection, null);
		assertTrue(page.performOk());
		assertTrue(PreferenceConstants.getBoolean(PreferenceConstants.FOLDER_ICONS));
	}

	private Button checkbox(String text) {
		for (Button button : checkboxes(shell)) {
			if (text.equals(button.getText())) {
				return button;
			}
		}
		return fail("no checkbox " + text);
	}

	private static List<Button> checkboxes(Composite parent) {
		List<Button> result = new ArrayList<>();
		for (Control child : parent.getChildren()) {
			if (child instanceof Button button && (button.getStyle() & SWT.CHECK) != 0) {
				result.add(button);
			} else if (child instanceof Composite composite) {
				result.addAll(checkboxes(composite));
			}
		}
		return result;
	}
}

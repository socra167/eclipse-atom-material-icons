package io.github.socra167.atommaterialicons.uitests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.socra167.atommaterialicons.Activator;
import io.github.socra167.atommaterialicons.preferences.PreferenceConstants;

/**
 * Checks the icons shown by the real Project Explorer.
 */
class ProjectExplorerIconsTest {

	private IProject project;
	private CommonNavigator explorer;

	@BeforeEach
	void setUp() throws CoreException, PartInitException {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("demo");
		project.create(null);
		project.open(null);
		createFile(project.getFile("pom.xml"));
		createFile(project.getFile("unknown.zzqq"));
		IFolder src = project.getFolder("src");
		src.create(true, true, null);
		createFile(src.getFile("Foo.java"));
		project.getFolder("unknownzz").create(true, true, null);

		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		explorer = (CommonNavigator) page.showView(IPageLayout.ID_PROJECT_EXPLORER);
		explorer.getCommonViewer().expandAll();
		processEvents();
	}

	@AfterEach
	void tearDown() throws CoreException {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.FILE_ICONS);
		project.delete(true, true, null);
	}

	@Test
	void showsMaterialIconsInProjectExplorer() {
		assertShows("icons/files/maven.png", project.getFile("pom.xml"));
		assertShows("icons/files/java.png", project.getFile("src/Foo.java"));
		assertShows("icons/folders/src.png", project.getFolder("src"));
		assertShows("icons/folders/folder.png", project.getFolder("unknownzz"));
	}

	@Test
	void keepsTheDefaultIconOfUnknownFiles() {
		Image image = imageOf(project.getFile("unknown.zzqq"));
		assertFalse(sameImage(image, "icons/files/maven.png"));
		assertFalse(sameImage(image, "icons/files/java.png"));
	}

	@Test
	void followsThePreferences() {
		IFile pom = project.getFile("pom.xml");
		assertShows("icons/files/maven.png", pom);

		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(PreferenceConstants.FILE_ICONS, false);
		processEvents();
		assertFalse(sameImage(imageOf(pom), "icons/files/maven.png"), "file icons should be turned off");

		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.FILE_ICONS);
		processEvents();
		assertShows("icons/files/maven.png", pom);
	}

	@Test
	void showsTheErrorOverlayOnFilesAndTheirFolders() throws CoreException {
		IFile java = project.getFile("src/Foo.java");
		IMarker marker = java.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		processEvents();

		for (IResource resource : new IResource[] { java, project.getFolder("src") }) {
			ImageData image = imageOf(resource).getImageData();
			assertTrue(redPixelsInBottomLeft(image) > 10, resource + " should show the error overlay");
			assertTrue(image.getAlpha(7, image.height - 8) == 0 || image.getAlpha(8, image.height - 9) == 0,
					resource + " should have a transparent margin around the overlay");
		}
		assertShows("icons/files/maven.png", project.getFile("pom.xml"));

		marker.delete();
		processEvents();
		assertShows("icons/files/java.png", java);
		assertShows("icons/folders/src.png", project.getFolder("src"));
	}

	private static int redPixelsInBottomLeft(ImageData image) {
		int count = 0;
		for (int x = 0; x < 7; x++) {
			for (int y = image.height - 8; y < image.height; y++) {
				RGB rgb = image.palette.getRGB(image.getPixel(x, y));
				if (image.getAlpha(x, y) > 0 && rgb.red > 150 && rgb.green < 120 && rgb.blue < 120) {
					count++;
				}
			}
		}
		return count;
	}

	private void assertShows(String icon, IResource resource) {
		assertTrue(sameImage(imageOf(resource), icon), resource.getFullPath() + " should show " + icon);
	}

	private Image imageOf(IResource resource) {
		Tree tree = explorer.getCommonViewer().getTree();
		TreeItem item = find(tree.getItems(), resource);
		assertNotNull(item, "no tree item for " + resource);
		return item.getImage();
	}

	private static TreeItem find(TreeItem[] items, IResource resource) {
		for (TreeItem item : items) {
			if (resource.equals(item.getData())) {
				return item;
			}
			TreeItem found = find(item.getItems(), resource);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static boolean sameImage(Image actual, String icon) {
		if (actual == null) {
			return false;
		}
		Image expected = Activator.getDefault().getImageDescriptor(icon).createImage();
		try {
			ImageData a = actual.getImageData();
			ImageData e = expected.getImageData();
			if (a.width != e.width || a.height != e.height) {
				return false;
			}
			for (int x = 0; x < a.width; x++) {
				for (int y = 0; y < a.height; y++) {
					if (a.getAlpha(x, y) != e.getAlpha(x, y)
							|| (e.getAlpha(x, y) != 0 && !a.palette.getRGB(a.getPixel(x, y)).equals(e.palette.getRGB(e.getPixel(x, y))))) {
						return false;
					}
				}
			}
			return true;
		} finally {
			expected.dispose();
		}
	}

	private static void createFile(IFile file) throws CoreException {
		file.create(new ByteArrayInputStream(new byte[0]), true, null);
	}

	private static void processEvents() {
		Display display = Display.getCurrent();
		long end = System.currentTimeMillis() + 1500;
		while (System.currentTimeMillis() < end) {
			while (display.readAndDispatch()) {
				// keep dispatching
			}
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}
}

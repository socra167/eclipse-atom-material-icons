package io.github.socra167.atommaterialicons.uitests;

import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.assertShows;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.createFile;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.imageOf;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.processEvents;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.sameImage;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.showExplorer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.eclipse.ui.PartInitException;
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

		explorer = showExplorer();
	}

	@AfterEach
	void tearDown() throws CoreException {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.FILE_ICONS);
		project.delete(true, true, null);
	}

	@Test
	void showsMaterialIconsInProjectExplorer() {
		assertShows(explorer, "icons/files/maven.png", project.getFile("pom.xml"));
		assertShows(explorer, "icons/files/java.png", project.getFile("src/Foo.java"));
		assertShows(explorer, "icons/folders/src.png", project.getFolder("src"));
		assertShows(explorer, "icons/folders/folder.png", project.getFolder("unknownzz"));
	}

	@Test
	void keepsTheDefaultIconOfUnknownFiles() {
		Image image = imageOf(explorer, project.getFile("unknown.zzqq"));
		assertFalse(sameImage(image, "icons/files/maven.png"));
		assertFalse(sameImage(image, "icons/files/java.png"));
	}

	@Test
	void followsThePreferences() {
		IFile pom = project.getFile("pom.xml");
		assertShows(explorer, "icons/files/maven.png", pom);

		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(PreferenceConstants.FILE_ICONS, false);
		processEvents();
		assertFalse(sameImage(imageOf(explorer, pom), "icons/files/maven.png"), "file icons should be turned off");

		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.FILE_ICONS);
		processEvents();
		assertShows(explorer, "icons/files/maven.png", pom);
	}

	@Test
	void showsTheErrorOverlayOnFilesAndTheirFolders() throws CoreException {
		IFile java = project.getFile("src/Foo.java");
		IMarker marker = java.createMarker(IMarker.PROBLEM);
		marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
		processEvents();

		for (IResource resource : new IResource[] { java, project.getFolder("src") }) {
			ImageData image = imageOf(explorer, resource).getImageData();
			assertTrue(redPixelsInBottomLeft(image) > 10, resource + " should show the error overlay");
			assertTrue(image.getAlpha(7, image.height - 8) == 0 || image.getAlpha(8, image.height - 9) == 0,
					resource + " should have a transparent margin around the overlay");
		}
		assertShows(explorer, "icons/files/maven.png", project.getFile("pom.xml"));

		marker.delete();
		processEvents();
		assertShows(explorer, "icons/files/java.png", java);
		assertShows(explorer, "icons/folders/src.png", project.getFolder("src"));
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

}

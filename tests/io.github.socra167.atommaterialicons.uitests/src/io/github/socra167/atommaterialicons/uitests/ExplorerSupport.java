package io.github.socra167.atommaterialicons.uitests;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;

import io.github.socra167.atommaterialicons.Activator;

/**
 * Helpers to inspect the icons of the Project Explorer.
 */
final class ExplorerSupport {

	private ExplorerSupport() {
	}

	static CommonNavigator showExplorer() throws PartInitException {
		CommonNavigator explorer = (CommonNavigator) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().showView(IPageLayout.ID_PROJECT_EXPLORER);
		explorer.getCommonViewer().refresh();
		explorer.getCommonViewer().expandAll();
		processEvents();
		return explorer;
	}

	static void assertShows(CommonNavigator explorer, String icon, IResource resource) {
		assertTrue(sameImage(imageOf(explorer, resource), icon), resource.getFullPath() + " should show " + icon);
	}

	/**
	 * Finds the tree item of a resource, also when the viewer shows it as a Java element.
	 */
	static Image imageOf(CommonNavigator explorer, IResource resource) {
		TreeItem item = find(explorer.getCommonViewer().getTree().getItems(), resource);
		assertNotNull(item, "no tree item for " + resource);
		return item.getImage();
	}

	/**
	 * @return the image of the first tree item showing the given element
	 */
	static Image imageOfElement(CommonNavigator explorer, Object element) {
		TreeItem item = findData(explorer.getCommonViewer().getTree().getItems(), element);
		assertNotNull(item, "no tree item for " + element);
		return item.getImage();
	}

	private static TreeItem findData(TreeItem[] items, Object element) {
		for (TreeItem item : items) {
			if (element.equals(item.getData())) {
				return item;
			}
			TreeItem found = findData(item.getItems(), element);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	private static TreeItem find(TreeItem[] items, IResource resource) {
		for (TreeItem item : items) {
			Object data = item.getData();
			if (data != null && resource.equals(Adapters.adapt(data, IResource.class))) {
				return item;
			}
			TreeItem found = find(item.getItems(), resource);
			if (found != null) {
				return found;
			}
		}
		return null;
	}

	static boolean sameImage(Image actual, String icon) {
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
					if (a.getAlpha(x, y) != e.getAlpha(x, y) || (e.getAlpha(x, y) != 0
							&& !a.palette.getRGB(a.getPixel(x, y)).equals(e.palette.getRGB(e.getPixel(x, y))))) {
						return false;
					}
				}
			}
			return true;
		} finally {
			expected.dispose();
		}
	}

	static void createFile(IFile file) throws CoreException {
		createFile(file, "");
	}

	static void createFile(IFile file, String content) throws CoreException {
		if (file.getParent() instanceof IFolder folder && !folder.exists()) {
			createFolder(folder);
		}
		file.create(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), true, null);
	}

	static void createFolder(IFolder folder) throws CoreException {
		if (folder.getParent() instanceof IFolder parent && !parent.exists()) {
			createFolder(parent);
		}
		folder.create(true, true, null);
	}

	static void processEvents() {
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

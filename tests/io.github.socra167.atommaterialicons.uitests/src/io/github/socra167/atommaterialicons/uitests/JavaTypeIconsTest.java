package io.github.socra167.atommaterialicons.uitests;

import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.assertShows;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.createFile;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.createFolder;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.imageOfElement;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.processEvents;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.sameImage;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static io.github.socra167.atommaterialicons.uitests.ExplorerSupport.showExplorer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.github.socra167.atommaterialicons.Activator;
import io.github.socra167.atommaterialicons.preferences.PreferenceConstants;

/**
 * Checks that Java source files show the kind of type they declare.
 */
class JavaTypeIconsTest {

	/** JDT's "Java Type Indicator" decorator, which adds badges on top of the icons. */
	private static final String TYPE_INDICATOR = "org.eclipse.jdt.ui.interface.decorator";

	private IProject project;
	private CommonNavigator explorer;
	private boolean typeIndicatorEnabled;

	@BeforeEach
	void setUp() throws CoreException, PartInitException {
		// the project has no JRE, so the build reports errors; compare the icons without overlays
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(PreferenceConstants.PROBLEM_OVERLAYS, false);
		IDecoratorManager decorators = PlatformUI.getWorkbench().getDecoratorManager();
		typeIndicatorEnabled = decorators.getEnabled(TYPE_INDICATOR);
		decorators.setEnabled(TYPE_INDICATOR, false);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject("javademo");
		project.create(null);
		project.open(null);
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);

		IJavaProject javaProject = JavaCore.create(project);
		createFolder(project.getFolder("src/main/java"));
		createFolder(project.getFolder("src/test/java"));
		javaProject.setRawClasspath(new IClasspathEntry[] {
				JavaCore.newSourceEntry(project.getFullPath().append("src/main/java")),
				JavaCore.newSourceEntry(project.getFullPath().append("src/test/java"), null, null,
						project.getFullPath().append("bin-test"),
						new IClasspathAttribute[] { JavaCore.newClasspathAttribute(IClasspathAttribute.TEST, "true") }) },
				project.getFullPath().append("bin"), null);
		Map<String, String> options = javaProject.getOptions(false);
		JavaCore.setComplianceOptions("17", options);
		javaProject.setOptions(options);

		source("main", "Plain", "public class Plain {}");
		source("main", "Shape", "public abstract class Shape {}");
		source("main", "Service", "public interface Service {}");
		source("main", "Color", "public enum Color { RED }");
		source("main", "Point", "public record Point(int x, int y) {}");
		source("main", "Marker", "public @interface Marker {}");
		source("main", "AppException", "public class AppException extends RuntimeException {}");
		source("main", "BaseException", "public abstract class BaseException extends java.lang.Exception {}");
		source("test", "PlainTest", "public class PlainTest {}");
		source("test", "TestData", "public class TestData {}");
		createFile(project.getFile("Loose.java"), "public interface Loose {}");

		explorer = showExplorer();
	}

	@AfterEach
	void tearDown() throws CoreException {
		PlatformUI.getWorkbench().getDecoratorManager().setEnabled(TYPE_INDICATOR, typeIndicatorEnabled);
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.JAVA_TYPE_ICONS);
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).remove(PreferenceConstants.PROBLEM_OVERLAYS);
		project.delete(true, true, null);
	}

	@Test
	void showsTheKindOfTheDeclaredType() {
		assertShows(explorer, "icons/java/class.png", file("main", "Plain"));
		assertShows(explorer, "icons/java/abstractClass.png", file("main", "Shape"));
		assertShows(explorer, "icons/java/interface.png", file("main", "Service"));
		assertShows(explorer, "icons/java/enum.png", file("main", "Color"));
		assertShows(explorer, "icons/java/record.png", file("main", "Point"));
		assertShows(explorer, "icons/java/annotationtype.png", file("main", "Marker"));
		assertShows(explorer, "icons/java/exceptionClass.png", file("main", "AppException"));
		assertShows(explorer, "icons/java/abstractException.png", file("main", "BaseException"));
	}

	@Test
	void keepsTheIconsOfTheMembersOfJavaFiles() {
		IFile shape = file("main", "Shape");
		assertShows(explorer, "icons/java/abstractClass.png", shape);
		ICompilationUnit unit = (ICompilationUnit) JavaCore.create(shape);
		IType type = unit.getType("Shape");
		explorer.getCommonViewer().expandToLevel(unit, 1);
		processEvents();
		Image typeImage = imageOfElement(explorer, type);
		assertNotNull(typeImage);
		assertFalse(sameImage(typeImage, "icons/java/abstractClass.png"), "the type node keeps the JDT icon");
	}

	@Test
	void showsTestClassesInTestSources() {
		assertShows(explorer, "icons/java/test.png", file("test", "PlainTest"));
		// test helpers keep the class icon
		assertShows(explorer, "icons/java/class.png", file("test", "TestData"));
	}

	@Test
	void usesTheFileIconOutsideTheBuildPath() {
		assertShows(explorer, "icons/files/java.png", project.getFile("Loose.java"));
	}

	@Test
	void updatesWhenTheTypeChanges() throws CoreException {
		IFile plain = file("main", "Plain");
		plain.setContents(new ByteArrayInputStream("public interface Plain {}".getBytes(StandardCharsets.UTF_8)), true,
				false, null);
		processEvents();
		assertShows(explorer, "icons/java/interface.png", plain);
	}

	@Test
	void canBeTurnedOff() {
		InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID).putBoolean(PreferenceConstants.JAVA_TYPE_ICONS, false);
		processEvents();
		assertShows(explorer, "icons/files/java.png", file("main", "Service"));
	}

	private void source(String sourceSet, String name, String content) throws CoreException {
		createFile(file(sourceSet, name), "package demo;\n" + content + "\n");
	}

	private IFile file(String sourceSet, String name) {
		return project.getFile("src/" + sourceSet + "/java/demo/" + name + ".java");
	}
}

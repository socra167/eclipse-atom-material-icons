package io.github.socra167.atommaterialicons.navigator;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Chooses the icon of a Java source file from its main type, like IntelliJ IDEA does:
 * class, abstract class, interface, enum, record, annotation, exception or test.
 * <p>
 * This class references JDT and must only be loaded when JDT is installed, see
 * {@link MaterialIconLabelProvider}.
 * </p>
 */
final class JavaTypeIcons {

	static final String CLASS = "class"; //$NON-NLS-1$
	static final String ABSTRACT_CLASS = "abstractClass"; //$NON-NLS-1$
	static final String INTERFACE = "interface"; //$NON-NLS-1$
	static final String ENUM = "enum"; //$NON-NLS-1$
	static final String RECORD = "record"; //$NON-NLS-1$
	static final String ANNOTATION = "annotationtype"; //$NON-NLS-1$
	static final String EXCEPTION = "exceptionClass"; //$NON-NLS-1$
	static final String ABSTRACT_EXCEPTION = "abstractException"; //$NON-NLS-1$
	static final String TEST = "test"; //$NON-NLS-1$

	private static final String ICON_FOLDER = "icons/java/"; //$NON-NLS-1$
	private static final String JAVA_EXTENSION = "java"; //$NON-NLS-1$

	/** Super classes whose subclasses are shown as exceptions, by simple name. */
	private static final Pattern THROWABLE = Pattern.compile(".*(Exception|Error|Throwable)"); //$NON-NLS-1$

	/** Names of test classes, following the JUnit, Surefire and Failsafe conventions. */
	private static final Pattern TEST_NAME = Pattern.compile(".+(Test|Tests|TestCase|IT|ITCase)"); //$NON-NLS-1$

	/** Icon per file, valid while the modification stamp of the file is unchanged. */
	private static final Map<IFile, Cached> CACHE = new ConcurrentHashMap<>();

	private record Cached(long stamp, String icon) {
	}

	private JavaTypeIcons() {
	}

	/**
	 * @param element the element shown in the viewer, an {@link ICompilationUnit} or an {@link IFile}
	 * @param file    the file of the element
	 * @return the bundle relative icon path, or {@code null} if the file is not a Java source
	 *         file on a build path or declares no type
	 */
	static String resolve(Object element, IFile file) {
		if (!JAVA_EXTENSION.equalsIgnoreCase(file.getFileExtension())) {
			return null;
		}
		long stamp = file.getModificationStamp();
		Cached cached = CACHE.get(file);
		if (cached == null || cached.stamp() != stamp) {
			cached = new Cached(stamp, compute(element, file));
			CACHE.put(file, cached);
		}
		return cached.icon() == null ? null : ICON_FOLDER + cached.icon() + ".png"; //$NON-NLS-1$
	}

	private static String compute(Object element, IFile file) {
		ICompilationUnit unit = element instanceof ICompilationUnit cu ? cu : null;
		if (unit == null && JavaCore.create(file) instanceof ICompilationUnit cu) {
			unit = cu;
		}
		if (unit == null || !unit.exists()) {
			return null;
		}
		try {
			IType type = unit.findPrimaryType();
			if (type == null) {
				IType[] types = unit.getTypes();
				if (types.length == 0) {
					return null;
				}
				type = types[0];
			}
			return iconOf(type, unit);
		} catch (JavaModelException e) {
			// not parseable at the moment, use the file icon
			return null;
		}
	}

	private static String iconOf(IType type, ICompilationUnit unit) throws JavaModelException {
		if (type.isAnnotation()) {
			return ANNOTATION;
		}
		if (type.isInterface()) {
			return INTERFACE;
		}
		if (type.isEnum()) {
			return ENUM;
		}
		if (type.isRecord()) {
			return RECORD;
		}
		boolean isAbstract = Flags.isAbstract(type.getFlags());
		String superclass = type.getSuperclassName();
		if (superclass != null && THROWABLE.matcher(simpleName(superclass)).matches()) {
			return isAbstract ? ABSTRACT_EXCEPTION : EXCEPTION;
		}
		if (!isAbstract && TEST_NAME.matcher(type.getElementName()).matches() && isInTestSources(unit)) {
			return TEST;
		}
		return isAbstract ? ABSTRACT_CLASS : CLASS;
	}

	private static String simpleName(String name) {
		int generic = name.indexOf('<');
		String raw = generic < 0 ? name : name.substring(0, generic);
		return raw.substring(raw.lastIndexOf('.') + 1);
	}

	private static boolean isInTestSources(ICompilationUnit unit) throws JavaModelException {
		IPackageFragmentRoot root = (IPackageFragmentRoot) unit.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
		if (root == null) {
			return false;
		}
		IClasspathEntry entry = root.getRawClasspathEntry();
		if (entry != null && entry.isTest()) {
			return true;
		}
		// build paths that do not flag test sources, such as src/test/java
		IPath path = root.getPath();
		for (String segment : path.segments()) {
			if ("test".equalsIgnoreCase(segment) || "tests".equalsIgnoreCase(segment)) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		}
		return false;
	}

	/**
	 * @return whether a Java element stands for its file, or {@code null} if the element is not a
	 *         Java element
	 */
	static Boolean representsFile(Object element) {
		if (element instanceof IJavaElement) {
			return Boolean.valueOf(element instanceof ICompilationUnit);
		}
		return null;
	}

	/**
	 * @return the Java elements shown for the given resources, so that viewers showing Java
	 *         elements instead of resources update them too
	 */
	static Set<Object> javaElementsOf(Collection<IResource> resources) {
		Set<Object> elements = new LinkedHashSet<>();
		for (IResource resource : resources) {
			IJavaElement element = JavaCore.create(resource);
			if (element != null) {
				elements.add(element);
			}
		}
		return elements;
	}

	/**
	 * Forgets the icon of files that were changed or removed.
	 */
	static void forget(Collection<IResource> resources) {
		for (IResource resource : resources) {
			if (resource instanceof IFile file) {
				CACHE.remove(file);
			}
		}
	}

	/**
	 * Forgets all icons.
	 */
	static void clear() {
		CACHE.clear();
	}
}

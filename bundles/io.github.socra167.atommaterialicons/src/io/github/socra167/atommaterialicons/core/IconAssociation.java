package io.github.socra167.atommaterialicons.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Associates a file or folder name pattern with an icon.
 * <p>
 * Mirrors the regex associations of the Atom Material Icons plug-in: the pattern
 * is matched case-insensitively against the whole name, or against the whole
 * path when the pattern contains a {@code /}.
 * </p>
 */
public final class IconAssociation {

	private final String name;
	private final int priority;
	private final Pattern pattern;
	private final boolean pathPattern;
	private final String icon;
	private final String pack;
	private final int order;

	/**
	 * @param name     display name of the association
	 * @param priority higher priorities win over lower ones
	 * @param pattern  the regular expression to match
	 * @param icon     the icon base name, without extension
	 * @param pack     the optional icon pack this association belongs to, or {@code null}
	 * @param order    position in the source file, used to break priority ties
	 */
	public IconAssociation(String name, int priority, String pattern, String icon, String pack, int order) {
		this.name = Objects.requireNonNull(name);
		this.priority = priority;
		this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.pathPattern = pattern.contains("/"); //$NON-NLS-1$
		this.icon = Objects.requireNonNull(icon);
		this.pack = pack;
		this.order = order;
	}

	public String getName() {
		return name;
	}

	public int getPriority() {
		return priority;
	}

	public String getIcon() {
		return icon;
	}

	/**
	 * @return the icon pack this association belongs to, or {@code null} for the default icons
	 */
	public String getPack() {
		return pack;
	}

	/**
	 * @return {@code true} if the pattern is matched against the path instead of the name
	 */
	public boolean isPathPattern() {
		return pathPattern;
	}

	int getOrder() {
		return order;
	}

	/**
	 * @param fileName the file or folder name
	 * @param path     the full path, using {@code /} as separator
	 * @return {@code true} if this association applies
	 */
	public boolean matches(String fileName, String path) {
		String target = pathPattern ? path : fileName;
		return target != null && pattern.matcher(target).matches();
	}

	/**
	 * @return {@code true} if this association should be preferred over {@code other}
	 */
	boolean beats(IconAssociation other) {
		if (other == null) {
			return true;
		}
		if (priority != other.priority) {
			return priority > other.priority;
		}
		return order < other.order;
	}

	@Override
	public String toString() {
		return name + " -> " + icon; //$NON-NLS-1$
	}
}

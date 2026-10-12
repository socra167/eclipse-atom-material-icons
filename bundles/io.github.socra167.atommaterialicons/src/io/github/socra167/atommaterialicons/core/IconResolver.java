package io.github.socra167.atommaterialicons.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Predicate;

/**
 * Resolves the icon of files and folders from the bundled Atom Material Icons associations.
 * <p>
 * Icons are returned as bundle relative paths of the 16x16 PNG, for example
 * {@code icons/files/java.png}. A {@code @2x} variant exists next to each of them.
 * </p>
 */
public final class IconResolver {

	public static final String FILE_ASSOCIATIONS = "associations/files.xml"; //$NON-NLS-1$
	public static final String FOLDER_ASSOCIATIONS = "associations/folders.xml"; //$NON-NLS-1$

	private static final String FILE_ICONS = "icons/files/"; //$NON-NLS-1$
	private static final String FOLDER_ICONS = "icons/folders/"; //$NON-NLS-1$
	private static final String EXTENSION = ".png"; //$NON-NLS-1$

	private final AssociationSet files;
	private final AssociationSet folders;

	public IconResolver(AssociationSet files, AssociationSet folders) {
		this.files = files;
		this.folders = folders;
	}

	/**
	 * Loads the associations bundled with the plug-in through the given class loader.
	 */
	public static IconResolver load(ClassLoader loader) throws IOException {
		return new IconResolver(load(loader, FILE_ASSOCIATIONS), load(loader, FOLDER_ASSOCIATIONS));
	}

	private static AssociationSet load(ClassLoader loader, String resource) throws IOException {
		try (InputStream in = loader.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("Missing resource " + resource); //$NON-NLS-1$
			}
			return AssociationSet.load(in);
		}
	}

	/**
	 * @param filter returns {@code true} for enabled icon packs
	 */
	public void setPackFilter(Predicate<String> filter) {
		files.setPackFilter(filter);
		folders.setPackFilter(filter);
	}

	/**
	 * @return the icon path for the file, or {@code null} if no association matches
	 */
	public String resolveFile(String fileName, String path) {
		IconAssociation association = files.find(fileName, path);
		return association == null ? null : FILE_ICONS + association.getIcon() + EXTENSION;
	}

	/**
	 * @param useDefault whether to return the generic folder icon when no association matches
	 * @return the icon path for the folder, or {@code null}
	 */
	public String resolveFolder(String folderName, String path, boolean useDefault) {
		IconAssociation association = folders.find(folderName, path);
		if (association != null) {
			return FOLDER_ICONS + association.getIcon() + EXTENSION;
		}
		String defaultIcon = folders.getDefaultIcon();
		return useDefault && defaultIcon != null ? FOLDER_ICONS + defaultIcon + EXTENSION : null;
	}

	public AssociationSet getFileAssociations() {
		return files;
	}

	public AssociationSet getFolderAssociations() {
		return folders;
	}
}

package io.github.socra167.atommaterialicons.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An ordered set of {@link IconAssociation}s, loaded from one of the generated
 * association files, with a cache for name based lookups.
 */
public final class AssociationSet {

	private final List<IconAssociation> nameAssociations;
	private final List<IconAssociation> pathAssociations;
	private final String defaultIcon;
	private final Map<String, Optional<IconAssociation>> nameCache = new ConcurrentHashMap<>();
	private volatile Predicate<String> packFilter = pack -> true;

	AssociationSet(List<IconAssociation> associations, String defaultIcon) {
		List<IconAssociation> byName = new ArrayList<>();
		List<IconAssociation> byPath = new ArrayList<>();
		for (IconAssociation association : associations) {
			(association.isPathPattern() ? byPath : byName).add(association);
		}
		this.nameAssociations = Collections.unmodifiableList(byName);
		this.pathAssociations = Collections.unmodifiableList(byPath);
		this.defaultIcon = defaultIcon == null || defaultIcon.isEmpty() ? null : defaultIcon;
	}

	/**
	 * Reads an association file.
	 *
	 * @param in the XML content; not closed by this method
	 * @return the loaded associations
	 * @throws IOException if the content cannot be read or parsed
	 */
	public static AssociationSet load(InputStream in) throws IOException {
		Document document;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); //$NON-NLS-1$
			factory.setExpandEntityReferences(false);
			DocumentBuilder builder = factory.newDocumentBuilder();
			document = builder.parse(in);
		} catch (ParserConfigurationException | SAXException e) {
			throw new IOException("Cannot parse icon associations", e); //$NON-NLS-1$
		}

		Element root = document.getDocumentElement();
		NodeList nodes = root.getElementsByTagName("association"); //$NON-NLS-1$
		List<IconAssociation> associations = new ArrayList<>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			String pack = element.getAttribute("pack"); //$NON-NLS-1$
			int priority;
			try {
				priority = Integer.parseInt(element.getAttribute("priority")); //$NON-NLS-1$
			} catch (NumberFormatException e) {
				priority = 0;
			}
			associations.add(new IconAssociation(
					element.getAttribute("name"), //$NON-NLS-1$
					priority,
					element.getAttribute("pattern"), //$NON-NLS-1$
					element.getAttribute("icon"), //$NON-NLS-1$
					pack.isEmpty() ? null : pack,
					i));
		}
		return new AssociationSet(associations, root.getAttribute("default")); //$NON-NLS-1$
	}

	/**
	 * Restricts the icon packs that may be used. Associations without a pack are always used.
	 *
	 * @param filter returns {@code true} for enabled icon packs
	 */
	public void setPackFilter(Predicate<String> filter) {
		this.packFilter = filter;
		nameCache.clear();
	}

	/**
	 * Finds the best association for a file or folder.
	 *
	 * @param fileName the file or folder name
	 * @param path     the full path using {@code /} as separator, may be {@code null}
	 * @return the matching association with the highest priority, or {@code null}
	 */
	public IconAssociation find(String fileName, String path) {
		IconAssociation best = nameCache.computeIfAbsent(fileName, this::findByName).orElse(null);
		if (path != null) {
			Predicate<String> filter = packFilter;
			for (IconAssociation association : pathAssociations) {
				if (association.beats(best) && isEnabled(association, filter) && association.matches(fileName, path)) {
					best = association;
				}
			}
		}
		return best;
	}

	private Optional<IconAssociation> findByName(String fileName) {
		Predicate<String> filter = packFilter;
		IconAssociation best = null;
		for (IconAssociation association : nameAssociations) {
			if (association.beats(best) && isEnabled(association, filter) && association.matches(fileName, null)) {
				best = association;
			}
		}
		return Optional.ofNullable(best);
	}

	private static boolean isEnabled(IconAssociation association, Predicate<String> filter) {
		return association.getPack() == null || filter.test(association.getPack());
	}

	/**
	 * @return the icon to use when nothing matches, or {@code null} if there is none
	 */
	public String getDefaultIcon() {
		return defaultIcon;
	}

	/**
	 * @return all associations, name based ones first
	 */
	public List<IconAssociation> getAssociations() {
		List<IconAssociation> all = new ArrayList<>(nameAssociations);
		all.addAll(pathAssociations);
		return all;
	}

	/**
	 * @return the number of associations
	 */
	public int size() {
		return nameAssociations.size() + pathAssociations.size();
	}

	@Override
	public String toString() {
		return "AssociationSet[" + size() + " associations, default=" + defaultIcon + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
}

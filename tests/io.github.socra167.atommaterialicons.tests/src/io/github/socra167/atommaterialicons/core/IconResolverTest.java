package io.github.socra167.atommaterialicons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IconResolverTest {

	private IconResolver resolver;

	@BeforeEach
	void setUp() throws IOException {
		resolver = IconResolver.load(IconResolver.class.getClassLoader());
	}

	@Test
	void loadsBundledAssociations() {
		assertTrue(resolver.getFileAssociations().size() > 1000);
		assertTrue(resolver.getFolderAssociations().size() > 100);
	}

	@Test
	void resolvesFilesByExtension() {
		assertEquals("icons/files/java.png", resolver.resolveFile("Foo.java", "/ws/p/src/Foo.java"));
		assertEquals("icons/files/java.png", resolver.resolveFile("FOO.JAVA", null));
	}

	@Test
	void resolvesWellKnownFileNames() {
		assertEquals("icons/files/maven.png", resolver.resolveFile("pom.xml", "/ws/p/pom.xml"));
		assertEquals("icons/files/npm.png", resolver.resolveFile("package.json", null));
		assertEquals("icons/files/gitignore.png", resolver.resolveFile(".gitignore", null));
		assertEquals("icons/files/docker.png", resolver.resolveFile("Dockerfile", null));
		assertEquals("icons/files/gradlekts.png", resolver.resolveFile("build.gradle.kts", null));
	}

	@Test
	void higherPriorityWins() {
		// matches both the generic XML association and the higher priority plug-in association
		assertEquals("icons/files/plugin.png", resolver.resolveFile("plugin.xml", "/ws/p/plugin.xml"));
	}

	@Test
	void unknownFilesHaveNoIcon() {
		assertNull(resolver.resolveFile("file.zzqq", "/ws/p/file.zzqq"));
	}

	@Test
	void iconPacksCanBeDisabled() {
		assertEquals("icons/files/angularcomponent.png", resolver.resolveFile("app.component.ts", null));
		resolver.setPackFilter(pack -> false);
		assertEquals("icons/files/typeScript.png", resolver.resolveFile("app.component.ts", null));
		resolver.setPackFilter(pack -> true);
		assertEquals("icons/files/angularcomponent.png", resolver.resolveFile("app.component.ts", null));
	}

	@Test
	void resolvesFolders() {
		assertEquals("icons/folders/src.png", resolver.resolveFolder("src", "/ws/p/src", true));
		assertEquals("icons/folders/git.png", resolver.resolveFolder(".git", "/ws/p/.git", true));
		assertEquals("icons/folders/node.png", resolver.resolveFolder("node_modules", null, true));
	}

	@Test
	void unknownFoldersUseTheDefaultIconOnlyWhenRequested() {
		assertEquals("icons/folders/folder.png", resolver.resolveFolder("unknownzz", null, true));
		assertNull(resolver.resolveFolder("unknownzz", null, false));
	}

	@Test
	void everyAssociationHasItsIcons() {
		assertIconsExist("files", resolver.getFileAssociations());
		assertIconsExist("folders", resolver.getFolderAssociations());
		assertNotNull(resolver.getFolderAssociations().getDefaultIcon());
	}

	private static void assertIconsExist(String kind, AssociationSet set) {
		ClassLoader loader = IconResolver.class.getClassLoader();
		List<String> icons = new ArrayList<>();
		for (IconAssociation association : set.getAssociations()) {
			icons.add(association.getIcon());
		}
		if (set.getDefaultIcon() != null) {
			icons.add(set.getDefaultIcon());
		}
		for (String icon : icons) {
			String base = "icons/" + kind + "/" + icon;
			assertNotNull(loader.getResource(base + ".png"), base);
			assertNotNull(loader.getResource(base + "@2x.png"), base);
		}
	}

	@Test
	void tiesAreBrokenByDocumentOrder() throws IOException {
		String xml = "<associations kind=\"files\">"
				+ "<association name=\"First\" priority=\"5\" pattern=\".*\\.txt$\" icon=\"first\"/>"
				+ "<association name=\"Second\" priority=\"5\" pattern=\".*\\.txt$\" icon=\"second\"/>"
				+ "<association name=\"Path\" priority=\"5\" pattern=\".*/docs/.*\\.txt$\" icon=\"path\"/>"
				+ "<association name=\"Higher path\" priority=\"6\" pattern=\".*/notes/.*\\.txt$\" icon=\"notes\"/>"
				+ "</associations>";
		AssociationSet set;
		try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
			set = AssociationSet.load(in);
		}
		assertEquals("first", set.find("a.txt", "/x/docs/a.txt").getIcon());
		assertEquals("notes", set.find("a.txt", "/x/notes/a.txt").getIcon());
		assertNull(set.getDefaultIcon());
	}
}

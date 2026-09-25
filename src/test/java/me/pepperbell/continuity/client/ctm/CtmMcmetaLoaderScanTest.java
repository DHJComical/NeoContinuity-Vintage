package me.pepperbell.continuity.client.ctm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CtmMcmetaLoaderScanTest {
	@TempDir
	Path tempDir;

	@Test
	void baseResourcesDirectoryMapsContentTweakerMetadataToItsResourceNamespace() throws Exception {
		Path resources = Files.createDirectory(tempDir.resolve("resources"));
		Path metadata = resources.resolve("contenttweaker/textures/blocks/glass.png.mcmeta");
		Files.createDirectories(metadata.getParent());
		Files.writeString(metadata, "{}");

		List<String> found = new ArrayList<>();
		CtmMcmetaLoader.scanDirectory(resources, (namespace, path) -> found.add(namespace + ":" + path));

		assertEquals(List.of("contenttweaker:textures/blocks/glass.png.mcmeta"), found);
	}

	@Test
	void normalPackKeepsAssetsMappingAndIgnoresUnmappedContentTweakerFolder() throws Exception {
		Path pack = Files.createDirectory(tempDir.resolve("pack"));
		Path metadata = pack.resolve("assets/contenttweaker/textures/blocks/glass.png.mcmeta");
		Files.createDirectories(metadata.getParent());
		Files.writeString(metadata, "{}");
		Path unmapped = pack.resolve("contenttweaker/textures/blocks/stray.png.mcmeta");
		Files.createDirectories(unmapped.getParent());
		Files.writeString(unmapped, "{}");

		List<String> found = new ArrayList<>();
		CtmMcmetaLoader.scanDirectory(pack, (namespace, path) -> found.add(namespace + ":" + path));

		assertEquals(List.of("contenttweaker:textures/blocks/glass.png.mcmeta"), found);
	}

	@Test
	void resourceLoaderPacksDiscoverNamespacedMetadataWithoutAssetsDirectory() throws Exception {
		for (String folder : List.of("resources", "oresources")) {
			Path root = Files.createDirectory(tempDir.resolve(folder));
			Path metadata = root.resolve("minecraft/textures/blocks/stone.png.mcmeta");
			Files.createDirectories(metadata.getParent());
			Files.writeString(metadata, "{}");
		}
		List<String> found = new ArrayList<>();
		for (Path root : CtmMcmetaLoader.externalRoots(tempDir, Set.of(), true)) {
			CtmMcmetaLoader.scanDirectory(root, (namespace, path) -> found.add(namespace + ":" + path));
		}
		assertEquals(List.of("minecraft:textures/blocks/stone.png.mcmeta",
				"minecraft:textures/blocks/stone.png.mcmeta"), found);
		assertEquals(List.of(tempDir.resolve("oresources")),
				CtmMcmetaLoader.externalRoots(tempDir, Set.of(tempDir.resolve("resources").toAbsolutePath().normalize()), true));
		assertEquals(List.of(tempDir.resolve("resources")),
				CtmMcmetaLoader.externalRoots(tempDir, Set.of(), false));
	}
}

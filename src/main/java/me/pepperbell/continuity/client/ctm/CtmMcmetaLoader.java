package me.pepperbell.continuity.client.ctm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

/**
 * Scans resource packs for CTM Mod format metadata: every {@code assets/<ns>/<path>.png.mcmeta}
 * whose mcmeta has a {@code "ctm"} section becomes a {@link CtmDefinition}.
 * <p>
 * Runs alongside (and independently of) the OptiFine {@code optifine/ctm/*.properties} loader.
 */
public final class CtmMcmetaLoader {
	private final IResourceManager resourceManager;
	private final List<CtmDefinition> properties = new ObjectArrayList<>();

	private CtmMcmetaLoader(IResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public static List<CtmDefinition> loadAll() {
		// Custom logic types (ctm.json + ctm_logic/*.json) must be loaded before mcmeta parsing,
		// since a mcmeta may reference a namespaced custom type.
		CtmDefinitionManager.reload();
		CtmMcmetaLoader loader = new CtmMcmetaLoader(Minecraft.getMinecraft().getResourceManager());
		loader.loadAllPacks();
		loader.properties.sort(null);
		return loader.properties;
	}

	private void loadAllPacks() {
		int packPriority = 0;
		Set<String> seenPacks = new HashSet<>();

		for (IResourcePack pack : FMLClientHandler.instance().getResourcePackList()) {
			if (seenPacks.add(pack.getPackName())) {
				loadAll(pack, packPriority++);
			}
		}

		ResourcePackRepository repository = Minecraft.getMinecraft().getResourcePackRepository();
		for (ResourcePackRepository.Entry entry : repository.getRepositoryEntries()) {
			if (seenPacks.add(entry.getResourcePackName())) {
				loadAll(entry.getResourcePack(), packPriority++);
			}
		}

		IResourcePack serverPack = repository.getServerResourcePack();
		if (serverPack != null && seenPacks.add(serverPack.getPackName())) {
			loadAll(serverPack, packPriority++);
		}

		ContinuityClient.LOGGER.debug("Loaded {} CTM Mod metadata definitions from {} packs", properties.size(), seenPacks.size());
	}

	private void loadAll(IResourcePack pack, int packPriority) {
		int[] count = new int[1];
		scanPack(pack, (namespace, path) -> {
			if (!path.endsWith(".mcmeta") || !path.contains("/")) {
				return;
			}
			String texturePath = path.substring(0, path.length() - ".mcmeta".length());
			if (!texturePath.endsWith(".png")) {
				return;
			}
			// The block-atlas sprite id is the file path relative to textures/, without the .png
			// extension (e.g. "blocks/glass"); the mcmeta resource lives at textures/<id>.png.mcmeta.
			String spriteIdPath = texturePath.startsWith("textures/")
					? texturePath.substring("textures/".length(), texturePath.length() - ".png".length())
					: texturePath.substring(0, texturePath.length() - ".png".length());
			ResourceLocation baseTextureId = new ResourceLocation(namespace, spriteIdPath);
				try {
					IResource resource = resourceManager.getResource(new ResourceLocation(namespace, texturePath + ".mcmeta"));
					CtmDefinition parsed = CtmMcmetaParser.parse(baseTextureId, resource, pack.getPackName(), packPriority);
					if (parsed != null) {
						// Proxy: the proxy target's definition replaces this one (the base texture
						// behaves as the proxied texture).
						if (parsed.getProxy() != null) {
							ResourceLocation proxyId = new ResourceLocation(parsed.getProxy());
							String proxyPath = "textures/" + proxyId.getPath() + ".png.mcmeta";
							try {
								IResource proxyResource = resourceManager.getResource(new ResourceLocation(proxyId.getNamespace(), proxyPath));
								CtmDefinition proxyDef = CtmMcmetaParser.parse(parsed.getResourceId(), proxyResource, pack.getPackName(), packPriority);
								if (proxyDef != null) {
									CtmMcmetaParser.overrideBaseTexture(proxyDef, proxyId);
									parsed = proxyDef;
								}
							} catch (Exception e) {
								ContinuityClient.LOGGER.warn("Failed to resolve CTM proxy '" + parsed.getProxy() + "' for '" + baseTextureId + "'", e);
							}
						}
						properties.add(parsed);
						count[0]++;
					}
				} catch (Exception e) {
					ContinuityClient.LOGGER.error("Failed to load CTM metadata from '" + namespace + ":" + texturePath + "' in pack '" + pack.getPackName() + "'", e);
				}
		});
		ContinuityClient.LOGGER.debug("Loaded {} CTM Mod definitions in pack '{}'", count[0], pack.getPackName());
	}

	private static void scanPack(IResourcePack pack, ScanConsumer consumer) {
		if (!(pack instanceof AbstractResourcePack abstractPack)) {
			ContinuityClient.LOGGER.debug("Skipping non-abstract resource pack '{}' while scanning CTM Mod metadata", pack.getPackName());
			return;
		}

		File file = abstractPack.getResourcePackFile();
		if (file.isDirectory()) {
			scanDirectory(file.toPath(), consumer);
		} else if (file.isFile()) {
			scanZip(file, consumer);
		}
	}

	private static void scanDirectory(Path root, ScanConsumer consumer) {
		try (var stream = Files.walk(root)) {
			stream.filter(Files::isRegularFile).forEach(path -> {
				String relative = root.relativize(path).toString().replace('\\', '/');
				ResourcePackPath resourcePath = parseResourcePackPath(relative);
				if (resourcePath != null) {
					consumer.accept(resourcePath.namespace(), resourcePath.path());
				}
			});
		} catch (Exception e) {
			ContinuityClient.LOGGER.error("Failed to scan CTM Mod metadata in folder pack '" + root + "'", e);
		}
	}

	private static void scanZip(File file, ScanConsumer consumer) {
		try (ZipFile zipFile = new ZipFile(file)) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					continue;
				}
				ResourcePackPath resourcePath = parseResourcePackPath(entry.getName());
				if (resourcePath != null) {
					consumer.accept(resourcePath.namespace(), resourcePath.path());
				}
			}
		} catch (Exception e) {
			ContinuityClient.LOGGER.error("Failed to scan CTM Mod metadata in zip pack '" + file + "'", e);
		}
	}

	@Nullable
	private static ResourcePackPath parseResourcePackPath(String relative) {
		if (!relative.startsWith("assets/")) {
			return null;
		}
		String rest = relative.substring("assets/".length());
		int slash = rest.indexOf('/');
		if (slash <= 0 || slash >= rest.length() - 1) {
			return null;
		}
		String namespace = rest.substring(0, slash);
		String path = rest.substring(slash + 1);
		// Exclude our own reserved directory to avoid double-processing
		if (path.startsWith("optifine/")) {
			return null;
		}
		return new ResourcePackPath(namespace, path);
	}

	private record ResourcePackPath(String namespace, String path) {
	}

	@FunctionalInterface
	private interface ScanConsumer {
		void accept(String namespace, String path);
	}
}

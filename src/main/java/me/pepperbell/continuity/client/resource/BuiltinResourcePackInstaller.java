package me.pepperbell.continuity.client.resource;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.settings.GameSettings;

public final class BuiltinResourcePackInstaller {
	private static final String[] PACK_NAMES = {
			"default",
			"glass_pane_culling_fix",
	};

	private BuiltinResourcePackInstaller() {
	}

	public static void install() {
		Minecraft minecraft = Minecraft.getMinecraft();
		if (minecraft == null) {
			ContinuityClient.LOGGER.warn("Cannot install built-in resource packs before Minecraft is initialized");
			return;
		}

		ResourcePackRepository repository = minecraft.getResourcePackRepository();
		for (String packName : PACK_NAMES) {
			copyPack(packName, repository.getDirResourcepacks());
		}
		repository.updateRepositoryEntriesAll();

		List<ResourcePackRepository.Entry> selectedPacks = new ArrayList<>(repository.getRepositoryEntries());
		Set<String> selectedNames = new HashSet<>();
		for (ResourcePackRepository.Entry entry : selectedPacks) {
			selectedNames.add(entry.getResourcePackName());
		}
		for (ResourcePackRepository.Entry entry : repository.getRepositoryEntriesAll()) {
			if (isBuiltinPack(entry.getResourcePackName()) && !selectedNames.contains(entry.getResourcePackName())) {
				selectedPacks.add(entry);
			}
		}
		repository.setRepositories(selectedPacks);

		GameSettings settings = minecraft.gameSettings;
		for (String packName : PACK_NAMES) {
			if (!settings.resourcePacks.contains(packName)) {
				settings.resourcePacks.add(packName);
			}
		}
		settings.saveOptions();
		minecraft.scheduleResourcesRefresh();
	}

	private static boolean isBuiltinPack(String packName) {
		for (String builtinName : PACK_NAMES) {
			if (builtinName.equals(packName)) {
				return true;
			}
		}
		return false;
	}

	private static void copyPack(String packName, File destinationDir) {
		URL url = BuiltinResourcePackInstaller.class.getClassLoader().getResource("resourcepacks/" + packName + "/pack.mcmeta");
		if (url == null) {
			ContinuityClient.LOGGER.error("Could not find bundled resource pack '{}'", packName);
			return;
		}

		Path target = new File(destinationDir, packName).toPath();
		try {
			if ("file".equals(url.getProtocol())) {
				copyDirectory(Paths.get(url.toURI()).getParent(), target);
			} else if ("jar".equals(url.getProtocol())) {
				copyZip(url, packName, target);
			} else {
				ContinuityClient.LOGGER.error("Unsupported resource pack URL '{}' for pack '{}'", url, packName);
			}
		} catch (Exception e) {
			ContinuityClient.LOGGER.error("Failed to install bundled resource pack '{}'", packName, e);
		}
	}

	private static void copyDirectory(Path source, Path target) throws Exception {
		try (Stream<Path> stream = Files.walk(source)) {
			for (Path path : stream.toList()) {
				Path relative = source.relativize(path);
				Path destination = target.resolve(relative.toString());
				if (Files.isDirectory(path)) {
					Files.createDirectories(destination);
				} else {
					Files.createDirectories(destination.getParent());
					Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private static void copyZip(URL url, String packName, Path target) throws Exception {
		String spec = url.toExternalForm();
		int separatorIndex = spec.indexOf("!/");
		if (separatorIndex < 0) {
			throw new IllegalArgumentException("Invalid jar resource URL: " + url);
		}
		URI jarUri = new URI(spec.substring("jar:".length(), separatorIndex));
		String prefix = "resourcepacks/" + packName + "/";
		try (ZipFile zipFile = new ZipFile(new File(jarUri))) {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory() || !entry.getName().startsWith(prefix)) {
					continue;
				}
				Path destination = target.resolve(entry.getName().substring(prefix.length()));
				Files.createDirectories(destination.getParent());
				try (InputStream inputStream = zipFile.getInputStream(entry)) {
					Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}
}

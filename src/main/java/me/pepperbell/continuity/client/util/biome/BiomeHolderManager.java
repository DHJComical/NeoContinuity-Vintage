package me.pepperbell.continuity.client.util.biome;

import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.world.biome.Biome;

public final class BiomeHolderManager {
	private static final Map<ResourceLocation, BiomeHolder> HOLDER_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Set<Runnable> REFRESH_CALLBACKS = new ReferenceOpenHashSet<>();

	private BiomeHolderManager() {
	}

	public static BiomeHolder getOrCreateHolder(ResourceLocation id) {
		return HOLDER_CACHE.computeIfAbsent(id, BiomeHolder::new);
	}

	public static void addRefreshCallback(Runnable callback) {
		REFRESH_CALLBACKS.add(callback);
	}

	public static void init() {
		refreshHolders();
	}

	public static void refreshHolders() {
		RegistryNamespaced<ResourceLocation, Biome> biomeRegistry = Biome.REGISTRY;
		Map<ResourceLocation, ResourceLocation> compactIdMap = new Object2ObjectOpenHashMap<>();
		for (ResourceLocation id : biomeRegistry.getKeys()) {
			String path = id.getPath();
			String compactPath = path.replace("_", "");
			if (!path.equals(compactPath)) {
				ResourceLocation compactId = new ResourceLocation(id.getNamespace(), compactPath);
				if (!biomeRegistry.containsKey(compactId)) {
					compactIdMap.put(compactId, id);
				}
			}
		}

		for (BiomeHolder holder : HOLDER_CACHE.values()) {
			holder.refresh(biomeRegistry, compactIdMap);
		}

		for (Runnable callback : REFRESH_CALLBACKS) {
			callback.run();
		}
	}

	public static void clearCache() {
		HOLDER_CACHE.clear();
		REFRESH_CALLBACKS.clear();
	}
}

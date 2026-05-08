package me.pepperbell.continuity.client.util.biome;

import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
// import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

public final class BiomeHolderManager {
	private static final Map<Identifier, BiomeHolder> HOLDER_CACHE = new Object2ObjectOpenHashMap<>();
	private static final Set<Runnable> REFRESH_CALLBACKS = new ReferenceOpenHashSet<>();

	@Nullable
	/* private */ public static RegistryAccess registryManager;

	public static BiomeHolder getOrCreateHolder(Identifier id) {
		return HOLDER_CACHE.computeIfAbsent(id, BiomeHolder::new);
	}

	public static void addRefreshCallback(Runnable callback) {
		REFRESH_CALLBACKS.add(callback);
	}

	public static void init() {
		/* ClientPlayConnectionEvents.JOIN.register(((handler, sender, client) -> {
			registryManager = handler.registryAccess();
			refreshHolders();
		})); */
	}

	public static void refreshHolders() {
		if (registryManager == null) {
			return;
		}

		Map<Identifier, Identifier> compactIdMap = new Object2ObjectOpenHashMap<>();
		Registry<Biome> biomeRegistry = registryManager.lookupOrThrow(Registries.BIOME);
		for (Identifier id : biomeRegistry.keySet()) {
			String path = id.getPath();
			String compactPath = path.replace("_", "");
			if (!path.equals(compactPath)) {
				Identifier compactId = id.withPath(compactPath);
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

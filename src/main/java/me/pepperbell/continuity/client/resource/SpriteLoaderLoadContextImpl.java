package me.pepperbell.continuity.client.resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

public class SpriteLoaderLoadContextImpl implements SpriteLoaderLoadContext {
	private final CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture;
	private final Map<Identifier, CompletableFuture<Set<Identifier>>> extraIdsFutures = new Object2ObjectOpenHashMap<>();
	private final SpriteLoaderLoadContext.EmissiveControl blockAtlasEmissiveControl;
	private final SpriteLoaderLoadContext.EmissiveControl itemAtlasEmissiveControl;

	public SpriteLoaderLoadContextImpl(CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture, CompletableFuture<Boolean> blockAtlasHasEmissivesFuture) {
		this.allExtraIdsFuture = allExtraIdsFuture;
		blockAtlasEmissiveControl = new EmissiveControlImpl(blockAtlasHasEmissivesFuture);
		itemAtlasEmissiveControl = new EmissiveControlImpl();
	}

	@Override
	public CompletableFuture<@Nullable Set<Identifier>> getExtraIdsFuture(Identifier atlasId) {
		return extraIdsFutures.computeIfAbsent(atlasId, id -> allExtraIdsFuture.thenApply(allExtraIds -> allExtraIds.get(id)));
	}

	@Override
	@Nullable
	public SpriteLoaderLoadContext.EmissiveControl getEmissiveControl(Identifier atlasId) {
		if (atlasId.equals(TextureAtlas.LOCATION_BLOCKS)) {
			return blockAtlasEmissiveControl;
		} else if (atlasId.equals(TextureAtlas.LOCATION_ITEMS)) {
			return itemAtlasEmissiveControl;
		}
		return null;
	}

	private static class EmissiveControlImpl implements SpriteLoaderLoadContext.EmissiveControl {
		@Nullable
		private volatile Map<Identifier, Identifier> emissiveIdMap;
		@Nullable
		private final CompletableFuture<Boolean> hasEmissivesFuture;

		public EmissiveControlImpl(@Nullable CompletableFuture<Boolean> hasEmissivesFuture) {
			this.hasEmissivesFuture = hasEmissivesFuture;
		}

		public EmissiveControlImpl() {
			this(null);
		}

		@Override
		@Nullable
		public Map<Identifier, Identifier> getEmissiveIdMap() {
			return emissiveIdMap;
		}

		@Override
		public void setEmissiveIdMap(Map<Identifier, Identifier> emissiveIdMap) {
			if (emissiveIdMap.isEmpty()) {
				if (hasEmissivesFuture != null) {
					hasEmissivesFuture.complete(false);
				}
			} else {
				this.emissiveIdMap = emissiveIdMap;
			}
		}

		@Override
		public void setHasEmissives(boolean hasEmissives) {
			if (hasEmissivesFuture != null) {
				hasEmissivesFuture.complete(hasEmissives);
			}
		}
	}
}

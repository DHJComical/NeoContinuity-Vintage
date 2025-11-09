package me.pepperbell.continuity.client.resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.util.Identifier;

public class SpriteLoaderLoadContextImpl implements SpriteLoaderLoadContext {
	private final CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture;
	private final Map<Identifier, CompletableFuture<Set<Identifier>>> extraIdsFutures = new Object2ObjectOpenHashMap<>();
	private final SpriteLoaderLoadContext.EmissiveControl blockAtlasEmissiveControl;

	public SpriteLoaderLoadContextImpl(CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture, CompletableFuture<Boolean> blockAtlasHasEmissivesFuture) {
		this.allExtraIdsFuture = allExtraIdsFuture;
		blockAtlasEmissiveControl = new EmissiveControlImpl(blockAtlasHasEmissivesFuture);
	}

	@Override
	public CompletableFuture<@Nullable Set<Identifier>> getExtraIdsFuture(Identifier atlasId) {
		return extraIdsFutures.computeIfAbsent(atlasId, id -> allExtraIdsFuture.thenApply(allExtraIds -> allExtraIds.get(id)));
	}

	@Override
	@Nullable
	public SpriteLoaderLoadContext.EmissiveControl getEmissiveControl(Identifier atlasId) {
		if (atlasId.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
			return blockAtlasEmissiveControl;
		}
		return null;
	}

	private static class EmissiveControlImpl implements SpriteLoaderLoadContext.EmissiveControl {
		@Nullable
		private volatile Map<Identifier, Identifier> emissiveIdMap;
		private final CompletableFuture<Boolean> hasEmissivesFuture;

		public EmissiveControlImpl(CompletableFuture<Boolean> hasEmissivesFuture) {
			this.hasEmissivesFuture = hasEmissivesFuture;
		}

		@Override
		@Nullable
		public Map<Identifier, Identifier> getEmissiveIdMap() {
			return emissiveIdMap;
		}

		@Override
		public void setEmissiveIdMap(Map<Identifier, Identifier> emissiveIdMap) {
			if (emissiveIdMap.isEmpty()) {
				hasEmissivesFuture.complete(false);
			} else {
				this.emissiveIdMap = emissiveIdMap;
			}
		}

		@Override
		public void setHasEmissives(boolean hasEmissives) {
			hasEmissivesFuture.complete(hasEmissives);
		}
	}
}

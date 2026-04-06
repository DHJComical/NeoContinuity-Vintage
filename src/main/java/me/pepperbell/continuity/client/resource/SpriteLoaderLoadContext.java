package me.pepperbell.continuity.client.resource;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

public interface SpriteLoaderLoadContext {
	ThreadLocal<SpriteLoaderLoadContext> THREAD_LOCAL = new ThreadLocal<>();

	@Nullable
	CompletableFuture<Set<Identifier>> getExtraIdsFuture(Identifier atlasId);

	@Nullable
	EmissiveControl getEmissiveControl(Identifier atlasId);

	interface EmissiveControl {
		@Nullable
		Map<Identifier, Identifier> getEmissiveIdMap();

		void setEmissiveIdMap(Map<Identifier, Identifier> emissiveIdMap);

		void setHasEmissives(boolean hasEmissives);
	}
}

package me.pepperbell.continuity.client.resource;

import java.util.Map;

import net.minecraft.resources.Identifier;

public interface SpriteLoaderStitchContext {
	ThreadLocal<SpriteLoaderStitchContext> THREAD_LOCAL = new ThreadLocal<>();

	Map<Identifier, Identifier> getEmissiveIdMap();

	void setHasEmissives(boolean hasEmissives);
}

package me.pepperbell.continuity.client.resource;

import java.util.Map;

import net.minecraft.util.Identifier;

public interface AtlasLoaderLoadContext {
	ThreadLocal<AtlasLoaderLoadContext> THREAD_LOCAL = new ThreadLocal<>();

	void setEmissiveIdMap(Map<Identifier, Identifier> map);
}

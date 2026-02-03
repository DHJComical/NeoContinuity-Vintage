package me.pepperbell.continuity.client.resource;

import java.util.Map;

import net.minecraft.resources.Identifier;

public interface SpriteSourceListListContext {
	ThreadLocal<SpriteSourceListListContext> THREAD_LOCAL = new ThreadLocal<>();

	void setEmissiveIdMap(Map<Identifier, Identifier> map);
}

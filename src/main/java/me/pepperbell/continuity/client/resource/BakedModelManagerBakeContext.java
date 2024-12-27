package me.pepperbell.continuity.client.resource;

import java.util.Map;

import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.util.Identifier;

public interface BakedModelManagerBakeContext {
	ThreadLocal<BakedModelManagerBakeContext> THREAD_LOCAL = new ThreadLocal<>();

	void beforeBake(Map<Identifier, SpriteAtlasManager.AtlasPreparation> atlases);
}

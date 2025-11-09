package me.pepperbell.continuity.client.resource;

import net.minecraft.client.texture.SpriteLoader;

public interface BakedModelManagerBakeContext {
	ThreadLocal<BakedModelManagerBakeContext> THREAD_LOCAL = new ThreadLocal<>();

	void beforeBake(SpriteLoader.StitchResult stitchResult);
}

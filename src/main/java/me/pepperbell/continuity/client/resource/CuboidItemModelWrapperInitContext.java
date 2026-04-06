package me.pepperbell.continuity.client.resource;

import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinderGetter;

public final class CuboidItemModelWrapperInitContext {
	public static final ThreadLocal<SpriteFinderGetter> SPRITE_FINDER_GETTER = new ThreadLocal<>();

	private CuboidItemModelWrapperInitContext() {
	}
}

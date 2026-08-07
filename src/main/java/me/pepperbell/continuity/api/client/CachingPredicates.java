package me.pepperbell.continuity.api.client;

import java.util.function.Function;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

public interface CachingPredicates {
	boolean affectsSprites();

	boolean affectsSprite(TextureAtlasSprite sprite);

	boolean affectsBlockStates();

	boolean affectsBlockState(IBlockState state);

	boolean isValidForMultipass();

	interface Factory<T extends CtmProperties> {
		CachingPredicates createPredicates(T properties, Function<ResourceLocation, TextureAtlasSprite> spriteGetter);
	}
}

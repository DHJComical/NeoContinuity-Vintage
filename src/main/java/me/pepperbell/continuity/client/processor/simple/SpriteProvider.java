package me.pepperbell.continuity.client.processor.simple;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface SpriteProvider {
	@Nullable
	TextureAtlasSprite getSprite(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, ProcessingDataProvider dataProvider);

	interface Factory<T extends BaseCtmProperties> {
		SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, T properties);

		int getSpriteAmount(T properties);
	}
}

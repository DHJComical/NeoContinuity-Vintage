package me.pepperbell.continuity.client.processor.simple;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class FixedSpriteProvider implements SpriteProvider {
	protected TextureAtlasSprite sprite;

	public FixedSpriteProvider(TextureAtlasSprite sprite) {
		this.sprite = sprite;
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, ProcessingDataProvider dataProvider) {
		return this.sprite;
	}

	public static class Factory implements SpriteProvider.Factory<BaseCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, BaseCtmProperties properties) {
			return new FixedSpriteProvider(sprites[0]);
		}

		@Override
		public int getSpriteAmount(BaseCtmProperties properties) {
			return 1;
		}
	}
}

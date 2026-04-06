package me.pepperbell.continuity.client.processor.simple;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RepeatCtmProperties;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class RepeatSpriteProvider implements SpriteProvider {
	protected TextureAtlasSprite[] sprites;
	protected int width;
	protected int height;
	protected Symmetry symmetry;
	protected OrientationMode orientationMode;

	public RepeatSpriteProvider(TextureAtlasSprite[] sprites, int width, int height, Symmetry symmetry, OrientationMode orientationMode) {
		this.sprites = sprites;
		this.width = width;
		this.height = height;
		this.symmetry = symmetry;
		this.orientationMode = orientationMode;
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, ProcessingDataProvider dataProvider) {
		Direction face = symmetry.apply(quad.lightFace());

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		int spriteX;
		int spriteY;
		switch (face) {
			case DOWN -> {
				// MCPatcher uses a different formula for the down face.
				// It is not used here to maintain Optifine parity.
				// spriteX = -x;
				// spriteY = -z;
				spriteX = x;
				spriteY = -z - 1;
			}
			case UP -> {
				spriteX = x;
				spriteY = z;
			}
			case NORTH -> {
				spriteX = -x - 1;
				spriteY = -y;
			}
			case SOUTH -> {
				spriteX = x;
				spriteY = -y;
			}
			case WEST -> {
				spriteX = z;
				spriteY = -y;
			}
			case EAST -> {
				spriteX = -z - 1;
				spriteY = -y;
			}
			default -> {
				spriteX = 0;
				spriteY = 0;
			}
		}

		switch (orientationMode.getOrientation(quad, appearanceState)) {
			case 1 -> {
				int temp = spriteX;
				spriteX = -spriteY - 1;
				spriteY = temp;
			}
			case 2 -> {
				spriteX = -spriteX - 1;
				spriteY = -spriteY - 1;
			}
			case 3 -> {
				int temp = spriteX;
				spriteX = spriteY;
				spriteY = -temp - 1;
			}
			case 4 -> {
				spriteX = -spriteX - 1;
			}
			case 5 -> {
				int temp = spriteX;
				spriteX = spriteY;
				spriteY = temp;
			}
			case 6 -> {
				spriteY = -spriteY - 1;
			}
			case 7 -> {
				int temp = spriteX;
				spriteX = -spriteY - 1;
				spriteY = -temp - 1;
			}
		}

		spriteX %= width;
		if (spriteX < 0) {
			spriteX += width;
		}
		spriteY %= height;
		if (spriteY < 0) {
			spriteY += height;
		}

		return sprites[width * spriteY + spriteX];
	}

	public static class Factory implements SpriteProvider.Factory<RepeatCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, RepeatCtmProperties properties) {
			return new RepeatSpriteProvider(sprites, properties.getWidth(), properties.getHeight(), properties.getSymmetry(), properties.getOrientationMode());
		}

		@Override
		public int getSpriteAmount(RepeatCtmProperties properties) {
			return properties.getWidth() * properties.getHeight();
		}
	}
}

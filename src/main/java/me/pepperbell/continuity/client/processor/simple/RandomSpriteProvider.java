package me.pepperbell.continuity.client.processor.simple;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RandomCtmProperties;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.RandomIndexProvider;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RandomSpriteProvider implements SpriteProvider {
	protected TextureAtlasSprite[] sprites;
	protected RandomIndexProvider indexProvider;
	protected int randomLoops;
	protected Symmetry symmetry;
	protected boolean linked;

	public RandomSpriteProvider(TextureAtlasSprite[] sprites, RandomIndexProvider indexProvider, int randomLoops, Symmetry symmetry, boolean linked) {
		this.sprites = sprites;
		this.indexProvider = indexProvider;
		this.randomLoops = randomLoops;
		this.symmetry = symmetry;
		this.linked = linked;
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, ProcessingDataProvider dataProvider) {
		Direction face = quad.lightFace();

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (linked) {
			Block block = appearanceState.getBlock();
			BlockPos.MutableBlockPos mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS).set(pos);

			int i = 0;
			do {
				mutablePos.setY(mutablePos.getY() - 1);
				i++;
			} while (i < 3 && block == level.getBlockState(mutablePos).getAppearance(level, mutablePos, face, state, pos).getBlock());
			y = mutablePos.getY() + 1;
		}

		int seed = MathUtil.mix(x, y, z, symmetry.apply(face).ordinal(), randomLoops);
		return sprites[indexProvider.getRandomIndex(seed)];
	}

	public static class Factory implements SpriteProvider.Factory<RandomCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, RandomCtmProperties properties) {
			if (sprites.length == 1) {
				return new FixedSpriteProvider(sprites[0]);
			}
			return new RandomSpriteProvider(sprites, properties.getIndexProviderFactory().createIndexProvider(sprites.length), properties.getRandomLoops(), properties.getSymmetry(), properties.getLinked());
		}

		@Override
		public int getSpriteAmount(RandomCtmProperties properties) {
			return properties.getSpriteIds().size();
		}
	}
}

package me.pepperbell.continuity.client.processor.simple;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.Symmetry;
import me.pepperbell.continuity.client.properties.RandomCtmProperties;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.RandomIndexProvider;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class RandomSpriteProvider implements SpriteProvider {
	protected Sprite[] sprites;
	protected RandomIndexProvider indexProvider;
	protected int randomLoops;
	protected Symmetry symmetry;
	protected boolean linked;

	public RandomSpriteProvider(Sprite[] sprites, RandomIndexProvider indexProvider, int randomLoops, Symmetry symmetry, boolean linked) {
		this.sprites = sprites;
		this.indexProvider = indexProvider;
		this.randomLoops = randomLoops;
		this.symmetry = symmetry;
		this.linked = linked;
	}

	@Override
	@Nullable
	public Sprite getSprite(QuadView quad, Sprite sprite, BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, Random random, ProcessingDataProvider dataProvider) {
		Direction face = quad.lightFace();

		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();

		if (linked) {
			Block block = appearanceState.getBlock();
			BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS).set(pos);

			int i = 0;
			do {
				mutablePos.setY(mutablePos.getY() - 1);
				i++;
			} while (i < 3 && block == blockView.getBlockState(mutablePos).getAppearance(blockView, mutablePos, face, state, pos).getBlock());
			y = mutablePos.getY() + 1;
		}

		int seed = MathUtil.mix(x, y, z, symmetry.apply(face).ordinal(), randomLoops);
		return sprites[indexProvider.getRandomIndex(seed)];
	}

	public static class Factory implements SpriteProvider.Factory<RandomCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(Sprite[] sprites, RandomCtmProperties properties) {
			if (sprites.length == 1) {
				return new FixedSpriteProvider(sprites[0]);
			}
			return new RandomSpriteProvider(sprites, properties.getIndexProviderFactory().createIndexProvider(sprites.length), properties.getRandomLoops(), properties.getSymmetry(), properties.getLinked());
		}

		@Override
		public int getTextureAmount(RandomCtmProperties properties) {
			return properties.getSpriteIds().size();
		}
	}
}

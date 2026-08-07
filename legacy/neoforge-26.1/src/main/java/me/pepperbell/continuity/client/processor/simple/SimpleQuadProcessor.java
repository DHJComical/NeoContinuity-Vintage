package me.pepperbell.continuity.client.processor.simple;

import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessorFactory;
import me.pepperbell.continuity.client.processor.BaseProcessingPredicate;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleQuadProcessor implements QuadProcessor {
	protected SpriteProvider spriteProvider;
	protected ProcessingPredicate processingPredicate;

	public SimpleQuadProcessor(SpriteProvider spriteProvider, ProcessingPredicate processingPredicate) {
		this.spriteProvider = spriteProvider;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, level, pos, appearanceState, state, context)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		TextureAtlasSprite newSprite = spriteProvider.getSprite(quad, sprite, level, pos, appearanceState, state, random, context);
		return process(quad, sprite, newSprite);
	}

	public static ProcessingResult process(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite oldSprite, @Nullable TextureAtlasSprite newSprite) {
		if (newSprite == null) {
			return ProcessingResult.STOP;
		}
		if (RenderUtil.isMissingSprite(newSprite)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		QuadUtil.interpolate(quad, oldSprite, newSprite);
		return ProcessingResult.NEXT_PASS;
	}

	public static class Factory<T extends BaseCtmProperties> extends AbstractQuadProcessorFactory<T> {
		protected SpriteProvider.Factory<? super T> spriteProviderFactory;

		public Factory(SpriteProvider.Factory<? super T> spriteProviderFactory) {
			this.spriteProviderFactory = spriteProviderFactory;
		}

		@Override
		public QuadProcessor createProcessor(T properties, TextureAtlasSprite[] sprites) {
			return new SimpleQuadProcessor(spriteProviderFactory.createSpriteProvider(sprites, properties), BaseProcessingPredicate.fromProperties(properties));
		}

		@Override
		public int getSpriteAmount(T properties) {
			return spriteProviderFactory.getSpriteAmount(properties);
		}
	}
}

package me.pepperbell.continuity.client.processor.simple;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessorFactory;
import me.pepperbell.continuity.client.processor.BaseProcessingPredicate;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.util.RenderUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuadRetextured;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class SimpleQuadProcessor implements QuadProcessor {
	protected SpriteProvider spriteProvider;
	protected ProcessingPredicate processingPredicate;

	public SimpleQuadProcessor(SpriteProvider spriteProvider, ProcessingPredicate processingPredicate) {
		this.spriteProvider = spriteProvider;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, int pass, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, level, pos, appearanceState, state, context)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		TextureAtlasSprite newSprite = spriteProvider.getSprite(quad, sprite, level, pos, appearanceState, state, rand, context);
		return process(quad, sprite, newSprite, context);
	}

	public static ProcessingResult process(BakedQuad quad, TextureAtlasSprite oldSprite, @Nullable TextureAtlasSprite newSprite, ProcessingContext context) {
		if (newSprite == null) {
			return ProcessingResult.STOP;
		}
		if (RenderUtil.isMissingSprite(newSprite)) {
			ContinuityClient.LOGGER.debug("Skipping CTM replacement for '{}' because replacement sprite '{}' is missing", oldSprite.getIconName(), newSprite.getIconName());
			return ProcessingResult.NEXT_PROCESSOR;
		}
		context.getExtraQuads().add(new BakedQuadRetextured(quad, newSprite));
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

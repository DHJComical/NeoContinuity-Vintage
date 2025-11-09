package me.pepperbell.continuity.client.processor.overlay;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SpriteProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.properties.overlay.OverlayPropertiesSection;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class SimpleOverlayQuadProcessor extends SimpleQuadProcessor {
	protected int tintIndex;
	@Nullable
	protected BlockState tintBlock;
	protected BlockRenderLayer layer;
	protected TriState ao;

	public SimpleOverlayQuadProcessor(SpriteProvider spriteProvider, ProcessingPredicate processingPredicate, int tintIndex, @Nullable BlockState tintBlock, BlockRenderLayer layer) {
		super(spriteProvider, processingPredicate);
		this.tintIndex = tintIndex;
		this.tintBlock = tintBlock;
		this.layer = layer;
		this.ao = RenderUtil.aoFromTintBlock(tintBlock);
	}

	@Override
	public ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, Random random, int pass, ProcessingContext context) {
		if (processingPredicate.shouldProcessQuad(quad, sprite, blockView, pos, appearanceState, state, context)) {
			Sprite newSprite = spriteProvider.getSprite(quad, sprite, blockView, pos, appearanceState, state, random, context);
			if (newSprite != null && !TextureUtil.isMissingSprite(newSprite)) {
				QuadUtil.emitOverlayQuad(context.getExtraQuadEmitter(), quad.lightFace(), newSprite, RenderUtil.getTintColor(tintBlock, blockView, pos, tintIndex), layer, ao);
			}
		}
		return ProcessingResult.NEXT_PROCESSOR;
	}

	public static class Factory<T extends BaseCtmProperties & OverlayPropertiesSection.Provider> extends SimpleQuadProcessor.Factory<T> {
		public Factory(SpriteProvider.Factory<? super T> spriteProviderFactory) {
			super(spriteProviderFactory);
		}

		@Override
		public QuadProcessor createProcessor(T properties, Sprite[] sprites) {
			OverlayPropertiesSection overlaySection = properties.getOverlayPropertiesSection();
			return new SimpleOverlayQuadProcessor(spriteProviderFactory.createSpriteProvider(sprites, properties), OverlayProcessingPredicate.fromProperties(properties), overlaySection.getTintIndex(), overlaySection.getTintBlock(), overlaySection.getLayer());
		}

		@Override
		public boolean supportsNullSprites(T properties) {
			return false;
		}
	}
}

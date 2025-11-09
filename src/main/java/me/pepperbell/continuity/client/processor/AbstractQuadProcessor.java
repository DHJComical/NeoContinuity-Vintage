package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.QuadProcessor;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public abstract class AbstractQuadProcessor implements QuadProcessor {
	protected Sprite[] sprites;
	protected ProcessingPredicate processingPredicate;

	public AbstractQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate) {
		this.sprites = sprites;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, Random random, int pass, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, blockView, pos, appearanceState, state, context)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		return processQuadInner(quad, sprite, blockView, pos, appearanceState, state, random, pass, context);
	}

	public abstract ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, Random random, int pass, ProcessingContext context);
}

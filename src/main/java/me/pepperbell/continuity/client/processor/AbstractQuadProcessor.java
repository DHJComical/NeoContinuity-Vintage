package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.QuadProcessor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public abstract class AbstractQuadProcessor implements QuadProcessor {
	protected TextureAtlasSprite[] sprites;
	protected ProcessingPredicate processingPredicate;

	public AbstractQuadProcessor(TextureAtlasSprite[] sprites, ProcessingPredicate processingPredicate) {
		this.sprites = sprites;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, int pass, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, level, pos, appearanceState, state, context)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		return processQuadInner(quad, sprite, level, pos, appearanceState, state, rand, pass, context);
	}

	public abstract ProcessingResult processQuadInner(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, int pass, ProcessingContext context);
}

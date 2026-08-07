package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.QuadProcessor;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

public abstract class AbstractQuadProcessor implements QuadProcessor {
	protected TextureAtlasSprite[] sprites;
	protected ProcessingPredicate processingPredicate;

	public AbstractQuadProcessor(TextureAtlasSprite[] sprites, ProcessingPredicate processingPredicate) {
		this.sprites = sprites;
		this.processingPredicate = processingPredicate;
	}

	@Override
	public ProcessingResult processQuad(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context) {
		if (!processingPredicate.shouldProcessQuad(quad, sprite, level, pos, appearanceState, state, context)) {
			return ProcessingResult.NEXT_PROCESSOR;
		}
		return processQuadInner(quad, sprite, level, pos, appearanceState, state, random, pass, context);
	}

	public abstract ProcessingResult processQuadInner(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context);
}

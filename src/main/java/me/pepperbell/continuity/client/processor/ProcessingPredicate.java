package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface ProcessingPredicate {
	boolean shouldProcessQuad(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, ProcessingDataProvider dataProvider);
}

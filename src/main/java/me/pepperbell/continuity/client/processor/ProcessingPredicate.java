package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface ProcessingPredicate {
	boolean shouldProcessQuad(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, ProcessingDataProvider dataProvider);
}

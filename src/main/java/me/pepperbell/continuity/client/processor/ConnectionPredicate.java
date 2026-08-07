package me.pepperbell.continuity.client.processor;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public interface ConnectionPredicate {
	boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite);

	default boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, EnumFacing face, TextureAtlasSprite quadSprite) {
		IBlockState otherState = level.getBlockState(otherPos);
		IBlockState otherAppearanceState = otherState.getActualState(level, otherPos);
		return shouldConnect(level, pos, appearanceState, state, otherPos, otherAppearanceState, otherState, face, quadSprite);
	}

	default boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos.MutableBlockPos otherPos, EnumFacing face, TextureAtlasSprite quadSprite, boolean innerSeams) {
		if (shouldConnect(level, pos, appearanceState, state, otherPos, face, quadSprite)) {
			if (innerSeams) {
				otherPos.move(face);
				return !shouldConnect(level, pos, appearanceState, state, otherPos, face, quadSprite);
			} else {
				return true;
			}
		}
		return false;
	}
}

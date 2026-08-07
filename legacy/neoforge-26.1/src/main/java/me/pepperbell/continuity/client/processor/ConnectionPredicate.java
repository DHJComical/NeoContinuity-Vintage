package me.pepperbell.continuity.client.processor;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public interface ConnectionPredicate {
	boolean shouldConnect(BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos otherPos, BlockState otherAppearanceState, BlockState otherState, Direction face, TextureAtlasSprite quadSprite);

	default boolean shouldConnect(BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face, TextureAtlasSprite quadSprite) {
		BlockState otherState = level.getBlockState(otherPos);
		BlockState otherAppearanceState = otherState.getAppearance(level, otherPos, face, state, pos);
		return shouldConnect(level, pos, appearanceState, state, otherPos, otherAppearanceState, otherState, face, quadSprite);
	}

	default boolean shouldConnect(BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos.MutableBlockPos otherPos, Direction face, TextureAtlasSprite quadSprite, boolean innerSeams) {
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

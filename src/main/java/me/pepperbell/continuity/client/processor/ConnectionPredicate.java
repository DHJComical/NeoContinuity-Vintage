package me.pepperbell.continuity.client.processor;

import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public interface ConnectionPredicate {
	boolean shouldConnect(BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos otherPos, BlockState otherAppearanceState, BlockState otherState, Direction face, Sprite quadSprite);

	default boolean shouldConnect(BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos otherPos, Direction face, Sprite quadSprite) {
		BlockState otherState = blockView.getBlockState(otherPos);
		BlockState otherAppearanceState = otherState.getAppearance(blockView, otherPos, face, state, pos);
		return shouldConnect(blockView, pos, appearanceState, state, otherPos, otherAppearanceState, otherState, face, quadSprite);
	}

	default boolean shouldConnect(BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, BlockPos.Mutable otherPos, Direction face, Sprite quadSprite, boolean innerSeams) {
		if (shouldConnect(blockView, pos, appearanceState, state, otherPos, face, quadSprite)) {
			if (innerSeams) {
				otherPos.move(face);
				return !shouldConnect(blockView, pos, appearanceState, state, otherPos, face, quadSprite);
			} else {
				return true;
			}
		}
		return false;
	}
}

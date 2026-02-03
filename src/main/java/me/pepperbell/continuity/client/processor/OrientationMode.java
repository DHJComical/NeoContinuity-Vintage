package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public enum OrientationMode {
	NONE,
	STATE_AXIS,
	TEXTURE;

	public static final int[][] AXIS_ORIENTATIONS = new int[][] {
			{ 3, 3, 1, 3, 0, 2 },
			{ 0, 0, 0, 0, 0, 0 },
			{ 2, 0, 2, 0, 1, 3 }
	};

	public int getOrientation(QuadView quad, BlockState state) {
		return switch (this) {
			case NONE -> 0;
			case STATE_AXIS -> {
				if (state.hasProperty(BlockStateProperties.AXIS)) {
					Direction.Axis axis = state.getValue(BlockStateProperties.AXIS);
					yield AXIS_ORIENTATIONS[axis.ordinal()][quad.lightFace().ordinal()];
				} else {
					yield 0;
				}
			}
			case TEXTURE -> QuadUtil.getTextureOrientation(quad);
		};
	}
}

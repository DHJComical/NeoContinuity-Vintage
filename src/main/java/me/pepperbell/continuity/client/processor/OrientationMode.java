package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.client.util.QuadUtil;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

public enum OrientationMode {
	NONE,
	STATE_AXIS,
	TEXTURE;

	public static final int[][] AXIS_ORIENTATIONS = new int[][] {
			{ 3, 3, 1, 3, 0, 2 },
			{ 0, 0, 0, 0, 0, 0 },
			{ 2, 0, 2, 0, 1, 3 }
	};

	public int getOrientation(BakedQuad quad, IBlockState state) {
		return switch (this) {
			case NONE -> 0;
			case STATE_AXIS -> {
				EnumFacing face = quad.getFace();
				if (face == null) {
					yield 0;
				}
				IProperty<?> axisProperty = null;
				for (IProperty<?> property : state.getProperties().keySet()) {
					if (property.getName().equals("axis")) {
						axisProperty = property;
						break;
					}
				}
				if (axisProperty == null) {
					yield 0;
				}
				Object axisValue = state.getValue(axisProperty);
				if (axisValue instanceof EnumFacing.Axis axis) {
					yield AXIS_ORIENTATIONS[axis.ordinal()][face.ordinal()];
				}
				yield 0;
			}
			case TEXTURE -> QuadUtil.getTextureOrientation(quad);
		};
	}
}

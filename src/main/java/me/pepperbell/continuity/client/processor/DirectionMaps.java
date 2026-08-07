package me.pepperbell.continuity.client.processor;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.EnumFacing;

public final class DirectionMaps {
	public static final EnumFacing[][][] DIRECTION_MAPS = new EnumFacing[6][8][];

	static {
		for (EnumFacing face : EnumFacing.values()) {
			EnumFacing textureUp;
			if (face == EnumFacing.UP) {
				textureUp = EnumFacing.NORTH;
			} else if (face == EnumFacing.DOWN) {
				textureUp = EnumFacing.SOUTH;
			} else {
				textureUp = EnumFacing.UP;
			}

			EnumFacing textureLeft;
			if (face.getAxisDirection() == EnumFacing.AxisDirection.NEGATIVE) {
				textureLeft = rotateClockwise(textureUp, face.getAxis());
			} else {
				textureLeft = rotateCounterClockwise(textureUp, face.getAxis());
			}

			EnumFacing[][] map = DIRECTION_MAPS[face.ordinal()];

			map[0] = new EnumFacing[] { textureLeft, textureUp.getOpposite(), textureLeft.getOpposite(), textureUp };
			map[1] = map[0].clone();
			ArrayUtils.shift(map[1], -1);
			map[2] = map[1].clone();
			ArrayUtils.shift(map[2], -1);
			map[3] = map[2].clone();
			ArrayUtils.shift(map[3], -1);

			map[4] = map[0].clone();
			ArrayUtils.swap(map[4], 0, 2);
			map[5] = map[1].clone();
			ArrayUtils.swap(map[5], 0, 2);
			map[6] = map[2].clone();
			ArrayUtils.swap(map[6], 0, 2);
			map[7] = map[3].clone();
			ArrayUtils.swap(map[7], 0, 2);
		}
	}

	private DirectionMaps() {
	}

	private static EnumFacing rotateClockwise(EnumFacing face, EnumFacing.Axis axis) {
		return face.rotateAround(axis);
	}

	private static EnumFacing rotateCounterClockwise(EnumFacing face, EnumFacing.Axis axis) {
		return face.rotateAround(axis).rotateAround(axis).rotateAround(axis);
	}

	public static EnumFacing[][] getMap(EnumFacing direction) {
		return DIRECTION_MAPS[direction.ordinal()];
	}

	public static EnumFacing[] getDirections(OrientationMode orientationMode, BakedQuad quad, IBlockState state) {
		EnumFacing face = quad.getFace();
		if (face == null) {
			face = EnumFacing.DOWN;
		}
		return getMap(face)[orientationMode.getOrientation(quad, state)];
	}
}

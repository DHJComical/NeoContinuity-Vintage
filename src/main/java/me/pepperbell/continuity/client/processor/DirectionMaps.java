package me.pepperbell.continuity.client.processor;

import org.apache.commons.lang3.ArrayUtils;

import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public final class DirectionMaps {
	public static final Direction[][][] DIRECTION_MAPS = new Direction[6][8][];
	static {
		for (Direction face : Direction.values()) {
			Direction textureUp;
			if (face == Direction.UP) {
				textureUp = Direction.NORTH;
			} else if (face == Direction.DOWN) {
				textureUp = Direction.SOUTH;
			} else {
				textureUp = Direction.UP;
			}

			Direction textureLeft;
			if (face.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
				textureLeft = textureUp.getClockWise(face.getAxis());
			} else {
				textureLeft = textureUp.getCounterClockWise(face.getAxis());
			}

			Direction[][] map = DIRECTION_MAPS[face.ordinal()];

			map[0] = new Direction[] { textureLeft, textureUp.getOpposite(), textureLeft.getOpposite(), textureUp }; // l d r u
			map[1] = map[0].clone(); // d r u l
			ArrayUtils.shift(map[1], -1);
			map[2] = map[1].clone(); // r u l d
			ArrayUtils.shift(map[2], -1);
			map[3] = map[2].clone(); // u l d r
			ArrayUtils.shift(map[3], -1);

			map[4] = map[0].clone(); // r d l u
			ArrayUtils.swap(map[4], 0, 2);
			map[5] = map[1].clone(); // u r d l
			ArrayUtils.swap(map[5], 0, 2);
			map[6] = map[2].clone(); // l u r d
			ArrayUtils.swap(map[6], 0, 2);
			map[7] = map[3].clone(); // d l u r
			ArrayUtils.swap(map[7], 0, 2);
		}
	}

	public static Direction[][] getMap(Direction direction) {
		return DIRECTION_MAPS[direction.ordinal()];
	}

	public static Direction[] getDirections(OrientationMode orientationMode, QuadView quad, BlockState state) {
		return getMap(quad.lightFace())[orientationMode.getOrientation(quad, state)];
	}
}

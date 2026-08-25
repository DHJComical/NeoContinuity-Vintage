package me.pepperbell.continuity.client.ctm;

import java.util.Arrays;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * The eight directions a block face can connect in (four edges + four diagonals), with the offset
 * to the tested neighbor precomputed for every face normal. Mirrors the CTM Mod's {@code Dir}
 * enum, with bit index = {@link #ordinal()}.
 */
public enum CtmDir {
	TOP(EnumFacing.UP),
	TOP_RIGHT(EnumFacing.UP, EnumFacing.EAST),
	RIGHT(EnumFacing.EAST),
	BOTTOM_RIGHT(EnumFacing.DOWN, EnumFacing.EAST),
	BOTTOM(EnumFacing.DOWN),
	BOTTOM_LEFT(EnumFacing.DOWN, EnumFacing.WEST),
	LEFT(EnumFacing.WEST),
	TOP_LEFT(EnumFacing.UP, EnumFacing.WEST);

	private static final EnumFacing NORMAL = EnumFacing.SOUTH;
	public static final CtmDir[] VALUES = values();

	private final EnumFacing[] dirs;
	private final BlockPos[] offsets = new BlockPos[6];

	CtmDir(EnumFacing... dirs) {
		this.dirs = dirs;
	}

	static {
		for (CtmDir dir : VALUES) {
			dir.buildOffsets();
		}
	}

	private void buildOffsets() {
		for (EnumFacing normal : EnumFacing.VALUES) {
			EnumFacing[] normalized;
			if (normal == NORMAL) {
				normalized = this.dirs;
			} else if (normal == NORMAL.getOpposite()) {
				// Mirror horizontally (no Y flip)
				EnumFacing[] ret = new EnumFacing[this.dirs.length];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = this.dirs[i].getYOffset() != 0 ? this.dirs[i] : this.dirs[i].getOpposite();
				}
				normalized = ret;
			} else {
				EnumFacing axis;
				if (normal.getYOffset() == 0) {
					axis = normal == NORMAL.rotateY() ? EnumFacing.UP : EnumFacing.DOWN;
				} else {
					axis = normal == EnumFacing.UP ? NORMAL.rotateYCCW() : NORMAL.rotateY();
				}
				EnumFacing[] ret = new EnumFacing[this.dirs.length];
				for (int i = 0; i < ret.length; i++) {
					ret[i] = rotate(this.dirs[i], axis);
				}
				normalized = ret;
			}
			BlockPos pos = BlockPos.ORIGIN;
			for (EnumFacing facing : normalized) {
				pos = pos.offset(facing);
			}
			this.offsets[normal.ordinal()] = pos;
		}
	}

	private static EnumFacing rotate(EnumFacing facing, EnumFacing axisFacing) {
		EnumFacing.Axis axis = axisFacing.getAxis();
		if (axisFacing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE) {
			return facing.rotateAround(axis);
		}
		if (facing.getAxis() == axis) {
			return facing;
		}
		return switch (axis) {
			case X -> switch (facing) {
				case NORTH -> EnumFacing.UP;
				case DOWN -> EnumFacing.NORTH;
				case SOUTH -> EnumFacing.DOWN;
				case UP -> EnumFacing.SOUTH;
				default -> facing;
			};
			case Y -> facing.rotateYCCW();
			default -> switch (facing) {
				case EAST -> EnumFacing.EAST;
				case WEST -> EnumFacing.WEST;
				case UP -> EnumFacing.DOWN;
				case DOWN -> EnumFacing.UP;
				default -> facing;
			};
		};
	}

	public BlockPos getOffset(EnumFacing normal) {
		return this.offsets[normal.ordinal()];
	}

	/** The neighbor position this direction tests, relative to the block at {@code pos}. */
	public BlockPos apply(BlockPos pos, EnumFacing side) {
		return pos.add(this.getOffset(side));
	}

	/** Matches a list of world facings to one of the 8 standard directions, if possible. */
	public static CtmDir fromDirections(EnumFacing... dirs) {
		for (CtmDir dir : VALUES) {
			if (Arrays.equals(dir.dirs, dirs)) {
				return dir;
			}
		}
		throw new IllegalArgumentException("Currently invalid local direction: " + Arrays.toString(dirs));
	}
}

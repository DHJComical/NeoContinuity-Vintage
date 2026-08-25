package me.pepperbell.continuity.client.ctm;

import java.util.EnumSet;

import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Computes the 8-bit connection map for a block face in CTM format semantics. Bit {@code i}
 * corresponds to {@link CtmDir#VALUES CtmDir.values()[i]} (TOP=0, TOP_RIGHT=1, RIGHT=2,
 * BOTTOM_RIGHT=3, BOTTOM=4, BOTTOM_LEFT=5, LEFT=6, TOP_LEFT=7).
 */
public final class CtmConnectionMap {
	private final ConnectionPredicate predicate;

	public CtmConnectionMap(ConnectionPredicate predicate) {
		this.predicate = predicate;
	}

	/**
	 * @return the connection bitmask, or -1 if the CTM processing should be skipped (e.g. no
	 *         predicate configured for this sprite)
	 */
	public int compute(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, TextureAtlasSprite quadSprite) {
		int connections = 0;
		for (CtmDir dir : CtmDir.VALUES) {
			BlockPos otherPos = dir.apply(pos, face);
			if (predicate.shouldConnect(level, pos, appearanceState, state, otherPos, face, quadSprite)) {
				connections |= 1 << dir.ordinal();
			}
		}
		return connections;
	}

	public boolean connected(int connections, CtmDir dir) {
		return ((connections >> dir.ordinal()) & 1) == 1;
	}

	public boolean connectedAnd(int connections, CtmDir... dirs) {
		for (CtmDir dir : dirs) {
			if (!connected(connections, dir)) {
				return false;
			}
		}
		return true;
	}

	public boolean connectedOr(int connections, CtmDir... dirs) {
		for (CtmDir dir : dirs) {
			if (connected(connections, dir)) {
				return true;
			}
		}
		return false;
	}

	public boolean connectedNone(int connections, CtmDir... dirs) {
		return !connectedOr(connections, dirs);
	}

	/** Convenience for pillar-style logic: checks the given EnumFacing neighbor. */
	public boolean connectedFacing(int connections, EnumFacing facing, EnumFacing face) {
		for (CtmDir dir : CtmDir.VALUES) {
			if (dir.getOffset(face).equals(new BlockPos(facing.getDirectionVec()))) {
				return connected(connections, dir);
			}
		}
		return false;
	}
}

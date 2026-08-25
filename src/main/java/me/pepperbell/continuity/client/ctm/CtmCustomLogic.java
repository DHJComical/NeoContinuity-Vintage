package me.pepperbell.continuity.client.ctm;

import java.util.List;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * A baked custom CTM logic (from {@code assets/<ns>/ctm_logic/*.json}), used by the
 * {@code ctm_version: 2} / namespaced-type path of the CTM Mod format.
 * <p>
 * For every possible combination of the logic's input connection bits, the lookup table holds an
 * ordered list of outputs. Each output names a texture index and a sub-region (submap) of that
 * texture, and optionally a sub-face region the quad is clipped to.
 */
public final class CtmCustomLogic {
	/** The ordered list of input directions; index i corresponds to connection bit i. */
	private final List<LocalDirection> directions;
	/** lookup[state] = ordered list of output ids. */
	private final int[][] lookups;
	/** All possible outputs, indexed by output id. */
	private final OutputFace[] outputs;

	public CtmCustomLogic(List<LocalDirection> directions, int[][] lookups, OutputFace[] outputs) {
		this.directions = directions;
		this.lookups = lookups;
		this.outputs = outputs;
	}

	public int inputCount() {
		return directions.size();
	}

	public List<LocalDirection> getDirections() {
		return directions;
	}

	/**
	 * Returns the ordered list of output ids for the given connection bitmask.
	 *
	 * @throws IllegalStateException if the state has no rule
	 */
	public int[] getOutputsForState(int state) {
		if (state < 0 || state >= lookups.length || lookups[state] == null) {
			throw new IllegalStateException("Input state found that is not in lookup table: " + Integer.toBinaryString(state));
		}
		return lookups[state];
	}

	public OutputFace getOutput(int id) {
		return outputs[id];
	}

	public int requiredTextures() {
		int max = -1;
		for (OutputFace output : outputs) {
			if (output.tex > max) {
				max = output.tex;
			}
		}
		return max + 1;
	}

	public int outputCount() {
		return outputs.length;
	}

	public boolean hasMultipleOutputs() {
		for (int[] lookup : lookups) {
			if (lookup != null && lookup.length > 1) {
				return true;
			}
		}
		return false;
	}

	/** A local direction used as a connection bit input. */
	public interface LocalDirection {
		/** The offset from the current block to the neighbor this direction tests. */
		BlockPos getOffset(EnumFacing normal);
	}

	/** A single output of the truth table. */
	public record OutputFace(int tex, CtmSubmap uvs, CtmSubmap face) {
	}
}

package me.pepperbell.continuity.client.ctm;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Connection logic matching the CTM Mod format's {@code ConnectionCheck} semantics:
 * <ul>
 * <li>By default two states connect when they are the same {@link IBlockState} object.</li>
 * <li>{@code ignore_states}: connect by {@link net.minecraft.block.Block} identity instead.</li>
 * <li>{@code use_actual_state}: resolve each state through {@code getActualState} before comparing.</li>
 * <li>{@code connect_to}: when present, both blocks must match an explicitly listed block id,
 * allowing different listed blocks to connect.</li>
 * <li>The "obscured face" check (a block directly in front of the checked face also connecting)
 * can be toggled via {@code connect_inside}; the CTM default is to perform it.</li>
 * </ul>
 */
public class CtmConnectionPredicate implements ConnectionPredicate {
	protected final boolean ignoreStates;
	protected final boolean useActualState;
	protected final boolean disableObscuredFaceCheck;
	protected final boolean connectToDefined;
	protected final Set<Block> connectToBlocks;

	public CtmConnectionPredicate(boolean ignoreStates, boolean useActualState, boolean disableObscuredFaceCheck) {
		this(ignoreStates, useActualState, disableObscuredFaceCheck, false, Set.of());
	}

	private CtmConnectionPredicate(boolean ignoreStates, boolean useActualState, boolean disableObscuredFaceCheck,
			boolean connectToDefined, Set<Block> connectToBlocks) {
		this.ignoreStates = ignoreStates;
		this.useActualState = useActualState;
		this.disableObscuredFaceCheck = disableObscuredFaceCheck;
		this.connectToDefined = connectToDefined;
		this.connectToBlocks = connectToBlocks;
	}

	@Override
	public boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite) {
		IBlockState from = resolve(appearanceState, level, pos);
		IBlockState to = resolve(otherAppearanceState, level, otherPos);

		boolean connects = statesEqual(from, to);

		if (!disableObscuredFaceCheck) {
			// The "obscured face" test: if the block beyond the neighbor (in the face direction)
			// also matches, the face is treated as not connected (avoids showing seams at inner
			// corners of thick connected regions).
			BlockPos obscuringPos = otherPos.offset(face);
			IBlockState obscuringRaw = level.getBlockState(obscuringPos);
			IBlockState obscuring = resolve(obscuringRaw.getActualState(level, obscuringPos), level, obscuringPos);
			if (statesEqual(from, obscuring)) {
				connects = false;
			}
		}

		return connects;
	}

	protected IBlockState resolve(IBlockState state, IBlockAccess level, BlockPos pos) {
		if (useActualState) {
			return state.getActualState(level, pos);
		}
		return state;
	}

	protected boolean statesEqual(IBlockState from, IBlockState to) {
		if (connectToDefined) {
			return connectToBlocks.contains(from.getBlock()) && connectToBlocks.contains(to.getBlock());
		}
		if (ignoreStates) {
			return from.getBlock() == to.getBlock();
		}
		return from == to;
	}

	public static CtmConnectionPredicate fromProperties(CtmDefinition properties, boolean forceDisableObscured) {
		Boolean connectInside = properties.getConnectInside();
		boolean disableObscured;
		if (connectInside != null) {
			disableObscured = connectInside;
		} else {
			disableObscured = forceDisableObscured;
		}
		Set<Block> allowedBlocks = new HashSet<>();
		for (ResourceLocation id : properties.getConnectToBlocks()) {
			Block block = Block.REGISTRY.getObject(id);
			if (block != null && id.equals(Block.REGISTRY.getNameForObject(block))) {
				allowedBlocks.add(block);
			}
		}
		return new CtmConnectionPredicate(properties.isIgnoreStates(), properties.isUseActualState(), disableObscured,
				properties.isConnectToDefined(), Set.copyOf(allowedBlocks));
	}
}

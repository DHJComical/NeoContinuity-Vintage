package me.pepperbell.continuity.client.util;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class RenderUtil {
	private static final BlockColors BLOCK_COLORS = Minecraft.getInstance().getBlockColors();

	private static SpriteFinder blockAtlasSpriteFinder;

	public static int getTintColor(@Nullable BlockState state, BlockAndTintGetter level, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		return 0xFF000000 | BLOCK_COLORS.getColor(state, level, pos, tintIndex);
	}

	public static TriState aoFromTintBlock(@Nullable BlockState tintBlock) {
		if (tintBlock != null) {
			return TriState.of(canHaveAO(tintBlock));
		} else {
			return TriState.TRUE;
		}
	}

	public static boolean canHaveAO(BlockState state) {
		return state.getLightEmission() == 0;
	}

	public static SpriteFinder getSpriteFinder() {
		return blockAtlasSpriteFinder;
	}

	public static class ReloadListener implements ResourceManagerReloadListener {
		public static final Identifier ID = ContinuityClient.asId("render_util");
		public static final ReloadListener INSTANCE = new ReloadListener();

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			blockAtlasSpriteFinder = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).spriteFinder();
		}
	}
}

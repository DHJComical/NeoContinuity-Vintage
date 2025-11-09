package me.pepperbell.continuity.client.util;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public final class RenderUtil {
	private static final BlockColors BLOCK_COLORS = MinecraftClient.getInstance().getBlockColors();

	private static SpriteFinder blockAtlasSpriteFinder;

	public static int getTintColor(@Nullable BlockState state, BlockRenderView blockView, BlockPos pos, int tintIndex) {
		if (state == null || tintIndex == -1) {
			return -1;
		}
		return 0xFF000000 | BLOCK_COLORS.getColor(state, blockView, pos, tintIndex);
	}

	public static TriState aoFromTintBlock(@Nullable BlockState tintBlock) {
		if (tintBlock != null) {
			return TriState.of(canHaveAO(tintBlock));
		} else {
			return TriState.TRUE;
		}
	}

	public static boolean canHaveAO(BlockState state) {
		return state.getLuminance() == 0;
	}

	public static SpriteFinder getSpriteFinder() {
		return blockAtlasSpriteFinder;
	}

	public static class ReloadListener implements SynchronousResourceReloader {
		public static final Identifier ID = ContinuityClient.asId("render_util");
		public static final ReloadListener INSTANCE = new ReloadListener();

		@Override
		public void reload(ResourceManager manager) {
			blockAtlasSpriteFinder = MinecraftClient.getInstance().getAtlasManager().getAtlasTexture(Atlases.BLOCKS).spriteFinder();
		}
	}
}

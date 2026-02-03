package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.resource.CustomBlockLayers;
import net.fabricmc.fabric.api.renderer.v1.render.RenderLayerHelper;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ItemBlockRenderTypes.class)
abstract class ItemBlockRenderTypesMixin {
	@Inject(method = "getChunkRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/chunk/ChunkSectionLayer;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetChunkRenderType(BlockState state, CallbackInfoReturnable<ChunkSectionLayer> cir) {
		if (!CustomBlockLayers.isEmpty() && ContinuityConfig.INSTANCE.customBlockLayers.get()) {
			ChunkSectionLayer layer = CustomBlockLayers.getLayer(state);
			if (layer != null) {
				cir.setReturnValue(layer);
			}
		}
	}

	@Inject(method = "getMovingBlockRenderType(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), cancellable = true)
	private static void continuity$onHeadGetMovingBlockRenderType(BlockState state, CallbackInfoReturnable<RenderType> cir) {
		if (!CustomBlockLayers.isEmpty() && ContinuityConfig.INSTANCE.customBlockLayers.get()) {
			ChunkSectionLayer layer = CustomBlockLayers.getLayer(state);
			if (layer != null) {
				cir.setReturnValue(RenderLayerHelper.getMovingBlockLayer(layer));
			}
		}
	}
}

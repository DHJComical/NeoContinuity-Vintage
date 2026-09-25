package me.pepperbell.continuity.client.mixin;

import me.pepperbell.continuity.client.ctm.CtmRenderLayerRouter;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockRenderLayerMixin {
	@Inject(method = "canRenderInLayer", at = @At("RETURN"), cancellable = true, remap = false)
	private void continuity$allowCtmLayer(IBlockState state, BlockRenderLayer layer,
			CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue() && CtmRenderLayerRouter.allowAdditionalLayer(state, layer)) {
			cir.setReturnValue(true);
		}
	}
}

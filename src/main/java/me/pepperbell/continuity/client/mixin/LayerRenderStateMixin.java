package me.pepperbell.continuity.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.model.BakedModel;

@Mixin(value = ItemRenderState.LayerRenderState.class, priority = -1000)
abstract class LayerRenderStateMixin {
	@ModifyVariable(method = "setModel(Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/client/render/RenderLayer;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private BakedModel continuity$modifyBakedModel(BakedModel model) {
		ModelWrappingHandler wrappingHandler = ModelWrappingHandler.getInstance();
		if (wrappingHandler != null) {
			return wrappingHandler.ensureWrapped(model);
		}
		return model;
	}
}

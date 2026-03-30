package me.pepperbell.continuity.client.mixin;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.mixinterface.SpriteExtension;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelSettings;
import net.minecraft.client.texture.Sprite;

@Mixin(BasicItemModel.class)
abstract class BasicItemModelMixin {
	@Unique
	@Nullable
	private List<BakedQuad> emissiveQuads;

	@Inject(method = "<init>(Ljava/util/List;Ljava/util/List;Lnet/minecraft/client/render/model/ModelSettings;)V", at = @At("RETURN"))
	private void onReturnInit(List<TintSource> tints, List<BakedQuad> quads, ModelSettings settings, CallbackInfo ci) {
		for (BakedQuad quad : quads) {
			Sprite emissiveSprite = ((SpriteExtension) quad.sprite()).continuity$getEmissiveSprite();
			if (emissiveSprite != null) {
				int[] emissiveVertexData = quad.vertexData().clone();
				QuadUtil.interpolate(emissiveVertexData, quad.sprite(), emissiveSprite);
				BakedQuad emissiveQuad = new BakedQuad(emissiveVertexData, quad.tintIndex(), quad.face(), emissiveSprite, false, 15);

				if (emissiveQuads == null) {
					emissiveQuads = new ArrayList<>();
				}
				emissiveQuads.add(emissiveQuad);
			}
		}
	}

	@Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/entity/LivingEntity;I)V", at = @At(value = "INVOKE", target = "java/util/List.addAll(Ljava/util/Collection;)Z", remap = false, shift = At.Shift.AFTER))
	private void afterAddVanillaQuads(CallbackInfo ci, @Local ItemRenderState.LayerRenderState layerRenderState) {
		if (emissiveQuads != null && ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			layerRenderState.getQuads().addAll(emissiveQuads);
		}
	}
}

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
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.model.BasicItemModel;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.ModelSettings;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HeldItemContext;

@Mixin(BasicItemModel.class)
abstract class BasicItemModelMixin {
	@Unique
	private static final Object EMISSIVE_GEOMETRY_MARKER = new Object();

	@Unique
	@Nullable
	private List<BakedQuad> emissiveQuads;
	@Unique
	private boolean emissiveAnimated = false;

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

				if (emissiveSprite.getContents().isAnimated()) {
					emissiveAnimated = true;
				}
			}
		}
	}

	@Inject(method = "update(Lnet/minecraft/client/render/item/ItemRenderState;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/item/ItemModelManager;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/util/HeldItemContext;I)V", at = @At(value = "INVOKE", target = "java/util/List.addAll(Ljava/util/Collection;)Z", remap = false, shift = At.Shift.AFTER))
	private void afterAddVanillaQuads(ItemRenderState state, ItemStack stack, ItemModelManager resolver, ItemDisplayContext displayContext, @Nullable ClientWorld world, @Nullable HeldItemContext heldItemContext, int seed, CallbackInfo ci, @Local ItemRenderState.LayerRenderState layerRenderState) {
		if (emissiveQuads != null && ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			layerRenderState.getQuads().addAll(emissiveQuads);
			state.addModelKey(EMISSIVE_GEOMETRY_MARKER);

			if (emissiveAnimated) {
				state.markAnimated();
			}
		}
	}
}

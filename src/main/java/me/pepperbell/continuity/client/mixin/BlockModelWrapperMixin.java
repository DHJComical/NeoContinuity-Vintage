package me.pepperbell.continuity.client.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.mixinterface.TextureAtlasSpriteExtension;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

@Mixin(BlockModelWrapper.class)
abstract class BlockModelWrapperMixin {
	@Unique
	private static final Object EMISSIVE_GEOMETRY_MARKER = new Object();

	@Unique
	@Nullable
	private List<BakedQuad> emissiveQuads;
	@Unique
	private boolean emissiveAnimated = false;

	@Inject(method = "<init>(Ljava/util/List;Ljava/util/List;Lnet/minecraft/client/renderer/item/ModelRenderProperties;Ljava/util/function/Function;)V", at = @At("RETURN"))
	private void onReturnInit(List<ItemTintSource> tints, List<BakedQuad> quads, ModelRenderProperties properties, Function<ItemStack, RenderType> renderType, CallbackInfo ci) {
		QuadUtil.PackedUvContainer output = new QuadUtil.PackedUvContainer();
		for (BakedQuad quad : quads) {
			TextureAtlasSprite emissiveSprite = ((TextureAtlasSpriteExtension) quad.sprite()).continuity$getEmissiveSprite();
			if (emissiveSprite != null) {
				QuadUtil.interpolate(quad, output, quad.sprite(), emissiveSprite);
				BakedQuad emissiveQuad = new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), output.packedUV0, output.packedUV1, output.packedUV2, output.packedUV3, quad.tintIndex(), quad.direction(), emissiveSprite, false, 15);

				if (emissiveQuads == null) {
					emissiveQuads = new ArrayList<>();
				}
				emissiveQuads.add(emissiveQuad);

				if (emissiveSprite.contents().isAnimated()) {
					emissiveAnimated = true;
				}
			}
		}
	}

	@Inject(method = "update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)V", at = @At(value = "INVOKE", target = "java/util/List.addAll(Ljava/util/Collection;)Z", remap = false, shift = At.Shift.AFTER))
	private void afterAddVanillaQuads(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed, CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
		if (emissiveQuads != null && ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			layer.prepareQuadList().addAll(emissiveQuads);
			output.appendModelIdentityElement(EMISSIVE_GEOMETRY_MARKER);

			if (emissiveAnimated) {
				output.setAnimated();
			}
		}
	}
}

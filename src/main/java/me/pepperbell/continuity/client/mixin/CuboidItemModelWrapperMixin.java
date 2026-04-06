package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.resource.CuboidItemModelWrapperInitContext;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.client.renderer.v1.model.MeshQuadCollection;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinderGetter;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

// Set priority to higher than FRAPI's mixin to this class to ensure emissive quads are rendered after FRAPI quads
@Mixin(value = CuboidItemModelWrapper.class, priority = 2000)
abstract class CuboidItemModelWrapperMixin {
	@Unique
	private static final Object EMISSIVE_GEOMETRY_MARKER = new Object();

	@Unique
	@Nullable
	private QuadCollection emissiveQuads;

	@Inject(method = "<init>(Ljava/util/List;Lnet/minecraft/client/resources/model/geometry/QuadCollection;Lnet/minecraft/client/renderer/item/ModelRenderProperties;Lorg/joml/Matrix4fc;)V", at = @At("RETURN"))
	private void onReturnInit(List<ItemTintSource> tints, QuadCollection quads, ModelRenderProperties properties, Matrix4fc transformation, CallbackInfo ci) {
		SpriteFinderGetter spriteFinderGetter = CuboidItemModelWrapperInitContext.SPRITE_FINDER_GETTER.get();
		emissiveQuads = QuadUtil.createEmissiveQuads(quads, spriteFinderGetter);
	}

	@Inject(method = "update(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/client/renderer/item/ItemModelResolver;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/world/entity/ItemOwner;I)V", at = @At("RETURN"))
	private void onReturnUpdate(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver, ItemDisplayContext displayContext, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed, CallbackInfo ci, @Local ItemStackRenderState.LayerRenderState layer) {
		if (emissiveQuads != null && ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			layer.prepareQuadList().addAll(emissiveQuads.getAll());
			if (emissiveQuads instanceof MeshQuadCollection meshQuadCollection) {
				meshQuadCollection.getMesh().outputTo(layer.emitter());
			}
			output.appendModelIdentityElement(EMISSIVE_GEOMETRY_MARKER);

			if (emissiveQuads.hasMaterialFlag(BakedQuad.FLAG_ANIMATED)) {
				output.setAnimated();
			}
		}
	}
}

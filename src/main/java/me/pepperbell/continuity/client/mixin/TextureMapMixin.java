package me.pepperbell.continuity.client.mixin;

import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureMap.class)
public abstract class TextureMapMixin {
	@Inject(method = "registerSprite(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;", at = @At("RETURN"))
	private void continuity$registerEmissive(ResourceLocation location, CallbackInfoReturnable<TextureAtlasSprite> cir) {
		String suffix = EmissiveSuffixLoader.getEmissiveSuffix();
		if (suffix == null || suffix.isEmpty() || location.getPath().endsWith(suffix)) {
			return;
		}
		ResourceLocation emissiveLocation = new ResourceLocation(location.getNamespace(), location.getPath() + suffix);
		((TextureMap) (Object) this).registerSprite(emissiveLocation);
	}
}

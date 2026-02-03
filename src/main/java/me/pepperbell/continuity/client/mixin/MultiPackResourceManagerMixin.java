package me.pepperbell.continuity.client.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

@Mixin(MultiPackResourceManager.class)
abstract class MultiPackResourceManagerMixin {
	@Unique
	private boolean continuity$useRedirects = false;

	@Inject(method = "<init>(Lnet/minecraft/server/packs/PackType;Ljava/util/List;)V", at = @At("TAIL"))
	private void continuity$onTailInit(PackType type, List<PackResources> packs, CallbackInfo ci) {
		continuity$useRedirects = type == PackType.CLIENT_RESOURCES;
	}

	@ModifyVariable(method = "getResource(Lnet/minecraft/resources/Identifier;)Ljava/util/Optional;", at = @At("HEAD"), argsOnly = true)
	private Identifier continuity$redirectGetResourceId(Identifier id) {
		if (continuity$useRedirects) {
			return ResourceRedirectHandler.redirect(id);
		}
		return id;
	}

	@ModifyVariable(method = "getResourceStack(Lnet/minecraft/resources/Identifier;)Ljava/util/List;", at = @At("HEAD"), argsOnly = true)
	private Identifier continuity$redirectGetResourceStackId(Identifier id) {
		if (continuity$useRedirects) {
			return ResourceRedirectHandler.redirect(id);
		}
		return id;
	}
}

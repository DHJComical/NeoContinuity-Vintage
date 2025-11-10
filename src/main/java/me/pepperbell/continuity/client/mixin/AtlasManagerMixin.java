package me.pepperbell.continuity.client.mixin;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.sugar.Local;

import me.pepperbell.continuity.client.resource.CtmResourceReloader;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import me.pepperbell.continuity.client.resource.SpriteLoaderLoadContext;
import me.pepperbell.continuity.client.resource.SpriteLoaderLoadContextImpl;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

@Mixin(AtlasManager.class)
abstract class AtlasManagerMixin {
	@Inject(method = "prepareSharedState(Lnet/minecraft/resource/ResourceReloader$Store;)V", at = @At("RETURN"))
	private void continuity$onReturnPrepareSharedState(ResourceReloader.Store store, CallbackInfo ci) {
		store.put(ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY, new CompletableFuture<>());
	}

	@Inject(method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/resource/ResourceReloader$Store;getResourceManager()Lnet/minecraft/resource/ResourceManager;"))
	private void continuity$onHeadReload(ResourceReloader.Store store, Executor prepareExecutor, ResourceReloader.Synchronizer synchronizer, Executor applyExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir, @Local AtlasManager.Stitch stitch) {
		EmissiveSuffixLoader.load(store.getResourceManager());
		CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture = store.getOrThrow(CtmResourceReloader.ALL_EXTRA_IDS_FUTURE_KEY);
		CompletableFuture<Boolean> wrapEmissiveFuture = store.getOrThrow(ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY);

		// This shouldn't be necessary, but it prevents a deadlock if for whatever reason the future isn't completed
		// before this.
		stitch.getPreparations(Atlases.BLOCKS).whenComplete((stitchResult, t) -> {
			wrapEmissiveFuture.complete(false);
		});

		SpriteLoaderLoadContext.THREAD_LOCAL.set(new SpriteLoaderLoadContextImpl(allExtraIdsFuture, wrapEmissiveFuture));
	}

	@Inject(method = "reload(Lnet/minecraft/resource/ResourceReloader$Store;Ljava/util/concurrent/Executor;Lnet/minecraft/resource/ResourceReloader$Synchronizer;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private void continuity$onReturnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		SpriteLoaderLoadContext.THREAD_LOCAL.set(null);
	}
}

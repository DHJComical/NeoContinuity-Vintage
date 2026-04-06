package me.pepperbell.continuity.client.mixin;

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
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

@Mixin(AtlasManager.class)
abstract class AtlasManagerMixin {
	@Inject(method = "prepareSharedState(Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;)V", at = @At("RETURN"))
	private void continuity$onReturnPrepareSharedState(PreparableReloadListener.SharedState currentReload, CallbackInfo ci) {
		currentReload.set(ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY, new CompletableFuture<>());
	}

	@Inject(method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;Ljava/util/concurrent/Executor;Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;resourceManager()Lnet/minecraft/server/packs/resources/ResourceManager;"))
	private void continuity$onHeadReload(PreparableReloadListener.SharedState currentReload, Executor prepareExecutor, PreparableReloadListener.PreparationBarrier preparationBarrier, Executor applyExecutor, CallbackInfoReturnable<CompletableFuture<Void>> cir, @Local AtlasManager.PendingStitchResults stitchResults) {
		EmissiveSuffixLoader.load(currentReload.resourceManager());
		CompletableFuture<Set<Identifier>> blockAtlasExtraIdsFuture = currentReload.get(CtmResourceReloader.BLOCK_ATLAS_EXTRA_IDS_FUTURE_KEY);
		CompletableFuture<Boolean> wrapEmissiveFuture = currentReload.get(ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY);

		// This shouldn't be necessary, but it prevents a deadlock if for whatever reason the future isn't completed
		// before this.
		stitchResults.get(AtlasIds.BLOCKS).whenComplete((preparations, t) -> {
			wrapEmissiveFuture.complete(false);
		});

		SpriteLoaderLoadContext.THREAD_LOCAL.set(new SpriteLoaderLoadContextImpl(blockAtlasExtraIdsFuture, wrapEmissiveFuture));
	}

	@Inject(method = "reload(Lnet/minecraft/server/packs/resources/PreparableReloadListener$SharedState;Ljava/util/concurrent/Executor;Lnet/minecraft/server/packs/resources/PreparableReloadListener$PreparationBarrier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", at = @At("RETURN"))
	private void continuity$onReturnReload(CallbackInfoReturnable<CompletableFuture<Void>> cir) {
		SpriteLoaderLoadContext.THREAD_LOCAL.remove();
	}
}

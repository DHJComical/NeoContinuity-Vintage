package me.pepperbell.continuity.client.resource;

import java.util.concurrent.CompletableFuture;

// import me.pepperbell.continuity.client.model.CtmBlockStateModel;
// import me.pepperbell.continuity.client.model.EmissiveBlockStateModel;
// import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
// import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public final class ModelWrappingHandler {
	public static /* final PreparableReloadListener.StateKey< */ CompletableFuture<Boolean> /* > */ WRAP_CTM_FUTURE_KEY = /* new PreparableReloadListener.StateKey<>() */ new CompletableFuture<>();
	public static /* final PreparableReloadListener.StateKey< */ CompletableFuture<Boolean> /* > */ WRAP_EMISSIVE_FUTURE_KEY = /* new PreparableReloadListener.StateKey<>() */ new CompletableFuture<>();

	public static void init() {
		/* PreparableModelLoadingPlugin.register((store, executor) -> {
			CompletableFuture<Boolean> wrapCtmFuture = store.get(WRAP_CTM_FUTURE_KEY);
			CompletableFuture<Boolean> wrapEmissiveFuture = store.get(WRAP_EMISSIVE_FUTURE_KEY);
			return CompletableFuture.allOf(wrapCtmFuture, wrapEmissiveFuture).thenApplyAsync(v -> {
				return new Data(wrapCtmFuture.join(), wrapEmissiveFuture.join());
			}, executor);
		}, (data, pluginCtx) -> {
			boolean wrapCtm = data.wrapCtm();
			boolean wrapEmissive = data.wrapEmissive();
			if (!wrapCtm && !wrapEmissive) {
				return;
			}

			pluginCtx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) -> {
				if (wrapCtm) {
					model = new CtmBlockStateModel(model, ctx.state());
				}
				if (wrapEmissive) {
					model = new EmissiveBlockStateModel(model);
				}
				return model;
			});
		}); */
	}

	private record Data(boolean wrapCtm, boolean wrapEmissive) {
	}
}

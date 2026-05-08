package me.pepperbell.continuity.client.resource;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class CtmResourceReloader implements PreparableReloadListener {
	public static final StateKey<CompletableFuture<Set<Identifier>>> BLOCK_ATLAS_EXTRA_IDS_FUTURE_KEY = new StateKey<>();

	public static final Identifier ID = ContinuityClient.asId("ctm");
	public static final CtmResourceReloader INSTANCE = new CtmResourceReloader();

	@Override
	public void prepareSharedState(SharedState currentReload) {
		currentReload.set(BLOCK_ATLAS_EXTRA_IDS_FUTURE_KEY, new CompletableFuture<>());
		// currentReload.set(ModelWrappingHandler.WRAP_CTM_FUTURE_KEY, new CompletableFuture<>());
		ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY = new CompletableFuture<>();
	}

	@Override
	public CompletableFuture<Void> reload(SharedState currentReload, Executor prepareExecutor, PreparationBarrier preparationBarrier, Executor applyExecutor) {
		ResourceManager resourceManager = currentReload.resourceManager();
		CompletableFuture<Set<Identifier>> blockAtlasExtraIdsFuture = currentReload.get(BLOCK_ATLAS_EXTRA_IDS_FUTURE_KEY);
		CompletableFuture<Boolean> wrapCtmFuture = /* currentReload.get( */ ModelWrappingHandler.WRAP_CTM_FUTURE_KEY /* ) */;
		CompletableFuture<CtmPropertiesLoader.LoadingResult> ctmLoadingResultFuture = CompletableFuture.supplyAsync(() -> CtmPropertiesLoader.loadAllWithState(resourceManager), prepareExecutor).whenComplete((ctmLoadingResult, t) -> {
			if (ctmLoadingResult != null) {
				blockAtlasExtraIdsFuture.complete(ctmLoadingResult.getBlockAtlasSpriteDependencies());
			} else {
				blockAtlasExtraIdsFuture.completeExceptionally(t);
			}
		});
		CompletableFuture<SpriteLoader.Preparations> blockAtlasPreparationsFuture = currentReload.get(AtlasManager.PENDING_STITCH).get(AtlasIds.BLOCKS);

		CompletableFuture<List<QuadProcessors.ProcessorHolder>> future = CompletableFuture.allOf(ctmLoadingResultFuture, blockAtlasPreparationsFuture).thenApplyAsync(v -> {
			CtmPropertiesLoader.LoadingResult ctmLoadingResult = ctmLoadingResultFuture.join();
			SpriteLoader.Preparations blockAtlasPreparations = blockAtlasPreparationsFuture.join();

			return ctmLoadingResult.createProcessorHolders(spriteId -> {
				TextureAtlasSprite sprite = blockAtlasPreparations.getSprite(spriteId);
				return sprite != null ? sprite : blockAtlasPreparations.missing();
			});
		}, prepareExecutor).whenComplete((processorHolders, t) -> {
			if (processorHolders != null) {
				wrapCtmFuture.complete(!processorHolders.isEmpty());
			} else {
				wrapCtmFuture.completeExceptionally(t);
			}
		});

		Objects.requireNonNull(preparationBarrier);
		return future.thenCompose(preparationBarrier::wait).thenAcceptAsync(this::apply, applyExecutor);
	}

	private void apply(List<QuadProcessors.ProcessorHolder> processorHolders) {
		QuadProcessors.reload(processorHolders);
	}
}

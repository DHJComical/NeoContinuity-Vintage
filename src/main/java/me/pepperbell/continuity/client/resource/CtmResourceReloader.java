package me.pepperbell.continuity.client.resource;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.texture.AtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.SpriteLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Atlases;
import net.minecraft.util.Identifier;

public class CtmResourceReloader implements ResourceReloader {
	public static final Key<CompletableFuture<Map<Identifier, Set<Identifier>>>> ALL_EXTRA_IDS_FUTURE_KEY = new Key<>();

	public static final Identifier ID = ContinuityClient.asId("ctm");
	public static final CtmResourceReloader INSTANCE = new CtmResourceReloader();

	@Override
	public void prepareSharedState(Store store) {
		store.put(ALL_EXTRA_IDS_FUTURE_KEY, new CompletableFuture<>());
		store.put(ModelWrappingHandler.WRAP_CTM_FUTURE_KEY, new CompletableFuture<>());
	}

	@Override
	public CompletableFuture<Void> reload(Store store, Executor prepareExecutor, Synchronizer synchronizer, Executor applyExecutor) {
		ResourceManager resourceManager = store.getResourceManager();
		CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture = store.getOrThrow(ALL_EXTRA_IDS_FUTURE_KEY);
		CompletableFuture<Boolean> wrapCtmFuture = store.getOrThrow(ModelWrappingHandler.WRAP_CTM_FUTURE_KEY);
		CompletableFuture<CtmPropertiesLoader.LoadingResult> ctmLoadingResultFuture = CompletableFuture.supplyAsync(() -> CtmPropertiesLoader.loadAllWithState(resourceManager), prepareExecutor).whenComplete((ctmLoadingResult, t) -> {
			if (ctmLoadingResult != null) {
				allExtraIdsFuture.complete(ctmLoadingResult.getTextureDependencies());
			} else {
				allExtraIdsFuture.completeExceptionally(t);
			}
		});
		CompletableFuture<SpriteLoader.StitchResult> blockAtlasStitchResultFuture = store.getOrThrow(AtlasManager.stitchKey).getPreparations(Atlases.BLOCKS);

		CompletableFuture<List<QuadProcessors.ProcessorHolder>> future = CompletableFuture.allOf(ctmLoadingResultFuture, blockAtlasStitchResultFuture).thenApplyAsync(v -> {
			CtmPropertiesLoader.LoadingResult ctmLoadingResult = ctmLoadingResultFuture.join();
			SpriteLoader.StitchResult blockAtlasStitchResult = blockAtlasStitchResultFuture.join();

			return ctmLoadingResult.createProcessorHolders(spriteId -> {
				if (spriteId.getAtlasId().equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
					Sprite sprite = blockAtlasStitchResult.getSprite(spriteId.getTextureId());
					if (sprite != null) {
						return sprite;
					}
				}
				return blockAtlasStitchResult.missing();
			});
		}, prepareExecutor).whenComplete(((processorHolders, t) -> {
			if (processorHolders != null) {
				wrapCtmFuture.complete(!processorHolders.isEmpty());
			} else {
				wrapCtmFuture.completeExceptionally(t);
			}
		}));

		Objects.requireNonNull(synchronizer);
		return future.thenCompose(synchronizer::whenPrepared).thenAcceptAsync(this::apply, applyExecutor);
	}

	private void apply(List<QuadProcessors.ProcessorHolder> processorHolders) {
		QuadProcessors.reload(processorHolders);
	}
}

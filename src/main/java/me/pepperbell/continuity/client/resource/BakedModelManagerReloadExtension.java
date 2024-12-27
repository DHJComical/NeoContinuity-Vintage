package me.pepperbell.continuity.client.resource;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.render.model.SpriteAtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class BakedModelManagerReloadExtension implements BakedModelManagerBakeContext {
	private final CompletableFuture<CtmPropertiesLoader.LoadingResult> ctmLoadingResultFuture;
	private final AtomicBoolean wrapEmissiveModels = new AtomicBoolean();
	private final SpriteLoaderLoadContextImpl spriteLoaderLoadContext;
	private volatile List<QuadProcessors.ProcessorHolder> processorHolders;

	public BakedModelManagerReloadExtension(ResourceManager resourceManager, Executor prepareExecutor) {
		ctmLoadingResultFuture = CompletableFuture.supplyAsync(() -> CtmPropertiesLoader.loadAllWithState(resourceManager), prepareExecutor);
		spriteLoaderLoadContext = new SpriteLoaderLoadContextImpl(ctmLoadingResultFuture.thenApply(CtmPropertiesLoader.LoadingResult::getTextureDependencies), wrapEmissiveModels);
		EmissiveSuffixLoader.load(resourceManager);
		ModelWrappingHandler.resetInstance();
	}

	public void setContext() {
		SpriteLoaderLoadContext.THREAD_LOCAL.set(spriteLoaderLoadContext);
	}

	public void clearContext() {
		SpriteLoaderLoadContext.THREAD_LOCAL.set(null);
	}

	@Override
	public void beforeBake(Map<Identifier, SpriteAtlasManager.AtlasPreparation> preparations) {
		CtmPropertiesLoader.LoadingResult result = ctmLoadingResultFuture.join();

		List<QuadProcessors.ProcessorHolder> processorHolders = result.createProcessorHolders(spriteId -> {
			SpriteAtlasManager.AtlasPreparation preparation = preparations.get(spriteId.getAtlasId());
			Sprite sprite = preparation.getSprite(spriteId.getTextureId());
			if (sprite != null) {
				return sprite;
			}
			return preparation.getMissingSprite();
		});

		this.processorHolders = processorHolders;

		ModelWrappingHandler.setInstance(!processorHolders.isEmpty(), wrapEmissiveModels.get());
	}

	public void apply() {
		List<QuadProcessors.ProcessorHolder> processorHolders = this.processorHolders;
		if (processorHolders != null) {
			QuadProcessors.reload(processorHolders);
		}
	}

	private static class SpriteLoaderLoadContextImpl implements SpriteLoaderLoadContext {
		private final CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture;
		private final Map<Identifier, CompletableFuture<Set<Identifier>>> extraIdsFutures = new Object2ObjectOpenHashMap<>();
		private final EmissiveControl blockAtlasEmissiveControl;

		public SpriteLoaderLoadContextImpl(CompletableFuture<Map<Identifier, Set<Identifier>>> allExtraIdsFuture, AtomicBoolean blockAtlasHasEmissivesHolder) {
			this.allExtraIdsFuture = allExtraIdsFuture;
			blockAtlasEmissiveControl = new EmissiveControlImpl(blockAtlasHasEmissivesHolder);
		}

		@Override
		public CompletableFuture<@Nullable Set<Identifier>> getExtraIdsFuture(Identifier atlasId) {
			return extraIdsFutures.computeIfAbsent(atlasId, id -> allExtraIdsFuture.thenApply(allExtraIds -> allExtraIds.get(id)));
		}

		@Override
		@Nullable
		public EmissiveControl getEmissiveControl(Identifier atlasId) {
			if (atlasId.equals(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE)) {
				return blockAtlasEmissiveControl;
			}
			return null;
		}

		private static class EmissiveControlImpl implements EmissiveControl {
			@Nullable
			private volatile Map<Identifier, Identifier> emissiveIdMap;
			private final AtomicBoolean hasEmissivesHolder;

			public EmissiveControlImpl(AtomicBoolean hasEmissivesHolder) {
				this.hasEmissivesHolder = hasEmissivesHolder;
			}

			@Override
			@Nullable
			public Map<Identifier, Identifier> getEmissiveIdMap() {
				return emissiveIdMap;
			}

			@Override
			public void setEmissiveIdMap(Map<Identifier, Identifier> emissiveIdMap) {
				this.emissiveIdMap = emissiveIdMap;
			}

			@Override
			public void markHasEmissives() {
				hasEmissivesHolder.set(true);
			}
		}
	}
}

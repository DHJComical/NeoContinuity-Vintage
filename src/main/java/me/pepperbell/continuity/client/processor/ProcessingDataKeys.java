package me.pepperbell.continuity.client.processor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.overlay.StandardOverlayQuadProcessor;
import net.minecraft.util.math.BlockPos;

public final class ProcessingDataKeys {
	public static final ProcessingDataKey<BlockPos.Mutable> MUTABLE_POS = create("mutable_pos", BlockPos.Mutable::new);
	public static final ProcessingDataKey<BaseProcessingPredicate.BiomeCache> BIOME_CACHE = create("biome_cache", BaseProcessingPredicate.BiomeCache::new, BaseProcessingPredicate.BiomeCache::reset);
	public static final ProcessingDataKey<BaseProcessingPredicate.BlockEntityNameCache> BLOCK_ENTITY_NAME_CACHE = create("block_entity_name_cache", BaseProcessingPredicate.BlockEntityNameCache::new, BaseProcessingPredicate.BlockEntityNameCache::reset);
	public static final ProcessingDataKey<CompactCtmQuadProcessor.VertexContainer> VERTEX_CONTAINER = create("vertex_container", CompactCtmQuadProcessor.VertexContainer::new);
	public static final ProcessingDataKey<StandardOverlayQuadProcessor.SpriteCollector> SPRITE_COLLECTOR = create("sprite_collector", StandardOverlayQuadProcessor.SpriteCollector::new);

	private static <T> ProcessingDataKey<T> create(String id, Supplier<T> valueSupplier) {
		return ProcessingDataKeyRegistry.get().registerKey(ContinuityClient.asId(id), valueSupplier);
	}

	private static <T> ProcessingDataKey<T> create(String id, Supplier<T> valueSupplier, Consumer<T> valueResetAction) {
		return ProcessingDataKeyRegistry.get().registerKey(ContinuityClient.asId(id), valueSupplier, valueResetAction);
	}

	public static void init() {
	}
}

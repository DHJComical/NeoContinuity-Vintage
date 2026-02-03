package me.pepperbell.continuity.client.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.PropertiesParsingHelper;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.state.BlockState;

public final class CustomBlockLayers {
	public static final Identifier LOCATION = Identifier.withDefaultNamespace("optifine/block.properties");

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] EMPTY_LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	@SuppressWarnings("unchecked")
	private static final Predicate<BlockState>[] LAYER_PREDICATES = new Predicate[BlockLayer.VALUES.length];

	private static boolean empty;

	private static boolean disableSolidCheck;

	public static boolean isEmpty() {
		return empty;
	}

	@Nullable
	public static ChunkSectionLayer getLayer(BlockState state) {
		if (!disableSolidCheck) {
			if (state.isSolidRender()) {
				return null;
			}
		}

		for (int i = 0; i < BlockLayer.VALUES.length; i++) {
			Predicate<BlockState> predicate = LAYER_PREDICATES[i];
			if (predicate != null) {
				if (predicate.test(state)) {
					return BlockLayer.VALUES[i].getLayer();
				}
			}
		}
		return null;
	}

	private static void reload(ResourceManager manager) {
		empty = true;
		System.arraycopy(EMPTY_LAYER_PREDICATES, 0, LAYER_PREDICATES, 0, EMPTY_LAYER_PREDICATES.length);
		disableSolidCheck = false;

		Optional<Resource> optionalResource = manager.getResource(LOCATION);
		if (optionalResource.isPresent()) {
			Resource resource = optionalResource.get();
			try (InputStream inputStream = resource.open()) {
				Properties properties = new Properties();
				properties.load(inputStream);
				reload(properties, LOCATION, resource.sourcePackId());
			} catch (IOException e) {
				ContinuityClient.LOGGER.error("Failed to load custom block layers from file '" + LOCATION + "' from pack '" + resource.sourcePackId() + "'", e);
			}
		}
	}

	private static void reload(Properties properties, Identifier fileLocation, String packId) {
		for (BlockLayer blockLayer : BlockLayer.VALUES) {
			String propertyKey = "layer." + blockLayer.getKey();
			Predicate<BlockState> predicate = PropertiesParsingHelper.parseBlockStates(properties, propertyKey, fileLocation, packId);
			if (predicate != null && predicate != PropertiesParsingHelper.EMPTY_BLOCK_STATE_PREDICATE) {
				LAYER_PREDICATES[blockLayer.ordinal()] = predicate;
				empty = false;
			}
		}

		String disableSolidCheckStr = properties.getProperty("disableSolidCheck");
		if (disableSolidCheckStr != null) {
			disableSolidCheck = Boolean.parseBoolean(disableSolidCheckStr.trim());
		}
	}

	public static class ReloadListener implements ResourceManagerReloadListener {
		public static final Identifier ID = ContinuityClient.asId("custom_block_layers");
		public static final ReloadListener INSTANCE = new ReloadListener();

		@Override
		public void onResourceManagerReload(ResourceManager manager) {
			CustomBlockLayers.reload(manager);
		}
	}

	private enum BlockLayer {
		SOLID(ChunkSectionLayer.SOLID),
		CUTOUT(ChunkSectionLayer.CUTOUT),
		TRANSLUCENT(ChunkSectionLayer.TRANSLUCENT);

		public static final BlockLayer[] VALUES = values();

		private final ChunkSectionLayer layer;
		private final String key;

		BlockLayer(ChunkSectionLayer layer) {
			this.layer = layer;
			key = name().toLowerCase(Locale.ROOT);
		}

		public ChunkSectionLayer getLayer() {
			return layer;
		}

		public String getKey() {
			return key;
		}
	}
}

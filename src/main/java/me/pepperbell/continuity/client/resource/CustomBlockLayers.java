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
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;

public final class CustomBlockLayers {
	public static final Identifier LOCATION = Identifier.ofVanilla("optifine/block.properties");

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
	public static BlockRenderLayer getLayer(BlockState state) {
		if (!disableSolidCheck) {
			if (state.isOpaqueFullCube()) {
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
			try (InputStream inputStream = resource.getInputStream()) {
				Properties properties = new Properties();
				properties.load(inputStream);
				reload(properties, LOCATION, resource.getPackId());
			} catch (IOException e) {
				ContinuityClient.LOGGER.error("Failed to load custom block layers from file '" + LOCATION + "' from pack '" + resource.getPackId() + "'", e);
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

	public static class ReloadListener implements SynchronousResourceReloader {
		public static final Identifier ID = ContinuityClient.asId("custom_block_layers");
		public static final ReloadListener INSTANCE = new ReloadListener();

		@Override
		public void reload(ResourceManager manager) {
			CustomBlockLayers.reload(manager);
		}
	}

	private enum BlockLayer {
		SOLID(BlockRenderLayer.SOLID),
		CUTOUT(BlockRenderLayer.CUTOUT),
		CUTOUT_MIPPED(BlockRenderLayer.CUTOUT_MIPPED),
		TRANSLUCENT(BlockRenderLayer.TRANSLUCENT);

		public static final BlockLayer[] VALUES = values();

		private final BlockRenderLayer layer;
		private final String key;

		BlockLayer(BlockRenderLayer layer) {
			this.layer = layer;
			key = name().toLowerCase(Locale.ROOT);
		}

		public BlockRenderLayer getLayer() {
			return layer;
		}

		public String getKey() {
			return key;
		}
	}
}

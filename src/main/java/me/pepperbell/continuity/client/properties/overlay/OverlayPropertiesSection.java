package me.pepperbell.continuity.client.properties.overlay;

import java.util.Locale;
import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.IdentifierException;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class OverlayPropertiesSection {
	protected Properties properties;
	protected Identifier resourceId;
	protected String packId;

	protected int tintIndex = -1;
	@Nullable
	protected BlockState tintBlock;
	protected ChunkSectionLayer layer = ChunkSectionLayer.CUTOUT;

	public OverlayPropertiesSection(Properties properties, Identifier resourceId, String packId) {
		this.properties = properties;
		this.resourceId = resourceId;
		this.packId = packId;
	}

	public void init() {
		parseTintIndex();
		parseTintBlock();
		parseLayer();
	}

	protected void parseTintIndex() {
		String tintIndexStr = properties.getProperty("tintIndex");
		if (tintIndexStr == null) {
			return;
		}

		try {
			int tintIndex = Integer.parseInt(tintIndexStr.trim());
			if (tintIndex >= 0) {
				this.tintIndex = tintIndex;
				return;
			}
		} catch (NumberFormatException e) {
			//
		}
		ContinuityClient.LOGGER.warn("Invalid 'tintIndex' value '" + tintIndexStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
	}

	protected void parseTintBlock() {
		String tintBlockStr = properties.getProperty("tintBlock");
		if (tintBlockStr == null) {
			return;
		}

		String[] parts = tintBlockStr.trim().split(":", 3);
		if (parts.length != 0) {
			Identifier blockId;
			try {
				if (parts.length == 1 || parts[1].contains("=")) {
					blockId = Identifier.withDefaultNamespace(parts[0]);
				} else {
					blockId = Identifier.fromNamespaceAndPath(parts[0], parts[1]);
				}
			} catch (IdentifierException e) {
				ContinuityClient.LOGGER.warn("Invalid 'tintBlock' value '" + tintBlockStr + "' in file '" + resourceId + "' in pack '" + packId + "'", e);
				return;
			}

			if (BuiltInRegistries.BLOCK.containsKey(blockId)) {
				Block block = BuiltInRegistries.BLOCK.getValue(blockId);
				tintBlock = block.defaultBlockState();
			} else {
				ContinuityClient.LOGGER.warn("Unknown block '" + blockId + "' in 'tintBlock' value '" + tintBlockStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
			}
		} else {
			ContinuityClient.LOGGER.warn("Invalid 'tintBlock' value '" + tintBlockStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
		}
	}

	protected void parseLayer() {
		String layerStr = properties.getProperty("layer");
		if (layerStr == null) {
			return;
		}

		String layerStr1 = layerStr.trim().toLowerCase(Locale.ROOT);
		switch (layerStr1) {
			case "cutout" -> layer = ChunkSectionLayer.CUTOUT;
			case "translucent" -> layer = ChunkSectionLayer.TRANSLUCENT;
			default -> ContinuityClient.LOGGER.warn("Unknown 'layer' value '" + layerStr + " in file '" + resourceId + "' in pack '" + packId + "'");
		}
	}

	public int getTintIndex() {
		return tintIndex;
	}

	@Nullable
	public BlockState getTintBlock() {
		return tintBlock;
	}

	public ChunkSectionLayer getLayer() {
		return layer;
	}

	public interface Provider {
		OverlayPropertiesSection getOverlayPropertiesSection();
	}
}

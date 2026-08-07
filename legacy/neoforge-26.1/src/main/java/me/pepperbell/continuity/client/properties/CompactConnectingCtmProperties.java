package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.OrientationMode;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;

public class CompactConnectingCtmProperties extends OrientedConnectingCtmProperties {
	@Nullable
	protected Int2IntMap tileReplacementMap;

	public CompactConnectingCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method, OrientationMode defaultOrientationMode) {
		super(properties, resourceId, pack, packPriority, resourceManager, method, defaultOrientationMode);
	}

	public CompactConnectingCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		this(properties, resourceId, pack, packPriority, resourceManager, method, OrientationMode.TEXTURE);
	}

	@Override
	public void init() {
		super.init();
		parseTileReplacements();
	}

	protected void parseTileReplacements() {
		for (String key : properties.stringPropertyNames()) {
			if (key.startsWith("ctm.")) {
				String indexStr = key.substring(4);
				int index;
				try {
					index = Integer.parseInt(indexStr);
				} catch (NumberFormatException e) {
					continue;
				}
				if (index < 0) {
					continue;
				}

				String valueStr = properties.getProperty(key);
				int value;
				try {
					value = Integer.parseInt(valueStr.trim());
				} catch (NumberFormatException e) {
					ContinuityClient.LOGGER.warn("Invalid '" + key + "' value '" + valueStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
					continue;
				}
				// TODO: deduplicate code
				if (value < 0) {
					ContinuityClient.LOGGER.warn("Invalid '" + key + "' value '" + valueStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
					continue;
				}

				if (tileReplacementMap == null) {
					tileReplacementMap = new Int2IntArrayMap();
				}
				tileReplacementMap.put(index, value);
			}
		}
	}

	@Nullable
	public Int2IntMap getTileReplacementMap() {
		return tileReplacementMap;
	}
}

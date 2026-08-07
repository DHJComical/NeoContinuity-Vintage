package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import me.pepperbell.continuity.client.processor.OrientationMode;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

public class OrientedConnectingCtmProperties extends ConnectingCtmProperties {
	protected OrientationMode orientationMode;

	public OrientedConnectingCtmProperties(Properties properties, ResourceLocation resourceId, IResourcePack pack, int packPriority, IResourceManager resourceManager, String method, OrientationMode defaultOrientationMode) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
		orientationMode = defaultOrientationMode;
	}

	public OrientedConnectingCtmProperties(Properties properties, ResourceLocation resourceId, IResourcePack pack, int packPriority, IResourceManager resourceManager, String method) {
		this(properties, resourceId, pack, packPriority, resourceManager, method, OrientationMode.TEXTURE);
	}

	@Override
	public void init() {
		super.init();
		parseOrient();
	}

	protected void parseOrient() {
		OrientationMode orientationMode = PropertiesParsingHelper.parseOrientationMode(properties, "orient", resourceId, packId);
		if (orientationMode != null) {
			this.orientationMode = orientationMode;
		}
	}

	public OrientationMode getOrientationMode() {
		return orientationMode;
	}
}

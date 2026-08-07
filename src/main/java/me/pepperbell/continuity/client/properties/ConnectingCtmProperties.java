package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

public class ConnectingCtmProperties extends BasicConnectingCtmProperties {
	protected boolean innerSeams = false;

	public ConnectingCtmProperties(Properties properties, ResourceLocation resourceId, IResourcePack pack, int packPriority, IResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
	}

	@Override
	public void init() {
		super.init();
		parseInnerSeams();
	}

	protected void parseInnerSeams() {
		String innerSeamsStr = properties.getProperty("innerSeams");
		if (innerSeamsStr == null) {
			return;
		}

		innerSeams = Boolean.parseBoolean(innerSeamsStr.trim());
	}

	public boolean getInnerSeams() {
		return innerSeams;
	}
}

package me.pepperbell.continuity.client.properties;

import java.util.Properties;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;

public class ConnectingCtmProperties extends BasicConnectingCtmProperties {
	protected boolean innerSeams = false;

	public ConnectingCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
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

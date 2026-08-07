package me.pepperbell.continuity.client.properties.overlay;

import java.util.Properties;

import me.pepperbell.continuity.client.properties.RepeatCtmProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ResourceManager;

public class RepeatOverlayCtmProperties extends RepeatCtmProperties implements OverlayPropertiesSection.Provider {
	protected OverlayPropertiesSection overlaySection;

	public RepeatOverlayCtmProperties(Properties properties, Identifier resourceId, PackResources pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
		overlaySection = new OverlayPropertiesSection(properties, resourceId, packId);
	}

	@Override
	public void init() {
		super.init();
		overlaySection.init();
	}

	@Override
	public OverlayPropertiesSection getOverlayPropertiesSection() {
		return overlaySection;
	}
}

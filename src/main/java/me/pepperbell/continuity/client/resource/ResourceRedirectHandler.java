package me.pepperbell.continuity.client.resource;

import net.minecraft.util.ResourceLocation;

public final class ResourceRedirectHandler {
	public static final String SPRITE_PATH_START = "continuity_reserved/";
	public static final String PATH_START = "textures/" + SPRITE_PATH_START;
	public static final int PATH_START_LENGTH = PATH_START.length();

	private ResourceRedirectHandler() {
	}

	public static ResourceLocation redirect(ResourceLocation id) {
		String path = id.getPath();
		if (!path.startsWith(PATH_START)) {
			return id;
		}

		return new ResourceLocation(id.getNamespace(), "optifine/" + path.substring(PATH_START_LENGTH));
	}
}

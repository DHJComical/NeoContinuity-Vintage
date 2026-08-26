package me.pepperbell.continuity.client.resource;

import net.minecraft.util.ResourceLocation;

public final class ResourceRedirectHandler {
	public static final String SPRITE_PATH_START = "continuity_reserved/";
	public static final String PATH_START = "textures/" + SPRITE_PATH_START;
	public static final int PATH_START_LENGTH = PATH_START.length();

	/** Real pack path prefixes that a {@code continuity_reserved/} sprite is redirected back to. */
	private static final String[] REDIRECT_PREFIXES = {
			"optifine/",
			"mcpatcher/",
	};

	private ResourceRedirectHandler() {
	}

	public static ResourceLocation redirect(ResourceLocation id) {
		String path = id.getPath();
		if (!path.startsWith(PATH_START)) {
			return id;
		}
		String rest = path.substring(PATH_START_LENGTH);
		// If the sprite id already carries a known pack prefix (mcpatcher/), use it as-is.
		// Otherwise treat the rest as an OptiFine path (parseTiles strips "optifine/") and
		// re-add it, restoring the pre-mcpatcher behavior.
		for (String prefix : REDIRECT_PREFIXES) {
			if (rest.startsWith(prefix)) {
				return new ResourceLocation(id.getNamespace(), rest);
			}
		}
		return new ResourceLocation(id.getNamespace(), "optifine/" + rest);
	}
}

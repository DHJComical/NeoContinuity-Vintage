package me.pepperbell.continuity.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;

public final class RenderUtil {
	private RenderUtil() {
	}

	public static boolean isMissingSprite(TextureAtlasSprite sprite) {
		TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
		return sprite == textureMap.getMissingSprite() || sprite.getIconName().equals(TextureMap.LOCATION_MISSING_TEXTURE.toString());
	}
}

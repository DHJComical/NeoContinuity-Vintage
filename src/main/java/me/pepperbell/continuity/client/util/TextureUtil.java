package me.pepperbell.continuity.client.util;

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;

public final class TextureUtil {
	public static final Material MISSING_MATERIAL = toMaterial(MissingTextureAtlasSprite.getLocation());

	public static Material toMaterial(Identifier id) {
		return new Material(TextureAtlas.LOCATION_BLOCKS, id);
	}

	public static boolean isMissingSprite(TextureAtlasSprite sprite) {
		return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
	}
}

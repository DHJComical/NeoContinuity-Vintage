package me.pepperbell.continuity.client.mixinterface;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface TextureAtlasSpriteExtension {
	@Nullable
	TextureAtlasSprite continuity$getEmissiveSprite();

	void continuity$setEmissiveSprite(TextureAtlasSprite sprite);
}

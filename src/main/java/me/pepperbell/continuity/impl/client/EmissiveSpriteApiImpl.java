package me.pepperbell.continuity.impl.client;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.mixinterface.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class EmissiveSpriteApiImpl implements EmissiveSpriteApi {
	public static final EmissiveSpriteApiImpl INSTANCE = new EmissiveSpriteApiImpl();

	@Override
	@Nullable
	public TextureAtlasSprite getEmissiveSprite(TextureAtlasSprite sprite) {
		return ((TextureAtlasSpriteExtension) sprite).continuity$getEmissiveSprite();
	}
}

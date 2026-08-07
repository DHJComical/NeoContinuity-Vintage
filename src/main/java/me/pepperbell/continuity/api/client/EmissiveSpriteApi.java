package me.pepperbell.continuity.api.client;

import javax.annotation.Nullable;

import me.pepperbell.continuity.impl.client.EmissiveSpriteApiImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface EmissiveSpriteApi {
	static EmissiveSpriteApi get() {
		return EmissiveSpriteApiImpl.INSTANCE;
	}

	@Nullable
	TextureAtlasSprite getEmissiveSprite(TextureAtlasSprite sprite);
}

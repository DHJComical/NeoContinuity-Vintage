package me.pepperbell.continuity.api.client;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.impl.client.EmissiveSpriteApiImpl;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@ApiStatus.NonExtendable
public interface EmissiveSpriteApi {
	static EmissiveSpriteApi get() {
		return EmissiveSpriteApiImpl.INSTANCE;
	}

	@Nullable
	TextureAtlasSprite getEmissiveSprite(TextureAtlasSprite sprite);
}

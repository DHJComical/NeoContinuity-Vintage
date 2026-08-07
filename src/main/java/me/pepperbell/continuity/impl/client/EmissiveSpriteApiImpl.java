package me.pepperbell.continuity.impl.client;

import java.util.Map;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class EmissiveSpriteApiImpl implements EmissiveSpriteApi {
	public static final EmissiveSpriteApiImpl INSTANCE = new EmissiveSpriteApiImpl();

	private final Map<TextureAtlasSprite, TextureAtlasSprite> emissiveMap = new Reference2ObjectOpenHashMap<>();

	@Override
	@Nullable
	public TextureAtlasSprite getEmissiveSprite(TextureAtlasSprite sprite) {
		return emissiveMap.get(sprite);
	}

	public void setEmissiveSprite(TextureAtlasSprite sprite, TextureAtlasSprite emissiveSprite) {
		emissiveMap.put(sprite, emissiveSprite);
	}

	public void clear() {
		emissiveMap.clear();
	}
}

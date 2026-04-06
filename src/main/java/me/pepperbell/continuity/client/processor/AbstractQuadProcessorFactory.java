package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.function.Function;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

public abstract class AbstractQuadProcessorFactory<T extends BaseCtmProperties> implements QuadProcessor.Factory<T> {
	@Override
	public QuadProcessor createProcessor(T properties, Function<Identifier, TextureAtlasSprite> spriteGetter) {
		int spriteAmount = getSpriteAmount(properties);
		List<Identifier> spriteIds = properties.getSpriteIds();
		int provided = spriteIds.size();
		int max = provided;

		if (provided > spriteAmount) {
			ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + spriteAmount + " tiles but " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
			max = spriteAmount;
		}

		TextureAtlasSprite[] sprites = new TextureAtlasSprite[spriteAmount];
		TextureAtlasSprite missingSprite = spriteGetter.apply(MissingTextureAtlasSprite.getLocation());
		boolean supportsNullSprites = supportsNullSprites(properties);
		for (int i = 0; i < max; i++) {
			TextureAtlasSprite sprite;
			Identifier spriteId = spriteIds.get(i);
			if (spriteId.equals(BaseCtmProperties.SPECIAL_SKIP_ID)) {
				sprite = missingSprite;
			} else if (spriteId.equals(BaseCtmProperties.SPECIAL_DEFAULT_ID)) {
				sprite = supportsNullSprites ? null : missingSprite;
			} else {
				sprite = spriteGetter.apply(spriteId);
			}
			sprites[i] = sprite;
		}

		if (provided < spriteAmount) {
			ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires " + spriteAmount + " tiles but only " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
			for (int i = provided; i < spriteAmount; i++) {
				sprites[i] = missingSprite;
			}
		}

		return createProcessor(properties, sprites);
	}

	public abstract QuadProcessor createProcessor(T properties, TextureAtlasSprite[] sprites);

	public abstract int getSpriteAmount(T properties);

	public boolean supportsNullSprites(T properties) {
		return true;
	}
}

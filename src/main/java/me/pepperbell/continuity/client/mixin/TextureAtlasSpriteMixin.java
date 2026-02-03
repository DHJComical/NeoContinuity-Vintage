package me.pepperbell.continuity.client.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import me.pepperbell.continuity.client.mixinterface.TextureAtlasSpriteExtension;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

@Mixin(TextureAtlasSprite.class)
abstract class TextureAtlasSpriteMixin implements TextureAtlasSpriteExtension {
	@Unique
	private TextureAtlasSprite continuity$emissiveSprite;

	@Override
	@Nullable
	public TextureAtlasSprite continuity$getEmissiveSprite() {
		return continuity$emissiveSprite;
	}

	@Override
	public void continuity$setEmissiveSprite(TextureAtlasSprite sprite) {
		continuity$emissiveSprite = sprite;
	}
}

package me.pepperbell.continuity.client.processor;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;

public class BaseCachingPredicates implements CachingPredicates {
	@Nullable
	protected Set<ResourceLocation> spriteIdSet;
	@Nullable
	protected Predicate<IBlockState> blockStatePredicate;
	protected boolean isValidForMultipass;

	public BaseCachingPredicates(@Nullable Set<ResourceLocation> spriteIdSet, @Nullable Predicate<IBlockState> blockStatePredicate, boolean isValidForMultipass) {
		this.spriteIdSet = spriteIdSet;
		this.blockStatePredicate = blockStatePredicate;
		this.isValidForMultipass = isValidForMultipass;
	}

	@Override
	public boolean affectsSprites() {
		return spriteIdSet != null;
	}

	@Override
	public boolean affectsSprite(TextureAtlasSprite sprite) {
		if (spriteIdSet != null) {
			ResourceLocation spriteId = new ResourceLocation(sprite.getIconName());
			return spriteIdSet.contains(spriteId);
		}
		return false;
	}

	@Override
	public boolean affectsBlockStates() {
		return blockStatePredicate != null;
	}

	@Override
	public boolean affectsBlockState(IBlockState state) {
		if (blockStatePredicate != null) {
			return blockStatePredicate.test(state);
		}
		return false;
	}

	@Override
	public boolean isValidForMultipass() {
		return isValidForMultipass;
	}

	public static class Factory<T extends BaseCtmProperties> implements CachingPredicates.Factory<T> {
		protected boolean isValidForMultipass;

		public Factory(boolean isValidForMultipass) {
			this.isValidForMultipass = isValidForMultipass;
		}

		@Override
		public CachingPredicates createPredicates(T properties, Function<ResourceLocation, TextureAtlasSprite> spriteGetter) {
			return new BaseCachingPredicates(properties.getMatchTilesSet(), properties.getMatchBlocksPredicate(), isValidForMultipass);
		}
	}
}

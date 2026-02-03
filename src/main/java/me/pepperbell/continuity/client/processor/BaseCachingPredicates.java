package me.pepperbell.continuity.client.processor;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class BaseCachingPredicates implements CachingPredicates {
	@Nullable
	protected Set<Identifier> spriteIdSet;
	@Nullable
	protected Predicate<BlockState> blockStatePredicate;
	protected boolean isValidForMultipass;

	public BaseCachingPredicates(@Nullable Set<Identifier> spriteIdSet, @Nullable Predicate<BlockState> blockStatePredicate, boolean isValidForMultipass) {
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
			return spriteIdSet.contains(sprite.contents().name());
		}
		return false;
	}

	@Override
	public boolean affectsBlockStates() {
		return blockStatePredicate != null;
	}

	@Override
	public boolean affectsBlockState(BlockState state) {
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
		public CachingPredicates createPredicates(T properties, Function<Material, TextureAtlasSprite> spriteGetter) {
			return new BaseCachingPredicates(properties.getMatchTilesSet(), properties.getMatchBlocksPredicate(), isValidForMultipass);
		}
	}
}

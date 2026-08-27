package me.pepperbell.continuity.client.model;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.client.model.BakedModelWrapper;

public class EmissiveItemModelWrapper extends BakedModelWrapper<IBakedModel> {
	public EmissiveItemModelWrapper(IBakedModel originalModel) {
		super(originalModel);
	}

	@Override
	public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
		List<BakedQuad> quads = super.getQuads(state, side, rand);
		ObjectArrayList<BakedQuad> output = new ObjectArrayList<>(quads);
		for (BakedQuad quad : quads) {
			TextureAtlasSprite sprite = quad.getSprite();
			if (sprite == null) {
				continue;
			}
			TextureAtlasSprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				output.add(new EmissiveBakedQuad(quad, emissiveSprite));
			}
		}
		return output;
	}

	/**
	 * Renders go through {@code model.getOverrides().handleItemState(model, ...)}, which hands the
	 * wrapper itself to foreign override lists. Mods such as Tinkers' Construct downcast that
	 * argument back to their own baked model class, so delegate overrides must see the unwrapped
	 * model instead.
	 */
	@Override
	public ItemOverrideList getOverrides() {
		ItemOverrideList original = originalModel.getOverrides();
		if (original == ItemOverrideList.NONE) {
			return original;
		}
		return new UnwrappingItemOverrideList(original);
	}

	private static class UnwrappingItemOverrideList extends ItemOverrideList {
		private final ItemOverrideList delegate;

		UnwrappingItemOverrideList(ItemOverrideList delegate) {
			super(List.of());
			this.delegate = delegate;
		}

		@Override
		public IBakedModel handleItemState(IBakedModel model, ItemStack stack, World world, EntityLivingBase entity) {
			IBakedModel unwrapped = model instanceof EmissiveItemModelWrapper ? ((EmissiveItemModelWrapper) model).originalModel : model;
			IBakedModel handled = delegate.handleItemState(unwrapped, stack, world, entity);
			if (handled == unwrapped) {
				return model;
			}
			return handled;
		}
	}
}

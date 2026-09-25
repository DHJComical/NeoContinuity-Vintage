package me.pepperbell.continuity.client.model;

import java.util.List;

import javax.vecmath.Matrix4f;

import org.apache.commons.lang3.tuple.Pair;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
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
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return quads;
		}
		ObjectArrayList<BakedQuad> output = new ObjectArrayList<>(quads);
		for (BakedQuad quad : quads) {
			TextureAtlasSprite sprite = quad.getSprite();
			if (sprite == null) {
				continue;
			}
			TextureAtlasSprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				BakedQuad overlay = new EmissiveBakedQuad(quad, emissiveSprite);
				// Item rendering uses Forge's per-quad UV1 lightmap only for non-ITEM formats.
				// Block overlays are rebuilt by the chunk transformer after CTM processing.
				if (state == null) {
					overlay = BakedQuadLightmap.withMinimum(overlay, 15, 15);
					overlay = new BakedQuad(overlay.getVertexData(), overlay.getTintIndex(),
							overlay.getFace(), overlay.getSprite(), false, overlay.getFormat());
				}
				output.add(overlay);
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

	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType transformType) {
		Pair<? extends IBakedModel, Matrix4f> perspective = super.handlePerspective(transformType);
		// Forge renders the model returned here. Keep the emissive wrapper when the delegate
		// merely applies a camera transform to itself.
		return perspective.getLeft() == originalModel
				? Pair.of(this, perspective.getRight()) : perspective;
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

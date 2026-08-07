package me.pepperbell.continuity.client.model;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
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
}

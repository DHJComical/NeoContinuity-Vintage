package me.pepperbell.continuity.client;

import java.util.List;

import com.dhj.actinium.api.render.terrain.BlockQuadTransformer;
import com.dhj.actinium.world.cloned.ActiniumBlockAccess;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.model.EmissiveBakedQuad;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class ContinuityCtmTransformer implements BlockQuadTransformer {
	@Override
	public List<BakedQuad> transform(IBlockState state, BlockPos pos, ActiniumBlockAccess blockAccess, BlockRenderLayer layer, EnumFacing side, List<BakedQuad> quads) {
		ObjectArrayList<BakedQuad> output = new ObjectArrayList<>();
		if (quads.isEmpty()) {
			return output;
		}

		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			output.addAll(quads);
		} else {
			long rand = MathHelper.getPositionRandom(pos);
			ProcessingContextImpl context = new ProcessingContextImpl();

			for (BakedQuad quad : quads) {
				TextureAtlasSprite sprite = quad.getSprite();
				if (sprite == null) {
					output.add(quad);
					continue;
				}

				QuadProcessors.Slice slice = QuadProcessors.getCache(state).apply(sprite);
				if (slice.processors().length == 0) {
					output.add(quad);
					continue;
				}

				context.reset();
				boolean discarded = false;
				boolean processed = false;
				for (QuadProcessor processor : slice.processors()) {
					QuadProcessor.ProcessingResult result = processor.processQuad(quad, sprite, blockAccess, pos, state, state, rand, 0, context);
					if (result == QuadProcessor.ProcessingResult.DISCARD) {
						discarded = true;
						break;
					}
					if (result == QuadProcessor.ProcessingResult.NEXT_PROCESSOR) {
						continue;
					}

					if (!context.getExtraQuads().isEmpty()) {
						output.addAll(context.getExtraQuads());
					} else {
						output.add(quad);
					}
					processed = true;
					break;
				}

				if (!discarded && !processed) {
					output.add(quad);
				}
			}
		}

		if (ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			int originalOutputSize = output.size();
			for (int i = 0; i < originalOutputSize; i++) {
				BakedQuad quad = output.get(i);
				TextureAtlasSprite sprite = quad.getSprite();
				if (sprite == null) {
					continue;
				}
				TextureAtlasSprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
				if (emissiveSprite != null) {
					output.add(new EmissiveBakedQuad(quad, emissiveSprite));
				}
			}
		}

		return output;
	}
}

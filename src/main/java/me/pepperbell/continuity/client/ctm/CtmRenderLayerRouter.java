package me.pepperbell.continuity.client.ctm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

/** Routes CTM metadata's per-texture layers through the block's normal layer checks. */
public final class CtmRenderLayerRouter {
	private static volatile Snapshot snapshot = new Snapshot(Map.of(), Set.of());
	private static final Map<IBlockState, Set<BlockRenderLayer>> modelLayers = new ConcurrentHashMap<>();
	private static final Set<BlockLayer> forcedLayers = ConcurrentHashMap.newKeySet();
	private static final ThreadLocal<Boolean> scanningModel = ThreadLocal.withInitial(() -> false);

	private CtmRenderLayerRouter() {
	}

	public static void reload(List<CtmDefinition> definitions) {
		Map<String, LayerRule> next = new HashMap<>();
		for (CtmDefinition definition : definitions) {
			if (definition.getLayer() != null) {
				next.putIfAbsent(definition.getResourceId().toString(),
						new LayerRule(definition.getLayer(), definition.hasEmissiveFallback()));
			}
		}
		modelLayers.clear();
		forcedLayers.clear();
		Set<BlockRenderLayer> targetLayers = EnumSet.noneOf(BlockRenderLayer.class);
		for (LayerRule rule : next.values()) {
			targetLayers.add(rule.layer());
		}
		snapshot = new Snapshot(Map.copyOf(next), Set.copyOf(targetLayers));
	}

	public static boolean allowAdditionalLayer(IBlockState state, BlockRenderLayer layer) {
		if (!ContinuityConfig.INSTANCE.connectedTextures.get() || !ContinuityConfig.INSTANCE.ctmModTextures.get()
				|| !snapshot.targetLayers().contains(layer) || scanningModel.get()) {
			return false;
		}
		boolean modelHasLayer = modelLayers.computeIfAbsent(state, CtmRenderLayerRouter::findModelLayers).contains(layer);
		if (modelHasLayer) {
			forcedLayers.add(new BlockLayer(state, layer));
			return true;
		}
		return false;
	}

	public static boolean isRoutedLayer(IBlockState state, BlockRenderLayer layer) {
		return forcedLayers.contains(new BlockLayer(state, layer));
	}

	public static boolean shouldRender(TextureAtlasSprite sprite, BlockRenderLayer layer, boolean routedLayer) {
		LayerRule rule = sprite == null ? null : snapshot.spriteLayers().get(sprite.getIconName());
		return rule == null ? !routedLayer : rule.layer() == layer || (rule.emissiveFallback() && !routedLayer);
	}

	public static boolean shouldProcessWrappedOverlay(TextureAtlasSprite sprite, BlockRenderLayer layer,
			boolean routedLayer) {
		return shouldRender(sprite, layer, routedLayer);
	}

	public static boolean shouldGenerateSuffixOverlay(TextureAtlasSprite sprite) {
		return sprite != null && !snapshot.spriteLayers().containsKey(sprite.getIconName());
	}

	public static boolean shouldFullbrightEmissiveFallback(TextureAtlasSprite sprite, boolean routedLayer) {
		LayerRule rule = sprite == null ? null : snapshot.spriteLayers().get(sprite.getIconName());
		return rule != null && rule.emissiveFallback() && !routedLayer;
	}

	private static Set<BlockRenderLayer> findModelLayers(IBlockState state) {
		EnumSet<BlockRenderLayer> found = EnumSet.noneOf(BlockRenderLayer.class);
		BlockRenderLayer previousLayer = MinecraftForgeClient.getRenderLayer();
		scanningModel.set(true);
		try {
			IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(state);
			for (BlockRenderLayer candidate : snapshot.targetLayers()) {
				ForgeHooksClient.setRenderLayer(candidate);
				for (EnumFacing face : EnumFacing.VALUES) {
					collectLayers(model.getQuads(state, face, 0), found);
				}
				collectLayers(model.getQuads(state, null, 0), found);
			}
		} catch (RuntimeException e) {
			ContinuityClient.LOGGER.warn("Could not inspect block model layers for CTM metadata on '{}'", state, e);
		} finally {
			ForgeHooksClient.setRenderLayer(previousLayer);
			scanningModel.remove();
		}
		return found;
	}

	private static void collectLayers(List<BakedQuad> quads, Set<BlockRenderLayer> found) {
		for (BakedQuad quad : quads) {
			TextureAtlasSprite sprite = quad.getSprite();
			if (sprite != null) {
				LayerRule rule = snapshot.spriteLayers().get(sprite.getIconName());
				if (rule != null) {
					found.add(rule.layer());
				}
			}
		}
	}

	private record BlockLayer(IBlockState state, BlockRenderLayer layer) {
	}

	private record LayerRule(BlockRenderLayer layer, boolean emissiveFallback) {
	}

	private record Snapshot(Map<String, LayerRule> spriteLayers, Set<BlockRenderLayer> targetLayers) {
	}
}

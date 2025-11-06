package me.pepperbell.continuity.client.resource;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import com.google.common.collect.ImmutableMap;

import me.pepperbell.continuity.client.model.CtmBakedModel;
import me.pepperbell.continuity.client.model.EmissiveBakedModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.block.BlockModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.MissingModel;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class ModelWrappingHandler {
	@Nullable
	private static volatile ModelWrappingHandler instance;

	private final boolean wrapCtm;
	private final boolean wrapEmissive;
	private final ImmutableMap<ModelIdentifier, BlockState> blockStateModelIds;

	private ModelWrappingHandler(boolean wrapCtm, boolean wrapEmissive) {
		this.wrapCtm = wrapCtm;
		this.wrapEmissive = wrapEmissive;
		blockStateModelIds = createBlockStateModelIdMap();
	}

	@Nullable
	public static ModelWrappingHandler getInstance() {
		return instance;
	}

	public static void setInstance(boolean wrapCtm, boolean wrapEmissive) {
		if (!wrapCtm && !wrapEmissive) {
			return;
		}
		instance = new ModelWrappingHandler(wrapCtm, wrapEmissive);
	}

	public static void resetInstance() {
		instance = null;
	}

	private static ImmutableMap<ModelIdentifier, BlockState> createBlockStateModelIdMap() {
		ImmutableMap.Builder<ModelIdentifier, BlockState> builder = ImmutableMap.builder();
		// Match code of BakedModelManager#bake
		for (Block block : Registries.BLOCK) {
			Identifier blockId = block.getRegistryEntry().registryKey().getValue();
			for (BlockState state : block.getStateManager().getStates()) {
				ModelIdentifier modelId = BlockModels.getModelId(blockId, state);
				builder.put(modelId, state);
			}
		}
		return builder.build();
	}

	public BakedModel wrap(@Nullable BakedModel model, @UnknownNullability Identifier resourceId, @UnknownNullability ModelIdentifier topLevelId) {
		if (model != null && !model.isBuiltin() && (resourceId == null || !resourceId.equals(MissingModel.ID))) {
			if (wrapCtm) {
				if (topLevelId != null) {
					BlockState state = blockStateModelIds.get(topLevelId);
					if (state != null) {
						model = new CtmBakedModel(model, state);
					}
				}
			}
			if (wrapEmissive) {
				model = new EmissiveBakedModel(model);
			}
		}
		return model;
	}

	public static void init() {
		ModelLoadingPlugin.register(pluginCtx -> {
			pluginCtx.modifyModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) -> {
				ModelWrappingHandler wrappingHandler = getInstance();
				if (wrappingHandler != null) {
					return wrappingHandler.wrap(model, ctx.resourceId(), ctx.topLevelId());
				}
				return model;
			});
		});
	}
}

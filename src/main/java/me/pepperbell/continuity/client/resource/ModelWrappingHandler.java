package me.pepperbell.continuity.client.resource;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.model.CtmBlockStateModel;
import me.pepperbell.continuity.client.model.EmissiveBlockStateModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BlockStateModel;

public class ModelWrappingHandler {
	@Nullable
	private static volatile ModelWrappingHandler instance;

	private final boolean wrapCtm;
	private final boolean wrapEmissive;

	private ModelWrappingHandler(boolean wrapCtm, boolean wrapEmissive) {
		this.wrapCtm = wrapCtm;
		this.wrapEmissive = wrapEmissive;
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

	public BlockStateModel wrapBlock(BlockStateModel model, BlockState state) {
		if (wrapCtm) {
			model = new CtmBlockStateModel(model, state);
		}
		if (wrapEmissive) {
			model = new EmissiveBlockStateModel(model);
		}
		return model;
	}

	public static void init() {
		ModelLoadingPlugin.register(pluginCtx -> {
			pluginCtx.modifyBlockModelAfterBake().register(ModelModifier.WRAP_LAST_PHASE, (model, ctx) -> {
				ModelWrappingHandler wrappingHandler = getInstance();
				if (wrappingHandler != null) {
					return wrappingHandler.wrapBlock(model, ctx.state());
				}
				return model;
			});
		});
	}
}

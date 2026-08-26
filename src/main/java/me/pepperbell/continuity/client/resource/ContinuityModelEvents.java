package me.pepperbell.continuity.client.resource;

import me.pepperbell.continuity.client.model.EmissiveItemModelWrapper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ContinuityModelEvents {
	/** Binary name of Forge's {@code FancyMissingModel.BakedModel} (package-private class). */
	private static final String FANCY_MISSING_MODEL_CLASS = "net.minecraftforge.client.model.FancyMissingModel$BakedModel";

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
		IBakedModel missingModel = event.getModelManager().getMissingModel();
		for (ModelResourceLocation location : registry.getKeys()) {
			IBakedModel model = registry.getObject(location);
			if (model == null) {
				continue;
			}
			if (FANCY_MISSING_MODEL_CLASS.equals(model.getClass().getName())) {
				// Forge's fancy missing variant lazily renders a "missing texture" label through
				// the font renderer on the first getQuads call, which requires a GL context.
				// Replace it with the plain missing model (no quads, no font rendering) so chunk
				// meshes can be built on worker threads, and skip wrapping it.
				registry.putObject(location, missingModel);
				continue;
			}
			registry.putObject(location, new EmissiveItemModelWrapper(model));
		}
	}
}
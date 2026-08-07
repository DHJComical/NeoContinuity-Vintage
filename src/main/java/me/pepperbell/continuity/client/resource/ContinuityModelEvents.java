package me.pepperbell.continuity.client.resource;

import me.pepperbell.continuity.client.model.EmissiveItemModelWrapper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ContinuityModelEvents {
	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
		for (ModelResourceLocation location : registry.getKeys()) {
			IBakedModel model = registry.getObject(location);
			if (model != null) {
				registry.putObject(location, new EmissiveItemModelWrapper(model));
			}
		}
	}
}

package me.pepperbell.continuity.client;

import me.pepperbell.continuity.client.model.CtmBlockStateModel;
import me.pepperbell.continuity.client.model.EmissiveBlockStateModel;
import me.pepperbell.continuity.client.resource.CtmResourceReloader;
import me.pepperbell.continuity.client.resource.ModelWrappingHandler;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.Map;

@EventBusSubscriber(modid = ContinuityClient.ID, value = Dist.CLIENT)
public class ContinuityClientEvents {

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		ProcessingDataKeyRegistryImpl.INSTANCE.setFrozen();
	}

	@SubscribeEvent
	public static void onRegisterResourceReloadListeners(AddClientReloadListenersEvent event) {
		event.addListener(CtmResourceReloader.ID, CtmResourceReloader.INSTANCE);
	}

	@SubscribeEvent
	public static void onAddPackFinders(AddPackFindersEvent event) {
		event.addPackFinders(ContinuityClient.asId("resourcepacks/default"), PackType.CLIENT_RESOURCES, Component.translatable("resourcePack.continuity.default.name"), PackSource.BUILT_IN, false, Pack.Position.TOP);
		event.addPackFinders(ContinuityClient.asId("resourcepacks/glass_pane_culling_fix"), PackType.CLIENT_RESOURCES, Component.translatable("resourcePack.continuity.glass_pane_culling_fix.name"), PackSource.BUILT_IN, false, Pack.Position.TOP);
	}

	@SubscribeEvent
	public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
		if (Minecraft.getInstance().level == null) {
			return;
		}

		BiomeHolderManager.registryManager = Minecraft.getInstance().level.registryAccess();
		BiomeHolderManager.refreshHolders();
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
		boolean wrapCtm = ModelWrappingHandler.WRAP_CTM_FUTURE_KEY.join();
		boolean wrapEmissive = ModelWrappingHandler.WRAP_EMISSIVE_FUTURE_KEY.join();

		Map<BlockState, BlockStateModel> models = event.getBakingResult().blockStateModels();

		for (Map.Entry<BlockState, BlockStateModel> entry : models.entrySet()) {
			BlockState state = entry.getKey();
			BlockStateModel model = entry.getValue();

			if (wrapCtm) {
				model = new CtmBlockStateModel(model, state);
			}

			if (wrapEmissive) {
				model = new EmissiveBlockStateModel(model);
			}

			models.put(state, model);
		}
	}
}

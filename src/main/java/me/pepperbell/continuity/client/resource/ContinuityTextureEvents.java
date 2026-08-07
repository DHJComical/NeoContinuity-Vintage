package me.pepperbell.continuity.client.resource;

import java.util.List;
import java.util.Map;

import me.pepperbell.continuity.impl.client.EmissiveSpriteApiImpl;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ContinuityTextureEvents {
	private CtmPropertiesLoader.LoadingResult lastResult;

	@SubscribeEvent
	public void onTextureStitchPre(TextureStitchEvent.Pre event) {
		EmissiveSuffixLoader.load(Minecraft.getMinecraft().getResourceManager());
		EmissiveSpriteApiImpl.INSTANCE.clear();
		lastResult = CtmPropertiesLoader.loadAll();
		TextureMap textureMap = event.getMap();
		ContinuityClient.LOGGER.debug("Registering {} redirected CTM sprite dependencies", lastResult.getBlockAtlasSpriteDependencies().size());
		for (ResourceLocation spriteId : lastResult.getBlockAtlasSpriteDependencies()) {
			textureMap.setTextureEntry(new RedirectedTextureAtlasSprite(spriteId));
		}
	}

	@SubscribeEvent
	public void onTextureStitchPost(TextureStitchEvent.Post event) {
		if (lastResult == null) {
			lastResult = CtmPropertiesLoader.loadAll();
		}
		TextureMap textureMap = Minecraft.getMinecraft().getTextureMapBlocks();
		List<QuadProcessors.ProcessorHolder> processorHolders = lastResult.createProcessorHolders(id -> textureMap.getAtlasSprite(id.toString()));
		ContinuityClient.LOGGER.debug("Reloaded {} CTM processor holders", processorHolders.size());
		QuadProcessors.reload(processorHolders);
		String suffix = EmissiveSuffixLoader.getEmissiveSuffix();
		if (suffix != null && !suffix.isEmpty()) {
			for (Map.Entry<String, TextureAtlasSprite> entry : textureMap.mapUploadedSprites.entrySet()) {
				if (entry.getKey().endsWith(suffix)) {
					String baseKey = entry.getKey().substring(0, entry.getKey().length() - suffix.length());
					TextureAtlasSprite base = textureMap.mapUploadedSprites.get(baseKey);
					if (base != null) {
						EmissiveSpriteApiImpl.INSTANCE.setEmissiveSprite(base, entry.getValue());
					}
				}
			}
		}
		lastResult = null;
	}
}

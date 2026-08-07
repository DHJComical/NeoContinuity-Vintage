package me.pepperbell.continuity.client.mixin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.pepperbell.continuity.client.resource.EmissiveSuffixLoader;
import me.pepperbell.continuity.client.resource.SpriteSourceListInitContext;
import me.pepperbell.continuity.client.resource.SpriteSourceListListContext;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

@Mixin(SpriteSourceList.class)
abstract class SpriteSourceListMixin {
	@ModifyVariable(method = "<init>(Ljava/util/List;)V", at = @At(value = "LOAD", ordinal = 0), argsOnly = true, ordinal = 0)
	// private List<SpriteSource> continuity$modifySources(List<SpriteSource> sources) {
	private static List<SpriteSource> continuity$modifySources(List<SpriteSource> sources) {
		SpriteSourceListInitContext context = SpriteSourceListInitContext.THREAD_LOCAL.get();
		if (context != null) {
			Set<Identifier> extraIds = context.getExtraIds();
			if (!extraIds.isEmpty()) {
				List<SpriteSource> extraSources = new ObjectArrayList<>();
				for (Identifier extraId : extraIds) {
					extraSources.add(new SingleFile(extraId, Optional.empty()));
				}

				if (sources instanceof ArrayList) {
					sources.addAll(0, extraSources);
				} else {
					List<SpriteSource> mutableSources = new ArrayList<>(extraSources);
					mutableSources.addAll(sources);
					return mutableSources;
				}
			}
		}
		return sources;
	}

	// @Inject(method = "list(Lnet/minecraft/server/packs/resources/ResourceManager;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
	@Inject(method = "list(Lnet/minecraft/server/packs/resources/ResourceManager;Ljava/util/Set;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableList;builder()Lcom/google/common/collect/ImmutableList$Builder;", remap = false), locals = LocalCapture.CAPTURE_FAILHARD)
	// private void continuity$afterLoadSources(ResourceManager resourceManager, CallbackInfoReturnable<List<Function<SpriteResourceLoader, SpriteContents>>> cir, Map<Identifier, SpriteSource.DiscardableLoader> loaders) {
	private void continuity$afterLoadSources(ResourceManager resourceManager, Set<MetadataSectionType<?>> additionalMetadata, CallbackInfoReturnable<List<Function<SpriteResourceLoader, SpriteContents>>> cir, Map<Identifier, SpriteSource.DiscardableLoader> loaders) {
		SpriteSourceListListContext context = SpriteSourceListListContext.THREAD_LOCAL.get();
		if (context != null) {
			String emissiveSuffix = EmissiveSuffixLoader.getEmissiveSuffix();
			if (emissiveSuffix != null) {
				Map<Identifier, SpriteSource.DiscardableLoader> emissiveLoaders = new Object2ObjectOpenHashMap<>();
				Map<Identifier, Identifier> emissiveIdMap = new Object2ObjectOpenHashMap<>();
				loaders.forEach((id, supplier) -> {
					if (!id.getPath().endsWith(emissiveSuffix)) {
						Identifier emissiveId = id.withPath(id.getPath() + emissiveSuffix);
						if (!loaders.containsKey(emissiveId)) {
							Identifier emissiveLocation = emissiveId.withPath("textures/" + emissiveId.getPath() + ".png");
							Optional<Resource> optionalResource = resourceManager.getResource(emissiveLocation);
							if (optionalResource.isPresent()) {
								Resource resource = optionalResource.get();
								emissiveLoaders.put(emissiveId, resourceLoader -> resourceLoader.loadSprite(emissiveId, resource));
								emissiveIdMap.put(id, emissiveId);
							}
						} else {
							emissiveIdMap.put(id, emissiveId);
						}
					}
				});
				loaders.putAll(emissiveLoaders);
				context.setEmissiveIdMap(emissiveIdMap);
			} else {
				context.setEmissiveIdMap(Collections.emptyMap());
			}
		}
	}
}

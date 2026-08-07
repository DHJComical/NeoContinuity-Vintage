package me.pepperbell.continuity.client.resource;

import java.io.InputStream;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.CtmLoader;
import me.pepperbell.continuity.api.client.CtmLoaderRegistry;
import me.pepperbell.continuity.api.client.CtmProperties;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.model.QuadProcessors;
import me.pepperbell.continuity.client.util.biome.BiomeHolderManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public class CtmPropertiesLoader {
	private final ResourceManager resourceManager;
	private final List<LoadingContainer<?>> containers = new ObjectArrayList<>();
	private final Set<Identifier> blockAtlasSpriteDependencies = new ObjectOpenHashSet<>();

	private CtmPropertiesLoader(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	public static LoadingResult loadAllWithState(ResourceManager resourceManager) {
		// TODO: move these to the very beginning of resource reload
		BiomeHolderManager.clearCache();

		LoadingResult result = loadAll(resourceManager);

		// TODO: move these to the very end of resource reload
		BiomeHolderManager.refreshHolders();

		return result;
	}

	public static LoadingResult loadAll(ResourceManager resourceManager) {
		return new CtmPropertiesLoader(resourceManager).loadAll();
	}

	private LoadingResult loadAll() {
		int packPriority = 0;
		Iterator<PackResources> iterator = resourceManager.listPacks().iterator();
		while (iterator.hasNext()) {
			PackResources pack = iterator.next();
			loadAll(pack, packPriority);
			packPriority++;
		}

		containers.sort(Comparator.reverseOrder());

		return new LoadingResult(containers, blockAtlasSpriteDependencies);
	}

	private void loadAll(PackResources pack, int packPriority) {
		for (String namespace : pack.getNamespaces(PackType.CLIENT_RESOURCES)) {
			pack.listResources(PackType.CLIENT_RESOURCES, namespace, "optifine/ctm", (resourceId, inputSupplier) -> {
				if (resourceId.getPath().endsWith(".properties")) {
					try (InputStream stream = inputSupplier.get()) {
						Properties properties = new Properties();
						properties.load(stream);
						load(properties, resourceId, pack, packPriority);
					} catch (Exception e) {
						ContinuityClient.LOGGER.error("Failed to load CTM properties from file '" + resourceId + "' in pack '" + pack.packId() + "'", e);
					}
				}
			});
		}
	}

	private void load(Properties properties, Identifier resourceId, PackResources pack, int packPriority) {
		String method = properties.getProperty("method", "ctm").trim();
		CtmLoader<?> loader = CtmLoaderRegistry.get().getLoader(method);
		if (loader != null) {
			load(loader, properties, resourceId, pack, packPriority, method);
		} else {
			ContinuityClient.LOGGER.error("Unknown 'method' value '" + method + "' in file '" + resourceId + "' in pack '" + pack.packId() + "'");
		}
	}

	private <T extends CtmProperties> void load(CtmLoader<T> loader, Properties properties, Identifier resourceId, PackResources pack, int packPriority, String method) {
		T ctmProperties = loader.getPropertiesFactory().createProperties(properties, resourceId, pack, packPriority, resourceManager, method);
		if (ctmProperties != null) {
			LoadingContainer<T> container = new LoadingContainer<>(loader, ctmProperties);
			containers.add(container);
			blockAtlasSpriteDependencies.addAll(ctmProperties.getSpriteDependencies());
		}
	}

	private record LoadingContainer<T extends CtmProperties>(CtmLoader<T> loader, T properties) implements Comparable<LoadingContainer<?>> {
		public QuadProcessors.ProcessorHolder toProcessorHolder(Function<Identifier, TextureAtlasSprite> spriteGetter) {
			QuadProcessor processor = loader.getProcessorFactory().createProcessor(properties, spriteGetter);
			CachingPredicates predicates = loader.getPredicatesFactory().createPredicates(properties, spriteGetter);
			return new QuadProcessors.ProcessorHolder(processor, predicates);
		}

		@Override
		public int compareTo(@NotNull LoadingContainer<?> o) {
			return properties.compareTo(o.properties);
		}
	}

	public static class LoadingResult {
		private final List<LoadingContainer<?>> containers;
		private final Set<Identifier> blockAtlasSpriteDependencies;

		private LoadingResult(List<LoadingContainer<?>> containers, Set<Identifier> blockAtlasSpriteDependencies) {
			this.containers = containers;
			this.blockAtlasSpriteDependencies = blockAtlasSpriteDependencies;
		}

		public List<QuadProcessors.ProcessorHolder> createProcessorHolders(Function<Identifier, TextureAtlasSprite> spriteGetter) {
			List<QuadProcessors.ProcessorHolder> processorHolders = new ObjectArrayList<>();
			for (LoadingContainer<?> container : containers) {
				processorHolders.add(container.toProcessorHolder(spriteGetter));
			}
			return processorHolders;
		}

		public Set<Identifier> getBlockAtlasSpriteDependencies() {
			return blockAtlasSpriteDependencies;
		}
	}
}

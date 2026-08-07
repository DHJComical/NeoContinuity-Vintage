package me.pepperbell.continuity.client.util.biome;

import java.util.Map;

import javax.annotation.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.world.biome.Biome;

public final class BiomeHolder {
	private final ResourceLocation id;
	@Nullable
	private Biome biome;

	BiomeHolder(ResourceLocation id) {
		this.id = id;
	}

	public ResourceLocation getId() {
		return id;
	}

	@Nullable
	public Biome getBiome() {
		return biome;
	}

	void refresh(RegistryNamespaced<ResourceLocation, Biome> biomeRegistry, Map<ResourceLocation, ResourceLocation> compactIdMap) {
		ResourceLocation id = compactIdMap.get(this.id);
		if (id == null) {
			id = this.id;
		}
		if (biomeRegistry.containsKey(id)) {
			biome = biomeRegistry.getObject(id);
		} else {
			ContinuityClient.LOGGER.warn("Unknown biome '" + this.id + "'");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		BiomeHolder that = (BiomeHolder) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}

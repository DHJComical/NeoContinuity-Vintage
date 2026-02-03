package me.pepperbell.continuity.client.util.biome;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;

public final class BiomeHolder {
	private final Identifier id;
	@Nullable
	private Biome biome;

	BiomeHolder(Identifier id) {
		this.id = id;
	}

	public Identifier getId() {
		return id;
	}

	@Nullable
	public Biome getBiome() {
		return biome;
	}

	void refresh(Registry<Biome> biomeRegistry, Map<Identifier, Identifier> compactIdMap) {
		Identifier id = compactIdMap.get(this.id);
		if (id == null) {
			id = this.id;
		}
		if (biomeRegistry.containsKey(id)) {
			biome = biomeRegistry.getValue(id);
		} else {
			ContinuityClient.LOGGER.warn("Unknown biome '" + this.id + "'");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BiomeHolder that = (BiomeHolder) o;
		return id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}
}

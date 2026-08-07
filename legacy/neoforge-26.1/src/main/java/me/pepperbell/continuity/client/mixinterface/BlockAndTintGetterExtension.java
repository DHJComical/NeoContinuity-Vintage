package me.pepperbell.continuity.client.mixinterface;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;

public interface BlockAndTintGetterExtension {

	boolean continuity$hasBiome();
	Holder<Biome> continuity$getBiome(BlockPos pos);
}

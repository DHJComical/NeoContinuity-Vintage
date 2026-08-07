package me.pepperbell.continuity.client.mixin.biomes;

import me.pepperbell.continuity.client.mixinterface.BlockAndTintGetterExtension;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockAndTintGetter.class)
public interface BlockAndTintGetterMixin extends BlockAndTintGetterExtension {
	@Override
	default boolean continuity$hasBiome() {
		return false;
	}

	@Override
	default Holder<Biome> continuity$getBiome(BlockPos pos) {
		return null;
	}
}

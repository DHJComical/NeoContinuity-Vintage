package me.pepperbell.continuity.client.mixin.biomes;

import me.pepperbell.continuity.client.mixinterface.BlockAndTintGetterExtension;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSectionRegion.class)
public class RenderSectionRegionMixin implements BlockAndTintGetterExtension {
	@Shadow
	@Final
	private ClientLevel level;

	@Override
	public boolean continuity$hasBiome() {
		return true;
	}

	@Override
	public Holder<Biome> continuity$getBiome(BlockPos pos) {
		return level.getBiome(pos);
	}
}

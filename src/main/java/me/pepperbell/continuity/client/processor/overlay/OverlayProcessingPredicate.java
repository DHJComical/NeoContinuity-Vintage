package me.pepperbell.continuity.client.processor.overlay;

import java.util.EnumSet;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.BaseProcessingPredicate;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public class OverlayProcessingPredicate extends BaseProcessingPredicate {
	public OverlayProcessingPredicate(@Nullable EnumSet<Direction> faces, @Nullable Predicate<Biome> biomePredicate, @Nullable IntPredicate heightPredicate, @Nullable Predicate<String> blockEntityNamePredicate) {
		super(faces, biomePredicate, heightPredicate, blockEntityNamePredicate);
	}

	@Override
	public boolean shouldProcessQuad(QuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, ProcessingDataProvider dataProvider) {
		if (!super.shouldProcessQuad(quad, sprite, level, pos, appearanceState, state, dataProvider)) {
			return false;
		}
		return QuadUtil.isQuadUnitSquare(quad);
	}

	public static OverlayProcessingPredicate fromProperties(BaseCtmProperties properties) {
		return new OverlayProcessingPredicate(properties.getFaces(), properties.getBiomePredicate(), properties.getHeightPredicate(), properties.getBlockEntityNamePredicate());
	}
}

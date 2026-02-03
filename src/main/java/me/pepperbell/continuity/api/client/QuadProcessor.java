package me.pepperbell.continuity.api.client;

import java.util.function.Function;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public interface QuadProcessor {
	ProcessingResult processQuad(MutableQuadView quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context);

	interface ProcessingContext extends ProcessingDataProvider {
		QuadEmitter getExtraQuadEmitter();
	}

	enum ProcessingResult {
		NEXT_PROCESSOR,
		NEXT_PASS,
		STOP,
		DISCARD;
	}

	interface Factory<T extends CtmProperties> {
		QuadProcessor createProcessor(T properties, Function<Material, TextureAtlasSprite> spriteGetter);
	}
}

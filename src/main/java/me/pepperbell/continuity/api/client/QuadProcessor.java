package me.pepperbell.continuity.api.client;

import java.util.function.Function;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public interface QuadProcessor {
	ProcessingResult processQuad(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, ProcessingContext context);

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
		QuadProcessor createProcessor(T properties, Function<SpriteIdentifier, Sprite> textureGetter);
	}
}

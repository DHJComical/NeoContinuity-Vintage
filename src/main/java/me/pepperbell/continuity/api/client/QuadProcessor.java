package me.pepperbell.continuity.api.client;

import java.util.function.Function;

// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import me.pepperbell.continuity.client.model.QuadCollectionBuilder;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;

public interface QuadProcessor {
	ProcessingResult processQuad(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context);

	interface ProcessingContext extends ProcessingDataProvider {
		/* QuadEmitter */ QuadCollectionBuilder getExtraQuadEmitter();
	}

	enum ProcessingResult {
		NEXT_PROCESSOR,
		NEXT_PASS,
		STOP,
		DISCARD;
	}

	interface Factory<T extends CtmProperties> {
		QuadProcessor createProcessor(T properties, Function<Identifier, TextureAtlasSprite> spriteGetter);
	}
}

package me.pepperbell.continuity.client.model;

import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public class CtmBlockStateModel extends WrapperBlockStateModel {
	public static final int PASSES = 4;

	protected final BlockState defaultState;
	protected volatile Function<TextureAtlasSprite, QuadProcessors.Slice> defaultSliceFunc;

	public CtmBlockStateModel(BlockStateModel wrapped, BlockState defaultState) {
		super(wrapped);
		this.defaultState = defaultState;
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, Predicate<@Nullable Direction> cullTest) {
		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}

		CtmQuadTransform quadTransform = container.ctmQuadTransform;
		if (quadTransform.isActive()) {
			super.emitQuads(emitter, level, pos, state, random, cullTest);
			return;
		}

		// The correct way to get the appearance of the origin state from within a block model is to call getAppearance
		// on the passed world state and pass the pos and world state of the adjacent block as the source pos and source
		// state.
		// The latter of these is not possible here because the appearance state is necessary to get the slice and only
		// the processors within the slice actually perform checks on adjacent blocks. Likewise, the processors
		// themselves cannot retrieve the appearance state since the correct processors can only be chosen with the
		// initially correct appearance state.
		// Additionally, the side is chosen to always be the first constant of the enum (DOWN) for simplicity. Querying
		// the appearance for all six sides would be more correct, but less efficient. This may be fixed in the future,
		// especially if there is an actual use case for it.
		BlockState appearanceState = state.getAppearance(level, pos, Direction.DOWN, state, pos);

		// It would be better to use random.nextLong() as the seed, but that changes the state of the random, which
		// cannot happen.
		quadTransform.prepare(level, pos, appearanceState, state, state.getSeed(pos), cullTest, getSliceFunc(appearanceState));

		emitter.pushTransform(quadTransform);
		super.emitQuads(emitter, level, pos, state, random, cullTest);
		emitter.popTransform();

		quadTransform.processingContext.outputTo(emitter);
		quadTransform.reset();
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
		if (!ContinuityConfig.INSTANCE.connectedTextures.get()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getConnectedTexturesState().isEnabled()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		CtmQuadTransform quadTransform = container.ctmQuadTransform;
		if (quadTransform.isActive()) {
			return super.createGeometryKey(level, pos, state, random);
		}

		// This would be nice to implement, but it's not as important as for a typical model since this CTM wrapper is
		// always supposed to be at the top level, and implementing this is far from trivial.
		return null;
	}

	protected Function<TextureAtlasSprite, QuadProcessors.Slice> getSliceFunc(BlockState state) {
		if (state == defaultState) {
			Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc = defaultSliceFunc;
			if (sliceFunc == null) {
				synchronized (this) {
					sliceFunc = defaultSliceFunc;
					if (sliceFunc == null) {
						sliceFunc = QuadProcessors.getCache(state);
						defaultSliceFunc = sliceFunc;
					}
				}
			}
			return sliceFunc;
		}
		return QuadProcessors.getCache(state);
	}

	protected static class CtmQuadTransform implements QuadTransform {
		protected final ProcessingContextImpl processingContext = new ProcessingContextImpl();
		protected final RandomSource random = RandomSource.createNewThreadLocalInstance();

		protected BlockAndTintGetter level;
		protected BlockPos pos;
		protected BlockState appearanceState;
		protected BlockState state;
		protected long randomSeed;
		protected Predicate<@Nullable Direction> cullTest;
		protected Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc;

		protected boolean active;

		@Override
		public boolean transform(MutableQuadView quad) {
			if (cullTest.test(quad.cullFace())) {
				return false;
			}

			for (int pass = 0; pass < PASSES; pass++) {
				Boolean result = transformOnce(quad, pass);
				if (result != null) {
					return result;
				}
			}

			return true;
		}

		protected Boolean transformOnce(MutableQuadView quad, int pass) {
			TextureAtlasSprite sprite = RenderUtil.getSpriteFinder().find(quad);
			QuadProcessors.Slice slice = sliceFunc.apply(sprite);
			QuadProcessor[] processors = pass == 0 ? slice.processors() : slice.multipassProcessors();
			for (QuadProcessor processor : processors) {
				random.setSeed(randomSeed);
				QuadProcessor.ProcessingResult result = processor.processQuad(quad, sprite, level, pos, appearanceState, state, random, pass, processingContext);
				if (result == QuadProcessor.ProcessingResult.NEXT_PROCESSOR) {
					continue;
				}
				if (result == QuadProcessor.ProcessingResult.NEXT_PASS) {
					return null;
				}
				if (result == QuadProcessor.ProcessingResult.STOP) {
					return true;
				}
				if (result == QuadProcessor.ProcessingResult.DISCARD) {
					return false;
				}
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, long randomSeed, Predicate<@Nullable Direction> cullTest, Function<TextureAtlasSprite, QuadProcessors.Slice> sliceFunc) {
			this.level = level;
			this.pos = pos;
			this.appearanceState = appearanceState;
			this.state = state;
			this.randomSeed = randomSeed;
			this.cullTest = cullTest;
			this.sliceFunc = sliceFunc;

			active = true;
		}

		public void reset() {
			level = null;
			pos = null;
			appearanceState = null;
			state = null;
			cullTest = null;
			sliceFunc = null;

			active = false;

			processingContext.reset();
		}
	}
}

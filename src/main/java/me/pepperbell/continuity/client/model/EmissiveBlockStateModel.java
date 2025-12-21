package me.pepperbell.continuity.client.model;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class EmissiveBlockStateModel extends WrapperBlockStateModel {
	public EmissiveBlockStateModel(BlockStateModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitQuads(QuadEmitter emitter, BlockRenderView blockView, BlockPos pos, BlockState state, Random random, Predicate<@Nullable Direction> cullTest) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.emitQuads(emitter, blockView, pos, state, random, cullTest);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			super.emitQuads(emitter, blockView, pos, state, random, cullTest);
			return;
		}

		EmissiveQuadTransform quadTransform = container.emissiveQuadTransform;
		if (quadTransform.isActive()) {
			super.emitQuads(emitter, blockView, pos, state, random, cullTest);
			return;
		}

		MutableMesh mutableMesh = container.mutableMesh;
		quadTransform.prepare(mutableMesh.emitter(), state, cullTest);

		emitter.pushTransform(quadTransform);
		super.emitQuads(emitter, blockView, pos, state, random, cullTest);
		emitter.popTransform();

		mutableMesh.outputTo(emitter);
		mutableMesh.clear();
		quadTransform.reset();
	}

	@Override
	@Nullable
	public Object createGeometryKey(BlockRenderView blockView, BlockPos pos, BlockState state, Random random) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return super.createGeometryKey(blockView, pos, state, random);
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			return super.createGeometryKey(blockView, pos, state, random);
		}

		EmissiveQuadTransform quadTransform = container.emissiveQuadTransform;
		if (quadTransform.isActive()) {
			return super.createGeometryKey(blockView, pos, state, random);
		}

		Object subkey = super.createGeometryKey(blockView, pos, state, random);
		if (subkey == null) {
			return null;
		}

		record Key(Object subkey) {
		}

		return new Key(subkey);
	}

	protected static class EmissiveQuadTransform implements QuadTransform {
		protected QuadEmitter emitter;
		protected BlockState state;
		protected Predicate<@Nullable Direction> cullTest;

		protected boolean active;
		protected boolean calculateDefaultLayer;
		protected boolean isDefaultLayerSolid;

		@Override
		public boolean transform(MutableQuadView quad) {
			if (cullTest.test(quad.cullFace())) {
				return false;
			}

			Sprite sprite = RenderUtil.getSpriteFinder().find(quad);
			Sprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				emitter.copyFrom(quad);
				emitter.emissive(true).diffuseShade(false).ambientOcclusion(TriState.FALSE);

				BlockRenderLayer renderLayer = quad.renderLayer();
				if (renderLayer == null) {
					if (calculateDefaultLayer) {
						isDefaultLayerSolid = BlockRenderLayers.getBlockLayer(state) == BlockRenderLayer.SOLID;
						calculateDefaultLayer = false;
					}

					if (isDefaultLayerSolid) {
						emitter.renderLayer(BlockRenderLayer.CUTOUT);
					}
				} else if (renderLayer == BlockRenderLayer.SOLID) {
					emitter.renderLayer(BlockRenderLayer.CUTOUT);
				}

				QuadUtil.interpolate(emitter, sprite, emissiveSprite);
				emitter.emit();
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(QuadEmitter emitter, BlockState state, Predicate<@Nullable Direction> cullTest) {
			this.emitter = emitter;
			this.state = state;
			this.cullTest = cullTest;

			active = true;
			calculateDefaultLayer = true;
			isDefaultLayerSolid = false;
		}

		public void reset() {
			emitter = null;
			state = null;
			cullTest = null;

			active = false;
		}
	}
}

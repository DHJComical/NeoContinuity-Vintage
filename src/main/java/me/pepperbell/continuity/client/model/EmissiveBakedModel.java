package me.pepperbell.continuity.client.model;

import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.EmissiveSpriteApi;
import me.pepperbell.continuity.client.config.ContinuityConfig;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.WrapperBakedModel;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class EmissiveBakedModel extends WrapperBakedModel {
	protected static final RenderMaterial[] EMISSIVE_MATERIALS;
	protected static final RenderMaterial DEFAULT_EMISSIVE_MATERIAL;
	protected static final RenderMaterial CUTOUT_MIPPED_EMISSIVE_MATERIAL;

	static {
		BlendMode[] blendModes = BlendMode.values();
		EMISSIVE_MATERIALS = new RenderMaterial[blendModes.length];
		MaterialFinder finder = RenderUtil.getMaterialFinder();
		for (BlendMode blendMode : blendModes) {
			EMISSIVE_MATERIALS[blendMode.ordinal()] = finder.emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE).blendMode(blendMode).find();
		}

		DEFAULT_EMISSIVE_MATERIAL = EMISSIVE_MATERIALS[BlendMode.DEFAULT.ordinal()];
		CUTOUT_MIPPED_EMISSIVE_MATERIAL = EMISSIVE_MATERIALS[BlendMode.CUTOUT_MIPPED.ordinal()];
	}

	public EmissiveBakedModel(BakedModel wrapped) {
		super(wrapped);
	}

	@Override
	public void emitBlockQuads(QuadEmitter emitter, BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, Predicate<@Nullable Direction> cullTest) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
			return;
		}

		EmissiveBlockQuadTransform quadTransform = container.emissiveBlockQuadTransform;
		if (quadTransform.isActive()) {
			super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
			return;
		}

		MutableMesh mutableMesh = container.mutableMesh;
		quadTransform.prepare(mutableMesh.emitter(), state, cullTest);

		emitter.pushTransform(quadTransform);
		super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
		emitter.popTransform();

		mutableMesh.outputTo(emitter);
		mutableMesh.clear();
		quadTransform.reset();
	}

	@Override
	public void emitItemQuads(QuadEmitter emitter, Supplier<Random> randomSupplier) {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			super.emitItemQuads(emitter, randomSupplier);
			return;
		}

		ModelObjectsContainer container = ModelObjectsContainer.get();
		if (!container.featureStates.getEmissiveTexturesState().isEnabled()) {
			super.emitItemQuads(emitter, randomSupplier);
			return;
		}

		EmissiveItemQuadTransform quadTransform = container.emissiveItemQuadTransform;
		if (quadTransform.isActive()) {
			super.emitItemQuads(emitter, randomSupplier);
			return;
		}

		MutableMesh mutableMesh = container.mutableMesh;
		quadTransform.prepare(mutableMesh.emitter());

		emitter.pushTransform(quadTransform);
		super.emitItemQuads(emitter, randomSupplier);
		emitter.popTransform();

		mutableMesh.outputTo(emitter);
		mutableMesh.clear();
		quadTransform.reset();
	}

	@Override
	public boolean isVanillaAdapter() {
		if (!ContinuityConfig.INSTANCE.emissiveTextures.get()) {
			return super.isVanillaAdapter();
		}
		return false;
	}

	protected static class EmissiveBlockQuadTransform implements QuadTransform {
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

				BlendMode blendMode = quad.material().blendMode();
				RenderMaterial emissiveMaterial;
				if (blendMode == BlendMode.DEFAULT) {
					if (calculateDefaultLayer) {
						isDefaultLayerSolid = RenderLayers.getBlockLayer(state) == RenderLayer.getSolid();
						calculateDefaultLayer = false;
					}

					if (isDefaultLayerSolid) {
						emissiveMaterial = CUTOUT_MIPPED_EMISSIVE_MATERIAL;
					} else {
						emissiveMaterial = DEFAULT_EMISSIVE_MATERIAL;
					}
				} else if (blendMode == BlendMode.SOLID) {
					emissiveMaterial = CUTOUT_MIPPED_EMISSIVE_MATERIAL;
				} else {
					emissiveMaterial = EMISSIVE_MATERIALS[blendMode.ordinal()];
				}

				emitter.material(emissiveMaterial);
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

	protected static class EmissiveItemQuadTransform implements QuadTransform {
		protected QuadEmitter emitter;

		protected boolean active;

		@Override
		public boolean transform(MutableQuadView quad) {
			Sprite sprite = RenderUtil.getSpriteFinder().find(quad);
			Sprite emissiveSprite = EmissiveSpriteApi.get().getEmissiveSprite(sprite);
			if (emissiveSprite != null) {
				emitter.copyFrom(quad);
				emitter.material(DEFAULT_EMISSIVE_MATERIAL);
				QuadUtil.interpolate(emitter, sprite, emissiveSprite);
				emitter.emit();
			}
			return true;
		}

		public boolean isActive() {
			return active;
		}

		public void prepare(QuadEmitter emitter) {
			this.emitter = emitter;

			active = true;
		}

		public void reset() {
			emitter = null;

			active = false;
		}
	}
}

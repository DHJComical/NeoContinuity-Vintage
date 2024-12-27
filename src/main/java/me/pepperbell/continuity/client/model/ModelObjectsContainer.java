package me.pepperbell.continuity.client.model;

import me.pepperbell.continuity.impl.client.ContinuityFeatureStatesImpl;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;

public class ModelObjectsContainer {
	public static final ThreadLocal<ModelObjectsContainer> THREAD_LOCAL = ThreadLocal.withInitial(ModelObjectsContainer::new);

	public final CtmBakedModel.CtmQuadTransform ctmQuadTransform = new CtmBakedModel.CtmQuadTransform();
	public final EmissiveBakedModel.EmissiveBlockQuadTransform emissiveBlockQuadTransform = new EmissiveBakedModel.EmissiveBlockQuadTransform();
	public final EmissiveBakedModel.EmissiveItemQuadTransform emissiveItemQuadTransform = new EmissiveBakedModel.EmissiveItemQuadTransform();

	public final ContinuityFeatureStatesImpl featureStates = new ContinuityFeatureStatesImpl();
	public final MutableMesh mutableMesh = Renderer.get().mutableMesh();

	public static ModelObjectsContainer get() {
		return THREAD_LOCAL.get();
	}
}

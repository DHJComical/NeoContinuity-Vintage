package me.pepperbell.continuity.client.ctm;

import java.util.Properties;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.CachingPredicates;
import me.pepperbell.continuity.api.client.CtmLoader;
import me.pepperbell.continuity.api.client.CtmProperties;
import me.pepperbell.continuity.api.client.QuadProcessor;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.ResourceLocation;

/**
 * Adapts a single parsed CTM Mod format definition ({@link CtmDefinition}) to the Continuity
 * loader pipeline. The three factories are used by {@code CtmPropertiesLoader} to build the
 * processor + predicates at stitch time.
 */
public class CtmModLoader implements CtmLoader<CtmDefinition> {
	protected final CtmDefinition properties;

	public CtmModLoader(CtmDefinition properties) {
		this.properties = properties;
	}

	@Override
	public CtmProperties.Factory<CtmDefinition> getPropertiesFactory() {
		return new CtmProperties.Factory<>() {
			@Override
			@Nullable
			public CtmDefinition createProperties(Properties properties, ResourceLocation resourceId, IResourcePack pack, int packPriority, IResourceManager resourceManager, String method) {
				return CtmModLoader.this.properties;
			}
		};
	}

	@Override
	public QuadProcessor.Factory<CtmDefinition> getProcessorFactory() {
		return new CtmQuadProcessor.Factory();
	}

	@Override
	public CachingPredicates.Factory<CtmDefinition> getPredicatesFactory() {
		return new CtmCachingPredicates.Factory<>();
	}
}

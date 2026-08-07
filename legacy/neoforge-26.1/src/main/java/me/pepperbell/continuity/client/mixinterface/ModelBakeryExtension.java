package me.pepperbell.continuity.client.mixinterface;

import net.minecraft.server.packs.resources.PreparableReloadListener;

public interface ModelBakeryExtension {

	PreparableReloadListener.SharedState continuity$getSharedState();
	void continuity$setSharedState(PreparableReloadListener.SharedState sharedState);
}

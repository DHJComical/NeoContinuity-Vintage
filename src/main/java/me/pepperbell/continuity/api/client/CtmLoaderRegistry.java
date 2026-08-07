package me.pepperbell.continuity.api.client;

import javax.annotation.Nullable;

import me.pepperbell.continuity.impl.client.CtmLoaderRegistryImpl;

public interface CtmLoaderRegistry {
	static CtmLoaderRegistry get() {
		return CtmLoaderRegistryImpl.INSTANCE;
	}

	void registerLoader(String method, CtmLoader<?> loader);

	@Nullable
	CtmLoader<?> getLoader(String method);
}

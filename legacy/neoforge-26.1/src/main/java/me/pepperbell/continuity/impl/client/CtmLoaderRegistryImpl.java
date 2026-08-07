package me.pepperbell.continuity.impl.client;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.pepperbell.continuity.api.client.CtmLoader;
import me.pepperbell.continuity.api.client.CtmLoaderRegistry;

public final class CtmLoaderRegistryImpl implements CtmLoaderRegistry {
	public static final CtmLoaderRegistryImpl INSTANCE = new CtmLoaderRegistryImpl();

	private final Map<String, CtmLoader<?>> loaderMap = new Object2ObjectOpenHashMap<>();

	@Override
	public void registerLoader(String method, CtmLoader<?> loader) {
		loaderMap.put(method, loader);
	}

	@Override
	@Nullable
	public CtmLoader<?> getLoader(String method) {
		return loaderMap.get(method);
	}
}

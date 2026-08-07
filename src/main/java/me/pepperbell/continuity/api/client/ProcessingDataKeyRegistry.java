package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import me.pepperbell.continuity.impl.client.ProcessingDataKeyRegistryImpl;
import net.minecraft.util.ResourceLocation;

public interface ProcessingDataKeyRegistry {
	static ProcessingDataKeyRegistry get() {
		return ProcessingDataKeyRegistryImpl.INSTANCE;
	}

	default <T> ProcessingDataKey<T> registerKey(ResourceLocation id, Supplier<T> valueSupplier) {
		return registerKey(id, valueSupplier, null);
	}

	<T> ProcessingDataKey<T> registerKey(ResourceLocation id, Supplier<T> valueSupplier, Consumer<T> valueResetAction);

	@Nullable
	ProcessingDataKey<?> getKey(ResourceLocation id);

	int getRegisteredAmount();
}

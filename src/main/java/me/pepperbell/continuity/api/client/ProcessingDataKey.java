package me.pepperbell.continuity.api.client;

import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.util.ResourceLocation;

public interface ProcessingDataKey<T> {
	ResourceLocation getId();

	int getRawId();

	Supplier<T> getValueSupplier();

	@Nullable
	Consumer<T> getValueResetAction();
}

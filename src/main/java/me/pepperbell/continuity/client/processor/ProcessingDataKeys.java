package me.pepperbell.continuity.client.processor;

import java.util.function.Supplier;

import me.pepperbell.continuity.api.client.ProcessingDataKey;
import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.util.math.BlockPos;

public final class ProcessingDataKeys {
	public static final ProcessingDataKey<BlockPos.MutableBlockPos> MUTABLE_POS = create("mutable_pos", BlockPos.MutableBlockPos::new);

	private static <T> ProcessingDataKey<T> create(String id, Supplier<T> valueSupplier) {
		return ProcessingDataKeyRegistry.get().registerKey(ContinuityClient.asId(id), valueSupplier);
	}

	public static void init() {
	}

	private ProcessingDataKeys() {
	}
}

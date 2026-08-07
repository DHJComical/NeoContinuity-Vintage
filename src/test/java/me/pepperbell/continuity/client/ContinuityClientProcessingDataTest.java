package me.pepperbell.continuity.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.pepperbell.continuity.api.client.ProcessingDataKeyRegistry;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.impl.client.ProcessingContextImpl;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

class ContinuityClientProcessingDataTest {
	@Test
	void registerLoadersInitializesProcessingDataBeforeContextUse() {
		ContinuityClient.registerLoaders();

		assertTrue(ProcessingDataKeyRegistry.get().getRegisteredAmount() > 0);
		ProcessingContextImpl context = new ProcessingContextImpl();
		assertInstanceOf(BlockPos.MutableBlockPos.class, context.getData(ProcessingDataKeys.MUTABLE_POS));
	}
}

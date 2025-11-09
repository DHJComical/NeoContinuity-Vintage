package me.pepperbell.continuity.client.processor;

import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.simple.SimpleQuadProcessor;
import me.pepperbell.continuity.client.properties.ConnectingCtmProperties;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class TopQuadProcessor extends AbstractQuadProcessor {
	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;

	public TopQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams) {
		super(sprites, processingPredicate);
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockPos pos, BlockState appearanceState, BlockState state, Random random, int pass, ProcessingContext context) {
		Direction lightFace = quad.lightFace();
		Direction.Axis axis;
		if (appearanceState.contains(Properties.AXIS)) {
			axis = appearanceState.get(Properties.AXIS);
		} else {
			axis = Direction.Axis.Y;
		}
		if (lightFace.getAxis() != axis) {
			Direction up = Direction.from(axis, Direction.AxisDirection.POSITIVE);
			BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS).set(pos, up);
			if (connectionPredicate.shouldConnect(blockView, pos, appearanceState, state, mutablePos, lightFace, sprite, innerSeams)) {
				return SimpleQuadProcessor.process(quad, sprite, sprites[0]);
			}
		}
		return ProcessingResult.NEXT_PROCESSOR;
	}

	public static class Factory extends AbstractQuadProcessorFactory<ConnectingCtmProperties> {
		@Override
		public QuadProcessor createProcessor(ConnectingCtmProperties properties, Sprite[] sprites) {
			return new TopQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams());
		}

		@Override
		public int getTextureAmount(ConnectingCtmProperties properties) {
			return 1;
		}
	}
}

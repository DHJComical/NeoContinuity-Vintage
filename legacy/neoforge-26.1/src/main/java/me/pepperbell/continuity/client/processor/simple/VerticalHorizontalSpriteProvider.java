package me.pepperbell.continuity.client.processor.simple;

import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class VerticalHorizontalSpriteProvider extends VerticalSpriteProvider {
	// Indices for this array are formed from these bit values:
	// 32     16
	// 1   *   8
	// 2       4
	protected static final int[] SECONDARY_SPRITE_INDEX_MAP = new int[] {
			3, 6, 3, 3, 3, 6, 3, 3, 4, 5, 4, 4, 3, 6, 3, 3,
			3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3, 3, 6, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 3, 3, 3, 3,
			3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
	};

	public VerticalHorizontalSpriteProvider(TextureAtlasSprite[] sprites, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode) {
		super(sprites, connectionPredicate, innerSeams, orientationMode);
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(/* QuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, ProcessingDataProvider dataProvider) {
		Direction[] directions = DirectionMaps.getDirections(orientationMode, quad, appearanceState);
		BlockPos.MutableBlockPos mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS);
		int connections = getConnections(directions, mutablePos, level, pos, appearanceState, state, /* quad.lightFace() */ quad.direction(), sprite);
		if (connections != 0) {
			return sprites[SPRITE_INDEX_MAP[connections]];
		} else {
			int secondaryConnections = getSecondaryConnections(directions, mutablePos, level, pos, appearanceState, state, /* quad.lightFace() */ quad.direction(), sprite);
			return sprites[SECONDARY_SPRITE_INDEX_MAP[secondaryConnections]];
		}
	}

	protected int getSecondaryConnections(Direction[] directions, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction face, TextureAtlasSprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			Direction direction = directions[i * 2];
			mutablePos.setWithOffset(pos, direction);
			if (connectionPredicate.shouldConnect(level, pos, state, appearanceState, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << (i * 3);
				for (int j = 0; j < 2; j++) {
					mutablePos.setWithOffset(pos, direction).move(directions[((i + j) % 2) * 2 + 1]);
					if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, mutablePos, face, quadSprite, innerSeams)) {
						connections |= 1 << ((i * 3 + j * 2 + 5) % 6);
					}
				}
			}
		}
		return connections;
	}

	public static class Factory implements SpriteProvider.Factory<OrientedConnectingCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, OrientedConnectingCtmProperties properties) {
			return new VerticalHorizontalSpriteProvider(sprites, properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode());
		}

		@Override
		public int getSpriteAmount(OrientedConnectingCtmProperties properties) {
			return 7;
		}
	}
}

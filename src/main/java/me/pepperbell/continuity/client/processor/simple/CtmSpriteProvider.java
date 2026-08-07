package me.pepperbell.continuity.client.processor.simple;

import javax.annotation.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.OrientationMode;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.properties.OrientedConnectingCtmProperties;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class CtmSpriteProvider implements SpriteProvider {
	public static final int[] SPRITE_INDEX_MAP = new int[] {
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
			16, 18, 16, 18, 6, 46, 6, 21, 16, 18, 16, 18, 28, 9, 28, 22,
			36, 17, 36, 17, 24, 19, 24, 43, 36, 17, 36, 17, 24, 19, 24, 43,
			37, 40, 37, 40, 30, 8, 30, 34, 37, 40, 37, 40, 25, 23, 25, 45,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			0, 3, 0, 3, 12, 5, 12, 15, 0, 3, 0, 3, 12, 5, 12, 15,
			1, 2, 1, 2, 4, 7, 4, 29, 1, 2, 1, 2, 13, 31, 13, 14,
			36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
			16, 42, 16, 42, 6, 20, 6, 10, 16, 42, 16, 42, 28, 35, 28, 44,
			36, 39, 36, 39, 24, 41, 24, 27, 36, 39, 36, 39, 24, 41, 24, 27,
			37, 38, 37, 38, 30, 11, 30, 32, 37, 38, 37, 38, 25, 33, 25, 26,
	};

	protected TextureAtlasSprite[] sprites;
	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected OrientationMode orientationMode;

	public CtmSpriteProvider(TextureAtlasSprite[] sprites, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode) {
		this.sprites = sprites;
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
		this.orientationMode = orientationMode;
	}

	@Override
	@Nullable
	public TextureAtlasSprite getSprite(BakedQuad quad, TextureAtlasSprite sprite, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, long rand, ProcessingDataProvider dataProvider) {
		EnumFacing[] directions = DirectionMaps.getDirections(orientationMode, quad, appearanceState);
		BlockPos.MutableBlockPos mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS);
		int connections = getConnections(directions, connectionPredicate, innerSeams, mutablePos, level, pos, appearanceState, state, quad.getFace(), sprite);
		return sprites[SPRITE_INDEX_MAP[connections]];
	}

	public static int getConnections(EnumFacing[] directions, ConnectionPredicate connectionPredicate, boolean innerSeams, BlockPos.MutableBlockPos mutablePos, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, TextureAtlasSprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 4; i++) {
			mutablePos.setPos(pos).move(directions[i]);
			if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << (i * 2);
			}
		}
		for (int i = 0; i < 4; i++) {
			int index1 = i;
			int index2 = (i + 1) % 4;
			if (((connections >>> index1 * 2) & 1) == 1 && ((connections >>> index2 * 2) & 1) == 1) {
				mutablePos.setPos(pos).move(directions[index1]).move(directions[index2]);
				if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, mutablePos, face, quadSprite, innerSeams)) {
					connections |= 1 << (i * 2 + 1);
				}
			}
		}
		return connections;
	}

	public static class Factory implements SpriteProvider.Factory<OrientedConnectingCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, OrientedConnectingCtmProperties properties) {
			return new CtmSpriteProvider(sprites, properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode());
		}

		@Override
		public int getSpriteAmount(OrientedConnectingCtmProperties properties) {
			return 47;
		}
	}
}

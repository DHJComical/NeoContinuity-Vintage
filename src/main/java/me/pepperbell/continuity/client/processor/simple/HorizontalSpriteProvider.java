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

public class HorizontalSpriteProvider implements SpriteProvider {
	protected static final int[] SPRITE_INDEX_MAP = new int[] {
			3, 2, 0, 1,
	};

	protected TextureAtlasSprite[] sprites;
	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected OrientationMode orientationMode;

	public HorizontalSpriteProvider(TextureAtlasSprite[] sprites, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode) {
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
		int connections = getConnections(directions, mutablePos, level, pos, appearanceState, state, quad.getFace(), sprite);
		return sprites[SPRITE_INDEX_MAP[connections]];
	}

	protected int getConnections(EnumFacing[] directions, BlockPos.MutableBlockPos mutablePos, IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, EnumFacing face, TextureAtlasSprite quadSprite) {
		int connections = 0;
		for (int i = 0; i < 2; i++) {
			mutablePos.setPos(pos).move(directions[i * 2]);
			if (connectionPredicate.shouldConnect(level, pos, appearanceState, state, mutablePos, face, quadSprite, innerSeams)) {
				connections |= 1 << i;
			}
		}
		return connections;
	}

	public static class Factory implements SpriteProvider.Factory<OrientedConnectingCtmProperties> {
		@Override
		public SpriteProvider createSpriteProvider(TextureAtlasSprite[] sprites, OrientedConnectingCtmProperties properties) {
			return new HorizontalSpriteProvider(sprites, properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode());
		}

		@Override
		public int getSpriteAmount(OrientedConnectingCtmProperties properties) {
			return 4;
		}
	}
}

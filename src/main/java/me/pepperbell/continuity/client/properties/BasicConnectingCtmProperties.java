package me.pepperbell.continuity.client.properties;

import java.util.Locale;
import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.util.SpriteCalculator;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourcePack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

public class BasicConnectingCtmProperties extends BaseCtmProperties {
	protected ConnectionPredicate connectionPredicate;

	public BasicConnectingCtmProperties(Properties properties, Identifier resourceId, ResourcePack pack, int packPriority, ResourceManager resourceManager, String method) {
		super(properties, resourceId, pack, packPriority, resourceManager, method);
	}

	@Override
	public void init() {
		super.init();
		parseConnect();
		detectConnect();
		validateConnect();
	}

	protected void parseConnect() {
		String connectStr = properties.getProperty("connect");
		if (connectStr == null) {
			return;
		}

		try {
			connectionPredicate = ConnectionType.valueOf(connectStr.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			ContinuityClient.LOGGER.warn("Unknown 'connect' value '" + connectStr + "' in file '" + resourceId + "' in pack '" + packId + "'");
		}
	}

	protected void detectConnect() {
		if (connectionPredicate == null) {
			if (matchBlocksPredicate != null) {
				connectionPredicate = ConnectionType.BLOCK;
			} else if (matchTilesSet != null) {
				connectionPredicate = ConnectionType.TILE;
			}
		}
	}

	protected void validateConnect() {
		if (connectionPredicate == null) {
			ContinuityClient.LOGGER.error("No valid connection type provided in file '" + resourceId + "' in pack '" + packId + "'");
			valid = false;
		}
	}

	public ConnectionPredicate getConnectionPredicate() {
		return connectionPredicate;
	}

	public enum ConnectionType implements ConnectionPredicate {
		BLOCK {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, Sprite quadSprite) {
				return appearanceState.getBlock() == otherAppearanceState.getBlock();
			}
		},
		TILE {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, Sprite quadSprite) {
				if (appearanceState == otherAppearanceState) {
					return true;
				}
				return SpriteCalculator.getSprites(otherAppearanceState, face).contains(quadSprite);
			}
		},
		STATE {
			@Override
			public boolean shouldConnect(BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, Direction face, Sprite quadSprite) {
				return appearanceState == otherAppearanceState;
			}
		};
	}
}

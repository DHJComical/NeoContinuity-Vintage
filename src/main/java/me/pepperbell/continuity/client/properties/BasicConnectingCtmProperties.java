package me.pepperbell.continuity.client.properties;

import java.util.Locale;
import java.util.Properties;

import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BasicConnectingCtmProperties extends BaseCtmProperties {
	protected ConnectionPredicate connectionPredicate;

	public BasicConnectingCtmProperties(Properties properties, ResourceLocation resourceId, IResourcePack pack, int packPriority, IResourceManager resourceManager, String method) {
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
			public boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite) {
				return appearanceState.getBlock() == otherAppearanceState.getBlock();
			}
		},
		TILE {
			@Override
			public boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite) {
				if (appearanceState == otherAppearanceState) {
					return true;
				}
				return stateUsesSprite(otherAppearanceState, face, quadSprite);
			}
		},
		STATE {
			@Override
			public boolean shouldConnect(IBlockAccess level, BlockPos pos, IBlockState appearanceState, IBlockState state, BlockPos otherPos, IBlockState otherAppearanceState, IBlockState otherState, EnumFacing face, TextureAtlasSprite quadSprite) {
				return appearanceState == otherAppearanceState;
			}
		};

		private static boolean stateUsesSprite(IBlockState state, EnumFacing face, TextureAtlasSprite sprite) {
			IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelShapes().getModelForState(state);
			if (model == null) {
				return false;
			}
			if (face != null && containsSprite(model.getQuads(state, face, 0), sprite)) {
				return true;
			}
			return containsSprite(model.getQuads(state, null, 0), sprite);
		}

		private static boolean containsSprite(java.util.List<BakedQuad> quads, TextureAtlasSprite sprite) {
			for (BakedQuad quad : quads) {
				if (quad.getSprite() == sprite) {
					return true;
				}
			}
			return false;
		}
	}
}

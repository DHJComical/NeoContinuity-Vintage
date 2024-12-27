package me.pepperbell.continuity.client.processor.overlay;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import me.pepperbell.continuity.api.client.ProcessingDataProvider;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessor;
import me.pepperbell.continuity.client.processor.AbstractQuadProcessorFactory;
import me.pepperbell.continuity.client.processor.ConnectionPredicate;
import me.pepperbell.continuity.client.processor.DirectionMaps;
import me.pepperbell.continuity.client.processor.ProcessingDataKeys;
import me.pepperbell.continuity.client.processor.ProcessingPredicate;
import me.pepperbell.continuity.client.properties.overlay.OverlayPropertiesSection;
import me.pepperbell.continuity.client.properties.overlay.StandardOverlayCtmProperties;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
import me.pepperbell.continuity.client.util.SpriteCalculator;
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class StandardOverlayQuadProcessor extends AbstractQuadProcessor {
	@Nullable
	protected Set<Identifier> matchTilesSet;
	@Nullable
	protected Predicate<BlockState> matchBlocksPredicate;
	@Nullable
	protected Set<Identifier> connectTilesSet;
	@Nullable
	protected Predicate<BlockState> connectBlocksPredicate;
	protected ConnectionPredicate connectionPredicate;

	protected int tintIndex;
	@Nullable
	protected BlockState tintBlock;
	protected RenderMaterial material;

	public StandardOverlayQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, @Nullable Set<Identifier> matchTilesSet, @Nullable Predicate<BlockState> matchBlocksPredicate, @Nullable Set<Identifier> connectTilesSet, @Nullable Predicate<BlockState> connectBlocksPredicate, ConnectionPredicate connectionPredicate, int tintIndex, @Nullable BlockState tintBlock, BlendMode layer) {
		super(sprites, processingPredicate);
		this.matchTilesSet = matchTilesSet;
		this.matchBlocksPredicate = matchBlocksPredicate;
		this.connectTilesSet = connectTilesSet;
		this.connectBlocksPredicate = connectBlocksPredicate;
		this.connectionPredicate = connectionPredicate;

		this.tintIndex = tintIndex;
		this.tintBlock = tintBlock;
		material = RenderUtil.findOverlayMaterial(layer, this.tintBlock);

		// Turn all missing sprites into null, since it is more efficient to check for a null sprite than a missing
		// sprite. There is no functional difference between missing and null sprites for this processor.
		for (int i = 0; i < sprites.length; i++) {
			Sprite sprite = sprites[i];
			if (TextureUtil.isMissingSprite(sprite)) {
				sprites[i] = null;
			}
		}
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, ProcessingContext context) {
		Direction lightFace = quad.lightFace();
		SpriteCollector collector = getSprites(blockView, appearanceState, state, pos, lightFace, sprite, DirectionMaps.getMap(lightFace)[0], context);
		if (collector != null) {
			QuadEmitter emitter = context.getExtraQuadEmitter();
			int tintColor = RenderUtil.getTintColor(tintBlock, blockView, pos, tintIndex);
			for (int i = 0; i < collector.spriteAmount; i++) {
				QuadUtil.emitOverlayQuad(emitter, lightFace, collector.sprites[i], tintColor, material);
			}
			collector.clear();
		}
		return ProcessingResult.NEXT_PROCESSOR;
	}

	protected boolean appliesOverlay(BlockState otherAppearanceState, BlockState otherState, BlockPos otherPos, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction face, Sprite quadSprite) {
		// OptiFine never applies overlays from blocks with dynamic bounds. To improve mod compatibility, call
		// isFullCube with the correct values and do not check for dynamic bounds explicitly. For vanilla blocks, this
		// change only makes it so retracted pistons and shulker boxes can apply overlays.
		if (!otherState.isFullCube(blockView, otherPos)) {
			return false;
		}
		if (connectBlocksPredicate != null) {
			if (!connectBlocksPredicate.test(otherAppearanceState)) {
				return false;
			}
		}
		if (connectTilesSet != null) {
			if (!connectTilesSet.contains(SpriteCalculator.getSprite(otherAppearanceState, face).getContents().getId())) {
				return false;
			}
		}
		return !connectionPredicate.shouldConnect(blockView, appearanceState, state, pos, otherAppearanceState, otherState, otherPos, face, quadSprite);
	}

	protected boolean hasSameOverlay(@Nullable BlockState otherAppearanceState, Direction face) {
		if (otherAppearanceState == null) {
			return false;
		}
		if (matchBlocksPredicate != null) {
			if (!matchBlocksPredicate.test(otherAppearanceState)) {
				return false;
			}
		}
		if (matchTilesSet != null) {
			if (!matchTilesSet.contains(SpriteCalculator.getSprite(otherAppearanceState, face).getContents().getId())) {
				return false;
			}
		}
		return true;
	}

	protected boolean appliesOverlayCorner(Direction dir0, Direction dir1, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction lightFace, Sprite quadSprite) {
		mutablePos.set(pos, dir0).move(dir1);
		BlockState otherState = blockView.getBlockState(mutablePos);
		BlockState otherAppearanceState = otherState.getAppearance(blockView, mutablePos, lightFace, state, pos);
		if (appliesOverlay(otherAppearanceState, otherState, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
			mutablePos.move(lightFace);
			return !blockView.getBlockState(mutablePos).isOpaqueFullCube();
		}
		return false;
	}

	protected SpriteCollector fromTwoSidesAdj(SpriteCollector collector, @Nullable BlockState appearanceState0, @Nullable BlockState appearanceState1, Direction dir0, Direction dir1, int sprite, int spriteC01, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction lightFace, Sprite quadSprite) {
		collector.add(sprites[sprite]);
		// OptiFine does not check whether the other two adjacent blocks have the same overlay before trying to apply
		// the corner overlay. I consider this a bug since it is inconsistent with other cases, so it is fixed here by
		// checking those blocks.
		if ((hasSameOverlay(appearanceState0, lightFace)
				|| hasSameOverlay(appearanceState1, lightFace))
				&& appliesOverlayCorner(dir0, dir1, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
			collector.add(sprites[spriteC01]);
		}
		return collector;
	}

	protected SpriteCollector fromOneSide(SpriteCollector collector, @Nullable BlockState appearanceState0, @Nullable BlockState appearanceState1, @Nullable BlockState appearanceState2, Direction dir0, Direction dir1, Direction dir2, int sprite, int spriteC01, int spriteC12, BlockPos.Mutable mutablePos, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction lightFace, Sprite quadSprite) {
		boolean c01;
		boolean c12;
		if (hasSameOverlay(appearanceState1, lightFace)) {
			c01 = true;
			c12 = true;
		} else {
			c01 = hasSameOverlay(appearanceState0, lightFace);
			c12 = hasSameOverlay(appearanceState2, lightFace);
		}

		collector.add(sprites[sprite]);
		if (c01 && appliesOverlayCorner(dir0, dir1, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
			collector.add(sprites[spriteC01]);
		}
		if (c12 && appliesOverlayCorner(dir1, dir2, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
			collector.add(sprites[spriteC12]);
		}
		return collector;
	}

	protected static SpriteCollector getCollector(ProcessingDataProvider dataProvider) {
		return dataProvider.getData(ProcessingDataKeys.SPRITE_COLLECTOR);
	}

	protected SpriteCollector prepareCollector(SpriteCollector collector, int sprite0) {
		collector.add(sprites[sprite0]);
		return collector;
	}

	protected SpriteCollector prepareCollector(SpriteCollector collector, int sprite0, int sprite1) {
		collector.add(sprites[sprite0]);
		collector.add(sprites[sprite1]);
		return collector;
	}

	/*
	0:	CORNER D+R
	1:	D
	2:	CORNER L+D
	3:	D R
	4:	L D
	5:	L D R
	6:	L D U
	7:	R
	8:	L D R U
	9:	L
	10:	R U
	11:	L U
	12:	D R U
	13:	L R U
	14:	CORNER R+U
	15:	U
	16:	CORNER L+U
	 */
	@Nullable
	protected SpriteCollector getSprites(BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Direction lightFace, Sprite quadSprite, Direction[] directions, ProcessingDataProvider dataProvider) {
		BlockPos.Mutable mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS);

		// [up] | [right] | [down] | [left]
		//     8
		// 1   *   4
		//     2
		int applications = 0;

		mutablePos.set(pos, directions[0]).move(lightFace);
		BlockState appearanceState0;
		if (!blockView.getBlockState(mutablePos).isOpaqueFullCube()) {
			mutablePos.set(pos, directions[0]);
			BlockState state0 = blockView.getBlockState(mutablePos);
			appearanceState0 = state0.getAppearance(blockView, mutablePos, lightFace, state, pos);
			if (appliesOverlay(appearanceState0, state0, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
				applications |= 0b0001;
			}
		} else {
			appearanceState0 = null;
		}

		mutablePos.set(pos, directions[1]).move(lightFace);
		BlockState appearanceState1;
		if (!blockView.getBlockState(mutablePos).isOpaqueFullCube()) {
			mutablePos.set(pos, directions[1]);
			BlockState state1 = blockView.getBlockState(mutablePos);
			appearanceState1 = state1.getAppearance(blockView, mutablePos, lightFace, state, pos);
			if (appliesOverlay(appearanceState1, state1, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
				applications |= 0b0010;
			}
		} else {
			appearanceState1 = null;
		}

		mutablePos.set(pos, directions[2]).move(lightFace);
		BlockState appearanceState2;
		if (!blockView.getBlockState(mutablePos).isOpaqueFullCube()) {
			mutablePos.set(pos, directions[2]);
			BlockState state2 = blockView.getBlockState(mutablePos);
			appearanceState2 = state2.getAppearance(blockView, mutablePos, lightFace, state, pos);
			if (appliesOverlay(appearanceState2, state2, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
				applications |= 0b0100;
			}
		} else {
			appearanceState2 = null;
		}

		mutablePos.set(pos, directions[3]).move(lightFace);
		BlockState appearanceState3;
		if (!blockView.getBlockState(mutablePos).isOpaqueFullCube()) {
			mutablePos.set(pos, directions[3]);
			BlockState state3 = blockView.getBlockState(mutablePos);
			appearanceState3 = state3.getAppearance(blockView, mutablePos, lightFace, state, pos);
			if (appliesOverlay(appearanceState3, state3, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite)) {
				applications |= 0b1000;
			}
		} else {
			appearanceState3 = null;
		}

		return switch (applications) {
			case 0b1111 -> prepareCollector(getCollector(dataProvider), 8);
			case 0b0111 -> prepareCollector(getCollector(dataProvider), 5);
			case 0b1011 -> prepareCollector(getCollector(dataProvider), 6);
			case 0b1101 -> prepareCollector(getCollector(dataProvider), 13);
			case 0b1110 -> prepareCollector(getCollector(dataProvider), 12);
			//
			case 0b0101 -> prepareCollector(getCollector(dataProvider), 9, 7);
			case 0b1010 -> prepareCollector(getCollector(dataProvider), 1, 15);
			//
			case 0b0011 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState2, appearanceState3, directions[2], directions[3], 4, 14, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b0110 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState3, appearanceState0, directions[3], directions[0], 3, 16, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b1100 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState0, appearanceState1, directions[0], directions[1], 10, 2, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b1001 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState1, appearanceState2, directions[1], directions[2], 11, 0, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			//
			case 0b0001 -> fromOneSide(getCollector(dataProvider), appearanceState1, appearanceState2, appearanceState3, directions[1], directions[2], directions[3], 9, 0, 14, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b0010 -> fromOneSide(getCollector(dataProvider), appearanceState2, appearanceState3, appearanceState0, directions[2], directions[3], directions[0], 1, 14, 16, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b0100 -> fromOneSide(getCollector(dataProvider), appearanceState3, appearanceState0, appearanceState1, directions[3], directions[0], directions[1], 7, 16, 2, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			case 0b1000 -> fromOneSide(getCollector(dataProvider), appearanceState0, appearanceState1, appearanceState2, directions[0], directions[1], directions[2], 15, 2, 0, mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
			//
			case 0b0000 -> {
				boolean s0 = hasSameOverlay(appearanceState0, lightFace);
				boolean s1 = hasSameOverlay(appearanceState1, lightFace);
				boolean s2 = hasSameOverlay(appearanceState2, lightFace);
				boolean s3 = hasSameOverlay(appearanceState3, lightFace);

				boolean c01 = (s0 | s1) && appliesOverlayCorner(directions[0], directions[1], mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
				boolean c12 = (s1 | s2) && appliesOverlayCorner(directions[1], directions[2], mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
				boolean c23 = (s2 | s3) && appliesOverlayCorner(directions[2], directions[3], mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);
				boolean c30 = (s3 | s0) && appliesOverlayCorner(directions[3], directions[0], mutablePos, blockView, appearanceState, state, pos, lightFace, quadSprite);

				if (c01 | c12 | c23 | c30) {
					SpriteCollector collector = getCollector(dataProvider);
					if (c01) {
						collector.add(sprites[2]);
					}
					if (c12) {
						collector.add(sprites[0]);
					}
					if (c23) {
						collector.add(sprites[14]);
					}
					if (c30) {
						collector.add(sprites[16]);
					}
					yield collector;
				}

				yield null;
			}
			//
			default -> throw new IllegalStateException("Unexpected value: " + applications);
		};
	}

	public static class SpriteCollector {
		protected static final Sprite[] EMPTY_SPRITES = new Sprite[4];

		protected Sprite[] sprites = new Sprite[4];
		protected int spriteAmount;

		public void add(@Nullable Sprite sprite) {
			if (sprite != null) {
				sprites[spriteAmount++] = sprite;
			}
		}

		public void clear() {
			System.arraycopy(EMPTY_SPRITES, 0, sprites, 0, EMPTY_SPRITES.length);
			spriteAmount = 0;
		}
	}

	public static class Factory extends AbstractQuadProcessorFactory<StandardOverlayCtmProperties> {
		@Override
		public QuadProcessor createProcessor(StandardOverlayCtmProperties properties, Sprite[] sprites) {
			OverlayPropertiesSection overlaySection = properties.getOverlayPropertiesSection();
			return new StandardOverlayQuadProcessor(sprites, OverlayProcessingPredicate.fromProperties(properties), properties.getMatchTilesSet(), properties.getMatchBlocksPredicate(), properties.getConnectTilesSet(), properties.getConnectBlocksPredicate(), properties.getConnectionPredicate(), overlaySection.getTintIndex(), overlaySection.getTintBlock(), overlaySection.getLayer());
		}

		@Override
		public int getTextureAmount(StandardOverlayCtmProperties properties) {
			return 17;
		}

		@Override
		public boolean supportsNullSprites(StandardOverlayCtmProperties properties) {
			return false;
		}
	}
}

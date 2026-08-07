package me.pepperbell.continuity.client.processor.overlay;

import java.util.Set;
import java.util.function.Predicate;

import me.pepperbell.continuity.client.model.QuadCollectionBuilder;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
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
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
// import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

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
	protected ChunkSectionLayer layer;
	protected TriState ao;

	public StandardOverlayQuadProcessor(TextureAtlasSprite[] sprites, ProcessingPredicate processingPredicate, @Nullable Set<Identifier> matchTilesSet, @Nullable Predicate<BlockState> matchBlocksPredicate, @Nullable Set<Identifier> connectTilesSet, @Nullable Predicate<BlockState> connectBlocksPredicate, ConnectionPredicate connectionPredicate, int tintIndex, @Nullable BlockState tintBlock, ChunkSectionLayer layer) {
		super(sprites, processingPredicate);
		this.matchTilesSet = matchTilesSet;
		this.matchBlocksPredicate = matchBlocksPredicate;
		this.connectTilesSet = connectTilesSet;
		this.connectBlocksPredicate = connectBlocksPredicate;
		this.connectionPredicate = connectionPredicate;

		this.tintIndex = tintIndex;
		this.tintBlock = tintBlock;
		this.layer = layer;
		this.ao = RenderUtil.aoFromTintBlock(tintBlock);

		// Turn all missing sprites into null, since it is more efficient to check for a null sprite than a missing
		// sprite. There is no functional difference between missing and null sprites for this processor.
		for (int i = 0; i < sprites.length; i++) {
			TextureAtlasSprite sprite = sprites[i];
			if (RenderUtil.isMissingSprite(sprite)) {
				sprites[i] = null;
			}
		}
	}

	@Override
	public ProcessingResult processQuadInner(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context) {
		Direction lightFace = /* quad.lightFace() */ quad.direction();
		SpriteCollector collector = getSprites(level, pos, appearanceState, state, lightFace, sprite, DirectionMaps.getMap(lightFace)[0], context);
		if (collector != null) {
			/* QuadEmitter */ QuadCollectionBuilder emitter = context.getExtraQuadEmitter();
			int tintColor = RenderUtil.getTintColor(tintBlock, level, pos, tintIndex);
			for (int i = 0; i < collector.spriteAmount; i++) {
				QuadUtil.emitOverlayQuad(emitter, lightFace, collector.sprites[i], tintColor, layer, ao);
			}
			collector.clear();
		}
		return ProcessingResult.NEXT_PROCESSOR;
	}

	protected static boolean matchesAny(Set<Identifier> tiles, Set<TextureAtlasSprite> sprites) {
		for (TextureAtlasSprite sprite : sprites) {
			if (tiles.contains(sprite.contents().name())) {
				return true;
			}
		}
		return false;
	}

	protected boolean appliesOverlay(BlockPos otherPos, BlockState otherAppearanceState, BlockState otherState, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction face, TextureAtlasSprite quadSprite) {
		// OptiFine never applies overlays from blocks with dynamic bounds. To improve mod compatibility, call
		// isFullCube with the correct values and do not check for dynamic bounds explicitly. For vanilla blocks, this
		// change only makes it so retracted pistons and shulker boxes can apply overlays.
		if (!otherState.isCollisionShapeFullBlock(level, otherPos)) {
			return false;
		}
		if (connectBlocksPredicate != null) {
			if (!connectBlocksPredicate.test(otherAppearanceState)) {
				return false;
			}
		}
		if (connectTilesSet != null) {
			if (!matchesAny(connectTilesSet, SpriteCalculator.getSprites(otherAppearanceState, face))) {
				return false;
			}
		}
		return !connectionPredicate.shouldConnect(level, pos, appearanceState, state, otherPos, otherAppearanceState, otherState, face, quadSprite);
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
			if (!matchesAny(matchTilesSet, SpriteCalculator.getSprites(otherAppearanceState, face))) {
				return false;
			}
		}
		return true;
	}

	protected boolean appliesOverlayCorner(Direction dir0, Direction dir1, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace, TextureAtlasSprite quadSprite) {
		mutablePos.setWithOffset(pos, dir0).move(dir1);
		BlockState otherState = level.getBlockState(mutablePos);
		BlockState otherAppearanceState = otherState.getAppearance(level, mutablePos, lightFace, state, pos);
		if (appliesOverlay(mutablePos, otherAppearanceState, otherState, level, pos, appearanceState, state, lightFace, quadSprite)) {
			mutablePos.move(lightFace);
			return !level.getBlockState(mutablePos).isSolidRender();
		}
		return false;
	}

	protected SpriteCollector fromTwoSidesAdj(SpriteCollector collector, @Nullable BlockState appearanceState0, @Nullable BlockState appearanceState1, Direction dir0, Direction dir1, int sprite, int spriteC01, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace, TextureAtlasSprite quadSprite) {
		collector.add(sprites[sprite]);
		// OptiFine does not check whether the other two adjacent blocks have the same overlay before trying to apply
		// the corner overlay. I consider this a bug since it is inconsistent with other cases, so it is fixed here by
		// checking those blocks.
		if ((hasSameOverlay(appearanceState0, lightFace)
				|| hasSameOverlay(appearanceState1, lightFace))
				&& appliesOverlayCorner(dir0, dir1, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite)) {
			collector.add(sprites[spriteC01]);
		}
		return collector;
	}

	protected SpriteCollector fromOneSide(SpriteCollector collector, @Nullable BlockState appearanceState0, @Nullable BlockState appearanceState1, @Nullable BlockState appearanceState2, Direction dir0, Direction dir1, Direction dir2, int sprite, int spriteC01, int spriteC12, BlockPos.MutableBlockPos mutablePos, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace, TextureAtlasSprite quadSprite) {
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
		if (c01 && appliesOverlayCorner(dir0, dir1, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite)) {
			collector.add(sprites[spriteC01]);
		}
		if (c12 && appliesOverlayCorner(dir1, dir2, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite)) {
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
	protected SpriteCollector getSprites(BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, Direction lightFace, TextureAtlasSprite quadSprite, Direction[] directions, ProcessingDataProvider dataProvider) {
		BlockPos.MutableBlockPos mutablePos = dataProvider.getData(ProcessingDataKeys.MUTABLE_POS);

		// [up] | [right] | [down] | [left]
		//     8
		// 1   *   4
		//     2
		int applications = 0;

		mutablePos.setWithOffset(pos, directions[0]).move(lightFace);
		BlockState appearanceState0;
		if (!level.getBlockState(mutablePos).isSolidRender()) {
			mutablePos.setWithOffset(pos, directions[0]);
			BlockState state0 = level.getBlockState(mutablePos);
			appearanceState0 = state0.getAppearance(level, mutablePos, lightFace, state, pos);
			if (appliesOverlay(mutablePos, appearanceState0, state0, level, pos, appearanceState, state, lightFace, quadSprite)) {
				applications |= 0b0001;
			}
		} else {
			appearanceState0 = null;
		}

		mutablePos.setWithOffset(pos, directions[1]).move(lightFace);
		BlockState appearanceState1;
		if (!level.getBlockState(mutablePos).isSolidRender()) {
			mutablePos.setWithOffset(pos, directions[1]);
			BlockState state1 = level.getBlockState(mutablePos);
			appearanceState1 = state1.getAppearance(level, mutablePos, lightFace, state, pos);
			if (appliesOverlay(mutablePos, appearanceState1, state1, level, pos, appearanceState, state, lightFace, quadSprite)) {
				applications |= 0b0010;
			}
		} else {
			appearanceState1 = null;
		}

		mutablePos.setWithOffset(pos, directions[2]).move(lightFace);
		BlockState appearanceState2;
		if (!level.getBlockState(mutablePos).isSolidRender()) {
			mutablePos.setWithOffset(pos, directions[2]);
			BlockState state2 = level.getBlockState(mutablePos);
			appearanceState2 = state2.getAppearance(level, mutablePos, lightFace, state, pos);
			if (appliesOverlay(mutablePos, appearanceState2, state2, level, pos, appearanceState, state, lightFace, quadSprite)) {
				applications |= 0b0100;
			}
		} else {
			appearanceState2 = null;
		}

		mutablePos.setWithOffset(pos, directions[3]).move(lightFace);
		BlockState appearanceState3;
		if (!level.getBlockState(mutablePos).isSolidRender()) {
			mutablePos.setWithOffset(pos, directions[3]);
			BlockState state3 = level.getBlockState(mutablePos);
			appearanceState3 = state3.getAppearance(level, mutablePos, lightFace, state, pos);
			if (appliesOverlay(mutablePos, appearanceState3, state3, level, pos, appearanceState, state, lightFace, quadSprite)) {
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
			case 0b0011 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState2, appearanceState3, directions[2], directions[3], 4, 14, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b0110 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState3, appearanceState0, directions[3], directions[0], 3, 16, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b1100 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState0, appearanceState1, directions[0], directions[1], 10, 2, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b1001 -> fromTwoSidesAdj(getCollector(dataProvider), appearanceState1, appearanceState2, directions[1], directions[2], 11, 0, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			//
			case 0b0001 -> fromOneSide(getCollector(dataProvider), appearanceState1, appearanceState2, appearanceState3, directions[1], directions[2], directions[3], 9, 0, 14, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b0010 -> fromOneSide(getCollector(dataProvider), appearanceState2, appearanceState3, appearanceState0, directions[2], directions[3], directions[0], 1, 14, 16, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b0100 -> fromOneSide(getCollector(dataProvider), appearanceState3, appearanceState0, appearanceState1, directions[3], directions[0], directions[1], 7, 16, 2, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			case 0b1000 -> fromOneSide(getCollector(dataProvider), appearanceState0, appearanceState1, appearanceState2, directions[0], directions[1], directions[2], 15, 2, 0, mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
			//
			case 0b0000 -> {
				boolean s0 = hasSameOverlay(appearanceState0, lightFace);
				boolean s1 = hasSameOverlay(appearanceState1, lightFace);
				boolean s2 = hasSameOverlay(appearanceState2, lightFace);
				boolean s3 = hasSameOverlay(appearanceState3, lightFace);

				boolean c01 = (s0 | s1) && appliesOverlayCorner(directions[0], directions[1], mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
				boolean c12 = (s1 | s2) && appliesOverlayCorner(directions[1], directions[2], mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
				boolean c23 = (s2 | s3) && appliesOverlayCorner(directions[2], directions[3], mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);
				boolean c30 = (s3 | s0) && appliesOverlayCorner(directions[3], directions[0], mutablePos, level, pos, appearanceState, state, lightFace, quadSprite);

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
		protected static final TextureAtlasSprite[] EMPTY_SPRITES = new TextureAtlasSprite[4];

		protected TextureAtlasSprite[] sprites = new TextureAtlasSprite[4];
		protected int spriteAmount;

		public void add(@Nullable TextureAtlasSprite sprite) {
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
		public QuadProcessor createProcessor(StandardOverlayCtmProperties properties, TextureAtlasSprite[] sprites) {
			OverlayPropertiesSection overlaySection = properties.getOverlayPropertiesSection();
			return new StandardOverlayQuadProcessor(sprites, OverlayProcessingPredicate.fromProperties(properties), properties.getMatchTilesSet(), properties.getMatchBlocksPredicate(), properties.getConnectTilesSet(), properties.getConnectBlocksPredicate(), properties.getConnectionPredicate(), overlaySection.getTintIndex(), overlaySection.getTintBlock(), overlaySection.getLayer());
		}

		@Override
		public int getSpriteAmount(StandardOverlayCtmProperties properties) {
			return 17;
		}

		@Override
		public boolean supportsNullSprites(StandardOverlayCtmProperties properties) {
			return false;
		}
	}
}

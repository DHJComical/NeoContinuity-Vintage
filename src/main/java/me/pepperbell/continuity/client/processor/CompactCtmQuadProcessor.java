package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.function.Function;

import me.pepperbell.continuity.client.model.QuadCollectionBuilder;
import net.neoforged.neoforge.client.model.quad.MutableQuad;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import me.pepperbell.continuity.api.client.QuadProcessor;
import me.pepperbell.continuity.client.ContinuityClient;
import me.pepperbell.continuity.client.processor.simple.CtmSpriteProvider;
import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import me.pepperbell.continuity.client.properties.CompactConnectingCtmProperties;
import me.pepperbell.continuity.client.util.MathUtil;
import me.pepperbell.continuity.client.util.QuadUtil;
import me.pepperbell.continuity.client.util.RenderUtil;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
// import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadView;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

public class CompactCtmQuadProcessor extends AbstractQuadProcessor {
	protected static final int[][] QUADRANT_INDEX_MAPS = new int[8][];
	static {
		int[][] map = QUADRANT_INDEX_MAPS;

		map[0] = new int[] { 0, 1, 2, 3 }; // 0 1 2 3
		map[1] = map[0].clone(); // 3 0 1 2
		ArrayUtils.shift(map[1], 1);
		map[2] = map[1].clone(); // 2 3 0 1
		ArrayUtils.shift(map[2], 1);
		map[3] = map[2].clone(); // 1 2 3 0
		ArrayUtils.shift(map[3], 1);

		map[4] = map[0].clone(); // 3 2 1 0
		ArrayUtils.reverse(map[4]);
		map[5] = map[4].clone(); // 0 3 2 1
		ArrayUtils.shift(map[5], 1);
		map[6] = map[5].clone(); // 1 0 3 2
		ArrayUtils.shift(map[6], 1);
		map[7] = map[6].clone(); // 2 1 0 3
		ArrayUtils.shift(map[7], 1);
	}

	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected OrientationMode orientationMode;
	@Nullable
	protected TextureAtlasSprite[] replacementSprites;

	public CompactCtmQuadProcessor(TextureAtlasSprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode, @Nullable TextureAtlasSprite[] replacementSprites) {
		super(sprites, processingPredicate);
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
		this.orientationMode = orientationMode;
		this.replacementSprites = replacementSprites;
	}

	@Override
	public ProcessingResult processQuadInner(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite sprite, BlockAndTintGetter level, BlockPos pos, BlockState appearanceState, BlockState state, RandomSource random, int pass, ProcessingContext context) {
		int orientation = orientationMode.getOrientation(quad, appearanceState);
		Direction[] directions = DirectionMaps.getMap(/* quad.lightFace() */ quad.direction())[orientation];
		BlockPos.MutableBlockPos mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS);
		int connections = CtmSpriteProvider.getConnections(directions, connectionPredicate, innerSeams, mutablePos, level, pos, appearanceState, state, /* quad.lightFace() */ quad.direction(), sprite);

		//

		if (replacementSprites != null) {
			int ctmIndex = CtmSpriteProvider.SPRITE_INDEX_MAP[connections];
			TextureAtlasSprite replacementSprite = replacementSprites[ctmIndex];
			if (replacementSprite != null) {
				if (!RenderUtil.isMissingSprite(replacementSprite)) {
					QuadUtil.interpolate(quad, sprite, replacementSprite);
				}
				return ProcessingResult.NEXT_PASS;
			}
		}

		//

		// UVs normalized to the sprite dimensions and centered at the middle of the sprite
		float un0 = Mth.inverseLerp(quad.u(0), sprite.getU0(), sprite.getU1()) - 0.5f;
		float vn0 = Mth.inverseLerp(quad.v(0), sprite.getV0(), sprite.getV1()) - 0.5f;
		float un1 = Mth.inverseLerp(quad.u(1), sprite.getU0(), sprite.getU1()) - 0.5f;
		float vn1 = Mth.inverseLerp(quad.v(1), sprite.getV0(), sprite.getV1()) - 0.5f;
		float un2 = Mth.inverseLerp(quad.u(2), sprite.getU0(), sprite.getU1()) - 0.5f;
		float vn2 = Mth.inverseLerp(quad.v(2), sprite.getV0(), sprite.getV1()) - 0.5f;
		float un3 = Mth.inverseLerp(quad.u(3), sprite.getU0(), sprite.getU1()) - 0.5f;
		float vn3 = Mth.inverseLerp(quad.v(3), sprite.getV0(), sprite.getV1()) - 0.5f;

		// Signums representing which side of the splitting line the U or V coordinate lies on
		int uSignum0 = (int) Math.signum(un0);
		int vSignum0 = (int) Math.signum(vn0);
		int uSignum1 = (int) Math.signum(un1);
		int vSignum1 = (int) Math.signum(vn1);
		int uSignum2 = (int) Math.signum(un2);
		int vSignum2 = (int) Math.signum(vn2);
		int uSignum3 = (int) Math.signum(un3);
		int vSignum3 = (int) Math.signum(vn3);

		boolean uSplit01 = shouldSplitUV(uSignum0, uSignum1);
		boolean vSplit01 = shouldSplitUV(vSignum0, vSignum1);
		boolean uSplit12 = shouldSplitUV(uSignum1, uSignum2);
		boolean vSplit12 = shouldSplitUV(vSignum1, vSignum2);
		boolean uSplit23 = shouldSplitUV(uSignum2, uSignum3);
		boolean vSplit23 = shouldSplitUV(vSignum2, vSignum3);
		boolean uSplit30 = shouldSplitUV(uSignum3, uSignum0);
		boolean vSplit30 = shouldSplitUV(vSignum3, vSignum0);

		// Cannot split across U and V at the same time
		if (uSplit01 & vSplit01 | uSplit12 & vSplit12 | uSplit23 & vSplit23 | uSplit30 & vSplit30) {
			return ProcessingResult.NEXT_PROCESSOR;
		}

		// Cannot split across U twice in a row
		if (uSplit01 & uSplit12 | uSplit12 & uSplit23 | uSplit23 & uSplit30 | uSplit30 & uSplit01) {
			return ProcessingResult.NEXT_PROCESSOR;
		}

		// Cannot split across V twice in a row
		if (vSplit01 & vSplit12 | vSplit12 & vSplit23 | vSplit23 & vSplit30 | vSplit30 & vSplit01) {
			return ProcessingResult.NEXT_PROCESSOR;
		}

		//

		boolean uSplit = uSplit01 & uSplit23 | uSplit12 & uSplit30;
		boolean vSplit = vSplit01 & vSplit23 | vSplit12 & vSplit30;

		if (uSplit & vSplit) {
			int[] quadrantIndexMap = QUADRANT_INDEX_MAPS[orientation];

			int spriteIndex0 = getSpriteIndex(quadrantIndexMap[0], connections);
			int spriteIndex1 = getSpriteIndex(quadrantIndexMap[1], connections);
			int spriteIndex2 = getSpriteIndex(quadrantIndexMap[2], connections);
			int spriteIndex3 = getSpriteIndex(quadrantIndexMap[3], connections);

			boolean split01 = spriteIndex0 != spriteIndex1;
			boolean split12 = spriteIndex1 != spriteIndex2;
			boolean split23 = spriteIndex2 != spriteIndex3;
			boolean split30 = spriteIndex3 != spriteIndex0;

			if (!(split01 | split12 | split23 | split30)) {
				tryInterpolate(quad, sprite, spriteIndex0);
				return ProcessingResult.STOP;
			}

			VertexContainer vertexContainer = context.getData(ProcessingDataKeys.VERTEX_CONTAINER);
			vertexContainer.fillBaseVertices(quad);

			/* QuadEmitter */ QuadCollectionBuilder extraQuadEmitter = context.getExtraQuadEmitter();

			if (split01 & split12 & split23 & split30) {
				float delta01;
				float delta23;
				float delta12;
				float delta30;
				float delta4;
				if (uSplit01) {
					delta01 = Mth.inverseLerp(0, un0, un1);
					delta23 = Mth.inverseLerp(0, un2, un3);
					delta12 = Mth.inverseLerp(0, vn1, vn2);
					delta30 = Mth.inverseLerp(0, vn3, vn0);
					delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, vn0, vn1), Mth.lerp(delta23, vn2, vn3));
				} else {
					delta01 = Mth.inverseLerp(0, vn0, vn1);
					delta23 = Mth.inverseLerp(0, vn2, vn3);
					delta12 = Mth.inverseLerp(0, un1, un2);
					delta30 = Mth.inverseLerp(0, un3, un0);
					delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, un0, un1), Mth.lerp(delta23, un2, un3));
				}

				vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
				vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
				vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);
				vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);
				vertexContainer.vertex4.setLerped(delta4, vertexContainer.vertex01, vertexContainer.vertex23);

				splitQuadrant(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndex0);
				splitQuadrant(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
				splitQuadrant(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndex2);
				splitQuadrant(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
			} else {
				if (!(split01 | split12)) {
					split12 = true;
				} else if (!(split12 | split23)) {
					split23 = true;
				} else if (!(split23 | split30)) {
					split30 = true;
				} else if (!(split30 | split01)) {
					split01 = true;
				}

				int splits = (split01 ? 1 : 0) + (split12 ? 1 : 0) + (split23 ? 1 : 0) + (split30 ? 1 : 0);
				if (splits == 2) {
					if (split01) {
						float delta01;
						float delta23;
						if (uSplit01) {
							delta01 = Mth.inverseLerp(0, un0, un1);
							delta23 = Mth.inverseLerp(0, un2, un3);
						} else {
							delta01 = Mth.inverseLerp(0, vn0, vn1);
							delta23 = Mth.inverseLerp(0, vn2, vn3);
						}

						vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
						vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);

						splitHalf(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
						splitHalf(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
					} else {
						float delta12;
						float delta30;
						if (uSplit01) {
							delta12 = Mth.inverseLerp(0, vn1, vn2);
							delta30 = Mth.inverseLerp(0, vn3, vn0);
						} else {
							delta12 = Mth.inverseLerp(0, un1, un2);
							delta30 = Mth.inverseLerp(0, un3, un0);
						}

						vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
						vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);

						splitHalf(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndex0);
						splitHalf(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndex2);
					}
				} else { // 3
					if (!split01) {
						float delta23;
						float delta12;
						float delta30;
						float delta4;
						if (uSplit01) {
							delta23 = Mth.inverseLerp(0, un2, un3);
							delta12 = Mth.inverseLerp(0, vn1, vn2);
							delta30 = Mth.inverseLerp(0, vn3, vn0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta12, un1, un2), Mth.lerp(delta30, un3, un0));
						} else {
							delta23 = Mth.inverseLerp(0, vn2, vn3);
							delta12 = Mth.inverseLerp(0, un1, un2);
							delta30 = Mth.inverseLerp(0, un3, un0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta12, vn1, vn2), Mth.lerp(delta30, vn3, vn0));
						}

						vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);
						vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
						vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);
						vertexContainer.vertex4.setLerped(delta4, vertexContainer.vertex12, vertexContainer.vertex30);

						splitHalf(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndex0);
						splitQuadrant(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndex2);
						splitQuadrant(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
					} else if (!split12) {
						float delta01;
						float delta23;
						float delta30;
						float delta4;
						if (uSplit01) {
							delta01 = Mth.inverseLerp(0, un0, un1);
							delta23 = Mth.inverseLerp(0, un2, un3);
							delta30 = Mth.inverseLerp(0, vn3, vn0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, vn0, vn1), Mth.lerp(delta23, vn2, vn3));
						} else {
							delta01 = Mth.inverseLerp(0, vn0, vn1);
							delta23 = Mth.inverseLerp(0, vn2, vn3);
							delta30 = Mth.inverseLerp(0, un3, un0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, un0, un1), Mth.lerp(delta23, un2, un3));
						}

						vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
						vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);
						vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);
						vertexContainer.vertex4.setLerped(delta4, vertexContainer.vertex01, vertexContainer.vertex23);

						splitQuadrant(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndex0);
						splitHalf(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
						splitQuadrant(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
					} else if (!split23) {
						float delta01;
						float delta12;
						float delta30;
						float delta4;
						if (uSplit01) {
							delta01 = Mth.inverseLerp(0, un0, un1);
							delta12 = Mth.inverseLerp(0, vn1, vn2);
							delta30 = Mth.inverseLerp(0, vn3, vn0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta12, un1, un2), Mth.lerp(delta30, un3, un0));
						} else {
							delta01 = Mth.inverseLerp(0, vn0, vn1);
							delta12 = Mth.inverseLerp(0, un1, un2);
							delta30 = Mth.inverseLerp(0, un3, un0);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta12, vn1, vn2), Mth.lerp(delta30, vn3, vn0));
						}

						vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
						vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
						vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);
						vertexContainer.vertex4.setLerped(delta4, vertexContainer.vertex12, vertexContainer.vertex30);

						splitQuadrant(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndex0);
						splitQuadrant(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
						splitHalf(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndex2);
					} else { // !split30
						float delta01;
						float delta23;
						float delta12;
						float delta4;
						if (uSplit01) {
							delta01 = Mth.inverseLerp(0, un0, un1);
							delta23 = Mth.inverseLerp(0, un2, un3);
							delta12 = Mth.inverseLerp(0, vn1, vn2);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, vn0, vn1), Mth.lerp(delta23, vn2, vn3));
						} else {
							delta01 = Mth.inverseLerp(0, vn0, vn1);
							delta23 = Mth.inverseLerp(0, vn2, vn3);
							delta12 = Mth.inverseLerp(0, un1, un2);
							delta4 = Mth.inverseLerp(0, Mth.lerp(delta01, un0, un1), Mth.lerp(delta23, un2, un3));
						}

						vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
						vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
						vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);
						vertexContainer.vertex4.setLerped(delta4, vertexContainer.vertex01, vertexContainer.vertex23);

						splitHalf(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
						splitQuadrant(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
						splitQuadrant(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndex2);
					}
				}
			}

			return ProcessingResult.DISCARD;
		} else if (uSplit | vSplit) {
			boolean firstSplit;
			boolean swapAB;
			int spriteIndexA;
			int spriteIndexB;
			if (uSplit) {
				firstSplit = uSplit01;
				swapAB = orientation == 2 || orientation == 3 || orientation == 4 || orientation == 5;
				if ((vSignum0 + vSignum1 + vSignum2 + vSignum3) <= 0) {
					spriteIndexA = getSpriteIndex(0, connections);
					spriteIndexB = getSpriteIndex(3, connections);
				} else {
					spriteIndexA = getSpriteIndex(1, connections);
					spriteIndexB = getSpriteIndex(2, connections);
				}
			} else {
				firstSplit = vSplit01;
				swapAB = orientation == 1 || orientation == 2 || orientation == 5 || orientation == 6;
				if ((uSignum0 + uSignum1 + uSignum2 + uSignum3) <= 0) {
					spriteIndexA = getSpriteIndex(1, connections);
					spriteIndexB = getSpriteIndex(0, connections);
				} else {
					spriteIndexA = getSpriteIndex(2, connections);
					spriteIndexB = getSpriteIndex(3, connections);
				}
			}

			if (spriteIndexA == spriteIndexB) {
				tryInterpolate(quad, sprite, spriteIndexA);
				return ProcessingResult.STOP;
			}

			if (swapAB) {
				int temp = spriteIndexA;
				spriteIndexA = spriteIndexB;
				spriteIndexB = temp;
			}

			VertexContainer vertexContainer = context.getData(ProcessingDataKeys.VERTEX_CONTAINER);
			vertexContainer.fillBaseVertices(quad);

			/* QuadEmitter */ QuadCollectionBuilder extraQuadEmitter = context.getExtraQuadEmitter();

			if (firstSplit) {
				float delta01;
				float delta23;
				if (uSplit) {
					delta01 = Mth.inverseLerp(0, un0, un1);
					delta23 = Mth.inverseLerp(0, un2, un3);
				} else {
					delta01 = Mth.inverseLerp(0, vn0, vn1);
					delta23 = Mth.inverseLerp(0, vn2, vn3);
				}

				vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
				vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);

				splitHalf(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndexA);
				splitHalf(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndexB);
			} else {
				float delta12;
				float delta30;
				if (uSplit) {
					delta12 = Mth.inverseLerp(0, un1, un2);
					delta30 = Mth.inverseLerp(0, un3, un0);
				} else {
					delta12 = Mth.inverseLerp(0, vn1, vn2);
					delta30 = Mth.inverseLerp(0, vn3, vn0);
				}

				vertexContainer.vertex12.setLerped(delta12, vertexContainer.vertex1, vertexContainer.vertex2);
				vertexContainer.vertex30.setLerped(delta30, vertexContainer.vertex3, vertexContainer.vertex0);

				splitHalf(quad, sprite, vertexContainer, 0, extraQuadEmitter, spriteIndexA);
				splitHalf(quad, sprite, vertexContainer, 2, extraQuadEmitter, spriteIndexB);
			}

			return ProcessingResult.DISCARD;
		} else {
			int quadrant;
			if ((uSignum0 + uSignum1 + uSignum2 + uSignum3) <= 0) {
				if ((vSignum0 + vSignum1 + vSignum2 + vSignum3) <= 0) {
					quadrant = 0;
				} else {
					quadrant = 1;
				}
			} else {
				if ((vSignum0 + vSignum1 + vSignum2 + vSignum3) <= 0) {
					quadrant = 3;
				} else {
					quadrant = 2;
				}
			}

			int spriteIndex = getSpriteIndex(quadrant, connections);
			tryInterpolate(quad, sprite, spriteIndex);
			return ProcessingResult.STOP;
		}
	}

	// True if and only if one argument is 1 and the other is -1
	protected static boolean shouldSplitUV(int signumA, int signumB) {
		return (signumA ^ signumB) == -2;
	}

	/*
	0 - Unconnected
	1 - Fully connected
	2 - Up and down / vertical
	3 - Left and right / horizontal
	4 - Unconnected corners
	 */
	protected int getSpriteIndex(int quadrantIndex, int connections) {
		int index1 = quadrantIndex;
		int index2 = (quadrantIndex + 3) % 4;
		boolean connected1 = ((connections >>> index1 * 2) & 1) == 1;
		boolean connected2 = ((connections >>> index2 * 2) & 1) == 1;
		if (connected1 & connected2) {
			if (((connections >>> (index2 * 2 + 1)) & 1) == 1) {
				return 1;
			}
			return 4;
		}
		if (connected1) { // 0 - h, 1 - v, 2 - h, 3 - v
			return 3 - quadrantIndex % 2;
		}
		if (connected2) { // 0 - v, 1 - h, 2 - v, 3 - h
			return 2 + quadrantIndex % 2;
		}
		return 0;
	}

	protected void tryInterpolate(/* MutableQuadView */ MutableQuad quad, TextureAtlasSprite oldSprite, int spriteIndex) {
		TextureAtlasSprite newSprite = sprites[spriteIndex];
		if (!RenderUtil.isMissingSprite(newSprite)) {
			QuadUtil.interpolate(quad, oldSprite, newSprite);
		}
	}

	protected void splitHalf(/* QuadView */ MutableQuad quad, TextureAtlasSprite sprite, VertexContainer vertexContainer, int id, /* QuadEmitter */ QuadCollectionBuilder quadEmitter, int spriteIndex) {
		var toEmit = quadEmitter.getScratchQuad(quad); // quadEmitter.copyFrom(quad);
		vertexContainer.lerpedVertices[(id + 1) % 4].writeToQuad(/* quadEmitter */ toEmit, (id + 2) % 4);
		int id3 = (id + 3) % 4;
		vertexContainer.lerpedVertices[id3].writeToQuad(/* quadEmitter */ toEmit, id3);
		tryInterpolate(/* quadEmitter */ toEmit, sprite, spriteIndex);
		quadEmitter.emitQuad(); // quadEmitter.emit();
	}

	protected void splitQuadrant(/* QuadView */ MutableQuad quad, TextureAtlasSprite sprite, VertexContainer vertexContainer, int id, /* QuadEmitter */ QuadCollectionBuilder quadEmitter, int spriteIndex) {
		var toEmit = quadEmitter.getScratchQuad(quad); // quadEmitter.copyFrom(quad);
		vertexContainer.lerpedVertices[id].writeToQuad(/* quadEmitter */ toEmit, (id + 1) % 4);
		vertexContainer.vertex4.writeToQuad(/* quadEmitter */ toEmit, (id + 2) % 4);
		int id3 = (id + 3) % 4;
		vertexContainer.lerpedVertices[id3].writeToQuad(/* quadEmitter */ toEmit, id3);
		tryInterpolate(/* quadEmitter */ toEmit, sprite, spriteIndex);
		quadEmitter.emitQuad(); // quadEmitter.emit();
	}

	public static class Vertex {
		public float x;
		public float y;
		public float z;
		public int color;
		// public int light;
		public float u;
		public float v;
		// public boolean hasNormal;
		public float normalX;
		public float normalY;
		public float normalZ;

		public void readFromQuad(/* QuadView */ MutableQuad quad, int vertexIndex) {
			x = quad.x(vertexIndex);
			y = quad.y(vertexIndex);
			z = quad.z(vertexIndex);
			color = quad.color(vertexIndex);
			// light = quad.lightmap(vertexIndex);
			u = quad.u(vertexIndex);
			v = quad.v(vertexIndex);
			// hasNormal = quad.hasNormal(vertexIndex);
			// if (hasNormal) {
				normalX = quad.normalX(vertexIndex);
				normalY = quad.normalY(vertexIndex);
				normalZ = quad.normalZ(vertexIndex);
			// }
		}

		public void writeToQuad(/* MutableQuadView */ MutableQuad quad, int vertexIndex) {
			quad.setPosition(vertexIndex, x, y, z); // quad.pos(vertexIndex, x, y, z);
			quad.setColor(vertexIndex, color); // quad.color(vertexIndex, color);
			// quad.lightmap(vertexIndex, light);
			quad.setUv(vertexIndex, u, v); // quad.uv(vertexIndex, u, v);
			// if (hasNormal) {
				quad.setNormal(vertexIndex, normalX, normalY, normalZ); // quad.normal(vertexIndex, normalX, normalY, normalZ);
			// }
		}

		public void set(Vertex other) {
			x = other.x;
			y = other.y;
			z = other.z;
			color = other.color;
			// light = other.light;
			u = other.u;
			v = other.v;
			// hasNormal = other.hasNormal;
			// if (hasNormal) {
				normalX = other.normalX;
				normalY = other.normalY;
				normalZ = other.normalZ;
			// }
		}

		public void setLerped(float delta, Vertex vertexA, Vertex vertexB) {
			x = Mth.lerp(delta, vertexA.x, vertexB.x);
			y = Mth.lerp(delta, vertexA.y, vertexB.y);
			z = Mth.lerp(delta, vertexA.z, vertexB.z);
			color = MathUtil.lerpColor(delta, vertexA.color, vertexB.color);
			// light = MathUtil.lerpLight(delta, vertexA.light, vertexB.light);
			u = Mth.lerp(delta, vertexA.u, vertexB.u);
			v = Mth.lerp(delta, vertexA.v, vertexB.v);
			// if (vertexA.hasNormal && vertexB.hasNormal) {
				normalX = Mth.lerp(delta, vertexA.normalX, vertexB.normalX);
				normalY = Mth.lerp(delta, vertexA.normalY, vertexB.normalY);
				normalZ = Mth.lerp(delta, vertexA.normalZ, vertexB.normalZ);
				float sqLength = normalX * normalX + normalY * normalY + normalZ * normalZ;
				if (sqLength != 0) {
					float scale = 1 / (float) Math.sqrt(sqLength);
					normalX *= scale;
					normalY *= scale;
					normalZ *= scale;
				}
			// }
		}
	}

	public static class VertexContainer {
		public final Vertex vertex0 = new Vertex();
		public final Vertex vertex1 = new Vertex();
		public final Vertex vertex2 = new Vertex();
		public final Vertex vertex3 = new Vertex();
		public final Vertex vertex01 = new Vertex();
		public final Vertex vertex12 = new Vertex();
		public final Vertex vertex23 = new Vertex();
		public final Vertex vertex30 = new Vertex();
		public final Vertex vertex4 = new Vertex();

		public final Vertex[] lerpedVertices = new Vertex[] {
				vertex01, vertex12, vertex23, vertex30
		};

		public void fillBaseVertices(/* QuadView */ MutableQuad quad) {
			vertex0.readFromQuad(quad, 0);
			vertex1.readFromQuad(quad, 1);
			vertex2.readFromQuad(quad, 2);
			vertex3.readFromQuad(quad, 3);
		}
	}

	// TODO
	public static class Factory implements QuadProcessor.Factory<CompactConnectingCtmProperties> {
		@Override
		public QuadProcessor createProcessor(CompactConnectingCtmProperties properties, Function<Identifier, TextureAtlasSprite> spriteGetter) {
			int spriteAmount = getSpriteAmount(properties);
			List<Identifier> spriteIds = properties.getSpriteIds();
			int provided = spriteIds.size();
			int max = provided;

			TextureAtlasSprite[] replacementSprites = null;
			Int2IntMap replacementMap = properties.getTileReplacementMap();
			if (replacementMap != null) {
				int replacementSpriteAmount = getReplacementSpriteAmount(properties);
				replacementSprites = new TextureAtlasSprite[replacementSpriteAmount];
				ObjectIterator<Int2IntMap.Entry> entryIterator = Int2IntMaps.fastIterator(replacementMap);
				while (entryIterator.hasNext()) {
					Int2IntMap.Entry entry = entryIterator.next();
					int key = entry.getIntKey();
					if (key < replacementSpriteAmount) {
						int value = entry.getIntValue();
						if (value < provided) {
							replacementSprites[key] = spriteGetter.apply(spriteIds.get(value));
						} else {
							ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " with tile " + value + " as only " + provided + " tiles were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
						}
					} else {
						ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " as method '" + properties.getMethod() + "' only supports " + replacementSpriteAmount + " replacement tiles in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
					}
				}
			}

			if (provided > spriteAmount) {
				if (replacementSprites == null) {
					ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + spriteAmount + " tiles but " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
				}
				max = spriteAmount;
			}

			TextureAtlasSprite[] sprites = new TextureAtlasSprite[spriteAmount];
			TextureAtlasSprite missingSprite = spriteGetter.apply(MissingTextureAtlasSprite.getLocation());
			boolean supportsNullSprites = supportsNullSprites(properties);
			for (int i = 0; i < max; i++) {
				TextureAtlasSprite sprite;
				Identifier spriteId = spriteIds.get(i);
				if (spriteId.equals(BaseCtmProperties.SPECIAL_SKIP_ID)) {
					sprite = missingSprite;
				} else if (spriteId.equals(BaseCtmProperties.SPECIAL_DEFAULT_ID)) {
					sprite = supportsNullSprites ? null : missingSprite;
				} else {
					sprite = spriteGetter.apply(spriteId);
				}
				sprites[i] = sprite;
			}

			if (provided < spriteAmount) {
				ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires at least " + spriteAmount + " tiles but only " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
				for (int i = provided; i < spriteAmount; i++) {
					sprites[i] = missingSprite;
				}
			}

			return createProcessor(properties, sprites, replacementSprites);
		}

		public QuadProcessor createProcessor(CompactConnectingCtmProperties properties, TextureAtlasSprite[] sprites, @Nullable TextureAtlasSprite[] replacementSprites) {
			return new CompactCtmQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode(), replacementSprites);
		}

		public int getSpriteAmount(CompactConnectingCtmProperties properties) {
			return 5;
		}

		public int getReplacementSpriteAmount(CompactConnectingCtmProperties properties) {
			return 47;
		}

		public boolean supportsNullSprites(CompactConnectingCtmProperties properties) {
			return false;
		}
	}
}

package me.pepperbell.continuity.client.processor;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

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
import me.pepperbell.continuity.client.util.TextureUtil;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.minecraft.block.BlockState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

public class CompactCtmQuadProcessor extends AbstractQuadProcessor {
	protected static final int[][] QUADRANT_INDEX_MAPS = new int[8][];
	static {
		int[][] map = QUADRANT_INDEX_MAPS;

		map[0] = new int[] { 0, 1, 2, 3 }; // 0 - 0 1 2 3
		map[1] = map[0].clone(); // 1 - 3 0 1 2
		ArrayUtils.shift(map[1], 1);
		map[2] = map[1].clone(); // 2 - 2 3 0 1
		ArrayUtils.shift(map[2], 1);
		map[3] = map[2].clone(); // 3 - 1 2 3 0
		ArrayUtils.shift(map[3], 1);

		map[4] = map[3].clone(); // 4 - 0 3 2 1
		ArrayUtils.reverse(map[4]);
		map[5] = map[4].clone(); // 5 - 1 0 3 2
		ArrayUtils.shift(map[5], 1);
		map[6] = map[5].clone(); // 6 - 2 1 0 3
		ArrayUtils.shift(map[6], 1);
		map[7] = map[6].clone(); // 7 - 3 2 1 0
		ArrayUtils.shift(map[7], 1);
	}

	protected ConnectionPredicate connectionPredicate;
	protected boolean innerSeams;
	protected OrientationMode orientationMode;
	@Nullable
	protected Sprite[] replacementSprites;

	public CompactCtmQuadProcessor(Sprite[] sprites, ProcessingPredicate processingPredicate, ConnectionPredicate connectionPredicate, boolean innerSeams, OrientationMode orientationMode, @Nullable Sprite[] replacementSprites) {
		super(sprites, processingPredicate);
		this.connectionPredicate = connectionPredicate;
		this.innerSeams = innerSeams;
		this.orientationMode = orientationMode;
		this.replacementSprites = replacementSprites;
	}

	@Override
	public ProcessingResult processQuadInner(MutableQuadView quad, Sprite sprite, BlockRenderView blockView, BlockState appearanceState, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, int pass, ProcessingContext context) {
		int orientation = orientationMode.getOrientation(quad, appearanceState);
		Direction[] directions = DirectionMaps.getMap(quad.lightFace())[orientation];
		BlockPos.Mutable mutablePos = context.getData(ProcessingDataKeys.MUTABLE_POS);
		int connections = CtmSpriteProvider.getConnections(directions, connectionPredicate, innerSeams, mutablePos, blockView, appearanceState, state, pos, quad.lightFace(), sprite);

		//

		if (replacementSprites != null) {
			int ctmIndex = CtmSpriteProvider.SPRITE_INDEX_MAP[connections];
			Sprite replacementSprite = replacementSprites[ctmIndex];
			if (replacementSprite != null) {
				if (!TextureUtil.isMissingSprite(replacementSprite)) {
					QuadUtil.interpolate(quad, sprite, replacementSprite);
				}
				return ProcessingResult.NEXT_PASS;
			}
		}

		//

		// UVs normalized to the sprite dimensions and centered at the middle of the sprite
		float un0 = MathHelper.getLerpProgress(quad.u(0), sprite.getMinU(), sprite.getMaxU()) - 0.5f;
		float vn0 = MathHelper.getLerpProgress(quad.v(0), sprite.getMinV(), sprite.getMaxV()) - 0.5f;
		float un1 = MathHelper.getLerpProgress(quad.u(1), sprite.getMinU(), sprite.getMaxU()) - 0.5f;
		float vn1 = MathHelper.getLerpProgress(quad.v(1), sprite.getMinV(), sprite.getMaxV()) - 0.5f;
		float un2 = MathHelper.getLerpProgress(quad.u(2), sprite.getMinU(), sprite.getMaxU()) - 0.5f;
		float vn2 = MathHelper.getLerpProgress(quad.v(2), sprite.getMinV(), sprite.getMaxV()) - 0.5f;
		float un3 = MathHelper.getLerpProgress(quad.u(3), sprite.getMinU(), sprite.getMaxU()) - 0.5f;
		float vn3 = MathHelper.getLerpProgress(quad.v(3), sprite.getMinV(), sprite.getMaxV()) - 0.5f;

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

			QuadEmitter extraQuadEmitter = context.getExtraQuadEmitter();

			if (split01 & split12 & split23 & split30) {
				float delta01;
				float delta23;
				float delta12;
				float delta30;
				float delta4;
				if (uSplit01) {
					delta01 = MathHelper.getLerpProgress(0, un0, un1);
					delta23 = MathHelper.getLerpProgress(0, un2, un3);
					delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
					delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
					delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, vn0, vn1), MathHelper.lerp(delta23, vn2, vn3));
				} else {
					delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
					delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
					delta12 = MathHelper.getLerpProgress(0, un1, un2);
					delta30 = MathHelper.getLerpProgress(0, un3, un0);
					delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, un0, un1), MathHelper.lerp(delta23, un2, un3));
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
							delta01 = MathHelper.getLerpProgress(0, un0, un1);
							delta23 = MathHelper.getLerpProgress(0, un2, un3);
						} else {
							delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
							delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
						}

						vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
						vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);

						splitHalf(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndex1);
						splitHalf(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndex3);
					} else {
						float delta12;
						float delta30;
						if (uSplit01) {
							delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
							delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
						} else {
							delta12 = MathHelper.getLerpProgress(0, un1, un2);
							delta30 = MathHelper.getLerpProgress(0, un3, un0);
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
							delta23 = MathHelper.getLerpProgress(0, un2, un3);
							delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
							delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta12, un1, un2), MathHelper.lerp(delta30, un3, un0));
						} else {
							delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
							delta12 = MathHelper.getLerpProgress(0, un1, un2);
							delta30 = MathHelper.getLerpProgress(0, un3, un0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta12, vn1, vn2), MathHelper.lerp(delta30, vn3, vn0));
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
							delta01 = MathHelper.getLerpProgress(0, un0, un1);
							delta23 = MathHelper.getLerpProgress(0, un2, un3);
							delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, vn0, vn1), MathHelper.lerp(delta23, vn2, vn3));
						} else {
							delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
							delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
							delta30 = MathHelper.getLerpProgress(0, un3, un0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, un0, un1), MathHelper.lerp(delta23, un2, un3));
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
							delta01 = MathHelper.getLerpProgress(0, un0, un1);
							delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
							delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta12, un1, un2), MathHelper.lerp(delta30, un3, un0));
						} else {
							delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
							delta12 = MathHelper.getLerpProgress(0, un1, un2);
							delta30 = MathHelper.getLerpProgress(0, un3, un0);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta12, vn1, vn2), MathHelper.lerp(delta30, vn3, vn0));
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
							delta01 = MathHelper.getLerpProgress(0, un0, un1);
							delta23 = MathHelper.getLerpProgress(0, un2, un3);
							delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, vn0, vn1), MathHelper.lerp(delta23, vn2, vn3));
						} else {
							delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
							delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
							delta12 = MathHelper.getLerpProgress(0, un1, un2);
							delta4 = MathHelper.getLerpProgress(0, MathHelper.lerp(delta01, un0, un1), MathHelper.lerp(delta23, un2, un3));
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
				swapAB = orientation == 2 || orientation == 3 || orientation == 4 || orientation == 7;
				if ((vSignum0 + vSignum1 + vSignum2 + vSignum3) <= 0) {
					spriteIndexA = getSpriteIndex(0, connections);
					spriteIndexB = getSpriteIndex(3, connections);
				} else {
					spriteIndexA = getSpriteIndex(1, connections);
					spriteIndexB = getSpriteIndex(2, connections);
				}
			} else {
				firstSplit = vSplit01;
				swapAB = orientation == 1 || orientation == 2 || orientation == 4 || orientation == 5;
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

			QuadEmitter extraQuadEmitter = context.getExtraQuadEmitter();

			if (firstSplit) {
				float delta01;
				float delta23;
				if (uSplit) {
					delta01 = MathHelper.getLerpProgress(0, un0, un1);
					delta23 = MathHelper.getLerpProgress(0, un2, un3);
				} else {
					delta01 = MathHelper.getLerpProgress(0, vn0, vn1);
					delta23 = MathHelper.getLerpProgress(0, vn2, vn3);
				}

				vertexContainer.vertex01.setLerped(delta01, vertexContainer.vertex0, vertexContainer.vertex1);
				vertexContainer.vertex23.setLerped(delta23, vertexContainer.vertex2, vertexContainer.vertex3);

				splitHalf(quad, sprite, vertexContainer, 1, extraQuadEmitter, spriteIndexA);
				splitHalf(quad, sprite, vertexContainer, 3, extraQuadEmitter, spriteIndexB);
			} else {
				float delta12;
				float delta30;
				if (uSplit) {
					delta12 = MathHelper.getLerpProgress(0, un1, un2);
					delta30 = MathHelper.getLerpProgress(0, un3, un0);
				} else {
					delta12 = MathHelper.getLerpProgress(0, vn1, vn2);
					delta30 = MathHelper.getLerpProgress(0, vn3, vn0);
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
		if (connected1 && connected2) {
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

	protected void tryInterpolate(MutableQuadView quad, Sprite oldSprite, int spriteIndex) {
		Sprite newSprite = sprites[spriteIndex];
		if (!TextureUtil.isMissingSprite(newSprite)) {
			QuadUtil.interpolate(quad, oldSprite, newSprite);
		}
	}

	protected void splitHalf(QuadView quad, Sprite sprite, VertexContainer vertexContainer, int id, QuadEmitter quadEmitter, int spriteIndex) {
		quadEmitter.copyFrom(quad);
		vertexContainer.lerpedVertices[(id + 1) % 4].writeToQuad(quadEmitter, (id + 2) % 4);
		int id3 = (id + 3) % 4;
		vertexContainer.lerpedVertices[id3].writeToQuad(quadEmitter, id3);
		tryInterpolate(quadEmitter, sprite, spriteIndex);
		quadEmitter.emit();
	}

	protected void splitQuadrant(QuadView quad, Sprite sprite, VertexContainer vertexContainer, int id, QuadEmitter quadEmitter, int spriteIndex) {
		quadEmitter.copyFrom(quad);
		vertexContainer.lerpedVertices[id].writeToQuad(quadEmitter, (id + 1) % 4);
		vertexContainer.vertex4.writeToQuad(quadEmitter, (id + 2) % 4);
		int id3 = (id + 3) % 4;
		vertexContainer.lerpedVertices[id3].writeToQuad(quadEmitter, id3);
		tryInterpolate(quadEmitter, sprite, spriteIndex);
		quadEmitter.emit();
	}

	public static class Vertex {
		public float x;
		public float y;
		public float z;
		public int color;
		public int light;
		public float u;
		public float v;
		public boolean hasNormal;
		public float normalX;
		public float normalY;
		public float normalZ;

		public void readFromQuad(QuadView quad, int vertexIndex) {
			x = quad.x(vertexIndex);
			y = quad.y(vertexIndex);
			z = quad.z(vertexIndex);
			color = quad.color(vertexIndex);
			light = quad.lightmap(vertexIndex);
			u = quad.u(vertexIndex);
			v = quad.v(vertexIndex);
			hasNormal = quad.hasNormal(vertexIndex);
			if (hasNormal) {
				normalX = quad.normalX(vertexIndex);
				normalY = quad.normalY(vertexIndex);
				normalZ = quad.normalZ(vertexIndex);
			}
		}

		public void writeToQuad(MutableQuadView quad, int vertexIndex) {
			quad.pos(vertexIndex, x, y, z);
			quad.color(vertexIndex, color);
			quad.lightmap(vertexIndex, light);
			quad.uv(vertexIndex, u, v);
			if (hasNormal) {
				quad.normal(vertexIndex, normalX, normalY, normalZ);
			}
		}

		public void set(Vertex other) {
			x = other.x;
			y = other.y;
			z = other.z;
			color = other.color;
			light = other.light;
			u = other.u;
			v = other.v;
			hasNormal = other.hasNormal;
			if (hasNormal) {
				normalX = other.normalX;
				normalY = other.normalY;
				normalZ = other.normalZ;
			}
		}

		public void setLerped(float delta, Vertex vertexA, Vertex vertexB) {
			x = MathHelper.lerp(delta, vertexA.x, vertexB.x);
			y = MathHelper.lerp(delta, vertexA.y, vertexB.y);
			z = MathHelper.lerp(delta, vertexA.z, vertexB.z);
			color = MathUtil.lerpColor(delta, vertexA.color, vertexB.color);
			light = MathUtil.lerpLight(delta, vertexA.light, vertexB.light);
			u = MathHelper.lerp(delta, vertexA.u, vertexB.u);
			v = MathHelper.lerp(delta, vertexA.v, vertexB.v);
			if (vertexA.hasNormal && vertexB.hasNormal) {
				normalX = MathHelper.lerp(delta, vertexA.normalX, vertexB.normalX);
				normalY = MathHelper.lerp(delta, vertexA.normalY, vertexB.normalY);
				normalZ = MathHelper.lerp(delta, vertexA.normalZ, vertexB.normalZ);
				float sqLength = normalX * normalX + normalY * normalY + normalZ * normalZ;
				if (sqLength != 0) {
					float scale = 1 / (float) Math.sqrt(sqLength);
					normalX *= scale;
					normalY *= scale;
					normalZ *= scale;
				}
			}
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

		public void fillBaseVertices(QuadView quad) {
			vertex0.readFromQuad(quad, 0);
			vertex1.readFromQuad(quad, 1);
			vertex2.readFromQuad(quad, 2);
			vertex3.readFromQuad(quad, 3);
		}
	}

	// TODO
	public static class Factory implements QuadProcessor.Factory<CompactConnectingCtmProperties> {
		@Override
		public QuadProcessor createProcessor(CompactConnectingCtmProperties properties, Function<SpriteIdentifier, Sprite> textureGetter) {
			int textureAmount = getTextureAmount(properties);
			List<SpriteIdentifier> spriteIds = properties.getSpriteIds();
			int provided = spriteIds.size();
			int max = provided;

			Sprite[] replacementSprites = null;
			Int2IntMap replacementMap = properties.getTileReplacementMap();
			if (replacementMap != null) {
				int replacementTextureAmount = getReplacementTextureAmount(properties);
				replacementSprites = new Sprite[replacementTextureAmount];
				ObjectIterator<Int2IntMap.Entry> entryIterator = Int2IntMaps.fastIterator(replacementMap);
				while (entryIterator.hasNext()) {
					Int2IntMap.Entry entry = entryIterator.next();
					int key = entry.getIntKey();
					if (key < replacementTextureAmount) {
						int value = entry.getIntValue();
						if (value < provided) {
							replacementSprites[key] = textureGetter.apply(spriteIds.get(value));
						} else {
							ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " with tile " + value + " as only " + provided + " tiles were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
						}
					} else {
						ContinuityClient.LOGGER.warn("Cannot replace tile " + key + " as method '" + properties.getMethod() + "' only supports " + replacementTextureAmount + " replacement tiles in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
					}
				}
			}

			if (provided > textureAmount) {
				if (replacementSprites == null) {
					ContinuityClient.LOGGER.warn("Method '" + properties.getMethod() + "' requires " + textureAmount + " tiles but " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
				}
				max = textureAmount;
			}

			Sprite[] sprites = new Sprite[textureAmount];
			Sprite missingSprite = textureGetter.apply(TextureUtil.MISSING_SPRITE_ID);
			boolean supportsNullSprites = supportsNullSprites(properties);
			for (int i = 0; i < max; i++) {
				Sprite sprite;
				SpriteIdentifier spriteId = spriteIds.get(i);
				if (spriteId.equals(BaseCtmProperties.SPECIAL_SKIP_SPRITE_ID)) {
					sprite = missingSprite;
				} else if (spriteId.equals(BaseCtmProperties.SPECIAL_DEFAULT_SPRITE_ID)) {
					sprite = supportsNullSprites ? null : missingSprite;
				} else {
					sprite = textureGetter.apply(spriteId);
				}
				sprites[i] = sprite;
			}

			if (provided < textureAmount) {
				ContinuityClient.LOGGER.error("Method '" + properties.getMethod() + "' requires at least " + textureAmount + " tiles but only " + provided + " were provided in file '" + properties.getResourceId() + "' in pack '" + properties.getPackId() + "'");
				for (int i = provided; i < textureAmount; i++) {
					sprites[i] = missingSprite;
				}
			}

			return createProcessor(properties, sprites, replacementSprites);
		}

		public QuadProcessor createProcessor(CompactConnectingCtmProperties properties, Sprite[] sprites, @Nullable Sprite[] replacementSprites) {
			return new CompactCtmQuadProcessor(sprites, BaseProcessingPredicate.fromProperties(properties), properties.getConnectionPredicate(), properties.getInnerSeams(), properties.getOrientationMode(), replacementSprites);
		}

		public int getTextureAmount(CompactConnectingCtmProperties properties) {
			return 5;
		}

		public int getReplacementTextureAmount(CompactConnectingCtmProperties properties) {
			return 47;
		}

		public boolean supportsNullSprites(CompactConnectingCtmProperties properties) {
			return false;
		}
	}
}

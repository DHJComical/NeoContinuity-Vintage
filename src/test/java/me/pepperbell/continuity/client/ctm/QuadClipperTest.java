package me.pepperbell.continuity.client.ctm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.pepperbell.continuity.client.ctm.QuadClipper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import org.junit.jupiter.api.Test;

/**
 * Verifies the UV/geometry clipping math of {@link QuadClipper} against simple analytic cases.
 * A single sprite covering 0..1 in UV space is used so expected values are exact.
 */
class QuadClipperTest {

	/** A north-facing quad with UVs 0..1 across a unit face. */
	private static BakedQuad makeQuad(float minU, float minV, float maxU, float maxV) {
		float[] verts = new float[4 * 5];
		// POSITION_TEX layout: x,y,z,u,v
		// North face: x goes right (u), y goes up (v). Vertex order: bottom-left, bottom-right, top-right, top-left
		write(verts, 0, 0, 0, 0, minU, minV);
		write(verts, 1, 1, 0, 0, maxU, minV);
		write(verts, 2, 1, 1, 0, maxU, maxV);
		write(verts, 3, 0, 1, 0, minU, maxV);
		int[] data = new int[verts.length];
		for (int i = 0; i < verts.length; i++) {
			data[i] = Float.floatToIntBits(verts[i]);
		}
		BakedQuad quad = new BakedQuad(data, -1, EnumFacing.NORTH, null, true, DefaultVertexFormats.POSITION_TEX);
		return quad;
	}

	private static void write(float[] data, int v, float x, float y, float z, float u, float vv) {
		int o = v * 5;
		data[o] = x;
		data[o + 1] = y;
		data[o + 2] = z;
		data[o + 3] = u;
		data[o + 4] = vv;
	}

	private static float minU(BakedQuad q) {
		float m = Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			m = Math.min(m, QuadClipper.getU(q, i));
		}
		return m;
	}

	private static float maxU(BakedQuad q) {
		float m = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			m = Math.max(m, QuadClipper.getU(q, i));
		}
		return m;
	}

	private static float minV(BakedQuad q) {
		float m = Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			m = Math.min(m, QuadClipper.getV(q, i));
		}
		return m;
	}

	private static float maxV(BakedQuad q) {
		float m = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			m = Math.max(m, QuadClipper.getV(q, i));
		}
		return m;
	}

	@Test
	void subdivide4_splitsIntoFourQuadrants() {
		BakedQuad base = makeQuad(0, 0, 1, 1);
		TextureAtlasSprite sprite = new TestSprite("test:block/glass");
		BakedQuad[] quads = QuadClipper.subdivide4(base, sprite);

		assertEquals(4, quads.length);
		// top-left (index 0): u in [0,0.5], v in [0,0.5]
		assertEquals(0f, minU(quads[0]), 0.001f);
		assertEquals(0.5f, maxU(quads[0]), 0.001f);
		assertEquals(0f, minV(quads[0]), 0.001f);
		assertEquals(0.5f, maxV(quads[0]), 0.001f);
		// top-right (index 1): u in [0.5,1], v in [0,0.5]
		assertEquals(0.5f, minU(quads[1]), 0.001f);
		assertEquals(1f, maxU(quads[1]), 0.001f);
		// bottom-left (index 2): v in [0.5,1]
		assertEquals(0.5f, minV(quads[2]), 0.001f);
		assertEquals(1f, maxV(quads[2]), 0.001f);
	}

	@Test
	void getQuadrant_matchesCtmLayout() {
		TextureAtlasSprite sprite = new TestSprite("test:block/glass");
		BakedQuad base = makeQuad(0, 0, 1, 1);
		BakedQuad[] quads = QuadClipper.subdivide4(base, sprite);
		// CTM quadrant: top-left=3, top-right=2, bottom-left=0, bottom-right=1
		assertEquals(3, QuadClipper.getQuadrant(quads[0], sprite));
		assertEquals(2, QuadClipper.getQuadrant(quads[1], sprite));
		assertEquals(0, QuadClipper.getQuadrant(quads[2], sprite));
		assertEquals(1, QuadClipper.getQuadrant(quads[3], sprite));
	}

	@Test
	void grow_expandsQuadrantUvToFullRange() {
		TextureAtlasSprite sprite = new TestSprite("test:block/glass");
		BakedQuad base = makeQuad(0, 0, 1, 1);
		BakedQuad[] quads = QuadClipper.subdivide4(base, sprite);
		BakedQuad grown = QuadClipper.grow(quads[1], sprite); // top-right
		assertEquals(0f, minU(grown), 0.001f);
		assertEquals(1f, maxU(grown), 0.001f);
		assertEquals(0f, minV(grown), 0.001f);
		assertEquals(1f, maxV(grown), 0.001f);
	}

	@Test
	void transformUvs_remapsQuadrantOntoFourByFourCell() {
		TextureAtlasSprite baseSprite = new TestSprite("test:block/glass");
		TextureAtlasSprite ctmSprite = new TestSprite("test:block/glass-ctm");
		BakedQuad base = makeQuad(0, 0, 1, 1);
		BakedQuad[] quads = QuadClipper.subdivide4(base, baseSprite);
		BakedQuad q = quads[0]; // top-left
		// Map onto cell 5 of a 4x4 grid: u in [4/16, 8/16], v in [4/16, 8/16]
		CtmSubmap cell5 = CtmSubmap.fromPixelScale(4, 4, 4, 4);
		BakedQuad remapped = QuadClipper.transformUVs(QuadClipper.grow(q, baseSprite), baseSprite, ctmSprite, cell5);
		assertEquals(0.25f, minU(remapped), 0.001f);
		assertEquals(0.5f, maxU(remapped), 0.001f);
		assertEquals(0.25f, minV(remapped), 0.001f);
		assertEquals(0.5f, maxV(remapped), 0.001f);
	}

	@Test
	void clip_fullQuadToCell_keepsGeometry() {
		TextureAtlasSprite sprite = new TestSprite("test:block/glass");
		BakedQuad base = makeQuad(0, 0, 1, 1);
		// clip the whole quad to cell 0 (top-left 4x4, UV [0,0.25]x[0,0.25]). The clip derives
		// geometry from the quad's own UV-to-world mapping, so both UV and geometry land in the
		// cell's quarter of the face.
		BakedQuad clipped = QuadClipper.clip(base, sprite, CtmSubmap.fromPixelScale(4, 4, 0, 0));
		assertEquals(0f, minU(clipped), 0.001f);
		assertEquals(0.25f, maxU(clipped), 0.001f);
		assertEquals(0f, minV(clipped), 0.001f);
		assertEquals(0.25f, maxV(clipped), 0.001f);
		// Geometry: the face spans x[0,1] and y[0,1] in the test quad's world, so the sub-rect is
		// x[0,0.25] and y[0,0.25].
		Vec3d[] pos = positions(clipped);
		assertTrue(pos[0].x >= -0.001f && pos[0].x <= 0.251f, "x0=" + pos[0].x);
		assertTrue(pos[1].x >= -0.001f && pos[1].x <= 0.251f, "x1=" + pos[1].x);
		assertTrue(pos[0].y >= -0.001f && pos[0].y <= 0.251f, "y0=" + pos[0].y);
		assertTrue(pos[1].y >= -0.001f && pos[1].y <= 0.251f, "y1=" + pos[1].y);
	}

	private static Vec3d[] positions(BakedQuad q) {
		Vec3d[] ret = new Vec3d[4];
		for (int i = 0; i < 4; i++) {
			ret[i] = new Vec3d(
					QuadClipper.positionComponent(q, i, 0),
					QuadClipper.positionComponent(q, i, 1),
					QuadClipper.positionComponent(q, i, 2));
		}
		return ret;
	}

	/** Minimal sprite with 0..1 UV range. */
	private static class TestSprite extends TextureAtlasSprite {
		TestSprite(String name) {
			super(name);
			this.width = 16;
			this.height = 16;
		}

		@Override
		public float getMinU() {
			return 0;
		}

		@Override
		public float getMaxU() {
			return 1;
		}

		@Override
		public float getMinV() {
			return 0;
		}

		@Override
		public float getMaxV() {
			return 1;
		}
	}
}

package me.pepperbell.continuity.client.ctm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import me.pepperbell.continuity.client.ctm.CtmCtmLogic;
import me.pepperbell.continuity.client.ctm.CtmDir;
import me.pepperbell.continuity.client.ctm.CtmConnectionMap;
import me.pepperbell.continuity.client.ctm.CtmConnectionPredicate;
import me.pepperbell.continuity.client.ctm.CtmQuadProcessor;
import org.junit.jupiter.api.Test;

/**
 * Verifies the CTM magic-number logic and per-type cell selection against the reference CTM
 * format semantics (transcribed from the CTM mod's {@code CTMLogic}).
 */
class CtmLogicTest {

	private static final CtmConnectionMap NO_CONNECT = new CtmConnectionMap(new CtmConnectionPredicate(false, false, true) {
		// always disconnected
	});

	private static int conn(CtmDir... dirs) {
		int c = 0;
		for (CtmDir d : dirs) {
			c |= 1 << d.ordinal();
		}
		return c;
	}

	@Test
	void ctmLogic_noConnections_usesBaseQuadrants() {
		int[] idx = CtmCtmLogic.getSubmapIndices(0, NO_CONNECT);
		assertArrayEquals(new int[]{18, 19, 17, 16}, idx);
	}

	@Test
	void ctmLogic_allConnections_usesCtmCells() {
		int[] idx = CtmCtmLogic.getSubmapIndices(0xFF, NO_CONNECT);
		// bottom-left=4, bottom-right=5, top-right=1, top-left=0
		assertArrayEquals(new int[]{4, 5, 1, 0}, idx);
	}

	@Test
	void ctmLogic_singleSideConnection_buildsInnerCorners() {
		// Connected to the right only (RIGHT dir). Bottom-left corner: BOTTOM+LEFT both unconnected
		// -> stays base (18). Bottom-right: BOTTOM unconnected, RIGHT connected -> offset 5 + 0 + 0? 
		// Reference: for idx1 (BOTTOM,RIGHT,BOTTOM_RIGHT), dirs[0]=BOTTOM(no), dirs[1]=RIGHT(yes)
		// -> submapOffsets[1]=5 + (BOTTOM?0) + (RIGHT?8) = 13. Top-right (idx2: TOP,RIGHT,TOP_RIGHT):
		// TOP(no), RIGHT(yes) -> 1 + 0 + 8 = 9. Top-left (idx3: TOP,LEFT,TOP_LEFT): TOP(no),LEFT(no) -> stays 16.
		int[] idx = CtmCtmLogic.getSubmapIndices(conn(CtmDir.RIGHT), NO_CONNECT);
		assertEquals(18, idx[0]); // bottom-left unchanged (base)
		assertEquals(13, idx[1]); // bottom-right
		assertEquals(9, idx[2]);  // top-right
		assertEquals(16, idx[3]); // top-left unchanged (base)
	}

	@Test
	void sctm_cells_matchReference() {
		CtmQuadProcessor processor = new CtmQuadProcessor(null, null);
		CtmSubmap[][] x2 = CtmSubmap.x2Grid();
		// no connections -> X2[0][0]
		assertEquals(x2[0][0], processor.getSctmCell(0));
		// top only -> vertical edge -> X2[0][0] (since left&right false)
		assertEquals(x2[0][0], processor.getSctmCell(conn(CtmDir.TOP)));
		// top and left -> vertical edge (top present, bottom absent) -> X2[0][0]
		assertEquals(x2[0][0], processor.getSctmCell(conn(CtmDir.TOP, CtmDir.LEFT)));
		// left and right (both horizontal) -> X2[0][1]
		assertEquals(x2[0][1], processor.getSctmCell(conn(CtmDir.LEFT, CtmDir.RIGHT)));
		// top and bottom (both vertical) -> X2[1][0]
		assertEquals(x2[1][0], processor.getSctmCell(conn(CtmDir.TOP, CtmDir.BOTTOM)));
	}

	@Test
	void plane_cells_matchReference() {
		CtmQuadProcessor processor = new CtmQuadProcessor(null, null);
		CtmSubmap[][] x2 = CtmSubmap.x2Grid();
		// vertical plane: no conn -> X2[0][0]
		assertEquals(x2[0][0], processor.getPlaneCell(0, true));
		// vertical: top only -> u=1, v=1 -> X2[1][1]
		assertEquals(x2[1][1], processor.getPlaneCell(conn(CtmDir.TOP), true));
		// vertical: bottom only -> u=1, v=0 -> X2[0][1]
		assertEquals(x2[0][1], processor.getPlaneCell(conn(CtmDir.BOTTOM), true));
		// vertical: both -> u=0, v=1 -> X2[1][0]
		assertEquals(x2[1][0], processor.getPlaneCell(conn(CtmDir.TOP, CtmDir.BOTTOM), true));
		// horizontal: left only -> u=1, v=1 -> X2[1][1]
		assertEquals(x2[1][1], processor.getPlaneCell(conn(CtmDir.LEFT), false));
		// horizontal: right only -> u=0, v=1 -> X2[1][0]
		assertEquals(x2[1][0], processor.getPlaneCell(conn(CtmDir.RIGHT), false));
	}
}

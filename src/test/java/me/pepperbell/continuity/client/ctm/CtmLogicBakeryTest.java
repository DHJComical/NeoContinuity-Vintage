package me.pepperbell.continuity.client.ctm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.pepperbell.continuity.client.ctm.CtmCustomLogic;
import me.pepperbell.continuity.client.ctm.CtmLogicBakery;
import me.pepperbell.continuity.client.ctm.CtmLogicDefinition;
import org.junit.jupiter.api.Test;

/**
 * Verifies the ctm_logic truth-table engine: baking, full coverage, multi-output states, and
 * error handling.
 */
class CtmLogicBakeryTest {

	// 2 directions (TOP, LEFT) -> 4 states, each with exactly one rule. Positions are assigned
	// bits last-to-first: TOP=1, LEFT=0.
	private static final String LOGIC_JSON = """
			{
			  "positions": [
			    {"id": "TOP", "directions": ["up"]},
			    {"id": "LEFT", "directions": ["west"]}
			  ],
			  "submaps": {
			    "": {"type": "grid", "width": 2, "height": 2}
			  },
			  "rules": [
			    {"output": "0,0", "connected": [], "unconnected": ["TOP", "LEFT"]},
			    {"output": "1,0", "connected": ["LEFT"], "unconnected": ["TOP"]},
			    {"output": "0,1", "connected": ["TOP"], "unconnected": ["LEFT"]},
			    {"output": "1,1", "connected": ["TOP", "LEFT"], "unconnected": []}
			  ]
			}
			""";

	private static CtmCustomLogic bake(String json) {
		JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
		return CtmLogicBakery.bake(CtmLogicDefinition.fromJson(obj));
	}

	@Test
	void bakesCompleteTruthTable() {
		CtmCustomLogic logic = bake(LOGIC_JSON);
		assertEquals(2, logic.inputCount());
		assertEquals(4, logic.outputCount());
		for (int state = 0; state < 4; state++) {
			assertEquals(1, logic.getOutputsForState(state).length, "state " + state);
		}
		// state 0 (nothing) -> output 0 ("0,0"); state 3 (TOP+LEFT) -> output 3 ("1,1")
		assertArrayEquals(new int[]{0}, logic.getOutputsForState(0));
		assertArrayEquals(new int[]{3}, logic.getOutputsForState(3));
	}

	@Test
	void multiRuleStateMergesConditions() {
		// Two rules target the same output; their conditions merge (AND). The merged rule requires
		// TOP && !LEFT, leaving state 0 (!TOP,!LEFT) uncovered -> bake throws.
		String json = """
				{
				  "positions": [{"id": "TOP", "directions": ["up"]}, {"id": "LEFT", "directions": ["west"]}],
				  "submaps": {"": {"type": "grid", "width": 2, "height": 1}},
				  "rules": [
				    {"output": "0,0", "connected": [], "unconnected": ["TOP", "LEFT"]},
				    {"output": "0,0", "connected": ["TOP"], "unconnected": ["LEFT"]},
				    {"output": "1,0", "connected": ["TOP", "LEFT"], "unconnected": []}
				  ]
				}
				""";
		// state 0 uncovered -> merge makes truth table incomplete
		assertThrows(IllegalStateException.class, () -> bake(json));
	}

	@Test
	void unknownOutputFails() {
		String bad = """
				{
				  "positions": [{"id": "TOP", "directions": ["up"]}],
				  "submaps": {"": {"type": "grid", "width": 1, "height": 1}},
				  "rules": [{"output": "nope", "connected": []}]
				}
				""";
		assertThrows(IllegalArgumentException.class, () -> bake(bad));
	}

	@Test
	void incompleteTruthTableFails() {
		String bad = """
				{
				  "positions": [{"id": "TOP", "directions": ["up"]}],
				  "submaps": {"": {"type": "grid", "width": 1, "height": 1}},
				  "rules": [{"output": "0,0", "connected": [], "unconnected": ["TOP"]}]
				}
				""";
		assertThrows(IllegalStateException.class, () -> bake(bad));
	}
}

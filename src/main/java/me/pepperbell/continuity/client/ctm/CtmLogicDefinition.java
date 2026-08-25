package me.pepperbell.continuity.client.ctm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.EnumFacing;

/**
 * A parsed {@code ctm_logic/<name>.json} definition. Mirrors the CTM Mod format's truth-table
 * logic definition: positions (connection inputs), submaps (output grids/singles), optional faces
 * (geometry sub-regions) and rules (connection conditions mapped to outputs).
 */
public final class CtmLogicDefinition {
	public final List<Position> positions;
	public final Map<String, MultiSubmap> submaps;
	public final Map<String, MultiSubmap> faces;
	public final List<Rule> rules;

	public CtmLogicDefinition(List<Position> positions, Map<String, MultiSubmap> submaps, Map<String, MultiSubmap> faces, List<Rule> rules) {
		this.positions = positions;
		this.submaps = submaps;
		this.faces = faces;
		this.rules = rules;
	}

	public static CtmLogicDefinition fromJson(JsonObject json) {
		List<Position> positions = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("positions")) {
			positions.add(Position.fromJson(element.getAsJsonObject()));
		}

		Map<String, MultiSubmap> submaps = new HashMap<>();
		JsonObject submapsObj = json.getAsJsonObject("submaps");
		for (Map.Entry<String, JsonElement> entry : submapsObj.entrySet()) {
			submaps.put(entry.getKey(), MultiSubmap.fromJson(entry.getValue().getAsJsonObject()));
		}

		Map<String, MultiSubmap> faces = new HashMap<>();
		if (json.has("faces")) {
			JsonObject facesObj = json.getAsJsonObject("faces");
			for (Map.Entry<String, JsonElement> entry : facesObj.entrySet()) {
				faces.put(entry.getKey(), MultiSubmap.fromJson(entry.getValue().getAsJsonObject()));
			}
		}

		List<Rule> rules = new ArrayList<>();
		for (JsonElement element : json.getAsJsonArray("rules")) {
			rules.add(Rule.fromJson(element.getAsJsonObject()));
		}

		return new CtmLogicDefinition(positions, submaps, faces, rules);
	}

	public record Position(String id, List<EnumFacing> directions) {
		public static Position fromJson(JsonObject json) {
			String id = json.get("id").getAsString();
			List<EnumFacing> directions = new ArrayList<>();
			for (JsonElement element : json.getAsJsonArray("directions")) {
				directions.add(EnumFacing.valueOf(element.getAsString().toUpperCase()));
			}
			return new Position(id, directions);
		}
	}

	public record Rule(String output, int from, Optional<String> at, List<String> connected, List<String> unconnected) {
		public static Rule fromJson(JsonObject json) {
			String output = json.get("output").getAsString();
			int from = json.has("from") ? json.get("from").getAsInt() : 0;
			Optional<String> at = json.has("at") ? Optional.of(json.get("at").getAsString()) : Optional.empty();
			List<String> connected = new ArrayList<>();
			if (json.has("connected")) {
				for (JsonElement element : json.getAsJsonArray("connected")) {
					connected.add(element.getAsString());
				}
			}
			List<String> unconnected = new ArrayList<>();
			if (json.has("unconnected")) {
				for (JsonElement element : json.getAsJsonArray("unconnected")) {
					unconnected.add(element.getAsString());
				}
			}
			return new Rule(output, from, at, connected, unconnected);
		}
	}

	/** A submap definition that can expand to a list of named submaps. */
	public abstract static class MultiSubmap {
		public static MultiSubmap fromJson(JsonObject json) {
			if (json.has("width") && json.has("height")) {
				float width = json.get("width").getAsFloat();
				float height = json.get("height").getAsFloat();
				if (width == Math.floor(width) && height == Math.floor(height)) {
					return new Grid((int) width, (int) height);
				}
				float offsetX = json.has("offsetX") ? json.get("offsetX").getAsFloat() : 0;
				float offsetY = json.has("offsetY") ? json.get("offsetY").getAsFloat() : 0;
				return new Single(width, height, offsetX, offsetY);
			}
			throw new IllegalArgumentException("submap must have width and height");
		}

		public abstract Iterable<NamedSubmap> forName(String baseName);
	}

	public record NamedSubmap(String name, CtmSubmap submap) {
	}

	public static class Single extends MultiSubmap {
		private final CtmSubmap submap;

		public Single(float width, float height, float offsetX, float offsetY) {
			// width/height/offsets are in 0..1 unit scale
			this.submap = CtmSubmap.fromUnitScale(width, height, offsetX, offsetY);
		}

		@Override
		public Iterable<NamedSubmap> forName(String baseName) {
			return List.of(new NamedSubmap(baseName, submap));
		}
	}

	public static class Grid extends MultiSubmap {
		private final List<NamedSubmap> submaps = new ArrayList<>();
		public final int width;
		public final int height;

		public Grid(int width, int height) {
			this.width = width;
			this.height = height;
			CtmSubmap[][] grid = CtmSubmap.grid(width, height);
			for (int y = 0; y < grid.length; y++) {
				for (int x = 0; x < grid[y].length; x++) {
					submaps.add(new NamedSubmap(x + "," + y, grid[y][x]));
				}
			}
		}

		@Override
		public Iterable<NamedSubmap> forName(String baseName) {
			List<NamedSubmap> ret = new ArrayList<>(submaps.size());
			for (NamedSubmap s : submaps) {
				ret.add(new NamedSubmap(baseName + s.name(), s.submap()));
			}
			return ret;
		}
	}
}

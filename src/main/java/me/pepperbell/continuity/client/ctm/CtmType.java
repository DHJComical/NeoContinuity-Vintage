package me.pepperbell.continuity.client.ctm;

import java.util.Locale;

/**
 * The connected-texture types of the CTM Mod format, keyed by the {@code "type"} string used in
 * a texture's {@code .png.mcmeta} {@code "ctm"} section.
 */
public enum CtmType {
	CTM("ctm"),
	SCTM("sctm", "ctm_simple"),
	PILLAR("pillar", "ctmv"),
	HORIZONTAL("ctmh", "ctm_horizontal"),
	VERTICAL("ctm_vertical"),
	RANDOM("random", "r"),
	PATTERN("pattern", "v"),
	EDGES("edges"),
	EDGES_FULL("edges_full"),
	ELDRITCH("eldritch"),
	NORMAL("normal");

	private final String[] ids;

	CtmType(String... ids) {
		this.ids = ids;
	}

	public String primaryId() {
		return ids[0];
	}

	public static CtmType fromId(String id) {
		if (id == null) {
			return NORMAL;
		}
		String lower = id.toLowerCase(Locale.ROOT);
		for (CtmType type : values()) {
			for (String candidate : type.ids) {
				if (candidate.equals(lower)) {
					return type;
				}
			}
		}
		return null;
	}
}

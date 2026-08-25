package me.pepperbell.continuity.client.ctm;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.pepperbell.continuity.client.ContinuityClient;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * Parses the {@code "ctm"} section of a texture's {@code .png.mcmeta} into a
 * {@link CtmDefinition}. Mirrors the CTM Mod format's v1 metadata (and the namespaced custom-logic
 * type reference) semantics.
 */
public final class CtmMcmetaParser {
	public static final String SECTION_NAME = "ctm";

	private static final Gson GSON = new Gson();

	private CtmMcmetaParser() {
	}

	/**
	 * Reads the mcmeta of the given texture resource and parses its {@code "ctm"} section.
	 *
	 * @return the parsed properties, or {@code null} if the resource has no ctm section
	 */
	@Nullable
	public static CtmDefinition parse(ResourceLocation baseTextureId, IResource resource, String packId, int packPriority) {
		try {
			JsonObject mcmeta = readMcmeta(resource);
			if (mcmeta == null) {
				return null;
			}
			JsonElement ctmElement = mcmeta.get(SECTION_NAME);
			if (ctmElement == null || !ctmElement.isJsonObject()) {
				return null;
			}
			JsonObject ctm = ctmElement.getAsJsonObject();
			return parse(baseTextureId, ctm, packId, packPriority);
		} catch (JsonParseException | IOException e) {
			ContinuityClient.LOGGER.error("Failed to parse CTM metadata of texture '" + baseTextureId + "' in pack '" + packId + "'", e);
			return null;
		}
	}

	@Nullable
	private static JsonObject readMcmeta(IResource resource) throws IOException {
		// Re-read the stream (a resource's getInputStream may only be usable once in some pack impls)
		try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IllegalStateException | JsonParseException e) {
			ContinuityClient.LOGGER.debug("Invalid mcmeta for resource '" + resource.getResourceLocation() + "'", e);
			return null;
		}
	}

	/**
	 * Parses a ctm section JSON object into properties.
	 *
	 * @param baseTextureId the texture whose mcmeta this is (sprite index 0)
	 */
	public static CtmDefinition parse(ResourceLocation baseTextureId, JsonObject ctm, String packId, int packPriority) {
		CtmDefinition properties = new CtmDefinition(baseTextureId, packId, packPriority);

		Integer version = getVersion(ctm);
		if (version != null && version != 1) {
			ContinuityClient.LOGGER.warn("Unsupported ctm_version " + version + " in mcmeta of '" + baseTextureId + "'; treating as plain texture");
			return null;
		}

		if (ctm.has("proxy")) {
			JsonElement proxyEle = ctm.get("proxy");
			if (proxyEle.isJsonPrimitive() && proxyEle.getAsJsonPrimitive().isString()) {
				properties.proxy = proxyEle.getAsString();
			}
			// Proxy may not combine with other fields (per format spec); ignore extras beyond version.
			finalizeSprites(properties);
			return properties;
		}

		if (ctm.has("type")) {
			JsonElement typeEle = ctm.get("type");
			if (typeEle.isJsonPrimitive() && typeEle.getAsJsonPrimitive().isString()) {
				String typeStr = typeEle.getAsString();
				CtmType type = CtmType.fromId(typeStr);
				if (type != null) {
					properties.type = type;
				} else {
					// Namespaced type: custom logic reference (e.g. "ctm:optifine_full_3tile")
					CtmCustomLogic logic = CtmDefinitionManager.getLogic(typeStr);
					if (logic != null) {
						properties.logic = logic;
					} else {
						ContinuityClient.LOGGER.warn("Unknown CTM type '" + typeStr + "' in mcmeta of '" + baseTextureId + "'");
						return null;
					}
				}
			}
		}

		if (ctm.has("layer")) {
			JsonElement layerEle = ctm.get("layer");
			if (layerEle.isJsonPrimitive() && layerEle.getAsJsonPrimitive().isString()) {
				try {
					properties.layer = BlockRenderLayer.valueOf(layerEle.getAsString());
				} catch (IllegalArgumentException e) {
					ContinuityClient.LOGGER.warn("Invalid CTM layer '" + layerEle.getAsString() + "' in mcmeta of '" + baseTextureId + "'");
				}
			}
		}

		if (ctm.has("textures")) {
			JsonElement texturesEle = ctm.get("textures");
			if (texturesEle.isJsonArray()) {
				List<ResourceLocation> additional = new ArrayList<>();
				for (JsonElement e : texturesEle.getAsJsonArray()) {
					if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
						additional.add(new ResourceLocation(e.getAsString()));
					}
				}
				properties.additionalTextures = additional.toArray(new ResourceLocation[0]);
			}
		}

		if (ctm.has("extra") && ctm.get("extra").isJsonObject()) {
			JsonObject extra = ctm.getAsJsonObject("extra");
			properties.extraData = extra;
			properties.ignoreStates = JsonUtils.getBoolean(extra, "ignore_states", false);
			properties.useActualState = JsonUtils.getBoolean(extra, "use_actual_state", false);
			if (extra.has("connect_inside")) {
				properties.connectInside = JsonUtils.getBoolean(extra, "connect_inside", false);
			}
			properties.connectToDefined = extra.has("connect_to");
			parseLight(properties, extra);
			parseMap(properties, extra);
		}

		finalizeSprites(properties);
		return properties;
	}

	private static void parseLight(CtmDefinition properties, JsonObject extra) {
		JsonElement light = extra.get("light");
		if (light == null) {
			return;
		}
		if (light.isJsonPrimitive()) {
			properties.hasLight = true;
			int value = parseLightValue(light);
			properties.blocklight = value;
			properties.skylight = value;
		} else if (light.isJsonObject()) {
			properties.hasLight = true;
			JsonObject lightObj = light.getAsJsonObject();
			properties.blocklight = parseLightValue(lightObj.get("block"));
			properties.skylight = parseLightValue(lightObj.get("sky"));
		}
	}

	private static void parseMap(CtmDefinition properties, JsonObject extra) {
		if (extra.has("width") && extra.has("height")) {
			properties.mapWidth = Math.max(1, JsonUtils.getInt(extra, "width", 2));
			properties.mapHeight = Math.max(1, JsonUtils.getInt(extra, "height", 2));
		} else if (extra.has("size")) {
			int size = Math.max(1, JsonUtils.getInt(extra, "size", 2));
			properties.mapWidth = size;
			properties.mapHeight = size;
		}
		properties.mapXOffset = JsonUtils.getInt(extra, "x_offset", 0);
		properties.mapYOffset = JsonUtils.getInt(extra, "y_offset", 0);
	}

	private static int parseLightValue(@Nullable JsonElement data) {
		if (data != null && data.isJsonPrimitive() && data.getAsJsonPrimitive().isNumber()) {
			return MathHelper.clamp(data.getAsInt(), 0, 15);
		}
		return 0;
	}

	private static void finalizeSprites(CtmDefinition properties) {
		List<ResourceLocation> spriteIds = new ObjectArrayList<>(properties.additionalTextures.length + 1);
		spriteIds.add(properties.resourceId);
		Collections.addAll(spriteIds, properties.additionalTextures);
		properties.spriteIds = spriteIds;
		Set<ResourceLocation> dependencies = new ObjectOpenHashSet<>();
		dependencies.addAll(spriteIds);
		if (properties.proxy != null) {
			dependencies.add(new ResourceLocation(properties.proxy));
		}
		properties.spriteDependencies = dependencies;
	}

	@Nullable
	private static Integer getVersion(JsonObject ctm) {
		JsonElement version = ctm.get("ctm_version");
		if (version != null && version.isJsonPrimitive() && version.getAsJsonPrimitive().isNumber()) {
			return version.getAsInt();
		}
		return null;
	}

	/**
	 * For proxy resolution: replaces the sprite-0 (base) of a proxy-target definition with the
	 * proxy target itself. The definition's {@code resourceId} (used for sprite matching) is
	 * unchanged (still the actual base texture), but rendering uses the proxy target's sprite.
	 */
	public static void overrideBaseTexture(CtmDefinition definition, ResourceLocation proxyTarget) {
		if (definition.spriteIds == null || definition.spriteIds.isEmpty()) {
			return;
		}
		definition.spriteIds = new ObjectArrayList<>(definition.spriteIds);
		definition.spriteIds.set(0, proxyTarget);
		definition.spriteDependencies = new ObjectOpenHashSet<>(definition.spriteIds);
	}
}

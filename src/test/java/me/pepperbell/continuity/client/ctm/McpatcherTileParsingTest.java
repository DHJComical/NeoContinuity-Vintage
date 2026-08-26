package me.pepperbell.continuity.client.ctm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import me.pepperbell.continuity.client.properties.BaseCtmProperties;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Verifies that MCPatcher {@code mcpatcher/ctm/} properties resolve numeric {@code tiles}
 * relative to the properties directory (both simple lists and ranges).
 */
class McpatcherTileParsingTest {

	private static final String PACK = "test";

	static class DummyPack implements IResourcePack {
		@Override
		public InputStream getInputStream(ResourceLocation location) {
			return null;
		}

		@Override
		public boolean resourceExists(ResourceLocation location) {
			return false;
		}

		@Override
		public Set<String> getResourceDomains() {
			return Set.of();
		}

		@Override
		public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) {
			return null;
		}

		@Override
		public BufferedImage getPackImage() {
			return null;
		}

		@Override
		public String getPackName() {
			return PACK;
		}
	}

	static class TestProps extends BaseCtmProperties {
		TestProps(Properties properties, ResourceLocation resourceId) {
			super(properties, resourceId, new DummyPack(), 0, null, "random");
		}

		void parse() {
			parseTiles();
		}

		List<ResourceLocation> tiles() {
			return getSpriteIds();
		}
	}

	private static List<ResourceLocation> parseTiles(String tilesValue, String path) {
		Properties props = new Properties();
		props.setProperty("tiles", tilesValue);
		props.setProperty("method", "random");
		TestProps test = new TestProps(props, new ResourceLocation("minecraft", path));
		test.parse();
		return test.tiles();
	}

	@Test
	void simpleNumericTilesResolveUnderMcpatcherDir() {
		List<ResourceLocation> tiles = parseTiles("1 10", "mcpatcher/ctm/cobblestone/default/cobblestone.properties");
		assertEquals(2, tiles.size());
		assertEquals("continuity_reserved/mcpatcher/ctm/cobblestone/default/1", tiles.get(0).getPath());
		assertEquals("continuity_reserved/mcpatcher/ctm/cobblestone/default/10", tiles.get(1).getPath());
		assertEquals("minecraft", tiles.get(0).getNamespace());
	}

	@Test
	void rangeTilesExpand() {
		List<ResourceLocation> tiles = parseTiles("10-18", "mcpatcher/ctm/cobblestone/default/2.properties");
		assertEquals(9, tiles.size());
		assertEquals("continuity_reserved/mcpatcher/ctm/cobblestone/default/10", tiles.get(0).getPath());
		assertEquals("continuity_reserved/mcpatcher/ctm/cobblestone/default/18", tiles.get(8).getPath());
	}

	@Test
	void neverEmptyForValidNumericTiles() {
		for (String tilesValue : new String[]{"1 10", "1-36", "<default> 2-4", "1 <default> 3-12", "1 17"}) {
			List<ResourceLocation> tiles = parseTiles(tilesValue, "mcpatcher/ctm/stone/default/stone1.properties");
			assertTrue(tiles.size() > 0, "tiles=[" + tilesValue + "] should resolve at least one sprite, got " + tiles);
		}
	}
}
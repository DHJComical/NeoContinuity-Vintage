package me.pepperbell.continuity.client.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import me.pepperbell.continuity.client.resource.ResourceRedirectHandler;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

/**
 * Verifies the sprite redirect mapping used for both OptiFine (optifine/) and MCPatcher
 * (mcpatcher/) CTM packs: {@code continuity_reserved/} prefixed sprite ids are mapped back to
 * their real pack path so they load from the resource pack.
 */
class ResourceRedirectHandlerTest {

	@Test
	void optifinePathsRedirectBack() {
		ResourceLocation id = new ResourceLocation("minecraft", "textures/continuity_reserved/optifine/ctm/default/glass/1");
		ResourceLocation redirected = ResourceRedirectHandler.redirect(id);
		assertEquals("optifine/ctm/default/glass/1", redirected.getPath());
		assertEquals("minecraft", redirected.getNamespace());
	}

	@Test
	void mcpatcherPathsRedirectBack() {
		ResourceLocation id = new ResourceLocation("minecraft", "textures/continuity_reserved/mcpatcher/ctm/cobblestone/default/1");
		ResourceLocation redirected = ResourceRedirectHandler.redirect(id);
		assertEquals("mcpatcher/ctm/cobblestone/default/1", redirected.getPath());
	}

	@Test
	void nonRedirectPathsUnchanged() {
		ResourceLocation plain = new ResourceLocation("minecraft", "textures/block/cobblestone");
		assertEquals(plain, ResourceRedirectHandler.redirect(plain));
	}

	@Test
	void unprefixedReservedPathGetsOptifinePrefix() {
		// OptiFine packs strip "optifine/" in parseTiles, so a reserved path without a known
		// prefix is re-prefixed with optifine/ to restore the pre-mcpatcher behavior.
		ResourceLocation id = new ResourceLocation("minecraft", "textures/continuity_reserved/ctm/default/glass/blue/42");
		ResourceLocation redirected = ResourceRedirectHandler.redirect(id);
		assertEquals("optifine/ctm/default/glass/blue/42", redirected.getPath());
		assertEquals("minecraft", redirected.getNamespace());
	}
}

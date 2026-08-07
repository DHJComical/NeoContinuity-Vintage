package me.pepperbell.continuity.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BakedQuadRetextured;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;

public class EmissiveBakedQuad extends BakedQuadRetextured {
	private static final int FULL_BRIGHT_LIGHTMAP = 0x00F000F0;

	public EmissiveBakedQuad(BakedQuad quad, TextureAtlasSprite emissiveSprite) {
		super(quad, emissiveSprite);
		VertexFormat format = getFormat();
		if (format.hasUvOffset(1)) {
			int uvIndex = format.getUvOffsetById(1) / 4;
			int vertexSize = format.getIntegerSize();
			for (int i = 0; i < 4; i++) {
				vertexData[i * vertexSize + uvIndex] = FULL_BRIGHT_LIGHTMAP;
			}
		}
	}
}

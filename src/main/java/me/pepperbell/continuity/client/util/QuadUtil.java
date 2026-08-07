package me.pepperbell.continuity.client.util;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;

public final class QuadUtil {
	private QuadUtil() {
	}

	public static float getU(BakedQuad quad, int vertex) {
		VertexFormat format = quad.getFormat();
		if (!format.hasUvOffset(0)) {
			return 0;
		}
		int index = format.getUvOffsetById(0) / 4 + vertex * format.getIntegerSize();
		return Float.intBitsToFloat(quad.getVertexData()[index]);
	}

	public static float getV(BakedQuad quad, int vertex) {
		VertexFormat format = quad.getFormat();
		if (!format.hasUvOffset(0)) {
			return 0;
		}
		int index = format.getUvOffsetById(0) / 4 + vertex * format.getIntegerSize() + 1;
		return Float.intBitsToFloat(quad.getVertexData()[index]);
	}

	public static float positionComponent(BakedQuad quad, int vertex, int axis) {
		VertexFormat format = quad.getFormat();
		int index = format.getOffset(0) / 4 + vertex * format.getIntegerSize() + axis;
		return Float.intBitsToFloat(quad.getVertexData()[index]);
	}

	public static int getTextureOrientation(BakedQuad quad) {
		EnumFacing face = quad.getFace();
		if (face == null) {
			return 0;
		}

		float tm00 = getU(quad, 3) - getU(quad, 1);
		float tm01 = getV(quad, 3) - getV(quad, 1);
		float tm10 = getU(quad, 2) - getU(quad, 0);
		float tm11 = getV(quad, 2) - getV(quad, 0);
		float determinant = tm00 * tm11 - tm10 * tm01;
		if (determinant == 0) {
			return 0;
		}
		float s = 1 / determinant;
		float itm10 = -tm10 * s;
		float itm11 = tm00 * s;

		int xAxis;
		int xAxisSign;
		int yAxis;
		int yAxisSign;
		switch (face) {
			case DOWN -> {
				xAxis = 0;
				xAxisSign = 1;
				yAxis = 2;
				yAxisSign = 1;
			}
			case UP -> {
				xAxis = 0;
				xAxisSign = 1;
				yAxis = 2;
				yAxisSign = -1;
			}
			case NORTH -> {
				xAxis = 0;
				xAxisSign = -1;
				yAxis = 1;
				yAxisSign = 1;
			}
			case SOUTH -> {
				xAxis = 0;
				xAxisSign = 1;
				yAxis = 1;
				yAxisSign = 1;
			}
			case WEST -> {
				xAxis = 2;
				xAxisSign = 1;
				yAxis = 1;
				yAxisSign = 1;
			}
			case EAST -> {
				xAxis = 2;
				xAxisSign = -1;
				yAxis = 1;
				yAxisSign = 1;
			}
			default -> {
				return 0;
			}
		}

		float pm00 = positionComponent(quad, 3, xAxis) - positionComponent(quad, 1, xAxis);
		float pm01 = positionComponent(quad, 3, yAxis) - positionComponent(quad, 1, yAxis);
		float pm10 = positionComponent(quad, 2, xAxis) - positionComponent(quad, 0, xAxis);
		float pm11 = positionComponent(quad, 2, yAxis) - positionComponent(quad, 0, yAxis);

		float x = -(pm00 * itm10 + pm10 * itm11) * xAxisSign;
		float y = -(pm01 * itm10 + pm11 * itm11) * yAxisSign;

		return (Math.abs(y) >= Math.abs(x) ? (y > 0 ? 0 : 2) : (x > 0 ? 3 : 1)) + (determinant < 0 ? 4 : 0);
	}
}

package me.pepperbell.continuity.client.ctm;

/**
 * A rectangular region of a texture, expressed in "16th pixel" units of the sprite's UV space.
 * <p>
 * This mirrors the submap convention of the CTM Mod format: all coordinates are measured in
 * sixteenths of a 16x16 texture sheet (e.g. a 4x4 cell of a CTM sheet is width=4, height=4),
 * and are converted to 0..1 UV space by dividing by 16 at use time.
 */
public final class CtmSubmap {
	public static final float UNITS_PER_PIXEL = 1f / 16f;

	public static final CtmSubmap X1 = fromPixelScale(16, 16, 0, 0);

	public static final CtmSubmap X2 = fromPixelScale(8, 8, 8, 8);

	public final float width;
	public final float height;
	public final float xOffset;
	public final float yOffset;

	public CtmSubmap(float width, float height, float xOffset, float yOffset) {
		this.width = width;
		this.height = height;
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	/**
	 * Creates a submap measured in pixel units of a 16x16 sheet. The resulting offsets/widths are
	 * stored in 16th-units (i.e. they are the raw pixel values).
	 */
	public static CtmSubmap fromPixelScale(float width, float height, float xOffset, float yOffset) {
		return new CtmSubmap(width, height, xOffset, yOffset);
	}

	/**
	 * Creates a submap measured in 0..1 UV units.
	 */
	public static CtmSubmap fromUnitScale(float width, float height, float xOffset, float yOffset) {
		return new CtmSubmap(width * 16f, height * 16f, xOffset * 16f, yOffset * 16f);
	}

	/**
	 * The 2x2 quarter grid of a 16x16 sheet. Rows top-to-bottom, columns left-to-right.
	 */
	public static CtmSubmap[][] x2Grid() {
		return new CtmSubmap[][] {
				{ fromPixelScale(8, 8, 0, 0), fromPixelScale(8, 8, 8, 0) },
				{ fromPixelScale(8, 8, 0, 8), fromPixelScale(8, 8, 8, 8) }
		};
	}

	/**
	 * The 3x3 grid of a 16x16 sheet.
	 */
	public static CtmSubmap[][] x3Grid() {
		float div = 16f / 3f;
		CtmSubmap[][] ret = new CtmSubmap[3][3];
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				ret[y][x] = fromPixelScale(div, div, div * x, div * y);
			}
		}
		return ret;
	}

	/**
	 * The 4x4 grid of a 16x16 sheet.
	 */
	public static CtmSubmap[][] x4Grid() {
		CtmSubmap[][] ret = new CtmSubmap[4][4];
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				ret[y][x] = fromPixelScale(4, 4, 4 * x, 4 * y);
			}
		}
		return ret;
	}

	/**
	 * A width x height grid of a 16x16 sheet.
	 */
	public static CtmSubmap[][] grid(int width, int height) {
		float xDiv = 16f / width;
		float yDiv = 16f / height;
		CtmSubmap[][] ret = new CtmSubmap[height][width];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				ret[y][x] = fromPixelScale(xDiv, yDiv, xDiv * x, yDiv * y);
			}
		}
		return ret;
	}

	/** The minimum U of this submap in 0..1 UV space. */
	public float getMinU() {
		return xOffset * UNITS_PER_PIXEL;
	}

	/** The maximum U of this submap in 0..1 UV space. */
	public float getMaxU() {
		return (xOffset + width) * UNITS_PER_PIXEL;
	}

	/** The minimum V of this submap in 0..1 UV space. */
	public float getMinV() {
		return yOffset * UNITS_PER_PIXEL;
	}

	/** The maximum V of this submap in 0..1 UV space. */
	public float getMaxV() {
		return (yOffset + height) * UNITS_PER_PIXEL;
	}

	public CtmSubmap flipX() {
		return fromPixelScale(width, height, 16f - xOffset - width, yOffset);
	}

	public CtmSubmap flipY() {
		return fromPixelScale(width, height, xOffset, 16f - yOffset - height);
	}

	@Override
	public String toString() {
		return "CtmSubmap[w=" + width + ", h=" + height + ", x=" + xOffset + ", y=" + yOffset + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CtmSubmap that)) {
			return false;
		}
		return Float.compare(that.width, width) == 0
				&& Float.compare(that.height, height) == 0
				&& Float.compare(that.xOffset, xOffset) == 0
				&& Float.compare(that.yOffset, yOffset) == 0;
	}

	@Override
	public int hashCode() {
		int result = Float.floatToIntBits(width);
		result = 31 * result + Float.floatToIntBits(height);
		result = 31 * result + Float.floatToIntBits(xOffset);
		result = 31 * result + Float.floatToIntBits(yOffset);
		return result;
	}
}

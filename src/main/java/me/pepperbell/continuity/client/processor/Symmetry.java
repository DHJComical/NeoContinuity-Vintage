package me.pepperbell.continuity.client.processor;

import net.minecraft.core.Direction;

public enum Symmetry {
	NONE,
	OPPOSITE,
	ALL;

	public Direction apply(Direction face) {
		if (this == Symmetry.OPPOSITE) {
			if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
				face = face.getOpposite();
			}
		} else if (this == Symmetry.ALL) {
			face = Direction.DOWN;
		}
		return face;
	}
}

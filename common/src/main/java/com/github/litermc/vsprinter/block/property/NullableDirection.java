package com.github.litermc.vsprinter.block.property;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum NullableDirection implements StringRepresentable {
	NULL(null),
	DOWN(Direction.DOWN),
	UP(Direction.UP),
	NORTH(Direction.NORTH),
	SOUTH(Direction.SOUTH),
	WEST(Direction.WEST),
	EAST(Direction.EAST);

	private final Direction dir;

	private NullableDirection(final Direction dir) {
		this.dir = dir;
	}

	public Direction asDirection() {
		return this.dir;
	}

	public static NullableDirection fromDirection(final Direction dir) {
		return switch (dir) {
			case DOWN -> DOWN;
			case UP -> UP;
			case NORTH -> NORTH;
			case SOUTH -> SOUTH;
			case WEST -> WEST;
			case EAST -> EAST;
			default -> null;
		};
	}

	@Override
	public String getSerializedName() {
		return this.dir == null ? "null" : this.dir.getSerializedName();
	}
}

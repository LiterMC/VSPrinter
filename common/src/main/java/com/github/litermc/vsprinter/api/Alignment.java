package com.github.litermc.vsprinter.api;

public enum Alignment {
	NEGATIVE,
	CENTER,
	POSITIVE;

	private static final Alignment[] VALUES = values();

	public static Alignment fromOrdinal(byte index) {
		if (index < 0 || index >= VALUES.length) {
			return CENTER;
		}
		return VALUES[index];
	}

	public double align(final double min, final double max, final double width) {
		if (max < min) {
			throw new IllegalArgumentException("max is less than min");
		}
		if (width < 0) {
			return switch (this) {
			case NEGATIVE -> min + width;
			case CENTER -> (min + max - width) / 2;
			case POSITIVE -> max;
			};
		}
		return switch (this) {
		case NEGATIVE -> min;
		case CENTER -> (min + max - width) / 2;
		case POSITIVE -> max - width;
		};
	}
}

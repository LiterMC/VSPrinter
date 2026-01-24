package com.github.litermc.vsprinter.api;

public enum Alignment {
	NEGATIVE(-1),
	CENTER(0),
	POSITIVE(1);

	private static final Alignment[] VALUES = values();
	private final int representative;

	private Alignment(final int representative)	{
		this.representative = representative;
	}

	public int getRepresentative() {
		return this.representative;
	}

	public static Alignment fromOrdinal(final byte index) {
		if (index < 0 || index >= VALUES.length) {
			return CENTER;
		}
		return VALUES[index];
	}

	public static Alignment fromRepresentative(final int representative) {
		if (representative < 0) {
			return NEGATIVE;
		}
		if (representative == 0) {
			return CENTER;
		}
		return POSITIVE;
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

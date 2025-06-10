package com.github.litermc.vsprinter.api;

import net.minecraft.nbt.CompoundTag;

public record PrintArguments(
	boolean skipFluids,
	boolean skipMissing,
	float scale,
	Alignment xAlign,
	Alignment yAlign,
	Alignment zAlign,
	// will multiple 90°
	int xRotate,
	// will multiple 90°
	int yRotate,
	boolean isValidated
) {
	public static final PrintArguments DEFAULT = new PrintArguments(
		false, false,
		0.25f,
		Alignment.CENTER, Alignment.CENTER, Alignment.CENTER,
		0, 0,
		true
	);

	public PrintArguments validated() {
		if (this.isValidated) {
			return this;
		}
		float scale = this.scale;
		if (scale < 0.1f) {
			scale = 0.1f;
		} else if (!(scale < 1.0f)) { // handle NaN case as well
			scale = 1.0f;
		}
		return new PrintArguments(
			this.skipFluids, this.skipMissing,
			scale,
			this.xAlign, this.yAlign, this.zAlign,
			((this.xRotate % 4) + 4) % 4,
			((this.yRotate % 4) + 4) % 4,
			true
		);
	}

	public static PrintArguments readFromNbt(final CompoundTag data) {
		final boolean skipFluids = data.getBoolean("SkipFluids");
		final boolean skipMissing = data.getBoolean("SkipMissing");
		final float scale = data.getFloat("Scale");
		final Alignment xAlign = Alignment.fromOrdinal(data.getByte("XAlign"));
		final Alignment yAlign = Alignment.fromOrdinal(data.getByte("YAlign"));
		final Alignment zAlign = Alignment.fromOrdinal(data.getByte("ZAlign"));
		final int xRotate = data.getByte("XRotate");
		final int yRotate = data.getByte("YRotate");
		return new PrintArguments(skipFluids, skipMissing, scale, xAlign, yAlign, zAlign, xRotate, yRotate, false);
	}

	public CompoundTag writeToNbt(final CompoundTag data) {
		data.putBoolean("SkipFluids", this.skipFluids);
		data.putBoolean("SkipMissing", this.skipMissing);
		data.putFloat("Scale", this.scale);
		data.putByte("XAlign", (byte) (this.xAlign.ordinal()));
		data.putByte("YAlign", (byte) (this.yAlign.ordinal()));
		data.putByte("ZAlign", (byte) (this.zAlign.ordinal()));
		data.putByte("XRotate", (byte) (this.xRotate));
		data.putByte("YRotate", (byte) (this.yRotate));
		return data;
	}
}

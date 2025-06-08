package com.github.litermc.vsprinter.api;

import net.minecraft.nbt.CompoundTag;

public record PrintArguments(
	String blueprint,
	boolean skipFluids,
	boolean skipMissing,
	float scale,
	Alignment xAlign,
	Alignment yAlign,
	Alignment zAlign,
	// must be multiple of 90°
	float xRotate,
	// must be multiple of 90°
	float yRotate
) {
	public PrintArguments withBlueprint(final String blueprint) {
		return new PrintArguments(
			blueprint,
			this.skipFluids, this.skipMissing, this.scale, this.xAlign, this.yAlign, this.zAlign, this.xRotate, this.yRotate
		);
	}

	public PrintArguments validated() {
		float scale = this.scale;
		if (scale < 0.1f) {
			scale = 0.1f;
		} else if (!(scale < 1.0f)) { // handle NaN case as well
			scale = 1.0f;
		}
		float xRotate = Float.isFinite(this.xRotate) ? 0 : Math.round(this.xRotate / 90) % 4 * 90;
		float yRotate = Float.isFinite(this.yRotate) ? 0 : Math.round(this.yRotate / 90) % 4 * 90;
		return new PrintArguments(
			blueprint,
			this.skipFluids, this.skipMissing,
			scale,
			this.xAlign, this.yAlign, this.zAlign,
			xRotate, yRotate
		);
	}

	public static PrintArguments readFromNbt(final CompoundTag data) {
		final String blueprint = data.getString("Blueprint");
		final boolean skipFluids = data.getBoolean("SkipFluids");
		final boolean skipMissing = data.getBoolean("SkipMissing");
		final float scale = data.getFloat("Scale");
		final Alignment xAlign = Alignment.fromOrdinal(data.getByte("XAlign"));
		final Alignment yAlign = Alignment.fromOrdinal(data.getByte("YAlign"));
		final Alignment zAlign = Alignment.fromOrdinal(data.getByte("ZAlign"));
		final float xRotate = data.getFloat("XRotate");
		final float yRotate = data.getFloat("YRotate");
		return new PrintArguments(blueprint, skipFluids, skipMissing, scale, xAlign, yAlign, zAlign, xRotate, yRotate);
	}

	public CompoundTag writeToNbt(final CompoundTag data) {
		data.putString("Blueprint", this.blueprint);
		data.putBoolean("SkipFluids", this.skipFluids);
		data.putBoolean("SkipMissing", this.skipMissing);
		data.putFloat("Scale", this.scale);
		data.putByte("XAlign", (byte) (this.xAlign.ordinal()));
		data.putByte("YAlign", (byte) (this.yAlign.ordinal()));
		data.putByte("ZAlign", (byte) (this.zAlign.ordinal()));
		data.putFloat("XRotate", this.xRotate);
		data.putFloat("YRotate", this.yRotate);
		return data;
	}
}

package com.github.litermc.vsprinter.api;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public enum PrintStatus {
	UNCONSTRUCTED("unconstructed"),
	IDLE("idle"),
	WORKING("working"),
	EMPTY_BLUEPRINT("empty_blueprint"),
	INVALID("invalid"),
	SCALE_TOO_SMALL("scale_too_small"),
	SCALE_TOO_LARGE("scale_too_large"),
	NOT_ENOUGH_SPACE("not_enough_space"),
	REQUIRE_ENERGY("require_energy"),
	REQUIRE_MATERIAL("require_material");

	private final String key;

	private PrintStatus(final String key) {
		this.key = key;
	}

	public MutableComponent getText() {
		return Component.translatable("vsprinter.status." + this.key + ".text");
	}

	public MutableComponent getDisplayText() {
		return Component.translatable("vsprinter.status." + this.key + ".display");
	}
}

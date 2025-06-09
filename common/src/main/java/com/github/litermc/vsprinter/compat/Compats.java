package com.github.litermc.vsprinter.compat;

import com.github.litermc.vsprinter.platform.PlatformHelper;

public enum Compats {
	CREATE("create");

	private final String id;
	private boolean loaded = false;

	private Compats(final String id) {
		this.id = id;
	}

	public boolean isLoaded() {
		if (this.loaded) {
			return true;
		}
		if (PlatformHelper.get().isModLoaded(this.id)) {
			this.loaded = true;
			return true;
		}
		return false;
	}
}

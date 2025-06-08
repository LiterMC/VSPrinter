package com.github.litermc.vsprinter.util;

public class IntRef {
	private int value;

	public IntRef() {
		this(0);
	}

	public IntRef(final int value) {
		this.value = value;
	}

	public int get() {
		return this.value;
	}

	public void set(int value) {
		this.value = value;
	}

	public void increment() {
		this.value++;
	}

	public int getAndIncrement() {
		return this.value++;
	}
}

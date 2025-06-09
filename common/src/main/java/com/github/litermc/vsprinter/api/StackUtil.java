package com.github.litermc.vsprinter.api;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class StackUtil {
	private static final int UNIT = 256;

	private StackUtil() {}

	public static ItemStack convertStackToUnits(final ItemStack stack) {
		return setStackToUnits(stack.copy());
	}

	public static ItemStack setStackToUnits(final ItemStack stack) {
		final Item item = stack.getItem();
		if (item.canBeDepleted()) {
			stack.setCount(stack.getCount() * item.getMaxDamage());
		} else {
			stack.setCount(stack.getCount() * UNIT);
		}
		return stack;
	}

	public static double convertUnitsToItemCount(final Item item, final int units) {
		if (item.canBeDepleted()) {
			return (double) (units) / item.getMaxDamage();
		}
		return (double) (units) / UNIT;
	}
}

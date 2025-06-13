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
			final int remainDamage = item.getMaxDamage() - stack.getDamageValue();
			stack.removeTagKey(ItemStack.TAG_DAMAGE);
			stack.setCount(stack.getCount() * remainDamage);
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

	public static ItemStack setUnitsToStack(final ItemStack stack) {
		final Item item = stack.getItem();
		if (item.canBeDepleted()) {
			final int maxDamage = item.getMaxDamage();
			final double count = stack.getCount() / maxDamage;
			if (count < 1) {
				stack.setCount(1);
				stack.setDamageValue((int) (count * maxDamage));
			} else {
				stack.setCount((int) (count));
			}
		} else {
			stack.setCount(stack.getCount() / UNIT);
		}
		return stack;
	}
}

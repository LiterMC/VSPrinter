package com.github.litermc.vsprinter.compat.create;

import com.github.litermc.vsprinter.api.StackUtil;

import com.simibubi.create.content.schematics.requirement.ItemRequirement;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CreateCompat {
	private CreateCompat() {}

	public static List<ItemStack> convertItemRequirementToUnits(final ItemRequirement requirement) {
		if (requirement == ItemRequirement.INVALID) {
			return null;
		}

		final List<ItemRequirement.StackRequirement> items = requirement.getRequiredItems();
		if (requirement == ItemRequirement.NONE || items.size() == 0) {
			return List.of();
		}

		return items.stream().map((stack) -> switch (stack.usage) {
			case CONSUME -> StackUtil.convertStackToUnits(stack.stack);
			case DAMAGE -> stack.stack;
		}).toList();
	}
}

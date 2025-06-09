package com.github.litermc.vsprinter.api;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import javax.annotation.Nullable;

public interface ISchematicSpecialBlock {
	List<ItemStack> getPrintRequiredUnits(BlockState state, @Nullable CompoundTag data);
}

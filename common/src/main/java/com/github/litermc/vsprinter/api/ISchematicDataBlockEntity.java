package com.github.litermc.vsprinter.api;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public interface ISchematicDataBlockEntity {
	/**
	 * getPrintSchemaData returns the special schema data for this block.
	 * The data or {@code null} will be used in {@link loadPrintSchemaData}.
	 *
	 * @return schema data to be saved.
	 */
	@Nullable
	CompoundTag getPrintableSchematicData();

	/**
	 * loadPrintSchemaData will be called after ship is printed.
	 * 
	 * @param data The schema data {@link #getPrintSchemaData} returns
	 */
	void loadPrintableSchematicData(@Nullable CompoundTag data);
}

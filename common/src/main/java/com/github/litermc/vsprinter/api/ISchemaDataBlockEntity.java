package com.github.litermc.vsprinter.api;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ISchemaDataBlockEntity {
	/**
	 * getPrintSchemaData returns the special schema data for this block.
	 * If the data is not null, it will be used in {@link loadPrintSchemaData}.
	 *
	 * @return schema data to be saved, or {@code null} if not exists.
	 */
	@Nullable
	CompoundTag getPrintSchemaData();

	/**
	 * loadPrintSchemaData will be called after ship is printed and if the block's schema data is not null.
	 * 
	 * @param data The schema data {@link #getPrintSchemaData} returns
	 */
	void loadPrintSchemaData(@Nonnull CompoundTag data);
}

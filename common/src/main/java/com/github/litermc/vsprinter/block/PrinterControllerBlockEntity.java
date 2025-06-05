package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.VSPRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class PrinterControllerBlockEntity extends BlockEntity {
	public PrinterControllerBlockEntity(BlockPos pos, BlockState state) {
		super(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), pos, state);
	}
}

package com.github.litermc.vsprinter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PrinterControllerBlock extends Block implements EntityBlock {
	public PrinterControllerBlock(BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public PrinterControllerBlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new PrinterControllerBlockEntity(pos, state);
	}
}

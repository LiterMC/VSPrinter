package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.block.property.NullableDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class PrinterControllerBlock extends Block implements EntityBlock {
	public static final EnumProperty<NullableDirection> FRAME = EnumProperty.create("frame", NullableDirection.class);

	public PrinterControllerBlock(final BlockBehaviour.Properties props) {
		super(props);
		BlockState defaultState = this.defaultBlockState();
		defaultState = defaultState.setValue(FRAME, NullableDirection.NULL);
		this.registerDefaultState(defaultState);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FRAME);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState updateShape(BlockState self, final Direction face, final BlockState neighbor, final LevelAccessor level, final BlockPos selfPos, final BlockPos neighborPos) {
		final boolean isFrame = neighbor.getBlock() instanceof PrinterFrameBlock;
		final Direction frameDir = self.getValue(FRAME).asDirection();
		boolean shouldInvalidate = false;
		if (isFrame) {
			final Direction controllerDir = neighbor.getValue(PrinterFrameBlock.CONTROLLER).asDirection();
			if (frameDir == null && (controllerDir == null || controllerDir == face.getOpposite())) {
				self = self.setValue(FRAME, NullableDirection.fromDirection(face));
				shouldInvalidate = true;
			}
		} else if (frameDir == face) {
			self = self.setValue(FRAME, NullableDirection.NULL);
			shouldInvalidate = true;
		}
		if (shouldInvalidate && level.getBlockEntity(selfPos) instanceof PrinterControllerBlockEntity be) {
			be.invalidate();
		}
		return self;
	}

	@Override
	public PrinterControllerBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new PrinterControllerBlockEntity(pos, state);
	}
}

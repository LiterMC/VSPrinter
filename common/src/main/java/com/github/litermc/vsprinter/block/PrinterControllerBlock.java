package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.PrintStatus;
import com.github.litermc.vsprinter.block.property.NullableDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class PrinterControllerBlock extends Block implements EntityBlock {
	public static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
	public static final EnumProperty<NullableDirection> FRAME = EnumProperty.create("frame", NullableDirection.class);

	public PrinterControllerBlock(final BlockBehaviour.Properties props) {
		super(props);
		BlockState defaultState = this.defaultBlockState()
			.setValue(ORIENTATION, FrontAndTop.WEST_UP)
			.setValue(FRAME, NullableDirection.NULL);
		this.registerDefaultState(defaultState);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ORIENTATION);
		builder.add(FRAME);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState rotate(final BlockState state, final Rotation rotation) {
		return state.setValue(ORIENTATION, rotation.rotation().rotate(state.getValue(ORIENTATION)));
	}

	@Override
	public BlockState mirror(final BlockState state, final Mirror mirror) {
		return state.setValue(ORIENTATION, mirror.rotation().rotate(state.getValue(ORIENTATION)));
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		final Direction frontDir = context.getNearestLookingDirection().getOpposite();
		final Direction topDir = frontDir == Direction.UP
			? context.getHorizontalDirection()
			: frontDir == Direction.DOWN
				? context.getHorizontalDirection().getOpposite()
				: Direction.UP;
		return this.defaultBlockState().setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(frontDir, topDir));
	}

	@Override
	public RenderShape getRenderShape(final BlockState state) {
		return RenderShape.MODEL;
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
			if (frameDir == face) {
				be.setStatus(isFrame ? PrintStatus.IDLE : PrintStatus.UNCONSTRUCTED);
			}
		}
		return self;
	}

	@Override
	public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block block, final BlockPos neighborPos, final boolean isMoving) {
		if (level.getBlockEntity(pos) instanceof PrinterControllerBlockEntity controller) {
			controller.neighborChanged(neighborPos);
		}
	}

	@Override
	public PrinterControllerBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new PrinterControllerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		if (type != VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get()) {
			return null;
		}
		return level.isClientSide ? null : (level2, pos, state2, entity) -> ((PrinterControllerBlockEntity) (entity)).serverTick();
	}
}

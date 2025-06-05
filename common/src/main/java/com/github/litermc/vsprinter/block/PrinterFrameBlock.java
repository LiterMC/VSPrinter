package com.github.litermc.vsprinter.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class PrinterFrameBlock extends Block {
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;

	private static final EnumMap<Direction, BooleanProperty> DIRECTION_VALUES = new EnumMap<>(
		Map.of(
			Direction.DOWN, DOWN,
			Direction.UP, UP,
			Direction.NORTH, NORTH,
			Direction.SOUTH, SOUTH,
			Direction.WEST, WEST,
			Direction.EAST, EAST
		)
	);

	public PrinterFrameBlock(BlockBehaviour.Properties props) {
		super(props);
		BlockState defaultState = this.defaultBlockState();
		for (final BooleanProperty dir : DIRECTION_VALUES.values()) {
			defaultState = defaultState.setValue(dir, false);
		}
		this.registerDefaultState(defaultState);
	}

	public Stream<BlockPos> streamConnected(final Level level, final BlockPos pos) {
		return streamConnected(level, pos, new HashSet<>());
	}

	private Stream<BlockPos> streamConnected(final Level level, final BlockPos pos, final Set<BlockPos> visited) {
		final BlockState state = level.getBlockState(pos);
		return DIRECTION_VALUES.entrySet().stream()
			.filter((e) -> state.getValue(e.getValue()))
			.map(Map.Entry::getKey)
			.map(pos::relative)
			.filter(visited::add)
			.flatMap((p) -> Stream.concat(Stream.of(p), streamConnected(level, p, visited)));
	}

	public PrinterControllerBlockEntity getController(final Level level, final BlockPos pos) {
		// level
		return null;
	}
}

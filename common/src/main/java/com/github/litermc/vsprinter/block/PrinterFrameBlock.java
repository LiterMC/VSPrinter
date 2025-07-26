package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.block.property.NullableDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.primitives.AABBi;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class PrinterFrameBlock extends Block {
	public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
	public static final BooleanProperty UP = BlockStateProperties.UP;
	public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
	public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
	public static final BooleanProperty WEST = BlockStateProperties.WEST;
	public static final BooleanProperty EAST = BlockStateProperties.EAST;
	public static final EnumProperty<NullableDirection> CONTROLLER = EnumProperty.create("controller", NullableDirection.class);
	// TODO: remvoe the valid prop
	private static final BooleanProperty VALID = BooleanProperty.create("valid");

	private static final EnumMap<Direction, BooleanProperty> DIRECTION_PROP_MAP = new EnumMap<>(
		Map.of(
			Direction.DOWN, DOWN,
			Direction.UP, UP,
			Direction.NORTH, NORTH,
			Direction.SOUTH, SOUTH,
			Direction.WEST, WEST,
			Direction.EAST, EAST
		)
	);

	public PrinterFrameBlock(final BlockBehaviour.Properties props) {
		super(props);
		BlockState defaultState = this.defaultBlockState();
		for (final BooleanProperty dir : DIRECTION_PROP_MAP.values()) {
			defaultState = defaultState.setValue(dir, false);
		}
		defaultState = defaultState.setValue(CONTROLLER, NullableDirection.NULL);
		defaultState = defaultState.setValue(VALID, false);
		this.registerDefaultState(defaultState);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		DIRECTION_PROP_MAP.values().forEach(builder::add);
		builder.add(CONTROLLER);
		builder.add(VALID);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext context) {
		BlockState state = this.defaultBlockState();
		final Level level = context.getLevel();
		final BlockPos pos = context.getClickedPos();
		for (final Direction dir : Direction.values()) {
			if (level.getBlockState(pos.relative(dir)).getBlock() instanceof PrinterControllerBlock) {
				state = state.setValue(CONTROLLER, NullableDirection.fromDirection(dir));
				state = state.setValue(VALID, getFrameBox(level, pos, state) != null);
				return state;
			}
		}
		return state;
	}

	@Override
	public RenderShape getRenderShape(final BlockState state) {
		return state.getValue(VALID)
			? RenderShape.INVISIBLE
			: RenderShape.MODEL;
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		final AABBi box = state.getValue(CONTROLLER) != NullableDirection.NULL && state.getValue(VALID) ? getFrameBox(level, pos) : null;
		final int frameLevel = box == null ? 0 : getFrameLevel(box);
		if (frameLevel >= 3) {
			return Shapes.block();
		}
		final double n = switch (frameLevel) {
		case 0 -> 1;
		case 1 -> 3;
		case 2 -> 7;
		default -> throw new IllegalStateException("unreachable");
		};
		final EnumSet<Direction> faces = getFacesFromBox(box, pos);
		VoxelShape shape = Shapes.empty();
		if (faces.contains(Direction.DOWN)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 0, 0, n, n, 16), BooleanOp.OR);
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(16 - n, 0, 0, 16, n, 16), BooleanOp.OR);
			}
			if (faces.contains(Direction.NORTH)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 0, 0, 16, n, n), BooleanOp.OR);
			}
			if (faces.contains(Direction.SOUTH)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 0, 16 - n, 16, n, 16), BooleanOp.OR);
			}
		}
		if (faces.contains(Direction.UP)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 16 - n, 0, n, 16, 16), BooleanOp.OR);
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(16 - n, 16 - n, 0, 16, 16, 16), BooleanOp.OR);
			}
			if (faces.contains(Direction.NORTH)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 16 - n, 0, 16, 16, n), BooleanOp.OR);
			}
			if (faces.contains(Direction.SOUTH)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 16 - n, 16 - n, 16, 16, 16), BooleanOp.OR);
			}
		}
		if (faces.contains(Direction.NORTH)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 0, 0, n, 16, n), BooleanOp.OR);
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(16 - n, 0, 0, 16, 16, n), BooleanOp.OR);
			}
		}
		if (faces.contains(Direction.SOUTH)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(0, 0, 16 - n, n, 16, 16), BooleanOp.OR);
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.joinUnoptimized(shape, Block.box(16 - n, 0, 16 - n, 16, 16, 16), BooleanOp.OR);
			}
		}
		return shape;
	}

	@Override
	public BlockState updateShape(BlockState self, final Direction face, final BlockState neighbor, final LevelAccessor level, final BlockPos selfPos, final BlockPos neighborPos) {
		final boolean isFrame = neighbor.getBlock() instanceof PrinterFrameBlock;
		final boolean isController = neighbor.getBlock() instanceof PrinterControllerBlock;
		final Direction oldController = self.getValue(CONTROLLER).asDirection();
		final boolean controllerChanged = oldController == face;
		self = self.setValue(DIRECTION_PROP_MAP.get(face), isFrame);
		if (isController) {
			final Direction neighborFrameDir = neighbor.getValue(PrinterControllerBlock.FRAME).asDirection();
			if (oldController == null) {
				if (neighborFrameDir == null || neighborFrameDir == face.getOpposite()) {
					self = self.setValue(CONTROLLER, NullableDirection.fromDirection(face));
				}
			} else if (oldController == face && neighborFrameDir != face.getOpposite()) {
				self = self.setValue(CONTROLLER, NullableDirection.NULL);
			}
		} else if (isFrame && (oldController == null || controllerChanged)) {
			final Direction neighborController = neighbor.getValue(CONTROLLER).asDirection();
			if (neighborController == null) {
				if (controllerChanged) {
					self = self.setValue(CONTROLLER, NullableDirection.NULL);
				}
			} else if (neighborController != face.getOpposite()) {
				invalidateController(level, neighborPos);
				self = self.setValue(CONTROLLER, NullableDirection.fromDirection(face));
			}
		} else if (controllerChanged) {
			self = self.setValue(CONTROLLER, NullableDirection.NULL);
		}
		// TODO: fix sometimes controller link won't update
		if (self.getValue(CONTROLLER) == NullableDirection.NULL) {
			for (final Direction dir : Direction.values()) {
				if (controllerChanged && dir == face) {
					continue;
				}
				final BlockPos otherPos = selfPos.relative(dir);
				final BlockState other = level.getBlockState(otherPos);
				if (other.getBlock() instanceof PrinterControllerBlock || other.getBlock() instanceof PrinterFrameBlock && other.getValue(CONTROLLER) != NullableDirection.NULL && getController(level, otherPos, new HashSet<>(List.of(selfPos))) != null) {
					self = self.setValue(CONTROLLER, NullableDirection.fromDirection(dir));
					break;
				}
			}
		}
		self = self.setValue(VALID, self.getValue(CONTROLLER) != NullableDirection.NULL && getFrameBox(level, selfPos) != null);
		return self;
	}

	public static Stream<BlockPos> streamSelfAndConnected(final BlockGetter level, final BlockPos pos) {
		return streamSelfAndConnected(level, pos, level.getBlockState(pos), new HashSet<>(List.of(pos)));
	}

	private static Stream<BlockPos> streamSelfAndConnected(final BlockGetter level, final BlockPos pos, final BlockState state) {
		return streamSelfAndConnected(level, pos, state, new HashSet<>(List.of(pos)));
	}

	private static Stream<BlockPos> streamSelfAndConnected(final BlockGetter level, final BlockPos pos, final BlockState state, final Set<BlockPos> visited) {
		if (!(state.getBlock() instanceof PrinterFrameBlock)) {
			return Stream.empty();
		}
		return Stream.concat(
			Stream.of(pos),
			DIRECTION_PROP_MAP.entrySet().stream()
				.filter((e) -> state.getValue(e.getValue()))
				.map(Map.Entry::getKey)
				.map(pos::relative)
				.filter(visited::add)
				.flatMap((p) -> streamSelfAndConnected(level, p, level.getBlockState(p), visited))
		);
	}

	public static PrinterControllerBlockEntity getController(final BlockGetter level, final BlockPos pos) {
		return getController(level, pos, new HashSet<>());
	}

	private static PrinterControllerBlockEntity getController(final BlockGetter level, final BlockPos pos, final Set<BlockPos> visited) {
		BlockPos p = pos;
		while (visited.add(p)) {
			if (level.getBlockEntity(p) instanceof PrinterControllerBlockEntity be) {
				return be;
			}
			final BlockState state = level.getBlockState(p);
			if (!(state.getBlock() instanceof PrinterFrameBlock)) {
				return null;
			}
			final Direction dir = state.getValue(CONTROLLER).asDirection();
			if (dir == null) {
				return null;
			}
			p = p.relative(dir);
		}
		return null;
	}

	private static void invalidateController(final BlockGetter level, final BlockPos pos) {
		final PrinterControllerBlockEntity controller = getController(level, pos);
		if (controller != null) {
			controller.invalidate();
		}
	}

	public static AABBi getFrameBox(final BlockGetter level, final BlockPos pos) {
		return getFrameBox(streamSelfAndConnected(level, pos).toList());
	}

	public static AABBi getFrameBox(final BlockGetter level, final BlockPos pos, final BlockState state) {
		return getFrameBox(streamSelfAndConnected(level, pos, state).toList());
	}

	public static AABBi getFrameBox(final List<BlockPos> frames) {
		if (frames.isEmpty()) {
			return null;
		}
		final AABBi box = newAABBiFromBlockPos(frames.get(0));
		for (final BlockPos frame : frames) {
			final int x = frame.getX(), y = frame.getY(), z = frame.getZ();
			if (!isOnEdge(box, x, y, z)) {
				return null;
			}
			box.union(x, y, z);
		}
		for (final BlockPos frame : frames) {
			final int x = frame.getX(), y = frame.getY(), z = frame.getZ();
			if (!isOnEdge(box, x, y, z)) {
				return null;
			}
		}
		return box;
	}

	public static EnumSet<Direction> getFacesFromBox(final AABBi box, final BlockPos pos) {
		if (box == null) {
			return EnumSet.allOf(Direction.class);
		}
		final EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
		final int x = pos.getX(), y = pos.getY(), z = pos.getZ();
		if (box.minX == x) {
			faces.add(Direction.WEST);
		}
		if (box.maxX == x) {
			faces.add(Direction.EAST);
		}
		if (box.minY == y) {
			faces.add(Direction.DOWN);
		}
		if (box.maxY == y) {
			faces.add(Direction.UP);
		}
		if (box.minZ == z) {
			faces.add(Direction.NORTH);
		}
		if (box.maxZ == z) {
			faces.add(Direction.SOUTH);
		}
		return faces;
	}

	private static final boolean isOnEdge(final AABBi box, final int x, final int y, final int z) {
		int i = 0;
		if (x == box.minX || x == box.maxX) {
			i++;
		}
		if (y == box.minY || y == box.maxY) {
			i++;
		}
		if (z == box.minZ || z == box.maxZ) {
			i++;
		}
		return i >= 2;
	}

	/**
	 * 0: 1 block wide, 1px frame, 14px inner space
	 * 1: 2 blocks wide, 3px frame, 26px inner space
	 * 2: 3 blocks wide, 7px frame, 30px inner space
	 * 3: 4+ blocks wide, full block (16px) frame, 32px inner space at least
	 */
	public static int getFrameLevel(final AABBi box) {
		final int sizeX = box.lengthX() + 1, sizeY = box.lengthY() + 1, sizeZ = box.lengthZ() + 1;
		for (int n = 1; n <= 3; n++) {
			if (sizeX == n || sizeY == n || sizeZ == n) {
				return n - 1;
			}
		}
		return 3;
	}

	private static final AABBi newAABBiFromBlockPos(final BlockPos pos) {
		return new AABBi(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
	}
}

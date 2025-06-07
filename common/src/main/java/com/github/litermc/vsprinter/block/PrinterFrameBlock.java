package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.block.property.NullableDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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

	private static final EnumMap<Direction, BooleanProperty> DIRECTION_MAP = new EnumMap<>(
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
		for (final BooleanProperty dir : DIRECTION_MAP.values()) {
			defaultState = defaultState.setValue(dir, false);
		}
		defaultState = defaultState.setValue(CONTROLLER, NullableDirection.NULL);
		this.registerDefaultState(defaultState);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		DIRECTION_MAP.values().forEach(builder::add);
		builder.add(CONTROLLER);
		super.createBlockStateDefinition(builder);
	}

	@Override
	public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
		final AABBi box = getFrameBox(level, pos);
		final EnumSet<Direction> faces;
		final int frameLevel;
		if (box == null) {
			faces = EnumSet.allOf(Direction.class);
			frameLevel = 0;
		} else {
			faces = EnumSet.noneOf(Direction.class);
			frameLevel = getFrameLevel(box);
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
		}
		if (frameLevel >= 3) {
			return Shapes.block();
		}
		final double n = switch (frameLevel) {
		case 0 -> 1;
		case 1 -> 3;
		case 2 -> 7;
		default -> throw new IllegalStateException("unreachable");
		};
		VoxelShape shape = Shapes.empty();
		if (faces.contains(Direction.DOWN)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.or(shape, Block.box(0, 0, 0, n, n, 16));
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.or(shape, Block.box(16 - n, 0, 0, 16, n, 16));
			}
			if (faces.contains(Direction.NORTH)) {
				shape = Shapes.or(shape, Block.box(0, 0, 0, 16, n, n));
			}
			if (faces.contains(Direction.SOUTH)) {
				shape = Shapes.or(shape, Block.box(0, 0, 16 - n, 16, n, 16));
			}
		}
		if (faces.contains(Direction.UP)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.or(shape, Block.box(0, 16 - n, 0, n, 16, 16));
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.or(shape, Block.box(16 - n, 16 - n, 0, 16, 16, 16));
			}
			if (faces.contains(Direction.NORTH)) {
				shape = Shapes.or(shape, Block.box(0, 16 - n, 0, 16, 16, n));
			}
			if (faces.contains(Direction.SOUTH)) {
				shape = Shapes.or(shape, Block.box(0, 16 - n, 16 - n, 16, 16, 16));
			}
		}
		if (faces.contains(Direction.NORTH)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.or(shape, Block.box(0, 0, 0, n, 16, n));
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.or(shape, Block.box(16 - n, 0, 0, 16, 16, n));
			}
		}
		if (faces.contains(Direction.SOUTH)) {
			if (faces.contains(Direction.WEST)) {
				shape = Shapes.or(shape, Block.box(0, 0, 16 - n, n, 16, 16));
			}
			if (faces.contains(Direction.EAST)) {
				shape = Shapes.or(shape, Block.box(16 - n, 0, 16 - n, 16, 16, 16));
			}
		}
		return shape;
	}

	@Override
	public BlockState updateShape(BlockState self, final Direction face, final BlockState neighbor, final LevelAccessor level, final BlockPos selfPos, final BlockPos neighborPos) {
		final boolean isFrame = neighbor.getBlock() instanceof PrinterFrameBlock;
		final boolean isController = neighbor.getBlock() instanceof PrinterControllerBlock;
		final Direction selfController = self.getValue(CONTROLLER).asDirection();
		final boolean controllerChanged = selfController == face;
		self = self.setValue(DIRECTION_MAP.get(face), isFrame);
		if (isController) {
			final Direction neighborFrameDir = neighbor.getValue(PrinterControllerBlock.FRAME).asDirection();
			if (selfController == null) {
				if (neighborFrameDir == null || neighborFrameDir == face.getOpposite()) {
					self = self.setValue(CONTROLLER, NullableDirection.fromDirection(face));
				}
			} else if (selfController == face && neighborFrameDir != face.getOpposite()) {
				self = self.setValue(CONTROLLER, NullableDirection.NULL);
			}
		} else if (isFrame && (selfController == null || controllerChanged) && neighbor.getValue(CONTROLLER) != NullableDirection.NULL) {
			self = self.setValue(CONTROLLER, NullableDirection.fromDirection(face));
		} else if (controllerChanged) {
			self = self
				.setValue(DIRECTION_MAP.get(face), false)
				.setValue(CONTROLLER, NullableDirection.NULL);
		}
		return self;
	}

	public static Stream<BlockPos> streamSelfAndConnected(final BlockGetter level, final BlockPos pos) {
		return streamSelfAndConnected(level, pos, new HashSet<>(List.of(pos)));
	}

	private static Stream<BlockPos> streamSelfAndConnected(final BlockGetter level, final BlockPos pos, final Set<BlockPos> visited) {
		final BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof PrinterFrameBlock)) {
			return Stream.empty();
		}
		return Stream.concat(
			Stream.of(pos),
			DIRECTION_MAP.entrySet().stream()
				.filter((e) -> state.getValue(e.getValue()))
				.map(Map.Entry::getKey)
				.map(pos::relative)
				.filter(visited::add)
				.flatMap((p) -> streamSelfAndConnected(level, p, visited))
		);
	}

	public PrinterControllerBlockEntity getController(final BlockGetter level, final BlockPos pos) {
		final BlockPos.MutableBlockPos p = pos.mutable();
		while (true) {
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
			p.move(dir);
		}
	}

	private void invalidateController(final BlockGetter level, final BlockPos pos) {
		final PrinterControllerBlockEntity controller = getController(level, pos);
		if (controller != null) {
			controller.invalidate();
		}
	}

	public static AABBi getFrameBox(final BlockGetter level, final BlockPos pos) {
		final List<BlockPos> frames = streamSelfAndConnected(level, pos).toList();
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

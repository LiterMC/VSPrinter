package com.github.litermc.vsprinter.api;

import com.github.litermc.vsprinter.compat.Compats;
import com.github.litermc.vsprinter.compat.create.CreateCompat;
import com.github.litermc.vsprinter.util.BlockPosBitSet;
import com.github.litermc.vsprinter.util.IntRef;

import com.simibubi.create.content.schematics.requirement.ISpecialBlockItemRequirement;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.primitives.AABBic;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class PrintableSchematic {
	private String fingerprint;
	private final BlockPosBitSet posList;
	private final List<BlockState> blocks;
	private final List<CompoundTag> datas;

	protected PrintableSchematic(
		final String fingerprint,
		final BlockPosBitSet posList,
		final List<BlockState> blocks,
		final List<CompoundTag> datas
	) {
		this.fingerprint = fingerprint;
		this.posList = posList;
		this.blocks = blocks;
		this.datas = datas;
	}

	public PrintableSchematic(
		final BlockPosBitSet posList,
		final List<BlockState> blocks,
		final List<CompoundTag> datas
	) {
		this(null, posList, blocks, datas);
	}

	public int size() {
		return this.blocks.size();
	}

	public Vec3i getDimension() {
		return this.posList.getDimension();
	}

	public String getFingerprint() {
		if (this.fingerprint == null) {
			this.fingerprint = this.calculateFingerprint();
		}
		return this.fingerprint;
	}

	public Stream<BlockData> stream() {
		final IntRef index = new IntRef();
		return this.posList.streamBlockPos()
			.map((pos) -> {
				final int i = index.getAndIncrement();
				final BlockState state = this.blocks.get(i);
				final CompoundTag data = this.datas.get(i);
				return new BlockData(pos, state, data);
			});
	}

	public record BlockData(BlockPos position, BlockState blockState, CompoundTag data) {
		public List<ItemStack> requiredItems() {
			final Block block = this.blockState.getBlock();
			if (block instanceof ISchematicSpecialBlock specialBlock) {
				return specialBlock.getPrintRequiredUnits(this.blockState, this.data);
			}
			if (Compats.CREATE.isLoaded() && block instanceof ISpecialBlockItemRequirement specialBlock) {
				return CreateCompat.convertItemRequirementToUnits(specialBlock.getRequiredItems(this.blockState, null));
			}
			return getBlockDefaultRequiredUnits(this.blockState);
		}
	}

	private static List<ItemStack> getBlockDefaultRequiredUnits(final BlockState state) {
		final Block block = state.getBlock();
		final Item item = block.asItem();
		if (item == Items.AIR) {
			return null;
		}
		return List.of(StackUtil.setStackToUnits(new ItemStack(item, 1)));
	}

	public static PrintableSchematic fromLevel(final Level level, final AABBic area) {
		return fromLevel(level, area, (l, p) -> true);
	}

	public static PrintableSchematic fromLevel(final Level level, final AABBic area, final BiPredicate<Level, BlockPos> validator) {
		final BlockPos min = new BlockPos(area.minX(), area.minY(), area.minZ());
		final Vec3i dimension = new Vec3i(area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1).subtract(min);
		BlockPosBitSet posList = new BlockPosBitSet(dimension);
		final ArrayList<BlockState> blocks = new ArrayList<>();
		final ArrayList<CompoundTag> datas = new ArrayList<>();
		final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = 0; y < dimension.getY(); y++) {
			for (int z = 0; z < dimension.getZ(); z++) {
				for (int x = 0; x < dimension.getX(); x++) {
					final BlockState state = level.getBlockState(cursor.setWithOffset(min, x, y, z));
					if (state.isAir()) {
						continue;
					}
					if (!validator.test(level, cursor)) {
						return null;
					}
					posList.set(x, y, z);
					blocks.add(state);
					datas.add(level.getBlockEntity(cursor) instanceof ISchematicDataBlockEntity dataBlock ? dataBlock.getPrintableSchematicData() : null);
				}
			}
		}
		if (blocks.isEmpty()) {
			return null;
		}
		blocks.trimToSize();
		datas.trimToSize();
		int lowY = 0, highY = dimension.getY() - 1;
	LOW_Y_LOOP:
		for (; lowY < highY; lowY++) {
			for (int z = 0; z < dimension.getZ(); z++) {
				for (int x = 0; x < dimension.getX(); x++) {
					if (posList.get(x, lowY, z)) {
						break LOW_Y_LOOP;
					}
				}
			}
		}
	HIGH_Y_LOOP:
		for (; highY > lowY ; highY--) {
			for (int z = 0; z < dimension.getZ(); z++) {
				for (int x = 0; x < dimension.getX(); x++) {
					if (posList.get(x, highY, z)) {
						break HIGH_Y_LOOP;
					}
				}
			}
		}
		highY++;
		int lowX = 0, highX = dimension.getX() - 1;
	LOW_X_LOOP:
		for (; lowX < highX; lowX++) {
			for (int y = lowY; y < highY; y++) {
				for (int z = 0; z < dimension.getZ(); z++) {
					if (posList.get(lowX, y, z)) {
						break LOW_X_LOOP;
					}
				}
			}
		}
	HIGH_X_LOOP:
		for (; highX > lowX ; highX--) {
			for (int y = lowY; y < highY; y++) {
				for (int z = 0; z < dimension.getZ(); z++) {
					if (posList.get(highX, y, z)) {
						break HIGH_X_LOOP;
					}
				}
			}
		}
		highX++;
		int lowZ = 0, highZ = dimension.getZ() - 1;
	LOW_Z_LOOP:
		for (; lowZ < highZ; lowZ++) {
			for (int x = lowX; x < highX; x++) {
				for (int y = lowY; y < highY; y++) {
					if (posList.get(x, y, lowZ)) {
						break LOW_Z_LOOP;
					}
				}
			}
		}
	HIGH_Z_LOOP:
		for (; highZ > lowZ ; highZ--) {
			for (int x = lowX; x < highX; x++) {
				for (int y = lowY; y < highY; y++) {
					if (posList.get(x, y, highZ)) {
						break HIGH_Z_LOOP;
					}
				}
			}
		}
		highZ++;
		final int lowXF = lowX, lowYF = lowY, lowZF = lowZ;
		final Vec3i dimensionTrimed = new Vec3i(highX - lowXF, highY - lowYF, highZ - lowZF);
		if (!dimensionTrimed.equals(dimension)) {
			final BlockPosBitSet posList2 = new BlockPosBitSet(dimensionTrimed);
			posList.forEach((pos) -> {
				final int newX = pos.getX() - lowXF;
				final int newY = pos.getY() - lowYF;
				final int newZ = pos.getZ() - lowZF;
				if (
					newX < 0 || newY < 0 || newZ < 0 ||
					newX >= dimensionTrimed.getX() || newY >= dimensionTrimed.getY() || newZ >= dimensionTrimed.getZ()
				) {
					return;
				}
				posList2.set(newX, newY, newZ);
			});
			posList = posList2;
		}
		return new PrintableSchematic(posList, blocks, datas);
	}

	public static PrintableSchematic readFrom(final FriendlyByteBuf buf) {
		final int startIndex = buf.readerIndex();
		final BlockPosBitSet posList = BlockPosBitSet.readFrom(buf);
		final int size = posList.cardinality();
		final ArrayList<BlockState> blocks = new ArrayList<>(size);
		final ArrayList<CompoundTag> datas = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			blocks.add(buf.readById(Block.BLOCK_STATE_REGISTRY));
		}
		for (int i = 0; i < size; i++) {
			buf.markReaderIndex();
			if (buf.readByte() == 0) {
				datas.add(null);
			} else {
				buf.resetReaderIndex();
				datas.add(buf.readNbt());
			}
		}
		final int endIndex = buf.readerIndex();
		final String fingerprint = calculateFingerprint(buf.nioBuffer(startIndex, endIndex - startIndex));

		return new PrintableSchematic(fingerprint, posList, blocks, datas);
	}

	public void writeTo(final FriendlyByteBuf buf) {
		this.posList.writeTo(buf);
		for (int i = 0; i < this.size(); i++) {
			buf.writeId(Block.BLOCK_STATE_REGISTRY, this.blocks.get(i));
		}
		for (int i = 0; i < this.size(); i++) {
			final CompoundTag tag = this.datas.get(i);
			if (tag == null) {
				buf.writeByte(0);
			} else {
				buf.writeNbt(tag);
			}
		}
	}

	private String calculateFingerprint() {
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(Math.max(512, (this.posList.size() + 7) / 8)));
		this.posList.writeTo(buf);
		for (int i = 0; i < this.size(); i++) {
			buf.writeId(Block.BLOCK_STATE_REGISTRY, this.blocks.get(i));
			md.update(buf.nioBuffer(0, buf.writerIndex()));
			buf.clear();
		}
		for (int i = 0; i < this.size(); i++) {
			final CompoundTag tag = this.datas.get(i);
			if (tag == null) {
				md.update((byte) (0));
			} else {
				buf.writeNbt(tag);
				md.update(buf.nioBuffer(0, buf.writerIndex()));
				buf.clear();
			}
		}
		return HexFormat.of().formatHex(md.digest());
	}

	private static String calculateFingerprint(final ByteBuffer buf) {
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		md.update(buf);
		return HexFormat.of().formatHex(md.digest());
	}

	public List<ISchematicDataBlockEntity> placeInLevel(final Level level, final BlockPos origin) {
		this.stream().forEach((data) -> {
			final BlockPos target = origin.offset(data.position());
			level.setBlock(target, data.blockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		});

		final ArrayList<ISchematicDataBlockEntity> dataBlocks = new ArrayList<>();
		this.stream().forEach((data) -> {
			final BlockPos target = origin.offset(data.position());
			if (level.getBlockEntity(target) instanceof ISchematicDataBlockEntity dataBlock) {
				dataBlock.loadPrintableSchematicData(data.data());
				dataBlocks.add(dataBlock);
			}
			level.blockUpdated(target, level.getBlockState(target).getBlock());
		});
		dataBlocks.trimToSize();
		return dataBlocks;
	}
}

package com.github.litermc.vsprinter.api;

import com.github.litermc.vsprinter.util.BlockPosBitSet;
import com.github.litermc.vsprinter.util.IntRef;

import io.netty.buffer.ByteBuf;
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

public class PrintSchema {
	private String fingerprint;
	private final BlockPosBitSet posList;
	private final List<BlockState> blocks;
	private final List<CompoundTag> datas;

	protected PrintSchema(
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

	public PrintSchema(
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
			return getBlockDefaultRequiredItems(this.blockState);
		}
	}

	private static List<ItemStack> getBlockDefaultRequiredItems(final BlockState state) {
		final Block block = state.getBlock();
		final Item item = block.asItem();
		if (item == Items.AIR) {
			return null;
		}
		return List.of(new ItemStack(item, 1));
	}

	public static PrintSchema fromLevel(final Level level, final AABBic area) {
		return fromLevel(level, area, (l, p) -> true);
	}

	public static PrintSchema fromLevel(final Level level, final AABBic area, final BiPredicate<Level, BlockPos> validator) {
		final BlockPos min = new BlockPos(area.minX(), area.minY(), area.minZ());
		final BlockPos max = new BlockPos(area.maxX() + 1, area.maxY() + 1, area.maxZ() + 1);
		final BlockPosBitSet posList = new BlockPosBitSet(max.subtract(min));
		final ArrayList<BlockState> blocks = new ArrayList<>();
		final ArrayList<CompoundTag> datas = new ArrayList<>();
		final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
		for (int y = min.getY(); y < max.getY(); y++) {
			for (int z = min.getZ(); z < max.getZ(); z++) {
				for (int x = min.getX(); x < max.getX(); x++) {
					final BlockState state = level.getBlockState(cursor.set(x, y, z));
					if (state.isAir()) {
						continue;
					}
					if (!validator.test(level, cursor)) {
						return null;
					}
					posList.set(cursor);
					blocks.add(state);
					datas.add(level.getBlockEntity(cursor) instanceof ISchemaDataBlockEntity dataBlock ? dataBlock.getPrintSchemaData() : null);
				}
			}
		}
		blocks.trimToSize();
		datas.trimToSize();
		return new PrintSchema(posList, blocks, datas);
	}

	public static PrintSchema readFrom(final FriendlyByteBuf buf) {
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

		return new PrintSchema(fingerprint, posList, blocks, datas);
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
}

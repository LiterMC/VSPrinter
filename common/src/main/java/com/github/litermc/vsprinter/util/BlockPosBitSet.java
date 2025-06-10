package com.github.litermc.vsprinter.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.stream.Stream;

public class BlockPosBitSet extends BitSet implements Iterable<BlockPos> {
	private final Vec3i dimension;

	public BlockPosBitSet(Vec3i dimension) {
		super(dimension.getX() * dimension.getY() * dimension.getZ());
		this.dimension = dimension;
	}

	public Vec3i getDimension() {
		return this.dimension;
	}

	@Override
	public int size() {
		return this.dimension.getX() * dimension.getY() * dimension.getZ();
	}

	public final int getIndex(final BlockPos pos) {
		return this.getIndex(pos.getX(), pos.getY(), pos.getZ());
	}

	public int getIndex(final int x, final int y, final int z) {
		if (x < 0 || x >= dimension.getX()) {
			throw new IndexOutOfBoundsException("x value out of range");
		}
		if (y < 0 || y >= dimension.getY()) {
			throw new IndexOutOfBoundsException("y value out of range");
		}
		if (z < 0 || z >= dimension.getZ()) {
			throw new IndexOutOfBoundsException("z value out of range");
		}
		return (y * dimension.getZ() + z) * dimension.getX() + x;
	}

	public BlockPos getBlockPos(int index) {
		if (index < 0) {
			throw new IllegalArgumentException("index must be greater than zero");
		}
		final int x = index % dimension.getX();
		index /= dimension.getX();
		final int z = index % dimension.getZ();
		index /= dimension.getZ();
		final int y = index;
		if (y >= dimension.getY()) {
			throw new IndexOutOfBoundsException("index is greater than the dimension");
		}
		return new BlockPos(x, y, z);
	}

	public void clear(final BlockPos pos) {
		this.clear(this.getIndex(pos.getX(), pos.getY(), pos.getZ()));
	}

	public void flip(final BlockPos pos) {
		this.flip(this.getIndex(pos.getX(), pos.getY(), pos.getZ()));
	}

	public boolean get(final BlockPos pos) {
		return this.get(this.getIndex(pos.getX(), pos.getY(), pos.getZ()));
	}

	public void set(final int x, final int y, final int z) {
		this.set(this.getIndex(x, y, z));
	}

	public void set(final int x, final int y, final int z, final boolean value) {
		this.set(this.getIndex(x, y, z), value);
	}

	public void set(final BlockPos pos) {
		this.set(this.getIndex(pos.getX(), pos.getY(), pos.getZ()));
	}

	public void set(final BlockPos pos, final boolean value) {
		this.set(this.getIndex(pos.getX(), pos.getY(), pos.getZ()), value);
	}

	@Override
	public boolean equals(final Object other) {
		return other instanceof BlockPosBitSet otherSet && this.dimension.equals(otherSet.dimension) && super.equals(otherSet);
	}

	@Override
	public int hashCode() {
		return this.dimension.hashCode() * 31 + super.hashCode();
	}

	public static BlockPosBitSet readFrom(final FriendlyByteBuf buf) {
		final int x = buf.readVarInt();
		final int y = buf.readVarInt();
		final int z = buf.readVarInt();
		final byte[] data = buf.readByteArray();
		final BlockPosBitSet set = new BlockPosBitSet(new Vec3i(x, y, z));
		set.or(BitSet.valueOf(data));
		return set;
	}

	public void writeTo(final FriendlyByteBuf buf) {
		buf.writeVarInt(this.dimension.getX());
		buf.writeVarInt(this.dimension.getY());
		buf.writeVarInt(this.dimension.getZ());
		buf.writeByteArray(this.toByteArray());
	}

	public Stream<BlockPos> streamBlockPos() {
		return this.stream().mapToObj(this::getBlockPos);
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return this.streamBlockPos().iterator();
	}

	@Override
	public Spliterator<BlockPos> spliterator() {
		return this.streamBlockPos().spliterator();
	}
}

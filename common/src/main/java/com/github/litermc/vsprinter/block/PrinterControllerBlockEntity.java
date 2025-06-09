package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.PrintArguments;
import com.github.litermc.vsprinter.api.PrintableSchematic;
import com.github.litermc.vsprinter.api.SchematicManager;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import org.joml.primitives.AABBi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PrinterControllerBlockEntity extends BlockEntity {
	private static final int MAX_RESOURCE_AMOUNT = 1024;

	private AABB frameCache = null;
	private final Object2IntMap<Item> items = new Object2IntOpenHashMap<>(8);
	private final List<ItemStack> nbtItems = new ArrayList<>();
	private PrintArguments printArgs = null;
	private PrintableSchematic blueprint = null;
	private Iterator<PrintableSchematic.BlockData> printing = null;
	private int progress = 0;

	public PrinterControllerBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), pos, state);
	}

	void invalidate() {
		this.frameCache = null;
	}

	/**
	 * put an item to the printer's storage
	 *
	 * @return {@code true} if action succeed, otherwise {@code false}
	 */
	public boolean putItemUnit(final ItemStack stack) {
		final CompoundTag tag = stack.getTag();
		if (tag == null || tag.isEmpty()) {
			this.items.computeInt(stack.getItem(), (i, v) -> (v == null ? 0 : v) + stack.getCount());
			return true;
		}
		for (final ItemStack s : this.nbtItems) {
			if (stack.getItem() == s.getItem() && tag.equals(s.getTag())) {
				s.grow(stack.getCount());
				return true;
			}
		}
		if (this.nbtItems.size() < 64) {
			this.nbtItems.add(stack);
			return true;
		}
		return false;
	}

	/**
	 * try consume an item
	 *
	 * @return {@code 0} if consume succeed, or the amount of unit missing
	 */
	protected int tryConsumeUnit(final ItemStack stack) {
		final CompoundTag tag = stack.getTag();
		if (tag == null || tag.isEmpty()) {
			final int count = this.items.getOrDefault(stack.getItem(), 0);
			final int remain = count - stack.getConut();
			if (remain < 0) {
				return -remain;
			}
			this.items.put(stack.getItem(), remain);
			return 0;
		}
		for (int i = 0; i < this.nbtItems.size(); i++) {
			final ItemStack s = this.nbtItems.get(i);
			if (stack.getItem() != s.getItem() || !tag.equals(s.getTag())) {
				continue;
			}
			final int remain = s.getCount() - stack.getCount();
			if (remain < 0) {
				return -remain;
			}
			s.setCount(remain);
			if (remain == 0) {
				final int lastIndex = this.nbtItems.size() - 1;
				this.nbtItems.set(i, this.nbtItems.get(lastIndex));
				this.nbtItems.remove(lastIndex);
			}
			return 0;
		}
		return stack.getCount();
	}

	@Override
	public void load(final CompoundTag data) {
		final CompoundTag items = data.getCompound("Items");
		this.items.clear();
		for (final String id : items.getAllKeys()) {
			final Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
			if (item == Items.AIR) {
				continue;
			}
			final int amount = items.getInt(id);
			if (amount > 0) {
				this.items.put(item, amount);
			}
		}
		final ListTag nbtItems = data.getList("NbtItems");
		this.nbtItems.clear();
		for (final Tag tag : nbtItems) {
			if (!(tag instanceof CompoundTag comp)) {
				continue;
			}
			final ItemStack stack = ItemStack.of(comp);
			if (!stack.isEmpty()) {
				this.nbtItems.add(stack);
			}
		}
		if (data.contains("PrintArgs")) {
			this.printArgs = PrintArguments.readFromNbt(data.getCompound("PrintArgs"));
		} else {
			this.printArgs = null;
		}
		if (data.contains("Blueprint")) {
			this.blueprint = SchematicManager.get().getSchematic(data.getString("Blueprint"));
			if (this.blueprint != null) {
				this.progress = data.getInt("Progress");
				this.printing = this.blueprint.stream().skip(this.progress).iterator();
			}
		}
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		final CompoundTag items = new CompoundTag();
		for (final Object2IntMap.Entry<Item> stack : this.items.object2IntEntrySet()) {
			final int amount = stack.getIntValue();
			if (amount > 0) {
				items.putInt(BuiltInRegistries.ITEM.getKey(stack.getKey()).toString(), amount);
			}
		}
		data.put("Items", items);
		final ListTag nbtItems = new ListTag();
		for (final ItemStack stack : this.nbtItems) {
			if (!stack.isEmpty()) {
				nbtItems.add(stack.save(new CompoundTag()));
			}
		}
		data.put("NbtItems", nbtItems);
		if (this.printArgs != null) {
			data.put("PrintArgs", this.printArgs.writeToNbt(new CompoundTag()));
		}
		if (this.blueprint != null) {
			data.putString("Blueprint", this.blueprint.getFingerprint());
			data.putInt("Progress", this.progress);
		}
	}

	public AABB getFrameSpace() {
		if (this.frameCache != null) {
			return this.frameCache;
		}
		final Direction frameDir = this.getBlockState().getValue(PrinterControllerBlock.FRAME).asDirection();
		if (frameDir == null) {
			return null;
		}
		final AABBi box = PrinterFrameBlock.getFrameBox(this.getLevel(), this.getBlockPos().relative(frameDir));
		this.frameCache = this.adjustFrame(box);
		return this.frameCache;
	}

	protected AABB adjustFrame(final AABBi box) {
		final int level = PrinterFrameBlock.getFrameLevel(box);
		final double frameSize = switch (level) {
			case 0 -> 1.0 / 16;
			case 1 -> 3.0 / 16;
			case 2 -> 7.0 / 16;
			default -> 1;
		};
		return new AABB(
			box.minX + frameSize, box.minY + frameSize, box.minZ + frameSize,
			box.maxX - frameSize, box.maxY - frameSize, box.maxZ - frameSize
		);
	}

	/**
	 * Start printing model
	 *
	 * @param arg The print argument
	 * @return start print error string, or {@code null} if action succeed
	 */
	public String startPrint(final PrintArguments args) {
		return null;
	}
}

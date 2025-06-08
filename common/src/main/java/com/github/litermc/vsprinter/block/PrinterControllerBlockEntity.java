package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.PrintArguments;
import com.github.litermc.vsprinter.api.PrintSchema;

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
	private PrintSchema blueprint = null;
	private Iterator<PrintSchema.BlockData> printing = null;
	private int progress = 0;

	public PrinterControllerBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), pos, state);
	}

	void invalidate() {
		this.frameCache = null;
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
		if (data.contains("PrintArgs")) {
			this.printArgs = PrintArguments.readFromNbt(data.getCompound("PrintArgs"));
		} else {
			this.printArgs = null;
		}
		if (data.contains("Blueprint")) {
			this.blueprint = this.loadBluePrint(data.getString("Blueprint"));
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

	public PrintSchema loadBluePrint(final String name) {
		return null;
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

package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.Constants;
import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.ISchematicDataBlockEntity;
import com.github.litermc.vsprinter.api.PrintArguments;
import com.github.litermc.vsprinter.api.PrintPlugin;
import com.github.litermc.vsprinter.api.PrintStatus;
import com.github.litermc.vsprinter.api.PrintableSchematic;
import com.github.litermc.vsprinter.api.SchematicManager;
import com.github.litermc.vsprinter.api.StackUtil;
import com.github.litermc.vsprinter.block.property.NullableDirection;
import com.github.litermc.vsprinter.item.QuantumFilmItem;
import com.github.litermc.vsprinter.ship.ShipPrintedInfoPlugin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBi;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class PrinterControllerBlockEntity extends BlockEntity implements Clearable {
	private static final int MAX_RESOURCE_TYPE = 128;
	private static final int MAX_RESOURCE_AMOUNT = 1024 * StackUtil.UNIT;

	private final List<PrintPlugin> plugins = new ArrayList<>();

	private final Object2IntSortedMap<Item> items = new Object2IntAVLTreeMap<>((a, b) -> {
		return Integer.compare(Item.getId(a), Item.getId(b));
	});
	private final List<ItemStack> nbtItems = new ArrayList<>();

	private PrintArguments printArgs = PrintArguments.DEFAULT;
	protected PrintStatus status = PrintStatus.UNCONSTRUCTED;

	private ItemStack blueprintItem = ItemStack.EMPTY;
	private PrintableSchematic blueprint = null;
	private Iterator<PrintableSchematic.BlockData> printing = null;
	private Queue<ItemStack> pendingItems = null;
	private ItemStack requiredItem = ItemStack.EMPTY;
	private int progress = 0;

	private int energyStored = 0;

	private int frameCheckCountdown = 0;
	private AABB frameCache = null;
	private int frameLevel = -1;
	private int lastSignal = 0;

	public PrinterControllerBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), pos, state);

		this.plugins.add(ShipPrintedInfoPlugin.INSTANCE);
	}

	public PrintArguments getPrintArgs() {
		return this.printArgs;
	}

	/**
	 * Client only
	 */
	public void sendPrintArgs(final PrintArguments args) {
		this.setPrintArgs(args);
		final CompoundTag data = this.printArgs.writeToNbt(new CompoundTag());
		//
	}

	public void setPrintArgs(final PrintArguments args) {
		this.printArgs = args.validated();
		this.markUpdated();
	}

	public PrintStatus getStatus() {
		return this.status;
	}

	void setStatus(final PrintStatus status) {
		if (this.status == status) {
			return;
		}
		this.status = status;
		this.markUpdated();
	}

	public ItemStack getBlueprintItem() {
		return this.blueprintItem;
	}

	public ItemStack takeBlueprintItem() {
		final ItemStack item = this.blueprintItem;
		this.blueprintItem = ItemStack.EMPTY;
		this.setChanged();
		return item;
	}

	public boolean putBlueprintItem(final ItemStack item) {
		if (!this.blueprintItem.isEmpty()) {
			return false;
		}
		if (item.getCount() != 1) {
			return false;
		}
		if (QuantumFilmItem.getBlueprint(item) == null) {
			return false;
		}
		this.blueprintItem = item;
		this.setChanged();
		return true;
	}

	public ItemStack getRequiredItem() {
		return this.requiredItem;
	}

	protected void markUpdated() {
		this.setChanged();
		this.getLevel().sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
	}

	void invalidate() {
		this.frameCache = null;
		this.frameLevel = -1;
		this.blueprint = null;
		this.printing = null;
		this.pendingItems = null;
		this.requiredItem = ItemStack.EMPTY;
		this.progress = 0;
		this.markUpdated();
	}

	protected int getScaledCount(int count) {
		final double scale = this.printArgs.scale() * this.getPrinterScale();
		return (int) (Math.ceil(count * (scale * scale * scale)));
	}

	/**
	 * put an item to the printer's storage
	 *
	 * @return The remaining units
	 */
	public ItemStack putItemUnit(final ItemStack stack) {
		return this.putItemUnit(stack, false);
	}

	public ItemStack putItemUnit(final ItemStack stack, final boolean simulate) {
		if (stack.isEmpty()) {
			return stack;
		}
		final Item item = stack.getItem();
		final CompoundTag tag = stack.getTag();
		final boolean canPutMore = this.nbtItems.size() + this.items.size() < MAX_RESOURCE_TYPE;
		if (tag == null || tag.isEmpty()) {
			final int count = this.items.getOrDefault(item, 0);
			if (count == 0 && !canPutMore) {
				return stack;
			}
			final int newCount = Math.min(count + stack.getCount(), MAX_RESOURCE_AMOUNT);
			stack.shrink(newCount - count);
			if (!simulate) {
				this.items.put(item, newCount);
			}
			return stack;
		}
		for (final ItemStack s : this.nbtItems) {
			if (item == s.getItem() && tag.equals(s.getTag())) {
				final int newCount = Math.min(s.getCount() + stack.getCount(), MAX_RESOURCE_AMOUNT);
				stack.shrink(newCount - s.getCount());
				if (!simulate) {
					s.setCount(newCount);
				}
				return stack;
			}
		}
		if (!canPutMore) {
			return stack;
		}
		if (!simulate) {
			this.nbtItems.add(stack);
		}
		return ItemStack.EMPTY;
	}

	/**
	 * try consume an item
	 *
	 * @return {@code 0} if consume succeed, or the amount of unit missing
	 */
	protected int tryConsumeUnit(final ItemStack stack) {
		final int needs = this.getScaledCount(stack.getCount());
		final CompoundTag tag = stack.getTag();
		if (tag == null || tag.isEmpty()) {
			final int count = this.items.getOrDefault(stack.getItem(), 0);
			final int remain = count - needs;
			if (remain < 0) {
				return -remain;
			}
			if (remain == 0) {
				this.items.removeInt(stack.getItem());
			} else {
				this.items.put(stack.getItem(), remain);
			}
			return 0;
		}
		for (int i = 0; i < this.nbtItems.size(); i++) {
			final ItemStack s = this.nbtItems.get(i);
			if (stack.getItem() != s.getItem() || !tag.equals(s.getTag())) {
				continue;
			}
			final int remain = s.getCount() - needs;
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
		return needs;
	}

	@Override
	public void clearContent() {
		this.blueprintItem = ItemStack.EMPTY;
		this.items.clear();
		this.nbtItems.clear();
	}

	public int getContainerSize() {
		return 2 + this.items.size() + this.nbtItems.size();
	}

	public ItemStack getItem(int slot) {
		if (slot == 0) {
			return this.getBlueprintItem();
		}
		slot -= 2;
		if (slot < 0) {
			return ItemStack.EMPTY;
		}
		final int nbtItemsSize = this.nbtItems.size();
		if (slot < nbtItemsSize) {
			return this.nbtItems.get(slot);
		}
		slot -= nbtItemsSize;
		return this.items.object2IntEntrySet().stream().skip(slot).findFirst().map((entry) -> new ItemStack(entry.getKey(), entry.getValue())).orElse(ItemStack.EMPTY);
	}

	public int getMaxStackSize() {
		return MAX_RESOURCE_AMOUNT / StackUtil.UNIT;
	}

	public int receiveEnergy(final int maxReceive, final boolean simulate) {
		final int avaliable = this.getMaxEnergyStored() - this.energyStored;
		if (avaliable <= 0) {
			return 0;
		}
		if (!simulate) {
			this.energyStored += Math.min(avaliable, maxReceive);
		}
		return Math.max(maxReceive - avaliable, 0);
	}

	public int getEnergyStored() {
		return this.energyStored;
	}

	/**
	 * Return the energy use to print one block.
	 * The actualy energy usage will be scaled based on the scale difference.
	 *
	 * @return the energy use to print standard block.
	 */
	public int getEnergyConsumeRate() {
		// TODO: config
		return 1024;
	}

	public int getMaxEnergyStored() {
		return this.getEnergyConsumeRate() * 2;
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
		final ListTag nbtItems = data.getList("NbtItems", Tag.TAG_COMPOUND);
		this.nbtItems.clear();
		for (final Tag tag : nbtItems) {
			final ItemStack stack = ItemStack.of((CompoundTag) (tag));
			if (!stack.isEmpty()) {
				this.nbtItems.add(stack);
			}
		}
		if (data.contains("PrintArgs")) {
			this.printArgs = PrintArguments.readFromNbt(data.getCompound("PrintArgs"));
		} else {
			this.printArgs = PrintArguments.DEFAULT;
		}
		this.status = PrintStatus.values()[data.getByte("Status")];
		this.blueprintItem = data.contains("BlueprintItem") ? ItemStack.of(data.getCompound("BlueprintItem")) : ItemStack.EMPTY;
		if (data.contains("Blueprint")) {
			this.blueprint = SchematicManager.get().getSchematic(data.getString("Blueprint"));
			if (this.blueprint != null) {
				this.printing = this.blueprint.stream().skip(this.progress).iterator();
				this.pendingItems = new ArrayDeque<>();
				for (final Tag tag : data.getList("PendingItems", Tag.TAG_COMPOUND)) {
					final ItemStack stack = ItemStack.of((CompoundTag) (tag));
					if (!stack.isEmpty()) {
						this.pendingItems.add(stack);
					}
				}
			}
		}
		this.progress = data.getInt("Progress");
		this.requiredItem = data.contains("RequiredItem") ? ItemStack.of(data.getCompound("RequiredItem")) : ItemStack.EMPTY;
		this.lastSignal = data.getInt("LastSignal");
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
		if (this.blueprint != null) {
			data.putString("Blueprint", this.blueprint.getFingerprint());
			if (this.pendingItems != null) {
				final ListTag pendingItems = new ListTag();
				for (final ItemStack stack : this.pendingItems) {
					if (!stack.isEmpty()) {
						pendingItems.add(stack.save(new CompoundTag()));
					}
				}
				data.put("PendingItems", pendingItems);
			}
		}
		data.putInt("LastSignal", this.lastSignal);
		this.saveShared(data);
	}

	protected void saveShared(final CompoundTag data) {
		data.put("PrintArgs", this.printArgs.writeToNbt(new CompoundTag()));
		data.putByte("Status", (byte) (this.status.ordinal()));
		if (!this.blueprintItem.isEmpty()) {
			data.put("BlueprintItem", this.blueprintItem.save(new CompoundTag()));
		}
		if (this.blueprint != null) {
			data.putInt("Progress", this.progress);
			if (!this.requiredItem.isEmpty()) {
				data.put("RequiredItem", this.requiredItem.save(new CompoundTag()));
			}
		}
	}

	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag data = super.getUpdateTag();
		this.saveShared(data);
		return data;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	/**
	 * get the scale of the printer's ship, if its on one, or 1.0 if the printer is not on a ship.
	 */
	public double getPrinterScale() {
		final Ship ship = VSGameUtilsKt.getShipManagingPos(this.getLevel(), this.getBlockPos());
		if (ship == null) {
			return 1;
		}
		return Math.sqrt(ship.getTransform().getShipToWorldScaling().lengthSquared() / 3);
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

		if (box == null) {
			this.frameLevel = -1;
		} else {
			this.frameLevel = this.calcFrameLevel(box);
			this.frameCache = this.adjustFrame(box, this.frameLevel);
		}
		return this.frameCache;
	}

	public int calcFrameLevel(final AABBi box) {
		return PrinterFrameBlock.getFrameLevel(box);
	}

	protected AABB adjustFrame(final AABBi box, final int level) {
		final double frameSize = 0.01 + switch (level) {
			case 0 -> 1.0 / 16;
			case 1 -> 3.0 / 16;
			case 2 -> 7.0 / 16;
			default -> 1;
		};
		return new AABB(
			box.minX + frameSize, box.minY + frameSize, box.minZ + frameSize,
			box.maxX + 1 - frameSize, box.maxY + 1 - frameSize, box.maxZ + 1 - frameSize
		);
	}

	// @Override forge only
	public AABB getRenderBoundingBox() {
		AABB box = new AABB(this.getBlockPos());
		final AABB frameBox = this.getFrameSpace();
		if (frameBox != null) {
			box = box.minmax(frameBox);
		}
		return box;
	}

	public void neighborChanged(final BlockPos neighborPos) {
		if (!(this.getLevel() instanceof ServerLevel level)) {
			return;
		}
		final int newSignal = level.getBestNeighborSignal(this.getBlockPos());
		if (newSignal > 0 && this.lastSignal == 0 && this.blueprint == null) {
			this.startPrint(SchematicManager.get().getSchematic(QuantumFilmItem.getBlueprint(this.blueprintItem)));
		}
		this.lastSignal = newSignal;
	}

	public void serverTick() {
		final ServerLevel level = (ServerLevel) this.getLevel();
		if (this.status == PrintStatus.UNCONSTRUCTED) {
			if (this.frameCheckCountdown <= 0) {
				if (this.getBlockState().getValue(PrinterControllerBlock.FRAME) != NullableDirection.NULL && this.getFrameSpace() != null) {
					this.setStatus(PrintStatus.IDLE);
				} else {
					this.frameCheckCountdown = 20;
				}
			} else {
				this.frameCheckCountdown--;
			}
		}
		if (this.printing == null) {
			return;
		}
		if (this.pendingItems == null) {
			final int newEnergy = this.energyStored - this.getEnergyConsumeRate();
			if (newEnergy < 0) {
				this.setStatus(PrintStatus.REQUIRE_ENERGY);
				return;
			}
			this.energyStored = newEnergy;
			if (this.canFinishPrint()) {
				this.finishPrint();
				return;
			}
			final List<ItemStack> itemsList = this.printing.next().requiredItems();
			if (itemsList == null) {
				this.setStatus(PrintStatus.INVALID);
				return;
			}
			this.pendingItems = new ArrayDeque<>(itemsList);
		}
		final AABB frame = this.getFrameSpace();
		if (frame == null) {
			this.setStatus(PrintStatus.UNCONSTRUCTED);
			return;
		}
		final AABBd framed = new AABBd(frame.minX, frame.minY, frame.minZ, frame.maxX, frame.maxY, frame.maxZ);
		if (!level.noCollision(frame)) {
			this.setStatus(PrintStatus.OBSTRUCTED);
			return;
		}
		final AABBd frameWorld = new AABBd(framed);
		final Matrix4d ship2world = new Matrix4d();
		final Ship selfShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
		if (selfShip != null) {
			ship2world.set(selfShip.getShipToWorld());
			frameWorld.transform(ship2world);
			if (!level.noCollision(new AABB(frameWorld.minX, frameWorld.minY, frameWorld.minZ, frameWorld.maxX, frameWorld.maxY, frameWorld.maxZ))) {
				this.setStatus(PrintStatus.OBSTRUCTED);
				return;
			}
		}
		final Matrix4d aabbMat = new Matrix4d();
		final AABBd convertedFrame = new AABBd();
		for (final Ship ship : VSGameUtilsKt.getShipsIntersecting(level, frameWorld)) {
			if (ship == selfShip) {
				continue;
			}
			framed.transform(ship2world.mul(ship.getWorldToShip(), aabbMat), convertedFrame);
			// TODO: not accurate enough
			if (!level.noCollision(new AABB(convertedFrame.minX, convertedFrame.minY, convertedFrame.minZ, convertedFrame.maxX, convertedFrame.maxY, convertedFrame.maxZ))) {
				this.setStatus(PrintStatus.OBSTRUCTED);
				return;
			}
		}
		while (true) {
			final ItemStack need = this.pendingItems.peek();
			if (need == null) {
				this.pendingItems = null;
				this.requiredItem = ItemStack.EMPTY;
				this.progress++;
				this.markUpdated();
				return;
			}
			final int require = this.tryConsumeUnit(need);
			if (require > 0) {
				this.onRequireItem(need.copyWithCount(require));
				return;
			}
			this.pendingItems.remove();
			this.setStatus(PrintStatus.WORKING);
		}
	}

	protected void onRequireItem(final ItemStack stack) {
		if (!ItemStack.isSameItemSameTags(this.requiredItem, stack)) {
			this.requiredItem = stack;
			this.markUpdated();
		}
		this.setStatus(PrintStatus.REQUIRE_MATERIAL);
	}

	/**
	 * Start printing model
	 *
	 * @param arg The print argument
	 */
	public void startPrint(final PrintableSchematic blueprint) {
		this.status = PrintStatus.IDLE;
		if (blueprint == null) {
			this.setStatus(PrintStatus.EMPTY_BLUEPRINT);
			return;
		}
		final ServerLevel level = (ServerLevel) (this.getLevel());
		final AABB frame = this.getFrameSpace();
		if (frame == null) {
			this.setStatus(PrintStatus.UNCONSTRUCTED);
			return;
		}

		final double scale = this.printArgs.scale() * this.getPrinterScale();

		if (scale < 0.1) {
			this.setStatus(PrintStatus.SCALE_TOO_SMALL);
			return;
		}
		if (scale > 10) {
			this.setStatus(PrintStatus.SCALE_TOO_LARGE);
			return;
		}

		final Vec3i blueprintDimension = blueprint.getDimension();
		final Vec3 dimension = Vec3.atLowerCornerOf(blueprintDimension)
			.scale(this.printArgs.scale())
			.directionFromRotation((float) (this.printArgs.xRotate()) * 90f, (float) (this.printArgs.yRotate()) * 90f);
		if (frame.getXsize() < Math.abs(dimension.x) || frame.getYsize() < Math.abs(dimension.y) || frame.getZsize() < Math.abs(dimension.z)) {
			this.setStatus(PrintStatus.NOT_ENOUGH_SPACE);
			return;
		}

		this.blueprint = blueprint;
		this.printing = this.blueprint.stream().iterator();
		this.pendingItems = null;
		this.progress = 0;
		this.markUpdated();
	}

	public boolean canFinishPrint() {
		return !this.printing.hasNext();
	}

	public ServerShip finishPrint() {
		final PrintableSchematic blueprint = this.blueprint;
		this.blueprint = null;
		this.printing = null;
		this.pendingItems = null;
		this.progress = 0;
		this.markUpdated();

		if (blueprint == null) {
			Constants.LOG.warn("Trying to print without a blueprint");
			return null;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final AABB frame = this.getFrameSpace();
		if (frame == null) {
			Constants.LOG.warn("Trying to print without a frame");
			return null;
		}
		final double relativeScale = this.printArgs.scale();
		final double scale = relativeScale * this.getPrinterScale();

		final Vec3i blueprintDimension = blueprint.getDimension();
		final float xRotate = (float) (this.printArgs.xRotate()) * 90f, yRotate = (float) (this.printArgs.yRotate()) * 90f;
		final Vec3 dimension = Vec3.atLowerCornerOf(blueprintDimension)
			.scale(relativeScale)
			.xRot(xRotate * Mth.DEG_TO_RAD)
			.yRot(yRotate * Mth.DEG_TO_RAD);
		final Vector3d worldOrigin = new Vector3d(
			this.printArgs.xAlign().align(frame.minX, frame.maxX, dimension.x),
			this.printArgs.yAlign().align(frame.minY, frame.maxY, dimension.y),
			this.printArgs.zAlign().align(frame.minZ, frame.maxZ, dimension.z)
		);

		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final ServerShip ship = shipWorld.createNewShipAtBlock(
			new Vector3i((int) (worldOrigin.x), (int) (worldOrigin.y), (int) (worldOrigin.z)),
			false,
			scale,
			levelId
		);
		ship.setSlug("+printed+" + blueprint.getFingerprint().substring(0, 8) + "+" + ship.getId());
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i shipOrigin = shipCenter.sub(blueprintDimension.getX() / 2, blueprintDimension.getY() / 2, blueprintDimension.getZ() / 2, new Vector3i());

		final List<ISchematicDataBlockEntity> dataBlocks = blueprint.placeInLevel(level, new BlockPos(shipOrigin.x, shipOrigin.y, shipOrigin.z));

		// TODO: this may not work correct on scaled ship
		final Vector3d absPosition = worldOrigin
			.add(
				ship.getInertiaData().getCenterOfMassInShip()
					.sub(shipOrigin.x - 0.5, shipOrigin.y - 0.5, shipOrigin.z - 0.5, new Vector3d())
					.mul(scale),
				new Vector3d()
			);
		final Vector3d position = new Vector3d(absPosition);
		final Quaterniond rotation = new Quaterniond();
		if (xRotate != 0) {
			rotation.rotationX(xRotate * Mth.DEG_TO_RAD);
			if (yRotate != 0) {
				rotation.rotateY(yRotate * Mth.DEG_TO_RAD);
			}
		} else if (yRotate != 0) {
			rotation.rotationY(yRotate * Mth.DEG_TO_RAD);
		}
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		final Vector3d scaling = new Vector3d(relativeScale);

		final ServerShip selfShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
		if (selfShip != null) {
			final ShipTransform selfTransform = selfShip.getTransform();
			selfTransform.getShipToWorld().transformPosition(position);
			rotation.set(selfTransform.getShipToWorldRotation());
			velocity.set(selfShip.getVelocity());
			omega.set(selfShip.getOmega());
			scaling.mul(selfTransform.getShipToWorldScaling());
		}
		shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, levelId, scale));

		// fix new ship's velocity and omega
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
					if (!transform.getPositionInWorld().equals(nextTransform.getPositionInWorld()) || !transform.getShipToWorldRotation().equals(nextTransform.getShipToWorldRotation())) {
						ship.setTransformProvider(null);
						return null;
					}
					if (ship.getVelocity().lengthSquared() == 0 && ship.getOmega().lengthSquared() == 0) {
						if (selfShip != null) {
							final ShipTransform selfTransform2 = selfShip.getTransform();
							selfTransform2.getShipToWorld().transformPosition(absPosition, position);
							rotation.set(selfTransform2.getShipToWorldRotation());
							velocity.set(selfShip.getVelocity());
							omega.set(selfShip.getOmega());
							scaling.set(relativeScale).mul(selfTransform2.getShipToWorldScaling());
						}
						return new NextTransformAndVelocityData(new ShipTransformImpl(position, nextTransform.getPositionInShip(), rotation, scaling), velocity, omega);
					}
					return null;
				}
			});
		}

		for (final ISchematicDataBlockEntity dataBlock : dataBlocks) {
			dataBlock.onPlacedByShip(selfShip, ship);
		}

		for (final PrintPlugin plugin : this.plugins) {
			plugin.onShipFinish(this, ship);
		}

		this.setStatus(PrintStatus.IDLE);
		return ship;
	}
}

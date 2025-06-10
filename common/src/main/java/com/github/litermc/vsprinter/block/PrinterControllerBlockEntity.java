package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.PrintArguments;
import com.github.litermc.vsprinter.api.PrintableSchematic;
import com.github.litermc.vsprinter.api.SchematicManager;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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

import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBi;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
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

public class PrinterControllerBlockEntity extends BlockEntity {
	private static final int MAX_RESOURCE_AMOUNT = 1024;

	private AABB frameCache = null;
	private final Object2IntMap<Item> items = new Object2IntOpenHashMap<>(8);
	private final List<ItemStack> nbtItems = new ArrayList<>();
	private PrintArguments printArgs = PrintArguments.DEFAULT;
	private PrintableSchematic blueprint = null;
	private Iterator<PrintableSchematic.BlockData> printing = null;
	private Queue<ItemStack> pendingItems = null;
	private int progress = 0;

	public PrinterControllerBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), pos, state);
	}

	public PrintArguments getPrintArgs() {
		return this.printArgs;
	}

	public void setPrintArgs(final PrintArguments args) {
		this.printArgs = args.validated();
		this.setChanged();
	}

	void invalidate() {
		this.frameCache = null;
		this.blueprint = null;
		this.printing = null;
		this.pendingItems = null;
		this.progress = 0;
		this.setChanged();
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
			final int remain = count - stack.getCount();
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
		final ListTag nbtItems = data.getList("NbtItems", Tag.TAG_COMPOUND);
		this.nbtItems.clear();
		for (final Tag tag : nbtItems) {
			final ItemStack stack = ItemStack.of((CompoundTag) (tag));
			if (!stack.isEmpty()) {
				this.nbtItems.add(stack);
			}
		}
		if (data.contains("PrintArgs")) {
			this.setPrintArgs(PrintArguments.readFromNbt(data.getCompound("PrintArgs")));
		} else {
			this.setPrintArgs(PrintArguments.DEFAULT);
		}
		if (data.contains("Blueprint")) {
			this.blueprint = SchematicManager.get().getSchematic(data.getString("Blueprint"));
			if (this.blueprint != null) {
				this.progress = data.getInt("Progress");
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
		data.put("PrintArgs", this.printArgs.writeToNbt(new CompoundTag()));
		if (this.blueprint != null) {
			data.putString("Blueprint", this.blueprint.getFingerprint());
			data.putInt("Progress", this.progress);
			final ListTag pendingItems = new ListTag();
			while (true) {
				final ItemStack stack = this.pendingItems.poll();
				if (stack == null) {
					break;
				}
				if (!stack.isEmpty()) {
					pendingItems.add(stack.save(new CompoundTag()));
				}
			}
			data.put("PendingItems", pendingItems);
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
			box.maxX + 1 - frameSize, box.maxY + 1 - frameSize, box.maxZ + 1 - frameSize
		);
	}

	public void serverTick() {
		if (this.printing == null) {
			return;
		}
		if (this.pendingItems == null || this.pendingItems.isEmpty()) {
			if (!this.printing.hasNext()) {
				this.finishPrint();
				return;
			}
			this.pendingItems = new ArrayDeque<>(this.printing.next().requiredItems());
		}
		while (true) {
			final ItemStack need = this.pendingItems.peek();
			if (need == null) {
				this.pendingItems = null;
				this.progress++;
				this.setChanged();
				return;
			}
			final int require = this.tryConsumeUnit(need);
			if (require > 0) {
				this.onRequireItem(need, require);
				return;
			}
			this.pendingItems.remove();
		}
	}

	protected void onRequireItem(final ItemStack item, final int amount) {
		// TODO: send notification to client
		this.setChanged();
	}

	/**
	 * Start printing model
	 *
	 * @param arg The print argument
	 * @return start print error string, or {@code null} if action succeed
	 */
	public String startPrint(final PrintableSchematic blueprint) {
		this.blueprint = blueprint;
		this.printing = this.blueprint.stream().iterator();
		this.pendingItems = null;
		this.progress = 0;
		this.setChanged();
		return null;
	}

	public void finishPrint() {
		this.printing = null;
		this.pendingItems = null;
		this.progress = 0;
		this.setChanged();

		if (this.blueprint == null) {
			return;
		}

		final ServerLevel level = (ServerLevel) (this.getLevel());
		final AABB frame = this.getFrameSpace();
		final Vec3i blueprintDimension = this.blueprint.getDimension();
		final Vec3 dimension = Vec3.atLowerCornerOf(blueprintDimension)
			.scale(this.printArgs.scale())
			.directionFromRotation((float) (this.printArgs.xRotate()) * 90f, (float) (this.printArgs.yRotate()) * 90f);
		final Vec3 worldOrigin = new Vec3(
			this.printArgs.xAlign().align(frame.minX, frame.maxX, dimension.x),
			this.printArgs.yAlign().align(frame.minY, frame.maxY, dimension.y),
			this.printArgs.zAlign().align(frame.minZ, frame.maxZ, dimension.z)
		);
		final double relativeScale = this.printArgs.scale();

		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final ServerShip ship = shipWorld.createNewShipAtBlock(
			new Vector3i((int) (worldOrigin.x), (int) (worldOrigin.y), (int) (worldOrigin.z)),
			false,
			this.printArgs.scale(),
			levelId
		);
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i shipOrigin = shipCenter.sub(blueprintDimension.getX() / 2, blueprintDimension.getY() / 2, blueprintDimension.getZ() / 2, new Vector3i());
		this.blueprint.placeInLevel(level, new BlockPos(shipOrigin.x, shipOrigin.y, shipOrigin.z));

		final Vector3d absPosition = ship.getTransform().getPositionInWorld()
			.add(ship.getInertiaData().getCenterOfMassInShip(), new Vector3d())
			.sub(shipOrigin.x, shipOrigin.y, shipOrigin.z)
			.add(worldOrigin.x - (int) (worldOrigin.x), worldOrigin.y - (int) (worldOrigin.y), worldOrigin.z - (int) (worldOrigin.z));
		final Vector3d position = new Vector3d(absPosition);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		double scale = relativeScale;
		final Vector3d scaling = new Vector3d(scale);

		final ServerShip selfShip = VSGameUtilsKt.getShipManagingPos(level, this.getBlockPos());
		if (selfShip != null) {
			final ShipTransform selfTransform = selfShip.getTransform();
			selfTransform.getShipToWorld().transformPosition(position);
			rotation.set(selfTransform.getShipToWorldRotation());
			velocity.set(selfShip.getVelocity());
			omega.set(selfShip.getOmega());
			scaling.mul(selfTransform.getShipToWorldScaling());
			scale *= Math.sqrt(scaling.lengthSquared() / 3);
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

		this.onFinishPrint(ship);
	}

	protected void onFinishPrint(final ServerShip ship) {
		//
	}
}

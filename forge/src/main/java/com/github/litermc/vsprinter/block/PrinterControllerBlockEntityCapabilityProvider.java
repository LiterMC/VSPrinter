package com.github.litermc.vsprinter.block;

import com.github.litermc.vsprinter.Constants;
import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.api.StackUtil;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;

public final class PrinterControllerBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "printer_controller");

	private final PrinterControllerBlockEntity be;
	private final LazyOptional<IItemHandler> itemHandler;
	private final LazyOptional<IEnergyStorage> energyStorage;

	private PrinterControllerBlockEntityCapabilityProvider(final PrinterControllerBlockEntity be) {
		this.be = be;
		this.itemHandler = LazyOptional.of(() -> new ItemHandler(this.be));
		this.energyStorage = LazyOptional.of(() -> new EnergyStorage(this.be));
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.ITEM_HANDLER) {
			return this.itemHandler.cast();
		} else if (cap == ForgeCapabilities.ENERGY) {
			return this.energyStorage.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.itemHandler.invalidate();
		this.energyStorage.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<PrinterControllerBlockEntity> event) {
		final PrinterControllerBlockEntityCapabilityProvider provider = new PrinterControllerBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class ItemHandler implements IItemHandler {
		private final PrinterControllerBlockEntity be;

		private ItemHandler(final PrinterControllerBlockEntity be) {
			this.be = be;
		}

		@Override
		public int getSlots() {
			return this.be.getContainerSize();
		}

		@Override
		public ItemStack getStackInSlot(final int slot) {
			return this.be.getItem(slot);
		}

		@Override
		public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
			if (slot == 0) {
				if (simulate) {
					return this.be.getBlueprintItem().isEmpty() && stack.is(VSPRegistry.Items.QUANTUM_FILM.get()) && stack.getCount() == 1 ? ItemStack.EMPTY : stack;
				}
				return this.be.putBlueprintItem(stack) ? ItemStack.EMPTY : stack;
			}
			if (slot != 1) {
				return stack;
			}
			if (!simulate) {
				this.be.setChanged();
			}
			return StackUtil.setUnitsToStack(this.be.putItemUnit(StackUtil.convertStackToUnits(stack), simulate));
		}

		@Override
		public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
			if (slot == 0) {
				if (amount <= 0) {
					return ItemStack.EMPTY;
				}
				if (simulate) {
					return this.be.getBlueprintItem();
				}
				this.be.setChanged();
				return this.be.takeBlueprintItem();
			}
			return ItemStack.EMPTY;
		}

		@Override
		public int getSlotLimit(final int slot) {
			return slot == 0 ? 1 : this.be.getMaxStackSize();
		}

		@Override
		public boolean isItemValid(final int slot, final ItemStack stack) {
			if (slot == 0) {
				return this.be.getBlueprintItem().isEmpty() && stack.is(VSPRegistry.Items.QUANTUM_FILM.get()) && stack.getCount() == 1;
			}
			return slot == 1;
		}
	}

	private static final class EnergyStorage implements IEnergyStorage {
		private final PrinterControllerBlockEntity be;

		private EnergyStorage(final PrinterControllerBlockEntity be) {
			this.be = be;
		}

		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			return this.be.receiveEnergy(maxReceive, simulate);
		}

		public int extractEnergy(final int maxExtract, final boolean simulate) {
			return 0;
		}

		public int getEnergyStored() {
			return this.be.getEnergyStored();
		}

		public int getMaxEnergyStored() {
			return this.be.getMaxEnergyStored();
		}

		public boolean canExtract() {
			return false;
		}

		public boolean canReceive() {
			return true;
		}
	}
}

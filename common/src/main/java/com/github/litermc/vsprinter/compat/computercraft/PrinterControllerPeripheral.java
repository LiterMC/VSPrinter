package com.github.litermc.vsprinter.compat.computercraft;

import com.github.litermc.vsprinter.api.Alignment;
import com.github.litermc.vsprinter.api.PrintStatus;
import com.github.litermc.vsprinter.block.PrinterControllerBlockEntity;
import com.github.litermc.vsprinter.item.QuantumFilmItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PrinterControllerPeripheral implements IPeripheral {
	private static final Set<String> ADDTIONAL_TYPES = Set.of("energy_storage");

	protected final PrinterControllerBlockEntity be;
	protected final AttachedComputerSet computers = new AttachedComputerSet();

	public PrinterControllerPeripheral(final PrinterControllerBlockEntity be) {
		this.be = be;
	}

	@Override
	public String getType() {
		return "vsprinter_controller";
	}

	@Override
	public Object getTarget() {
		return this.be;
	}

	@Override
	public Set<String> getAdditionalTypes() {
		return ADDTIONAL_TYPES;
	}

	@Override
	public void attach(final IComputerAccess computer) {
		this.computers.add(computer);
	}

	@Override
	public void detach(final IComputerAccess computer) {
		this.computers.remove(computer);
	}

	@LuaFunction
	public final MethodResult getStatus() {
		final PrintStatus status = this.be.getStatus();
		final boolean isWorking = this.be.isWorking();
		final boolean isError = status != PrintStatus.IDLE && status != PrintStatus.WORKING;
		return MethodResult.of(status.getId(), isWorking, isError);
	}

	@LuaFunction
	public final double getProgress() {
		return this.be.getProgress();
	}

	@LuaFunction
	public final double getScale() {
		return this.be.getPrintArgs().scale();
	}

	@LuaFunction
	public final void setScale(final double scale) {
		this.be.setPrintArgs(this.be.getPrintArgs().withScale((float) (scale)));
	}

	@LuaFunction
	public final int getXAlign() {
		return this.be.getPrintArgs().xAlign().getRepresentative();
	}

	@LuaFunction
	public final void setXAlign(final int representative) throws LuaException {
		if (representative != -1 && representative != 0 && representative != 1) {
			throw new LuaException("argument #1 (alignment) can only be -1, 0, or 1, got " + representative);
		}
		this.be.setPrintArgs(this.be.getPrintArgs().withXAlign(Alignment.fromRepresentative(representative)));
	}

	@LuaFunction
	public final int getYAlign() {
		return this.be.getPrintArgs().yAlign().getRepresentative();
	}

	@LuaFunction
	public final void setYAlign(final int representative) throws LuaException {
		if (representative != -1 && representative != 0 && representative != 1) {
			throw new LuaException("argument #1 (alignment) can only be -1, 0, or 1, got " + representative);
		}
		this.be.setPrintArgs(this.be.getPrintArgs().withYAlign(Alignment.fromRepresentative(representative)));
	}

	@LuaFunction
	public final int getZAlign() {
		return this.be.getPrintArgs().zAlign().getRepresentative();
	}

	@LuaFunction
	public final void setZAlign(final int representative) throws LuaException {
		if (representative != -1 && representative != 0 && representative != 1) {
			throw new LuaException("argument #1 (alignment) can only be -1, 0, or 1, got " + representative);
		}
		this.be.setPrintArgs(this.be.getPrintArgs().withZAlign(Alignment.fromRepresentative(representative)));
	}

	@LuaFunction
	public final int getXRotate() {
		return this.be.getPrintArgs().xRotate();
	}

	@LuaFunction
	public final void setXRotate(final int xRotate) {
		this.be.setPrintArgs(this.be.getPrintArgs().withXRotate(xRotate));
	}

	@LuaFunction
	public final int getYRotate() {
		return this.be.getPrintArgs().yRotate();
	}

	@LuaFunction
	public final void setYRotate(final int yRotate) {
		this.be.setPrintArgs(this.be.getPrintArgs().withYRotate(yRotate));
	}

	@LuaFunction(mainThread = true)
	public final Map<String, Object> getBlueprintItem() {
		final ItemStack blueprint = this.be.getBlueprintItem();
		if (blueprint.isEmpty()) {
			return null;
		}
		final Map<String, Object> result = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getDetails(blueprint));
		result.put("blueprintId", QuantumFilmItem.getBlueprint(blueprint));
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<String, Object> getRequiredItem() {
		final ItemStack stack = this.be.getRequiredItem();
		if (stack.isEmpty()) {
			return null;
		}
		final Map<String, Object> result = new HashMap<>(VanillaDetailRegistries.ITEM_STACK.getBasicDetails(stack));
		result.put("units", result.remove("count"));
		return result;
	}

	@LuaFunction(mainThread = true)
	public final Map<String, ?> getItemUnits() {
		return this.be.getItemUnits().object2IntEntrySet().stream()
			.collect(Collectors.toMap(
				(entry) -> BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(),
				(entry) -> Integer.valueOf(entry.getIntValue())
			));
	}

	@LuaFunction(mainThread = true)
	public final List<Map<String, Object>> getSpecialItemUnits() {
		return this.be.getSpecialItemUnits().stream()
			.map(VanillaDetailRegistries.ITEM_STACK::getBasicDetails)
			.map((result0) -> {
				final Map<String, Object> result = new HashMap<>(result0);
				result.put("units", result.remove("count"));
				return result;
			})
			.toList();
	}

	@LuaFunction(mainThread = true)
	public final MethodResult startPrint() {
		if (!this.be.isWorking() && this.be.startPrint()) {
			return MethodResult.of(true);
		}
		return MethodResult.of(false, this.be.getStatus().getId());
	}

	public void onPrintFinish() {
		this.queueEvent("print_complete");
	}

	protected void queueEvent(final String event, final Object... args) {
		this.computers.forEach((computer) -> computer.queueEvent(event, args));
	}

	@Override
	public boolean equals(final IPeripheral other) {
		if (this == other) {
			return true;
		}
		if (other instanceof final PrinterControllerPeripheral otherPeripheral) {
			return this.be == otherPeripheral.be;
		}
		return false;
	}

	//// Generic methods ////

	@LuaFunction(mainThread = true)
	public int getEnergy() {
		return this.be.getEnergyStored();
	}

	@LuaFunction(mainThread = true)
	public int getEnergyCapacity() {
		return this.be.getMaxEnergyStored();
	}
}

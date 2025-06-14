package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.api.ISchematicDataBlockEntity;
import com.github.litermc.vsprinter.compat.computercraft.ComputerDataBlockEntityHelper;

import net.minecraft.nbt.CompoundTag;

import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ServerComputer;
import org.valkyrienskies.core.api.ships.ServerShip;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(AbstractComputerBlockEntity.class)
public abstract class MixinAbstractComputerBlockEntity implements ISchematicDataBlockEntity {
	@Shadow(remap = false)
	@Final
	public abstract ServerComputer createServerComputer();

	@Shadow(remap = false)
	public abstract ServerComputer getServerComputer();

	@Override
	public CompoundTag getPrintableSchematicData() {
		final ServerComputer serverComputer = this.getServerComputer();
		if (serverComputer == null) {
			return null;
		}
		return ComputerDataBlockEntityHelper.getPrintableSchematicData(serverComputer);
	}

	@Override
	public void loadPrintableSchematicData(final CompoundTag data) {
		if (data == null || data.getCompound("FS").isEmpty()) {
			return;
		}
		final ServerComputer serverComputer = this.createServerComputer();
		ComputerDataBlockEntityHelper.loadPrintableSchematicData(serverComputer, data);
	}

	@Override
	public void onPlacedByShip(final ServerShip parentShip, final ServerShip currentShip) {
		final ServerComputer serverComputer = this.getServerComputer();
		if (serverComputer == null) {
			return;
		}
		ComputerDataBlockEntityHelper.onPlacedByShip(serverComputer, parentShip, currentShip);
	}
}

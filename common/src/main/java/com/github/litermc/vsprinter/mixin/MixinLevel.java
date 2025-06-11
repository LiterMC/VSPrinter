package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.ship.PrintedInfoAttachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class MixinLevel {
	@Unique
	private BlockPos destroying = null;

	@Inject(
		method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
		at = @At("HEAD")
	)
	public void destroyBlock$head(
		final BlockPos pos,
		final boolean doDrop,
		final Entity entity,
		final int maxUpdates,
		final CallbackInfoReturnable<Boolean> cir
	) {
		this.destroying = pos;
	}

	@ModifyVariable(
		method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0
	)
	public boolean destroyBlock$doDrop(final boolean doDrop) {
		if (!doDrop) {
			return false;
		}
		final BlockPos pos = this.destroying;
		this.destroying = null;
		final Ship ship = VSGameUtilsKt.getShipManagingPos((Level) ((Object) (this)), pos);
		if (ship == null || !(ship instanceof ServerShip serverShip)) {
			return true;
		}
		final PrintedInfoAttachment info = serverShip.getAttachment(PrintedInfoAttachment.class);
		if (info == null) {
			return true;
		}
		if (!info.getPreventBlockDrops()) {
			return true;
		}
		return false;
	}
}
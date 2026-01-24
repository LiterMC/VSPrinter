package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.attachment.PrintedInfoAttachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Level.class)
public abstract class MixinLevel {
	@ModifyVariable(
		method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
		at = @At("HEAD"),
		argsOnly = true,
		ordinal = 0
	)
	public boolean destroyBlock$doDrop(final boolean doDrop, final @Local(argsOnly = true) BlockPos pos) {
		if (!doDrop) {
			return false;
		}
		final Ship ship = VSGameUtilsKt.getShipManagingPos((Level) ((Object) (this)), pos);
		if (ship == null || !(ship instanceof ServerShip serverShip)) {
			return true;
		}
		final PrintedInfoAttachment info = PrintedInfoAttachment.get(serverShip);
		if (info == null) {
			return true;
		}
		if (!info.getPreventBlockDrops()) {
			return true;
		}
		return false;
	}
}
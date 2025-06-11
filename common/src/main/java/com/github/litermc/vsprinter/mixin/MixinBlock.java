package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.ship.PrintedInfoAttachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class MixinBlock {
	@Inject(
		method = "popResource",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void popResource(final Level level, final BlockPos pos, final ItemStack stack, final CallbackInfo ci) {
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
		if (ship == null || !(ship instanceof ServerShip serverShip)) {
			return;
		}
		final PrintedInfoAttachment info = serverShip.getAttachment(PrintedInfoAttachment.class);
		if (info == null) {
			return;
		}
		if (info.getPreventBlockDrops()) {
			ci.cancel();
		}
	}
}

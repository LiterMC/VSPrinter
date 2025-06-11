package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.ship.PrintedInfoAttachment;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockPlaceContext.class)
public abstract class MixinBlockPlaceContext extends UseOnContext {
	protected MixinBlockPlaceContext() {
		super(null, null, null);
	}

	@Shadow
	public abstract BlockPos getClickedPos();

	@Inject(
		method = "canPlace()Z",
		at = @At("HEAD"),
		cancellable = true
	)
	public void canPlace(final CallbackInfoReturnable<Boolean> cir) {
		final Level level = this.getLevel();
		final Player player = this.getPlayer();
		if (player != null && player.isCreative()) {
			return;
		}
		final BlockPos pos = this.getClickedPos();
		final Ship ship = VSGameUtilsKt.getShipManagingPos(level, pos);
		if (ship == null) {
			return;
		}
		if (!(ship instanceof ServerShip serverShip)) {
			if (level.isClientSide) {
				final String slug = ship.getSlug();
				if (slug.startsWith("+printed+")) {
					// TODO: need proper way to cancel client animation
					cir.setReturnValue(false);
				}
			}
			return;
		}
		final PrintedInfoAttachment info = serverShip.getAttachment(PrintedInfoAttachment.class);
		if (info == null) {
			return;
		}
		if (info.getPreventBlockPlacement()) {
			cir.setReturnValue(false);
		}
	}
}

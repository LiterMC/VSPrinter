package com.github.litermc.vsprinter.block;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class BlockCapabilityProviders {
	private BlockCapabilityProviders() {}

	public static void register() {
		MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<? extends BlockEntity> event) -> {
			if (event.getObject() instanceof PrinterControllerBlockEntity) {
				PrinterControllerBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<PrinterControllerBlockEntity>) (event));
			}
		});
	}
}

package com.github.litermc.vsprinter;

import com.github.litermc.vsprinter.block.BlockCapabilityProviders;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class VSPrinterMod {
	public VSPrinterMod() {
		// IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		BlockCapabilityProviders.register();
		VSPRegistry.register();
	}
}

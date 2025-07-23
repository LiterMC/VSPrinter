package com.github.litermc.vsprinter.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.github.litermc.vsprinter.Constants;
import com.github.litermc.vsprinter.VSPRegistry;
import com.github.litermc.vsprinter.client.renderer.PrinterControllerRenderer;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRegistry {
	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemBlockRenderTypes.setRenderLayer(VSPRegistry.Blocks.PRINTER_FRAME.get(), RenderType.cutout());
		});
	}

	@SubscribeEvent
	public static void registeringRenderers(final EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(VSPRegistry.BlockEntities.PRINTER_CONTROLLER.get(), PrinterControllerRenderer::new);
	}
}

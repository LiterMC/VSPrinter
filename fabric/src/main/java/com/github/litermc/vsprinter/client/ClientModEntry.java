package com.github.litermc.vsprinter.client;

import com.github.litermc.vsprinter.VSPRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class ClientModEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VSPRegistry.Blocks.onRegisterRenderType(BlockRenderLayerMap.INSTANCE::putBlock);
	}
}

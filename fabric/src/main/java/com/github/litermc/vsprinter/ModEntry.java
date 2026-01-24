package com.github.litermc.vsprinter;

import com.github.litermc.vsprinter.block.BlockCapabilityProviders;
import com.github.litermc.vsprinter.command.VSPCommands;
import com.github.litermc.vsprinter.config.ConfigSpec;
import com.github.litermc.vsprinter.platform.FabricConfigFile;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.storage.LevelResource;

public class ModEntry implements ModInitializer {
	private static final String SERVERCONFIG = "serverconfig";

	@Override
	public void onInitialize() {
		VSPListeners.onModInit();
		BlockCapabilityProviders.register();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> VSPCommands.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec))
				.load(server.getWorldPath(LevelResource.ROOT).resolve(SERVERCONFIG).resolve(Constants.MOD_ID + "-server.toml"));
		});

		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec)).unload();
		});

		ServerWorldEvents.LOAD.register((server, level) -> {
			VSPListeners.onServerLevelLoad(level);
		});

		ServerWorldEvents.UNLOAD.register((server, level) -> {
			VSPListeners.onServerLevelUnload(level);
		});

		ServerTickEvents.START_SERVER_TICK.register(VSPListeners::preServerTick);
		ServerTickEvents.END_SERVER_TICK.register(VSPListeners::postServerTick);

		VSPListeners.onModSetup();
	}
}

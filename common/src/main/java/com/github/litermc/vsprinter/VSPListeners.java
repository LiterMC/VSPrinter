package com.github.litermc.vsprinter;

import com.github.litermc.vsprinter.attachment.PrintedInfoAttachment;
import com.github.litermc.vtil.util.LevelUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

public final class VSPListeners {
	private VSPListeners() {}

	public static void onModInit() {
		VSPRegistry.register();
	}

	public static void onModSetup() {
		registerAttachments();
	}

	private static void registerAttachments() {
		ValkyrienSkiesMod.getApi().registerAttachment(PrintedInfoAttachment.class);
	}

	public static void onServerLevelLoad(final ServerLevel level) {
	}

	public static void onServerLevelUnload(final ServerLevel level) {
	}

	public static void preServerTick(final MinecraftServer server) {
	}

	public static void postServerTick(final MinecraftServer server) {
	}
}

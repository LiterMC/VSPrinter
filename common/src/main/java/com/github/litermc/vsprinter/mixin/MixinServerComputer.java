package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.accessor.IComputerAccessor;
import com.github.litermc.vsprinter.accessor.IServerComputerAccessor;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.shared.computer.core.ServerComputer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(ServerComputer.class)
public abstract class MixinServerComputer implements IServerComputerAccessor {
	@Shadow(remap = false)
	@Final
	private Computer computer;

	@Override
	public Computer vsp$getComputer() {
		return this.computer;
	}

	@Override
	public FileSystem vsp$getFileSystem() {
		return ((IComputerAccessor) (this.computer)).vsp$getFileSystem();
	}
}

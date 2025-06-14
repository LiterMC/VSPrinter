package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.accessor.IComputerAccessor;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileSystem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(Computer.class)
public abstract class MixinComputer implements IComputerAccessor {
	@Shadow(remap = false)
	abstract FileSystem getFileSystem();

	@Override
	public FileSystem vsp$getFileSystem() {
		return this.getFileSystem();
	}
}

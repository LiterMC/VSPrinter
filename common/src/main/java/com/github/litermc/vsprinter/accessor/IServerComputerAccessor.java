package com.github.litermc.vsprinter.accessor;

import dan200.computercraft.core.computer.Computer;
import dan200.computercraft.core.filesystem.FileSystem;

public interface IServerComputerAccessor {
	Computer vsp$getComputer();
	FileSystem vsp$getFileSystem();
}

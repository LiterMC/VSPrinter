package com.github.litermc.vsprinter.mixin;

import com.github.litermc.vsprinter.accessor.IServerComputerAccessor;
import com.github.litermc.vsprinter.api.ISchematicDataBlockEntity;

import net.minecraft.nbt.CompoundTag;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.computer.ApiLifecycle;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ServerComputer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.Properties;

@Pseudo
@Mixin(AbstractComputerBlockEntity.class)
public abstract class MixinAbstractComputerBlockEntity implements ISchematicDataBlockEntity {
	@Shadow(remap = false)
	@Final
	public abstract ServerComputer createServerComputer();

	@Shadow(remap = false)
	public abstract ServerComputer getServerComputer();

	@Override
	public CompoundTag getPrintableSchematicData() {
		final ServerComputer serverComputer = this.getServerComputer();
		if (serverComputer == null || !serverComputer.isOn()) {
			return null;
		}
		final FileSystem fs = ((IServerComputerAccessor) (serverComputer)).vsp$getFileSystem();
		final Properties props = new Properties();
		try {
			try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForRead(".vsprinter")) {
				final SeekableByteChannel channel = ref.get();
				props.load(Channels.newInputStream(channel));
			} catch (IOException e) {
				return null;
			}
		} catch (FileSystemException e) {
			return null;
		}
		if (!Boolean.parseBoolean(props.getProperty("blueprint"))) {
			return null;
		}
		final CompoundTag data = new CompoundTag();
		final CompoundTag fsTag = new CompoundTag();
		data.put("FS", fsTag);
		try {
			try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForRead("startup.lua")) {
				final SeekableByteChannel channel = ref.get();
				final ByteBuffer buf = ByteBuffer.allocate((int) (channel.size()));
				channel.read(buf);
				fsTag.putByteArray("startup.lua", buf.array());
			} catch (IOException e) {
			}
		} catch (FileSystemException e) {
		}
		return data;
	}

	@Override
	public void loadPrintableSchematicData(final CompoundTag data) {
		if (data == null) {
			return;
		}
		final CompoundTag fsTag = data.getCompound("FS");
		if (fsTag.isEmpty()) {
			return;
		}
		final ServerComputer serverComputer = this.createServerComputer();
		((IServerComputerAccessor) (serverComputer)).vsp$getComputer().addApi(new ILuaAPI() {
			@Override
			public String[] getNames() {
				return new String[0];
			}
		}, new ApiLifecycle() {
			@Override
			public void startup() {
				final FileSystem fs = ((IServerComputerAccessor) (serverComputer)).vsp$getFileSystem();
				for (final String path : fsTag.getAllKeys()) {
					final ByteBuffer buf = ByteBuffer.wrap(fsTag.getByteArray(path));
					try {
						fs.makeDir(FileSystem.getDirectory(path));
						try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForWrite(path, MountConstants.WRITE_OPTIONS)) {
							final SeekableByteChannel channel = ref.get();
							channel.write(buf);
						} catch (IOException e) {
						}
					} catch (FileSystemException e) {
					}
				}
			}
		});
		serverComputer.turnOn();
	}
}

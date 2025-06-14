package com.github.litermc.vsprinter.compat.computercraft;

import com.github.litermc.vsprinter.accessor.IServerComputerAccessor;

import net.minecraft.nbt.CompoundTag;

import dan200.computercraft.api.filesystem.MountConstants;
import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.core.computer.ApiLifecycle;
import dan200.computercraft.core.filesystem.FileSystem;
import dan200.computercraft.core.filesystem.FileSystemException;
import dan200.computercraft.core.filesystem.FileSystemWrapper;
import dan200.computercraft.shared.computer.core.ServerComputer;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ComputerDataBlockEntityHelper {
	private static final String VSP_PROP_PATH = "/.vsprinter";
	private static final String[] ROOT_ONLY_ARRAY = new String[]{"/"};
	private static final ILuaAPI NOOP_LUA_API = new ILuaAPI() {
		@Override
		public String[] getNames() {
			return new String[0];
		}
	};

	public static CompoundTag getPrintableSchematicData(final ServerComputer serverComputer) {
		if (!serverComputer.isOn()) {
			return null;
		}
		final FileSystem fs = ((IServerComputerAccessor) (serverComputer)).vsp$getFileSystem();
		final Properties props = new Properties();
		try {
			try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForRead(VSP_PROP_PATH)) {
				final SeekableByteChannel channel = ref.get();
				props.load(Channels.newInputStream(channel));
			} catch (IOException e) {
				return null;
			}
		} catch (FileSystemException e) {
			return null;
		}
		if (!Boolean.parseBoolean(props.getProperty("blueprint", ""))) {
			return null;
		}

		final String includePathsValue = props.getProperty("includes");
		final String[] includePaths = includePathsValue == null ? ROOT_ONLY_ARRAY : includePathsValue.split("\\s*,\\s*");
		final String[] ignorePaths = props.getProperty("ignores", "").split("\\s*,\\s*");
		Predicate<String> fileFilter = (path) -> !path.equals(VSP_PROP_PATH);
		for (final String pattern : ignorePaths) {
			if (pattern.isEmpty()) {
				continue;
			}
			final boolean onlyDir = pattern.charAt(pattern.length() - 1) == '/';
			final String cleanedPattern = FileSystem.sanitizePath(pattern, false) + (onlyDir ? "/" : "");
			fileFilter = fileFilter.and((path) -> {
				if (cleanedPattern.equals(path)) {
					return false;
				}
				if (cleanedPattern.length() >= path.length()) {
					return true;
				}
				if (path.startsWith(cleanedPattern) && (onlyDir || path.charAt(cleanedPattern.length()) == '/')) {
					return false;
				}
				return true;
			});
		}
		final Predicate<String> finalFileFilter = fileFilter;

		final CompoundTag data = new CompoundTag();
		final CompoundTag fsTag = new CompoundTag();
		data.put("FS", fsTag);
		Stream.of(includePaths)
			.flatMap((path) -> {
				try {
					return streamAllFiles(fs, path, finalFileFilter);
				} catch (FileSystemException e) {
					return Stream.empty();
				}
			})
			.forEach((path) -> {
				try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForRead(path)) {
					final SeekableByteChannel channel = ref.get();
					final ByteBuffer buf = ByteBuffer.allocate((int) (channel.size()));
					channel.read(buf);
					fsTag.putByteArray(path, buf.array());
				} catch (IOException e) {
				} catch (FileSystemException e) {
				}
			});
		return data;
	}

	public static void loadPrintableSchematicData(final ServerComputer serverComputer, final CompoundTag data) {
		if (data == null) {
			return;
		}
		final CompoundTag fsTag = data.getCompound("FS");
		if (fsTag.isEmpty()) {
			return;
		}
		final IServerComputerAccessor serverComputerAccessor = ((IServerComputerAccessor) (serverComputer));
		serverComputerAccessor.vsp$getComputer().addApi(NOOP_LUA_API, new ApiLifecycle() {
			@Override
			public void startup() {
				final FileSystem fs = serverComputerAccessor.vsp$getFileSystem();
				for (final String path : fsTag.getAllKeys()) {
					try {
						fs.makeDir(FileSystem.getDirectory(path));
						try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForWrite(path, MountConstants.WRITE_OPTIONS)) {
							final SeekableByteChannel channel = ref.get();
							channel.write(ByteBuffer.wrap(fsTag.getByteArray(path)));
						} catch (IOException e) {
						}
					} catch (FileSystemException e) {
					}
				}
			}
		});
		serverComputer.turnOn();
	}

	public static void onPlacedByShip(final ServerComputer serverComputer, final ServerShip parentShip, final ServerShip currentShip) {
		final Properties props = new Properties(3);
		props.setProperty("printed", "true");
		props.setProperty("parent-ship-id", Optional.ofNullable(parentShip).map((ship) -> Long.toString(ship.getId())).orElse(""));
		props.setProperty("parent-ship", Optional.ofNullable(parentShip).map(ServerShip::getSlug).orElse(""));

		final IServerComputerAccessor serverComputerAccessor = ((IServerComputerAccessor) (serverComputer));
		serverComputerAccessor.vsp$getComputer().addApi(NOOP_LUA_API, new ApiLifecycle() {
			@Override
			public void startup() {
				final FileSystem fs = serverComputerAccessor.vsp$getFileSystem();
				try (final FileSystemWrapper<SeekableByteChannel> ref = fs.openForWrite(VSP_PROP_PATH, MountConstants.WRITE_OPTIONS)) {
					final SeekableByteChannel channel = ref.get();
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					props.store(baos, "Generated by VSPrinter");
					channel.write(ByteBuffer.wrap(baos.toByteArray()));
				} catch (IOException e) {
				} catch (FileSystemException e) {
				}
			}
		});
	}

	private static Stream<String> streamAllFiles(final FileSystem fs, final String base, final Predicate<String> filter) throws FileSystemException {
		if (!filter.test(base.endsWith("/") ? base : (base + "/"))) {
			return Stream.empty();
		}
		final String driveName = fs.getMountLabel(base);
		return fs.list(base).stream().flatMap((name) -> {
			final String newPath = fs.combine(base, name);
			try {
				if (!fs.isDir(newPath)) {
					return filter.test(newPath) ? Stream.of(newPath) : Stream.empty();
				}
				if (!fs.getMountLabel(newPath).equals(driveName)) {
					return Stream.empty();
				}
				return streamAllFiles(fs, newPath, filter);
			} catch (FileSystemException e) {
				return Stream.empty();
			}
		});
	}
}

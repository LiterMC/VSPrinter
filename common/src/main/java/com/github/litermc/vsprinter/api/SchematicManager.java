package com.github.litermc.vsprinter.api;

import com.github.litermc.vsprinter.Constants;
import com.github.litermc.vsprinter.platform.PlatformHelper;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class SchematicManager {
	public final static String SCHEMA_DIR = "vsp_schema";
	private final static SchematicManager INSTANCE = new SchematicManager();

	private final MinecraftServer server;
	private final CacheMap<String, PrintableSchematic> cachedSchematic = new CacheMap<>(32);

	private SchematicManager() {
		this.server = PlatformHelper.get().getCurrentServer();
	}

	public static SchematicManager get() {
		return INSTANCE;
	}

	public PrintableSchematic getSchematic(final String fingerprint) {
		return this.cachedSchematic.computeIfAbsentUnref(fingerprint, this::loadSchematic);
	}

	public void putSchematic(final PrintableSchematic schematic) {
		final String fingerprint = schematic.getFingerprint();
		this.cachedSchematic.computeIfAbsentUnref(fingerprint, (k) -> {
			this.saveSchematic(schematic);
			return schematic;
		});
	}

	private File getSchematicFile(final String fingerprint) {
		final Path dirPath = this.server.getWorldPath(LevelResource.ROOT).resolve(SCHEMA_DIR);
		try {
			Files.createDirectories(dirPath);
		} catch (IOException e) {
			Constants.LOG.error("[VSP]: Cannot create schematic directory", e);
		}
		return dirPath.resolve(fingerprint + ".dat").toFile();
	}

	private PrintableSchematic loadSchematic(final String fingerprint) {
		final File schematicFile = this.getSchematicFile(fingerprint);
		if (!schematicFile.isFile()) {
			return null;
		}
		final byte[] bytes;
		try (
			final FileInputStream fi = new FileInputStream(schematicFile);
			final GZIPInputStream gi = new GZIPInputStream(fi)
		) {
			bytes = gi.readAllBytes();
		} catch (IOException e) {
			Constants.LOG.error("[VSP]: Cannot read schematic {}", schematicFile.getPath(), e);
			return null;
		}
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
		try {
			return PrintableSchematic.readFrom(buf);
		} catch (RuntimeException e) {
			Constants.LOG.error("[VSP]: Cannot parse schematic {}", schematicFile.getPath(), e);
			return null;
		}
	}

	private void saveSchematic(final PrintableSchematic schematic) {
		final String fingerprint = schematic.getFingerprint();
		final File schematicFile = this.getSchematicFile(fingerprint);
		final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(1024));
		schematic.writeTo(buf);
		try {
			schematicFile.createNewFile();
		} catch (IOException e) {
			Constants.LOG.error("[VSP]: Cannot create schematic file {}", schematicFile.getPath(), e);
		}
		try (
			final FileOutputStream fo = new FileOutputStream(schematicFile);
			final GZIPOutputStream go = new GZIPOutputStream(fo)
		) {
			buf.readBytes(go, buf.readableBytes());
		} catch (IOException e) {
			Constants.LOG.error("[VSP]: Cannot write schematic {}", schematicFile.getPath(), e);
		}
	}

	private static final class CacheMap<K, V> extends LinkedHashMap<K, SoftReference<V>> {
		private final int maxSize;

		public CacheMap(final int maxSize) {
			super(8, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		public boolean containsKey(final Object key) {
			return this.getUnref(key) != null;
		}

		public V getUnref(final Object key) {
			return unRef(this.get(key));
		}

		public V getOrDefaultUnref(final Object key, final V def) {
			final V value = this.getUnref(key);
			return value != null ? value : def;
		}

		public V computeUnref(final K key, final BiFunction<? super K, ? super V, ? extends V> remapFunc) {
			return unRef(this.compute(key, (k, ref) -> asRef(remapFunc.apply(k, unRef(ref)))));
		}

		public V computeIfAbsentUnref(final K key, final Function<? super K, ? extends V> mapFunc) {
			final SoftReference<V> ref = this.get(key);
			if (ref != null && ref.get() == null) {
				this.remove(key);
			}
			return unRef(this.compute(key, (k, r) -> {
				final V v = unRef(r);
				return v == null ? asRef(mapFunc.apply(k)) : r;
			}));
		}

		public V computeIfPresentUnref(final K key, final BiFunction<? super K, ? super V, ? extends V> remapFunc) {
			return unRef(this.computeIfPresent(key, (k, ref) -> {
				final V v = unRef(ref);
				return v != null ? asRef(remapFunc.apply(k, v)) : null;
			}));
		}

		public V putUnref(final K key, final V value) {
			if (value == null) {
				throw new NullPointerException("value cannot be null");
			}
			return unRef(this.put(key, new SoftReference<V>(value)));
		}

		public V putIfAbsentUnref(final K key, final V value) {
			if (value == null) {
				throw new NullPointerException("value cannot be null");
			}
			return unRef(this.putIfAbsent(key, new SoftReference<V>(value)));
		}

		@Override
		protected boolean removeEldestEntry(final Map.Entry<K, SoftReference<V>> eldest) {
			if (eldest.getValue().get() == null) {
				return true;
			}
			if (this.size() < this.maxSize) {
				return false;
			}
			final Iterator<Map.Entry<K, SoftReference<V>>> iterator = this.entrySet().iterator();
			while (iterator.hasNext()) {
				if (iterator.next().getValue().get() == null) {
					iterator.remove();
					return false;
				}
			}
			return true;
		}

		private static <V> SoftReference<V> asRef(V v) {
			return v == null ? null : new SoftReference<V>(v);
		}

		private static <V> V unRef(SoftReference<V> ref) {
			return ref == null ? null : ref.get();
		}
	}
}

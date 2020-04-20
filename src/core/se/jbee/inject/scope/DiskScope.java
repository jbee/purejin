package se.jbee.inject.scope;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static se.jbee.inject.Type.raw;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;
import se.jbee.inject.extend.Config;

/**
 * The {@link DiskScope} is a {@link Scope} that persists objects on disk.
 * 
 * Therefore there isn't *the* disk scope but particular scopes for particular
 * directories can be created using {@link Scope#disk(File)} which is
 * automatically bound to an instance of the {@link DiskScope} if the defaults
 * are in place.
 * 
 * {@link DiskScope}s are limited to {@link Serializable} types.
 * 
 * This implementation is not heavily optimised. It does the job and illustrates
 * the principle.
 * 
 * @since 19.1
 */
public final class DiskScope implements Scope, Closeable {

	public static final String SYNC_INTERVAL = "sync";

	private static final class DiskEntry implements Serializable {

		final Serializable obj;
		final long asOfLastModified;
		final File file;
		final File tmpFile;
		final AtomicBoolean syncing = new AtomicBoolean();

		DiskEntry(Serializable obj, long asOfLastModified, File file) {
			this.obj = obj;
			this.asOfLastModified = asOfLastModified;
			this.file = file;
			this.tmpFile = new File(file.getAbsoluteFile() + ".tmp");
		}
	}

	/**
	 * NB. {@link ConcurrentHashMap} does not allow updates while updating.
	 */
	private final Map<String, DiskEntry> loaded = new ConcurrentSkipListMap<>();
	private final long syncInterval;
	private final File dir;
	private final Function<Dependency<?>, String> filenames;

	public DiskScope(Config config, ScheduledExecutorService executor, File dir,
			Function<Dependency<?>, String> filenames) {
		this.dir = dir;
		this.filenames = filenames;
		this.syncInterval = config.of(DiskScope.class).longValue(SYNC_INTERVAL,
				60 * 1000);
		if (syncInterval > 0) {
			executor.scheduleAtFixedRate(this::syncToDisk, syncInterval,
					syncInterval, TimeUnit.MILLISECONDS);
		}
		Runtime.getRuntime().addShutdownHook(new Thread(this::close));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T provide(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		if (!dep.type().isAssignableTo(raw(Serializable.class)))
			throw new UnresolvableDependency.SupplyFailed(
					"Any disk type has to be serializable", null);
		return (T) loaded.compute(filenames.apply(dep),
				(key, value) -> loadFromFile(key, value, provider)).obj;
	}

	private <T> DiskEntry loadFromFile(String filename, DiskEntry value,
			Provider<T> provider) {
		File file = new File(dir,
				filename.replace('.', '_').replaceAll("[*@]", "_") + ".bin");
		final long lastModified = file.lastModified();
		if (value != null && value.asOfLastModified == lastModified)
			return value; // still valid
		if (file.exists()) {
			try (ObjectInputStream in = new ObjectInputStream(
					new FileInputStream(file))) {
				return new DiskEntry((Serializable) in.readObject(),
						lastModified, file);
			} catch (Exception e) {
				throw new UnresolvableDependency.SupplyFailed(
						"Failed to load object", e);
			}
		}
		Serializable res = (Serializable) provider.provide();
		DiskEntry entry = new DiskEntry(res, lastModified, file);
		if (dirReady())
			syncToDisk(entry);
		return entry;
	}

	private boolean dirReady() {
		return dir.exists() || dir.mkdirs();
	}

	private static void syncToDisk(DiskEntry entry) {
		if (!entry.syncing.compareAndSet(false, true))
			return; // already doing it...
		try (ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream(entry.tmpFile))) {
			out.writeObject(entry.obj);
			Files.move(entry.tmpFile.toPath(), entry.file.toPath(),
					REPLACE_EXISTING, ATOMIC_MOVE);
		} catch (Exception e) {
			// too bad...
		} finally {
			entry.syncing.set(false);
		}
	}

	@Override
	public void close() {
		syncToDisk();
	}

	private void syncToDisk() {
		if (!dirReady())
			return;
		for (DiskEntry e : loaded.values())
			syncToDisk(e);
	}
}

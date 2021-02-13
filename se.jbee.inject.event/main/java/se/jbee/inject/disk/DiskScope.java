package se.jbee.inject.disk;

import se.jbee.inject.*;
import se.jbee.inject.event.On;
import se.jbee.inject.schedule.Scheduled;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static se.jbee.lang.Type.raw;

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
 * @since 8.1
 */
public final class DiskScope implements Scope, Closeable, Scheduled.Aware {

	public static final class DiskEntry implements Serializable {

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
	private final Map<String, DiskEntry> entriesByName = new ConcurrentSkipListMap<>();
	private final File rootDir;
	private final Function<Dependency<?>, String> dep2name;
	private final Consumer<DiskEntry> disk;

	public DiskScope(File rootDir, Function<Dependency<?>, String> dep2name,
			Consumer<DiskEntry> disk) {
		this.rootDir = rootDir;
		this.dep2name = dep2name;
		this.disk = disk;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency {
		if (!dep.type().isAssignableTo(raw(Serializable.class)))
			throw new UnresolvableDependency.SupplyFailed(
					"Any disk type has to be serializable", null);
		return (T) entriesByName.compute(dep2name.apply(dep),
				(key, value) -> loadFromFile(key, value, provider)).obj;
	}

	private <T> DiskEntry loadFromFile(String filename, DiskEntry value,
			Provider<T> provider) {
		File file = new File(rootDir,
				filename.replace('.', '_').replaceAll("[*@]", "_") + ".bin");
		final long lastModified = file.lastModified();
		if (value != null && value.asOfLastModified == lastModified)
			return value; // still valid
		if (file.exists()) {
			try (ObjectInputStream in = new ObjectInputStream(
					new BufferedInputStream(new FileInputStream(file)))) {
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
			disk.accept(entry);
		return entry;
	}

	private boolean dirReady() {
		return rootDir.exists() || rootDir.mkdirs();
	}

	@On(On.Shutdown.class)
	@Override
	public void close() {
		syncToDisk();
	}

	@Scheduled(every = 1, unit = TimeUnit.MINUTES, by = "syncTime")
	public void syncToDisk() {
		if (!dirReady())
			return;
		for (DiskEntry e : entriesByName.values())
			disk.accept(e);
	}

	public static void syncToDisk(DiskEntry entry) {
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
}

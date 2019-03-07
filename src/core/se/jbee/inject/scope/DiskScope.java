package se.jbee.inject.scope;

import static se.jbee.inject.Type.raw;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

/**
 * The {@link DiskScope} is a {@link Scope} that persists objects on disk.
 * 
 * Therefore there isn't *the* disk scope but particular scopes for particular
 * directories can be created using {@link Scope#disk(File)} which is
 * automatically bound to an instance of the {@link DiskScope} if the defaults
 * are in place.
 * 
 * {@link DiskScope}s are limited to instances that can be serialised to disk.
 * 
 * This implementation is not heavily optimised. It does the job and illustrates
 * the principle.
 * 
 * @since 19.1
 */
public final class DiskScope implements Scope, Closeable {

	private static final class DiskEntry implements Serializable {

		final Serializable obj;
		final long asOfLastModified;
		final File file;

		DiskEntry(Serializable obj, long asOfLastModified, File file) {
			this.obj = obj;
			this.asOfLastModified = asOfLastModified;
			this.file = file;
		}
	}

	private final File dir;
	private final Function<Dependency<?>, String> filenames;
	private final Map<String, DiskEntry> loaded = new ConcurrentHashMap<>();

	//TODO add daemon that periodically snycs to disk - add bindable config for period

	public DiskScope(File dir, Function<Dependency<?>, String> filenames) {
		this.dir = dir;
		this.filenames = filenames;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> close()));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T yield(int serialID, Dependency<? super T> dep,
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
		if (dirReady())
			save(res, file);
		return new DiskEntry(res, lastModified, file);
	}

	private boolean dirReady() {
		return dir.exists() || dir.mkdirs();
	}

	private static boolean save(Serializable obj, File file) {
		try (ObjectOutputStream out = new ObjectOutputStream(
				new FileOutputStream(file))) {
			out.writeObject(obj);
			return true;
		} catch (Exception e) {
			// well, that's not good but we can serve from provider
			return false;
		}
	}

	@Override
	public void close() {
		if (!dirReady())
			return;
		for (DiskEntry e : loaded.values()) {
			save(e.obj, e.file);
		}
	}
}

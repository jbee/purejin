package se.jbee.inject.scope;

import java.io.File;
import java.util.function.Function;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

/**
 * The {@link DiskScope} is a {@link Scope} that persists objects on disk.
 * 
 * Therefore there isn't *the* disk scope but particular scopes for particular
 * directories.
 * 
 * {@link DiskScope}s are limited to instances that can be serialised to disk.
 * 
 * @since 19.1
 */
public final class DiskScope implements Scope {

	private final File dir;
	private final Function<Dependency<?>, String> filenames;

	private DiskScope(File dir, Function<Dependency<?>, String> filenames) {
		this.dir = dir;
		this.filenames = filenames;
	}

	@Override
	public <T> T yield(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		// TODO Auto-generated method stub
		// check Serializable
		// read from disk by using the full dependency path
		// if not on disk ask the provider and write to disk
		return null;
	}

}

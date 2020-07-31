package se.jbee.inject.scope;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

/**
 * The 'synchronous'-{@link Scope} will be asked first passing a special
 * resolver that will ask the 'asynchronous' repository when invoked. Thereby
 * the repository originally bound will be asked once. Thereafter the result is
 * stored in the synchronous repository.
 *
 * Both repositories will remember the resolved instance whereby the repository
 * considered as the synchronous-repository will deliver a consistent image of
 * the world as long as it exists.
 *
 * @author Jan Bernitt (jan@jbee.se)
 */
public final class SnapshotScope implements Scope {

	public static Scope asSnapshot(Scope src, Scope dest) {
		return new SnapshotScope(src, dest);
	}

	private final Scope src;
	private final Scope dest;

	private SnapshotScope(Scope src, Scope dest) {
		this.src = src;
		this.dest = dest;
	}

	@Override
	public <T> T provide(int serialID, int resources, Dependency<? super T> dep,
			Provider<T> provider) throws UnresolvableDependency {
		return dest.provide(serialID, resources, dep,
				() -> src.provide(serialID, resources, dep, provider));
	}

}
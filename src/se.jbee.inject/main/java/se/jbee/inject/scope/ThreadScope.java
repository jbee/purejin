package se.jbee.inject.scope;

import se.jbee.inject.Dependency;
import se.jbee.inject.Provider;
import se.jbee.inject.Resource;
import se.jbee.inject.Scope;
import se.jbee.inject.UnresolvableDependency;

/**
 * Asks the {@link Provider} once per thread per {@link Resource} which is
 * understand commonly as a usual 'per-thread' singleton.
 */
public final class ThreadScope implements Scope {

	private final ThreadLocal<Object[]> instances = new ThreadLocal<>();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T provide(int serialID, Dependency<? super T> dep,
			Provider<T> provider, int generators)
			throws UnresolvableDependency {
		Object[] objects = instances.get();
		if (objects == null) {
			objects = new Object[generators];
			instances.set(objects);
		}
		Object res = objects[serialID];
		if (res == null) {
			res = provider.provide();
			objects[serialID] = res;
		}
		return (T) res;
	}
}